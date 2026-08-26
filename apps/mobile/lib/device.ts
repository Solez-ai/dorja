import { Platform, Dimensions } from 'react-native';

export interface DeviceCapabilities {
  compatible: boolean;
  cameraAvailable: boolean;
  screenResolution: string;
  screenScore: 'low' | 'medium' | 'high';
  osVersion: string;
  issues: string[];
}

export function checkDeviceCapabilities(): DeviceCapabilities {
  const { width, height } = Dimensions.get('window');
  const pixelCount = width * height;
  const issues: string[] = [];

  // Check screen resolution
  let screenScore: 'low' | 'medium' | 'high' = 'high';
  if (pixelCount < 400000) {
    screenScore = 'low';
    issues.push('Screen resolution is below recommended (720p+). Scan quality may be limited.');
  } else if (pixelCount < 700000) {
    screenScore = 'medium';
  }

  // Check OS version
  const osVersion = Platform.Version?.toString() || 'unknown';
  const majorVersion = Platform.OS === 'android'
    ? parseInt(osVersion, 10)
    : parseInt(osVersion, 10);

  if (Platform.OS === 'android' && majorVersion < 24) {
    issues.push('Android version is below 7.0. Some features may not work.');
  }

  // Camera availability (expo-camera is bundled, always available in builds)
  const cameraAvailable = true;

  // Overall compatibility
  const compatible = issues.length === 0 || screenScore !== 'low';

  return {
    compatible,
    cameraAvailable,
    screenResolution: `${Math.round(width)}x${Math.round(height)}`,
    screenScore,
    osVersion,
    issues,
  };
}

export function getScreenResolutionForScanner(): { width: number; height: number } {
  const { width, height } = Dimensions.get('window');
  // Scanner uses full screen
  return { width, height };
}
