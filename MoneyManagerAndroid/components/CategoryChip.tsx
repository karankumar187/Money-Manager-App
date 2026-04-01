import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Theme } from '../constants/Theme';

interface CategoryChipProps {
  emoji: string;
  label: string;
  color: string;
  amount: string;
  onPress: () => void;
}

export const CategoryChip: React.FC<CategoryChipProps> = ({ emoji, label, color, amount, onPress }) => {
  return (
    <TouchableOpacity onPress={onPress} style={styles.container}>
      <View style={[styles.circle, { backgroundColor: color + '2E' }]}> 
        <Text style={styles.emoji}>{emoji}</Text>
      </View>
      <Text style={styles.label} numberOfLines={1}>{label}</Text>
      <Text style={[styles.amount, { color }]} numberOfLines={1} adjustsFontSizeToFit>{amount}</Text>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    minWidth: 68,
    alignItems: 'center',
    marginRight: 10,
  },
  circle: {
    width: 50,
    height: 50,
    borderRadius: 25,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 6,
  },
  emoji: {
    fontSize: 24,
  },
  label: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '600',
    marginBottom: 2,
  },
  amount: {
    fontSize: 12,
    fontWeight: 'bold',
  }
});
