import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { Icon } from '../components/Icons';

const COLORS = {
  ink950: '#0B1F33',
  jol600: '#007C78',
  paper50: '#FBF8F2',
  sand300: '#D9CCB9',
};

export default function RootLayout() {
  return (
    <>
      <StatusBar style="dark" />
      <Stack screenOptions={{ headerShown: false }}>
        {/* Tab navigator */}
        <Stack.Screen name="(tabs)" />

        {/* Full-screen routes */}
        <Stack.Screen name="property/[slug]" options={{ headerShown: false }} />
        <Stack.Screen name="property/tour/[slug]" options={{ headerShown: false, presentation: 'fullScreenModal' }} />
        <Stack.Screen name="listing/create" options={{ headerShown: false }} />
        <Stack.Screen name="capture/start" options={{ headerShown: false }} />
        <Stack.Screen name="capture/scan" options={{ headerShown: false }} />
        <Stack.Screen name="capture/check-pass" options={{ headerShown: false }} />
        <Stack.Screen name="capture/scan-room" options={{ headerShown: false, presentation: 'fullScreenModal' }} />
        <Stack.Screen name="chat/[conversationId]" options={{ headerShown: false, presentation: 'modal' }} />
      </Stack>
    </>
  );
}
