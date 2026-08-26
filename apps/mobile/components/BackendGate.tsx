import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  TextInput,
  ActivityIndicator,
  StyleSheet,
  TouchableOpacity,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import {
  getApiUrl,
  setApiUrl as saveApiUrl,
  loadSavedApiUrl,
} from '../config';

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
  const [status, setStatus] = useState<'checking' | 'connected' | 'failed'>(
    'checking',
  );
  const [apiUrl, setApiUrl] = useState(getApiUrl());
  const [inputValue, setInputValue] = useState(apiUrl);
  const [retries, setRetries] = useState(0);
  const [editing, setEditing] = useState(false);

  // Load saved URL from AsyncStorage on mount
  useEffect(() => {
    loadSavedApiUrl().then((url) => {
      setApiUrl(url);
      setInputValue(url);
    });
  }, []);

  const checkBackend = useCallback(
    async (url?: string) => {
      const target = url ?? apiUrl;
      setStatus('checking');
      try {
        const res = await fetch(`${target}/v1/health`, {
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
    },
    [apiUrl],
  );

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

  /** Save the typed URL, update state, and re-check */
  const applyNewUrl = async () => {
    const trimmed = inputValue.trim().replace(/\/+$/, '');
    if (!trimmed) return;
    await saveApiUrl(trimmed);
    setApiUrl(trimmed);
    setEditing(false);
    setRetries(0);
    checkBackend(trimmed);
  };

  if (status === 'connected') {
    return <>{children}</>;
  }

  if (status === 'checking') {
    return (
      <View style={[s.container, { paddingTop: insets.top }]}>
        <View style={s.center}>
          <View style={s.logoContainer}>
            <Ionicons
              name="shield-checkmark"
              size={64}
              color={COLORS.jol600}
            />
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

  // ─── Failed state ────────────────────────────────────────────────
  return (
    <View style={[s.container, { paddingTop: insets.top }]}>
      <View style={s.center}>
        <View style={s.logoContainer}>
          <Ionicons
            name="shield-checkmark"
            size={64}
            color={COLORS.jol600}
          />
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

          {/* ── URL Editor ─────────────────────────────────────── */}
          <TouchableOpacity
            style={s.editToggle}
            onPress={() => setEditing(!editing)}
          >
            <Ionicons
              name={editing ? 'chevron-up' : 'create-outline'}
              size={14}
              color={COLORS.jol600}
            />
            <Text style={s.editToggleText}>
              {editing ? 'Hide editor' : 'Change server URL'}
            </Text>
          </TouchableOpacity>

          {editing && (
            <View style={s.urlEditor}>
              <Text style={s.urlLabel}>Backend URL</Text>
              <TextInput
                style={s.urlInput}
                value={inputValue}
                onChangeText={setInputValue}
                placeholder="http://192.168.1.100:4000"
                placeholderTextColor={COLORS.sand300}
                autoCapitalize="none"
                autoCorrect={false}
                keyboardType="url"
              />
              <View style={s.urlBtnRow}>
                <TouchableOpacity
                  style={s.urlSaveBtn}
                  onPress={applyNewUrl}
                >
                  <Ionicons name="checkmark" size={14} color="white" />
                  <Text style={s.urlSaveBtnText}>Save & Test</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={s.urlCancelBtn}
                  onPress={() => {
                    setInputValue(apiUrl);
                    setEditing(false);
                  }}
                >
                  <Text style={s.urlCancelBtnText}>Cancel</Text>
                </TouchableOpacity>
              </View>
              <Text style={s.urlHint}>
                Enter the IP address of the machine running the DORJA API.
                {'\n'}Find it with: ipconfig (Windows) or ifconfig (Mac/Linux)
              </Text>
            </View>
          )}

          {/* ── Tips ──────────────────────────────────────────── */}
          <Text style={s.helpTitle}>To fix this, make sure:</Text>
          {[
            'Docker containers are running on your PC',
            'The API server is started (port 4000)',
            'Your phone is on the same WiFi network',
            'Windows Firewall allows port 4000 (see below)',
          ].map((tip, i) => (
            <View key={i} style={s.tipRow}>
              <View style={s.tipNum}>
                <Text style={s.tipNumText}>{i + 1}</Text>
              </View>
              <Text style={s.tipText}>{tip}</Text>
            </View>
          ))}

          {/* ── Firewall fix ─────────────────────────────────── */}
          <View style={s.firewallCard}>
            <Ionicons name="shield-checkmark" size={16} color="#C2710B" />
            <View style={{ flex: 1 }}>
              <Text style={s.firewallTitle}>Windows Firewall</Text>
              <Text style={s.firewallText}>
                Run this on your PC as Administrator in PowerShell:
              </Text>
              <Text style={s.firewallCode}>
                {'New-NetFirewallRule -DisplayName "DORJA API" -Direction Inbound -LocalPort 4000 -Protocol TCP -Action Allow'}
              </Text>
            </View>
          </View>

          {/* ── Browser test ─────────────────────────────────── */}
          <View style={s.browserTestCard}>
            <Ionicons name="globe" size={16} color={COLORS.jol600} />
            <View style={{ flex: 1 }}>
              <Text style={s.browserTestTitle}>Quick test — open this on your phone:</Text>
              <Text style={s.browserTestUrl}>
                {apiUrl}/v1/health
              </Text>
              <Text style={s.browserTestHint}>
                If it shows {"{"}status:"ok"{"}"} → server is reachable.
                {'\n'}If it times out → firewall or network issue.
              </Text>
            </View>
          </View>

          <TouchableOpacity
            style={s.retryBtn}
            onPress={() => {
              setRetries(0);
              checkBackend();
            }}
          >
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
  container: { flex: 1, backgroundColor: COLORS.paper50 },
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

  // URL editor
  editToggle: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 16,
    paddingVertical: 8,
    paddingHorizontal: 14,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: COLORS.teal100,
    backgroundColor: COLORS.paper50,
  },
  editToggleText: {
    fontSize: 13,
    color: COLORS.jol600,
    fontWeight: '600',
    fontFamily: 'IBM Plex Sans',
  },
  urlEditor: {
    width: '100%',
    marginTop: 12,
    gap: 8,
  },
  urlLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: COLORS.gray700,
    fontFamily: 'IBM Plex Sans',
  },
  urlInput: {
    backgroundColor: COLORS.paper50,
    borderWidth: 1,
    borderColor: COLORS.sand300,
    borderRadius: 6,
    padding: 10,
    fontSize: 14,
    fontFamily: 'IBM Plex Mono',
    color: COLORS.ink950,
  },
  urlBtnRow: {
    flexDirection: 'row',
    gap: 8,
  },
  urlSaveBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    backgroundColor: COLORS.jol600,
    paddingVertical: 10,
    borderRadius: 6,
  },
  urlSaveBtnText: {
    color: 'white',
    fontSize: 13,
    fontWeight: '600',
    fontFamily: 'IBM Plex Sans',
  },
  urlCancelBtn: {
    paddingVertical: 10,
    paddingHorizontal: 14,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: COLORS.sand300,
  },
  urlCancelBtnText: {
    fontSize: 13,
    color: COLORS.gray700,
    fontFamily: 'IBM Plex Sans',
  },
  urlHint: {
    fontSize: 11,
    color: COLORS.sand300,
    fontFamily: 'IBM Plex Mono',
    textAlign: 'center',
    lineHeight: 16,
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
