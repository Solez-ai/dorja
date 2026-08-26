import Constants from 'expo-constants';
import { Platform } from 'react-native';

/**
 * API URL configuration.
 * On a real phone, localhost doesn't work — we need the PC's actual IP.
 * Set DORJA_API_URL env var or it auto-detects from Expo's manifest.
 *
 * For EAS builds or when running on a phone:
 *   - expo start --tunnel  (uses tunnel, set DORJA_API_URL to tunnel URL)
 *   - expo start           (same network, auto-detects local IP)
 */
const DEV_PORT = 4000;

function getDevHost(): string {
  // In Expo Go / dev client, the debugger host is set by Expo
  const debuggerHost = Constants.expoConfig?.hostUri ?? Constants.manifest2?.extra?.expoGo?.debuggerHost;
  if (debuggerHost) {
    // debuggerHost is like "192.168.1.5:19000" — we need just the IP
    const ip = debuggerHost.split(':')[0];
    if (ip && ip !== 'localhost' && ip !== '127.0.0.1') {
      return ip;
    }
  }
  // Fallback for web or local development
  return 'localhost';
}

const host = getDevHost();
const isDev = host === 'localhost';

export const API_URL = isDev
  ? `http://localhost:${DEV_PORT}`
  : `http://${host}:${DEV_PORT}`;

// For EAS builds or explicit override
export const EXPO_API_URL = process.env.EXPO_PUBLIC_API_URL || API_URL;

export default {
  API_URL: EXPO_API_URL,
  appName: 'DORJA',
  colors: {
    ink950: '#0B1F33',
    jol600: '#007C78',
    jol700: '#006B68',
    paper50: '#FBF8F2',
    sand300: '#D9CCB9',
    warm100: '#F2EDE3',
    warm200: '#E8E0D0',
    teal100: '#D7F1EE',
    amber100: '#FEF3CD',
    red50: '#FEF2F2',
    red300: '#FECACA',
    red700: '#B91C1C',
    amber600: '#C2710B',
    gray700: '#17324D',
  },
};
