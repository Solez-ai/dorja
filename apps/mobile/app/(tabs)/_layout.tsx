import { Tabs } from 'expo-router';
import { Text, View } from 'react-native';
import { Icon } from '../../components/Icons';

const COLORS = {
  ink950: '#0B1F33',
  jol600: '#007C78',
  paper50: '#FBF8F2',
  sand300: '#D9CCB9',
};

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: COLORS.jol600,
        tabBarInactiveTintColor: COLORS.sand300,
        tabBarStyle: {
          backgroundColor: COLORS.paper50,
          borderTopColor: COLORS.sand300,
          height: 64,
          paddingBottom: 8,
          paddingTop: 4,
        },
        tabBarLabelStyle: {
          fontSize: 10,
          fontWeight: '600',
        },
        headerStyle: {
          backgroundColor: COLORS.paper50,
        },
        headerTintColor: COLORS.ink950,
        headerTitleStyle: {
          fontFamily: 'Space Grotesk',
          fontWeight: '700',
        },
      }}
    >
      <Tabs.Screen
        name="explore"
        options={{
          title: 'Explore',
          headerTitle: 'DORJA',
          tabBarIcon: ({ color }) => <Icon name="explore" size={22} color={color} />,
        }}
      />
      <Tabs.Screen
        name="inbox"
        options={{
          title: 'Inbox',
          tabBarIcon: ({ color }) => <Icon name="message" size={22} color={color} />,
        }}
      />
      <Tabs.Screen
        name="capture"
        options={{
          title: 'Capture',
          headerTitle: 'Actions',
          tabBarIcon: ({ color }) => (
            <View style={{
              backgroundColor: COLORS.jol600,
              width: 48,
              height: 48,
              borderRadius: 24,
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: 16,
            }}>
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
          tabBarIcon: ({ color }) => <Icon name="calendar" size={22} color={color} />,
        }}
      />
      <Tabs.Screen
        name="account"
        options={{
          title: 'Account',
          tabBarIcon: ({ color }) => <Icon name="user" size={22} color={color} />,
        }}
      />
      {/* Hide auth from tab bar */}
      <Tabs.Screen name="auth" options={{ href: null }} />
    </Tabs>
  );
}
