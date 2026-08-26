import { useEffect } from 'react';
import { Platform } from 'react-native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import * as SplashScreen from 'expo-splash-screen';
import {
  useFonts,
  SpaceGrotesk_400Regular,
  SpaceGrotesk_600SemiBold,
  SpaceGrotesk_700Bold,
  IBMPlexSans_400Regular,
  IBMPlexSans_500Medium,
  IBMPlexSans_600SemiBold,
  IBMPlexMono_400Regular,
  IBMPlexMono_500Medium,
} from '@expo-google-fonts/dev';
import { BackendGate } from '../components/BackendGate';
import { loadSavedApiUrl } from '../config';

// Keep splash screen visible while loading fonts
SplashScreen.preventAutoHideAsync().catch(() => {});

// Load any saved backend URL from AsyncStorage at startup
loadSavedApiUrl().catch(() => {});

export default function RootLayout() {
  const [fontsLoaded] = useFonts({
    SpaceGrotesk: SpaceGrotesk_700Bold,
    'Space Grotesk': SpaceGrotesk_700Bold,
    'IBM Plex Sans': IBMPlexSans_400Regular,
    'IBM Plex Mono': IBMPlexMono_400Regular,
  });

  useEffect(() => {
    if (fontsLoaded) {
      SplashScreen.hideAsync().catch(() => {});
    }
  }, [fontsLoaded]);

  if (!fontsLoaded) return null;

  return (
    <SafeAreaProvider>
      {/* Full screen: hide status bar, transparent on Android */}
      <StatusBar
        style="light"
        backgroundColor="transparent"
        translucent={Platform.OS === 'android'}
      />
      <BackendGate>
        <Stack
          screenOptions={{
            headerShown: false,
            animation: 'slide_from_bottom',
            contentStyle: { backgroundColor: '#0B1F33' },
          }}
        >
          <Stack.Screen name="(tabs)" />
          <Stack.Screen
            name="property/[slug]"
            options={{ animation: 'slide_from_right' }}
          />
          <Stack.Screen
            name="property/tour/[slug]"
            options={{ animation: 'fade', presentation: 'fullScreenModal' }}
          />
          <Stack.Screen
            name="listing/create"
            options={{ animation: 'slide_from_bottom' }}
          />
          <Stack.Screen
            name="capture/start"
            options={{ animation: 'slide_from_bottom' }}
          />
          <Stack.Screen
            name="capture/scan"
            options={{ animation: 'slide_from_bottom' }}
          />
          <Stack.Screen
            name="capture/check-pass"
            options={{ animation: 'slide_from_bottom' }}
          />
          <Stack.Screen
            name="capture/scan-room"
            options={{ animation: 'slide_from_bottom', presentation: 'fullScreenModal' }}
          />
          <Stack.Screen
            name="chat/[conversationId]"
            options={{ animation: 'slide_from_bottom', presentation: 'modal' }}
          />
        </Stack>
      </BackendGate>
    </SafeAreaProvider>
  );
}
