import { StatusBar } from 'expo-status-bar';
import { StyleSheet, View } from 'react-native';
import { Theme } from './constants/Theme';
import { DataStoreProvider } from './store/DataStore';
import Navigation from './Navigation';

export default function App() {
  return (
    <DataStoreProvider>
      <View style={styles.container}>
        <StatusBar style="light" />
        <Navigation />
      </View>
    </DataStoreProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Theme.colors.bgPrimary,
  },
});
