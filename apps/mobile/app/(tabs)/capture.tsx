import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { Icon } from '../../components/Icons';

const ACTIONS = [
  {
    id: 'create',
    title: 'Add your property',
    desc: 'List a property for rent or sale',
    icon: 'home',
    route: '/listing/create',
  },
  {
    id: 'scan-room',
    title: '3D Room Scanner',
    desc: 'Scan a room with your camera for a 3D walkthrough',
    icon: 'cube',
    route: '/capture/scan-room',
  },
  {
    id: 'capture',
    title: 'Capture a listing',
    desc: 'Guided hold-to-capture room route',
    icon: 'camera',
    route: '/capture/start',
  },
  {
    id: 'scan',
    title: 'Scan a DORJA property code',
    desc: 'Open a property by scanning its QR',
    icon: 'qr',
    route: '/capture/scan',
  },
  {
    id: 'check-pass',
    title: 'Check a viewing pass',
    desc: 'Verify a SafeView QR pass',
    icon: 'shield',
    route: '/capture/check-pass',
  },
];

export default function CaptureScreen() {
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={styles.sheet}>
        <Text style={styles.sheetTitle}>Actions</Text>
        <Text style={styles.sheetSubtitle}>What would you like to do?</Text>

        {ACTIONS.map((action) => (
          <TouchableOpacity
            key={action.id}
            style={styles.action}
            activeOpacity={0.7}
            onPress={() => router.push(action.route as any)}
          >
            <View style={styles.actionIcon}>
              <Icon name={action.icon} size={20} color="#007C78" />
            </View>
            <View style={styles.actionText}>
              <Text style={styles.actionTitle}>{action.title}</Text>
              <Text style={styles.actionDesc}>{action.desc}</Text>
            </View>
            <Icon name="arrowRight" size={16} color="#007C78" />
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2', justifyContent: 'center', padding: 24 },
  sheet: {
    backgroundColor: 'white',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E8E0D0',
    padding: 24,
    shadowColor: '#0B1F33',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 12,
    elevation: 4,
  },
  sheetTitle: { fontSize: 24, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk', marginBottom: 4 },
  sheetSubtitle: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans', marginBottom: 24 },
  action: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#F2EDE3',
    marginBottom: 10,
    backgroundColor: '#FBF8F2',
  },
  actionIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#D7F1EE',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  actionText: { flex: 1 },
  actionTitle: { fontSize: 15, fontWeight: '600', color: '#0B1F33', fontFamily: 'IBM Plex Sans' },
  actionDesc: { fontSize: 12, color: '#17324D', fontFamily: 'IBM Plex Sans', marginTop: 2 },
});
