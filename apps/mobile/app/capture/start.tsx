import { useState, useRef, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Animated, Alert } from 'react-native';
import { router } from 'expo-router';
import { Icon, IconCircle } from '../../components/Icons';

const ROOM_SEQUENCE = [
  { type: 'LIVING_ROOM', label: 'Living Room', hint: 'Start with the main living area' },
  { type: 'BEDROOM', label: 'Bedroom', hint: 'Move to the bedroom' },
  { type: 'KITCHEN', label: 'Kitchen', hint: 'Capture the kitchen' },
  { type: 'BATHROOM', label: 'Bathroom', hint: 'Record the bathroom' },
  { type: 'BALCONY', label: 'Balcony', hint: 'If there is a balcony, capture it' },
];

export default function CaptureStartScreen() {
  const [phase, setPhase] = useState<'intro' | 'recording' | 'review' | 'done'>('intro');
  const [currentRoom, setCurrentRoom] = useState(0);
  const [holdProgress, setHoldProgress] = useState(0);
  const [capturedRooms, setCapturedRooms] = useState<string[]>([]);
  const holdTimer = useRef<ReturnType<typeof setInterval> | null>(null);
  const pulseAnim = useRef(new Animated.Value(1)).current;

  useEffect(() => {
    if (phase === 'recording') {
      const pulse = Animated.loop(
        Animated.sequence([
          Animated.timing(pulseAnim, { toValue: 1.3, duration: 800, useNativeDriver: true }),
          Animated.timing(pulseAnim, { toValue: 1, duration: 800, useNativeDriver: true }),
        ])
      );
      pulse.start();
      return () => pulse.stop();
    }
  }, [phase, pulseAnim]);

  const startHold = () => {
    setHoldProgress(0);
    let progress = 0;
    holdTimer.current = setInterval(() => {
      progress += 2;
      setHoldProgress(progress);
      if (progress >= 100) {
        if (holdTimer.current) clearInterval(holdTimer.current);
        captureRoom();
      }
    }, 60);
  };

  const stopHold = () => {
    if (holdTimer.current) {
      clearInterval(holdTimer.current);
      holdTimer.current = null;
    }
    setHoldProgress(0);
  };

  const captureRoom = () => {
    const room = ROOM_SEQUENCE[currentRoom];
    setCapturedRooms(prev => [...prev, room.type]);
    if (currentRoom < ROOM_SEQUENCE.length - 1) {
      setCurrentRoom(prev => prev + 1);
      setHoldProgress(0);
    } else {
      setPhase('review');
    }
  };

  const finishCapture = () => {
    setPhase('done');
    Alert.alert('Capture Complete', `${capturedRooms.length} rooms captured!`, [
      { text: 'OK', onPress: () => router.back() },
    ]);
  };

  const room = ROOM_SEQUENCE[currentRoom];

  if (phase === 'intro') {
    return (
      <View style={s.container}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Icon name="back" size={16} color="#007C78" />
          <Text style={s.backText}>Back</Text>
        </TouchableOpacity>

        <View style={s.centerContent}>
          <IconCircle name="camera" size={72} bgColor="#D7F1EE" iconColor="#007C78" iconSize={32} />
          <Text style={s.title}>Capture a Listing</Text>
          <Text style={s.sub}>
            Walk through each room and hold to record. The app guides you through each room type.
          </Text>

          <View style={s.steps}>
            {ROOM_SEQUENCE.map((r, i) => (
              <View key={i} style={s.stepRow}>
                <View style={s.stepNum}>
                  <Text style={s.stepNumText}>{i + 1}</Text>
                </View>
                <View style={s.stepInfo}>
                  <Text style={s.stepTitle}>{r.label}</Text>
                  <Text style={s.stepHint}>{r.hint}</Text>
                </View>
              </View>
            ))}
          </View>

          <TouchableOpacity style={s.startBtn} onPress={() => setPhase('recording')}>
            <Icon name="camera" size={16} color="white" />
            <Text style={s.startBtnText}>Start Capture</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  if (phase === 'review') {
    return (
      <View style={s.container}>
        <View style={s.centerContent}>
          <Icon name="check" size={48} color="#007C78" />
          <Text style={s.title}>Review Capture</Text>
          <Text style={s.sub}>{capturedRooms.length} rooms captured successfully</Text>

          <View style={s.reviewList}>
            {capturedRooms.map((rt, i) => (
              <View key={i} style={s.reviewItem}>
                <Icon name="check" size={14} color="#007C78" />
                <Text style={s.reviewText}>{rt.replace(/_/g, ' ')}</Text>
              </View>
            ))}
          </View>

          <TouchableOpacity style={s.startBtn} onPress={finishCapture}>
            <Text style={s.startBtnText}>Finish & Publish</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <View style={s.recordContainer}>
      {/* Top bar */}
      <View style={s.topBar}>
        <Text style={s.roomCounter}>{currentRoom + 1}/{ROOM_SEQUENCE.length}</Text>
        <Text style={s.roomTitle}>{room.label}</Text>
        <Text style={s.roomHint}>{room.hint}</Text>
      </View>

      {/* Camera viewfinder */}
      <View style={s.viewfinder}>
        <View style={s.viewfinderCorners}>
          <View style={[s.corner, s.cornerTL]} />
          <View style={[s.corner, s.cornerTR]} />
          <View style={[s.corner, s.cornerBL]} />
          <View style={[s.corner, s.cornerBR]} />
        </View>

        <Animated.View style={[s.recordDot, { transform: [{ scale: pulseAnim }] }]}>
          <Icon name="camera" size={32} color="white" />
        </Animated.View>

        <Text style={s.recordHint}>Hold the button to record this room</Text>
      </View>

      {/* Progress bar */}
      <View style={s.progressBar}>
        <View style={[s.progressFill, { width: `${holdProgress}%` } as any]} />
      </View>

      {/* Hold button */}
      <View style={s.holdArea}>
        <TouchableOpacity
          style={[s.holdBtn, holdProgress > 0 && s.holdBtnActive]}
          onPressIn={startHold}
          onPressOut={stopHold}
        >
          <View style={[s.holdBtnInner, holdProgress > 0 && s.holdBtnInnerActive]} />
        </TouchableOpacity>
        <Text style={s.holdLabel}>
          {holdProgress > 0 ? `Recording... ${Math.round(holdProgress)}%` : 'Hold to Record'}
        </Text>
      </View>

      {/* Room dots */}
      <View style={s.roomDots}>
        {ROOM_SEQUENCE.map((_, i) => (
          <View key={i} style={[s.roomDot, i < currentRoom && s.roomDotDone, i === currentRoom && s.roomDotActive]} />
        ))}
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2', padding: 20 },
  backBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, marginBottom: 20 },
  backText: { fontSize: 14, color: '#007C78', fontFamily: 'IBM Plex Sans' },
  centerContent: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 16 },
  title: { fontSize: 22, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk', textAlign: 'center' },
  sub: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans', textAlign: 'center', lineHeight: 20, maxWidth: 320 },

  steps: { width: '100%', gap: 10, marginTop: 16 },
  stepRow: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 12, backgroundColor: 'white', borderRadius: 4, borderWidth: 1, borderColor: '#E8E0D0' },
  stepNum: { width: 28, height: 28, borderRadius: 14, backgroundColor: '#D7F1EE', alignItems: 'center', justifyContent: 'center' },
  stepNumText: { fontSize: 12, fontWeight: '700', color: '#007C78', fontFamily: 'IBM Plex Mono' },
  stepInfo: { flex: 1 },
  stepTitle: { fontSize: 14, fontWeight: '600', color: '#0B1F33', fontFamily: 'IBM Plex Sans' },
  stepHint: { fontSize: 12, color: '#17324D', fontFamily: 'IBM Plex Sans', marginTop: 2 },

  startBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, backgroundColor: '#007C78', padding: 14, borderRadius: 4, width: '100%', marginTop: 16 },
  startBtnText: { color: 'white', fontSize: 14, fontWeight: '600', fontFamily: 'IBM Plex Sans' },

  // Recording view
  recordContainer: { flex: 1, backgroundColor: '#0B1F33' },
  topBar: { padding: 20, paddingTop: 60, alignItems: 'center' },
  roomCounter: { fontSize: 12, color: '#aaa', fontFamily: 'IBM Plex Mono', marginBottom: 4 },
  roomTitle: { fontSize: 20, fontWeight: '700', color: 'white', fontFamily: 'Space Grotesk' },
  roomHint: { fontSize: 13, color: '#aaa', fontFamily: 'IBM Plex Sans', marginTop: 4 },

  viewfinder: { flex: 1, margin: 20, borderRadius: 8, borderWidth: 1, borderColor: '#333', position: 'relative', alignItems: 'center', justifyContent: 'center' },
  viewfinderCorners: { ...StyleSheet.absoluteFillObject },
  corner: { position: 'absolute', width: 20, height: 20, borderColor: '#007C78' },
  cornerTL: { top: 0, left: 0, borderTopWidth: 2, borderLeftWidth: 2 },
  cornerTR: { top: 0, right: 0, borderTopWidth: 2, borderRightWidth: 2 },
  cornerBL: { bottom: 0, left: 0, borderBottomWidth: 2, borderLeftWidth: 2 },
  cornerBR: { bottom: 0, right: 0, borderBottomWidth: 2, borderRightWidth: 2 },
  recordDot: { width: 64, height: 64, borderRadius: 32, backgroundColor: 'rgba(0,124,120,0.3)', alignItems: 'center', justifyContent: 'center' },
  recordHint: { position: 'absolute', bottom: 20, color: '#aaa', fontSize: 12, fontFamily: 'IBM Plex Sans' },

  progressBar: { height: 4, backgroundColor: '#333', marginHorizontal: 20 },
  progressFill: { height: 4, backgroundColor: '#007C78', borderRadius: 2 },

  holdArea: { alignItems: 'center', padding: 32 },
  holdBtn: { width: 80, height: 80, borderRadius: 40, borderWidth: 3, borderColor: '#555', alignItems: 'center', justifyContent: 'center' },
  holdBtnActive: { borderColor: '#007C78' },
  holdBtnInner: { width: 56, height: 56, borderRadius: 28, backgroundColor: '#555' },
  holdBtnInnerActive: { backgroundColor: '#007C78' },
  holdLabel: { color: '#aaa', fontSize: 12, fontFamily: 'IBM Plex Mono', marginTop: 12 },

  roomDots: { flexDirection: 'row', justifyContent: 'center', gap: 8, paddingBottom: 40 },
  roomDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#333' },
  roomDotDone: { backgroundColor: '#007C78' },
  roomDotActive: { backgroundColor: '#007C78', width: 24 },

  // Review
  reviewList: { width: '100%', gap: 8, marginTop: 16 },
  reviewItem: { flexDirection: 'row', alignItems: 'center', gap: 8, padding: 12, backgroundColor: 'white', borderRadius: 4, borderWidth: 1, borderColor: '#D7F1EE' },
  reviewText: { fontSize: 14, color: '#0B1F33', fontFamily: 'IBM Plex Sans', textTransform: 'capitalize' },
});
