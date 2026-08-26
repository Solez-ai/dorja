'use client';
import { useState, useEffect } from 'react';
import { Star, MapPin, ArrowRight, Trash2 } from 'lucide-react';

export default function ShortlistPage() {
  const [items, setItems] = useState<any[]>([]);
  useEffect(() => {
    const s = localStorage.getItem('dorja_shortlist');
    if (s) setItems(JSON.parse(s));
  }, []);
  const removeItem = (idx: number) => {
    const next = items.filter((_: any, i: number) => i !== idx);
    setItems(next);
    localStorage.setItem('dorja_shortlist', JSON.stringify(next));
  };
  return (
    <div style={{ padding: '32px 40px', maxWidth: 900, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
        <Star size={24} strokeWidth={1.8} style={{ color: 'var(--amber-500)' }} />
        <h1 style={{ fontFamily: 'var(--font-display)' }}>Shortlist</h1>
      </div>
      <p style={{ color: 'var(--ink-800)', fontSize: 14, marginBottom: 24 }}>
        Properties you saved for later comparison.
      </p>
      {items.length === 0 ? (
        <div className="empty-state-clean">
          <Star size={48} style={{ color: 'var(--sand-300)', marginBottom: 12 }} strokeWidth={1} />
          <h3 style={{ color: 'var(--ink-800)', marginBottom: 4 }}>No saved properties</h3>
          <p style={{ fontSize: 14, color: 'var(--sand-400)' }}>
            Tap the star on any listing card to save it here.
          </p>
          <a href="/" className="btn-primary" style={{ marginTop: 16, display: 'inline-flex' }}>
            Explore Properties
          </a>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {items.map((l: any, i: number) => (
            <div key={i} className="shortlist-card">
              <a href={'/properties/' + l.slug} style={{ flex: 1, textDecoration: 'none', color: 'inherit', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div style={{ fontWeight: 600, fontSize: 15, marginBottom: 2 }}>{l.title}</div>
                  <div style={{ fontSize: 13, color: 'var(--ink-800)', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <MapPin size={12} /> {l.publicArea}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, fontSize: 16 }}>
                    ৳{l.priceAmount?.toLocaleString()}
                  </div>
                  <ArrowRight size={16} style={{ color: 'var(--sand-400)' }} />
                </div>
              </a>
              <button onClick={() => removeItem(i)} style={{ background: 'none', border: 'none', color: 'var(--sand-400)', cursor: 'pointer', padding: 8, marginLeft: 8 }}>
                <Trash2 size={16} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
