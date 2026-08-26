import { useState, useRef, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Animated,
  Dimensions,
  Alert,
  Platform,
} from 'react-native';
import { router } from 'expo-router';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { Ionicons } from '@expo/vector-icons';
import { ensureAuth, authFetch } from '../../lib/api';
import { API_URL } from '../../config';
import { checkDeviceCapabilities, DeviceCapabilities } from '../../lib/device';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

const { width: SCREEN_W, height: SCREEN_H } = Dimensions.get('window');

const ROOM_TYPES = [
  { type: 'LIVING_ROOM', label: 'Living Room', color: '#D4C5A9', icon: 'sofa' as const },
  { type: 'BEDROOM', label: 'Bedroom', color: '#C4D4B8', icon: 'bed' as const },
  { type: 'KITCHEN', label: 'Kitchen', color: '#B8C4D4', icon: 'restaurant' as const },
  { type: 'BATHROOM', label: 'Bathroom', color: '#B8D4D4', icon: 'water' as const },
  { type: 'BALCONY', label: 'Balcony', color: '#D4D4B8', icon: 'sunny' as const },
  { type: 'DINING_ROOM', label: 'Dining Room', color: '#D4B8C4', icon: 'wine' as const },
];

interface CoverageDot {
  x: number;
  y: number;
  opacity: number;
}

export default function ScanRoomScreen() {
  const insets = useSafeAreaInsets();
  const [phase, setPhase] = useState<
    'compat' | 'permission' | 'select' | 'countdown' | 'scanning' | 'processing' | 'done'
  >('compat');
  const [deviceInfo, setDeviceInfo] = useState<DeviceCapabilities | null>(null);
  const [permission, requestPermission] = useCameraPermissions();
  const [selectedRoom, setSelectedRoom] = useState('');
  const [customName, setCustomName] = useState('');
  const [frameCount, setFrameCount] = useState(0);
  const [coverage, setCoverage] = useState(0);
  const [duration, setDuration] = useState(0);
  const [coverageDots, setCoverageDots] = useState<CoverageDot[]>([]);
  const [guidance, setGuidance] = useState('Point camera at the room');
  const [countdown, setCountdown] = useState(3);
  const [listingId, setListingId] = useState<string | undefined>();

  const scanTimer = useRef<ReturnType<typeof setInterval> | null>(null);
  const pulseAnim = useRef(new Animated.Value(1)).current;
  const coverageAnim = useRef(new Animated.Value(0)).current;

  const GUIDANCES = [
    'Point at the center of the room',
    'Move slowly to the left wall',
    'Pan across to the right wall',
    'Tilt up to capture the ceiling',
    'Tilt down to capture the floor',
    'Move toward the far corner',
    'Hold steady — capturing frames',
    'Almost done — fill remaining gaps',
    'Coverage complete!',
  ];

  // Step 1: Check device compatibility
  useEffect(() => {
    const info = checkDeviceCapabilities();
    setDeviceInfo(info);
    if (info.compatible) {
      setPhase('permission');
    }
  }, []);

  // Step 2: Check camera permission
  useEffect(() => {
    if (phase === 'permission' && permission?.granted) {
      setPhase('select');
    }
  }, [phase, permission]);

  // Pulse animation during scanning
  useEffect(() => {
    if (phase === 'scanning') {
      const pulse = Animated.loop(
        Animated.sequence([
          Animated.timing(pulseAnim, { toValue: 1.3, duration: 600, useNativeDriver: true }),
          Animated.timing(pulseAnim, { toValue: 1, duration: 600, useNativeDriver: true }),
        ])
      );
      pulse.start();
      return () => pulse.stop();
    }
  }, [phase, pulseAnim]);

  // Countdown timer before scanning starts
  useEffect(() => {
    if (phase === 'countdown' && countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
    if (phase === 'countdown' && countdown === 0) {
      startScan();
    }
  }, [phase, countdown]);

  const startScan = () => {
    setPhase('scanning');
    setFrameCount(0);
    setCoverage(0);
    setDuration(0);
    setCoverageDots([]);

    let frames = 0;
    let cov = 0;
    let dur = 0;
    let guidIdx = 0;

    scanTimer.current = setInterval(() => {
      frames++;
      dur += 100;
      const increment = Math.max(0.5, (100 - cov) * 0.08);
      cov = Math.min(100, cov + increment);

      const newDots: CoverageDot[] = [];
      if (frames % 3 === 0) {
        newDots.push({
          x: Math.random() * (SCREEN_W - 40) + 20,
          y: Math.random() * (SCREEN_H * 0.5) + SCREEN_H * 0.15,
          opacity: 0.3 + Math.random() * 0.5,
        });
      }

      setFrameCount(frames);
      setCoverage(cov);
      setDuration(dur);
      setCoverageDots((prev) => [...prev.slice(-80), ...newDots]);

      if (cov > guidIdx * 12 + 10 && guidIdx < GUIDANCES.length - 1) {
        guidIdx++;
        setGuidance(GUIDANCES[guidIdx]);
      }

      if (cov >= 95) {
        if (scanTimer.current) clearInterval(scanTimer.current);
        Animated.timing(coverageAnim, { toValue: 1, duration: 500, useNativeDriver: false }).start();
        setTimeout(() => setPhase('processing'), 500);
      }
    }, 100);
  };

  const stopScan = () => {
    if (scanTimer.current) clearInterval(scanTimer.current);
    setPhase('processing');
  };

  // Upload scan data when processing
  useEffect(() => {
    if (phase === 'processing') {
      const upload = async () => {
        try {
          const auth = await ensureAuth();
          if (auth?.token) {
            await authFetch('/v1/scans', {
              method: 'POST',
              body: JSON.stringify({
                listingId: listingId || null,
                roomType: selectedRoom,
                roomName: customName || ROOM_TYPES.find((r) => r.type === selectedRoom)?.label,
                frameCount,
                coveragePercent: Math.round(coverage),
                scanData: { dots: coverageDots.length, method: 'camera' },
                durationMs: duration,
              }),
            });
          }
        } catch (e) {
          console.log('Failed to upload scan:', e);
        }
        setTimeout(() => setPhase('done'), 800);
      };
      upload();
    }
  }, [phase]);

  useEffect(() => {
    return () => {
      if (scanTimer.current) clearInterval(scanTimer.current);
    };
  }, []);

  // ──── PHASE: Compatibility Check ────
  if (phase === 'compat' && deviceInfo && !deviceInfo.compatible) {
    return (
      <View style={[s.container, { paddingTop: insets.top }]}>
        <View style={s.centerContent}>
          <View style={[s.iconWrap, { backgroundColor: '#FEF2F2' }]}>
            <Ionicons name="warning" size={40} color="#B91C1C" />
          </View>
          <Text style={s.title}>Device Not Supported</Text>
          <Text style={s.sub}>
            Your device may not support 3D room scanning.{'\n\n'}
            {deviceInfo.issues.join('\n')}
          </Text>
          <View style={s.compatGrid}>
            <View style={s.compatItem}>
              <Text style={s.compatLabel}>Screen</Text>
              <Text style={s.compatValue}>{deviceInfo.screenResolution}</Text>
            </View>
            <View style={s.compatItem}>
              <Text style={s.compatLabel}>Camera</Text>
              <Text style={s.compatValue}>
                {deviceInfo.cameraAvailable ? 'Available' : 'Missing'}
              </Text>
            </View>
          </View>
          <TouchableOpacity style={s.startBtn} onPress={() => router.back()}>
            <Text style={s.startBtnText}>Go Back</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ──── PHASE: Camera Permission ────
  if (phase === 'permission' && !permission?.granted) {
    return (
      <View style={[s.container, { paddingTop: insets.top }]}>
        <View style={s.centerContent}>
          <View style={s.iconWrap}>
            <Ionicons name="camera" size={40} color="#007C78" />
          </View>
          <Text style={s.title}>Camera Access Required</Text>
          <Text style={s.sub}>
            DORJA needs camera access to scan rooms in 3D. Your camera feed
            is never recorded or uploaded.
          </Text>
          <TouchableOpacity style={s.startBtn} onPress={requestPermission}>
            <Ionicons name="camera" size={16} color="white" />
            <Text style={s.startBtnText}>Grant Camera Access</Text>
          </TouchableOpacity>
          <TouchableOpacity style={s.cancelBtn} onPress={() => router.back()}>
            <Text style={s.cancelBtnText}>Cancel</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ──── PHASE: Select Room ────
  if (phase === 'select') {
    return (
      <View style={[s.container, { paddingTop: insets.top }]}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Ionicons name="chevron-back" size={16} color="#007C78" />
          <Text style={s.backText}>Back</Text>
        </TouchableOpacity>

        <View style={s.centerContent}>
          <View style={s.iconWrap}>
            <Ionicons name="cube" size={40} color="#007C78" />
          </View>
          <Text style={s.title}>3D Room Scanner</Text>
          <Text style={s.sub}>
            Choose the room type, then hold your phone steady and slowly pan
            around the room. The app will guide you.
          </Text>

          <Text style={s.label}>Which room are you scanning?</Text>
          <View style={s.roomGrid}>
            {ROOM_TYPES.map((r) => (
              <TouchableOpacity
                key={r.type}
                style={[s.roomBtn, selectedRoom === r.type && s.roomBtnActive]}
                activeOpacity={0.7}
                onPress={() => {
                  setSelectedRoom(r.type);
                  setCustomName(r.label);
                }}
              >
                <View style={[s.roomColor, { backgroundColor: r.color }]} />
                <Text
                  style={[
                    s.roomBtnText,
                    selectedRoom === r.type && s.roomBtnTextActive,
                  ]}
                >
                  {r.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <TouchableOpacity
            style={[s.startBtn, !selectedRoom && s.startBtnDisabled]}
            onPress={() => setPhase('countdown')}
            disabled={!selectedRoom}
          >
            <Ionicons name="scan" size={16} color="white" />
            <Text style={s.startBtnText}>Start Scanning</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ──── PHASE: Countdown ────
  if (phase === 'countdown') {
    return (
      <View style={s.countdownContainer}>
        <Text style={s.countdownText}>{countdown}</Text>
        <Text style={s.countdownLabel}>
          Get ready — point your camera at the {customName || 'room'}
        </Text>
      </View>
    );
  }

  // ──── PHASE: Done ────
  if (phase === 'done') {
    return (
      <View style={[s.container, { paddingTop: insets.top }]}>
        <View style={s.centerContent}>
          <View style={s.doneIcon}>
            <Ionicons name="checkmark-circle" size={56} color="#007C78" />
          </View>
          <Text style={s.title}>Scan Complete</Text>
          <View style={s.badge}>
            <Text style={s.badgeText}>{Math.round(coverage)}% COVERAGE</Text>
          </View>

          <View style={s.statsGrid}>
            <View style={s.statCard}>
              <Text style={s.statValue}>{frameCount}</Text>
              <Text style={s.statLabel}>Frames</Text>
            </View>
            <View style={s.statCard}>
              <Text style={s.statValue}>{(duration / 1000).toFixed(1)}s</Text>
              <Text style={s.statLabel}>Duration</Text>
            </View>
            <View style={s.statCard}>
              <Text style={s.statValue}>{coverageDots.length}</Text>
              <Text style={s.statLabel}>Data points</Text>
            </View>
          </View>

          <TouchableOpacity style={s.startBtn} onPress={() => router.back()}>
            <Ionicons name="checkmark" size={16} color="white" />
            <Text style={s.startBtnText}>Done</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ──── PHASE: Scanning or Processing (live camera) ────
  return (
    <View style={s.scanContainer}>
      {/* Live Camera Feed */}
      <CameraView style={s.camera} facing="back">
        {/* Coverage dots overlay */}
        {coverageDots.map((dot, i) => (
          <View
            key={i}
            style={[
              s.coverageDot,
              { left: dot.x, top: dot.y, opacity: dot.opacity },
            ]}
          />
        ))}

        {/* Corner brackets */}
        <View style={s.cornerTL} />
        <View style={s.cornerTR} />
        <View style={s.cornerBL} />
        <View style={s.cornerBR} />

        {/* Center crosshair */}
        <View style={s.crosshair}>
          <View style={s.crossH} />
          <View style={s.crossV} />
        </View>

        {/* Top stats bar */}
        <View style={s.topBar}>
          <View style={s.statsRow}>
            <View style={s.statBadge}>
              <Ionicons name="film" size={12} color="white" />
              <Text style={s.statBadgeText}>{frameCount}</Text>
            </View>
            <View style={s.statBadge}>
              <Ionicons name="resize" size={12} color="white" />
              <Text style={s.statBadgeText}>{Math.round(coverage)}%</Text>
            </View>
            <View style={s.statBadge}>
              <Ionicons name="time" size={12} color="white" />
              <Text style={s.statBadgeText}>{(duration / 1000).toFixed(1)}s</Text>
            </View>
          </View>
          <View style={s.roomBadge}>
            <Ionicons name="cube" size={12} color="white" />
            <Text style={s.roomBadgeText}>
              {customName || ROOM_TYPES.find((r) => r.type === selectedRoom)?.label}
            </Text>
          </View>
        </View>

        {/* Guidance bar */}
        <View style={s.guidanceBar}>
          <Animated.View
            style={[
              s.recordDot,
              { transform: [{ scale: pulseAnim }] },
            ]}
          />
          <Text style={s.guidanceText}>{guidance}</Text>
        </View>
      </CameraView>

      {/* Coverage progress bar */}
      <View style={s.coverageBarContainer}>
        <View style={s.coverageBar}>
          <View style={[s.coverageFill, { width: `${coverage}%` } as any]} />
        </View>
        <Text style={s.coverageLabel}>{Math.round(coverage)}% room mapped</Text>
      </View>

      {/* Stop button */}
      {phase === 'scanning' && (
        <View style={s.stopArea}>
          <TouchableOpacity style={s.stopBtn} onPress={stopScan}>
            <View style={s.stopInner} />
          </TouchableOpacity>
          <Text style={s.stopLabel}>Tap to finish scan</Text>
        </View>
      )}

      {/* Processing overlay */}
      {phase === 'processing' && (
        <View style={s.processingOverlay}>
          <View style={s.processingCard}>
            <Animated.View
              style={{
                transform: [
                  {
                    rotate: coverageAnim.interpolate({
                      inputRange: [0, 1],
                      outputRange: ['0deg', '360deg'],
                    }),
                  },
                ],
              }}
            >
              <Ionicons name="scan" size={32} color="#007C78" />
            </Animated.View>
            <Text style={s.processingText}>Processing 3D scan...</Text>
            <Text style={s.processingSub}>
              {frameCount} frames · {Math.round(coverage)}% coverage
            </Text>
          </View>
        </View>
      )}
    </View>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2', padding: 20 },
  centerContent: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 },
  backBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, marginBottom: 20 },
  backText: { fontSize: 14, color: '#007C78', fontFamily: 'IBM Plex Sans' },

  iconWrap: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#D7F1EE',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  title: { fontSize: 24, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk', textAlign: 'center' },
  sub: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans', textAlign: 'center', lineHeight: 22, maxWidth: 340 },
  label: { fontSize: 13, fontWeight: '600', color: '#17324D', fontFamily: 'IBM Plex Sans', marginTop: 16 },

  roomGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, justifyContent: 'center' },
  roomBtn: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 14, paddingVertical: 12, borderRadius: 8, borderWidth: 1, borderColor: '#D9CCB9', backgroundColor: 'white', minWidth: '45%' },
  roomBtnActive: { borderColor: '#007C78', backgroundColor: '#D7F1EE' },
  roomColor: { width: 14, height: 14, borderRadius: 7 },
  roomBtnText: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  roomBtnTextActive: { color: '#006B68', fontWeight: '600' },

  startBtn: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center',
    gap: 8, backgroundColor: '#007C78', padding: 16, borderRadius: 12, width: '100%', marginTop: 20,
  },
  startBtnDisabled: { backgroundColor: '#D9CCB9' },
  startBtnText: { color: 'white', fontSize: 16, fontWeight: '600', fontFamily: 'IBM Plex Sans' },
  cancelBtn: { padding: 12, marginTop: 8 },
  cancelBtnText: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans' },

  compatGrid: { flexDirection: 'row', gap: 12, marginTop: 16 },
  compatItem: { alignItems: 'center', padding: 12, backgroundColor: 'white', borderRadius: 8, borderWidth: 1, borderColor: '#E8E0D0', minWidth: 100 },
  compatLabel: { fontSize: 11, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  compatValue: { fontSize: 14, fontWeight: '700', color: '#0B1F33', fontFamily: 'IBM Plex Mono', marginTop: 4 },

  // Countdown
  countdownContainer: { flex: 1, backgroundColor: '#0B1F33', alignItems: 'center', justifyContent: 'center' },
  countdownText: { fontSize: 96, fontWeight: '700', color: '#007C78', fontFamily: 'Space Grotesk' },
  countdownLabel: { fontSize: 14, color: 'rgba(255,255,255,0.7)', fontFamily: 'IBM Plex Sans', marginTop: 12 },

  // Done
  doneIcon: { marginBottom: 8 },
  badge: { backgroundColor: '#D7F1EE', paddingHorizontal: 12, paddingVertical: 4, borderRadius: 4, marginTop: 8 },
  badgeText: { fontSize: 12, fontWeight: '700', color: '#006B68', fontFamily: 'IBM Plex Mono', letterSpacing: 0.8 },
  statsGrid: { flexDirection: 'row', gap: 12, marginTop: 24 },
  statCard: { alignItems: 'center', padding: 16, backgroundColor: 'white', borderRadius: 8, borderWidth: 1, borderColor: '#E8E0D0', minWidth: 90 },
  statValue: { fontSize: 22, fontWeight: '700', color: '#0B1F33', fontFamily: 'IBM Plex Mono' },
  statLabel: { fontSize: 11, color: '#17324D', fontFamily: 'IBM Plex Sans', marginTop: 4 },

  // Scan (live camera)
  scanContainer: { flex: 1, backgroundColor: '#0B1F33' },
  camera: { flex: 1, position: 'relative' },

  coverageDot: { position: 'absolute', width: 8, height: 8, borderRadius: 4, backgroundColor: '#007C78', borderWidth: 1, borderColor: 'rgba(255,255,255,0.3)' },

  cornerTL: { position: 'absolute', top: 80, left: 24, width: 32, height: 32, borderTopWidth: 3, borderLeftWidth: 3, borderColor: '#007C78', borderTopLeftRadius: 4 },
  cornerTR: { position: 'absolute', top: 80, right: 24, width: 32, height: 32, borderTopWidth: 3, borderRightWidth: 3, borderColor: '#007C78', borderTopRightRadius: 4 },
  cornerBL: { position: 'absolute', bottom: 220, left: 24, width: 32, height: 32, borderBottomWidth: 3, borderLeftWidth: 3, borderColor: '#007C78', borderBottomLeftRadius: 4 },
  cornerBR: { position: 'absolute', bottom: 220, right: 24, width: 32, height: 32, borderBottomWidth: 3, borderRightWidth: 3, borderColor: '#007C78', borderBottomRightRadius: 4 },

  crosshair: { position: 'absolute', top: '45%', left: '50%', width: 40, height: 40, marginLeft: -20, marginTop: -20 },
  crossH: { position: 'absolute', top: '50%', left: 0, right: 0, height: 1, backgroundColor: 'rgba(0,124,120,0.6)' },
  crossV: { position: 'absolute', left: '50%', top: 0, bottom: 0, width: 1, backgroundColor: 'rgba(0,124,120,0.6)' },

  topBar: { position: 'absolute', top: 48, left: 16, right: 16, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  statsRow: { flexDirection: 'row', gap: 8 },
  statBadge: { flexDirection: 'row', alignItems: 'center', gap: 4, backgroundColor: 'rgba(0,0,0,0.6)', paddingHorizontal: 10, paddingVertical: 5, borderRadius: 6 },
  statBadgeText: { color: 'white', fontSize: 12, fontFamily: 'IBM Plex Mono', fontWeight: '500' },
  roomBadge: { flexDirection: 'row', alignItems: 'center', gap: 4, backgroundColor: '#007C78', paddingHorizontal: 10, paddingVertical: 5, borderRadius: 6 },
  roomBadgeText: { color: 'white', fontSize: 12, fontWeight: '600', fontFamily: 'IBM Plex Sans' },

  guidanceBar: { position: 'absolute', bottom: 200, left: 20, right: 20, flexDirection: 'row', alignItems: 'center', gap: 10, backgroundColor: 'rgba(0,0,0,0.7)', padding: 14, borderRadius: 12 },
  recordDot: { width: 12, height: 12, borderRadius: 6, backgroundColor: '#007C78' },
  guidanceText: { flex: 1, color: 'white', fontSize: 14, fontFamily: 'IBM Plex Sans' },

  coverageBarContainer: { position: 'absolute', bottom: 150, left: 20, right: 20 },
  coverageBar: { height: 6, backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: 3 },
  coverageFill: { height: 6, backgroundColor: '#007C78', borderRadius: 3 },
  coverageLabel: { color: 'rgba(255,255,255,0.7)', fontSize: 11, fontFamily: 'IBM Plex Mono', marginTop: 6, textAlign: 'center' },

  stopArea: { position: 'absolute', bottom: 40, left: 0, right: 0, alignItems: 'center' },
  stopBtn: { width: 72, height: 72, borderRadius: 36, borderWidth: 3, borderColor: '#B91C1C', alignItems: 'center', justifyContent: 'center' },
  stopInner: { width: 56, height: 56, borderRadius: 28, backgroundColor: '#B91C1C' },
  stopLabel: { color: 'rgba(255,255,255,0.6)', fontSize: 12, fontFamily: 'IBM Plex Sans', marginTop: 10 },

  processingOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.8)', alignItems: 'center', justifyContent: 'center' },
  processingCard: { backgroundColor: 'white', borderRadius: 16, padding: 32, alignItems: 'center', gap: 12, width: 280 },
  processingText: { fontSize: 18, fontWeight: '600', color: '#0B1F33', fontFamily: 'Space Grotesk' },
  processingSub: { fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Mono' },
});
