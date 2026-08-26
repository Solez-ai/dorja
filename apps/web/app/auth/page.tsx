'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Key, Phone, User, ArrowRight, Loader2, Shield, Lock } from 'lucide-react';

const API_URL = 'http://localhost:4000';

export default function AuthPage() {
  const router = useRouter();
  const [mode, setMode] = useState<'login' | 'otp'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [otpPhone, setOtpPhone] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [otpStep, setOtpStep] = useState<'phone' | 'code'>('phone');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async () => {
    if (!username || !password) { setError('Enter username and password'); return; }
    setLoading(true);
    setError('');
    try {
      const res = await fetch(API_URL + '/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      const data = await res.json();
      if (res.ok && data.data?.accessToken) {
        localStorage.setItem('dorja_token', data.data.accessToken);
        localStorage.setItem('dorja_user', JSON.stringify(data.data.user));
        router.push('/');
      } else {
        setError(data.error?.message || 'Login failed');
      }
    } catch (e: any) {
      setError('Cannot reach server');
    } finally {
      setLoading(false);
    }
  };

  const handleOtpStart = async () => {
    if (!otpPhone) { setError('Enter phone number'); return; }
    setLoading(true);
    setError('');
    try {
      const res = await fetch(API_URL + '/v1/auth/otp/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: otpPhone }),
      });
      if (res.ok) setOtpStep('code');
      else setError('Failed to send OTP');
    } catch { setError('Cannot reach server'); }
    finally { setLoading(false); }
  };

  const handleOtpVerify = async () => {
    if (!otpCode) { setError('Enter OTP code'); return; }
    setLoading(true);
    setError('');
    try {
      const res = await fetch(API_URL + '/v1/auth/otp/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: otpPhone, code: otpCode }),
      });
      const data = await res.json();
      if (res.ok && data.data?.accessToken) {
        localStorage.setItem('dorja_token', data.data.accessToken);
        router.push('/');
      } else {
        setError(data.error?.message || 'Invalid code');
      }
    } catch { setError('Cannot reach server'); }
    finally { setLoading(false); }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--paper-50)' }}>
      <div style={{ width: 400, maxWidth: '90vw' }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{ width: 56, height: 56, borderRadius: 28, background: 'var(--jol-100)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
            <Key size={24} style={{ color: 'var(--jol-600)' }} />
          </div>
          <h1 style={{ fontSize: 28, fontWeight: 700, fontFamily: 'var(--font-display)', margin: 0 }}>DORJA</h1>
          <p style={{ fontSize: 14, color: 'var(--ink-700)', fontFamily: 'var(--font-body)', marginTop: 8 }}>
            Property trust platform for Bangladesh
          </p>
        </div>

        {/* Login card */}
        <div style={{ background: 'white', border: '1px solid var(--sand-300)', borderRadius: 8, padding: 32 }}>
          {/* Mode toggle */}
          <div style={{ display: 'flex', marginBottom: 24, borderBottom: '1px solid var(--sand-200)' }}>
            <button
              onClick={() => { setMode('login'); setError(''); }}
              style={{
                flex: 1, padding: '12px 0', border: 'none', background: 'none', cursor: 'pointer',
                borderBottom: mode === 'login' ? '2px solid var(--jol-600)' : '2px solid transparent',
                color: mode === 'login' ? 'var(--jol-600)' : 'var(--ink-700)',
                fontWeight: 600, fontSize: 14, fontFamily: 'var(--font-body)',
              }}
            >
              Sign In
            </button>
            <button
              onClick={() => { setMode('otp'); setError(''); }}
              style={{
                flex: 1, padding: '12px 0', border: 'none', background: 'none', cursor: 'pointer',
                borderBottom: mode === 'otp' ? '2px solid var(--jol-600)' : '2px solid transparent',
                color: mode === 'otp' ? 'var(--jol-600)' : 'var(--ink-700)',
                fontWeight: 600, fontSize: 14, fontFamily: 'var(--font-body)',
              }}
            >
              Phone OTP
            </button>
          </div>

          {mode === 'login' ? (
            <>
              {/* Username/Password login */}
              <div style={{ marginBottom: 16 }}>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 600, color: 'var(--ink-700)', fontFamily: 'var(--font-body)', marginBottom: 6 }}>Username</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, border: '1px solid var(--sand-300)', borderRadius: 4, padding: '10px 12px', background: 'var(--paper-50)' }}>
                  <User size={16} style={{ color: 'var(--sand-400)' }} />
                  <input
                    type="text" value={username} onChange={e => setUsername(e.target.value)}
                    placeholder="seller or buyer"
                    style={{ flex: 1, border: 'none', background: 'none', outline: 'none', fontSize: 14, fontFamily: 'var(--font-body)' }}
                  />
                </div>
              </div>

              <div style={{ marginBottom: 16 }}>
                <label style={{ display: 'block', fontSize: 13, fontWeight: 600, color: 'var(--ink-700)', fontFamily: 'var(--font-body)', marginBottom: 6 }}>Password</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, border: '1px solid var(--sand-300)', borderRadius: 4, padding: '10px 12px', background: 'var(--paper-50)' }}>
                  <Lock size={16} style={{ color: 'var(--sand-400)' }} />
                  <input
                    type="password" value={password} onChange={e => setPassword(e.target.value)}
                    placeholder="Enter password"
                    style={{ flex: 1, border: 'none', background: 'none', outline: 'none', fontSize: 14, fontFamily: 'var(--font-body)' }}
                  />
                </div>
              </div>

              {error && <div style={{ padding: '8px 12px', background: '#FEF2F2', borderRadius: 4, fontSize: 13, color: '#B91C1C', marginBottom: 16 }}>{error}</div>}

              <button
                onClick={handleLogin}
                disabled={loading}
                style={{
                  width: '100%', padding: 14, background: 'var(--jol-600)', color: 'white', border: 'none',
                  borderRadius: 4, fontSize: 14, fontWeight: 600, fontFamily: 'var(--font-body)',
                  cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
                  opacity: loading ? 0.6 : 1,
                }}
              >
                {loading ? <Loader2 size={16} className="animate-spin" /> : <ArrowRight size={16} />}
                Sign In
              </button>

              <div style={{ marginTop: 16, padding: 12, background: 'var(--jol-100)', borderRadius: 4 }}>
                <p style={{ fontSize: 12, color: 'var(--ink-700)', fontFamily: 'var(--font-body)', margin: 0, lineHeight: 1.6 }}>
                  <strong>Demo credentials:</strong><br />
                  <span style={{ fontFamily: 'var(--font-mono)' }}>seller</span> / <span style={{ fontFamily: 'var(--font-mono)' }}>12345678</span> → Seller account<br />
                  <span style={{ fontFamily: 'var(--font-mono)' }}>buyer</span> / <span style={{ fontFamily: 'var(--font-mono)' }}>12345678</span> → Buyer account
                </p>
              </div>
            </>
          ) : (
            <>
              {/* OTP login */}
              {otpStep === 'phone' ? (
                <div style={{ marginBottom: 16 }}>
                  <label style={{ display: 'block', fontSize: 13, fontWeight: 600, color: 'var(--ink-700)', fontFamily: 'var(--font-body)', marginBottom: 6 }}>Phone Number</label>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, border: '1px solid var(--sand-300)', borderRadius: 4, padding: '10px 12px', background: 'var(--paper-50)' }}>
                    <Phone size={16} style={{ color: 'var(--sand-400)' }} />
                    <input
                      type="tel" value={otpPhone} onChange={e => setOtpPhone(e.target.value)}
                      placeholder="+8801700000001"
                      style={{ flex: 1, border: 'none', background: 'none', outline: 'none', fontSize: 14, fontFamily: 'var(--font-body)' }}
                    />
                  </div>
                </div>
              ) : (
                <div style={{ marginBottom: 16 }}>
                  <label style={{ display: 'block', fontSize: 13, fontWeight: 600, color: 'var(--ink-700)', fontFamily: 'var(--font-body)', marginBottom: 6 }}>Enter OTP Code</label>
                  <input
                    type="text" value={otpCode} onChange={e => setOtpCode(e.target.value)}
                    placeholder="123456"
                    maxLength={6}
                    style={{ width: '100%', border: '1px solid var(--sand-300)', borderRadius: 4, padding: '12px', fontSize: 18, fontFamily: 'var(--font-mono)', textAlign: 'center', letterSpacing: 4 }}
                  />
                  <p style={{ fontSize: 11, color: 'var(--sand-400)', fontFamily: 'var(--font-mono)', marginTop: 6 }}>Demo: use any 6-digit code</p>
                </div>
              )}

              {error && <div style={{ padding: '8px 12px', background: '#FEF2F2', borderRadius: 4, fontSize: 13, color: '#B91C1C', marginBottom: 16 }}>{error}</div>}

              <button
                onClick={otpStep === 'phone' ? handleOtpStart : handleOtpVerify}
                disabled={loading}
                style={{
                  width: '100%', padding: 14, background: 'var(--jol-600)', color: 'white', border: 'none',
                  borderRadius: 4, fontSize: 14, fontWeight: 600, fontFamily: 'var(--font-body)',
                  cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
                  opacity: loading ? 0.6 : 1,
                }}
              >
                {loading ? <Loader2 size={16} className="animate-spin" /> : <ArrowRight size={16} />}
                {otpStep === 'phone' ? 'Send OTP' : 'Verify & Sign In'}
              </button>
            </>
          )}
        </div>

        {/* Footer */}
        <div style={{ textAlign: 'center', marginTop: 24, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
          <Shield size={12} style={{ color: 'var(--jol-600)' }} />
          <span style={{ fontSize: 12, color: 'var(--sand-400)', fontFamily: 'var(--font-body)' }}>
            AES-256-GCM encrypted · SafeView verified
          </span>
        </div>
      </div>
    </div>
  );
}
