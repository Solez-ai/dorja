import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

// Map our custom icon names to Ionicons glyph names
const ICON_MAP: Record<string, keyof typeof Ionicons.glyphMap> = {
  // Navigation
  explore: 'compass',
  inbox: 'chatbubbles',
  capture: 'camera',
  visits: 'calendar',
  account: 'person',
  // Actions
  search: 'search',
  star: 'star',
  starOutline: 'star-outline',
  heart: 'heart',
  heartOutline: 'heart-outline',
  check: 'checkmark-circle',
  close: 'close',
  back: 'chevron-back',
  forward: 'chevron-forward',
  up: 'chevron-up',
  down: 'chevron-down',
  arrowRight: 'arrow-forward',
  chevronRight: 'chevron-forward',
  chevronLeft: 'chevron-back',
  chevronDown: 'chevron-down',
  menu: 'menu',
  // Property
  home: 'home',
  building: 'business',
  door: 'enter',
  camera: 'camera',
  map: 'map',
  mapPin: 'location',
  lock: 'lock-closed',
  unlock: 'lock-open',
  shield: 'shield-checkmark',
  key: 'key',
  // Status
  clock: 'time',
  pulse: 'pulse',
  alert: 'warning',
  info: 'information-circle',
  bell: 'notifications',
  // Communication
  message: 'chatbubble',
  send: 'send',
  phone: 'call',
  mail: 'mail',
  // Social
  user: 'person',
  users: 'people',
  group: 'people-circle',
  // UI
  calendar: 'calendar',
  filter: 'filter',
  sort: 'swap-vertical',
  settings: 'settings',
  edit: 'create',
  trash: 'trash',
  share: 'share',
  download: 'download',
  upload: 'cloud-upload',
  print: 'print',
  qr: 'qr-code',
  scan: 'scan',
  cube: 'cube',
  eye: 'eye',
  eyeOff: 'eye-off',
};

export function Icon({
  name,
  size = 16,
  color = '#0B1F33',
  style,
}: {
  name: string;
  size?: number;
  color?: string;
  style?: any;
}) {
  const ionName = ICON_MAP[name] || 'ellipse';
  return <Ionicons name={ionName} size={size} color={color} style={style} />;
}

export function IconCircle({
  name,
  size = 40,
  bgColor = '#D7F1EE',
  iconColor = '#007C78',
  iconSize = 18,
}: {
  name: string;
  size?: number;
  bgColor?: string;
  iconColor?: string;
  iconSize?: number;
}) {
  return (
    <View
      style={[
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor: bgColor,
          alignItems: 'center',
          justifyContent: 'center',
        },
      ]}
    >
      <Icon name={name} size={iconSize} color={iconColor} />
    </View>
  );
}

export function Badge({
  text,
  bgColor = '#D7F1EE',
  textColor = '#006B68',
}: {
  text: string;
  bgColor?: string;
  textColor?: string;
}) {
  return (
    <View
      style={[
        {
          paddingHorizontal: 8,
          paddingVertical: 3,
          borderRadius: 2,
          backgroundColor: bgColor,
          alignSelf: 'flex-start',
        },
      ]}
    >
      <Text
        style={{
          fontSize: 10,
          fontWeight: '700',
          color: textColor,
          letterSpacing: 0.8,
          textTransform: 'uppercase',
          fontFamily: 'IBM Plex Mono',
        }}
      >
        {text}
      </Text>
    </View>
  );
}
