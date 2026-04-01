import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';

// ── Models ───────────────────────────────────────────────

export interface AppCategory {
  id: string;
  name: string;
  emoji: string;
  color: string; // hex string e.g. "#FF6B6B"
}

export interface Transaction {
  id: string;
  amount: number;
  recipientName: string;
  upiId?: string;
  note: string;
  categoryId: string;
  categoryName: string;
  categoryEmoji: string;
  categoryColor: string;
  date: string;
  upiAppUsed?: string;
}

export type LendBorrowType = 'lent' | 'borrowed';

export interface LendBorrow {
  id: string;
  type: LendBorrowType;
  personName: string;
  contactPhone?: string;
  amount: number;
  note: string;
  date: string;
  dueDate?: string;
  isPaid: boolean;
  paidAmount: number;
}

// ── Storage Keys ─────────────────────────────────────────
const KEYS = {
  transactions: 'mm_transactions_v4',
  lendBorrows:  'mm_lendborrow_v4',
  budget:       'mm_budget',
  userName:     'mm_username',
  currency:     'mm_currency',
  categories:   'mm_usercategories_v1',
  savedUPIs:    'mm_saved_upis_v1',
  savedContacts:'mm_savedContacts_v1',
  savedAvatars: 'savedAvatars_v4',
};

const DEFAULT_CATEGORIES: AppCategory[] = [
  { id: '1', name: 'Food',          emoji: '🍔', color: '#FF9F0A' },
  { id: '2', name: 'Transport',     emoji: '🚌', color: '#5AC8FA' },
  { id: '3', name: 'Shopping',      emoji: '🛍️', color: '#7B61FF' },
  { id: '4', name: 'Bills',         emoji: '🧾', color: '#FF375F' },
  { id: '5', name: 'Subscriptions', emoji: '📱', color: '#30D158' },
  { id: '6', name: 'Rent',          emoji: '🏠', color: '#FF453A' },
  { id: '7', name: 'Other',         emoji: '📦', color: '#FFD60A' },
];

// ── Context Type ─────────────────────────────────────────
interface DataStoreContextType {
  transactions:   Transaction[];
  lendBorrows:    LendBorrow[];
  categories:     AppCategory[];
  savedUPIs:      Record<string, string>;
  savedContacts:  Record<string, string | null>;
  savedAvatars:   Record<string, string>;   // name → base64 uri
  budget:         number;
  userName:       string;
  currency:       string;

  // Transactions
  addTransaction:    (t: Transaction) => void;
  deleteTransaction: (id: string) => void;

  // LendBorrow
  addLendBorrow:     (lb: LendBorrow) => void;
  updateLendBorrow:  (lb: LendBorrow) => void;
  deleteLendBorrow:  (id: string) => void;
  markPaid:          (id: string) => void;
  addPartialPayment: (id: string, paidNow: number) => void;

  // Categories
  addCategory:    (c: AppCategory) => void;
  updateCategory: (c: AppCategory) => void;
  deleteCategory: (id: string) => void;

  // Contacts / UPI
  saveUPI:            (name: string, upi: string) => void;
  addPaymentContact:  (name: string, phone: string | null) => void;
  removePaymentContact:(name: string) => void;
  saveAvatar:         (name: string, uri: string) => void;

  // Settings
  setBudget:   (v: number) => void;
  setUserName: (v: string) => void;
  setCurrency: (v: string) => void;

  // Helpers
  formatted: (v: number) => string;
}

const DataStoreContext = createContext<DataStoreContextType | null>(null);

// ── Provider ─────────────────────────────────────────────
export const DataStoreProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [transactions,   setTransactions]   = useState<Transaction[]>([]);
  const [lendBorrows,    setLendBorrows]    = useState<LendBorrow[]>([]);
  const [categories,     setCategories]     = useState<AppCategory[]>(DEFAULT_CATEGORIES);
  const [savedUPIs,      setSavedUPIs]      = useState<Record<string, string>>({});
  const [savedContacts,  setSavedContacts]  = useState<Record<string, string | null>>({});
  const [savedAvatars,   setSavedAvatars]   = useState<Record<string, string>>({});
  const [budget,         setBudgetState]    = useState<number>(30000);
  const [userName,       setUserNameState]  = useState<string>('User');
  const [currency,       setCurrencyState]  = useState<string>('₹');

  // Load
  useEffect(() => {
    const load = async () => {
      try {
        const vals = await AsyncStorage.multiGet(Object.values(KEYS));
        const map = Object.fromEntries(vals.map(([k, v]) => [k, v]));

        const txRaw  = map[KEYS.transactions];
        const lbRaw  = map[KEYS.lendBorrows];
        const catRaw = map[KEYS.categories];
        const upiRaw = map[KEYS.savedUPIs];
        const conRaw = map[KEYS.savedContacts];
        const avRaw  = map[KEYS.savedAvatars];

        if (txRaw)  setTransactions(JSON.parse(txRaw));
        if (lbRaw)  setLendBorrows(JSON.parse(lbRaw));
        if (catRaw) {
          const parsed = JSON.parse(catRaw);
          if (parsed?.length) setCategories(parsed);
        }
        if (upiRaw) setSavedUPIs(JSON.parse(upiRaw));
        if (conRaw) setSavedContacts(JSON.parse(conRaw));
        if (avRaw)  setSavedAvatars(JSON.parse(avRaw));

        const b = map[KEYS.budget];
        const u = map[KEYS.userName];
        const c = map[KEYS.currency];
        if (b) setBudgetState(parseFloat(b));
        if (u) setUserNameState(u);
        if (c) setCurrencyState(c);
      } catch (e) {
        console.error('DataStore load error:', e);
      }
    };
    load();
  }, []);

  // ── Helpers ─────────────────────────────────────────
  const persist = useCallback((key: string, value: unknown) => {
    AsyncStorage.setItem(key, JSON.stringify(value));
  }, []);

  const formatted = useCallback((v: number) => {
    return `${currency}${v.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
  }, [currency]);

  // ── Transactions ─────────────────────────────────────
  const addTransaction = useCallback((t: Transaction) => {
    setTransactions(prev => { const n = [...prev, t]; persist(KEYS.transactions, n); return n; });
  }, [persist]);

  const deleteTransaction = useCallback((id: string) => {
    setTransactions(prev => { const n = prev.filter(t => t.id !== id); persist(KEYS.transactions, n); return n; });
  }, [persist]);

  // ── LendBorrow ───────────────────────────────────────
  const addLendBorrow = useCallback((lb: LendBorrow) => {
    setLendBorrows(prev => { const n = [...prev, lb]; persist(KEYS.lendBorrows, n); return n; });
  }, [persist]);

  const updateLendBorrow = useCallback((lb: LendBorrow) => {
    setLendBorrows(prev => { const n = prev.map(x => x.id === lb.id ? lb : x); persist(KEYS.lendBorrows, n); return n; });
  }, [persist]);

  const deleteLendBorrow = useCallback((id: string) => {
    setLendBorrows(prev => { const n = prev.filter(lb => lb.id !== id); persist(KEYS.lendBorrows, n); return n; });
  }, [persist]);

  const markPaid = useCallback((id: string) => {
    setLendBorrows(prev => {
      const n = prev.map(lb => lb.id === id ? { ...lb, isPaid: true, paidAmount: lb.amount } : lb);
      persist(KEYS.lendBorrows, n);
      return n;
    });
  }, [persist]);

  const addPartialPayment = useCallback((id: string, paidNow: number) => {
    setLendBorrows(prev => {
      const n = prev.map(lb => {
        if (lb.id === id) {
          const newPaid = Math.min(lb.amount, lb.paidAmount + paidNow);
          return { ...lb, paidAmount: newPaid, isPaid: newPaid >= lb.amount };
        }
        return lb;
      });
      persist(KEYS.lendBorrows, n);
      return n;
    });
  }, [persist]);

  // ── Categories ───────────────────────────────────────
  const addCategory = useCallback((c: AppCategory) => {
    setCategories(prev => { const n = [...prev, c]; persist(KEYS.categories, n); return n; });
  }, [persist]);

  const updateCategory = useCallback((c: AppCategory) => {
    setCategories(prev => { const n = prev.map(x => x.id === c.id ? c : x); persist(KEYS.categories, n); return n; });
  }, [persist]);

  const deleteCategory = useCallback((id: string) => {
    setCategories(prev => { const n = prev.filter(c => c.id !== id); persist(KEYS.categories, n); return n; });
  }, [persist]);

  // ── Contacts / UPI ───────────────────────────────────
  const saveUPI = useCallback((name: string, upi: string) => {
    setSavedUPIs(prev => { const n = { ...prev, [name]: upi }; persist(KEYS.savedUPIs, n); return n; });
  }, [persist]);

  const addPaymentContact = useCallback((name: string, phone: string | null) => {
    if (!name.trim()) return;
    setSavedContacts(prev => { const n = { ...prev, [name]: phone }; persist(KEYS.savedContacts, n); return n; });
  }, [persist]);

  const removePaymentContact = useCallback((name: string) => {
    setSavedContacts(prev => { const n = { ...prev }; delete n[name]; persist(KEYS.savedContacts, n); return n; });
  }, [persist]);

  const saveAvatar = useCallback((name: string, uri: string) => {
    setSavedAvatars(prev => { const n = { ...prev, [name]: uri }; persist(KEYS.savedAvatars, n); return n; });
  }, [persist]);

  // ── Settings ─────────────────────────────────────────
  const setBudget = useCallback((v: number) => {
    setBudgetState(v);
    AsyncStorage.setItem(KEYS.budget, String(v));
  }, []);

  const setUserName = useCallback((v: string) => {
    setUserNameState(v);
    AsyncStorage.setItem(KEYS.userName, v);
  }, []);

  const setCurrency = useCallback((v: string) => {
    setCurrencyState(v);
    AsyncStorage.setItem(KEYS.currency, v);
  }, []);

  return (
    <DataStoreContext.Provider value={{
      transactions, lendBorrows, categories, savedUPIs, savedContacts, savedAvatars,
      budget, userName, currency,
      addTransaction, deleteTransaction,
      addLendBorrow, updateLendBorrow, deleteLendBorrow, markPaid, addPartialPayment,
      addCategory, updateCategory, deleteCategory,
      saveUPI, addPaymentContact, removePaymentContact, saveAvatar,
      setBudget, setUserName, setCurrency,
      formatted,
    }}>
      {children}
    </DataStoreContext.Provider>
  );
};

// ── Hook ─────────────────────────────────────────────────
export const useDataStore = () => {
  const ctx = useContext(DataStoreContext);
  if (!ctx) throw new Error('useDataStore must be used within DataStoreProvider');
  return ctx;
};
