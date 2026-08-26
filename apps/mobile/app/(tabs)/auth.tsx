import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { Icon, IconCircle } from '../../components/Icons';
import { loginWithPassword, ensureAuth } from '../../lib/api';

export default function AuthScreen() {
  const insets = useSafeAreaInsets();
  const [mode, setMode] = useState<'login' | 'otp'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [otpPhone, setOtpPhone] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [otpStep, setOtpStep] = useState<'phone' | 'code'>('phone');
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!username || !password) {
      Alert.alert('Missing', 'Enter username and password');
      return;
    }
    setLoading(true);
    try {
      const result = await loginWithPassword(username, password);
      if (result) {
        router.replace('/(tabs)/explore');
      } else {
        Alert.alert('Error', 'Login failed');
      }
    } catch (e: any) {
      Alert.alert('Network error', e.message);
    } finally {
      setLoading(false);
    }
  };

  const handleOtpStart = async () => {
    if (!otpPhone) {
      Alert.alert('Missing', 'Enter phone number');
      return;
    }
    setLoading(true);
    try {
      const result = await ensureAuth(otpPhone);
      if (result) {
        router.replace('/(tabs)/explore');
      } else {
        Alert.alert('Error', 'Failed to send OTP');
      }
    } catch (e: any) {
      Alert.alert('Network error', e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={[s.container, { paddingTop: insets.top + 20 }]}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView
        contentContainerStyle={s.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <View style={s.card}>
          {/* Logo */}
          <View style={s.logo}>
            <IconCircle
              name="key"
              size={56}
              bgColor="#D7F1EE"
              iconColor="#007C78"
              iconSize={24}
            />
          </View>
          <Text style={s.brand}>DORJA</Text>
          <Text style={s.sub}>Property trust platform for Bangladesh</Text>

          {/* Mode toggle */}
          <View style={s.toggleRow}>
            <TouchableOpacity
              style={[s.toggleBtn, mode === 'login' && s.toggleActive]}
              onPress={() => setMode('login')}
            >
              <Text
                style={[
                  s.toggleText,
                  mode === 'login' && s.toggleTextActive,
                ]}
              >
                Sign In
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[s.toggleBtn, mode === 'otp' && s.toggleActive]}
              onPress={() => setMode('otp')}
            >
              <Text
                style={[
                  s.toggleText,
                  mode === 'otp' && s.toggleTextActive,
                ]}
              >
                Phone OTP
              </Text>
            </TouchableOpacity>
          </View>

          {mode === 'login' ? (
            <>
              {/* Username */}
              <View style={s.field}>
                <Text style={s.label}>Username</Text>
                <View style={s.inputRow}>
                  <Icon name="user" size={16} color="#D9CCB9" />
                  <TextInput
                    style={s.input}
                    value={username}
                    onChangeText={setUsername}
                    placeholder="seller or buyer"
                    placeholderTextColor="#C4B5A0"
                    autoCapitalize="none"
                  />
                </View>
              </View>

              {/* Password */}
              <View style={s.field}>
                <Text style={s.label}>Password</Text>
                <View style={s.inputRow}>
                  <Icon name="lock" size={16} color="#D9CCB9" />
                  <TextInput
                    style={s.input}
                    value={password}
                    onChangeText={setPassword}
                    placeholder="Enter password"
                    placeholderTextColor="#C4B5A0"
                    secureTextEntry
                  />
                </View>
              </View>

              <TouchableOpacity
                style={[s.submitBtn, loading && { opacity: 0.6 }]}
                onPress={handleLogin}
                disabled={loading}
              >
                {loading ? (
                  <ActivityIndicator color="white" />
                ) : (
                  <Icon name="arrowRight" size={16} color="white" />
                )}
                <Text style={s.submitText}>Sign In</Text>
              </TouchableOpacity>

              <View style={s.hintBox}>
                <Text style={s.hintTitle}>Demo credentials</Text>
                <Text style={s.hintText}>
                  seller / 12345678 → Seller account
                </Text>
                <Text style={s.hintText}>
                  buyer / 12345678 → Buyer account
                </Text>
              </View>
            </>
          ) : (
            <>
              {otpStep === 'phone' ? (
                <View style={s.field}>
                  <Text style={s.label}>Phone Number</Text>
                  <View style={s.inputRow}>
                    <Icon name="phone" size={16} color="#D9CCB9" />
                    <TextInput
                      style={s.input}
                      value={otpPhone}
                      onChangeText={setOtpPhone}
                      placeholder="+8801700000001"
                      placeholderTextColor="#C4B5A0"
                      keyboardType="phone-pad"
                    />
                  </View>
                </View>
              ) : (
                <View style={s.field}>
                  <Text style={s.label}>Enter OTP Code</Text>
                  <TextInput
                    style={s.otpInput}
                    value={otpCode}
                    onChangeText={setOtpCode}
                    placeholder="123456"
                    placeholderTextColor="#C4B5A0"
                    keyboardType="number-pad"
                    maxLength={6}
                  />
                  <Text style={s.hintSmall}>
                    Demo: use any 6-digit code
                  </Text>
                </View>
              )}

              <TouchableOpacity
                style={[s.submitBtn, loading && { opacity: 0.6 }]}
                onPress={handleOtpStart}
                disabled={loading}
              >
                {loading ? (
                  <ActivityIndicator color="white" />
                ) : (
                  <Icon name="arrowRight" size={16} color="white" />
                )}
                <Text style={s.submitText}>
                  {otpStep === 'phone' ? 'Send OTP' : 'Verify & Sign In'}
                </Text>
              </TouchableOpacity>
            </>
          )}
        </View>

        {/* Footer */}
        <View style={s.footer}>
          <Icon name="shield" size={12} color="#007C78" />
          <Text style={s.footerText}>
            AES-256-GCM encrypted · SafeView verified
          </Text>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2' },
  scrollContent: { flexGrow: 1, justifyContent: 'center', padding: 24 },
  card: {
    backgroundColor: 'white',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E8E0D0',
    padding: 28,
    shadowColor: '#0B1F33',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 12,
    elevation: 4,
  },
  logo: { alignItems: 'center', marginBottom: 12 },
  brand: {
    fontSize: 28,
    fontWeight: '700',
    color: '#0B1F33',
    fontFamily: 'Space Grotesk',
    textAlign: 'center',
  },
  sub: {
    fontSize: 13,
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
    textAlign: 'center',
    marginTop: 4,
    marginBottom: 24,
  },
  toggleRow: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderBottomColor: '#E8E0D0',
    marginBottom: 20,
  },
  toggleBtn: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  toggleActive: { borderBottomColor: '#007C78' },
  toggleText: {
    fontSize: 14,
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
  },
  toggleTextActive: { color: '#007C78', fontWeight: '600' },
  field: { marginBottom: 16 },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
    marginBottom: 6,
  },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    borderWidth: 1,
    borderColor: '#D9CCB9',
    borderRadius: 8,
    padding: 12,
    backgroundColor: '#FBF8F2',
  },
  input: {
    flex: 1,
    fontSize: 14,
    fontFamily: 'IBM Plex Sans',
    color: '#0B1F33',
    padding: 0,
  },
  otpInput: {
    borderWidth: 1,
    borderColor: '#D9CCB9',
    borderRadius: 8,
    padding: 14,
    fontSize: 20,
    fontFamily: 'IBM Plex Mono',
    textAlign: 'center',
    letterSpacing: 8,
    backgroundColor: '#FBF8F2',
  },
  hintSmall: {
    fontSize: 11,
    color: '#D9CCB9',
    fontFamily: 'IBM Plex Mono',
    marginTop: 6,
  },
  submitBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: '#007C78',
    padding: 14,
    borderRadius: 8,
    marginTop: 8,
  },
  submitText: {
    color: 'white',
    fontSize: 14,
    fontWeight: '600',
    fontFamily: 'IBM Plex Sans',
  },
  hintBox: {
    marginTop: 16,
    padding: 12,
    backgroundColor: '#D7F1EE',
    borderRadius: 8,
  },
  hintTitle: {
    fontSize: 12,
    fontWeight: '600',
    color: '#006B68',
    fontFamily: 'IBM Plex Sans',
    marginBottom: 4,
  },
  hintText: {
    fontSize: 12,
    color: '#17324D',
    fontFamily: 'IBM Plex Mono',
    lineHeight: 18,
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    marginTop: 24,
  },
  footerText: {
    fontSize: 12,
    color: '#D9CCB9',
    fontFamily: 'IBM Plex Sans',
  },
});
