import { useState, useRef, useEffect } from 'react';
import { Plus, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import { useMedicineSearch } from './useMedicineSearch';
import type { MedicineCatalogItem } from '../../types/medication';

export interface MedicineSearchInputProps {
  canEdit: boolean;
  isDoctor: boolean;
  onAddItem: (data: { medicineName: string; medicineMethod?: string; regime?: string }) => Promise<void>;
  onSearchMedicine?: (keyword: string, signal?: AbortSignal) => Promise<MedicineCatalogItem[]>;
}

export default function MedicineSearchInput({
  canEdit, isDoctor, onAddItem, onSearchMedicine,
}: MedicineSearchInputProps) {
  const [medSearch, setMedSearch] = useState('');
  const [selectedMed, setSelectedMed] = useState<MedicineCatalogItem | undefined>(undefined);
  const [newMethod, setNewMethod] = useState('');
  const [newRegime, setNewRegime] = useState('');
  const [addingDrug, setAddingDrug] = useState(false);
  const [suggestionsDismissed, setSuggestionsDismissed] = useState(false);
  const blurTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => () => {
    if (blurTimerRef.current) clearTimeout(blurTimerRef.current);
  }, []);

  const { options: medOptions, loading, error: searchError, active, retry } = useMedicineSearch(medSearch, onSearchMedicine);

  const selectMedicine = (med: MedicineCatalogItem) => {
    setSelectedMed(med);
    setMedSearch(med.name);
    setSuggestionsDismissed(true);
  };

  const handleAddDrug = async () => {
    const medName = selectedMed?.name?.trim() || medSearch.trim();
    if (!medName) return;
    setAddingDrug(true);
    try {
      await onAddItem({
        medicineName: medName,
        medicineMethod: newMethod || undefined,
        regime: newRegime || undefined,
      });
      setSelectedMed(undefined);
      setMedSearch('');
      setNewMethod('');
      setNewRegime('');
      setSuggestionsDismissed(true);
    } finally {
      setAddingDrug(false);
    }
  };

  const hasValidDrug = !!(selectedMed?.name?.trim() || medSearch.trim());

  if (!canEdit || !isDoctor) return null;

  // Whenever the query has crossed the threshold and hasn't been dismissed, the
  // dropdown is visible and shows the honest state for it: loading, error
  // (with retry), an empty result, or the catalog options.
  const showSuggestions = active && !suggestionsDismissed;

  return (
    <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-1.5 flex gap-1 items-center flex-wrap relative">
      <div className="relative min-w-[220px] flex-1">
        <Input
          placeholder="Препарат"
          value={medSearch}
          onChange={(e) => {
            setMedSearch(e.target.value);
            setSelectedMed(undefined);
            setSuggestionsDismissed(false);
          }}
          onFocus={() => { if (active) setSuggestionsDismissed(false); }}
          onBlur={() => {
            if (blurTimerRef.current) clearTimeout(blurTimerRef.current);
            blurTimerRef.current = setTimeout(() => setSuggestionsDismissed(true), 150);
          }}
        />
        {showSuggestions && (
          <div className="absolute top-full left-0 right-0 z-50 mt-0.5 rounded-lg border bg-popover text-popover-foreground shadow-md max-h-48 overflow-y-auto">
            {loading && (
              <div className="flex min-h-11 items-center gap-1 px-2 text-xs text-muted-foreground">
                <Loader2 className="size-4 animate-spin" /> Завантаження каталогу…
              </div>
            )}
            {!loading && searchError && (
              <div className="flex min-h-11 items-center px-2 text-xs text-destructive">
                Не вдалося завантажити каталог ліків з MIS —{' '}
                <button type="button" className="underline hover:decoration-accent-foreground" onClick={retry}>
                  Спробувати ще раз
                </button>
              </div>
            )}
            {!loading && !searchError && medOptions.length === 0 && (
              <div className="flex min-h-11 items-center px-2 text-xs text-muted-foreground">Нічого не знайдено</div>
            )}
            {!loading && !searchError && medOptions.map((med) => (
              <button
                key={med.id}
                type="button"
                disabled={med.itemKindIsDisabled === true}
                aria-disabled={med.itemKindIsDisabled === true}
                className={cn(
                  'flex w-full min-h-11 items-center px-2 text-left text-sm',
                  med.itemKindIsDisabled === true
                    ? 'cursor-not-allowed text-muted-foreground/60'
                    : 'hover:bg-accent hover:text-accent-foreground',
                  selectedMed?.id === med.id && 'bg-accent'
                )}
                onClick={() => { if (med.itemKindIsDisabled !== true) selectMedicine(med); }}
              >
                {med.name}
                {med.itemKindIsDisabled === true && <span className="ml-1 text-muted-foreground/60 text-xs">(вимкнено)</span>}
                {med.isHighRisk && <span className="ml-1 text-destructive text-xs">(HR)</span>}
              </button>
            ))}
          </div>
        )}
      </div>
      <Input placeholder="Спосіб" value={newMethod}
        onChange={e => setNewMethod(e.target.value)} className="w-[120px]" />
      <Input placeholder="Режим" value={newRegime}
        onChange={e => setNewRegime(e.target.value)} className="w-[100px]" />
      <Button variant="default" size="sm" className="min-h-11" disabled={!hasValidDrug || addingDrug}
        onClick={handleAddDrug}><Plus className="size-4 mr-1" />Додати</Button>
    </div>
  );
}
