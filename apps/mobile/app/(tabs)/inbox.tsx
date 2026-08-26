import { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { Icon, IconCircle, Badge } from '../../components/Icons';
import { ensureAuth, authFetch } from '../../lib/api';

interface Conversation {
  id: string;
  listingId: string;
  status: string;
  lastMessageAt: string;
  listing: { id: string; title: string; slug: string; publicArea: string };
  seeker: { id: string; displayName: string; primaryRole: string };
  host: { id: string; displayName: string; primaryRole: string };
  messages: Array<{
    safePreview: string;
    createdAt: string;
    senderUserId: string;
  }>;
}

export default function InboxScreen() {
  const insets = useSafeAreaInsets();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const auth = await ensureAuth();
        if (auth?.token) {
          const res = await authFetch('/v1/chat/conversations');
          const data = await res.json();
          if (data.data) setConversations(data.data);
        }
      } catch (e) {
        console.log('Failed to load conversations:', e);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <ScrollView
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}
      >
        {/* Safety banner */}
        <View style={styles.safetyBanner}>
          <View style={styles.safetyIcon}>
            <Icon name="shield" size={14} color="#C2710B" />
          </View>
          <Text style={styles.safetyBannerText}>
            Keep your phone number, money, and exact address private until
            SafeView confirms the visit.
          </Text>
        </View>

        {/* Conversations */}
        {loading ? (
          <View style={styles.emptyState}>
            <ActivityIndicator size="large" color="#007C78" />
            <Text style={styles.loadingText}>Loading messages...</Text>
          </View>
        ) : conversations.length === 0 ? (
          <View style={styles.emptyState}>
            <IconCircle
              name="message"
              size={56}
              bgColor="#F2EDE3"
              iconColor="#D9CCB9"
              iconSize={24}
            />
            <Text style={styles.emptyTitle}>No messages yet</Text>
            <Text style={styles.emptyText}>
              Ask a question from a property page to start a protected
              conversation.
            </Text>
          </View>
        ) : (
          <View style={styles.convList}>
            {conversations.map((conv) => {
              const lastMsg = conv.messages?.[0];
              const otherUser = conv.host;
              return (
                <TouchableOpacity
                  key={conv.id}
                  style={styles.convCard}
                  activeOpacity={0.7}
                  onPress={() => router.push(`/chat/${conv.id}` as any)}
                >
                  <View style={styles.convAvatar}>
                    <Text style={styles.convAvatarText}>
                      {otherUser.displayName[0]}
                    </Text>
                  </View>
                  <View style={styles.convInfo}>
                    <View style={styles.convHeader}>
                      <Text style={styles.convName}>
                        {otherUser.displayName}
                      </Text>
                      <Text style={styles.convTime}>
                        {lastMsg
                          ? new Date(lastMsg.createdAt).toLocaleTimeString([], {
                              hour: '2-digit',
                              minute: '2-digit',
                            })
                          : ''}
                      </Text>
                    </View>
                    <Text style={styles.convProperty}>
                      {conv.listing?.title}
                    </Text>
                    {lastMsg && (
                      <Text style={styles.convPreview} numberOfLines={1}>
                        {lastMsg.safePreview}
                      </Text>
                    )}
                  </View>
                  <View style={styles.convBadge}>
                    <Icon name="lock" size={10} color="#007C78" />
                  </View>
                </TouchableOpacity>
              );
            })}
          </View>
        )}

        {/* How Protected Chat works */}
        <View style={styles.infoSection}>
          <Text style={styles.infoTitle}>How Protected Chat Works</Text>
          {[
            {
              icon: 'lock',
              title: 'End-to-end encrypted',
              desc: 'Messages are encrypted before leaving your device',
            },
            {
              icon: 'shield',
              title: 'Identity verified',
              desc: 'Both parties confirmed through SafeView',
            },
            {
              icon: 'phone',
              title: 'Phone stays private',
              desc: 'Your number is never shared directly',
            },
          ].map((item) => (
            <View key={item.title} style={styles.infoRow}>
              <IconCircle name={item.icon} size={36} iconSize={16} />
              <View style={styles.infoContent}>
                <Text style={styles.infoItemTitle}>{item.title}</Text>
                <Text style={styles.infoItemDesc}>{item.desc}</Text>
              </View>
            </View>
          ))}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2' },
  scrollView: { flex: 1 },
  safetyBanner: {
    margin: 16,
    padding: 12,
    backgroundColor: '#FEF3CD',
    borderRadius: 8,
    flexDirection: 'row',
    gap: 10,
    alignItems: 'flex-start',
  },
  safetyIcon: { marginTop: 1 },
  safetyBannerText: {
    flex: 1,
    fontSize: 12,
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
    lineHeight: 18,
  },
  emptyState: { alignItems: 'center', padding: 48, gap: 12 },
  loadingText: {
    fontSize: 14,
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
    marginTop: 8,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#0B1F33',
    fontFamily: 'Space Grotesk',
  },
  emptyText: {
    fontSize: 14,
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
    textAlign: 'center',
    lineHeight: 20,
    paddingHorizontal: 16,
  },
  convList: { padding: 16, gap: 8 },
  convCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
    backgroundColor: 'white',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E8E0D0',
    gap: 12,
    shadowColor: '#0B1F33',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  convAvatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#007C78',
    alignItems: 'center',
    justifyContent: 'center',
  },
  convAvatarText: {
    color: 'white',
    fontSize: 18,
    fontWeight: '700',
    fontFamily: 'Space Grotesk',
  },
  convInfo: { flex: 1 },
  convHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  convName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#0B1F33',
    fontFamily: 'IBM Plex Sans',
  },
  convTime: {
    fontSize: 11,
    color: '#D9CCB9',
    fontFamily: 'IBM Plex Mono',
  },
  convProperty: {
    fontSize: 12,
    color: '#007C78',
    fontFamily: 'IBM Plex Sans',
    marginTop: 2,
  },
  convPreview: {
    fontSize: 13,
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
    marginTop: 4,
  },
  convBadge: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#D7F1EE',
    alignItems: 'center',
    justifyContent: 'center',
  },
  infoSection: { padding: 16 },
  infoTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#0B1F33',
    fontFamily: 'Space Grotesk',
    marginBottom: 12,
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 12,
    backgroundColor: 'white',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#E8E0D0',
    marginBottom: 8,
  },
  infoContent: { flex: 1 },
  infoItemTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#0B1F33',
    fontFamily: 'IBM Plex Sans',
  },
  infoItemDesc: {
    fontSize: 12,
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
    marginTop: 2,
  },
});
