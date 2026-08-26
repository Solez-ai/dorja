import { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { router } from 'expo-router';
import { Icon, IconCircle } from '../../components/Icons';

export default function ScanScreen() {
  const [code, setCode] = useState('');

  const handleScan = () => {
    if (code.trim()) {
      // In a real app this would open the camera for QR scanning
      // For demo, accept manual code entry
      router.push(`/property/${code.trim()}` as any);
    } else {
      Alert.alert('Enter code', 'Type or paste a DORJA property code to view its passport.');
    }
  };

  return (
    <View style={s.container}>
      <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
        <Icon name="back" size={16} color="#007C78" />
        <Text style={s.backText}>Back</Text>
      </TouchableOpacity>

      <View style={s.centerContent}>
        <IconCircle name="qr" size={72} bgColor="#D7F1EE" iconColor="#007C78" iconSize={32} />
        <Text style={s.title}>Scan Property Code</Text>
        <Text style={s.sub}>
          Point your camera at a DORJA property QR code, or enter the code manually below.
        </Text>

        {/* Camera viewfinder placeholder */}
        <View style={s.viewfinder}>
          <View style={s.cornerTL} />
          <View style={s.cornerTR} />
          <View style={s.cornerBL} />
          <View style={s.cornerBR} />
          <Text style={s.viewfinderText}>Camera viewfinder</Text>
          <Text style={s.viewfinderHint}>Position QR code within the frame</Text>
        </View>

        {/* Manual entry */}
        <View style={s.manualSection}>
          <Text style={s.manualLabel}>Or enter code manually</Text>
          <View style={s.inputRow}>
            <TextInput
              style={s.input}
              value={code}
              onChangeText={setCode}
              placeholder="e.g. uttara-family-apartment"
              placeholderTextColor="#C4B5A0"
              autoCapitalize="none"
            />
            <TouchableOpacity style={s.goBtn} onPress={handleScan}>
              <Icon name="arrowRight" size={16} color="white" />
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2', padding: 20 },
  backBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, marginBottom: 20 },
  backText: { fontSize: 14, color: '#007C78', fontFamily: 'IBM Plex Sans' },
  centerContent: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 16 },
  title: { fontSize: 22, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk', textAlign: 'center' },
  sub: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans', textAlign: 'center', lineHeight: 20, maxWidth: 320 },

  viewfinder: {
    width: '100%',
    height: 200,
    backgroundColor: '#1A1A1A',
    borderRadius: 8,
    position: 'relative',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 16,
    borderWidth: 1,
    borderColor: '#333',
  },
  cornerTL: { position: 'absolute', top: 12, left: 12, width: 24, height: 24, borderTopWidth: 2, borderLeftWidth: 2, borderColor: '#007C78' },
  cornerTR: { position: 'absolute', top: 12, right: 12, width: 24, height: 24, borderTopWidth: 2, borderRightWidth: 2, borderColor: '#007C78' },
  cornerBL: { position: 'absolute', bottom: 12, left: 12, width: 24, height: 24, borderBottomWidth: 2, borderLeftWidth: 2, borderColor: '#007C78' },
  cornerBR: { position: 'absolute', bottom: 12, right: 12, width: 24, height: 24, borderBottomWidth: 2, borderRightWidth: 2, borderColor: '#007C78' },
  viewfinderText: { color: '#555', fontSize: 14, fontFamily: 'IBM Plex Sans' },
  viewfinderHint: { color: '#444', fontSize: 11, fontFamily: 'IBM Plex Sans', marginTop: 4 },

  manualSection: { width: '100%', marginTop: 16 },
  manualLabel: { fontSize: 12, color: '#17324D', fontFamily: 'IBM Plex Sans', textAlign: 'center', marginBottom: 8 },
  inputRow: { flexDirection: 'row', gap: 8 },
  input: { flex: 1, backgroundColor: 'white', borderWidth: 1, borderColor: '#D9CCB9', borderRadius: 4, padding: 12, fontSize: 14, fontFamily: 'IBM Plex Mono', color: '#0B1F33' },
  goBtn: { width: 48, height: 48, borderRadius: 4, backgroundColor: '#007C78', alignItems: 'center', justifyContent: 'center' },
});
