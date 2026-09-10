import { useState, useRef, useEffect, useCallback } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Loader2 } from 'lucide-react';
import { patientApi } from '../../api/platform';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PatientDto } from '../../types/core';
import { cn } from '@/lib/utils';

/** ICU patient source (Phase 8, #261): the backend roster holds department 19 only. */
const ICU_MODULE = 'icu';

interface PatientSearchProps {
  onSelect: (patient: PatientDto) => void;
  label?: string;
}

export default function PatientSearch({ onSelect, label }: PatientSearchProps) {
  const resolvedLabel = label ?? 'ПІБ, телефон або ID';
  const [search, setSearch] = useState('');
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [selected, setSelected] = useState<PatientDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const debounceRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const abortRef = useRef<AbortController>(undefined);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
      if (abortRef.current) abortRef.current.abort();
    };
  }, []);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const performSearch = useCallback(async (query: string) => {
    if (abortRef.current) abortRef.current.abort();
    if (query.length < 2) { setPatients([]); return; }

    const controller = new AbortController();
    abortRef.current = controller;
    setLoading(true);
    setError(null);
    try {
      const res = await patientApi.searchByModule(ICU_MODULE, query, controller.signal);
      setPatients(res.data);
      setOpen(true);
    } catch (err: unknown) {
      if (err instanceof Error && err.name === 'CanceledError') return;
      setPatients([]);
      setError(getErrorMessage(err, 'Не вдалося знайти пацієнтів'));
      setOpen(true);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleInputChange = (value: string) => {
    setSearch(value);
    if (selected) setSelected(null);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => performSearch(value), 300);
  };

  const handleSelect = (patient: PatientDto) => {
    setSelected(patient);
    setSearch(`${patient.fullName} (ID ${patient.id})`);
    setOpen(false);
    onSelect(patient);
  };

  return (
    <div className="relative" ref={wrapperRef}>
      <div className="relative">
        <Input
          value={search}
          onChange={(e) => handleInputChange(e.target.value)}
          onFocus={() => { if (patients.length > 0) setOpen(true); }}
          placeholder={resolvedLabel}
          aria-label={resolvedLabel}
        />
        {loading && (
          <Loader2 role="progressbar" aria-label="Loading" className="absolute right-2 top-1/2 -translate-y-1/2 size-4 animate-spin text-muted-foreground" />
        )}
      </div>
      {open && patients.length > 0 && (
        <div className="absolute z-50 mt-1 w-full rounded-lg border bg-popover text-popover-foreground shadow-md">
          {patients.map((p) => (
            <div
              key={p.id}
              onClick={() => handleSelect(p)}
              className={cn(
                'px-2 py-1.5 cursor-pointer hover:bg-accent hover:text-accent-foreground',
                selected?.id === p.id && 'bg-accent'
              )}
            >
              <p className="font-semibold text-sm">{p.fullName}</p>
              <p className="text-xs text-muted-foreground">
                ID {p.id} &middot; {p.birthDate} &middot; {p.address?.split(',')[0]?.trim()}
              </p>
            </div>
          ))}
        </div>
      )}
      {open && search.length >= 2 && patients.length === 0 && !loading && (
        <div className="absolute z-50 mt-1 w-full rounded-lg border bg-popover text-popover-foreground shadow-md px-2 py-1.5 text-sm text-muted-foreground">
          {error ?? 'Пацієнтів не знайдено'}
          {error && (
            <Button variant="ghost" size="sm" className="ml-2" onClick={() => performSearch(search)}>
              Повторити
            </Button>
          )}
        </div>
      )}
      {search.length < 2 && (
        <p className="text-xs text-muted-foreground mt-0.5">Введіть мінімум 2 символи</p>
      )}
    </div>
  );
}
