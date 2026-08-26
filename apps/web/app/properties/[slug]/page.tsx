'use client';
import { useState, useEffect } from 'react';
import { Shield, CheckCircle, Lock, MapPin, ArrowLeft, MessageCircle, Send, Calendar, Clock, AlertTriangle, Star, Package } from 'lucide-react';
import { TourCanvas } from '../../../components/TourCanvas';

export default function PropertyPassportPage({ params }: { params: Promise<{ slug: string }> }) {
  const [passport, setPassport] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    params.then(({ slug }) => {
      fetch(`http://localhost:4000/v1/listings/${slug}`, { cache: 'no-store' })
        .then(r => r.ok ? r.json() : null)
        .then(d => { setPassport(d?.data); setLoading(false); })
        .catch(() => setLoading(false));
    });
  }, [params]);

  if (loading) return (
    <div style={{ display:'flex', alignItems:'center', justifyContent:'center', height:'100vh', color:'var(--sand-400)' }}>
      Loading passport...
    </div>
  );
  if (!passport) return (
    <div style={{ display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', height:'100vh', gap:16 }}>
      <h2>Property not found</h2>
      <a href="/" className="btn-primary">Back to Explore</a>
    </div>
  );

  const lp = passport.listing;
  const live = lp.livePulse?.status === 'AVAILABLE';

  return (
    <div className="passport-layout">
      {/* Left: Evidence Ledger */}
      <aside className="passport-evidence">
        <a href="/" style={{ display:'inline-flex', alignItems:'center', gap:6, fontSize:13, color:'var(--sand-400)', marginBottom:20, textDecoration:'none' }}>
          <ArrowLeft size={14} /> Back
        </a>

        <h2 style={{ fontFamily:'var(--font-display)', fontSize:20, marginBottom:24 }}>{lp.title}</h2>

        {/* Live Pulse */}
        <div style={{ marginBottom:24 }}>
          <div style={{ fontSize:11, textTransform:'uppercase', letterSpacing:1, color:'var(--ink-800)', marginBottom:8, fontWeight:600, display:'flex', alignItems:'center', gap:6 }}>
            <Clock size={12} /> LIVE PULSE
          </div>
          <div className={`pulse-bar ${live ? 'live' : 'stale'}`}>
            <span style={{ width:8, height:8, borderRadius:'50%', background: live ? 'var(--jol-600)' : 'var(--amber-500)', flexShrink:0 }} />
            {live ? 'Available — confirmed' : 'Unconfirmed availability'}
          </div>
        </div>

        {/* Reality Passport */}
        <div style={{ marginBottom:24 }}>
          <div style={{ fontSize:11, textTransform:'uppercase', letterSpacing:1, color:'var(--ink-800)', marginBottom:8, fontWeight:600, display:'flex', alignItems:'center', gap:6 }}>
            <Shield size={12} /> REALITY PASSPORT
          </div>
          <div style={{ padding:12, background:'white', borderRadius:'var(--radius-stamp)', border:'1px solid var(--sand-300)' }}>
            <div style={{ fontSize:14, fontWeight:600, marginBottom:4 }}>
              {passport.reality?.reviewLevel === 'SELLER_CAPTURED' ? 'Seller-captured' :
               passport.reality?.reviewLevel === 'AGENT_VERIFIED' ? 'Agent-verified' :
               passport.reality?.reviewLevel === 'EXPIRED' ? 'Capture expired' : 'Incomplete capture'}
            </div>
            {passport.reality?.capturedAt && (
              <div style={{ fontSize:12, color:'var(--ink-800)', fontFamily:'var(--font-mono)' }}>
                {new Date(passport.reality.capturedAt).toLocaleDateString('en-GB', { day:'numeric', month:'short', year:'numeric' })}
              </div>
            )}
            <div style={{ fontSize:13, marginTop:4 }}>
              Coverage: {passport.rooms.length - (passport.reality?.missingRoomLabels?.length || 0)} of {passport.rooms.length} rooms
            </div>
            {passport.reality?.missingRoomLabels?.length > 0 && (
              <div style={{ fontSize:12, color:'var(--amber-500)', marginTop:4, display:'flex', alignItems:'center', gap:4 }}>
                <AlertTriangle size={12} /> Not captured: {passport.reality.missingRoomLabels.join(', ')}
              </div>
            )}
          </div>
        </div>

        {/* Listing Authority */}
        <div style={{ marginBottom:24 }}>
          <div style={{ fontSize:11, textTransform:'uppercase', letterSpacing:1, color:'var(--ink-800)', marginBottom:8, fontWeight:600, display:'flex', alignItems:'center', gap:6 }}>
            <CheckCircle size={12} /> LISTING AUTHORITY
          </div>
          <div style={{ padding:12, background:'white', borderRadius:'var(--radius-stamp)', border:'1px solid var(--sand-300)' }}>
            <div style={{ fontSize:14, fontWeight:600, display:'flex', alignItems:'center', gap:6 }}>
              <CheckCircle size={14} style={{ color:'var(--leaf-600)' }} /> Owner authority reviewed
            </div>
            <div style={{ fontSize:11, color:'var(--ink-800)', marginTop:4 }}>This is not title certification.</div>
          </div>
        </div>

        {/* Location */}
        <div>
          <div style={{ fontSize:11, textTransform:'uppercase', letterSpacing:1, color:'var(--ink-800)', marginBottom:8, fontWeight:600, display:'flex', alignItems:'center', gap:6 }}>
            <MapPin size={12} /> LOCATION
          </div>
          <div style={{ padding:12, background:'white', borderRadius:'var(--radius-stamp)', border:'1px solid var(--sand-300)' }}>
            <div style={{ fontSize:14, fontWeight:600 }}>Approximate: {lp.publicArea}</div>
            <div style={{ fontSize:12, color:'var(--ink-800)', marginTop:4, display:'flex', alignItems:'center', gap:4 }}>
              <Lock size={12} /> Exact entry unlocks after confirmed SafeView
            </div>
          </div>
        </div>
      </aside>

      {/* Centre: Tour Canvas */}
      <div className="passport-tour">
        <TourCanvas passport={passport} />
      </div>

      {/* Right: Decision Panel */}
      <aside className="passport-decisions">
        <div style={{ marginBottom:8 }}>
          <div style={{ fontFamily:'var(--font-mono)', fontSize:28, fontWeight:700, color:'var(--ink-950)' }}>
            ৳{lp.priceAmount?.toLocaleString()}
          </div>
          <div style={{ fontSize:14, color:'var(--ink-800)', display:'flex', alignItems:'center', gap:4 }}>
            {lp.intent === 'RENT' ? 'per month' : 'proposed price'} · {lp.currency || 'BDT'}
          </div>
        </div>

        <button className="btn-primary" style={{ width:'100%' }}>
          <Star size={16} /> Add to Twin View
        </button>

        <button className="btn-secondary" style={{ width:'100%' }}>
          <MessageCircle size={16} /> Ask in protected chat
        </button>

        <button className="btn-secondary" style={{ width:'100%' }}>
          <Send size={16} /> Send structured offer
        </button>

        <button className="btn-primary" style={{ width:'100%', background:'var(--ink-950)' }}>
          <Calendar size={16} /> Request a SafeView
        </button>

        <a href={`/properties/${params ? '' : ''}/handover`} style={{ textDecoration: 'none', width: '100%' }}>
          <button className="btn-secondary" style={{ width:'100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
            <Package size={16} /> Open Handover Passport
          </button>
        </a>

        <div style={{ marginTop:'auto', padding:12, background:'var(--amber-100)', borderRadius:'var(--radius-stamp)', fontSize:12, color:'var(--ink-800)', display:'flex', gap:8, alignItems:'flex-start' }}>
          <AlertTriangle size={14} style={{ flexShrink:0, marginTop:1 }} />
          <span><strong>Safety reminder:</strong> Keep your phone number, money, and exact address private until SafeView confirms the visit.</span>
        </div>
      </aside>
    </div>
  );
}
