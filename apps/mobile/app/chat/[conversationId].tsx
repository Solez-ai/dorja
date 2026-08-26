import { useState, useEffect, useRef } from 'react';
import { View, Text, TextInput, ScrollView, TouchableOpacity, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import { Icon, Badge } from '../../components/Icons';

const API_URL = 'http://localhost:4000';

interface Message {
  id: string;
  senderUserId: string;
  kind: string;
  safePreview: string;
  createdAt: string;
  sender: { id: string; displayName: string; primaryRole: string };
}

export default function ChatScreen() {
  const { conversationId } = useLocalSearchParams<{ conversationId: string }>();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [myUserId, setMyUserId] = useState('');
  const scrollRef = useRef<ScrollView>(null);

  useEffect(() => {
    // Get user ID from localStorage
    try {
      const stored = localStorage.getItem('dorja_user');
      if (stored) {
        const u = JSON.parse(stored);
        setMyUserId(u.id || '');
      }
    } catch {}

    if (!conversationId) return;

    // Auto-auth and fetch messages
    const load = async () => {
      try {
        // Auto-login if needed
        let token = '';
        try { token = localStorage.getItem('dorja_token') || ''; } catch {}
        if (!token) {
          await fetch(API_URL + '/v1/auth/otp/start', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ phone: '+8801700000001' }),
          });
          const vr = await fetch(API_URL + '/v1/auth/otp/verify', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ phone: '+8801700000001', code: '123456' }),
          });
          const vd = await vr.json();
          token = vd.data?.accessToken || '';
          if (token) {
            try { localStorage.setItem('dorja_token', token); } catch {}
            const u = { name: 'Rahim Ahmed', phone: '+8801700000001', role: 'SEEKER', id: '' };
            try { localStorage.setItem('dorja_user', JSON.stringify(u)); } catch {}
          }
        }

        if (token) {
          const res = await fetch(`${API_URL}/v1/chat/conversations/${conversationId}/messages`, {
            headers: { Authorization: 'Bearer ' + token },
          });
          const data = await res.json();
          if (data.data) {
            setMessages(data.data);
            // Get my user ID from the first message I sent
            const myMsg = data.data.find((m: Message) => m.sender?.displayName === 'Rahim Ahmed');
            if (myMsg) setMyUserId(myMsg.senderUserId);
          }
        }
      } catch (e) {
        console.log('Failed to load messages:', e);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [conversationId]);

  useEffect(() => {
    scrollRef.current?.scrollToEnd({ animated: true });
  }, [messages]);

  const sendMessage = async () => {
    if (!input.trim() || !conversationId) return;
    const text = input.trim();
    setInput('');

    // Optimistic add
    const optimistic: Message = {
      id: 'temp-' + Date.now(),
      senderUserId: myUserId || 'me',
      kind: 'TEXT',
      safePreview: text,
      createdAt: new Date().toISOString(),
      sender: { id: myUserId || 'me', displayName: 'You', primaryRole: 'SEEKER' },
    };
    setMessages(prev => [...prev, optimistic]);

    try {
      let token = '';
      try { token = localStorage.getItem('dorja_token') || ''; } catch {}
      if (token) {
        const res = await fetch(`${API_URL}/v1/chat/conversations/${conversationId}/messages`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
          body: JSON.stringify({ body: text }),
        });
        const data = await res.json();
        if (data.data) {
          setMessages(prev => prev.map(m => m.id === optimistic.id ? data.data : m));
        }
      }
    } catch (e) {
      console.log('Failed to send:', e);
    }
  };

  const isMe = (msg: Message) => msg.senderUserId === myUserId || msg.sender?.displayName === 'Rahim Ahmed';

  return (
    <KeyboardAvoidingView style={s.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Icon name="back" size={16} color="#007C78" />
        </TouchableOpacity>
        <View style={s.headerInfo}>
          <Text style={s.headerTitle}>Protected Chat</Text>
          <View style={s.headerMeta}>
            <Icon name="lock" size={10} color="#007C78" />
            <Text style={s.headerSub}>End-to-end encrypted</Text>
          </View>
        </View>
        <View style={s.headerBadge}>
          <Badge text="SECURE" />
        </View>
      </View>

      {/* Safety banner */}
      <View style={s.safetyBanner}>
        <Icon name="shield" size={12} color="#C2710B" />
        <Text style={s.safetyText}>
          Never send money or share your exact address before SafeView confirms the visit.
        </Text>
      </View>

      {/* Messages */}
      <ScrollView
        ref={scrollRef}
        style={s.messagesArea}
        contentContainerStyle={s.messagesContent}
        showsVerticalScrollIndicator={false}
      >
        {loading ? (
          <View style={s.loadingState}>
            <Text style={s.loadingText}>Loading messages...</Text>
          </View>
        ) : messages.length === 0 ? (
          <View style={s.emptyState}>
            <Icon name="message" size={32} color="#D9CCB9" />
            <Text style={s.emptyText}>Start a conversation about this property</Text>
          </View>
        ) : (
          messages.map((msg) => {
            const mine = isMe(msg);
            return (
              <View key={msg.id} style={[s.bubble, mine ? s.bubbleMe : s.bubbleThem]}>
                {!mine && (
                  <Text style={s.senderName}>{msg.sender?.displayName || 'Unknown'}</Text>
                )}
                <Text style={[s.bubbleText, mine && s.bubbleTextMe]}>{msg.safePreview}</Text>
                <Text style={[s.timestamp, mine && s.timestampMe]}>
                  {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </Text>
              </View>
            );
          })
        )}
      </ScrollView>

      {/* Input */}
      <View style={s.inputBar}>
        <TextInput
          style={s.textInput}
          value={input}
          onChangeText={setInput}
          placeholder="Type a message..."
          placeholderTextColor="#C4B5A0"
          multiline
          maxLength={1000}
        />
        <TouchableOpacity
          style={[s.sendBtn, !input.trim() && s.sendBtnDisabled]}
          onPress={sendMessage}
          disabled={!input.trim()}
        >
          <Icon name="send" size={16} color={input.trim() ? 'white' : '#D9CCB9'} />
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2' },
  header: { flexDirection: 'row', alignItems: 'center', padding: 12, paddingTop: 48, backgroundColor: 'white', borderBottomWidth: 1, borderBottomColor: '#E8E0D0' },
  backBtn: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#F2EDE3', alignItems: 'center', justifyContent: 'center', marginRight: 10 },
  headerInfo: { flex: 1 },
  headerTitle: { fontSize: 16, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk' },
  headerMeta: { flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: 2 },
  headerSub: { fontSize: 11, color: '#007C78', fontFamily: 'IBM Plex Sans' },
  headerBadge: {},

  safetyBanner: { flexDirection: 'row', alignItems: 'center', gap: 6, margin: 12, padding: 10, backgroundColor: '#FEF3CD', borderRadius: 4 },
  safetyText: { flex: 1, fontSize: 11, color: '#17324D', fontFamily: 'IBM Plex Sans', lineHeight: 16 },

  messagesArea: { flex: 1 },
  messagesContent: { padding: 16, gap: 8 },

  loadingState: { alignItems: 'center', padding: 48 },
  loadingText: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  emptyState: { alignItems: 'center', padding: 48, gap: 8 },
  emptyText: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans' },

  bubble: { maxWidth: '80%', padding: 12, borderRadius: 12 },
  bubbleMe: { alignSelf: 'flex-end', backgroundColor: '#007C78', borderBottomRightRadius: 4 },
  bubbleThem: { alignSelf: 'flex-start', backgroundColor: 'white', borderWidth: 1, borderColor: '#E8E0D0', borderBottomLeftRadius: 4 },
  senderName: { fontSize: 11, fontWeight: '600', color: '#007C78', fontFamily: 'IBM Plex Sans', marginBottom: 4 },
  bubbleText: { fontSize: 14, color: '#0B1F33', fontFamily: 'IBM Plex Sans', lineHeight: 20 },
  bubbleTextMe: { color: 'white' },
  timestamp: { fontSize: 10, color: '#D9CCB9', fontFamily: 'IBM Plex Mono', marginTop: 4, textAlign: 'right' },
  timestampMe: { color: 'rgba(255,255,255,0.6)' },

  inputBar: { flexDirection: 'row', alignItems: 'flex-end', padding: 12, paddingBottom: 24, backgroundColor: 'white', borderTopWidth: 1, borderTopColor: '#E8E0D0', gap: 8 },
  textInput: { flex: 1, backgroundColor: '#FBF8F2', borderWidth: 1, borderColor: '#D9CCB9', borderRadius: 20, paddingHorizontal: 16, paddingVertical: 10, fontSize: 14, fontFamily: 'IBM Plex Sans', color: '#0B1F33', maxHeight: 100 },
  sendBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#007C78', alignItems: 'center', justifyContent: 'center' },
  sendBtnDisabled: { backgroundColor: '#F2EDE3' },
});
