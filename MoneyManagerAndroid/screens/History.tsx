import React, { useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, SafeAreaView,
  TouchableOpacity, Alert, Pressable,
} from 'react-native';
import { Theme } from '../constants/Theme';
import { useDataStore, Transaction } from '../store/DataStore';

function TxRow({ tx, onDelete }: { tx: Transaction; onDelete: () => void }) {
  const d = new Date(tx.date);
  const label = d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
  const hex = tx.categoryColor || '#A29BFE';
  const n = parseInt(hex.replace('#',''), 16);
  const [r, g, b] = [(n >> 16) & 255, (n >> 8) & 255, n & 255];

  return (
    <Pressable
      onLongPress={() => Alert.alert('Delete Transaction?', `"${tx.note}"`, [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Delete', style: 'destructive', onPress: onDelete },
      ])}
      style={styles.txCard}
    >
      <View style={[styles.txIcon, { backgroundColor: `rgba(${r},${g},${b},0.15)` }]}>
        <Text style={{ fontSize: 20 }}>{tx.categoryEmoji || '💸'}</Text>
      </View>
      <View style={{ flex: 1 }}>
        <Text style={styles.txNote} numberOfLines={1}>{tx.note}</Text>
        <Text style={styles.txMeta}>{tx.categoryName} • {tx.recipientName}</Text>
      </View>
      <View style={{ alignItems: 'flex-end' }}>
        <Text style={styles.txAmount}>-{tx.amount.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</Text>
        <Text style={styles.txDate}>{label}</Text>
      </View>
    </Pressable>
  );
}

export default function History() {
  const { transactions, deleteTransaction, currency } = useDataStore();
  const [filter, setFilter] = useState<'all' | 'week' | 'month'>('all');

  const now = new Date();
  const filtered = transactions.filter(t => {
    const d = new Date(t.date);
    if (filter === 'week') {
      const diff = (now.getTime() - d.getTime()) / 86400000;
      return diff <= 7;
    }
    if (filter === 'month') {
      return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
    }
    return true;
  }).sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

  // Group by date label
  const groups: Record<string, Transaction[]> = {};
  filtered.forEach(t => {
    const d  = new Date(t.date);
    const key = d.toLocaleDateString('en-IN', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
    if (!groups[key]) groups[key] = [];
    groups[key].push(t);
  });

  const total = filtered.reduce((s, t) => s + t.amount, 0);

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: Theme.colors.bgPrimary }}>
      <View style={styles.header}>
        <Text style={styles.pageTitle}>History</Text>
        <Text style={[styles.totalBadge, { color: Theme.colors.expenseRed }]}>
          -{currency}{total.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
        </Text>
      </View>

      {/* Filters */}
      <View style={styles.filterRow}>
        {(['all', 'month', 'week'] as const).map(f => (
          <TouchableOpacity key={f} onPress={() => setFilter(f)}
            style={[styles.filterBtn, filter === f && styles.filterBtnActive]}>
            <Text style={[styles.filterBtnText, filter === f && { color: '#fff' }]}>
              {f === 'all' ? 'All Time' : f === 'month' ? 'This Month' : 'This Week'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        {Object.keys(groups).length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={{ fontSize: 48 }}>🕐</Text>
            <Text style={{ color: Theme.colors.textSecondary, marginTop: 12, fontSize: 15 }}>No transactions found</Text>
          </View>
        ) : (
          Object.entries(groups).map(([date, txs]) => (
            <View key={date} style={{ marginBottom: 4 }}>
              <Text style={styles.dateSep}>{date}</Text>
              {txs.map(t => (
                <View key={t.id} style={{ paddingHorizontal: 20, marginBottom: 8 }}>
                  <TxRow tx={t} onDelete={() => deleteTransaction(t.id)} />
                </View>
              ))}
            </View>
          ))
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  header:      { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 20, paddingBottom: 16 },
  pageTitle:   { fontSize: 28, fontWeight: 'bold', color: '#fff' },
  totalBadge:  { fontSize: 16, fontWeight: 'bold' },
  filterRow:   { flexDirection: 'row', paddingHorizontal: 20, gap: 8, marginBottom: 20 },
  filterBtn:   { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20, backgroundColor: 'rgba(255,255,255,0.06)', borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)' },
  filterBtnActive: { backgroundColor: Theme.colors.accent1, borderColor: Theme.colors.accent1 },
  filterBtnText:   { color: Theme.colors.textSecondary, fontSize: 13, fontWeight: '600' },
  dateSep:     { fontSize: 11, fontWeight: '700', color: Theme.colors.textSecondary, letterSpacing: 1, paddingHorizontal: 24, paddingVertical: 8 },
  txCard:      { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: 'rgba(255,255,255,0.04)', borderRadius: 16, borderWidth: 1, borderColor: 'rgba(255,255,255,0.06)', padding: 14 },
  txIcon:      { width: 44, height: 44, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  txNote:      { fontSize: 15, fontWeight: '600', color: '#fff' },
  txMeta:      { fontSize: 12, color: Theme.colors.textSecondary, marginTop: 2 },
  txAmount:    { fontSize: 15, fontWeight: 'bold', color: Theme.colors.expenseRed },
  txDate:      { fontSize: 11, color: Theme.colors.textTertiary, marginTop: 2 },
  emptyState:  { alignItems: 'center', paddingTop: 80, opacity: 0.5 },
});
