'use client';
import { useState, useEffect } from 'react';
import { User, Phone, Shield, Home, Calendar, MessageCircle, LogOut, ChevronRight, Bell, Lock } from 'lucide-react';

export default function AccountPage() {
  const [user, setUser] = useState<any>(null);
  useEffect(() => {
    const saved = localStorage.getItem('dorja_user');
    if (saved) setUser(JSON.parse(saved));
  }, []);

  if (!user) {
    return (
      <div style={{ padding: '60px 40px', textAlign: 'center' }}>
        <User size={48} style={{ color: 'var(--sand-300)', marginBottom: 16 }} />
        <h2 style={{ fontFamily: 'var(--font-display)', marginBottom: 8 }}>Not signed in</h2>
        <p style={{ color: 'var(--ink-800)', fontSize: 14, marginBottom: 16 }}>Sign in to manage your account and listings.</p>
        <a href="/auth" className="btn-primary">Sign In</a>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 640, margin: '0 auto' }}>
      <div className="profile-header">
        <div className="profile-avatar">{user.name?.charAt(0) || 'U'}</div>
        <div>
          <h2 style={{ fontFamily: 'var(--font-display)', margin: 0 }}>{user.name || 'User'}</h2>
          <p style={{ fontSize: 14, color: 'var(--ink-800)', margin: 0, display: 'flex', alignItems: 'center', gap: 6 }}>
            <Phone size={12} /> {user.phone || 'No phone'}
          </p>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, marginTop: 4, padding: '2px 8px', background: user.role === 'SELLER' ? 'var(--jol-100)' : 'var(--amber-100)', borderRadius: 4, fontSize: 11, fontWeight: 600, color: user.role === 'SELLER' ? 'var(--jol-700)' : 'var(--amber-500)' }}>
            <Shield size={10} /> {user.role || 'BUYER'}
          </span>
        </div>
      </div>

      <div className="profile-section">
        <h2>Property Activity</h2>
        {[
          { icon: Home, label: 'My Listings', desc: 'Properties you own', href: '/my-listings' },
          { icon: Calendar, label: 'My Visits', desc: 'Scheduled SafeView appointments', href: '/visits' },
          { icon: MessageCircle, label: 'Messages', desc: 'Protected conversations', href: '/inbox' },
        ].map(item => (
          <a key={item.label} href={item.href} className="profile-row" style={{ textDecoration: 'none', color: 'inherit' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <item.icon size={18} style={{ color: 'var(--sand-400)' }} />
              <div><div className="profile-row-label">{item.label}</div><div style={{ fontSize: 12, color: 'var(--sand-400)' }}>{item.desc}</div></div>
            </div>
            <ChevronRight size={16} style={{ color: 'var(--sand-300)' }} />
          </a>
        ))}
      </div>

      <div className="profile-section">
        <h2>Security & Privacy</h2>
        {[
          { icon: Shield, label: 'Identity Verification', desc: 'Verified', action: 'View' },
          { icon: Lock, label: 'Trusted Contacts', desc: '0 contacts', action: 'Manage' },
          { icon: Bell, label: 'Safety Alerts', desc: 'Enabled', action: 'Configure' },
        ].map(item => (
          <div key={item.label} className="profile-row">
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <item.icon size={18} style={{ color: 'var(--sand-400)' }} />
              <div><div className="profile-row-label">{item.label}</div><div style={{ fontSize: 12, color: 'var(--sand-400)' }}>{item.desc}</div></div>
            </div>
            <button className="profile-row-action">{item.action}</button>
          </div>
        ))}
      </div>

      <div className="profile-section">
        <button onClick={() => { localStorage.removeItem('dorja_user'); localStorage.removeItem('dorja_token'); window.location.href = '/auth'; }}
          style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '10px 16px', background: 'var(--red-100)', border: 'none', borderRadius: 4, color: 'var(--red-600)', fontWeight: 600, fontSize: 14, cursor: 'pointer', width: '100%', justifyContent: 'center' }}>
          <LogOut size={16} /> Sign Out
        </button>
      </div>
    </div>
  );
}
