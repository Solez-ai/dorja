import { useState, useEffect, useRef, useCallback } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Dimensions, Animated, PanResponder } from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import { Icon } from '../../../components/Icons';

const API_URL = 'http://localhost:4000';
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

// Room colors by type
const ROOM_COLORS: Record<string, { wall: string; floor: string; ceiling: string; accent: string }> = {
  LIVING_ROOM: { wall: '#E8E0D0', floor: '#C4B5A0', ceiling: '#FBF8F2', accent: '#D7F1EE' },
  BEDROOM: { wall: '#E0D8CE', floor: '#BFB09C', ceiling: '#F5F0E8', accent: '#D7F1EE' },
  KITCHEN: { wall: '#E5DDD0', floor: '#C0B4A0', ceiling: '#FAF7F0', accent: '#D7F1EE' },
  BATHROOM: { wall: '#D8E5E4', floor: '#A0B8B6', ceiling: '#F0F7F6', accent: '#007C78' },
  BALCONY: { wall: '#E8E5DE', floor: '#C8C0B0', ceiling: '#FFFFFF', accent: '#87CEEB' },
  DINING_ROOM: { wall: '#E2DCD2', floor: '#BEB4A4', ceiling: '#F8F5EE', accent: '#D7F1EE' },
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
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const [listing, setListing] = useState<Listing | null>(null);
  const [currentRoom, setCurrentRoom] = useState(0);
  const [rotation, setRotation] = useState({ x: 0, y: 0 }); // perspective rotation
  const [showHint, setShowHint] = useState(true);
  const knobAnim = useRef(new Animated.ValueXY({ x: 0, y: 0 })).current;
  const animRef = useRef({ x: 0, y: 0 });

  useEffect(() => {
    if (!slug) return;
    fetch(API_URL + '/v1/listings/' + slug)
      .then(r => r.json())
      .then(d => { if (d.data) setListing(d.data); })
      .catch(() => {});
  }, [slug]);

  // Joystick pan responder
  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: () => {
        knobAnim.setOffset({ x: animRef.current.x, y: animRef.current.y });
        knobAnim.setValue({ x: 0, y: 0 });
      },
      onPanResponderMove: (_, gestureState) => {
        // Clamp to joystick bounds
        const maxDist = (JOYSTICK_SIZE - JOYSTICK_KNOB) / 2;
        let dx = gestureState.dx;
        let dy = gestureState.dy;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > maxDist) {
          dx = (dx / dist) * maxDist;
          dy = (dy / dist) * maxDist;
        }
        knobAnim.setValue({ x: dx, y: dy });

        // Update rotation based on joystick position
        const rotX = (dy / maxDist) * -30; // up/down pitch
        const rotY = (dx / maxDist) * 60; // left/right yaw
        setRotation({ x: rotX, y: rotY });
      },
      onPanResponderRelease: () => {
        knobAnim.flattenOffset();
        Animated.spring(knobAnim, { toValue: { x: 0, y: 0 }, useNativeDriver: false }).start();
        setRotation({ x: 0, y: 0 });
      },
    })
  ).current;

  // Navigate to next room
  const goToRoom = useCallback((idx: number) => {
    if (!listing) return;
    setCurrentRoom(idx);
    setShowHint(false);
  }, [listing]);

  if (!listing || !listing.rooms.length) {
    return (
      <View style={styles.center}>
        <Text style={styles.loadingText}>Loading tour...</Text>
      </View>
    );
  }

  const rooms = listing.rooms.sort((a, b) => a.ordinal - b.ordinal);
  const room = rooms[currentRoom];
  const colors = ROOM_COLORS[room.roomType] || ROOM_COLORS.LIVING_ROOM;
  const nextDoor = ROOM_DOORS[room.roomType] || 'Room';

  // Find next room index
  const nextRoomIdx = rooms.findIndex((r, i) => i !== currentRoom && r.displayName.toLowerCase().includes(nextDoor.toLowerCase()));
  const doorTarget = nextRoomIdx >= 0 ? nextRoomIdx : (currentRoom + 1) % rooms.length;

  // Compute 3D perspective transform
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
      <View style={styles.roomBar}>
        <TouchableOpacity onPress={() => router.back()} style={styles.closeBtn}>
          <Icon name="close" size={20} color="white" />
        </TouchableOpacity>
        <View style={styles.roomBarInfo}>
          <Text style={styles.roomBarTitle}>{room.displayName}</Text>
          <Text style={styles.roomBarSub}>{listing.title}</Text>
        </View>
        <View style={styles.roomCounter}>
          <Text style={styles.roomCounterText}>{currentRoom + 1}/{rooms.length}</Text>
        </View>
      </View>

      {/* 3D Room Canvas */}
      <View style={[styles.canvas, perspectiveTransform]}>
        {/* Floor */}
        <View style={[styles.floor, { backgroundColor: colors.floor }]}>
          <View style={styles.floorGrid}>
            {Array.from({ length: 6 }).map((_, i) => (
              <View key={i} style={[styles.floorLine, { left: `${i * 20}%` }]} />
            ))}
          </View>
        </View>

        {/* Back wall */}
        <View style={[styles.backWall, { backgroundColor: colors.wall }]}>
          {/* Window */}
          <View style={styles.window}>
            <View style={styles.windowPane} />
            <View style={styles.windowDivider} />
            <View style={styles.windowFrame} />
          </View>

          {/* Room info overlay */}
          <View style={styles.roomOverlay}>
            <Text style={styles.roomOverlayName}>{room.displayName}</Text>
            <Text style={styles.roomOverlayType}>{room.roomType.replace(/_/g, ' ')}</Text>
          </View>
        </View>

        {/* Left wall */}
        <View style={[styles.leftWall, { backgroundColor: colors.wall, opacity: 0.7 }]} />

        {/* Right wall */}
        <View style={[styles.rightWall, { backgroundColor: colors.wall, opacity: 0.7 }]} />

        {/* Ceiling */}
        <View style={[styles.ceiling, { backgroundColor: colors.ceiling }]} />

        {/* Doorway hotspot */}
        <TouchableOpacity style={styles.doorway} onPress={() => goToRoom(doorTarget)}>
          <View style={styles.doorFrame}>
            <Icon name="door" size={24} color="#5C4A36" />
          </View>
          <Text style={styles.doorLabel}>To {nextDoor}</Text>
        </TouchableOpacity>

        {/* Accent element */}
        <View style={[styles.accentCircle, { backgroundColor: colors.accent }]} />
      </View>

      {/* Room tabs */}
      <View style={styles.tabsRow}>
        <View style={styles.tabsScroll}>
          {rooms.map((r, idx) => (
            <TouchableOpacity
              key={r.id}
              style={[styles.tab, idx === currentRoom && styles.tabActive]}
              onPress={() => goToRoom(idx)}
            >
              <Icon name="door" size={12} color={idx === currentRoom ? 'white' : '#17324D'} />
              <Text style={[styles.tabText, idx === currentRoom && styles.tabTextActive]}>
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
            style={[styles.joystickKnob, {
              transform: [
                { translateX: Animated.add(knobAnim.x, 0) },
                { translateY: Animated.add(knobAnim.y, 0) },
              ],
            }]}
            {...panResponder.panHandlers}
          />
          {/* Direction hints */}
          <View style={[styles.dirHint, { top: 4 }]}>
            <Icon name="up" size={10} color="rgba(255,255,255,0.4)" />
          </View>
          <View style={[styles.dirHint, { bottom: 4 }]}>
            <Icon name="down" size={10} color="rgba(255,255,255,0.4)" />
          </View>
          <View style={[styles.dirHint, { left: 4 }]}>
            <Icon name="back" size={10} color="rgba(255,255,255,0.4)" />
          </View>
          <View style={[styles.dirHint, { right: 4 }]}>
            <Icon name="arrowRight" size={10} color="rgba(255,255,255,0.4)" />
          </View>
        </View>
        <Text style={styles.joystickLabel}>LOOK AROUND</Text>
      </View>

      {/* Control hint */}
      {showHint && (
        <View style={styles.hintOverlay}>
          <View style={styles.hintCard}>
            <Text style={styles.hintTitle}>3D Tour Controls</Text>
            <View style={styles.hintRow}>
              <Icon name="eye" size={14} color="#007C78" />
              <Text style={styles.hintText}>Drag the joystick to look around</Text>
            </View>
            <View style={styles.hintRow}>
              <Icon name="door" size={14} color="#007C78" />
              <Text style={styles.hintText}>Tap doors to walk between rooms</Text>
            </View>
            <TouchableOpacity style={styles.hintDismiss} onPress={() => setShowHint(false)}>
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
  center: { flex: 1, backgroundColor: '#0B1F33', alignItems: 'center', justifyContent: 'center' },
  loadingText: { color: '#aaa', fontSize: 14, fontFamily: 'IBM Plex Sans' },

  // Room bar
  roomBar: { flexDirection: 'row', alignItems: 'center', padding: 12, paddingTop: 48, backgroundColor: 'rgba(0,0,0,0.6)' },
  closeBtn: { width: 36, height: 36, borderRadius: 18, backgroundColor: 'rgba(255,255,255,0.15)', alignItems: 'center', justifyContent: 'center' },
  roomBarInfo: { flex: 1, marginLeft: 12 },
  roomBarTitle: { color: 'white', fontSize: 16, fontWeight: '700', fontFamily: 'Space Grotesk' },
  roomBarSub: { color: '#aaa', fontSize: 11, fontFamily: 'IBM Plex Sans', marginTop: 2 },
  roomCounter: { backgroundColor: 'rgba(255,255,255,0.15)', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 2 },
  roomCounterText: { color: 'white', fontSize: 12, fontFamily: 'IBM Plex Mono' },

  // 3D Canvas
  canvas: {
    flex: 1,
    position: 'relative',
    overflow: 'hidden',
  },

  // Floor
  floor: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: '40%',
    transform: [{ perspective: 400 }, { rotateX: '50deg' }],
    transformOrigin: 'bottom',
  },
  floorGrid: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 },
  floorLine: { position: 'absolute', top: 0, bottom: 0, width: 1, backgroundColor: 'rgba(0,0,0,0.08)' },

  // Walls
  backWall: {
    position: 'absolute',
    top: '15%',
    left: '10%',
    right: '10%',
    height: '50%',
    borderRadius: 2,
  },
  leftWall: {
    position: 'absolute',
    top: '15%',
    left: 0,
    width: '12%',
    height: '50%',
  },
  rightWall: {
    position: 'absolute',
    top: '15%',
    right: 0,
    width: '12%',
    height: '50%',
  },
  ceiling: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: '15%',
  },

  // Window
  window: {
    position: 'absolute',
    top: '15%',
    left: '30%',
    right: '30%',
    height: '40%',
    backgroundColor: '#D4E8F0',
    borderWidth: 2,
    borderColor: '#8B7355',
    borderRadius: 2,
    overflow: 'hidden',
  },
  windowPane: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: '#B8D8E8' },
  windowDivider: { position: 'absolute', top: 0, bottom: 0, left: '50%', width: 2, backgroundColor: '#8B7355' },
  windowFrame: { position: 'absolute', top: '50%', left: 0, right: 0, height: 2, backgroundColor: '#8B7355' },

  // Room info overlay
  roomOverlay: { position: 'absolute', bottom: 12, left: 12 },
  roomOverlayName: { color: '#0B1F33', fontSize: 14, fontWeight: '700', fontFamily: 'Space Grotesk' },
  roomOverlayType: { color: '#17324D', fontSize: 11, fontFamily: 'IBM Plex Mono', textTransform: 'uppercase', marginTop: 2 },

  // Doorway
  doorway: {
    position: 'absolute',
    bottom: '35%',
    right: '15%',
    width: 60,
    height: 100,
    backgroundColor: '#5C4A36',
    borderRadius: 4,
    borderWidth: 3,
    borderColor: '#3E2E1E',
    alignItems: 'center',
    justifyContent: 'center',
  },
  doorFrame: { marginBottom: 4 },
  doorLabel: { color: '#D7F1EE', fontSize: 9, fontWeight: '600', fontFamily: 'IBM Plex Sans', textAlign: 'center' },

  // Accent
  accentCircle: {
    position: 'absolute',
    top: '20%',
    left: '15%',
    width: 40,
    height: 40,
    borderRadius: 20,
    opacity: 0.5,
  },

  // Room tabs
  tabsRow: { backgroundColor: '#1A1A1A', paddingVertical: 8 },
  tabsScroll: { flexDirection: 'row', paddingHorizontal: 12, gap: 6 },
  tab: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 2,
    borderWidth: 1,
    borderColor: '#333',
    backgroundColor: 'transparent',
  },
  tabActive: { backgroundColor: '#007C78', borderColor: '#007C78' },
  tabText: { color: '#aaa', fontSize: 11, fontFamily: 'IBM Plex Sans' },
  tabTextActive: { color: 'white', fontWeight: '600' },

  // Joystick
  joystickContainer: {
    position: 'absolute',
    bottom: 80,
    left: 24,
    alignItems: 'center',
  },
  joystickBase: {
    width: JOYSTICK_SIZE,
    height: JOYSTICK_SIZE,
    borderRadius: JOYSTICK_SIZE / 2,
    backgroundColor: 'rgba(255,255,255,0.1)',
    borderWidth: 2,
    borderColor: 'rgba(255,255,255,0.2)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  joystickKnob: {
    width: JOYSTICK_KNOB,
    height: JOYSTICK_KNOB,
    borderRadius: JOYSTICK_KNOB / 2,
    backgroundColor: 'rgba(0, 124, 120, 0.8)',
    borderWidth: 2,
    borderColor: 'rgba(0, 124, 120, 1)',
  },
  dirHint: { position: 'absolute' },
  joystickLabel: { color: 'rgba(255,255,255,0.4)', fontSize: 9, fontFamily: 'IBM Plex Mono', letterSpacing: 1, marginTop: 8 },

  // Hint overlay
  hintOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.5)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },
  hintCard: {
    backgroundColor: 'white',
    borderRadius: 8,
    padding: 24,
    width: '100%',
  },
  hintTitle: { fontSize: 18, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk', marginBottom: 16 },
  hintRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 12 },
  hintText: { flex: 1, fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  hintDismiss: { backgroundColor: '#007C78', padding: 12, borderRadius: 2, alignItems: 'center', marginTop: 16 },
  hintDismissText: { color: 'white', fontSize: 14, fontWeight: '600', fontFamily: 'IBM Plex Sans' },
});
