import { useState } from 'react';
import { View, Text, TextInput, ScrollView, TouchableOpacity, StyleSheet, Alert, Image } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { Icon, Badge } from '../../components/Icons';
import { ensureAuth, authFetch } from '../../lib/api';
import { getApiUrl } from '../../config';

const ROOM_TYPES = [
  { value: 'LIVING_ROOM', label: 'Living Room', icon: 'home' },
  { value: 'BEDROOM', label: 'Bedroom', icon: 'door' },
  { value: 'KITCHEN', label: 'Kitchen', icon: 'home' },
  { value: 'BATHROOM', label: 'Bathroom', icon: 'lock' },
  { value: 'BALCONY', label: 'Balcony', icon: 'eye' },
  { value: 'DINING_ROOM', label: 'Dining Room', icon: 'home' },
];

interface RoomEntry {
  roomType: string;
  displayName: string;
  ordinal: number;
  photo?: string; // base64 data URL
  photoLabel?: string; // custom label for the photo
}

export default function CreateListingScreen() {
  const [title, setTitle] = useState('');
  const [intent, setIntent] = useState<'RENT' | 'SALE'>('RENT');
  const [propertyType, setPropertyType] = useState('APARTMENT');
  const [publicArea, setPublicArea] = useState('');
  const [exactAddress, setExactAddress] = useState('');
  const [price, setPrice] = useState('');
  const [rooms, setRooms] = useState<RoomEntry[]>([]);
  const [loading, setLoading] = useState(false);

  const addRoom = (rt: string) => {
    const existing = rooms.filter(r => r.roomType === rt).length;
    const lbl = ROOM_TYPES.find(r => r.value === rt)?.label || rt;
    setRooms([...rooms, {
      roomType: rt,
      displayName: existing > 0 ? lbl + ' ' + (existing + 1) : lbl,
      ordinal: rooms.length,
    }]);
  };

  const removeRoom = (i: number) => {
    setRooms(rooms.filter((_, idx) => idx !== i).map((r, idx) => ({ ...r, ordinal: idx })));
  };

  const updateRoomLabel = (i: number, label: string) => {
    setRooms(rooms.map((r, idx) => idx === i ? { ...r, displayName: label } : r));
  };

  const updateRoomPhoto = (i: number, photo: string) => {
    setRooms(rooms.map((r, idx) => idx === i ? { ...r, photo } : r));
  };

  const updateRoomPhotoLabel = (i: number, label: string) => {
    setRooms(rooms.map((r, idx) => idx === i ? { ...r, photoLabel: label } : r));
  };

  const submit = async () => {
    if (!title || !publicArea || !exactAddress || !price || rooms.length === 0) {
      Alert.alert('Missing fields', 'Fill all fields and add at least one room.');
      return;
    }
    setLoading(true);
    try {
      const auth = await ensureAuth();
      if (!auth?.token) { Alert.alert('Auth failed', 'Could not authenticate.'); setLoading(false); return; }
      const token = auth.token;

      const lr = await fetch(getApiUrl() + '/v1/listings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
        body: JSON.stringify({
          title, intent, propertyType, publicArea, exactAddress,
          priceAmount: parseInt(price, 10), currency: 'BDT',
          rooms: rooms.map(r => ({ roomType: r.roomType, displayName: r.displayName, ordinal: r.ordinal })),
        }),
      });
      const ld = await lr.json();
      if (lr.ok && ld.data) {
        // Upload photos for each room that has one
        for (let i = 0; i < rooms.length; i++) {
          if (rooms[i].photo) {
            try {
              await fetch(getApiUrl() + '/v1/photos/upload', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
                body: JSON.stringify({
                  listingId: ld.data.id,
                  dataUrl: rooms[i].photo,
                  label: rooms[i].photoLabel || rooms[i].displayName,
                  roomType: rooms[i].roomType,
                }),
              });
            } catch {}
          }
        }
        Alert.alert('Created!', `"${title}" is live with ${rooms.length} rooms.`, [{ text: 'OK' }]);
        setTitle(''); setPrice(''); setPublicArea(''); setExactAddress(''); setRooms([]);
      } else {
        Alert.alert('Error', ld.error?.message || 'Failed to create.');
      }
    } catch (e: any) {
      Alert.alert('Network error', e.message || 'Cannot reach server.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView style={s.container} showsVerticalScrollIndicator={false}>
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={s.back}><Icon name="back" size={14} color="#007C78" /> Back</Text>
        </TouchableOpacity>
        <Text style={s.title}>Add Your Property</Text>
        <Text style={s.sub}>Fill details to list on DORJA. Exact address stays private.</Text>
      </View>

      {/* Title */}
      <View style={s.field}>
        <Text style={s.label}>Property Title *</Text>
        <TextInput style={s.input} value={title} onChangeText={setTitle} placeholder="Family Apartment in Dhanmondi" placeholderTextColor="#C4B5A0" />
      </View>

      {/* Intent */}
      <View style={s.field}>
        <Text style={s.label}>For Rent or Sale? *</Text>
        <View style={s.row}>
          {(['RENT', 'SALE'] as const).map(i => (
            <TouchableOpacity key={i} style={[s.chip, intent === i && s.chipActive]} onPress={() => setIntent(i)}>
              <Icon name={i === 'RENT' ? 'home' : 'building'} size={12} color={intent === i ? '#006B68' : '#17324D'} />
              <Text style={[s.chipText, intent === i && s.chipTextActive]}>{i === 'RENT' ? 'For Rent' : 'For Sale'}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Property type */}
      <View style={s.field}>
        <Text style={s.label}>Property Type *</Text>
        <View style={s.row}>
          {['APARTMENT', 'HOUSE', 'ROOM', 'OFFICE', 'SHOP'].map(t => (
            <TouchableOpacity key={t} style={[s.chip, propertyType === t && s.chipActive]} onPress={() => setPropertyType(t)}>
              <Text style={[s.chipText, propertyType === t && s.chipTextActive]}>{t.charAt(0) + t.slice(1).toLowerCase()}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Area */}
      <View style={s.field}>
        <Text style={s.label}>Approximate Area *</Text>
        <TextInput style={s.input} value={publicArea} onChangeText={setPublicArea} placeholder="Dhanmondi 27" placeholderTextColor="#C4B5A0" />
      </View>

      {/* Exact address */}
      <View style={s.field}>
        <Text style={s.label}>Exact Address * (kept private)</Text>
        <TextInput style={s.input} value={exactAddress} onChangeText={setExactAddress} placeholder="Full address for appointments" placeholderTextColor="#C4B5A0" multiline numberOfLines={2} />
      </View>

      {/* Price */}
      <View style={s.field}>
        <Text style={s.label}>Price (BDT) *</Text>
        <TextInput style={s.input} value={price} onChangeText={setPrice} placeholder={intent === 'RENT' ? 'Monthly rent' : 'Asking price'} placeholderTextColor="#C4B5A0" keyboardType="numeric" />
      </View>

      {/* Rooms */}
      <View style={s.field}>
        <Text style={s.label}>Rooms * (tap to add)</Text>
        <View style={s.roomTypeGrid}>
          {ROOM_TYPES.map(r => (
            <TouchableOpacity key={r.value} style={s.roomTypeBtn} onPress={() => addRoom(r.value)}>
              <Icon name={r.icon} size={14} color="#007C78" />
              <Text style={s.roomTypeText}>+ {r.label}</Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Added rooms with photo upload */}
        {rooms.length > 0 && (
          <View style={s.roomList}>
            {rooms.map((room, idx) => (
              <View key={idx} style={s.roomCard}>
                <View style={s.roomCardHeader}>
                  <Icon name="door" size={14} color="#007C78" />
                  <TextInput
                    style={s.roomNameInput}
                    value={room.displayName}
                    onChangeText={(t) => updateRoomLabel(idx, t)}
                    placeholder="Room name"
                    placeholderTextColor="#C4B5A0"
                  />
                  <TouchableOpacity onPress={() => removeRoom(idx)}>
                    <Icon name="close" size={16} color="#B91C1C" />
                  </TouchableOpacity>
                </View>

                {/* Photo area */}
                {room.photo ? (
                  <View style={s.photoPreview}>
                    <View style={s.photoThumb}>
                      <Icon name="camera" size={20} color="#007C78" />
                      <Text style={s.photoThumbText}>Photo added</Text>
                    </View>
                    <TextInput
                      style={s.photoLabelInput}
                      value={room.photoLabel || ''}
                      onChangeText={(t) => updateRoomPhotoLabel(idx, t)}
                      placeholder="Label this photo (e.g. 'Master bedroom wardrobe')"
                      placeholderTextColor="#C4B5A0"
                    />
                    <TouchableOpacity onPress={() => updateRoomPhoto(idx, '')}>
                      <Text style={s.removePhotoText}>Remove photo</Text>
                    </TouchableOpacity>
                  </View>
                ) : (
                  <TouchableOpacity
                    style={s.photoUpload}
                    onPress={() => {
                      // In production: launch image picker
                      // For demo: generate a placeholder
                      updateRoomPhoto(idx, 'data:image/placeholder;base64,demo');
                      updateRoomPhotoLabel(idx, room.displayName);
                    }}
                  >
                    <Icon name="camera" size={20} color="#D9CCB9" />
                    <Text style={s.photoUploadText}>Add photo of {room.displayName}</Text>
                    <Text style={s.photoUploadHint}>Label it with a custom name (e.g. "Wardrobe view")</Text>
                  </TouchableOpacity>
                )}
              </View>
            ))}
          </View>
        )}
      </View>

      <TouchableOpacity style={[s.submit, loading && { opacity: 0.6 }]} onPress={submit} disabled={loading}>
        <Icon name={loading ? 'clock' : 'check'} size={16} color="white" />
        <Text style={s.submitText}>{loading ? 'Creating...' : 'Create Listing'}</Text>
      </TouchableOpacity>
      <View style={{ height: 60 }} />
    </ScrollView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8F2', padding: 20 },
  header: { marginBottom: 24 },
  back: { fontSize: 14, color: '#007C78', fontFamily: 'IBM Plex Sans', marginBottom: 12, flexDirection: 'row', alignItems: 'center', gap: 4 },
  title: { fontSize: 24, fontWeight: '700', color: '#0B1F33', fontFamily: 'Space Grotesk' },
  sub: { fontSize: 14, color: '#17324D', fontFamily: 'IBM Plex Sans', marginTop: 4 },

  field: { marginBottom: 20 },
  label: { fontSize: 13, fontWeight: '600', color: '#17324D', fontFamily: 'IBM Plex Sans', marginBottom: 8 },
  input: { backgroundColor: 'white', borderWidth: 1, borderColor: '#D9CCB9', borderRadius: 2, padding: 12, fontSize: 14, fontFamily: 'IBM Plex Sans', color: '#0B1F33', minHeight: 44 },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 14, paddingVertical: 8, borderRadius: 2, borderWidth: 1, borderColor: '#D9CCB9', backgroundColor: 'white' },
  chipActive: { borderColor: '#007C78', backgroundColor: '#D7F1EE' },
  chipText: { fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  chipTextActive: { color: '#006B68', fontWeight: '600' },

  roomTypeGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  roomTypeBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 10, paddingVertical: 6, borderRadius: 2, borderWidth: 1, borderColor: '#007C78', backgroundColor: '#D7F1EE' },
  roomTypeText: { fontSize: 12, color: '#006B68', fontWeight: '500', fontFamily: 'IBM Plex Sans' },

  roomList: { marginTop: 12, gap: 10 },
  roomCard: { backgroundColor: 'white', borderRadius: 4, borderWidth: 1, borderColor: '#E8E0D0', padding: 12 },
  roomCardHeader: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8 },
  roomNameInput: { flex: 1, fontSize: 14, fontWeight: '600', color: '#0B1F33', fontFamily: 'IBM Plex Sans', padding: 0 },

  photoUpload: { borderWidth: 1, borderColor: '#D9CCB9', borderStyle: 'dashed', borderRadius: 4, padding: 16, alignItems: 'center', gap: 4 },
  photoUploadText: { fontSize: 13, color: '#17324D', fontFamily: 'IBM Plex Sans' },
  photoUploadHint: { fontSize: 11, color: '#D9CCB9', fontFamily: 'IBM Plex Sans' },

  photoPreview: { gap: 8 },
  photoThumb: { flexDirection: 'row', alignItems: 'center', gap: 8, padding: 12, backgroundColor: '#D7F1EE', borderRadius: 4 },
  photoThumbText: { fontSize: 13, color: '#006B68', fontWeight: '600', fontFamily: 'IBM Plex Sans' },
  photoLabelInput: { backgroundColor: '#FBF8F2', borderWidth: 1, borderColor: '#D9CCB9', borderRadius: 2, padding: 8, fontSize: 13, fontFamily: 'IBM Plex Sans', color: '#0B1F33' },
  removePhotoText: { fontSize: 12, color: '#B91C1C', fontFamily: 'IBM Plex Sans', textAlign: 'center' },

  submit: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, backgroundColor: '#007C78', padding: 14, borderRadius: 2, marginTop: 8 },
  submitText: { color: 'white', fontSize: 14, fontWeight: '600', fontFamily: 'IBM Plex Sans' },
});
