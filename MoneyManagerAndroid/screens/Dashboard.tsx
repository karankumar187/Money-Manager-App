import React, { useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, SafeAreaView,
  TouchableOpacity, Modal, TextInput, FlatList, Alert, Pressable,
} from 'react-native';
import { Theme } from '../constants/Theme';
import { useDataStore, Transaction, AppCategory } from '../store/DataStore';

// ── Palette helper ─────────────────────────────────────────
const COLORS = ['#FF6B6B','#FF9F43','#FDCB6E','#1DD1A1','#48DBFB','#A29BFE','#FD79A8','#6C5CE7','#D980FA','#B53471','#EE5A24','#009432'];

function hexToRgb(hex: string) {
  const h = hex.replace('#','');
  const n = parseInt(h, 16);
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
}

// ── Small components ──────────────────────────────────────
function SummaryPill({ label, value, color }: { label: string; value: string; color: string }) {
  return (
    <View style={[styles.pill, { borderColor: color + '40' }]}>
      <Text style={[styles.pillLabel]}>{label}</Text>
      <Text style={[styles.pillValue, { color }]}>{value}</Text>
    </View>
  );
}

function TxCard({ tx, onDelete }: { tx: Transaction; onDelete: () => void }) {
  const { r, g, b } = hexToRgb(tx.categoryColor || '#A29BFE');
  return (
    <Pressable
      onLongPress={() => Alert.alert('Delete?', `Remove "${tx.note}"?`, [
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
      <Text style={styles.txAmount}>-{tx.amount.toLocaleString('en-IN', { maximumFractionDigits: 0 })}</Text>
    </Pressable>
  );
}

// ── Add Transaction Modal ─────────────────────────────────
function AddTransactionModal({ visible, onClose }: { visible: boolean; onClose: () => void }) {
  const { categories, addTransaction, formatted, currency } = useDataStore();
  const [amount, setAmount]     = useState('');
  const [note, setNote]         = useState('');
  const [recipient, setRecipient] = useState('');
  const [selCat, setSelCat]     = useState<AppCategory | null>(null);

  const reset = () => { setAmount(''); setNote(''); setRecipient(''); setSelCat(null); };

  const handleAdd = () => {
    const a = parseFloat(amount);
    if (!a || !note.trim()) return;
    const cat = selCat || categories[0];
    addTransaction({
      id: Date.now().toString(),
      amount: a,
      recipientName: recipient.trim() || 'Unknown',
      note: note.trim(),
      categoryId: cat.id,
      categoryName: cat.name,
      categoryEmoji: cat.emoji,
      categoryColor: cat.color,
      date: new Date().toISOString(),
    });
    reset();
    onClose();
  };

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View style={styles.modalBg}>
        <View style={styles.modalHandle} />
        <Text style={styles.modalTitle}>Add Transaction</Text>

        <View style={styles.amountRow}>
          <Text style={styles.currencySymbol}>{currency}</Text>
          <TextInput
            style={styles.amountInput}
            value={amount}
            onChangeText={setAmount}
            keyboardType="decimal-pad"
            placeholder="0"
            placeholderTextColor={Theme.colors.textTertiary}
            autoFocus
          />
        </View>

        <TextInput
          style={styles.fieldInput}
          value={note}
          onChangeText={setNote}
          placeholder="Note (e.g. Lunch)"
          placeholderTextColor={Theme.colors.textTertiary}
        />

        <TextInput
          style={styles.fieldInput}
          value={recipient}
          onChangeText={setRecipient}
          placeholder="Recipient name (optional)"
          placeholderTextColor={Theme.colors.textTertiary}
        />

        <Text style={styles.subLabel}>Category</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginBottom: 20 }}>
          {categories.map(c => (
            <TouchableOpacity
              key={c.id}
              onPress={() => setSelCat(c)}
              style={[styles.catChip, selCat?.id === c.id && { borderColor: c.color, backgroundColor: c.color + '22' }]}
            >
              <Text>{c.emoji} {c.name}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        <TouchableOpacity style={[styles.addBtn, !amount && { opacity: 0.4 }]} onPress={handleAdd} disabled={!amount}>
          <Text style={styles.addBtnText}>Add Transaction</Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={() => { reset(); onClose(); }} style={styles.cancelBtn}>
          <Text style={styles.cancelBtnText}>Cancel</Text>
        </TouchableOpacity>
      </View>
    </Modal>
  );
}

// ── Main Dashboard ─────────────────────────────────────────
export default function Dashboard() {
  const { transactions, lendBorrows, budget, formatted, userName, categories } = useDataStore();
  const [showAdd, setShowAdd] = useState(false);

  const now    = new Date();
  const thisMonthTx = transactions.filter(t => {
    const d = new Date(t.date);
    return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
  });
  const monthTotal = thisMonthTx.reduce((s, t) => s + t.amount, 0);
  const todayTx    = transactions.filter(t => new Date(t.date).toDateString() === now.toDateString());
  const todayTotal = todayTx.reduce((s, t) => s + t.amount, 0);

  const totalLent     = lendBorrows.filter(l => l.type === 'lent'     && !l.isPaid).reduce((s, l) => s + (l.amount - l.paidAmount), 0);
  const totalBorrowed = lendBorrows.filter(l => l.type === 'borrowed' && !l.isPaid).reduce((s, l) => s + (l.amount - l.paidAmount), 0);

  const budgetFrac = budget > 0 ? Math.min(monthTotal / budget, 1) : 0;
  const recentTx   = [...transactions].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()).slice(0, 10);

  // Per-category spend this month
  const catSpend: Record<string, { cat: AppCategory; total: number }> = {};
  thisMonthTx.forEach(t => {
    if (!catSpend[t.categoryId]) {
      const cat = categories.find(c => c.id === t.categoryId) || { id: t.categoryId, name: t.categoryName, emoji: t.categoryEmoji, color: t.categoryColor };
      catSpend[t.categoryId] = { cat, total: 0 };
    }
    catSpend[t.categoryId].total += t.amount;
  });
  const catList = Object.values(catSpend).sort((a, b) => b.total - a.total);

  const { deleteTransaction } = useDataStore();

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: Theme.colors.bgPrimary }}>
      <FlatList
        data={recentTx}
        keyExtractor={t => t.id}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: 40 }}
        ListHeaderComponent={() => (
          <View>
            {/* Header */}
            <View style={styles.header}>
              <View>
                <Text style={styles.greeting}>Hello,</Text>
                <Text style={styles.userName}>{userName} 👋</Text>
              </View>
              <TouchableOpacity onPress={() => setShowAdd(true)} style={styles.fabSmall}>
                <Text style={{ color: '#fff', fontSize: 22, fontWeight: 'bold' }}>+</Text>
              </TouchableOpacity>
            </View>

            {/* Hero Card */}
            <View style={styles.heroCard}>
              <Text style={styles.heroLabel}>SPENT THIS MONTH</Text>
              <Text style={styles.heroAmount}>{formatted(monthTotal)}</Text>
              <View style={styles.budgetTrack}>
                <View style={[styles.budgetFill, { width: `${budgetFrac * 100}%` as any, backgroundColor: budgetFrac > 0.85 ? Theme.colors.expenseRed : Theme.colors.incomeGreen }]} />
              </View>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 6 }}>
                <Text style={styles.budgetSubtext}>Budget {formatted(budget)}</Text>
                <Text style={[styles.budgetSubtext, { color: budgetFrac > 0.85 ? Theme.colors.expenseRed : Theme.colors.textSecondary }]}>
                  {Math.round(budgetFrac * 100)}% used
                </Text>
              </View>
            </View>

            {/* Stat Pills */}
            <View style={{ flexDirection: 'row', paddingHorizontal: 20, marginBottom: 20, gap: 10 }}>
              <SummaryPill label="TODAY"    value={formatted(todayTotal)} color={Theme.colors.accent1} />
              <SummaryPill label="LENT OUT" value={formatted(totalLent)}     color={Theme.colors.incomeGreen} />
              <SummaryPill label="BORROWED" value={formatted(totalBorrowed)} color={Theme.colors.expenseRed} />
            </View>

            {/* Category Spend */}
            {catList.length > 0 && (
              <View style={{ marginBottom: 20 }}>
                <Text style={styles.sectionTitle}>By Category</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingHorizontal: 20, gap: 10 }}>
                  {catList.map(({ cat, total }) => {
                    const { r, g, b } = hexToRgb(cat.color);
                    return (
                      <View key={cat.id} style={[styles.catCard, { backgroundColor: `rgba(${r},${g},${b},0.12)`, borderColor: `rgba(${r},${g},${b},0.3)` }]}>
                        <Text style={{ fontSize: 22 }}>{cat.emoji}</Text>
                        <Text style={{ color: '#fff', fontWeight: '600', fontSize: 13, marginTop: 4 }}>{cat.name}</Text>
                        <Text style={{ color: cat.color, fontWeight: 'bold', fontSize: 14 }}>{formatted(total)}</Text>
                      </View>
                    );
                  })}
                </ScrollView>
              </View>
            )}

            {recentTx.length > 0 && <Text style={styles.sectionTitle}>Recent</Text>}
          </View>
        )}
        renderItem={({ item }) => (
          <View style={{ paddingHorizontal: 20, marginBottom: 10 }}>
            <TxCard tx={item} onDelete={() => deleteTransaction(item.id)} />
          </View>
        )}
        ListEmptyComponent={() => (
          <View style={{ alignItems: 'center', paddingTop: 60, opacity: 0.5 }}>
            <Text style={{ fontSize: 48 }}>💸</Text>
            <Text style={{ color: Theme.colors.textSecondary, marginTop: 12, fontSize: 15 }}>No transactions yet</Text>
            <Text style={{ color: Theme.colors.textTertiary, fontSize: 13, marginTop: 4 }}>Tap + to add your first one</Text>
          </View>
        )}
      />

      <AddTransactionModal visible={showAdd} onClose={() => setShowAdd(false)} />
    </SafeAreaView>
  );
}

// ── Styles ─────────────────────────────────────────────────
const styles = StyleSheet.create({
  header: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingHorizontal: 24, paddingTop: 20, paddingBottom: 20,
  },
  greeting: { fontSize: 14, color: Theme.colors.textSecondary, fontWeight: '500' },
  userName:  { fontSize: 26, fontWeight: 'bold', color: Theme.colors.textPrimary },
  fabSmall:  {
    width: 44, height: 44, borderRadius: 22,
    backgroundColor: Theme.colors.accent1, alignItems: 'center', justifyContent: 'center',
  },
  heroCard: {
    backgroundColor: 'rgba(0,0,0,0.25)', borderRadius: 22,
    borderWidth: 1, borderColor: 'rgba(255,255,255,0.06)',
    padding: 24, marginHorizontal: 20, marginBottom: 20,
  },
  heroLabel:  { fontSize: 11, fontWeight: '600', color: Theme.colors.textSecondary, letterSpacing: 1.4, marginBottom: 14 },
  heroAmount: { fontSize: 40, fontWeight: 'bold', color: '#fff', marginBottom: 18 },
  budgetTrack: { height: 5, backgroundColor: 'rgba(255,255,255,0.08)', borderRadius: 4 },
  budgetFill:  { height: '100%', borderRadius: 4 },
  budgetSubtext: { fontSize: 11, color: Theme.colors.textSecondary },
  pill: {
    flex: 1, borderRadius: 14, borderWidth: 1,
    backgroundColor: 'rgba(255,255,255,0.04)',
    padding: 12, alignItems: 'center',
  },
  pillLabel: { fontSize: 9, fontWeight: '700', color: Theme.colors.textSecondary, letterSpacing: 1, marginBottom: 4 },
  pillValue: { fontSize: 14, fontWeight: 'bold' },
  sectionTitle: {
    fontSize: 17, fontWeight: 'bold', color: Theme.colors.textPrimary,
    paddingHorizontal: 24, marginBottom: 12,
  },
  catCard: {
    borderRadius: 16, borderWidth: 1, padding: 14,
    alignItems: 'center', minWidth: 90,
  },
  txCard: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    backgroundColor: 'rgba(255,255,255,0.04)', borderRadius: 16,
    borderWidth: 1, borderColor: 'rgba(255,255,255,0.06)', padding: 14,
  },
  txIcon:    { width: 44, height: 44, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  txNote:    { fontSize: 15, fontWeight: '600', color: '#fff' },
  txMeta:    { fontSize: 12, color: Theme.colors.textSecondary, marginTop: 2 },
  txAmount:  { fontSize: 15, fontWeight: 'bold', color: Theme.colors.expenseRed },
  // Modal
  modalBg:    { flex: 1, backgroundColor: Theme.colors.bgPrimary, padding: 24, paddingTop: 16 },
  modalHandle:{ width: 40, height: 4, borderRadius: 2, backgroundColor: 'rgba(255,255,255,0.15)', alignSelf: 'center', marginBottom: 24 },
  modalTitle: { fontSize: 22, fontWeight: 'bold', color: '#fff', marginBottom: 24, textAlign: 'center' },
  amountRow:  { flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'center', marginBottom: 24 },
  currencySymbol: { fontSize: 28, color: Theme.colors.accent1, fontWeight: 'bold', marginRight: 4 },
  amountInput:{ fontSize: 52, fontWeight: 'bold', color: '#fff', minWidth: 100, textAlign: 'center' },
  fieldInput: {
    backgroundColor: 'rgba(255,255,255,0.06)', borderRadius: 14, borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)', color: '#fff', fontSize: 15, padding: 16, marginBottom: 14,
  },
  subLabel:   { fontSize: 12, fontWeight: '600', color: Theme.colors.textSecondary, marginBottom: 10 },
  catChip:    {
    flexDirection: 'row', alignItems: 'center', gap: 6,
    paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20,
    backgroundColor: 'rgba(255,255,255,0.06)', borderWidth: 1, borderColor: 'transparent', marginRight: 8,
  },
  addBtn:     { backgroundColor: Theme.colors.accent1, borderRadius: 14, padding: 16, alignItems: 'center', marginBottom: 12 },
  addBtnText: { color: '#fff', fontWeight: 'bold', fontSize: 16 },
  cancelBtn:  { alignItems: 'center', padding: 12 },
  cancelBtnText: { color: Theme.colors.textSecondary, fontSize: 15 },
});
