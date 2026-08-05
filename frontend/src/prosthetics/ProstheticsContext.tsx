import {
  createContext,
  useContext,
  useState,
  useEffect,
  type ReactNode,
  useCallback,
} from 'react';
import type {
  ProstheticsPatient,
  ProstheticsOrder,
  FlowTemplate,
  ProstheticsDraft,
} from './types';
import {
  prostheticsPatientApi,
  prostheticsOrderApi,
  flowTemplateApi,
} from '../api/prosthetics';

interface ProstheticsContextType {
  draft: ProstheticsDraft;
  setDraftField: (field: keyof ProstheticsDraft, value: string | null) => void;
  resetDraft: () => void;
  patient: ProstheticsPatient | null;
  orders: ProstheticsOrder[];
  templates: FlowTemplate[];
  loadingOrders: boolean;
  loadingTemplates: boolean;
  loadOrders: () => Promise<void>;
  loadTemplates: () => Promise<void>;
}

const STORAGE_KEY = 'prosthetics:draft';

const ProstheticsContext = createContext<ProstheticsContextType | null>(null);

const initialDraft: ProstheticsDraft = {
  patientId: null,
  orderId: null,
  templateId: null,
  instanceId: null,
};

export function ProstheticsProvider({ children }: { children: ReactNode }) {
  const [draft, setDraft] = useState<ProstheticsDraft>(initialDraft);
  const [patient, setPatient] = useState<ProstheticsPatient | null>(null);
  const [orders, setOrders] = useState<ProstheticsOrder[]>([]);
  const [templates, setTemplates] = useState<FlowTemplate[]>([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [loadingTemplates, setLoadingTemplates] = useState(false);

  useEffect(() => {
    const saved = sessionStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        setDraft(JSON.parse(saved));
      } catch {
        sessionStorage.removeItem(STORAGE_KEY);
      }
    }
  }, []);

  const persist = (updated: ProstheticsDraft) => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  };

  const setDraftField = useCallback(
    (field: keyof ProstheticsDraft, value: string | null) => {
      setDraft((prev) => {
        const updated = { ...prev, [field]: value };
        persist(updated);
        return updated;
      });
    },
    [],
  );

  const resetDraft = useCallback(() => {
    setDraft(initialDraft);
    sessionStorage.removeItem(STORAGE_KEY);
  }, []);

  const loadOrders = useCallback(async () => {
    setLoadingOrders(true);
    try {
      const res = await prostheticsOrderApi.list();
      setOrders(res.data);
    } catch {
      setOrders([]);
    } finally {
      setLoadingOrders(false);
    }
  }, []);

  const loadTemplates = useCallback(async () => {
    setLoadingTemplates(true);
    try {
      const res = await flowTemplateApi.list();
      setTemplates(res.data);
    } catch {
      setTemplates([]);
    } finally {
      setLoadingTemplates(false);
    }
  }, []);

  useEffect(() => {
    if (draft.patientId) {
      prostheticsPatientApi
        .getById(draft.patientId)
        .then((res) => setPatient(res.data))
        .catch(() => setPatient(null));
    }
  }, [draft.patientId]);

  return (
    <ProstheticsContext.Provider
      value={{
        draft,
        setDraftField,
        resetDraft,
        patient,
        orders,
        templates,
        loadingOrders,
        loadingTemplates,
        loadOrders,
        loadTemplates,
      }}
    >
      {children}
    </ProstheticsContext.Provider>
  );
}

export function useProsthetics() {
  const ctx = useContext(ProstheticsContext);
  if (!ctx) {
    throw new Error('useProsthetics must be used within ProstheticsProvider');
  }
  return ctx;
}
