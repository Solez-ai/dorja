'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Shield, MessageCircle, Lock, Send, Search, MapPin, ChevronRight } from 'lucide-react';

const API_URL = 'http://localhost:4000';

interface Conversation {
  id: string;
  listingId: string;
  status: string;
  lastMessageAt: string;
  listing: { id: string; title: string; slug: string; publicArea: string };
  seeker: { id: string; displayName: string; primaryRole: string };
  host: { id: string; displayName: string; primaryRole: string };
  messages: Array<{ safePreview: string; createdAt: string; senderUserId: string }>;
}

export default function InboxPage() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        // Always get a fresh token for demo
        await fetch(API_URL + '/v1/auth/otp/start', {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ phone: '+8801700000001' }),
        });
        const vr = await fetch(API_URL + '/v1/auth/otp/verify', {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ phone: '+8801700000001', code: '123456' }),
        });
        const vd = await vr.json();
        const token = vd.data?.accessToken || '';
        if (token) {
          try { localStorage.setItem('dorja_token', token); } catch {}
          const res = await fetch(API_URL + '/v1/chat/conversations', {
            headers: { Authorization: 'Bearer ' + token },
          });
          const data = await res.json();
          if (data.data) setConversations(data.data);
        }
      } catch (e) { console.error('Chat load error:', e); }
      setLoading(false);
    };
    load();
  }, []);

  const filtered = conversations.filter(c =>
    !search || c.listing?.title?.toLowerCase().includes(search.toLowerCase()) ||
    c.host?.displayName?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div style={{ minHeight: '100vh', display: 'flex' }}>
      {/* Conversation List */}
      <div style={{ width: 360, borderRight: '1px solid var(--sand-300)', display: 'flex', flexDirection: 'column', background: 'var(--paper-50)' }}>
        <div style={{ padding: 16, borderBottom: '1px solid var(--sand-300)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <MessageCircle size={20} />
            <h2 style={{ fontSize: 18, fontWeight: 700, fontFamily: 'var(--font-display)', margin: 0 }}>Protected Chat</h2>
            <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 4, background: 'var(--jol-100)', padding: '2px 8px', borderRadius: 2 }}>
              <Lock size={10} style={{ color: 'var(--jol-600)' }} />
              <span style={{ fontSize: 10, fontFamily: 'var(--font-mono)', color: 'var(--jol-600)' }}>E2E ENCRYPTED</span>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <Search size={14} style={{ color: 'var(--sand-400)' }} />
            <input
              type="text"
              placeholder="Search conversations..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              style={{ flex: 1, border: '1px solid var(--sand-300)', borderRadius: 2, padding: '8px 12px', fontSize: 13, fontFamily: 'var(--font-body)', background: 'white' }}
            />
          </div>
        </div>

        <div style={{ flex: 1, overflowY: 'auto' }}>
          {loading ? (
            <div style={{ padding: 32, textAlign: 'center', color: 'var(--sand-400)' }}>Loading...</div>
          ) : filtered.length === 0 ? (
            <div style={{ padding: 32, textAlign: 'center' }}>
              <MessageCircle size={32} style={{ color: 'var(--sand-300)', margin: '0 auto 12px' }} />
              <p style={{ color: 'var(--ink-700)', fontSize: 14, fontFamily: 'var(--font-body)' }}>No conversations yet</p>
            </div>
          ) : (
            filtered.map(conv => {
              const lastMsg = conv.messages?.[0];
              const otherUser = conv.host;
              return (
                <div key={conv.id} style={{ padding: '14px 16px', borderBottom: '1px solid var(--sand-200)', cursor: 'pointer', transition: 'background 150ms' }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'var(--sand-100)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                >
                  <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                    <div style={{ width: 40, height: 40, borderRadius: 20, background: 'var(--jol-600)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                      <span style={{ color: 'white', fontWeight: 700, fontSize: 16, fontFamily: 'var(--font-display)' }}>{otherUser?.displayName?.[0]}</span>
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontWeight: 600, fontSize: 14, fontFamily: 'var(--font-body)' }}>{otherUser?.displayName}</span>
                        <span style={{ fontSize: 11, color: 'var(--sand-400)', fontFamily: 'var(--font-mono)' }}>
                          {lastMsg ? new Date(lastMsg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                        </span>
                      </div>
                      <div style={{ fontSize: 12, color: 'var(--jol-600)', fontFamily: 'var(--font-body)', marginTop: 2 }}>{conv.listing?.title}</div>
                      {lastMsg && (
                        <div style={{ fontSize: 13, color: 'var(--ink-700)', fontFamily: 'var(--font-body)', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{lastMsg.safePreview}</div>
                      )}
                    </div>
                    <ChevronRight size={14} style={{ color: 'var(--sand-300)', marginTop: 4 }} />
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Chat Area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: 'var(--paper-50)' }}>
        {filtered.length > 0 ? (
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <div style={{ padding: 16, borderBottom: '1px solid var(--sand-300)', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Shield size={16} style={{ color: 'var(--jol-600)' }} />
              <span style={{ fontSize: 13, color: 'var(--ink-700)', fontFamily: 'var(--font-body)' }}>
                Messages are end-to-end encrypted. Phone numbers are never shared.
              </span>
            </div>

            {/* Messages for first conversation */}
            <div style={{ flex: 1, padding: 16, display: 'flex', flexDirection: 'column', gap: 8, overflowY: 'auto' }}>
              {filtered[0]?.messages?.map((msg, i) => {
                const isMe = msg.senderUserId !== filtered[0].host?.id;
                return (
                  <div key={i} style={{
                    maxWidth: '70%', alignSelf: isMe ? 'flex-end' : 'flex-start',
                    background: isMe ? 'var(--jol-600)' : 'white',
                    color: isMe ? 'white' : 'var(--ink-950)',
                    padding: '10px 14px', borderRadius: 8,
                    borderBottomRightRadius: isMe ? 4 : 8,
                    borderBottomLeftRadius: isMe ? 8 : 4,
                    border: isMe ? 'none' : '1px solid var(--sand-200)',
                  }}>
                    <div style={{ fontSize: 13, fontFamily: 'var(--font-body)', lineHeight: 1.5 }}>{msg.safePreview}</div>
                    <div style={{ fontSize: 10, opacity: 0.6, marginTop: 4, textAlign: 'right', fontFamily: 'var(--font-mono)' }}>
                      {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Input */}
            <div style={{ padding: 12, borderTop: '1px solid var(--sand-300)', display: 'flex', gap: 8, background: 'white' }}>
              <input type="text" placeholder="Type a message..." style={{ flex: 1, border: '1px solid var(--sand-300)', borderRadius: 20, padding: '8px 16px', fontSize: 13, fontFamily: 'var(--font-body)' }} />
              <button style={{ background: 'var(--jol-600)', color: 'white', border: 'none', borderRadius: '50%', width: 36, height: 36, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
                <Send size={14} />
              </button>
            </div>
          </div>
        ) : (
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 12 }}>
            <MessageCircle size={48} style={{ color: 'var(--sand-300)' }} />
            <p style={{ color: 'var(--ink-700)', fontSize: 16, fontFamily: 'var(--font-display)' }}>Select a conversation</p>
            <p style={{ color: 'var(--sand-400)', fontSize: 13, fontFamily: 'var(--font-body)' }}>Your protected messages appear here</p>
          </div>
        )}
      </div>
    </div>
  );
}
