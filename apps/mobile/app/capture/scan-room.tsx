import { useState, useRef, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Animated, Dimensions, Alert } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { Icon, Badge } from '../../components/Icons';

const API_URL = 'http://localhost:4000';
const { width: SCREEN_W, height: SCREEN_H } = Dimensions.get('window');

const ROOM_TYPES = [
  { type: 'LIVING_ROOM', label: 'Living Room', color: '#D4C5A9' },
  { type: 'BEDROOM', label: 'Bedroom', color: '#C4D4B8' },
  { type: 'KITCHEN', label: 'Kitchen', color: '#B8C4D4' },
  { type: 'BATHROOM', label: 'Bathroom', color: '#B8D4D4' },
  { type: 'BALCONY', label: 'Balcony', color: '#D4D4B8' },
  { type: 'DINING_ROOM', label: 'Dining Room', color: '#D4B8C4' },
];

interface CoverageDot {
  x: number;
  y: number;
  opacity: number;
}

export default function ScanRoomScreen() {
  const { listingId, roomType: initialRoomType } = useLocalSearchParams<{ listingId: string; roomType?: string }>();
  const [phase, setPhase] = useState<'select' | 'scanning' | 'processing' | 'done'>('select');
  const [selectedRoom, setSelectedRoom] = useState(initialRoomType || '');
  const [customName, setCustomName] = useState('');
  const [frameCount, setFrameCount] = useState(0);
  const [coverage, setCoverage] = useState(0);
  const [duration, setDuration] = useState(0);
  const [coverageDots, setCoverageDots] = useState<CoverageDot[]>([]);
  const [guidance, setGuidance] = useState('Point camera at the room');

  const scanTimer = useRef<ReturnType<typeof setInterval> | null>(null);
  const guidanceTimer = useRef<ReturnType<typeof setInterval> | null>(null);
  const pulseAnim = useRef(new Animated.Value(1)).current;
  const coverageAnim = useRef(new Animated.Value(0)).current;

  const GUIDANCES = [
    'Point camera at the center of the room',
    'Move slowly to the left wall',
    'Pan across to the right wall',
    'Tilt up to capture the ceiling',
    'Tilt down to capture the floor',
    'Move toward the far corner',
    'Hold steady — capturing frames',
    'Almost done — fill remaining gaps',
    'Coverage complete!',
  ];

  useEffect(() => {
    if (phase === 'scanning') {
      const pulse = Animated.loop(
        Animated.sequence([
          Animated.timing(pulseAnim, { toValue: 1.2, duration: 600, useNativeDriver: true }),
          Animated.timing(pulseAnim, { toValue: 1, duration: 600, useNativeDriver: true }),
        ])
      );
      pulse.start();
      return () => pulse.stop();
    }
  }, [phase, pulseAnim]);

  const startScan = () => {
    if (!selectedRoom) {
      Alert.alert('Select room type', 'Choose a room type to scan.');
      return;
    }
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
      // Simulate coverage growth (slows as it approaches 100%)
      const increment = Math.max(0.5, (100 - cov) * 0.08);
      cov = Math.min(100, cov + increment);

      // Add coverage dots
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
      setCoverageDots(prev => [...prev.slice(-80), ...newDots]);

      // Update guidance
      if (cov > guidIdx * 12 + 10 && guidIdx < GUIDANCES.length - 1) {
        guidIdx++;
        setGuidance(GUIDANCES[guidIdx]);
      }

      // Auto-complete at 95%
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

  useEffect(() => {
    if (phase === 'processing') {
      // Upload scan data
      const upload = async () => {
        try {
          let token = '';
          try { token = localStorage.getItem('dorja_token') || ''; } catch {}
          if (!token) {
            await fetch(API_URL + '/v1/auth/otp/start', {
              method: 'POST', headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ phone: '+8801700000001' }),
            });
            const vr = await fetch(API_URL + '/v1/auth/otp/verify', {
              method: 'POST', headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ phone: '+8801700000001', code: '123456' }),
            });
            const vd = await vr.json();
            token = vd.data?.accessToken || '';
          }

          if (token) {
            await fetch(API_URL + '/v1/scans', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
              body: JSON.stringify({
                listingId: listingId || null,
                roomType: selectedRoom,
                roomName: customName || ROOM_TYPES.find(r => r.type === selectedRoom)?.label,
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
      if (guidanceTimer.current) clearInterval(guidanceTimer.current);
    };
  }, []);

  // Phase: Select room type
  if (phase === 'select') {
    return (
      <View style={s.container}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Icon name="back" size={16} color="#007C78" />
          <Text style={s.backText}>Back</Text>
        </TouchableOpacity>

        <View style={s.centerContent}>
          <View style={s.iconWrap}>
            <Icon name="scan" size={40} color="#007C78" />
          </View>
          <Text style={s.title}>3D Room Scanner</Text>
          <Text style={s.sub}>
            Scan a room using your phone camera. The app captures spatial data to create a 3D walkthrough.
          </Text>

          <Text style={s.label}>Select room type to scan</Text>
          <View style={s.roomGrid}>
            {ROOM_TYPES.map(r => (
              <TouchableOpacity
                key={r.type}
                style={[s.roomBtn, selectedRoom === r.type && s.roomBtnActive]}
                onPress={() => { setSelectedRoom(r.type); setCustomName(r.label); }}
              >
                <View style={[s.roomColor, { backgroundColor: r.color }]} />
                <Text style={[s.roomBtnText, selectedRoom === r.type && s.roomBtnTextActive]}>{r.label}</Text>
              </TouchableOpacity>
            ))}
          </View>

          <TouchableOpacity
            style={[s.startBtn, !selectedRoom && s.startBtnDisabled]}
            onPress={startScan}
            disabled={!selectedRoom}
          >
            <Icon name="camera" size={16} color="white" />
            <Text style={s.startBtnText}>Start Scanning</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // Phase: Done
  if (phase === 'done') {
    return (
      <View style={s.container}>
        <View style={s.centerContent}>
          <View style={s.doneIcon}>
            <Icon name="check" size={40} color="#007C78" />
          </View>
          <Text style={s.title}>Scan Complete</Text>
          <Badge text={`${Math.round(coverage)}% COVERAGE`} />
          <View style={s.statsGrid}>
            <View style={s.statCard}>
              <Text style={s.statValue}>{frameCount}</Text>
              <Text style={s.statLabel}>Frames captured</Text>
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
            <Text style={s.startBtnText}>Done</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // Phase: Scanning or Processing
  return (
    <View style={s.scanContainer}>
      {/* Camera viewfinder */}
      <View style={s.viewfinder}>
        {/* Coverage dots */}
        {coverageDots.map((dot, i) => (
          <View
            key={i}
            style={[s.coverageDot, {
              left: dot.x,
              top: dot.y,
              opacity: dot.opacity,
            }]}
          />
        ))}

        {/* Scanning corners */}
        <View style={s.cornerTL} />
        <View style={s.cornerTR} />
        <View style={s.cornerBL} />
        <View style={s.cornerBR} />

        {/* Center crosshair */}
        <View style={s.crosshair}>
          <View style={s.crossH} />
          <View style={s.crossV} />
        </View>

        {/* Guidance text */}
        <View style={s.guidanceBar}>
          <Animated.View style={[s.recordDot, { transform: [{ scale: pulseAnim }] }]} />
          <Text style={s.guidanceText}>{guidance}</Text>
        </View>
      </View>

      {/* Stats overlay */}
      <View style={s.statsOverlay}>
        <View style={s.statBadge}>
          <Text style={s.statBadgeText}>{frameCount} frames</Text>
        </View>
        <View style={s.statBadge}>
          <Text style={s.statBadgeText}>{Math.round(coverage)}%</Text>
        </View>
        <View style={s.statBadge}>
          <Text style={s.statBadgeText}>{(duration / 1000).toFixed(1)}s</Text>
        </View>
      </View>

      {/* Room type badge */}
      <View style={s.roomBadge}>
        <Text style={s.roomBadgeText}>
          Scanning: {customName || ROOM_TYPES.find(r => r.type === selectedRoom)?.label}
        </Text>
      </View>

      {/* Coverage bar */}
      <View style={s.coverageBarContainer}>
        <View style={s.coverageBar}>
          <View style={[s.coverageFill, { width: `${coverage}%` } as any]} />
        </View>
        <Text style={s.coverageLabel}>{Math.round(coverage)}% coverage</Text>
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

      {phase === 'processing' && (
        <View style={s.processingOverlay}>
          <View style={s.processingCard}>
            <Animated.View style={{ transform: [{ rotate: coverageAnim.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] }) }] }}>
              <Icon name="scan" size={32} color="#007C78" />
            </Animated.View>
            <Text style={s.processingText}>Processing 3D scan...</Text>
            <Text style={s.processingSub}>{frameCount} frames · {Math.round(coverage)}% coverage</Text>
          </View>
        </View>
      )}
    </View>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2', padding: 20 },
  backBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, marginBottom: 20 },
  backText: { fontSize: 14, color: '#007C78', fontFamily: 'IBM Plex Sans' },
  centerContent: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 16 },
  iconWrap: { width: 80, height: 80, borderRadius: 40, backgroundColor: '#D7F1EE', alignItems: 'center', justifyContent: 'center' },
  title: { fontSize: 22, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk', textAlign: 'center' },
  sub: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans', textAlign: 'center', lineHeight: 20, maxWidth: 340 },
  label: { fontSize: 13, fontWeight: '600', color: '#17324D', fontFamily: 'IBM Plex Sans', marginTop: 16 },
  roomGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, justifyContent: 'center' },
  roomBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 14, paddingVertical: 10, borderRadius: 4, borderWidth: 1, borderColor: '#D9CCB9', backgroundColor: 'white' },
  roomBtnActive: { borderColor: '#007C78', backgroundColor: '#D7F1EE' },
  roomColor: { width: 12, height: 12, borderRadius: 6 },
  roomBtnText: { fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  roomBtnTextActive: { color: '#006B68', fontWeight: '600' },
  startBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, backgroundColor: '#007C78', padding: 14, borderRadius: 4, width: '100%', marginTop: 16 },
  startBtnDisabled: { backgroundColor: '#D9CCB9' },
  startBtnText: { color: 'white', fontSize: 14, fontWeight: '600', fontFamily: 'IBM Plex Sans' },

  // Scan view
  scanContainer: { flex: 1, backgroundColor: '#0B1F33' },
  viewfinder: { flex: 1, position: 'relative', overflow: 'hidden' },
  coverageDot: { position: 'absolute', width: 6, height: 6, borderRadius: 3, backgroundColor: '#007C78' },

  cornerTL: { position: 'absolute', top: 60, left: 20, width: 30, height: 30, borderTopWidth: 2, borderLeftWidth: 2, borderColor: '#007C78' },
  cornerTR: { position: 'absolute', top: 60, right: 20, width: 30, height: 30, borderTopWidth: 2, borderRightWidth: 2, borderColor: '#007C78' },
  cornerBL: { position: 'absolute', bottom: 200, left: 20, width: 30, height: 30, borderBottomWidth: 2, borderLeftWidth: 2, borderColor: '#007C78' },
  cornerBR: { position: 'absolute', bottom: 200, right: 20, width: 30, height: 30, borderBottomWidth: 2, borderRightWidth: 2, borderColor: '#007C78' },

  crosshair: { position: 'absolute', top: '45%', left: '50%', width: 40, height: 40, marginLeft: -20, marginTop: -20 },
  crossH: { position: 'absolute', top: '50%', left: 0, right: 0, height: 1, backgroundColor: 'rgba(0,124,120,0.5)' },
  crossV: { position: 'absolute', left: '50%', top: 0, bottom: 0, width: 1, backgroundColor: 'rgba(0,124,120,0.5)' },

  guidanceBar: { position: 'absolute', bottom: 180, left: 20, right: 20, flexDirection: 'row', alignItems: 'center', gap: 8, backgroundColor: 'rgba(0,0,0,0.6)', padding: 12, borderRadius: 4 },
  recordDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: '#007C78' },
  guidanceText: { flex: 1, color: 'white', fontSize: 13, fontFamily: 'IBM Plex Sans' },

  statsOverlay: { position: 'absolute', top: 50, left: 20, flexDirection: 'row', gap: 8 },
  statBadge: { backgroundColor: 'rgba(0,0,0,0.6)', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 2 },
  statBadgeText: { color: 'white', fontSize: 11, fontFamily: 'IBM Plex Mono' },

  roomBadge: { position: 'absolute', top: 50, right: 20, backgroundColor: '#007C78', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 2 },
  roomBadgeText: { color: 'white', fontSize: 11, fontWeight: '600', fontFamily: 'IBM Plex Sans' },

  coverageBarContainer: { position: 'absolute', bottom: 130, left: 20, right: 20 },
  coverageBar: { height: 4, backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: 2 },
  coverageFill: { height: 4, backgroundColor: '#007C78', borderRadius: 2 },
  coverageLabel: { color: 'rgba(255,255,255,0.6)', fontSize: 10, fontFamily: 'IBM Plex Mono', marginTop: 4, textAlign: 'center' },

  stopArea: { alignItems: 'center', paddingBottom: 40 },
  stopBtn: { width: 72, height: 72, borderRadius: 36, borderWidth: 3, borderColor: '#B91C1C', alignItems: 'center', justifyContent: 'center' },
  stopInner: { width: 56, height: 56, borderRadius: 28, backgroundColor: '#B91C1C' },
  stopLabel: { color: 'rgba(255,255,255,0.6)', fontSize: 12, fontFamily: 'IBM Plex Sans', marginTop: 12 },

  processingOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.7)', alignItems: 'center', justifyContent: 'center' },
  processingCard: { backgroundColor: 'white', borderRadius: 8, padding: 32, alignItems: 'center', gap: 12, width: 280 },
  processingText: { fontSize: 16, fontWeight: '600', color: '#0B1F33', fontFamily: 'Space Grotesk' },
  processingSub: { fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Mono' },

  // Done
  doneIcon: { width: 80, height: 80, borderRadius: 40, backgroundColor: '#D7F1EE', alignItems: 'center', justifyContent: 'center' },
  statsGrid: { flexDirection: 'row', gap: 12, marginTop: 16 },
  statCard: { alignItems: 'center', padding: 16, backgroundColor: 'white', borderRadius: 4, borderWidth: 1, borderColor: '#E8E0D0', minWidth: 80 },
  statValue: { fontSize: 20, fontWeight: '700', color: '#0B1F33', fontFamily: 'IBM Plex Mono' },
  statLabel: { fontSize: 11, color: '#17324D', fontFamily: 'IBM Plex Sans', marginTop: 4 },
});
