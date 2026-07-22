import { useCallback, useEffect, useRef, useState } from 'react';

type AutoSaveStatus = 'idle' | 'saving' | 'saved' | 'error';

interface UseAutoSaveOptions {
  onSave: () => Promise<void>;
  delay?: number;
  enabled?: boolean;
}

interface UseAutoSaveReturn {
  status: AutoSaveStatus;
  markDirty: () => void;
  saveNow: () => Promise<void>;
  lastSavedAt: Date | null;
  error: string | null;
}

export function useAutoSave({
  onSave,
  delay = 8000,
  enabled = true,
}: UseAutoSaveOptions): UseAutoSaveReturn {
  const [status, setStatus] = useState<AutoSaveStatus>('idle');
  const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);
  const [error, setError] = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const dirtyRef = useRef(false);
  const savingRef = useRef(false);
  const onSaveRef = useRef(onSave);
  onSaveRef.current = onSave;

  const clearTimer = useCallback(() => {
    if (timerRef.current !== null) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const performSave = useCallback(async () => {
    if (savingRef.current || !dirtyRef.current) return;
    savingRef.current = true;
    setStatus('saving');
    setError(null);
    try {
      await onSaveRef.current();
      dirtyRef.current = false;
      setStatus('saved');
      setLastSavedAt(new Date());
    } catch {
      setStatus('error');
      setError('Помилка збереження');
    } finally {
      savingRef.current = false;
    }
  }, []);

  const markDirty = useCallback(() => {
    if (!enabled) return;
    dirtyRef.current = true;
    clearTimer();
    timerRef.current = setTimeout(performSave, delay);
    if (status === 'saved' || status === 'error') {
      setStatus('idle');
    }
  }, [enabled, delay, clearTimer, performSave, status]);

  const saveNow = useCallback(async () => {
    clearTimer();
    await performSave();
  }, [clearTimer, performSave]);

  useEffect(() => {
    if (status === 'saved' || status === 'error') {
      const t = setTimeout(() => setStatus('idle'), 3000);
      return () => clearTimeout(t);
    }
  }, [status]);

  useEffect(() => {
    return () => clearTimer();
  }, [clearTimer]);

  return { status, markDirty, saveNow, lastSavedAt, error };
}
