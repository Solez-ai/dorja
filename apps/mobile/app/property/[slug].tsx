import { useState, useEffect } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator, Dimensions } from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import { Icon, Badge } from '../../components/Icons';

const API_URL = 'http://localhost:4000';
const SCREEN_W = Dimensions.get('window').width;

interface Room {
  id: string;
  roomType: string;
  displayName: string;
  ordinal: number;
}

interface ListingDetail {
  id: string;
  slug: string;
  title: string;
  intent: string;
  propertyType: string;
  publicArea: string;
  priceAmount: number;
  currency: string;
  pulseStatus: string;
  rooms: Room[];
  captureCount?: number;
  owner?: { displayName: string };
}

export default function PropertyDetailScreen() {
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const [listing, setListing] = useState<ListingDetail | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!slug) return;
    fetch(API_URL + '/v1/listings/' + slug)
      .then(r => r.json())
      .then(d => { if (d.data) setListing(d.data); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [slug]);

  if (loading) {
    return (
      <View style={s.center}>
        <ActivityIndicator size="large" color="#007C78" />
        <Text style={s.loadingText}>Loading passport...</Text>
      </View>
    );
  }

  if (!listing) {
    return (
      <View style={s.center}>
        <Icon name="alert" size={32} color="#D9CCB9" />
        <Text style={s.errorText}>Property not found</Text>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={s.backLink}>← Go back</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const roomGroups = listing.rooms.reduce((acc: Record<string, Room[]>, r) => {
    (acc[r.roomType] = acc[r.roomType] || []).push(r);
    return acc;
  }, {});

  return (
    <ScrollView style={s.container} showsVerticalScrollIndicator={false}>
      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Icon name="back" size={16} color="#007C78" />
          <Text style={s.backText}>Back</Text>
        </TouchableOpacity>
        <View style={s.badgeRow}>
          <Badge text={listing.intent === 'RENT' ? 'FOR RENT' : 'FOR SALE'} bgColor="#0B1F33" textColor="white" />
          {listing.pulseStatus === 'ACTIVE' && (
            <View style={s.liveBadge}>
              <View style={s.pulseDot} />
              <Text style={s.liveText}>LIVE</Text>
            </View>
          )}
        </View>
      </View>

      {/* Hero area */}
      <View style={s.hero}>
        <Text style={s.title}>{listing.title}</Text>
        <Text style={s.price}>
          ৳{listing.priceAmount.toLocaleString()}
          <Text style={s.pricePeriod}>{listing.intent === 'RENT' ? ' /month' : ' total'}</Text>
        </Text>
        <View style={s.locationRow}>
          <Icon name="mapPin" size={14} color="#17324D" />
          <Text style={s.locationText}>{listing.publicArea} · {listing.propertyType}</Text>
        </View>
      </View>

      {/* 3D Tour Section */}
      <View style={s.section}>
        <View style={s.sectionHeader}>
          <Icon name="cube" size={16} color="#007C78" />
          <Text style={s.sectionTitle}>3D Walkthrough</Text>
        </View>
        <TouchableOpacity style={s.tourCard} onPress={() => router.push(`/property/tour/${listing.slug}`)}>
          <View style={s.tourPreview}>
            <Icon name="cube" size={40} color="#007C78" />
            <View style={s.tourOverlay}>
              <Text style={s.tourOverlayText}>Tap to enter 3D tour</Text>
              <View style={s.tourControls}>
                <Text style={s.tourControlText}>Joystick to look around · Tap doors to walk</Text>
              </View>
            </View>
          </View>
          <View style={s.tourInfo}>
            <Text style={s.tourInfoText}>{Object.keys(roomGroups).length} room types · {listing.rooms.length} rooms</Text>
          </View>
        </TouchableOpacity>
      </View>

      {/* Reality Passport */}
      <View style={s.section}>
        <View style={s.sectionHeader}>
          <Icon name="shield" size={16} color="#007C78" />
          <Text style={s.sectionTitle}>Reality Passport</Text>
        </View>
        <View style={s.passportCard}>
          <View style={s.passportRow}>
            <View style={s.passportIcon}>
              <Icon name="check" size={16} color="#007C78" />
            </View>
            <View style={s.passportInfo}>
              <Text style={s.passportLabel}>Pulse Status</Text>
              <Text style={s.passportValue}>{listing.pulseStatus}</Text>
            </View>
          </View>
          <View style={s.passportRow}>
            <View style={s.passportIcon}>
              <Icon name="camera" size={16} color="#007C78" />
            </View>
            <View style={s.passportInfo}>
              <Text style={s.passportLabel}>Capture Status</Text>
              <Text style={s.passportValue}>{listing.rooms.length} rooms mapped</Text>
            </View>
          </View>
          <View style={s.passportRow}>
            <View style={s.passportIcon}>
              <Icon name="lock" size={16} color="#007C78" />
            </View>
            <View style={s.passportInfo}>
              <Text style={s.passportLabel}>Data Protection</Text>
              <Text style={s.passportValue}>AES-256-GCM encrypted</Text>
            </View>
          </View>
        </View>
      </View>

      {/* Room Map */}
      <View style={s.section}>
        <View style={s.sectionHeader}>
          <Icon name="home" size={16} color="#007C78" />
          <Text style={s.sectionTitle}>Room Map</Text>
        </View>
        {Object.entries(roomGroups).map(([type, rooms]) => (
          <View key={type} style={s.roomGroup}>
            <Text style={s.roomGroupTitle}>{type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())}</Text>
            <View style={s.roomList}>
              {rooms.map((room) => (
                <View key={room.id} style={s.roomItem}>
                  <View style={s.roomDot} />
                  <Text style={s.roomName}>{room.displayName}</Text>
                </View>
              ))}
            </View>
          </View>
        ))}
      </View>

      {/* Action buttons */}
      <View style={s.actions}>
        <TouchableOpacity style={s.primaryBtn} onPress={() => router.push(`/property/tour/${listing.slug}`)}>
          <Icon name="cube" size={16} color="white" />
          <Text style={s.primaryBtnText}>Open 3D Tour</Text>
        </TouchableOpacity>
        <TouchableOpacity style={s.secondaryBtn}>
          <Icon name="star" size={16} color="#007C78" />
          <Text style={s.secondaryBtnText}>Add to Shortlist</Text>
        </TouchableOpacity>
        <TouchableOpacity style={s.secondaryBtn}>
          <Icon name="message" size={16} color="#007C78" />
          <Text style={s.secondaryBtnText}>Ask in Protected Chat</Text>
        </TouchableOpacity>
        <TouchableOpacity style={s.darkBtn}>
          <Icon name="calendar" size={16} color="white" />
          <Text style={s.darkBtnText}>Request a SafeView</Text>
        </TouchableOpacity>
      </View>

      {/* Safety note */}
      <View style={s.safetyNote}>
        <Icon name="alert" size={14} color="#C2710B" />
        <Text style={s.safetyText}>
          Meet only through SafeView appointments. Never send money before physical inspection.
        </Text>
      </View>

      <View style={{ height: 40 }} />
    </ScrollView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2' },
  center: { flex: 1, backgroundColor: '#FBF8F2', alignItems: 'center', justifyContent: 'center', padding: 32 },
  loadingText: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans', marginTop: 12 },
  errorText: { fontSize: 16, color: '#0B1F33', fontFamily: 'Space Grotesk', marginTop: 12 },
  backLink: { fontSize: 14, color: '#007C78', fontFamily: 'IBM Plex Sans', marginTop: 12 },

  header: { padding: 16, paddingBottom: 8 },
  backBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, marginBottom: 12 },
  backText: { fontSize: 14, color: '#007C78', fontFamily: 'IBM Plex Sans' },
  badgeRow: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  liveBadge: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#D7F1EE', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 2, gap: 4 },
  pulseDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: '#007C78' },
  liveText: { fontSize: 10, fontWeight: '700', color: '#006B68', fontFamily: 'IBM Plex Mono', letterSpacing: 0.8 },

  hero: { padding: 16, paddingTop: 8 },
  title: { fontSize: 24, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk', marginBottom: 8 },
  price: { fontSize: 28, fontWeight: '700', color: '#0B1F33', fontFamily: 'IBM Plex Mono' },
  pricePeriod: { fontSize: 14, fontWeight: '400', color: '#17324D', fontFamily: 'IBM Plex Sans' },
  locationRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 8 },
  locationText: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans' },

  section: { padding: 16, paddingTop: 8 },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 12 },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk' },

  tourCard: { backgroundColor: '#1A1A1A', borderRadius: 10, overflow: 'hidden', borderWidth: 1, borderColor: '#333' },
  tourPreview: { height: 200, alignItems: 'center', justifyContent: 'center', position: 'relative' },
  tourOverlay: { position: 'absolute', bottom: 0, left: 0, right: 0, padding: 12, backgroundColor: 'rgba(0,0,0,0.6)' },
  tourOverlayText: { color: 'white', fontSize: 14, fontWeight: '600', fontFamily: 'IBM Plex Sans' },
  tourControls: { marginTop: 4 },
  tourControlText: { color: '#aaa', fontSize: 11, fontFamily: 'IBM Plex Sans' },
  tourInfo: { padding: 12 },
  tourInfoText: { color: '#aaa', fontSize: 12, fontFamily: 'IBM Plex Mono' },

  passportCard: { backgroundColor: 'white', borderRadius: 6, borderWidth: 1, borderColor: '#D9CCB9', padding: 16, gap: 16 },
  passportRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  passportIcon: { width: 32, height: 32, borderRadius: 16, backgroundColor: '#D7F1EE', alignItems: 'center', justifyContent: 'center' },
  passportInfo: { flex: 1 },
  passportLabel: { fontSize: 12, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  passportValue: { fontSize: 14, fontWeight: '600', color: '#0B1F33', fontFamily: 'IBM Plex Mono', marginTop: 2 },

  roomGroup: { marginBottom: 16 },
  roomGroupTitle: { fontSize: 13, fontWeight: '600', color: '#17324D', fontFamily: 'IBM Plex Sans', marginBottom: 8 },
  roomList: { gap: 6 },
  roomItem: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingVertical: 4 },
  roomDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: '#007C78' },
  roomName: { fontSize: 14, color: '#0B1F33', fontFamily: 'IBM Plex Sans' },

  actions: { padding: 16, gap: 10 },
  primaryBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, backgroundColor: '#007C78', padding: 14, borderRadius: 2 },
  primaryBtnText: { color: 'white', fontSize: 14, fontWeight: '600', fontFamily: 'IBM Plex Sans' },
  secondaryBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, backgroundColor: 'white', padding: 14, borderRadius: 2, borderWidth: 1, borderColor: '#D9CCB9' },
  secondaryBtnText: { color: '#007C78', fontSize: 14, fontWeight: '600', fontFamily: 'IBM Plex Sans' },
  darkBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, backgroundColor: '#0B1F33', padding: 14, borderRadius: 2 },
  darkBtnText: { color: 'white', fontSize: 14, fontWeight: '600', fontFamily: 'IBM Plex Sans' },

  safetyNote: { margin: 16, padding: 12, backgroundColor: '#FEF3CD', borderRadius: 4, flexDirection: 'row', gap: 8, alignItems: 'flex-start' },
  safetyText: { flex: 1, fontSize: 12, color: '#17324D', fontFamily: 'IBM Plex Sans', lineHeight: 18 },
});
