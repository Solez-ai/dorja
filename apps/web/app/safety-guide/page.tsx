import { Shield, Lock, CheckCircle, Calendar, AlertTriangle, Phone, ArrowLeft } from 'lucide-react';

const FEATURES = [
  { icon: Lock, title: 'Encrypted Addresses', desc: 'Exact address is AES-256-GCM encrypted. Only visible through a confirmed SafeView appointment.' },
  { icon: CheckCircle, title: 'Identity Verification', desc: 'Sellers verify identity before listings go live. Buyer identity confirmed before viewings.' },
  { icon: Calendar, title: 'SafeView Appointments', desc: 'Scheduled appointments with one-time QR passes. Timed entry for maximum safety.' },
  { icon: AlertTriangle, title: 'Safety Check-in/out', desc: 'Timed check-in and check-out with automatic missed checkout alerts to trusted contacts.' },
  { icon: Phone, title: 'Trusted Contacts', desc: 'Add contacts who receive real-time alerts during your property viewings.' },
  { icon: Shield, title: 'Report Concerns', desc: 'Report suspicious listings or unsafe situations directly to DORJA safety team.' },
];

export default function SafetyGuidePage() {
  return (
    <div style={{ padding: '32px 40px', maxWidth: 800, margin: '0 auto' }}>
      <a href="/" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 13, color: 'var(--sand-400)', marginBottom: 20, textDecoration: 'none' }}>
        <ArrowLeft size={14} /> Back to Explore
      </a>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
        <Shield size={28} strokeWidth={1.8} style={{ color: 'var(--jol-600)' }} />
        <h1 style={{ fontFamily: 'var(--font-display)', margin: 0 }}>Safety Guide</h1>
      </div>
      <p style={{ color: 'var(--ink-800)', fontSize: 14, marginBottom: 32, lineHeight: 1.6 }}>
        How DORJA keeps property seekers safe in Bangladesh. Every feature is designed to protect
        your privacy, identity, and physical safety during property discovery.
      </p>
      <div>
        {FEATURES.map((item, i) => (
          <div key={i} className="safety-row">
            <div className="safety-icon">
              <item.icon size={22} strokeWidth={1.8} />
            </div>
            <div>
              <h3 style={{ fontFamily: 'var(--font-display)', marginBottom: 4, fontSize: 15 }}>{item.title}</h3>
              <p style={{ fontSize: 13, color: 'var(--ink-800)', lineHeight: 1.5 }}>{item.desc}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
