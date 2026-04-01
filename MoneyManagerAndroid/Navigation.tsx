import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { NavigationContainer } from '@react-navigation/native';
import Dashboard from './screens/Dashboard';
import Payments  from './screens/Payments';
import History   from './screens/History';
import Settings  from './screens/Settings';
import { Theme } from './constants/Theme';

const Tab = createBottomTabNavigator();

// Simple icon via emoji since we don't have vector icons installed
function TabIcon({ emoji, focused }: { emoji: string; focused: boolean }) {
  return (
    <View style={[tabStyles.iconWrap, focused && tabStyles.iconWrapActive]}>
      <Text style={[tabStyles.emoji, { opacity: focused ? 1 : 0.55 }]}>{emoji}</Text>
    </View>
  );
}

export default function Navigation() {
  return (
    <NavigationContainer>
      <Tab.Navigator
        screenOptions={{
          headerShown: false,
          tabBarStyle: {
            backgroundColor: Theme.colors.bgSecondary,
            borderTopColor: 'rgba(255,255,255,0.06)',
            borderTopWidth: 1,
            elevation: 0,
            shadowOpacity: 0,
            height: 82,
            paddingBottom: 20,
            paddingTop: 10,
          },
          tabBarActiveTintColor:   Theme.colors.accent1,
          tabBarInactiveTintColor: Theme.colors.textSecondary,
          tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
        }}
      >
        <Tab.Screen
          name="Dashboard"
          component={Dashboard}
          options={{
            tabBarLabel: 'Home',
            tabBarIcon: ({ focused }) => <TabIcon emoji="🏠" focused={focused} />,
          }}
        />
        <Tab.Screen
          name="Payments"
          component={Payments}
          options={{
            tabBarLabel: 'Payments',
            tabBarIcon: ({ focused }) => <TabIcon emoji="👥" focused={focused} />,
          }}
        />
        <Tab.Screen
          name="History"
          component={History}
          options={{
            tabBarLabel: 'History',
            tabBarIcon: ({ focused }) => <TabIcon emoji="🕐" focused={focused} />,
          }}
        />
        <Tab.Screen
          name="Settings"
          component={Settings}
          options={{
            tabBarLabel: 'Settings',
            tabBarIcon: ({ focused }) => <TabIcon emoji="⚙️" focused={focused} />,
          }}
        />
      </Tab.Navigator>
    </NavigationContainer>
  );
}

const tabStyles = StyleSheet.create({
  iconWrap:       { width: 28, height: 28, alignItems: 'center', justifyContent: 'center', borderRadius: 8 },
  iconWrapActive: { backgroundColor: Theme.colors.accent1 + '22' },
  emoji:          { fontSize: 16 },
});
