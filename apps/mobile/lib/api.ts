import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_URL } from '../config';

const TOKEN_KEY = 'dorja_token';
const USER_KEY = 'dorja_user';

export interface DorjaUser {
  id: string;
  name: string;
  phone: string;
  role: string;
}

export async function getToken(): Promise<string> {
  try {
    return (await AsyncStorage.getItem(TOKEN_KEY)) || '';
  } catch {
    return '';
  }
}

export async function setToken(token: string): Promise<void> {
  await AsyncStorage.setItem(TOKEN_KEY, token);
}

export async function getUser(): Promise<DorjaUser | null> {
  try {
    const raw = await AsyncStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export async function setUser(user: DorjaUser): Promise<void> {
  await AsyncStorage.setItem(USER_KEY, JSON.stringify(user));
}

export async function clearAuth(): Promise<void> {
  await AsyncStorage.removeItem(TOKEN_KEY);
  await AsyncStorage.removeItem(USER_KEY);
}

/**
 * Auto-login: tries stored token first, then does OTP if needed.
 * Returns { token, user } or null on failure.
 */
export async function ensureAuth(phone = '+8801700000001'): Promise<{ token: string; user: DorjaUser } | null> {
  try {
    let token = await getToken();
    let user = await getUser();

    if (token && user) return { token, user };

    // Auto-login with fake OTP
    await fetch(`${API_URL}/v1/auth/otp/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone }),
    });

    const vr = await fetch(`${API_URL}/v1/auth/otp/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone, code: '123456' }),
    });

    const vd = await vr.json();
    if (vd.data?.accessToken) {
      token = vd.data.accessToken;
      user = {
        id: vd.data.user?.id || '',
        name: vd.data.user?.displayName || 'Demo User',
        phone,
        role: vd.data.user?.primaryRole || 'SEEKER',
      };
      await setToken(token);
      await setUser(user);
      return { token, user };
    }
  } catch (e) {
    console.log('ensureAuth failed:', e);
  }
  return null;
}

/**
 * Login with username/password
 */
export async function loginWithPassword(username: string, password: string): Promise<{ token: string; user: DorjaUser } | null> {
  try {
    const res = await fetch(`${API_URL}/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const data = await res.json();
    if (res.ok && data.data?.accessToken) {
      const token = data.data.accessToken;
      const user: DorjaUser = {
        id: data.data.user?.id || '',
        name: data.data.user?.displayName || username,
        phone: data.data.user?.phone || '',
        role: data.data.user?.primaryRole || (username === 'seller' ? 'OWNER' : 'SEEKER'),
      };
      await setToken(token);
      await setUser(user);
      return { token, user };
    }
  } catch (e) {
    console.log('loginWithPassword failed:', e);
  }
  return null;
}

/**
 * Fetch with auth header
 */
export async function authFetch(path: string, options: RequestInit = {}): Promise<Response> {
  const token = await getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return fetch(`${API_URL}${path}`, { ...options, headers });
}
