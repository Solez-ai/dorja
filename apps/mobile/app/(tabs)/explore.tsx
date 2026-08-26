import { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  TextInput,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Link } from 'expo-router';
import { Icon, Badge } from '../../components/Icons';
import { API_URL } from '../../config';

const FILTER_CHIPS = [
  { label: 'For rent', icon: 'home' },
  { label: 'For sale', icon: 'building' },
  { label: 'Family', icon: 'users' },
  { label: 'Bachelor', icon: 'user' },
  { label: 'Available now', icon: 'check' },
  { label: '3D tour', icon: 'cube' },
];

interface Listing {
  id: string;
  slug: string;
  title: string;
  intent: string;
  propertyType: string;
  publicArea: string;
  priceAmount: number;
  currency: string;
  pulseStatus: string;
  rooms: Array<{ roomType: string; displayName: string }>;
  captures: any[];
  captureCount?: number;
}

export default function ExploreScreen() {
  const insets = useSafeAreaInsets();
  const [listings, setListings] = useState<Listing[]>([]);
  const [search, setSearch] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchListings = useCallback(async () => {
    try {
      const res = await fetch(`${API_URL}/v1/listings`);
      const data = await res.json();
      if (data.data?.items) {
        setListings(data.data.items);
      }
    } catch (e) {
      console.log('Failed to fetch listings:', e);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchListings();
  }, [fetchListings]);

  const onRefresh = () => {
    setRefreshing(true);
    fetchListings();
  };

  const filtered = listings.filter(
    (l) =>
      !search ||
      l.title.toLowerCase().includes(search.toLowerCase()) ||
      l.publicArea.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <ScrollView
        style={styles.scrollView}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor="#007C78"
          />
        }
      >
        {/* Search */}
        <View style={styles.searchContainer}>
          <View style={styles.searchRow}>
            <Icon name="search" size={16} color="#D9CCB9" />
            <TextInput
              style={styles.searchInput}
              placeholder="Search area, road, or property..."
              placeholderTextColor="#C4B5A0"
              value={search}
              onChangeText={setSearch}
            />
          </View>
        </View>

        {/* Filters */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          style={styles.filtersContainer}
          contentContainerStyle={{ gap: 8 }}
        >
          {FILTER_CHIPS.map((chip) => (
            <TouchableOpacity key={chip.label} style={styles.filterChip}>
              <Icon name={chip.icon} size={12} color="#17324D" />
              <Text style={styles.filterChipText}>{chip.label}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* Property count */}
        <View style={styles.countRow}>
          <Icon name="building" size={14} color="#17324D" />
          <Text style={styles.countText}>
            {filtered.length}{' '}
            {filtered.length === 1 ? 'property' : 'properties'} listed
          </Text>
        </View>

        {/* Listings */}
        {loading ? (
          <View style={styles.emptyState}>
            <ActivityIndicator size="large" color="#007C78" />
            <Text style={styles.loadingText}>Loading listings...</Text>
          </View>
        ) : filtered.length === 0 ? (
          <View style={styles.emptyState}>
            <Icon name="building" size={48} color="#D9CCB9" />
            <Text style={styles.emptyTitle}>No listings found</Text>
            <Text style={styles.emptyText}>
              Create your first listing from the Capture tab.
            </Text>
          </View>
        ) : (
          <View style={styles.listingsContainer}>
            {filtered.map((listing) => (
              <Link
                key={listing.id}
                href={`/property/${listing.slug}`}
                asChild
              >
                <TouchableOpacity style={styles.listingCard} activeOpacity={0.7}>
                  {/* Header */}
                  <View style={styles.cardHeader}>
                    <View style={styles.cardBadgeRow}>
                      <Badge
                        text={
                          listing.intent === 'RENT' ? 'FOR RENT' : 'FOR SALE'
                        }
                        bgColor="#0B1F33"
                        textColor="white"
                      />
                      {listing.pulseStatus === 'ACTIVE' && (
                        <View style={styles.liveBadge}>
                          <View style={styles.pulseDot} />
                          <Text style={styles.liveText}>AVAILABLE</Text>
                        </View>
                      )}
                    </View>
                  </View>

                  {/* Body */}
                  <View style={styles.cardBody}>
                    <Text style={styles.cardTitle}>{listing.title}</Text>
                    <Text style={styles.cardPrice}>
                      ৳{listing.priceAmount.toLocaleString()}
                      <Text style={styles.cardPricePeriod}>
                        {listing.intent === 'RENT' ? ' /month' : ' total'}
                      </Text>
                    </Text>
                    <View style={styles.cardLocation}>
                      <Icon name="mapPin" size={12} color="#17324D" />
                      <Text style={styles.cardLocationText}>
                        {listing.publicArea} · {listing.propertyType} ·{' '}
                        {listing.rooms.length} rooms
                      </Text>
                    </View>
                    <View style={styles.cardFooter}>
                      <View style={styles.cardPulse}>
                        <View style={styles.pulseDotSmall} />
                        <Text style={styles.cardPulseText}>CONFIRMED</Text>
                      </View>
                      <View style={styles.cardAction}>
                        <Text style={styles.cardActionText}>Open Passport</Text>
                        <Icon name="arrowRight" size={12} color="#007C78" />
                      </View>
                    </View>
                  </View>
                </TouchableOpacity>
              </Link>
            ))}
          </View>
        )}
        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2' },
  scrollView: { flex: 1 },
  searchContainer: { padding: 16, paddingBottom: 8 },
  searchRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'white',
    borderWidth: 1,
    borderColor: '#D9CCB9',
    borderRadius: 8,
    padding: 12,
    gap: 8,
    shadowColor: '#0B1F33',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 3,
    elevation: 1,
  },
  searchInput: {
    flex: 1,
    fontSize: 14,
    fontFamily: 'IBM Plex Sans',
    color: '#0B1F33',
    padding: 0,
  },
  filtersContainer: { paddingHorizontal: 16, marginBottom: 12 },
  filterChip: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#D9CCB9',
    backgroundColor: 'white',
    gap: 4,
  },
  filterChipText: {
    fontSize: 13,
    color: '#0B1F33',
    fontFamily: 'IBM Plex Sans',
  },
  countRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginBottom: 12,
    gap: 6,
  },
  countText: {
    fontSize: 13,
    color: '#17324D',
    fontFamily: 'IBM Plex Mono',
  },
  listingsContainer: { padding: 16, gap: 16 },
  listingCard: {
    backgroundColor: 'white',
    borderWidth: 1,
    borderColor: '#E8E0D0',
    borderRadius: 12,
    overflow: 'hidden',
    shadowColor: '#0B1F33',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 3,
  },
  cardHeader: {
    height: 100,
    backgroundColor: '#E8E0D0',
    position: 'relative',
    padding: 12,
    justifyContent: 'flex-end',
  },
  cardBadgeRow: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  liveBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#D7F1EE',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 4,
    gap: 4,
  },
  pulseDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#007C78',
  },
  liveText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#006B68',
    fontFamily: 'IBM Plex Mono',
    letterSpacing: 0.8,
  },
  cardBody: { padding: 16 },
  cardTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0B1F33',
    fontFamily: 'Space Grotesk',
    marginBottom: 6,
  },
  cardPrice: {
    fontSize: 20,
    fontWeight: '700',
    color: '#0B1F33',
    fontFamily: 'IBM Plex Mono',
  },
  cardPricePeriod: {
    fontSize: 13,
    fontWeight: '400',
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
  },
  cardLocation: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 8,
  },
  cardLocationText: {
    fontSize: 13,
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 12,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#F2EDE3',
  },
  cardPulse: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  pulseDotSmall: {
    width: 5,
    height: 5,
    borderRadius: 3,
    backgroundColor: '#007C78',
  },
  cardPulseText: {
    fontSize: 10,
    fontWeight: '600',
    color: '#17324D',
    fontFamily: 'IBM Plex Mono',
    letterSpacing: 0.5,
  },
  cardAction: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  cardActionText: {
    fontSize: 13,
    color: '#007C78',
    fontWeight: '600',
    fontFamily: 'IBM Plex Sans',
  },
  emptyState: { alignItems: 'center', padding: 48, gap: 8 },
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
    marginTop: 16,
    marginBottom: 8,
  },
  emptyText: {
    fontSize: 14,
    color: '#17324D',
    fontFamily: 'IBM Plex Sans',
    textAlign: 'center',
    lineHeight: 20,
  },
});
