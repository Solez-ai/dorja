import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

// Since @expo/vector-icons may not be installed, we use simple SVG-free text icons
// that render reliably on all platforms

const ICON_MAP: Record<string, string> = {
  // Navigation
  explore: '◎',
  inbox: '◇',
  capture: '◉',
  visits: '◻',
  account: '◎',
  // Actions
  search: '⌕',
  star: '★',
  starOutline: '☆',
  heart: '♥',
  heartOutline: '♡',
  check: '✓',
  close: '✕',
  back: '←',
  forward: '→',
  up: '↑',
  down: '↓',
  arrowRight: '→',
  chevronRight: '›',
  chevronLeft: '‹',
  chevronDown: '▾',
  menu: '≡',
  // Property
  home: '⌂',
  building: '▣',
  door: '▯',
  camera: '◎',
  map: '◈',
  mapPin: '◉',
  lock: '●',
  unlock: '○',
  shield: '◆',
  key: '⚷',
  // Status
  clock: '◷',
  pulse: '●',
  alert: '⚠',
  info: 'ℹ',
  bell: '◎',
  // Communication
  message: '◇',
  send: '▲',
  phone: '☎',
  mail: '✉',
  // Social
  user: '◎',
  users: '◎',
  group: '◎',
  // UI
  calendar: '◻',
  filter: '≡',
  sort: '↕',
  settings: '⚙',
  edit: '✎',
  trash: '✕',
  share: '⬡',
  download: '⇩',
  upload: '⇧',
  print: '⎙',
  qr: '▦',
  scan: '▧',
  cube: '◇',
  eye: '◎',
  eyeOff: '○',
};

// Simple text-based icon that works everywhere without native modules
export function Icon({ name, size = 16, color = '#0B1F33', style }: {
  name: string;
  size?: number;
  color?: string;
  style?: any;
}) {
  const icon = ICON_MAP[name] || '•';
  return (
    <Text style={[{ fontSize: size, color, lineHeight: size + 2 }, style]}>
      {icon}
    </Text>
  );
}

// Icon with circle background (for feature rows, etc)
export function IconCircle({ name, size = 40, bgColor = '#D7F1EE', iconColor = '#007C78', iconSize = 18 }: {
  name: string;
  size?: number;
  bgColor?: string;
  iconColor?: string;
  iconSize?: number;
}) {
  return (
    <View style={[{ width: size, height: size, borderRadius: size / 2, backgroundColor: bgColor, alignItems: 'center', justifyContent: 'center' }]}>
      <Icon name={name} size={iconSize} color={iconColor} />
    </View>
  );
}

// Badge component
export function Badge({ text, bgColor = '#D7F1EE', textColor = '#006B68' }: {
  text: string;
  bgColor?: string;
  textColor?: string;
}) {
  return (
    <View style={[{ paddingHorizontal: 8, paddingVertical: 3, borderRadius: 2, backgroundColor: bgColor, alignSelf: 'flex-start' }]}>
      <Text style={{ fontSize: 10, fontWeight: '700', color: textColor, letterSpacing: 0.8, textTransform: 'uppercase', fontFamily: 'IBM Plex Mono' }}>{text}</Text>
    </View>
  );
}

export { ICON_MAP };
