import Constants from 'expo-constants';
import AsyncStorage from '@react-native-async-storage/async-storage';

const DEV_PORT = 4000;
const STORAGE_KEY = '@dorja/api_url';

// ─── Default URL resolution (build-time) ───────────────────────────
function getDefaultApiUrl(): string {
  const envUrl = process.env.EXPO_PUBLIC_API_URL;
  if (envUrl) return envUrl;

  const debuggerHost =
    Constants.expoConfig?.hostUri ??
    Constants.manifest2?.extra?.expoGo?.debuggerHost;
  if (debuggerHost) {
    const ip = debuggerHost.split(':')[0];
    if (ip && ip !== 'localhost' && ip !== '127.0.0.1') {
      return `http://${ip}:${DEV_PORT}`;
    }
  }

  return `http://localhost:${DEV_PORT}`;
}

// ─── Global-backed mutable state ───────────────────────────────────
// Using a global object so every module that calls getApiUrl()
// always sees the current value, even after setApiUrl() mutates it.
const G = globalThis as Record<string, any>;
if (!G.__DORJA_CONFIG__) {
  G.__DORJA_CONFIG__ = { apiUrl: getDefaultApiUrl() };
}
const cfg: { apiUrl: string } = G.__DORJA_CONFIG__;

/** Synchronous getter — returns the currently active backend URL. */
export function getApiUrl(): string {
  return cfg.apiUrl;
}

/** Read the persisted URL from AsyncStorage (call once at startup). */
export async function loadSavedApiUrl(): Promise<string> {
  try {
    const saved = await AsyncStorage.getItem(STORAGE_KEY);
    if (saved && saved.trim().length > 0) {
      cfg.apiUrl = saved.trim();
    }
  } catch {
    // AsyncStorage unavailable — keep default
  }
  return cfg.apiUrl;
}

/** Persist a new backend URL and update the in-memory value. */
export async function setApiUrl(url: string): Promise<void> {
  const trimmed = url.trim().replace(/\/+$/, '');
  cfg.apiUrl = trimmed;
  await AsyncStorage.setItem(STORAGE_KEY, trimmed);
}

/** Clear saved URL and revert to the build-time default. */
export async function resetApiUrl(): Promise<void> {
  cfg.apiUrl = getDefaultApiUrl();
  await AsyncStorage.removeItem(STORAGE_KEY);
}

// ─── Legacy named export for backwards compat ──────────────────────
// NOTE: This is evaluated once at import time.  Prefer getApiUrl()
// for any code that needs the *current* value after a runtime change.
export const API_URL: string = cfg.apiUrl;

export default {
  get API_URL() { return cfg.apiUrl; },
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
