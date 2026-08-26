import { Tabs } from 'expo-router';
import { View, Platform } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Icon } from '../../components/Icons';

const COLORS = {
  ink950: '#0B1F33',
  jol600: '#007C78',
  paper50: '#FBF8F2',
  sand300: '#D9CCB9',
};

export default function TabsLayout() {
  const insets = useSafeAreaInsets();

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: COLORS.jol600,
        tabBarInactiveTintColor: COLORS.sand300,
        tabBarStyle: {
          backgroundColor: COLORS.paper50,
          borderTopColor: COLORS.sand300,
          height: 60 + insets.bottom,
          paddingBottom: insets.bottom + 4,
          paddingTop: 6,
        },
        tabBarLabelStyle: {
          fontSize: 10,
          fontWeight: '600',
          fontFamily: 'IBM Plex Sans',
        },
        headerStyle: {
          backgroundColor: COLORS.paper50,
        },
        headerTintColor: COLORS.ink950,
        headerTitleStyle: {
          fontFamily: 'Space Grotesk',
          fontWeight: '700',
        },
        headerShadowVisible: false,
        headerLeftContainerStyle: { paddingLeft: 16 },
      }}
    >
      <Tabs.Screen
        name="explore"
        options={{
          title: 'Explore',
          headerTitle: 'DORJA',
          tabBarIcon: ({ color, focused }) => (
            <Icon name="explore" size={focused ? 24 : 22} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="inbox"
        options={{
          title: 'Inbox',
          tabBarIcon: ({ color, focused }) => (
            <Icon name="message" size={focused ? 24 : 22} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="capture"
        options={{
          title: 'Actions',
          headerTitle: 'Actions',
          tabBarIcon: () => (
            <View
              style={{
                backgroundColor: COLORS.jol600,
                width: 48,
                height: 48,
                borderRadius: 24,
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: 16,
                shadowColor: COLORS.jol600,
                shadowOffset: { width: 0, height: 4 },
                shadowOpacity: 0.3,
                shadowRadius: 8,
                elevation: 6,
              }}
            >
              <Icon name="camera" size={22} color="white" />
            </View>
          ),
          tabBarLabel: () => null,
        }}
      />
      <Tabs.Screen
        name="visits"
        options={{
          title: 'Visits',
          tabBarIcon: ({ color, focused }) => (
            <Icon name="visits" size={focused ? 24 : 22} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="account"
        options={{
          title: 'Account',
          tabBarIcon: ({ color, focused }) => (
            <Icon name="account" size={focused ? 24 : 22} color={color} />
          ),
        }}
      />
      <Tabs.Screen name="auth" options={{ href: null }} />
    </Tabs>
  );
}
