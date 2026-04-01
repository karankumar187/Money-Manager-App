import React, { useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, SafeAreaView,
  TouchableOpacity, Modal, TextInput, Alert, Pressable, FlatList,
} from 'react-native';
import { Theme } from '../constants/Theme';
import { useDataStore, LendBorrow, LendBorrowType } from '../store/DataStore';

// ── Person card ───────────────────────────────────────────
function avatarColor(name: string): string {
  const palette = ['#BF5AF2','#64D2FF','#FF9F0A','#32D74B','#FF375F','#FFD60A'];
  return palette[Math.abs(name.split('').reduce((a, c) => a + c.charCodeAt(0), 0)) % palette.length];
}

interface PersonGroup {
  name: string;
  phone?: string;
  entries: LendBorrow[];
  totalLent: number;
  totalBorrowed: number;
}

function buildGroups(lendBorrows: LendBorrow[], savedContacts: Record<string, string | null>): PersonGroup[] {
  const dict: Record<string, LendBorrow[]> = {};
  for (const lb of lendBorrows) {
    if (!dict[lb.personName]) dict[lb.personName] = [];
    dict[lb.personName].push(lb);
  }
  // Add contacts with no lend entries
  for (const name of Object.keys(savedContacts)) {
    if (!dict[name]) dict[name] = [];
  }
  return Object.entries(dict).map(([name, entries]) => {
    const phone = entries.find(e => e.contactPhone)?.contactPhone || savedContacts[name] || undefined;
    const activeLent     = entries.filter(e => e.type === 'lent'     && !e.isPaid);
    const activeBorrowed = entries.filter(e => e.type === 'borrowed' && !e.isPaid);
    return {
      name, phone, entries: entries.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()),
      totalLent:     activeLent.reduce((s, e)     => s + (e.amount - e.paidAmount), 0),
      totalBorrowed: activeBorrowed.reduce((s, e) => s + (e.amount - e.paidAmount), 0),
    };
  }).sort((a, b) => a.name.localeCompare(b.name));
}

function PersonCard({ group, onPress }: { group: PersonGroup; onPress: () => void }) {
  const color = avatarColor(group.name);
  const net   = group.totalLent - group.totalBorrowed;
  const { formatted } = useDataStore();
  return (
    <TouchableOpacity style={styles.personCard} onPress={onPress}>
      <View style={[styles.avatar, { backgroundColor: color + '28' }]}>
        <Text style={[styles.avatarText, { color }]}>{group.name.charAt(0).toUpperCase()}</Text>
      </View>
      <View style={{ flex: 1 }}>
        <Text style={styles.personName}>{group.name}</Text>
        {group.phone && <Text style={styles.personPhone}>{group.phone}</Text>}
        <View style={{ flexDirection: 'row', gap: 6, marginTop: 4 }}>
          {group.totalLent > 0 && (
            <View style={[styles.miniPill, { backgroundColor: Theme.colors.incomeGreen + '22', borderColor: Theme.colors.incomeGreen + '44' }]}>
              <Text style={{ color: Theme.colors.incomeGreen, fontSize: 11, fontWeight: '600' }}>Lent {formatted(group.totalLent)}</Text>
            </View>
          )}
          {group.totalBorrowed > 0 && (
            <View style={[styles.miniPill, { backgroundColor: Theme.colors.expenseRed + '22', borderColor: Theme.colors.expenseRed + '44' }]}>
              <Text style={{ color: Theme.colors.expenseRed, fontSize: 11, fontWeight: '600' }}>Owe {formatted(group.totalBorrowed)}</Text>
            </View>
          )}
          {group.totalLent === 0 && group.totalBorrowed === 0 && (
            <View style={[styles.miniPill, { backgroundColor: 'rgba(255,255,255,0.06)', borderColor: 'rgba(255,255,255,0.12)' }]}>
              <Text style={{ color: Theme.colors.textSecondary, fontSize: 11 }}>
                {group.entries.length === 0 ? 'No active lends' : 'Settled ✓'}
              </Text>
            </View>
          )}
        </View>
      </View>
      {net !== 0 && (
        <View style={{ alignItems: 'flex-end' }}>
          <Text style={{ color: net > 0 ? Theme.colors.incomeGreen : Theme.colors.expenseRed, fontWeight: 'bold', fontSize: 14 }}>
            {net > 0 ? '+' : ''}{formatted(Math.abs(net))}
          </Text>
          <Text style={{ color: Theme.colors.textSecondary, fontSize: 10 }}>
            {net > 0 ? 'they owe' : 'you owe'}
          </Text>
        </View>
      )}
    </TouchableOpacity>
  );
}

// ── Detail Modal ──────────────────────────────────────────
function PersonDetailModal({ group, onClose }: { group: PersonGroup; onClose: () => void }) {
  const { formatted, markPaid, deleteLendBorrow, addLendBorrow, transactions, removePaymentContact } = useDataStore();
  const [tab, setTab]       = useState<'lends' | 'transfers'>('lends');
  const [showAdd, setShowAdd] = useState(false);
  const [addType, setAddType] = useState<LendBorrowType>('lent');
  const [amount, setAmount]  = useState('');
  const [note, setNote]      = useState('');

  const transfers = transactions
    .filter(t => t.recipientName === group.name)
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

  const lentEntries     = group.entries.filter(e => e.type === 'lent');
  const borrowedEntries = group.entries.filter(e => e.type === 'borrowed');
  const color           = avatarColor(group.name);
  const net             = group.totalLent - group.totalBorrowed;

  const handleAdd = () => {
    const a = parseFloat(amount);
    if (!a) return;
    addLendBorrow({ id: Date.now().toString(), type: addType, personName: group.name, contactPhone: group.phone,
      amount: a, note: note.trim() || (addType === 'lent' ? 'Lend' : 'Borrow'), date: new Date().toISOString(), isPaid: false, paidAmount: 0 });
    setAmount(''); setNote(''); setShowAdd(false);
  };

  const renderEntry = (e: LendBorrow, accent: string) => (
    <View key={e.id} style={[styles.entryCard, { borderLeftColor: accent }]}>
      <View style={{ flex: 1 }}>
        <Text style={{ color: '#fff', fontWeight: '600', fontSize: 14 }}>{formatted(e.amount)}</Text>
        <Text style={{ color: Theme.colors.textSecondary, fontSize: 12 }}>{e.note}</Text>
        {e.paidAmount > 0 && !e.isPaid && (
          <Text style={{ color: Theme.colors.accentBlue, fontSize: 11 }}>Paid: {formatted(e.paidAmount)}</Text>
        )}
      </View>
      <View style={{ gap: 6 }}>
        {!e.isPaid && (
          <TouchableOpacity onPress={() => Alert.alert('Mark Paid?', '', [
            { text: 'Cancel', style: 'cancel' },
            { text: 'Mark Paid', onPress: () => markPaid(e.id) },
          ])} style={[styles.entryBtn, { backgroundColor: accent + '22', borderColor: accent + '55' }]}>
            <Text style={{ color: accent, fontSize: 11, fontWeight: '600' }}>Paid ✓</Text>
          </TouchableOpacity>
        )}
        {e.isPaid && <Text style={{ color: Theme.colors.incomeGreen, fontSize: 11 }}>✓ Done</Text>}
        <TouchableOpacity onPress={() => deleteLendBorrow(e.id)} style={[styles.entryBtn, { backgroundColor: Theme.colors.expenseRed + '22', borderColor: Theme.colors.expenseRed + '44' }]}>
          <Text style={{ color: Theme.colors.expenseRed, fontSize: 11 }}>Delete</Text>
        </TouchableOpacity>
      </View>
    </View>
  );

  return (
    <Modal visible animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <SafeAreaView style={{ flex: 1, backgroundColor: Theme.colors.bgPrimary }}>
        <View style={styles.detailHeader}>
          <View style={[styles.avatarLg, { backgroundColor: color + '28' }]}>
            <Text style={[styles.avatarTextLg, { color }]}>{group.name.charAt(0).toUpperCase()}</Text>
          </View>
          <View style={{ flex: 1, marginLeft: 14 }}>
            <Text style={{ color: '#fff', fontWeight: 'bold', fontSize: 18 }}>{group.name}</Text>
            {group.phone && <Text style={{ color: Theme.colors.textSecondary, fontSize: 13 }}>{group.phone}</Text>}
            {net !== 0 && (
              <Text style={{ color: net > 0 ? Theme.colors.incomeGreen : Theme.colors.expenseRed, fontSize: 13, fontWeight: '600' }}>
                {net > 0 ? `${group.name.split(' ')[0]} owes you ${formatted(net)}` : `You owe ${formatted(Math.abs(net))}`}
              </Text>
            )}
          </View>
          <TouchableOpacity onPress={onClose}>
            <Text style={{ color: Theme.colors.textSecondary, fontSize: 28 }}>×</Text>
          </TouchableOpacity>
        </View>

        {/* Segment */}
        <View style={styles.segmentRow}>
          <TouchableOpacity style={[styles.segBtn, tab === 'lends' && styles.segBtnActive]} onPress={() => setTab('lends')}>
            <Text style={[styles.segBtnText, tab === 'lends' && { color: '#fff' }]}>Lends</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.segBtn, tab === 'transfers' && styles.segBtnActive]} onPress={() => setTab('transfers')}>
            <Text style={[styles.segBtnText, tab === 'transfers' && { color: '#fff' }]}>Transfers</Text>
          </TouchableOpacity>
        </View>

        <ScrollView contentContainerStyle={{ padding: 20, paddingBottom: 40 }}>
          {tab === 'lends' ? (
            <>
              <TouchableOpacity onPress={() => setShowAdd(true)} style={styles.addEntryBtn}>
                <Text style={{ color: Theme.colors.accent1, fontWeight: '600' }}>+ Add Lend / Borrow</Text>
              </TouchableOpacity>
              {lentEntries.length > 0 && (
                <View style={{ marginBottom: 16 }}>
                  <Text style={styles.subHead}>LENT</Text>
                  {lentEntries.map(e => renderEntry(e, Theme.colors.incomeGreen))}
                </View>
              )}
              {borrowedEntries.length > 0 && (
                <View>
                  <Text style={styles.subHead}>BORROWED</Text>
                  {borrowedEntries.map(e => renderEntry(e, Theme.colors.expenseRed))}
                </View>
              )}
              {group.entries.length === 0 && (
                <Text style={{ color: Theme.colors.textSecondary, textAlign: 'center', marginTop: 40 }}>No lend records yet.</Text>
              )}
            </>
          ) : (
            <>
              {transfers.length === 0 ? (
                <Text style={{ color: Theme.colors.textSecondary, textAlign: 'center', marginTop: 40 }}>No transfer history.</Text>
              ) : transfers.map(t => (
                <View key={t.id} style={styles.txRow}>
                  <Text style={{ fontSize: 20 }}>{t.categoryEmoji}</Text>
                  <View style={{ flex: 1, marginLeft: 12 }}>
                    <Text style={{ color: '#fff', fontWeight: '600', fontSize: 14 }}>{t.note}</Text>
                    <Text style={{ color: Theme.colors.textSecondary, fontSize: 12 }}>{t.categoryName}</Text>
                  </View>
                  <Text style={{ color: Theme.colors.expenseRed, fontWeight: 'bold' }}>-{formatted(t.amount)}</Text>
                </View>
              ))}
            </>
          )}
        </ScrollView>

        {/* Add lend/borrow modal */}
        <Modal visible={showAdd} animationType="slide" presentationStyle="formSheet" onRequestClose={() => setShowAdd(false)}>
          <View style={{ flex: 1, backgroundColor: Theme.colors.bgPrimary, padding: 24 }}>
            <Text style={styles.modalTitle}>Add Record</Text>
            <View style={styles.segmentRow}>
              <TouchableOpacity style={[styles.segBtn, addType === 'lent' && styles.segBtnActive]} onPress={() => setAddType('lent')}>
                <Text style={[styles.segBtnText, addType === 'lent' && { color: '#fff' }]}>Lent</Text>
              </TouchableOpacity>
              <TouchableOpacity style={[styles.segBtn, addType === 'borrowed' && styles.segBtnActive]} onPress={() => setAddType('borrowed')}>
                <Text style={[styles.segBtnText, addType === 'borrowed' && { color: '#fff' }]}>Borrowed</Text>
              </TouchableOpacity>
            </View>
            <TextInput style={styles.fieldInput} value={amount} onChangeText={setAmount} keyboardType="decimal-pad" placeholder="Amount" placeholderTextColor={Theme.colors.textTertiary} autoFocus />
            <TextInput style={styles.fieldInput} value={note} onChangeText={setNote} placeholder="Note (optional)" placeholderTextColor={Theme.colors.textTertiary} />
            <TouchableOpacity style={[styles.addBtn, !amount && { opacity: 0.4 }]} onPress={handleAdd} disabled={!amount}>
              <Text style={styles.addBtnText}>Save</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={() => setShowAdd(false)} style={{ alignItems: 'center', marginTop: 12 }}>
              <Text style={{ color: Theme.colors.textSecondary }}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </Modal>
      </SafeAreaView>
    </Modal>
  );
}

// ── Add Contact Modal ─────────────────────────────────────
function AddContactModal({ visible, onClose }: { visible: boolean; onClose: () => void }) {
  const { addPaymentContact } = useDataStore();
  const [name, setName]   = useState('');
  const [phone, setPhone] = useState('');

  const handleSave = () => {
    if (!name.trim()) return;
    addPaymentContact(name.trim(), phone.trim() || null);
    setName(''); setPhone(''); onClose();
  };

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="formSheet" onRequestClose={onClose}>
      <View style={{ flex: 1, backgroundColor: Theme.colors.bgPrimary, padding: 24 }}>
        <Text style={styles.modalTitle}>Add Contact</Text>
        <TextInput style={styles.fieldInput} value={name} onChangeText={setName} placeholder="Name" placeholderTextColor={Theme.colors.textTertiary} autoFocus />
        <TextInput style={styles.fieldInput} value={phone} onChangeText={setPhone} placeholder="Phone (optional)" placeholderTextColor={Theme.colors.textTertiary} keyboardType="phone-pad" />
        <TouchableOpacity style={[styles.addBtn, !name.trim() && { opacity: 0.4 }]} onPress={handleSave} disabled={!name.trim()}>
          <Text style={styles.addBtnText}>Add Contact</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={onClose} style={{ alignItems: 'center', marginTop: 12 }}>
          <Text style={{ color: Theme.colors.textSecondary }}>Cancel</Text>
        </TouchableOpacity>
      </View>
    </Modal>
  );
}

// ── Payments Screen ───────────────────────────────────────
export default function Payments() {
  const { lendBorrows, savedContacts, formatted } = useDataStore();
  const [selectedPerson, setSelectedPerson] = useState<PersonGroup | null>(null);
  const [showAddContact, setShowAddContact] = useState(false);

  const groups       = buildGroups(lendBorrows, savedContacts);
  const activeGroups = groups.filter(g => g.totalLent > 0 || g.totalBorrowed > 0);
  const otherGroups  = groups.filter(g => g.totalLent === 0 && g.totalBorrowed === 0);

  const totalLent     = activeGroups.reduce((s, g) => s + g.totalLent, 0);
  const totalBorrowed = activeGroups.reduce((s, g) => s + g.totalBorrowed, 0);

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: Theme.colors.bgPrimary }}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.pageTitle}>Payments</Text>
        <TouchableOpacity onPress={() => setShowAddContact(true)} style={styles.addContactBtn}>
          <Text style={{ color: '#fff', fontSize: 20, fontWeight: 'bold' }}>+</Text>
        </TouchableOpacity>
      </View>

      {/* Summary */}
      <View style={{ flexDirection: 'row', paddingHorizontal: 20, marginBottom: 20, gap: 12 }}>
        <View style={[styles.summPill, { borderColor: Theme.colors.incomeGreen + '44' }]}>
          <Text style={styles.summLabel}>LENT OUT</Text>
          <Text style={[styles.summValue, { color: Theme.colors.incomeGreen }]}>{formatted(totalLent)}</Text>
        </View>
        <View style={[styles.summPill, { borderColor: Theme.colors.expenseRed + '44' }]}>
          <Text style={styles.summLabel}>BORROWED</Text>
          <Text style={[styles.summValue, { color: Theme.colors.expenseRed }]}>{formatted(totalBorrowed)}</Text>
        </View>
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        {activeGroups.length > 0 && (
          <>
            <Text style={styles.sectionLabel}>OUTSTANDING</Text>
            {activeGroups.map(g => (
              <View key={g.name} style={{ paddingHorizontal: 20, marginBottom: 10 }}>
                <PersonCard group={g} onPress={() => setSelectedPerson(g)} />
              </View>
            ))}
          </>
        )}

        {otherGroups.length > 0 && (
          <>
            <Text style={styles.sectionLabel}>OTHER CONTACTS</Text>
            {otherGroups.map(g => (
              <View key={g.name} style={{ paddingHorizontal: 20, marginBottom: 10 }}>
                <PersonCard group={g} onPress={() => setSelectedPerson(g)} />
              </View>
            ))}
          </>
        )}

        {groups.length === 0 && (
          <View style={{ alignItems: 'center', paddingTop: 80, opacity: 0.5 }}>
            <Text style={{ fontSize: 48 }}>🤝</Text>
            <Text style={{ color: Theme.colors.textSecondary, marginTop: 12, fontSize: 15 }}>No contacts yet</Text>
            <Text style={{ color: Theme.colors.textTertiary, fontSize: 13, marginTop: 4 }}>Tap + to add a contact</Text>
          </View>
        )}
      </ScrollView>

      {selectedPerson && (
        <PersonDetailModal group={selectedPerson} onClose={() => setSelectedPerson(null)} />
      )}
      <AddContactModal visible={showAddContact} onClose={() => setShowAddContact(false)} />
    </SafeAreaView>
  );
}

// ── Styles ─────────────────────────────────────────────────
const styles = StyleSheet.create({
  header:       { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 20, paddingBottom: 20 },
  pageTitle:    { fontSize: 28, fontWeight: 'bold', color: '#fff' },
  addContactBtn:{ width: 40, height: 40, borderRadius: 20, backgroundColor: Theme.colors.accent1, alignItems: 'center', justifyContent: 'center' },
  summPill:     { flex: 1, borderRadius: 14, borderWidth: 1, backgroundColor: 'rgba(255,255,255,0.04)', padding: 14 },
  summLabel:    { fontSize: 9, fontWeight: '700', color: Theme.colors.textSecondary, letterSpacing: 1, marginBottom: 4 },
  summValue:    { fontSize: 18, fontWeight: 'bold' },
  sectionLabel: { fontSize: 10, fontWeight: '700', color: Theme.colors.textSecondary, letterSpacing: 1.4, paddingHorizontal: 24, marginBottom: 10, marginTop: 4 },
  personCard:   { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: 'rgba(255,255,255,0.04)', borderRadius: 18, borderWidth: 1, borderColor: 'rgba(255,255,255,0.06)', padding: 14 },
  avatar:       { width: 46, height: 46, borderRadius: 23, alignItems: 'center', justifyContent: 'center' },
  avatarText:   { fontSize: 18, fontWeight: 'bold' },
  personName:   { fontSize: 15, fontWeight: '600', color: '#fff' },
  personPhone:  { fontSize: 12, color: Theme.colors.textSecondary },
  miniPill:     { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 8, borderWidth: 1 },
  // Detail modal
  detailHeader: { flexDirection: 'row', alignItems: 'center', padding: 20 },
  avatarLg:     { width: 56, height: 56, borderRadius: 28, alignItems: 'center', justifyContent: 'center' },
  avatarTextLg: { fontSize: 22, fontWeight: 'bold' },
  segmentRow:   { flexDirection: 'row', backgroundColor: 'rgba(255,255,255,0.06)', borderRadius: 12, padding: 3, marginHorizontal: 20, marginBottom: 16 },
  segBtn:       { flex: 1, paddingVertical: 8, borderRadius: 10, alignItems: 'center' },
  segBtnActive: { backgroundColor: Theme.colors.accent1 },
  segBtnText:   { color: Theme.colors.textSecondary, fontWeight: '600', fontSize: 14 },
  subHead:      { fontSize: 10, fontWeight: '700', color: Theme.colors.textSecondary, letterSpacing: 1.2, marginBottom: 8 },
  entryCard:    { flexDirection: 'row', alignItems: 'center', backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: 14, borderWidth: 1, borderColor: 'rgba(255,255,255,0.08)', borderLeftWidth: 3, padding: 14, marginBottom: 10 },
  entryBtn:     { paddingHorizontal: 10, paddingVertical: 5, borderRadius: 8, borderWidth: 1, alignItems: 'center' },
  txRow:        { flexDirection: 'row', alignItems: 'center', backgroundColor: 'rgba(255,255,255,0.04)', borderRadius: 14, borderWidth: 1, borderColor: 'rgba(255,255,255,0.06)', padding: 14, marginBottom: 10 },
  addEntryBtn:  { borderWidth: 1, borderColor: Theme.colors.accent1 + '44', borderRadius: 14, padding: 14, alignItems: 'center', marginBottom: 16, backgroundColor: Theme.colors.accent1 + '11' },
  modalTitle:   { fontSize: 20, fontWeight: 'bold', color: '#fff', textAlign: 'center', marginBottom: 24 },
  fieldInput:   { backgroundColor: 'rgba(255,255,255,0.06)', borderRadius: 14, borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)', color: '#fff', fontSize: 15, padding: 16, marginBottom: 14 },
  addBtn:       { backgroundColor: Theme.colors.accent1, borderRadius: 14, padding: 16, alignItems: 'center' },
  addBtnText:   { color: '#fff', fontWeight: 'bold', fontSize: 16 },
});
