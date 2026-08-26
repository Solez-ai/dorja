import type { ExpoConfig } from 'expo/config';

const config: ExpoConfig = {
  name: 'DORJA',
  slug: 'dorja',
  version: '0.1.0',
  orientation: 'portrait',
  icon: '../Assets/android-app-icon-512.png',
  scheme: 'dorja',
  userInterfaceStyle: 'light',
  newArchEnabled: true,
  splash: {
    image: '../Assets/dorja-logo-512-transparent.png',
    resizeMode: 'contain',
    backgroundColor: '#FBF8F2',
  },
  ios: {
    supportsTablet: true,
    bundleIdentifier: 'com.dorja.app',
  },
  android: {
    adaptiveIcon: {
      foregroundImage: '../Assets/android-adaptive-foreground-432.png',
      backgroundColor: '#FBF8F2',
    },
    package: 'com.dorja.app',
  },
  web: {
    bundler: 'metro',
    output: 'static',
    favicon: '../Assets/favicon.ico',
  },
  plugins: [
    'expo-router',
    'expo-camera',
    'expo-secure-store',
    'expo-font',
  ],
  experiments: {
    typedRoutes: true,
  },
};

export default config;
