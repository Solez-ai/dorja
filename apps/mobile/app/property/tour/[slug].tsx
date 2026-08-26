import { useState, useEffect, useRef, useCallback } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Dimensions,
  Animated,
  PanResponder,
  StatusBar,
  Easing,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useLocalSearchParams, router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { API_URL } from '../../../config';

const { width: SCREEN_W, height: SCREEN_H } = Dimensions.get('window');
const JOYSTICK_SIZE = 120;
const JOYSTICK_KNOB = 50;

interface RoomData {
  id: string;
  roomType: string;
  displayName: string;
  ordinal: number;
}

interface Listing {
  title: string;
  rooms: RoomData[];
}

const ROOM_COLORS: Record<
  string,
  { wall: string; floor: string; ceiling: string; accent: string; sky: string }
> = {
  LIVING_ROOM: { wall: '#E8E0D0', floor: '#C4B5A0', ceiling: '#FBF8F2', accent: '#D7F1EE', sky: '#B8D8E8' },
  BEDROOM: { wall: '#E0D8CE', floor: '#BFB09C', ceiling: '#F5F0E8', accent: '#D7F1EE', sky: '#C4D8E8' },
  KITCHEN: { wall: '#E5DDD0', floor: '#C0B4A0', ceiling: '#FAF7F0', accent: '#D7F1EE', sky: '#B8D0E0' },
  BATHROOM: { wall: '#D8E5E4', floor: '#A0B8B6', ceiling: '#F0F7F6', accent: '#007C78', sky: '#A8C8D8' },
  BALCONY: { wall: '#E8E5DE', floor: '#C8C0B0', ceiling: '#FFFFFF', accent: '#87CEEB', sky: '#87CEEB' },
  DINING_ROOM: { wall: '#E2DCD2', floor: '#BEB4A4', ceiling: '#F8F5EE', accent: '#D7F1EE', sky: '#B0C8D8' },
};

const ROOM_DOORS: Record<string, string> = {
  LIVING_ROOM: 'Bedroom',
  BEDROOM: 'Living Room',
  KITCHEN: 'Living Room',
  BATHROOM: 'Bedroom',
  BALCONY: 'Living Room',
  DINING_ROOM: 'Living Room',
};

export default function TourScreen() {
  const insets = useSafeAreaInsets();
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const [listing, setListing] = useState<Listing | null>(null);
  const [currentRoom, setCurrentRoom] = useState(0);
  const [rotation, setRotation] = useState({ x: 0, y: 0 });
  const [showHint, setShowHint] = useState(true);

  // Transition animations
  const [transitioning, setTransitioning] = useState(false);
  const canvasOpacity = useRef(new Animated.Value(1)).current;
  const canvasScale = useRef(new Animated.Value(1)).current;
  const canvasTranslateX = useRef(new Animated.Value(0)).current;
  const doorwayOverlay = useRef(new Animated.Value(0)).current;
  const roomLabelOpacity = useRef(new Animated.Value(1)).current;

  const knobAnim = useRef(new Animated.ValueXY({ x: 0, y: 0 })).current;
  const animRef = useRef({ x: 0, y: 0 });

  // Hide system bars for immersive experience
  useEffect(() => {
    StatusBar.setHidden(true, 'fade');
    return () => {
      StatusBar.setHidden(false, 'fade');
    };
  }, []);

  useEffect(() => {
    if (!slug) return;
    fetch(`${API_URL}/v1/listings/${slug}`)
      .then((r) => r.json())
      .then((d) => {
        if (d.data) setListing(d.data);
      })
      .catch(() => {});
  }, [slug]);

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => !transitioning,
      onMoveShouldSetPanResponder: () => !transitioning,
      onPanResponderGrant: () => {
        knobAnim.setOffset({ x: animRef.current.x, y: animRef.current.y });
        knobAnim.setValue({ x: 0, y: 0 });
      },
      onPanResponderMove: (_, gestureState) => {
        const maxDist = (JOYSTICK_SIZE - JOYSTICK_KNOB) / 2;
        let dx = gestureState.dx;
        let dy = gestureState.dy;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > maxDist) {
          dx = (dx / dist) * maxDist;
          dy = (dy / dist) * maxDist;
        }
        knobAnim.setValue({ x: dx, y: dy });
        const rotX = (dy / maxDist) * -30;
        const rotY = (dx / maxDist) * 60;
        setRotation({ x: rotX, y: rotY });
      },
      onPanResponderRelease: () => {
        knobAnim.flattenOffset();
        Animated.spring(knobAnim, {
          toValue: { x: 0, y: 0 },
          useNativeDriver: false,
        }).start();
        setRotation({ x: 0, y: 0 });
      },
    })
  ).current;

  /**
   * Smooth room transition with 3 phases:
   * 1. Fade out current room + slide canvas toward doorway
   * 2. Show doorway overlay (brief black flash simulating walking through)
   * 3. Fade in new room from the other side
   */
  const goToRoom = useCallback(
    (idx: number) => {
      if (!listing || idx === currentRoom || transitioning) return;
      setTransitioning(true);
      setShowHint(false);

      // Phase 1: Fade out + slide canvas to the right (as if walking forward)
      Animated.parallel([
        Animated.timing(canvasOpacity, {
          toValue: 0,
          duration: 250,
          easing: Easing.in(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(canvasScale, {
          toValue: 1.15,
          duration: 250,
          easing: Easing.in(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(roomLabelOpacity, {
          toValue: 0,
          duration: 150,
          useNativeDriver: true,
        }),
      ]).start(() => {
        // Phase 2: Show doorway overlay briefly
        setCurrentRoom(idx);
        setRotation({ x: 0, y: 0 });

        Animated.sequence([
          // Doorway zoom-in overlay
          Animated.timing(doorwayOverlay, {
            toValue: 1,
            duration: 200,
            easing: Easing.in(Easing.ease),
            useNativeDriver: true,
          }),
          // Brief hold (0ms — just the timing function makes it feel natural)
        ]).start(() => {
          // Phase 3: Fade in new room with slight zoom-out
          canvasScale.setValue(0.9);
          canvasTranslateX.setValue(-40);

          Animated.parallel([
            Animated.timing(canvasOpacity, {
              toValue: 1,
              duration: 300,
              easing: Easing.out(Easing.ease),
              useNativeDriver: true,
            }),
            Animated.timing(canvasScale, {
              toValue: 1,
              duration: 350,
              easing: Easing.out(Easing.ease),
              useNativeDriver: true,
            }),
            Animated.timing(canvasTranslateX, {
              toValue: 0,
              duration: 350,
              easing: Easing.out(Easing.ease),
              useNativeDriver: true,
            }),
            Animated.timing(doorwayOverlay, {
              toValue: 0,
              duration: 250,
              easing: Easing.out(Easing.ease),
              useNativeDriver: true,
            }),
            Animated.timing(roomLabelOpacity, {
              toValue: 1,
              duration: 300,
              delay: 150,
              useNativeDriver: true,
            }),
          ]).start(() => {
            setTransitioning(false);
          });
        });
      });
    },
    [listing, currentRoom, transitioning, canvasOpacity, canvasScale, canvasTranslateX, doorwayOverlay, roomLabelOpacity]
  );

  if (!listing || !listing.rooms.length) {
    return (
      <View style={styles.center}>
        <Ionicons name="cube" size={24} color="#007C78" />
        <Text style={styles.loadingText}>Loading tour...</Text>
      </View>
    );
  }

  const rooms = listing.rooms.sort((a, b) => a.ordinal - b.ordinal);
  const room = rooms[currentRoom];
  const colors = ROOM_COLORS[room.roomType] || ROOM_COLORS.LIVING_ROOM;
  const nextDoor = ROOM_DOORS[room.roomType] || 'Room';
  const nextRoomIdx = rooms.findIndex(
    (r, i) =>
      i !== currentRoom &&
      r.displayName.toLowerCase().includes(nextDoor.toLowerCase())
  );
  const doorTarget = nextRoomIdx >= 0 ? nextRoomIdx : (currentRoom + 1) % rooms.length;

  const perspectiveTransform = {
    transform: [
      { perspective: 800 },
      { rotateX: `${rotation.x}deg` },
      { rotateY: `${rotation.y}deg` },
    ],
  };

  return (
    <View style={styles.container}>
      {/* Room header bar */}
      <View style={[styles.roomBar, { paddingTop: insets.top + 8 }]}>
        <TouchableOpacity
          onPress={() => router.back()}
          style={styles.closeBtn}
          disabled={transitioning}
        >
          <Ionicons name="close" size={22} color="white" />
        </TouchableOpacity>
        <View style={styles.roomBarInfo}>
          <Animated.Text style={[styles.roomBarTitle, { opacity: roomLabelOpacity }]}>
            {room.displayName}
          </Animated.Text>
          <Text style={styles.roomBarSub}>{listing.title}</Text>
        </View>
        <View style={styles.roomCounter}>
          <Text style={styles.roomCounterText}>
            {currentRoom + 1}/{rooms.length}
          </Text>
        </View>
      </View>

      {/* 3D Room Canvas (animated) */}
      <Animated.View
        style={[
          styles.canvas,
          perspectiveTransform,
          {
            opacity: canvasOpacity,
            transform: [
              { perspective: 800 },
              { rotateX: `${rotation.x}deg` },
              { rotateY: `${rotation.y}deg` },
              { scale: canvasScale },
              { translateX: canvasTranslateX },
            ],
          },
        ]}
        {...panResponder.panHandlers}
      >
        {/* Sky/ceiling gradient */}
        <View style={[styles.ceiling, { backgroundColor: colors.ceiling }]}>
          <View style={[styles.skyGradient, { backgroundColor: colors.sky, opacity: 0.3 }]} />
        </View>

        {/* Floor with perspective */}
        <View style={[styles.floor, { backgroundColor: colors.floor }]}>
          <View style={styles.floorGrid}>
            {Array.from({ length: 8 }).map((_, i) => (
              <View
                key={i}
                style={[styles.floorLine, { left: `${i * 14}%` }]}
              />
            ))}
            {Array.from({ length: 4 }).map((_, i) => (
              <View
                key={`h${i}`}
                style={[styles.floorLineH, { top: `${i * 30}%` }]}
              />
            ))}
          </View>
        </View>

        {/* Back wall */}
        <View style={[styles.backWall, { backgroundColor: colors.wall }]}>
          {/* Window */}
          <View style={styles.window}>
            <View style={[styles.windowPane, { backgroundColor: colors.sky }]} />
            <View style={styles.windowDividerV} />
            <View style={styles.windowDividerH} />
            <View style={styles.windowFrame} />
          </View>

          {/* Room info overlay */}
          <View style={styles.roomOverlay}>
            <Text style={styles.roomOverlayName}>{room.displayName}</Text>
            <Text style={styles.roomOverlayType}>
              {room.roomType.replace(/_/g, ' ')}
            </Text>
          </View>
        </View>

        {/* Left wall */}
        <View
          style={[styles.leftWall, { backgroundColor: colors.wall, opacity: 0.6 }]}
        />
        {/* Right wall */}
        <View
          style={[styles.rightWall, { backgroundColor: colors.wall, opacity: 0.6 }]}
        />

        {/* Accent circle */}
        <View
          style={[
            styles.accentCircle,
            { backgroundColor: colors.accent },
          ]}
        />

        {/* Doorway hotspot */}
        <TouchableOpacity
          style={styles.doorway}
          activeOpacity={0.7}
          onPress={() => goToRoom(doorTarget)}
          disabled={transitioning}
        >
          <View style={styles.doorFrame}>
            <Ionicons name="enter" size={24} color="#D7F1EE" />
          </View>
          <Text style={styles.doorLabel}>To {nextDoor}</Text>
        </TouchableOpacity>
      </Animated.View>

      {/* Doorway transition overlay — black vignette that flashes during room switch */}
      <Animated.View
        style={[
          styles.doorwayTransition,
          {
            opacity: doorwayOverlay,
          },
        ]}
        pointerEvents="none"
      >
        {/* Central doorway shape */}
        <View style={styles.doorwayShape}>
          <Ionicons name="enter" size={48} color="#007C78" />
        </View>
      </Animated.View>

      {/* Room tabs */}
      <View style={styles.tabsRow}>
        <View style={styles.tabsScroll}>
          {rooms.map((r, idx) => (
            <TouchableOpacity
              key={r.id}
              style={[
                styles.tab,
                idx === currentRoom && styles.tabActive,
                transitioning && styles.tabDisabled,
              ]}
              activeOpacity={0.7}
              onPress={() => goToRoom(idx)}
              disabled={transitioning}
            >
              <Ionicons
                name="enter"
                size={12}
                color={idx === currentRoom ? 'white' : '#17324D'}
              />
              <Text
                style={[
                  styles.tabText,
                  idx === currentRoom && styles.tabTextActive,
                ]}
              >
                {r.displayName}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Virtual Joystick */}
      <View style={styles.joystickContainer}>
        <View style={styles.joystickBase}>
          <Animated.View
            style={[
              styles.joystickKnob,
              {
                transform: [
                  { translateX: Animated.add(knobAnim.x, 0) },
                  { translateY: Animated.add(knobAnim.y, 0) },
                ],
              },
            ]}
            {...(transitioning ? {} : panResponder.panHandlers)}
          />
          <View style={[styles.dirHint, { top: 6 }]}>
            <Ionicons name="chevron-up" size={12} color="rgba(255,255,255,0.4)" />
          </View>
          <View style={[styles.dirHint, { bottom: 6 }]}>
            <Ionicons name="chevron-down" size={12} color="rgba(255,255,255,0.4)" />
          </View>
          <View style={[styles.dirHint, { left: 6 }]}>
            <Ionicons name="chevron-back" size={12} color="rgba(255,255,255,0.4)" />
          </View>
          <View style={[styles.dirHint, { right: 6 }]}>
            <Ionicons name="chevron-forward" size={12} color="rgba(255,255,255,0.4)" />
          </View>
        </View>
        <Text style={styles.joystickLabel}>LOOK AROUND</Text>
      </View>

      {/* Quick nav buttons (right side) */}
      {doorTarget !== currentRoom && !transitioning && (
        <TouchableOpacity
          style={[styles.quickNav, { bottom: 80 + insets.bottom }]}
          activeOpacity={0.7}
          onPress={() => goToRoom(doorTarget)}
        >
          <Ionicons name="enter" size={16} color="white" />
          <Text style={styles.quickNavText}>To {nextDoor}</Text>
        </TouchableOpacity>
      )}

      {/* Control hint */}
      {showHint && !transitioning && (
        <View style={styles.hintOverlay}>
          <View style={styles.hintCard}>
            <Ionicons name="cube" size={32} color="#007C78" />
            <Text style={styles.hintTitle}>3D Tour Controls</Text>
            <View style={styles.hintRow}>
              <Ionicons name="move" size={16} color="#007C78" />
              <Text style={styles.hintText}>
                Drag the joystick to look around
              </Text>
            </View>
            <View style={styles.hintRow}>
              <Ionicons name="enter" size={16} color="#007C78" />
              <Text style={styles.hintText}>
                Tap doors or room tabs to walk
              </Text>
            </View>
            <TouchableOpacity
              style={styles.hintDismiss}
              activeOpacity={0.7}
              onPress={() => setShowHint(false)}
            >
              <Text style={styles.hintDismissText}>Got it</Text>
            </TouchableOpacity>
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0B1F33' },
  center: {
    flex: 1,
    backgroundColor: '#0B1F33',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  loadingText: { color: '#aaa', fontSize: 14, fontFamily: 'IBM Plex Sans' },

  // Room bar
  roomBar: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    backgroundColor: 'rgba(0,0,0,0.7)',
    zIndex: 10,
  },
  closeBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(255,255,255,0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  roomBarInfo: { flex: 1, marginLeft: 12 },
  roomBarTitle: { color: 'white', fontSize: 16, fontWeight: '700', fontFamily: 'Space Grotesk' },
  roomBarSub: { color: '#aaa', fontSize: 11, fontFamily: 'IBM Plex Sans', marginTop: 2 },
  roomCounter: {
    backgroundColor: 'rgba(255,255,255,0.15)',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 6,
  },
  roomCounterText: { color: 'white', fontSize: 13, fontFamily: 'IBM Plex Mono' },

  // 3D Canvas
  canvas: { flex: 1, position: 'relative', overflow: 'hidden' },

  ceiling: { position: 'absolute', top: 0, left: 0, right: 0, height: '18%' },
  skyGradient: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 },

  floor: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: '38%',
    transform: [{ perspective: 400 }, { rotateX: '50deg' }],
    transformOrigin: 'bottom',
  },
  floorGrid: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 },
  floorLine: { position: 'absolute', top: 0, bottom: 0, width: 1, backgroundColor: 'rgba(0,0,0,0.06)' },
  floorLineH: { position: 'absolute', left: 0, right: 0, height: 1, backgroundColor: 'rgba(0,0,0,0.04)' },

  backWall: {
    position: 'absolute',
    top: '16%',
    left: '10%',
    right: '10%',
    height: '48%',
    borderRadius: 2,
  },
  leftWall: { position: 'absolute', top: '16%', left: 0, width: '12%', height: '48%' },
  rightWall: { position: 'absolute', top: '16%', right: 0, width: '12%', height: '48%' },

  window: {
    position: 'absolute',
    top: '12%',
    left: '28%',
    right: '28%',
    height: '44%',
    backgroundColor: '#D4E8F0',
    borderWidth: 2,
    borderColor: '#8B7355',
    borderRadius: 3,
    overflow: 'hidden',
  },
  windowPane: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 },
  windowDividerV: { position: 'absolute', top: 0, bottom: 0, left: '50%', width: 2, backgroundColor: '#8B7355' },
  windowDividerH: { position: 'absolute', top: '50%', left: 0, right: 0, height: 2, backgroundColor: '#8B7355' },
  windowFrame: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, borderWidth: 2, borderColor: '#8B7355', borderRadius: 3 },

  roomOverlay: { position: 'absolute', bottom: 16, left: 16 },
  roomOverlayName: { color: '#0B1F33', fontSize: 15, fontWeight: '700', fontFamily: 'Space Grotesk' },
  roomOverlayType: { color: '#17324D', fontSize: 11, fontFamily: 'IBM Plex Mono', textTransform: 'uppercase', marginTop: 3, letterSpacing: 1 },

  doorway: {
    position: 'absolute',
    bottom: '32%',
    right: '14%',
    width: 64,
    height: 110,
    backgroundColor: '#5C4A36',
    borderRadius: 6,
    borderWidth: 3,
    borderColor: '#3E2E1E',
    alignItems: 'center',
    justifyContent: 'center',
  },
  doorFrame: { marginBottom: 4 },
  doorLabel: { color: '#D7F1EE', fontSize: 10, fontWeight: '600', fontFamily: 'IBM Plex Sans', textAlign: 'center' },

  accentCircle: {
    position: 'absolute',
    top: '22%',
    left: '15%',
    width: 44,
    height: 44,
    borderRadius: 22,
    opacity: 0.4,
  },

  // Doorway transition overlay
  doorwayTransition: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#0B1F33',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 5,
  },
  doorwayShape: {
    width: 80,
    height: 120,
    backgroundColor: '#1A1A1A',
    borderRadius: 8,
    borderWidth: 2,
    borderColor: '#007C78',
    alignItems: 'center',
    justifyContent: 'center',
  },

  // Room tabs
  tabsRow: { backgroundColor: '#111', paddingVertical: 8, borderTopWidth: 1, borderTopColor: '#222', zIndex: 10 },
  tabsScroll: { flexDirection: 'row', paddingHorizontal: 12, gap: 6 },
  tab: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#333',
    backgroundColor: 'transparent',
  },
  tabActive: { backgroundColor: '#007C78', borderColor: '#007C78' },
  tabDisabled: { opacity: 0.5 },
  tabText: { color: '#aaa', fontSize: 11, fontFamily: 'IBM Plex Sans' },
  tabTextActive: { color: 'white', fontWeight: '600' },

  // Joystick
  joystickContainer: { position: 'absolute', bottom: 100, left: 24, alignItems: 'center', zIndex: 10 },
  joystickBase: {
    width: JOYSTICK_SIZE,
    height: JOYSTICK_SIZE,
    borderRadius: JOYSTICK_SIZE / 2,
    backgroundColor: 'rgba(255,255,255,0.08)',
    borderWidth: 2,
    borderColor: 'rgba(255,255,255,0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  joystickKnob: {
    width: JOYSTICK_KNOB,
    height: JOYSTICK_KNOB,
    borderRadius: JOYSTICK_KNOB / 2,
    backgroundColor: 'rgba(0, 124, 120, 0.85)',
    borderWidth: 2,
    borderColor: 'rgba(0, 124, 120, 1)',
    shadowColor: '#007C78',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.4,
    shadowRadius: 6,
    elevation: 4,
  },
  dirHint: { position: 'absolute' },
  joystickLabel: {
    color: 'rgba(255,255,255,0.35)',
    fontSize: 9,
    fontFamily: 'IBM Plex Mono',
    letterSpacing: 1.5,
    marginTop: 8,
    textTransform: 'uppercase',
  },

  // Quick nav
  quickNav: {
    position: 'absolute',
    right: 20,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(0, 124, 120, 0.8)',
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 20,
    zIndex: 10,
  },
  quickNavText: { color: 'white', fontSize: 12, fontWeight: '600', fontFamily: 'IBM Plex Sans' },

  // Hint overlay
  hintOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.6)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
    zIndex: 20,
  },
  hintCard: {
    backgroundColor: 'white',
    borderRadius: 16,
    padding: 28,
    width: '100%',
    alignItems: 'center',
  },
  hintTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#0B1F33',
    fontFamily: 'Space Grotesk',
    marginTop: 12,
    marginBottom: 20,
  },
  hintRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 14,
    alignSelf: 'flex-start',
  },
  hintText: { flex: 1, fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  hintDismiss: {
    backgroundColor: '#007C78',
    paddingVertical: 14,
    paddingHorizontal: 32,
    borderRadius: 10,
    alignItems: 'center',
    marginTop: 20,
    width: '100%',
  },
  hintDismissText: { color: 'white', fontSize: 15, fontWeight: '600', fontFamily: 'IBM Plex Sans' },
});
