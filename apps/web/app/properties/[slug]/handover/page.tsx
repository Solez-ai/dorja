'use client';
import { useState, useEffect } from 'react';
import {
  ArrowLeft, Shield, Clock, Calendar, FileText, AlertTriangle, CheckCircle,
  Camera, MessageCircle, ChevronDown, ChevronRight, Download, Plus, Eye,
  Package, Target, Zap, Hammer, CreditCard, Ruler, Car, Wrench, Plug, FileCheck,
} from 'lucide-react';

const API = 'http://localhost:4000';

const CATEGORY_ICONS: Record<string, any> = {
  HANDOVER_DATE: Calendar, PAYMENT_TERM: CreditCard, PRICE_OR_FEE: CreditCard,
  UNIT_SIZE_OR_LAYOUT: Ruler, PARKING: Car, FITMENT_OR_MATERIAL: Hammer,
  UTILITY_OR_SERVICE: Plug, REGISTRATION_OR_DOCUMENT: FileCheck, OTHER: Target,
};

const STATUS_COLORS: Record<string, { bg: string; color: string; label: string }> = {
  PENDING_ACKNOWLEDGEMENT: { bg: 'var(--amber-100)', color: 'var(--amber-500)', label: 'Pending acknowledgement' },
  ACKNOWLEDGED: { bg: 'var(--jol-100)', color: 'var(--jol-700)', label: 'Acknowledged' },
  EVIDENCE_SUBMITTED: { bg: 'var(--jol-100)', color: 'var(--jol-700)', label: 'Evidence submitted' },
  CHANGE_PROPOSED: { bg: 'var(--amber-100)', color: 'var(--amber-500)', label: 'Change proposed' },
  REMEDY_OPEN: { bg: 'var(--red-100)', color: 'var(--red-600)', label: 'Remedy open' },
  REMEDY_IN_PROGRESS: { bg: 'var(--amber-100)', color: 'var(--amber-500)', label: 'Remedy in progress' },
  RESOLVED: { bg: 'var(--leaf-100)', color: 'var(--leaf-600)', label: 'Resolved' },
  CONTESTED: { bg: 'var(--red-100)', color: 'var(--red-600)', label: 'Contested' },
  DRAFT: { bg: 'var(--paper-100)', color: 'var(--sand-400)', label: 'Draft' },
};

const REMEDY_STATUS: Record<string, { bg: string; color: string; label: string }> = {
  OPEN: { bg: 'var(--amber-100)', color: 'var(--amber-500)', label: 'Open' },
  ACKNOWLEDGED: { bg: 'var(--jol-100)', color: 'var(--jol-700)', label: 'Acknowledged' },
  REMEDY_PROPOSED: { bg: 'var(--amber-100)', color: 'var(--amber-500)', label: 'Remedy proposed' },
  IN_PROGRESS: { bg: 'var(--amber-100)', color: 'var(--amber-500)', label: 'In progress' },
  READY_FOR_REVIEW: { bg: 'var(--jol-100)', color: 'var(--jol-700)', label: 'Ready for review' },
  RESOLVED: { bg: 'var(--leaf-100)', color: 'var(--leaf-600)', label: 'Resolved' },
  CONTESTED: { bg: 'var(--red-100)', color: 'var(--red-600)', label: 'Contested' },
  CLOSED: { bg: 'var(--paper-100)', color: 'var(--sand-400)', label: 'Closed' },
};

function timeAgo(d: string) {
  const m = Math.floor((Date.now() - new Date(d).getTime()) / 60000);
  if (m < 60) return m + 'm ago';
  const h = Math.floor(m / 60);
  if (h < 24) return h + 'h ago';
  return Math.floor(h / 24) + 'd ago';
}

function daysUntil(d: string) {
  const diff = Math.ceil((new Date(d).getTime() - Date.now()) / (1000 * 60 * 60 * 24));
  if (diff < 0) return `${Math.abs(diff)}d overdue`;
  if (diff === 0) return 'Today';
  return `${diff}d remaining`;
}

export default function HandoverPassportPage({ params }: { params: Promise<{ slug: string }> }) {
  const [passport, setPassport] = useState<any>(null);
  const [listingId, setListingId] = useState('');
  const [loading, setLoading] = useState(true);
  const [selectedPromise, setSelectedPromise] = useState<any>(null);
  const [tab, setTab] = useState<'promises' | 'remedies' | 'timeline' | 'evidence'>('promises');
  const [showAddPromise, setShowAddPromise] = useState(false);
  const [promiseForm, setPromiseForm] = useState({ category: 'HANDOVER_DATE', title: '', originalPromiseText: '', sourceReferenceLabel: '', promisedDate: '' });

  useEffect(() => {
    params.then(async ({ slug }) => {
      // Auto-login if no token
      let token = localStorage.getItem('dorja_token');
      if (!token) {
        try {
          await fetch(`${API}/v1/auth/otp/start`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({phone:'+8801700000002'}) });
          const vr = await fetch(`${API}/v1/auth/otp/verify`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({phone:'+8801700000002',code:'123456'}) });
          const vd = await vr.json();
          if (vd.data?.accessToken) {
            token = vd.data.accessToken;
            localStorage.setItem('dorja_token', token!);
            localStorage.setItem('dorja_user', JSON.stringify({id:'dev',name:'Developer',phone:'+8801700000002',role:'DEVELOPER_REPRESENTATIVE'}));
          }
        } catch {}
      }
      fetch(`http://localhost:4000/v1/listings/${slug}`, { cache: 'no-store' })
        .then(r => r.ok ? r.json() : null)
        .then(async d => {
          if (!d?.data) { setLoading(false); return; }
          const lid = d.data.listing.id;
          setListingId(lid);
          if (!token) { setLoading(false); return; }
          const hr = await fetch(`${API}/v1/listings/${lid}/handover`, { headers: { Authorization: `Bearer ${token}` } });
          const hd = await hr.json();
          if (hd.data) {
            // Fetch promises, remedies, timeline
            const [pr, rr, tr, er] = await Promise.all([
              fetch(`${API}/v1/handover/${hd.data.id}/promises`, { headers: { Authorization: `Bearer ${token}` } }).then(r => r.json()),
              fetch(`${API}/v1/handover/${hd.data.id}/remedies`, { headers: { Authorization: `Bearer ${token}` } }).then(r => r.json()),
              fetch(`${API}/v1/handover/${hd.data.id}/timeline`, { headers: { Authorization: `Bearer ${token}` } }).then(r => r.json()),
              fetch(`${API}/v1/handover/${hd.data.id}/evidence`, { headers: { Authorization: `Bearer ${token}` } }).then(r => r.json()),
            ]);
            setPassport({ ...hd.data, promises: pr.data || [], remedies: rr.data || [], events: tr.data || [], evidence: er.data || [] });
          }
          setLoading(false);
        })
        .catch(() => setLoading(false));
    });
  }, [params]);

  const addPromise = async () => {
    const token = localStorage.getItem('dorja_token');
    if (!token || !passport) return;
    await fetch(`${API}/v1/handover/${passport.id}/promises`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(promiseForm),
    });
    setShowAddPromise(false);
    setPromiseForm({ category: 'HANDOVER_DATE', title: '', originalPromiseText: '', sourceReferenceLabel: '', promisedDate: '' });
    // Refresh
    const pr = await fetch(`${API}/v1/handover/${passport.id}/promises`, { headers: { Authorization: `Bearer ${token}` } }).then(r => r.json());
    setPassport({ ...passport, promises: pr.data || [] });
  };

  if (loading) return <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', color: 'var(--sand-400)' }}>Loading handover passport...</div>;
  if (!passport) return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100vh', gap: 16 }}>
      <Package size={48} style={{ color: 'var(--sand-300)' }} />
      <h2>No handover passport</h2>
      <p style={{ color: 'var(--sand-400)' }}>This property does not have a handover passport yet.</p>
      <a href={`/properties/${params ? '' : ''}`} className="btn-primary">Back to Property</a>
    </div>
  );

  const resolvedCount = passport.promises?.filter((p: any) => p.currentStatus === 'RESOLVED' || p.currentStatus === 'ACKNOWLEDGED').length || 0;
  const totalCount = passport.promises?.length || 0;

  const statusStyle = (s: string) => STATUS_COLORS[s] || STATUS_COLORS.DRAFT;
  const remedyStyle = (s: string) => REMEDY_STATUS[s] || REMEDY_STATUS.OPEN;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: '24px 32px' }}>
      {/* Back link */}
      <a href={`/properties/${params ? '' : ''}`} style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 13, color: 'var(--sand-400)', marginBottom: 20, textDecoration: 'none' }}>
        <ArrowLeft size={14} /> Back to Property
      </a>

      {/* Passport Header */}
      <div style={{ marginBottom: 32 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
          <Package size={28} strokeWidth={1.8} style={{ color: 'var(--jol-600)' }} />
          <h1 style={{ fontFamily: 'var(--font-display)', margin: 0 }}>Handover Passport</h1>
        </div>
        <p style={{ color: 'var(--ink-800)', fontSize: 14, marginBottom: 16 }}>
          Promise → Proof → Remedy → Evidence Pack
        </p>

        {/* Status strip */}
        <div style={{ display: 'flex', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ padding: '6px 14px', background: 'var(--jol-100)', border: '1px solid var(--jol-600)', borderRadius: 'var(--radius-stamp)', fontFamily: 'var(--font-mono)', fontSize: 12, fontWeight: 600, color: 'var(--jol-700)', display: 'flex', alignItems: 'center', gap: 6 }}>
            <Shield size={12} /> {passport.status}
          </div>
          {passport.agreementDate && (
            <div style={{ fontSize: 13, color: 'var(--ink-800)', display: 'flex', alignItems: 'center', gap: 4 }}>
              <Calendar size={13} /> Agreement: {new Date(passport.agreementDate).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })}
            </div>
          )}
          <div style={{ fontSize: 13, color: 'var(--ink-800)', display: 'flex', alignItems: 'center', gap: 4 }}>
            <Target size={13} /> Promise completion: {resolvedCount} of {totalCount} resolved
          </div>
          <div style={{ fontSize: 13, color: 'var(--sand-400)', display: 'flex', alignItems: 'center', gap: 4 }}>
            <Clock size={13} /> Latest activity: {timeAgo(passport.latestActivityAt || passport.createdAt)}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 4, borderBottom: '2px solid var(--sand-300)', marginBottom: 24 }}>
        {[
          { key: 'promises', label: 'Promise Line', icon: Zap },
          { key: 'remedies', label: 'Remedy Clock', icon: Hammer },
          { key: 'timeline', label: 'Timeline', icon: Clock },
          { key: 'evidence', label: 'Evidence Pack', icon: Package },
        ].map(t => (
          <button key={t.key} onClick={() => setTab(t.key as any)} style={{
            display: 'flex', alignItems: 'center', gap: 6, padding: '12px 20px',
            background: tab === t.key ? 'var(--jol-100)' : 'transparent',
            border: 'none', borderBottom: tab === t.key ? '2px solid var(--jol-600)' : '2px solid transparent',
            color: tab === t.key ? 'var(--jol-700)' : 'var(--sand-400)',
            fontFamily: 'var(--font-display)', fontWeight: 600, fontSize: 14, cursor: 'pointer',
            marginBottom: -2, transition: 'all 150ms',
          }}>
            <t.icon size={16} /> {t.label}
            {t.key === 'promises' && <span style={{ background: 'var(--paper-100)', padding: '1px 6px', borderRadius: 8, fontSize: 11 }}>{totalCount}</span>}
            {t.key === 'remedies' && <span style={{ background: 'var(--red-100)', padding: '1px 6px', borderRadius: 8, fontSize: 11 }}>{passport.remedies?.length || 0}</span>}
          </button>
        ))}
      </div>

      {/* Promise Line Tab */}
      {tab === 'promises' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ fontFamily: 'var(--font-display)', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Zap size={18} /> Promise Line
            </h3>
            <button onClick={() => setShowAddPromise(!showAddPromise)} className="btn-primary" style={{ fontSize: 13, padding: '6px 14px' }}>
              <Plus size={14} /> Add Promise
            </button>
          </div>

          {/* Add Promise Form */}
          {showAddPromise && (
            <div style={{ background: 'white', border: '1px solid var(--sand-300)', borderRadius: 8, padding: 20, marginBottom: 20 }}>
              <h4 style={{ marginBottom: 12, fontFamily: 'var(--font-display)' }}>New Promise</h4>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 12 }}>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 4 }}>Category</label>
                  <select value={promiseForm.category} onChange={e => setPromiseForm({ ...promiseForm, category: e.target.value })} className="form-select" style={{ padding: '8px 10px', fontSize: 13 }}>
                    {Object.keys(CATEGORY_ICONS).map(c => <option key={c} value={c}>{c.replace(/_/g, ' ').toLowerCase()}</option>)}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 4 }}>Promised Date</label>
                  <input type="date" value={promiseForm.promisedDate} onChange={e => setPromiseForm({ ...promiseForm, promisedDate: e.target.value })} className="form-input" style={{ padding: '8px 10px', fontSize: 13 }} />
                </div>
              </div>
              <div style={{ marginBottom: 12 }}>
                <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 4 }}>Short Title</label>
                <input value={promiseForm.title} onChange={e => setPromiseForm({ ...promiseForm, title: e.target.value })} className="form-input" placeholder="e.g. Handover by 30 June 2026" style={{ padding: '8px 10px', fontSize: 13 }} />
              </div>
              <div style={{ marginBottom: 12 }}>
                <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 4 }}>What was promised?</label>
                <textarea value={promiseForm.originalPromiseText} onChange={e => setPromiseForm({ ...promiseForm, originalPromiseText: e.target.value })} className="form-input" rows={3} placeholder="Describe the promise in detail..." style={{ padding: '8px 10px', fontSize: 13, resize: 'vertical' }} />
              </div>
              <div style={{ marginBottom: 12 }}>
                <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 4 }}>Source Reference</label>
                <input value={promiseForm.sourceReferenceLabel} onChange={e => setPromiseForm({ ...promiseForm, sourceReferenceLabel: e.target.value })} className="form-input" placeholder="e.g. Booking agreement, clause 3.2" style={{ padding: '8px 10px', fontSize: 13 }} />
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button onClick={addPromise} className="btn-primary" style={{ fontSize: 13 }}>Submit Promise</button>
                <button onClick={() => setShowAddPromise(false)} className="btn-outline" style={{ fontSize: 13 }}>Cancel</button>
              </div>
            </div>
          )}

          {/* Promise Cards */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {passport.promises?.map((p: any) => {
              const CatIcon = CATEGORY_ICONS[p.category] || Target;
              const st = statusStyle(p.currentStatus);
              const isSelected = selectedPromise?.id === p.id;
              return (
                <div key={p.id} onClick={() => setSelectedPromise(isSelected ? null : p)} style={{
                  background: 'white', border: `1px solid ${isSelected ? 'var(--jol-600)' : 'var(--sand-300)'}`,
                  borderRadius: 8, padding: 20, cursor: 'pointer', transition: 'all 150ms',
                }}>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 12 }}>
                    <div style={{ width: 36, height: 36, borderRadius: 8, background: 'var(--paper-100)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                      <CatIcon size={18} style={{ color: 'var(--ink-800)' }} />
                    </div>
                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                        <h4 style={{ margin: 0, fontSize: 15 }}>{p.title}</h4>
                        <span style={{ padding: '2px 8px', background: st.bg, color: st.color, borderRadius: 4, fontSize: 10, fontWeight: 700, textTransform: 'uppercase' }}>{st.label}</span>
                      </div>
                      <div style={{ fontSize: 12, color: 'var(--sand-400)', display: 'flex', gap: 12 }}>
                        <span>{p.category.replace(/_/g, ' ')}</span>
                        {p.promisedDate && <span>Due: {new Date(p.promisedDate).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })}</span>}
                        <span>{timeAgo(p.createdAt)}</span>
                      </div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--sand-400)', fontSize: 12 }}>
                      {p.evidence?.length > 0 && <><FileText size={12} /> {p.evidence.length}</>}
                      {p.remedies?.length > 0 && <><AlertTriangle size={12} /> {p.remedies.length}</>}
                      {isSelected ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                    </div>
                  </div>

                  {/* Expanded detail */}
                  {isSelected && (
                    <div style={{ borderTop: '1px solid var(--paper-200)', paddingTop: 12 }}>
                      <div style={{ background: 'var(--paper-50)', padding: 12, borderRadius: 4, marginBottom: 12, fontSize: 13, lineHeight: 1.6, color: 'var(--ink-800)' }}>
                        {p.originalPromiseText}
                      </div>
                      {p.sourceReferenceLabel && (
                        <div style={{ fontSize: 12, color: 'var(--sand-400)', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 4 }}>
                          <FileText size={12} /> Source: {p.sourceReferenceLabel}
                        </div>
                      )}
                      {p.acknowledgedAt && (
                        <div style={{ fontSize: 12, color: 'var(--jol-700)', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 4 }}>
                          <CheckCircle size={12} /> Acknowledged {timeAgo(p.acknowledgedAt)}
                        </div>
                      )}
                      {/* Evidence items */}
                      {p.evidence?.length > 0 && (
                        <div style={{ marginTop: 12 }}>
                          <div style={{ fontSize: 11, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5, color: 'var(--sand-400)', marginBottom: 6 }}>Evidence</div>
                          {p.evidence.map((e: any) => (
                            <div key={e.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 0', borderTop: '1px solid var(--paper-200)', fontSize: 13 }}>
                              <Camera size={14} style={{ color: 'var(--sand-400)' }} />
                              <div style={{ flex: 1 }}>
                                <div style={{ fontWeight: 500 }}>{e.label}</div>
                                {e.visibleNote && <div style={{ fontSize: 12, color: 'var(--sand-400)', marginTop: 2 }}>{e.visibleNote}</div>}
                              </div>
                              <span style={{ fontSize: 11, color: 'var(--sand-400)' }}>{timeAgo(e.createdAt)}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Remedy Clock Tab */}
      {tab === 'remedies' && (
        <div>
          <h3 style={{ fontFamily: 'var(--font-display)', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Hammer size={18} /> Remedy Clock
          </h3>
          {passport.remedies?.length === 0 ? (
            <div style={{ textAlign: 'center', padding: 40, background: 'white', border: '1px solid var(--sand-300)', borderRadius: 8 }}>
              <Hammer size={32} style={{ color: 'var(--sand-300)', marginBottom: 8 }} />
              <p style={{ color: 'var(--sand-400)' }}>No remedy issues tracked.</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {passport.remedies?.map((r: any) => {
                const rs = remedyStyle(r.status);
                const linked = passport.promises?.find((p: any) => p.id === r.linkedPromiseId);
                return (
                  <div key={r.id} style={{ background: 'white', border: '1px solid var(--sand-300)', borderRadius: 8, padding: 20 }}>
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 12 }}>
                      <div style={{ width: 36, height: 36, borderRadius: 8, background: 'var(--red-100)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                        <AlertTriangle size={18} style={{ color: 'var(--red-600)' }} />
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                          <h4 style={{ margin: 0, fontSize: 15 }}>{r.title}</h4>
                          <span style={{ padding: '2px 8px', background: rs.bg, color: rs.color, borderRadius: 4, fontSize: 10, fontWeight: 700, textTransform: 'uppercase' }}>{rs.label}</span>
                          {r.priority === 'HIGH' && (
                            <span style={{ padding: '2px 6px', background: 'var(--red-100)', color: 'var(--red-600)', borderRadius: 4, fontSize: 10, fontWeight: 700 }}>HIGH</span>
                          )}
                        </div>
                        {linked && (
                          <div style={{ fontSize: 12, color: 'var(--sand-400)', display: 'flex', alignItems: 'center', gap: 4, marginBottom: 4 }}>
                            <Zap size={11} /> Linked to: {linked.title}
                          </div>
                        )}
                      </div>
                    </div>
                    {r.description && (
                      <div style={{ fontSize: 13, color: 'var(--ink-800)', lineHeight: 1.5, marginBottom: 12, padding: '10px 12px', background: 'var(--paper-50)', borderRadius: 4 }}>
                        {r.description}
                      </div>
                    )}
                    <div style={{ display: 'flex', gap: 16, fontSize: 12, color: 'var(--sand-400)' }}>
                      <span>Reported: {timeAgo(r.createdAt)}</span>
                      {r.proposedCompletionAt && (
                        <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: r.status === 'RESOLVED' ? 'var(--leaf-600)' : 'var(--amber-500)' }}>
                          <Clock size={12} /> {daysUntil(r.proposedCompletionAt)}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Timeline Tab */}
      {tab === 'timeline' && (
        <div>
          <h3 style={{ fontFamily: 'var(--font-display)', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Clock size={18} /> Activity Timeline
          </h3>
          <div style={{ position: 'relative', paddingLeft: 24 }}>
            <div style={{ position: 'absolute', left: 7, top: 0, bottom: 0, width: 2, background: 'var(--sand-300)' }} />
            {passport.events?.map((e: any, i: number) => (
              <div key={e.id} style={{ position: 'relative', marginBottom: 20, paddingLeft: 24 }}>
                <div style={{ position: 'absolute', left: 0, top: 4, width: 16, height: 16, borderRadius: '50%', background: 'var(--jol-600)', border: '2px solid white', zIndex: 1 }} />
                <div style={{ fontSize: 13, fontWeight: 500, marginBottom: 2 }}>{e.eventType.replace(/_/g, ' ').toLowerCase()}</div>
                <div style={{ fontSize: 12, color: 'var(--sand-400)' }}>{timeAgo(e.createdAt)}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Evidence Pack Tab */}
      {tab === 'evidence' && (
        <div>
          <h3 style={{ fontFamily: 'var(--font-display)', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Package size={18} /> Evidence Pack
          </h3>
          <div style={{ background: 'white', border: '1px solid var(--sand-300)', borderRadius: 8, padding: 24 }}>
            <div style={{ background: 'var(--amber-100)', padding: 12, borderRadius: 4, marginBottom: 16, fontSize: 12, color: 'var(--ink-800)', lineHeight: 1.6 }}>
              <AlertTriangle size={14} style={{ verticalAlign: -2, marginRight: 4 }} />
              <strong>Disclaimer:</strong> This record is a chronological summary of information submitted through DORJA by the participating parties. It does not verify legal title, determine liability, certify construction quality, or replace professional legal or technical advice.
            </div>

            <div style={{ marginBottom: 16 }}>
              <h4 style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>Passport Summary</h4>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, fontSize: 13 }}>
                <div>Status: <strong>{passport.status}</strong></div>
                <div>Agreement: <strong>{passport.agreementDate ? new Date(passport.agreementDate).toLocaleDateString() : 'N/A'}</strong></div>
                <div>Promises: <strong>{totalCount}</strong></div>
                <div>Resolved: <strong>{resolvedCount}</strong></div>
                <div>Evidence items: <strong>{passport.evidence?.length || 0}</strong></div>
                <div>Remedy issues: <strong>{passport.remedies?.length || 0}</strong></div>
              </div>
            </div>

            <div style={{ marginBottom: 16 }}>
              <h4 style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>Promise Index</h4>
              {passport.promises?.map((p: any) => (
                <div key={p.id} style={{ padding: '8px 0', borderTop: '1px solid var(--paper-200)', fontSize: 13 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ fontWeight: 500 }}>{p.title}</span>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--sand-400)' }}>{p.currentStatus}</span>
                  </div>
                  {p.evidence?.length > 0 && (
                    <div style={{ fontSize: 12, color: 'var(--sand-400)', marginTop: 2 }}>
                      Evidence: {p.evidence.map((e: any) => e.label).join(', ')}
                    </div>
                  )}
                </div>
              ))}
            </div>

            <button onClick={() => window.print()} className="btn-primary" style={{ width: '100%' }}>
              <Download size={16} /> Export / Print Evidence Pack
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
