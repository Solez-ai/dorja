import { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { router } from 'expo-router';
import { Icon, IconCircle, Badge } from '../../components/Icons';

export default function CheckPassScreen() {
  const [passCode, setPassCode] = useState('');
  const [verified, setVerified] = useState<boolean | null>(null);

  const verifyPass = () => {
    if (!passCode.trim()) {
      Alert.alert('Enter pass code', 'Enter or scan a viewing pass code.');
      return;
    }
    // Demo verification — always succeeds
    setVerified(true);
  };

  return (
    <View style={s.container}>
      <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
        <Icon name="back" size={16} color="#007C78" />
        <Text style={s.backText}>Back</Text>
      </TouchableOpacity>

      <View style={s.centerContent}>
        <IconCircle name="shield" size={72} bgColor="#D7F1EE" iconColor="#007C78" iconSize={32} />
        <Text style={s.title}>Check Viewing Pass</Text>
        <Text style={s.sub}>
          Verify a SafeView viewing pass before entering a property.
        </Text>

        {verified === null ? (
          <>
            {/* Viewfinder */}
            <View style={s.viewfinder}>
              <View style={s.cornerTL} />
              <View style={s.cornerTR} />
              <View style={s.cornerBL} />
              <View style={s.cornerBR} />
              <Text style={s.viewfinderText}>Scan QR pass</Text>
            </View>

            {/* Manual entry */}
            <View style={s.manualSection}>
              <Text style={s.manualLabel}>Or enter pass code</Text>
              <View style={s.inputRow}>
                <TextInput
                  style={s.input}
                  value={passCode}
                  onChangeText={setPassCode}
                  placeholder="PASS-XXXX-XXXX"
                  placeholderTextColor="#C4B5A0"
                  autoCapitalize="characters"
                />
                <TouchableOpacity style={s.verifyBtn} onPress={verifyPass}>
                  <Icon name="check" size={16} color="white" />
                </TouchableOpacity>
              </View>
            </View>
          </>
        ) : (
          /* Verified result */
          <View style={s.resultCard}>
            <View style={s.resultIcon}>
              <Icon name="check" size={32} color="#007C78" />
            </View>
            <Text style={s.resultTitle}>Pass Verified</Text>
            <Badge text="VALID" />
            <View style={s.resultDetails}>
              <View style={s.resultRow}>
                <Icon name="home" size={14} color="#17324D" />
                <Text style={s.resultText}>Property: Family Apartment, Mirpur 11</Text>
              </View>
              <View style={s.resultRow}>
                <Icon name="user" size={14} color="#17324D" />
                <Text style={s.resultText}>Buyer: Identity confirmed</Text>
              </View>
              <View style={s.resultRow}>
                <Icon name="calendar" size={14} color="#17324D" />
                <Text style={s.resultText}>Date: Today, 4:30–5:00 PM</Text>
              </View>
              <View style={s.resultRow}>
                <Icon name="check" size={14} color="#007C78" />
                <Text style={s.resultText}>Status: All checks passed</Text>
              </View>
            </View>

            <TouchableOpacity style={s.resetBtn} onPress={() => { setVerified(null); setPassCode(''); }}>
              <Text style={s.resetBtnText}>Verify Another Pass</Text>
            </TouchableOpacity>
          </View>
        )}
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
    width: '100%', height: 180, backgroundColor: '#1A1A1A', borderRadius: 8, position: 'relative',
    alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: '#333',
  },
  cornerTL: { position: 'absolute', top: 12, left: 12, width: 24, height: 24, borderTopWidth: 2, borderLeftWidth: 2, borderColor: '#007C78' },
  cornerTR: { position: 'absolute', top: 12, right: 12, width: 24, height: 24, borderTopWidth: 2, borderRightWidth: 2, borderColor: '#007C78' },
  cornerBL: { position: 'absolute', bottom: 12, left: 12, width: 24, height: 24, borderBottomWidth: 2, borderLeftWidth: 2, borderColor: '#007C78' },
  cornerBR: { position: 'absolute', bottom: 12, right: 12, width: 24, height: 24, borderBottomWidth: 2, borderRightWidth: 2, borderColor: '#007C78' },
  viewfinderText: { color: '#555', fontSize: 14, fontFamily: 'IBM Plex Sans' },

  manualSection: { width: '100%', marginTop: 8 },
  manualLabel: { fontSize: 12, color: '#17324D', fontFamily: 'IBM Plex Sans', textAlign: 'center', marginBottom: 8 },
  inputRow: { flexDirection: 'row', gap: 8 },
  input: { flex: 1, backgroundColor: 'white', borderWidth: 1, borderColor: '#D9CCB9', borderRadius: 4, padding: 12, fontSize: 14, fontFamily: 'IBM Plex Mono', color: '#0B1F33' },
  verifyBtn: { width: 48, height: 48, borderRadius: 4, backgroundColor: '#007C78', alignItems: 'center', justifyContent: 'center' },

  resultCard: { backgroundColor: 'white', borderRadius: 8, borderWidth: 1, borderColor: '#D7F1EE', padding: 24, width: '100%', alignItems: 'center', gap: 12 },
  resultIcon: { width: 64, height: 64, borderRadius: 32, backgroundColor: '#D7F1EE', alignItems: 'center', justifyContent: 'center' },
  resultTitle: { fontSize: 20, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk' },
  resultDetails: { width: '100%', gap: 8, marginTop: 12 },
  resultRow: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingVertical: 4 },
  resultText: { flex: 1, fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  resetBtn: { width: '100%', padding: 12, borderRadius: 4, borderWidth: 1, borderColor: '#D9CCB9', alignItems: 'center', marginTop: 12 },
  resetBtnText: { fontSize: 14, color: '#007C78', fontWeight: '600', fontFamily: 'IBM Plex Sans' },
});
