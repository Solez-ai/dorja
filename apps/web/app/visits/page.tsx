import { Calendar, MapPin, Clock, CheckCircle, ArrowRight } from 'lucide-react';

export default function VisitsPage() {
  return (
    <div style={{ padding: '24px 32px', maxWidth: 720, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 24 }}>
        <Calendar size={24} strokeWidth={1.8} />
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 24 }}>Visits</h1>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div className='appointment-card'>
          <div className='appointment-status upcoming'>UPCOMING</div>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 16, fontWeight: 600, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Clock size={16} /> TODAY · 4:30–5:00 PM
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 14, marginBottom: 8 }}>
            <MapPin size={14} /> Mirpur 11 · exact address unlocks in 43 min
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: 'var(--ink-800)', marginBottom: 4 }}>
            <CheckCircle size={12} /> Buyer: Identity confirmed
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: 'var(--ink-800)', marginBottom: 12 }}>
            <CheckCircle size={12} /> Host: Owner authority reviewed
          </div>
          <button className='btn-primary' style={{ fontSize: 13 }}>
            Open Viewing Pass <ArrowRight size={14} />
          </button>
        </div>
        <div className='appointment-card'>
          <div className='appointment-status completed'>COMPLETED</div>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 16, fontWeight: 600, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Clock size={16} /> YESTERDAY · 2:00–2:30 PM
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 14, marginBottom: 8 }}>
            <MapPin size={14} /> Uttara Sector 7 · Apartment 4B
          </div>
          <div style={{ fontSize: 12, color: 'var(--leaf-600)', display: 'flex', alignItems: 'center', gap: 4 }}>
            <CheckCircle size={12} /> Visit completed safely
          </div>
        </div>
        <div style={{ padding: '32px', textAlign: 'center', background: 'var(--paper-100)', borderRadius: 'var(--radius-card)', border: '1px solid var(--sand-300)' }}>
          <Calendar size={32} style={{ color: 'var(--sand-300)', marginBottom: 8 }} />
          <p style={{ fontSize: 13, color: 'var(--ink-800)' }}>Request a SafeView from a property page to schedule a visit.</p>
          <a href='/' className='btn-primary' style={{ marginTop: 12, display: 'inline-flex' }}>Explore Properties</a>
        </div>
      </div>
    </div>
  );
}
