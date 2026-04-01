import React, { useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, SafeAreaView,
  TouchableOpacity, Modal, TextInput, Alert, Switch,
} from 'react-native';
import { Theme } from '../constants/Theme';
import { useDataStore, AppCategory } from '../store/DataStore';

const COLORS = [
  '#FF6B6B','#FF9F43','#FDCB6E','#1DD1A1',
  '#48DBFB','#A29BFE','#FD79A8','#6C5CE7',
  '#D980FA','#B53471','#EE5A24','#009432'
];

// ── Edit Category Modal ───────────────────────────────────
function EditCategoryModal({ cat, onClose }: { cat: AppCategory; onClose: () => void }) {
  const { updateCategory } = useDataStore();
  const [name, setName]     = useState(cat.name);
  const [emoji, setEmoji]   = useState(cat.emoji);
  const [color, setColor]   = useState(cat.color);

  const handleSave = () => {
    updateCategory({ ...cat, name: name.trim() || cat.name, emoji: emoji || cat.emoji, color });
    onClose();
  };

  return (
    <Modal visible animationType="slide" presentationStyle="formSheet" onRequestClose={onClose}>
      <View style={styles.modalBg}>
        <Text style={styles.modalTitle}>Edit Category</Text>

        <View style={{ flexDirection: 'row', gap: 12, marginBottom: 16 }}>
          <TextInput
            style={[styles.input, { width: 60, textAlign: 'center', fontSize: 28 }]}
            value={emoji} onChangeText={setEmoji} maxLength={2}
          />
          <TextInput
            style={[styles.input, { flex: 1 }]}
            value={name} onChangeText={setName}
            placeholder="Category name" placeholderTextColor={Theme.colors.textTertiary}
          />
        </View>

        <Text style={styles.subLabel}>Color</Text>
        <View style={styles.colorGrid}>
          {COLORS.map(c => (
            <TouchableOpacity key={c} onPress={() => setColor(c)}
              style={[styles.colorDot, { backgroundColor: c }, color === c && styles.colorDotSelected]}
            />
          ))}
        </View>

        <TouchableOpacity style={[styles.saveBtn, !name.trim() && { opacity: 0.4 }]} onPress={handleSave} disabled={!name.trim()}>
          <Text style={styles.saveBtnText}>Save Changes</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={onClose} style={{ alignItems: 'center', marginTop: 12 }}>
          <Text style={{ color: Theme.colors.textSecondary }}>Cancel</Text>
        </TouchableOpacity>
      </View>
    </Modal>
  );
}

// ── Add Category Modal ────────────────────────────────────
function AddCategoryModal({ visible, onClose }: { visible: boolean; onClose: () => void }) {
  const { addCategory } = useDataStore();
  const [name, setName]   = useState('');
  const [emoji, setEmoji] = useState('');
  const [color, setColor] = useState('#A29BFE');

  const handleAdd = () => {
    if (!name.trim()) return;
    addCategory({ id: Date.now().toString(), name: name.trim(), emoji: emoji || '📌', color });
    setName(''); setEmoji(''); setColor('#A29BFE');
    onClose();
  };

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="formSheet" onRequestClose={onClose}>
      <View style={styles.modalBg}>
        <Text style={styles.modalTitle}>Add Category</Text>

        <View style={{ flexDirection: 'row', gap: 12, marginBottom: 16 }}>
          <TextInput
            style={[styles.input, { width: 60, textAlign: 'center', fontSize: 28 }]}
            value={emoji} onChangeText={setEmoji} placeholder="😀"
            placeholderTextColor={Theme.colors.textTertiary} maxLength={2} autoFocus
          />
          <TextInput
            style={[styles.input, { flex: 1 }]}
            value={name} onChangeText={setName}
            placeholder="Category name" placeholderTextColor={Theme.colors.textTertiary}
          />
        </View>

        <Text style={styles.subLabel}>Color</Text>
        <View style={styles.colorGrid}>
          {COLORS.map(c => (
            <TouchableOpacity key={c} onPress={() => setColor(c)}
              style={[styles.colorDot, { backgroundColor: c }, color === c && styles.colorDotSelected]}
            />
          ))}
        </View>

        <TouchableOpacity style={[styles.saveBtn, !name.trim() && { opacity: 0.4 }]} onPress={handleAdd} disabled={!name.trim()}>
          <Text style={styles.saveBtnText}>Add Category</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={onClose} style={{ alignItems: 'center', marginTop: 12 }}>
          <Text style={{ color: Theme.colors.textSecondary }}>Cancel</Text>
        </TouchableOpacity>
      </View>
    </Modal>
  );
}

// ── Settings Screen ───────────────────────────────────────
export default function Settings() {
  const { categories, deleteCategory, userName, setUserName, currency, setCurrency, budget, setBudget, transactions, lendBorrows } = useDataStore();
  const [editCat,    setEditCat]    = useState<AppCategory | null>(null);
  const [showAddCat, setShowAddCat] = useState(false);
  const [nameInput,  setNameInput]  = useState('');
  const [budgetInput,setBudgetInput]= useState('');

  const CURRENCIES = ['₹','$','€','£','¥','د.إ'];

  const totalSpent  = transactions.reduce((s, t) => s + t.amount, 0);
  const totalLent   = lendBorrows.filter(l => l.type === 'lent').reduce((s, l) => s + l.amount, 0);
  const totalBorrow = lendBorrows.filter(l => l.type === 'borrowed').reduce((s, l) => s + l.amount, 0);

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: Theme.colors.bgPrimary }}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        <View style={styles.pageHeader}>
          <Text style={styles.pageTitle}>Settings</Text>
        </View>

        {/* Profile */}
        <Text style={styles.groupLabel}>PROFILE</Text>
        <View style={styles.card}>
          <View style={styles.settingRow}>
            <Text style={styles.settingLabel}>Display Name</Text>
            <TextInput
              style={styles.inlineInput}
              value={nameInput || userName}
              onChangeText={setNameInput}
              onEndEditing={() => { if (nameInput) { setUserName(nameInput); setNameInput(''); } }}
              placeholder={userName}
              placeholderTextColor={Theme.colors.textSecondary}
            />
          </View>
        </View>

        {/* Finance */}
        <Text style={styles.groupLabel}>FINANCE</Text>
        <View style={styles.card}>
          <View style={styles.settingRow}>
            <Text style={styles.settingLabel}>Monthly Budget</Text>
            <TextInput
              style={styles.inlineInput}
              value={budgetInput}
              onChangeText={setBudgetInput}
              onEndEditing={() => { const v = parseFloat(budgetInput); if (v > 0) { setBudget(v); setBudgetInput(''); } }}
              keyboardType="decimal-pad"
              placeholder={budget.toLocaleString('en-IN')}
              placeholderTextColor={Theme.colors.textSecondary}
            />
          </View>
          <View style={styles.divider} />
          <View style={styles.settingRow}>
            <Text style={styles.settingLabel}>Currency</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false}>
              <View style={{ flexDirection: 'row', gap: 8 }}>
                {CURRENCIES.map(c => (
                  <TouchableOpacity key={c} onPress={() => setCurrency(c)}
                    style={[styles.currencyChip, currency === c && { borderColor: Theme.colors.accent1, backgroundColor: Theme.colors.accent1 + '22' }]}>
                    <Text style={{ color: currency === c ? Theme.colors.accent1 : Theme.colors.textSecondary, fontWeight: '600' }}>{c}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            </ScrollView>
          </View>
        </View>

        {/* Categories */}
        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, marginTop: 28, marginBottom: 10 }}>
          <Text style={[styles.groupLabel, { marginTop: 0, marginBottom: 0 }]}>CATEGORIES</Text>
          <TouchableOpacity onPress={() => setShowAddCat(true)}>
            <Text style={{ color: Theme.colors.accent1, fontWeight: '600', fontSize: 14 }}>+ Add New</Text>
          </TouchableOpacity>
        </View>
        <View style={styles.card}>
          {categories.map((cat, i) => (
            <View key={cat.id}>
              <TouchableOpacity style={styles.catRow} onPress={() => setEditCat(cat)}>
                <Text style={{ fontSize: 22 }}>{cat.emoji}</Text>
                <Text style={[styles.catName]}>{cat.name}</Text>
                <View style={{ flex: 1 }} />
                <View style={[styles.colorIndicator, { backgroundColor: cat.color }]} />
                <Text style={{ color: Theme.colors.textSecondary, marginLeft: 8 }}>›</Text>
              </TouchableOpacity>
              {i < categories.length - 1 && <View style={styles.divider} />}
            </View>
          ))}
        </View>

        {/* Stats */}
        <Text style={styles.groupLabel}>STATISTICS</Text>
        <View style={styles.card}>
          {[
            { label: 'Total Transactions', value: transactions.length.toString() },
            { label: 'Total Spent',        value: `${currency}${totalSpent.toLocaleString('en-IN', { maximumFractionDigits: 0 })}` },
            { label: 'Money Lent',         value: `${currency}${totalLent.toLocaleString('en-IN', { maximumFractionDigits: 0 })}` },
            { label: 'Money Borrowed',     value: `${currency}${totalBorrow.toLocaleString('en-IN', { maximumFractionDigits: 0 })}` },
          ].map((r, i, arr) => (
            <View key={r.label}>
              <View style={styles.settingRow}>
                <Text style={styles.settingLabel}>{r.label}</Text>
                <Text style={{ color: Theme.colors.textSecondary, fontSize: 14 }}>{r.value}</Text>
              </View>
              {i < arr.length - 1 && <View style={styles.divider} />}
            </View>
          ))}
        </View>

        {/* About */}
        <Text style={styles.groupLabel}>ABOUT</Text>
        <View style={styles.card}>
          <View style={styles.settingRow}>
            <Text style={styles.settingLabel}>Version</Text>
            <Text style={{ color: Theme.colors.textSecondary }}>1.0.0</Text>
          </View>
          <View style={styles.divider} />
          <View style={styles.settingRow}>
            <Text style={styles.settingLabel}>Built with</Text>
            <Text style={{ color: Theme.colors.textSecondary }}>React Native + Expo</Text>
          </View>
        </View>
      </ScrollView>

      {editCat && <EditCategoryModal cat={editCat} onClose={() => setEditCat(null)} />}
      <AddCategoryModal visible={showAddCat} onClose={() => setShowAddCat(false)} />
    </SafeAreaView>
  );
}

// ── Styles ─────────────────────────────────────────────────
const styles = StyleSheet.create({
  pageHeader: { paddingHorizontal: 24, paddingTop: 20, paddingBottom: 16 },
  pageTitle:  { fontSize: 28, fontWeight: 'bold', color: '#fff' },
  groupLabel: { fontSize: 11, fontWeight: '700', color: Theme.colors.textSecondary, letterSpacing: 1.2, paddingHorizontal: 24, marginTop: 28, marginBottom: 10 },
  card:       { backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: 18, borderWidth: 1, borderColor: 'rgba(255,255,255,0.07)', marginHorizontal: 20, overflow: 'hidden' },
  settingRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16, minHeight: 52 },
  settingLabel:{ fontSize: 15, color: '#fff', fontWeight: '500', flex: 1 },
  inlineInput:{ color: Theme.colors.textSecondary, fontSize: 14, textAlign: 'right', flex: 1 },
  divider:    { height: 1, backgroundColor: 'rgba(255,255,255,0.06)', marginHorizontal: 16 },
  catRow:     { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 14 },
  catName:    { fontSize: 15, color: '#fff', fontWeight: '500' },
  colorIndicator: { width: 12, height: 12, borderRadius: 6 },
  currencyChip: { paddingHorizontal: 14, paddingVertical: 7, borderRadius: 10, borderWidth: 1, borderColor: 'rgba(255,255,255,0.15)' },
  // Modals
  modalBg:   { flex: 1, backgroundColor: Theme.colors.bgPrimary, padding: 24, paddingTop: 32 },
  modalTitle:{ fontSize: 22, fontWeight: 'bold', color: '#fff', textAlign: 'center', marginBottom: 28 },
  subLabel:  { fontSize: 12, fontWeight: '600', color: Theme.colors.textSecondary, marginBottom: 10 },
  input:     { backgroundColor: 'rgba(255,255,255,0.07)', borderRadius: 14, borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)', color: '#fff', fontSize: 15, padding: 14 },
  colorGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12, marginBottom: 28 },
  colorDot:  { width: 34, height: 34, borderRadius: 17 },
  colorDotSelected: { borderWidth: 3, borderColor: '#fff' },
  saveBtn:   { backgroundColor: Theme.colors.accent1, borderRadius: 14, padding: 16, alignItems: 'center' },
  saveBtnText:{ color: '#fff', fontWeight: 'bold', fontSize: 16 },
});
