import { useState, useEffect } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { Icon, IconCircle, Badge } from '../../components/Icons';

const API_URL = 'http://localhost:4000';

const MENU_ITEMS = [
  { label: 'My listings', icon: 'home', route: '/listing/create' },
  { label: 'Identity verification', icon: 'shield', route: null },
  { label: 'Trusted contacts', icon: 'users', route: null },
  { label: 'Safety guide', icon: 'lock', route: null },
  { label: 'Settings', icon: 'settings', route: null },
];

export default function AccountScreen() {
  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    try {
      const stored = localStorage.getItem('dorja_user');
      if (stored) setUser(JSON.parse(stored));
    } catch {}
  }, []);

  const signOut = () => {
    try {
      localStorage.removeItem('dorja_user');
      localStorage.removeItem('dorja_token');
    } catch {}
    setUser(null);
  };

  const signIn = async () => {
    try {
      // Auto-login with dev OTP
      await fetch(API_URL + '/v1/auth/otp/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: '+8801700000001' }),
      });
      const vr = await fetch(API_URL + '/v1/auth/otp/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: '+8801700000001', code: '123456' }),
      });
      const vd = await vr.json();
      if (vd.data?.accessToken) {
        const u = { name: 'Demo User', phone: '+8801700000001', role: 'SEEKER' };
        localStorage.setItem('dorja_user', JSON.stringify(u));
        localStorage.setItem('dorja_token', vd.data.accessToken);
        setUser(u);
      }
    } catch {}
  };

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      <View style={styles.header}>
        <Text style={styles.title}>Account</Text>
      </View>

      {user ? (
        <>
          {/* Profile card */}
          <View style={styles.profileCard}>
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>{user.name?.[0] || 'U'}</Text>
            </View>
            <View style={styles.profileInfo}>
              <Text style={styles.profileName}>{user.name}</Text>
              <View style={styles.profileMeta}>
                <Badge text={user.role || 'SEEKER'} />
                <Text style={styles.profilePhone}>{user.phone}</Text>
              </View>
            </View>
          </View>

          {/* Menu items */}
          <View style={styles.menu}>
            {MENU_ITEMS.map((item) => (
              <TouchableOpacity key={item.label} style={styles.menuItem} onPress={() => item.route && router.push(item.route as any)}>
                <View style={styles.menuIconCircle}>
                  <Icon name={item.icon} size={16} color="#007C78" />
                </View>
                <Text style={styles.menuLabel}>{item.label}</Text>
                <Icon name="chevronRight" size={16} color="#D9CCB9" />
              </TouchableOpacity>
            ))}
          </View>

          {/* Sign out */}
          <TouchableOpacity style={styles.signOutBtn} onPress={signOut}>
            <Icon name="lock" size={16} color="#B91C1C" />
            <Text style={styles.signOutText}>Sign Out</Text>
          </TouchableOpacity>
        </>
      ) : (
        /* Not signed in */
        <View style={styles.signInSection}>
          <IconCircle name="user" size={64} bgColor="#D7F1EE" iconColor="#007C78" iconSize={28} />
          <Text style={styles.signInTitle}>Not signed in</Text>
          <Text style={styles.signInDesc}>
            Sign in to manage listings, schedule visits, and use Protected Chat.
          </Text>
          <TouchableOpacity style={styles.signInBtn} onPress={signIn}>
            <Icon name="key" size={16} color="white" />
            <Text style={styles.signInBtnText}>Sign In (Demo)</Text>
          </TouchableOpacity>
          <Text style={styles.signInHint}>Uses dev OTP bypass — code: 123456</Text>
        </View>
      )}

      <View style={{ height: 40 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2' },
  header: { padding: 20 },
  title: { fontSize: 24, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk' },

  profileCard: {
    flexDirection: 'row',
    alignItems: 'center',
    margin: 16,
    padding: 16,
    backgroundColor: 'white',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#D9CCB9',
  },
  avatar: { width: 52, height: 52, borderRadius: 26, backgroundColor: '#007C78', alignItems: 'center', justifyContent: 'center', marginRight: 14 },
  avatarText: { color: 'white', fontSize: 22, fontWeight: '700', fontFamily: 'Space Grotesk' },
  profileInfo: { flex: 1 },
  profileName: { fontSize: 17, fontWeight: '600', color: '#0B1F33', fontFamily: 'IBM Plex Sans' },
  profileMeta: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 6 },
  profilePhone: { fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Mono' },

  menu: { padding: 16, paddingTop: 0, gap: 8 },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
    backgroundColor: 'white',
    borderWidth: 1,
    borderColor: '#E8E0D0',
    borderRadius: 4,
  },
  menuIconCircle: { width: 32, height: 32, borderRadius: 16, backgroundColor: '#D7F1EE', alignItems: 'center', justifyContent: 'center', marginRight: 12 },
  menuLabel: { flex: 1, fontSize: 15, color: '#0B1F33', fontFamily: 'IBM Plex Sans' },

  signOutBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, margin: 16, padding: 14, backgroundColor: '#FEF2F2', borderRadius: 4, borderWidth: 1, borderColor: '#FECACA' },
  signOutText: { fontSize: 14, fontWeight: '600', color: '#B91C1C', fontFamily: 'IBM Plex Sans' },

  signInSection: { alignItems: 'center', padding: 32, gap: 12 },
  signInTitle: { fontSize: 18, fontWeight: '600', color: '#0B1F33', fontFamily: 'Space Grotesk' },
  signInDesc: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans', textAlign: 'center', lineHeight: 20 },
  signInBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, backgroundColor: '#007C78', padding: 14, borderRadius: 4, width: '100%', marginTop: 8 },
  signInBtnText: { color: 'white', fontSize: 14, fontWeight: '600', fontFamily: 'IBM Plex Sans' },
  signInHint: { fontSize: 11, color: '#D9CCB9', fontFamily: 'IBM Plex Mono' },
});
