import type { ExpoConfig } from 'expo/config';

const config: ExpoConfig = {
  name: 'DORJA',
  slug: 'dorja',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/android-app-icon-512.png',
  scheme: 'dorja',
  userInterfaceStyle: 'light',
  newArchEnabled: true,
  splash: {
    image: './assets/dorja-logo-512-transparent.png',
    resizeMode: 'contain',
    backgroundColor: '#FBF8F2',
  },
  ios: {
    supportsTablet: true,
    bundleIdentifier: 'com.dorja.app',
    infoPlist: {
      UIStatusBarHidden: true,
    },
  },
  android: {
    adaptiveIcon: {
      foregroundImage: './assets/android-adaptive-foreground-432.png',
      backgroundColor: '#FBF8F2',
    },
    package: 'com.dorja.app',
    versionCode: 1,
    permissions: ['CAMERA', 'INTERNET', 'ACCESS_NETWORK_STATE'],
    backgroundColor: '#0B1F33',
  },
  extra: {
    eas: {
      projectId: 'f944c233-7540-46c6-b3ba-c0f7fbcfe9db',
    },
  },
  web: {
    bundler: 'metro',
    output: 'static',
    favicon: './assets/favicon.ico',
  },
  plugins: [
    'expo-router',
    [
      'expo-camera',
      {
        cameraPermission:
          'DORJA needs camera access to scan rooms in 3D and scan QR codes. Your camera feed is never recorded or uploaded to our servers.',
      },
    ],
    'expo-secure-store',
    'expo-font',
    [
      'expo-splash-screen',
      {
        image: './assets/dorja-logo-512-transparent.png',
        resizeMode: 'contain',
        backgroundColor: '#FBF8F2',
      },
    ],
  ],
  experiments: {
    typedRoutes: true,
  },
};

export default config;
