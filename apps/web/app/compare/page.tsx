'use client';
import { useState } from 'react';
import { Camera, Check, X as XIcon, Scale } from 'lucide-react';

const ROOM_TYPES = ['Living room', 'Kitchen', 'Bedroom', 'Balcony', 'Bathroom'];
const DEMO_PROPERTIES = [
  { id: 'a', title: 'Mirpur 11', pulse: '2h ago', captured: '14 Aug 2026', rooms: ['Drawing Room', 'Master Bedroom', 'Kitchen', 'Bathroom', 'Balcony'] },
  { id: 'b', title: 'Uttara Sector 7', pulse: '5h ago', captured: '12 Aug 2026', rooms: ['Living Room', 'Master Bedroom', 'Kids Room', 'Kitchen', 'Bathroom 1', 'Bathroom 2'] },
];

export default function ComparePage() {
  const [roomType, setRoomType] = useState('Living room');
  return (
    <div>
      <div style={{ padding: '16px 32px', borderBottom: '1px solid var(--sand-300)', display: 'flex', gap: 8, alignItems: 'center' }}>
        <Scale size={16} style={{ color: 'var(--ink-800)' }} />
        <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink-800)', marginRight: 8 }}>Compare:</span>
        {ROOM_TYPES.map(rt => (
          <button key={rt} onClick={() => setRoomType(rt)} className={`filter-chip ${rt === roomType ? 'active' : ''}`}>{rt}</button>
        ))}
      </div>
      <div className='twin-view'>
        {DEMO_PROPERTIES.map(prop => (
          <div key={prop.id} className='twin-canvas'>
            <div style={{ textAlign: 'center', color: 'white', padding: 24 }}>
              <div style={{ width: '100%', maxWidth: 500, height: 350, background: 'linear-gradient(135deg, #1a2a3a, #0B1F33)', borderRadius: 'var(--radius-card)', display: 'flex', alignItems: 'center', justifyContent: 'center', border: '1px solid rgba(255,255,255,0.1)', marginBottom: 16 }}>
                <div style={{ textAlign: 'center' }}>
                  <Camera size={36} style={{ color: 'rgba(255,255,255,0.3)', marginBottom: 8 }} />
                  <div style={{ fontSize: 14, color: 'var(--sand-300)' }}>{roomType} — {prop.title}</div>
                </div>
              </div>
              <div style={{ fontSize: 14, fontWeight: 600 }}>{prop.title}</div>
              <div style={{ fontSize: 12, color: 'var(--sand-300)', fontFamily: 'var(--font-mono)' }}>A / B</div>
            </div>
          </div>
        ))}
      </div>
      <div style={{ padding: '16px 32px', background: 'var(--paper-100)', borderTop: '1px solid var(--sand-300)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead><tr style={{ borderBottom: '1px solid var(--sand-300)' }}>
            <th style={{ textAlign: 'left', padding: '8px 12px', color: 'var(--ink-800)', fontWeight: 600 }}>Field</th>
            <th style={{ textAlign: 'left', padding: '8px 12px', color: 'var(--ink-800)', fontWeight: 600 }}>Property A</th>
            <th style={{ textAlign: 'left', padding: '8px 12px', color: 'var(--ink-800)', fontWeight: 600 }}>Property B</th>
          </tr></thead>
          <tbody>
            {[
              ['Live Pulse', `Confirmed ${DEMO_PROPERTIES[0].pulse}`, `Confirmed ${DEMO_PROPERTIES[1].pulse}`],
              ['Capture date', DEMO_PROPERTIES[0].captured, DEMO_PROPERTIES[1].captured],
              ['Room captured', 'Yes', 'Yes'],
              ['Balcony', 'Captured', 'Not captured'],
            ].map(([field, a, b]) => (
              <tr key={field} style={{ borderBottom: '1px solid var(--sand-300)' }}>
                <td style={{ padding: '8px 12px', fontWeight: 500 }}>{field}</td>
                <td style={{ padding: '8px 12px', fontFamily: 'var(--font-mono)', display: 'flex', alignItems: 'center', gap: 4 }}>{a === 'Yes' || a === 'Captured' ? <Check size={12} style={{ color: 'var(--leaf-600)' }} /> : a === 'Not captured' ? <XIcon size={12} style={{ color: 'var(--red-600)' }} /> : null}{a}</td>
                <td style={{ padding: '8px 12px', fontFamily: 'var(--font-mono)', display: 'flex', alignItems: 'center', gap: 4 }}>{b === 'Yes' || b === 'Captured' ? <Check size={12} style={{ color: 'var(--leaf-600)' }} /> : b === 'Not captured' ? <XIcon size={12} style={{ color: 'var(--red-600)' }} /> : null}{b}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
