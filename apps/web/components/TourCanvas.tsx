'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { Home, DoorOpen, ArrowLeft, ArrowRight, ArrowUp, ArrowDown, Move, MousePointer } from 'lucide-react';

type Room = {
  id: string;
  roomType: string;
  displayName: string;
  previewUrl?: string;
  panoramaUrl?: string;
  sourceType?: string;
};

type Edge = {
  fromRoomId: string;
  toRoomId: string;
  doorwayLabel: string;
};

type Props = {
  passport: {
    rooms: Room[];
    edges: Edge[];
    listing?: { title: string };
  };
};

const ROOM_COLORS: Record<string, { floor: string; walls: string; ceiling: string; accent: string }> = {
  LIVING_ROOM: { floor: '#8B7355', walls: '#F5F0E8', ceiling: '#FAFAFA', accent: '#D4C5A9' },
  BEDROOM: { floor: '#7B8B6F', walls: '#F0F5ED', ceiling: '#FAFAFA', accent: '#C4D4B8' },
  KITCHEN: { floor: '#6B7B8B', walls: '#EDF2F5', ceiling: '#FAFAFA', accent: '#B8C4D4' },
  BATHROOM: { floor: '#5B8B8B', walls: '#EDF5F5', ceiling: '#FAFAFA', accent: '#B8D4D4' },
  BALCONY: { floor: '#8B8B6B', walls: '#F5F5ED', ceiling: '#87CEEB', accent: '#D4D4B8' },
  DINING_ROOM: { floor: '#8B6B7B', walls: '#F5EDF2', ceiling: '#FAFAFA', accent: '#D4B8C4' },
  ENTRY: { floor: '#7B7B6B', walls: '#F2F2ED', ceiling: '#FAFAFA', accent: '#C4C4B8' },
  OTHER: { floor: '#7B7B7B', walls: '#F0F0F0', ceiling: '#FAFAFA', accent: '#C4C4C4' },
};

// Room icons use Lucide components rendered in JSX below
const ROOM_ICON_NAMES: Record<string, string> = {
  LIVING_ROOM: 'home', BEDROOM: 'door', KITCHEN: 'home', BATHROOM: 'lock',
  BALCONY: 'eye', DINING_ROOM: 'home', ENTRY: 'door', OTHER: 'home',
};

// ─── Joystick Component ─────────────────────────────────────────
function Joystick({ onMove }: { onMove: (dx: number, dy: number) => void }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const knobRef = useRef<HTMLDivElement>(null);
  const [active, setActive] = useState(false);
  const origin = useRef({ x: 0, y: 0 });
  const rafRef = useRef<number>(0);
  const deltaRef = useRef({ x: 0, y: 0 });

  const handleStart = useCallback((clientX: number, clientY: number) => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    origin.current = { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 };
    setActive(true);
  }, []);

  const handleMove = useCallback((clientX: number, clientY: number) => {
    if (!active) return;
    const dx = clientX - origin.current.x;
    const dy = clientY - origin.current.y;
    const maxDist = 40;
    const dist = Math.sqrt(dx * dx + dy * dy);
    const clampedX = dist > maxDist ? (dx / dist) * maxDist : dx;
    const clampedY = dist > maxDist ? (dy / dist) * maxDist : dy;
    deltaRef.current = { x: clampedX / maxDist, y: clampedY / maxDist };
    if (knobRef.current) {
      knobRef.current.style.transform = `translate(${clampedX}px, ${clampedY}px)`;
    }
  }, [active]);

  const handleEnd = useCallback(() => {
    setActive(false);
    deltaRef.current = { x: 0, y: 0 };
    if (knobRef.current) knobRef.current.style.transform = 'translate(0px, 0px)';
  }, []);

  useEffect(() => {
    if (!active) return;
    let running = true;
    const tick = () => {
      if (!running) return;
      onMove(deltaRef.current.x, deltaRef.current.y);
      rafRef.current = requestAnimationFrame(tick);
    };
    rafRef.current = requestAnimationFrame(tick);
    return () => { running = false; cancelAnimationFrame(rafRef.current); };
  }, [active, onMove]);

  return (
    <div
      ref={containerRef}
      onMouseDown={e => handleStart(e.clientX, e.clientY)}
      onMouseMove={e => handleMove(e.clientX, e.clientY)}
      onMouseUp={handleEnd}
      onMouseLeave={handleEnd}
      onTouchStart={e => handleStart(e.touches[0].clientX, e.touches[0].clientY)}
      onTouchMove={e => { e.preventDefault(); handleMove(e.touches[0].clientX, e.touches[0].clientY); }}
      onTouchEnd={handleEnd}
      style={{
        position: 'absolute', bottom: 24, left: 24,
        width: 100, height: 100, borderRadius: '50%',
        background: 'rgba(255,255,255,0.12)', border: '2px solid rgba(255,255,255,0.25)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        touchAction: 'none', cursor: 'grab', zIndex: 10,
      }}
    >
      <div
        ref={knobRef}
        style={{
          width: 44, height: 44, borderRadius: '50%',
          background: active ? 'var(--jol-600)' : 'rgba(255,255,255,0.3)',
          border: '2px solid rgba(255,255,255,0.5)',
          transition: active ? 'none' : 'transform 100ms ease',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}
      >
        <Move size={18} style={{ color: 'white' }} />
      </div>
    </div>
  );
}

// ─── Main TourCanvas ────────────────────────────────────────────
export function TourCanvas({ passport }: Props) {
  const [selectedRoom, setSelectedRoom] = useState<string | null>(passport.rooms[0]?.id ?? null);
  const [viewAngle, setViewAngle] = useState(0); // yaw in degrees
  const [pitch, setPitch] = useState(0);
  const [zoom, setZoom] = useState(1);
  const [isPC, setIsPC] = useState(true);
  const [showControls, setShowControls] = useState(true);
  const containerRef = useRef<HTMLDivElement>(null);
  const keysRef = useRef<Set<string>>(new Set());
  const mouseDownRef = useRef(false);
  const lastMouseRef = useRef({ x: 0, y: 0 });

  const room = passport.rooms.find(r => r.id === selectedRoom);
  const exits = passport.edges.filter(e => e.fromRoomId === selectedRoom);
  const colors = ROOM_COLORS[room?.roomType || 'OTHER'] || ROOM_COLORS.OTHER;

  // Detect platform
  useEffect(() => {
    const check = () => setIsPC(window.innerWidth > 768 && !('ontouchstart' in window));
    check();
    window.addEventListener('resize', check);
    return () => window.removeEventListener('resize', check);
  }, []);

  // Keyboard WASD controls
  useEffect(() => {
    if (!isPC) return;
    const onKeyDown = (e: KeyboardEvent) => {
      keysRef.current.add(e.key.toLowerCase());
      if (['w', 'a', 's', 'd', 'arrowleft', 'arrowright', 'arrowup', 'arrowdown'].includes(e.key.toLowerCase())) {
        e.preventDefault();
      }
    };
    const onKeyUp = (e: KeyboardEvent) => keysRef.current.delete(e.key.toLowerCase());
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    return () => { window.removeEventListener('keydown', onKeyDown); window.removeEventListener('keyup', onKeyUp); };
  }, [isPC]);

  // WASD movement loop
  useEffect(() => {
    if (!isPC) return;
    let raf: number;
    const tick = () => {
      const keys = keysRef.current;
      if (keys.has('a') || keys.has('arrowleft')) setViewAngle(v => v - 2);
      if (keys.has('d') || keys.has('arrowright')) setViewAngle(v => v + 2);
      if (keys.has('w') || keys.has('arrowup')) setZoom(z => Math.min(z + 0.02, 2.5));
      if (keys.has('s') || keys.has('arrowdown')) setZoom(z => Math.max(z - 0.02, 0.5));
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [isPC]);

  // Mouse drag for view rotation (PC)
  const onMouseDown = (e: React.MouseEvent) => {
    mouseDownRef.current = true;
    lastMouseRef.current = { x: e.clientX, y: e.clientY };
  };
  const onMouseMove = (e: React.MouseEvent) => {
    if (!mouseDownRef.current) return;
    const dx = e.clientX - lastMouseRef.current.x;
    const dy = e.clientY - lastMouseRef.current.y;
    setViewAngle(v => v + dx * 0.3);
    setPitch(p => Math.max(-30, Math.min(30, p - dy * 0.3)));
    lastMouseRef.current = { x: e.clientX, y: e.clientY };
  };
  const onMouseUp = () => { mouseDownRef.current = false; };

  // Scroll zoom (PC)
  const onWheel = (e: React.WheelEvent) => {
    setZoom(z => Math.max(0.5, Math.min(2.5, z - e.deltaY * 0.001)));
  };

  // Joystick handler (mobile)
  const onJoystickMove = useCallback((dx: number, dy: number) => {
    setViewAngle(v => v + dx * 3);
    setZoom(z => Math.max(0.5, Math.min(2.5, z - dy * 0.02)));
  }, []);

  if (!room) {
    return (
      <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--sand-300)' }}>
        Select a room to explore
      </div>
    );
  }

  const angleRad = (viewAngle * Math.PI) / 180;
  const pitchRad = (pitch * Math.PI) / 180;

  // 3D perspective transform
  const translateZ = zoom * 200;
  const rotateY = viewAngle;
  const rotateX = pitch;

  return (
    <div
      ref={containerRef}
      style={{ width: '100%', height: '100%', position: 'relative', overflow: 'hidden', background: '#000' }}
      onMouseDown={onMouseDown}
      onMouseMove={onMouseMove}
      onMouseUp={onMouseUp}
      onMouseLeave={onMouseUp}
      onWheel={onWheel}
    >
      {/* 3D Room Scene */}
      <div style={{
        position: 'absolute', inset: 0,
        perspective: '800px',
        perspectiveOrigin: '50% 50%',
        cursor: isPC ? 'grab' : 'default',
      }}>
        <div style={{
          width: '100%', height: '100%',
          transformStyle: 'preserve-3d',
          transform: `translateZ(${-translateZ}px) rotateY(${rotateY}deg) rotateX(${rotateX}deg)`,
          transition: 'none',
        }}>
          {/* Floor */}
          <div style={{
            position: 'absolute',
            width: '600px', height: '600px',
            left: '50%', top: '50%',
            marginLeft: '-300px', marginTop: '-100px',
            transform: 'rotateX(90deg) translateZ(-100px)',
            background: `linear-gradient(135deg, ${colors.floor} 0%, ${adjustColor(colors.floor, -20)} 100%)`,
            border: `2px solid ${adjustColor(colors.floor, -30)}`,
            boxShadow: 'inset 0 0 100px rgba(0,0,0,0.1)',
          }}>
            {/* Floor grid lines */}
            <div style={{ position: 'absolute', inset: 0, backgroundImage: `repeating-linear-gradient(0deg, transparent, transparent 59px, rgba(0,0,0,0.05) 59px, rgba(0,0,0,0.05) 60px), repeating-linear-gradient(90deg, transparent, transparent 59px, rgba(0,0,0,0.05) 59px, rgba(0,0,0,0.05) 60px)` }} />
          </div>

          {/* Ceiling */}
          <div style={{
            position: 'absolute',
            width: '600px', height: '600px',
            left: '50%', top: '50%',
            marginLeft: '-300px', marginTop: '-400px',
            transform: 'rotateX(90deg) translateZ(-100px)',
            background: colors.ceiling,
            opacity: 0.6,
          }} />

          {/* Front Wall */}
          <div style={{
            position: 'absolute',
            width: '600px', height: '300px',
            left: '50%', top: '50%',
            marginLeft: '-300px', marginTop: '-250px',
            transform: 'translateZ(300px)',
            background: `linear-gradient(180deg, ${colors.ceiling} 0%, ${colors.walls} 30%, ${colors.walls} 100%)`,
            border: `1px solid ${adjustColor(colors.walls, -10)}`,
          }}>
            {/* Wall decoration */}
            <div style={{ position: 'absolute', top: 20, left: 20, right: 20, bottom: 20, border: `1px solid ${colors.accent}`, borderRadius: 4, opacity: 0.3 }} />
            {/* Window placeholder */}
            <div style={{
              position: 'absolute', top: 40, left: '50%', marginLeft: -60,
              width: 120, height: 80, background: 'linear-gradient(180deg, #87CEEB 0%, #B0E0E6 100%)',
              border: `3px solid ${colors.accent}`, borderRadius: 2,
            }}>
              <div style={{ position: 'absolute', left: '50%', top: 0, bottom: 0, width: 2, background: colors.accent }} />
              <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, height: 2, background: colors.accent }} />
            </div>
          </div>

          {/* Back Wall */}
          <div style={{
            position: 'absolute',
            width: '600px', height: '300px',
            left: '50%', top: '50%',
            marginLeft: '-300px', marginTop: '-250px',
            transform: 'translateZ(-300px) rotateY(180deg)',
            background: `linear-gradient(180deg, ${colors.ceiling} 0%, ${colors.walls} 30%, ${colors.walls} 100%)`,
            border: `1px solid ${adjustColor(colors.walls, -10)}`,
          }} />

          {/* Left Wall */}
          <div style={{
            position: 'absolute',
            width: '600px', height: '300px',
            left: '50%', top: '50%',
            marginLeft: '-600px', marginTop: '-250px',
            transform: 'rotateY(-90deg) translateZ(0px)',
            transformOrigin: 'right center',
            background: `linear-gradient(180deg, ${colors.ceiling} 0%, ${colors.walls} 30%, ${colors.walls} 100%)`,
            border: `1px solid ${adjustColor(colors.walls, -10)}`,
          }} />

          {/* Right Wall */}
          <div style={{
            position: 'absolute',
            width: '600px', height: '300px',
            left: '50%', top: '50%',
            marginLeft: '0px', marginTop: '-250px',
            transform: 'rotateY(90deg) translateZ(0px)',
            transformOrigin: 'left center',
            background: `linear-gradient(180deg, ${colors.ceiling} 0%, ${colors.walls} 30%, ${colors.walls} 100%)`,
            border: `1px solid ${adjustColor(colors.walls, -10)}`,
          }}>
            {/* Doorway on right wall if there's an exit */}
            {exits.length > 0 && (
              <div
                onClick={() => setSelectedRoom(exits[0].toRoomId)}
                style={{
                  position: 'absolute', bottom: 0, left: '50%', marginLeft: -50,
                  width: 100, height: 160, background: 'rgba(0,0,0,0.3)',
                  border: `3px solid ${colors.accent}`, borderBottom: 'none',
                  cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
                  transition: 'background 150ms',
                }}
                onMouseEnter={e => (e.currentTarget.style.background = 'rgba(0,124,120,0.4)')}
                onMouseLeave={e => (e.currentTarget.style.background = 'rgba(0,0,0,0.3)')}
              >
                <div style={{ color: 'white', fontSize: 11, textAlign: 'center', fontWeight: 600 }}>
                  <DoorOpen size={20} style={{ marginBottom: 4 }} />
                  <div>{exits[0].doorwayLabel}</div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Room Info Overlay */}
      <div style={{
        position: 'absolute', top: 16, left: 16,
        background: 'rgba(11, 31, 51, 0.85)', padding: '10px 16px',
        borderRadius: 'var(--radius-stamp)', color: 'white',
        display: 'flex', alignItems: 'center', gap: 8, zIndex: 5,
        backdropFilter: 'blur(8px)',
      }}>
        <Home size={16} />
        <div>
          <div style={{ fontWeight: 600, fontSize: 14 }}>{room.displayName}</div>
          <div style={{ fontSize: 11, opacity: 0.7 }}>{room.roomType.replace(/_/g, ' ')} · {passport.listing?.title || 'Property'}</div>
        </div>
      </div>

      {/* Controls hint (PC) */}
      {isPC && showControls && (
        <div style={{
          position: 'absolute', top: 16, right: 16,
          background: 'rgba(11, 31, 51, 0.85)', padding: '10px 14px',
          borderRadius: 'var(--radius-stamp)', color: 'white',
          fontSize: 11, zIndex: 5, backdropFilter: 'blur(8px)',
          maxWidth: 180,
        }}>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>Controls</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2, opacity: 0.8 }}>
            <span><strong>W/S</strong> — Zoom in/out</span>
            <span><strong>A/D</strong> — Look left/right</span>
            <span><strong>Mouse drag</strong> — Look around</span>
            <span><strong>Scroll</strong> — Zoom</span>
            <span><strong>Click door</strong> — Enter room</span>
          </div>
          <button onClick={() => setShowControls(false)} style={{ background: 'none', border: 'none', color: 'var(--sand-400)', fontSize: 10, cursor: 'pointer', marginTop: 4 }}>Dismiss</button>
        </div>
      )}

      {/* Joystick (mobile) */}
      {!isPC && <Joystick onMove={onJoystickMove} />}

      {/* Room Navigation Bar */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0,
        padding: '12px 16px', background: 'rgba(11, 31, 51, 0.95)',
        borderTop: '1px solid rgba(255,255,255,0.1)',
        display: 'flex', gap: 6, overflowX: 'auto', zIndex: 5,
        backdropFilter: 'blur(8px)',
      }}>
        {passport.rooms.map(r => (
          <button
            key={r.id}
            onClick={() => { setSelectedRoom(r.id); setViewAngle(0); setPitch(0); setZoom(1); }}
            style={{
              flex: '0 0 auto', padding: '8px 14px',
              background: r.id === selectedRoom ? 'var(--jol-600)' : 'rgba(255,255,255,0.08)',
              color: 'white',
              border: `1px solid ${r.id === selectedRoom ? 'var(--jol-600)' : 'rgba(255,255,255,0.15)'}`,
              borderRadius: 'var(--radius-stamp)', fontSize: 12, cursor: 'pointer',
              fontFamily: 'var(--font-body)', transition: 'all 150ms',
              whiteSpace: 'nowrap', display: 'flex', alignItems: 'center', gap: 6,
            }}
          >
            <DoorOpen size={12} />
            {r.displayName}
          </button>
        ))}
      </div>

      {/* Quick nav arrows (mobile-friendly) */}
      {exits.length > 0 && (
        <div style={{
          position: 'absolute', bottom: 70, right: 16,
          display: 'flex', flexDirection: 'column', gap: 6, zIndex: 5,
        }}>
          {exits.map(e => {
            const target = passport.rooms.find(r => r.id === e.toRoomId);
            return (
              <button
                key={`${e.fromRoomId}-${e.toRoomId}`}
                onClick={() => { setSelectedRoom(e.toRoomId); setViewAngle(0); setPitch(0); setZoom(1); }}
                style={{
                  padding: '8px 12px', background: 'var(--jol-600)',
                  color: 'white', border: 'none', borderRadius: 'var(--radius-stamp)',
                  fontSize: 11, cursor: 'pointer', fontFamily: 'var(--font-body)',
                  display: 'flex', alignItems: 'center', gap: 4, fontWeight: 600,
                }}
              >
                <ArrowRight size={12} /> {e.doorwayLabel}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ─── Utility ────────────────────────────────────────────────────
function adjustColor(hex: string, amount: number): string {
  const num = parseInt(hex.replace('#', ''), 16);
  const r = Math.max(0, Math.min(255, ((num >> 16) & 0xFF) + amount));
  const g = Math.max(0, Math.min(255, ((num >> 8) & 0xFF) + amount));
  const b = Math.max(0, Math.min(255, (num & 0xFF) + amount));
  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`;
}
