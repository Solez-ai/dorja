import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  ActivityIndicator,
  StyleSheet,
  TouchableOpacity,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { API_URL } from '../config';

interface Props {
  children: React.ReactNode;
}

const COLORS = {
  ink950: '#0B1F33',
  jol600: '#007C78',
  paper50: '#FBF8F2',
  sand300: '#D9CCB9',
  teal100: '#D7F1EE',
  gray700: '#17324D',
};

export function BackendGate({ children }: Props) {
  const insets = useSafeAreaInsets();
  const [status, setStatus] = useState<'checking' | 'connected' | 'failed'>('checking');
  const [apiUrl, setApiUrl] = useState(API_URL);
  const [retries, setRetries] = useState(0);

  const checkBackend = useCallback(async () => {
    setStatus('checking');
    try {
      const res = await fetch(`${apiUrl}/v1/health`, {
        method: 'GET',
        signal: AbortSignal.timeout(5000),
      });
      if (res.ok) {
        setStatus('connected');
      } else {
        setStatus('failed');
      }
    } catch {
      setStatus('failed');
    }
  }, [apiUrl]);

  useEffect(() => {
    checkBackend();
  }, []);

  // Auto-retry with backoff
  useEffect(() => {
    if (status !== 'checking' && retries < 3) {
      const timer = setTimeout(() => {
        setRetries((r) => r + 1);
        checkBackend();
      }, (retries + 1) * 3000);
      return () => clearTimeout(timer);
    }
  }, [status, retries, checkBackend]);

  if (status === 'connected') {
    return <>{children}</>;
  }

  if (status === 'checking') {
    return (
      <View style={[s.container, { paddingTop: insets.top }]}>

        <View style={s.center}>
          {/* Logo */}
          <View style={s.logoContainer}>
            <Ionicons name="shield-checkmark" size={64} color={COLORS.jol600} />
          </View>
          <Text style={s.brand}>DORJA</Text>
          <Text style={s.tagline}>Property Trust Platform</Text>

          <View style={s.loadingRow}>
            <ActivityIndicator size="small" color={COLORS.jol600} />
            <Text style={s.loadingText}>Connecting to server...</Text>
          </View>

          <Text style={s.apiUrl}>{apiUrl}</Text>
        </View>

        <View style={s.footer}>
          <Ionicons name="shield" size={12} color={COLORS.jol600} />
          <Text style={s.footerText}>AES-256-GCM encrypted</Text>
        </View>
      </View>
    );
  }

  // Failed state
  return (
    <View style={[s.container, { paddingTop: insets.top }]}>
      <View style={s.center}>
        {/* Logo */}
        <View style={s.logoContainer}>
          <Ionicons name="shield-checkmark" size={64} color={COLORS.jol600} />
        </View>
        <Text style={s.brand}>DORJA</Text>
        <Text style={s.tagline}>Property Trust Platform</Text>

        <View style={s.errorCard}>
          <Ionicons name="cloud-offline" size={32} color="#B91C1C" />
          <Text style={s.errorTitle}>Server not found</Text>
          <Text style={s.errorDesc}>
            Cannot reach the DORJA backend at:{'\n'}
            <Text style={{ fontFamily: 'IBM Plex Mono', fontWeight: '600' }}>
              {apiUrl}
            </Text>
          </Text>

          <Text style={s.helpTitle}>To fix this, make sure:</Text>
          {[
            'Docker containers are running on your PC',
            'The API server is started (port 4000)',
            'Your phone is on the same WiFi network',
          ].map((tip, i) => (
            <View key={i} style={s.tipRow}>
              <View style={s.tipNum}>
                <Text style={s.tipNumText}>{i + 1}</Text>
              </View>
              <Text style={s.tipText}>{tip}</Text>
            </View>
          ))}

          <TouchableOpacity style={s.retryBtn} onPress={() => { setRetries(0); checkBackend(); }}>
            <Ionicons name="refresh" size={16} color="white" />
            <Text style={s.retryText}>Try Again</Text>
          </TouchableOpacity>
        </View>
      </View>

      <View style={s.footer}>
        <Ionicons name="shield" size={12} color={COLORS.jol600} />
        <Text style={s.footerText}>AES-256-GCM encrypted</Text>
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.paper50,
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },

  // Logo
  logoContainer: {
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: COLORS.teal100,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
    shadowColor: COLORS.jol600,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 12,
    elevation: 6,
  },
  brand: {
    fontSize: 36,
    fontWeight: '700',
    color: COLORS.ink950,
    fontFamily: 'Space Grotesk',
    letterSpacing: 2,
  },
  tagline: {
    fontSize: 14,
    color: COLORS.gray700,
    fontFamily: 'IBM Plex Sans',
    marginTop: 4,
  },

  // Loading
  loadingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginTop: 40,
    paddingVertical: 12,
    paddingHorizontal: 20,
    backgroundColor: COLORS.teal100,
    borderRadius: 8,
  },
  loadingText: {
    fontSize: 14,
    color: COLORS.jol600,
    fontFamily: 'IBM Plex Sans',
    fontWeight: '500',
  },
  apiUrl: {
    fontSize: 11,
    color: COLORS.sand300,
    fontFamily: 'IBM Plex Mono',
    marginTop: 12,
  },

  // Error
  errorCard: {
    marginTop: 32,
    backgroundColor: 'white',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E8E0D0',
    padding: 24,
    width: '100%',
    alignItems: 'center',
    shadowColor: COLORS.ink950,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 8,
    elevation: 3,
  },
  errorTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: COLORS.ink950,
    fontFamily: 'Space Grotesk',
    marginTop: 12,
  },
  errorDesc: {
    fontSize: 13,
    color: COLORS.gray700,
    fontFamily: 'IBM Plex Sans',
    textAlign: 'center',
    marginTop: 8,
    lineHeight: 20,
  },
  helpTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: COLORS.gray700,
    fontFamily: 'IBM Plex Sans',
    marginTop: 20,
    marginBottom: 8,
    alignSelf: 'flex-start',
  },
  tipRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginBottom: 8,
    alignSelf: 'flex-start',
  },
  tipNum: {
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: COLORS.teal100,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tipNumText: {
    fontSize: 11,
    fontWeight: '700',
    color: COLORS.jol600,
    fontFamily: 'IBM Plex Mono',
  },
  tipText: {
    flex: 1,
    fontSize: 13,
    color: COLORS.gray700,
    fontFamily: 'IBM Plex Sans',
  },
  retryBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: COLORS.jol600,
    paddingVertical: 14,
    paddingHorizontal: 32,
    borderRadius: 8,
    width: '100%',
    marginTop: 20,
  },
  retryText: {
    color: 'white',
    fontSize: 15,
    fontWeight: '600',
    fontFamily: 'IBM Plex Sans',
  },

  // Footer
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingBottom: 24,
  },
  footerText: {
    fontSize: 12,
    color: COLORS.sand300,
    fontFamily: 'IBM Plex Sans',
  },
});
