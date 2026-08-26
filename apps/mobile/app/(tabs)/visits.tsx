import { View, Text, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Icon, IconCircle, Badge } from '../../components/Icons';

export default function VisitsScreen() {
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <ScrollView style={styles.scrollView} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Text style={styles.title}>Visits</Text>
          <Text style={styles.subtitle}>SafeView appointments</Text>
        </View>

        {/* Active visit */}
        <View style={styles.visitCard}>
          <View style={styles.visitHeader}>
            <Badge text="TODAY" bgColor="#D7F1EE" textColor="#006B68" />
            <View style={styles.visitStatus}>
              <View style={styles.pulseDot} />
              <Text style={styles.visitStatusText}>Confirmed</Text>
            </View>
          </View>
          <Text style={styles.visitTime}>4:30–5:00 PM</Text>
          <View style={styles.visitDetailRow}>
            <Icon name="mapPin" size={14} color="#17324D" />
            <Text style={styles.visitDetail}>
              Mirpur 11 · exact address unlocks in 43 min
            </Text>
          </View>
          <View style={styles.visitChecks}>
            <View style={styles.checkRow}>
              <Icon name="check" size={14} color="#007C78" />
              <Text style={styles.checkText}>Buyer identity confirmed</Text>
            </View>
            <View style={styles.checkRow}>
              <Icon name="check" size={14} color="#007C78" />
              <Text style={styles.checkText}>Host authority reviewed</Text>
            </View>
            <View style={styles.checkRow}>
              <Icon name="check" size={14} color="#007C78" />
              <Text style={styles.checkText}>Location verified</Text>
            </View>
          </View>
          <TouchableOpacity style={styles.passButton} activeOpacity={0.7}>
            <Icon name="qr" size={16} color="white" />
            <Text style={styles.passButtonText}>Open Viewing Pass</Text>
          </TouchableOpacity>
        </View>

        {/* Upcoming visits */}
        <View style={styles.sectionHeader}>
          <Icon name="calendar" size={16} color="#0B1F33" />
          <Text style={styles.sectionTitle}>Upcoming</Text>
        </View>
        <View style={styles.emptyState}>
          <IconCircle name="calendar" size={48} bgColor="#F2EDE3" iconColor="#D9CCB9" iconSize={20} />
          <Text style={styles.emptyTitle}>No more upcoming visits</Text>
          <Text style={styles.emptyText}>
            Request a SafeView from a property page to schedule a visit.
          </Text>
        </View>

        {/* Past visits */}
        <View style={styles.sectionHeader}>
          <Icon name="clock" size={16} color="#0B1F33" />
          <Text style={styles.sectionTitle}>Past Visits</Text>
        </View>
        {[
          { date: 'Jun 15, 2025', location: 'Banani 11', status: 'Completed' },
          { date: 'Jun 12, 2025', location: 'Gulshan 2', status: 'Cancelled' },
        ].map((v, i) => (
          <View key={i} style={styles.pastCard}>
            <View style={styles.pastRow}>
              <Icon name="calendar" size={14} color="#17324D" />
              <Text style={styles.pastDate}>{v.date}</Text>
              <Badge
                text={v.status}
                bgColor={v.status === 'Completed' ? '#D7F1EE' : '#FCE8BE'}
                textColor={v.status === 'Completed' ? '#006B68' : '#C2710B'}
              />
            </View>
            <View style={styles.pastRow}>
              <Icon name="mapPin" size={12} color="#17324D" />
              <Text style={styles.pastLocation}>{v.location}</Text>
            </View>
          </View>
        ))}
        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2' },
  scrollView: { flex: 1 },
  header: { padding: 20 },
  title: { fontSize: 24, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk' },
  subtitle: { fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Sans', marginTop: 4 },
  visitCard: {
    margin: 16,
    padding: 16,
    backgroundColor: 'white',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E8E0D0',
    shadowColor: '#0B1F33',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 3,
  },
  visitHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  visitStatus: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  pulseDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: '#007C78' },
  visitStatusText: { fontSize: 11, fontWeight: '600', color: '#006B68', fontFamily: 'IBM Plex Sans' },
  visitTime: { fontSize: 22, fontWeight: '700', color: '#0B1F33', fontFamily: 'IBM Plex Mono', marginBottom: 12 },
  visitDetailRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 12 },
  visitDetail: { flex: 1, fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  visitChecks: { gap: 8, marginBottom: 16 },
  checkRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  checkText: { fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  passButton: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center',
    gap: 8, backgroundColor: '#007C78', padding: 14, borderRadius: 8,
  },
  passButtonText: { color: 'white', fontSize: 14, fontWeight: '600', fontFamily: 'IBM Plex Sans' },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', gap: 8, padding: 16, paddingBottom: 8 },
  sectionTitle: { fontSize: 14, fontWeight: '600', color: '#0B1F33', fontFamily: 'Space Grotesk' },
  emptyState: { alignItems: 'center', padding: 32, gap: 8 },
  emptyTitle: { fontSize: 16, fontWeight: '600', color: '#0B1F33', fontFamily: 'Space Grotesk' },
  emptyText: { fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Sans', textAlign: 'center', lineHeight: 18 },
  pastCard: {
    marginHorizontal: 16, padding: 12, backgroundColor: 'white', borderRadius: 8,
    borderWidth: 1, borderColor: '#E8E0D0', marginBottom: 8, gap: 6,
  },
  pastRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  pastDate: { flex: 1, fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Mono' },
  pastLocation: { fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Sans' },
});
