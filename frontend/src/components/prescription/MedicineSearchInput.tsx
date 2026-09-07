import { useState, useCallback, useRef, useEffect } from 'react';
import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import type { MedicineCatalogItem } from '../../types/medication';

export interface MedicineSearchInputProps {
  canEdit: boolean;
  isDoctor: boolean;
  onAddItem: (data: { medicineName: string; medicineMethod?: string; regime?: string }) => Promise<void>;
  onSearchMedicine: (keyword: string) => Promise<MedicineCatalogItem[]>;
}

export default function MedicineSearchInput({
  canEdit, isDoctor, onAddItem, onSearchMedicine,
}: MedicineSearchInputProps) {
  const [medSearch, setMedSearch] = useState('');
  const [medOptions, setMedOptions] = useState<MedicineCatalogItem[]>([]);
  const [selectedMed, setSelectedMed] = useState<MedicineCatalogItem | undefined>(undefined);
  const [newMethod, setNewMethod] = useState('');
  const [newRegime, setNewRegime] = useState('');
  const [addingDrug, setAddingDrug] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [searchError, setSearchError] = useState(false);
  const blurTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => () => {
    if (blurTimerRef.current) clearTimeout(blurTimerRef.current);
  }, []);

  const handleMedSearch = useCallback(async (q: string) => {
    setMedSearch(q);
    if (q.length < 2) { setMedOptions([]); setShowSuggestions(false); setSearchError(false); return; }
    setSearchError(false);
    try {
      const res = await onSearchMedicine(q);
      setMedOptions(res);
      setShowSuggestions(res.length > 0);
    } catch {
      setMedOptions([]);
      setShowSuggestions(false);
      setSearchError(true);
    }
  }, [onSearchMedicine]);

  const selectMedicine = (med: MedicineCatalogItem) => {
    setSelectedMed(med);
    setMedSearch(med.name);
    setShowSuggestions(false);
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
      setMedOptions([]);
      setShowSuggestions(false);
      setSearchError(false);
    } finally {
      setAddingDrug(false);
    }
  };

  const hasValidDrug = !!(selectedMed?.name?.trim() || medSearch.trim());

  if (!canEdit || !isDoctor) return null;

  return (
    <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-1.5 flex gap-1 items-center flex-wrap relative">
      <div className="relative min-w-[220px] flex-1">
        <Input
          placeholder="Препарат"
          value={medSearch}
          onChange={(e) => {
            setMedSearch(e.target.value);
            setSelectedMed(undefined);
            handleMedSearch(e.target.value);
          }}
          onFocus={() => { if (medOptions.length > 0) setShowSuggestions(true); }}
          onBlur={() => {
            if (blurTimerRef.current) clearTimeout(blurTimerRef.current);
            blurTimerRef.current = setTimeout(() => setShowSuggestions(false), 150);
          }}
        />
        {showSuggestions && (
          <div className="absolute top-full left-0 right-0 z-50 mt-0.5 rounded-lg border bg-popover text-popover-foreground shadow-md max-h-48 overflow-y-auto">
            {medOptions.map((med) => (
              <button
                key={med.id}
                type="button"
                className={cn(
                  'flex w-full min-h-11 items-center px-2 text-left text-sm hover:bg-accent hover:text-accent-foreground',
                  selectedMed?.id === med.id && 'bg-accent'
                )}
                onClick={() => selectMedicine(med)}
              >
                {med.name}
                {med.isHighRisk && <span className="ml-1 text-destructive text-xs">(HR)</span>}
              </button>
            ))}
          </div>
        )}
        {searchError && !showSuggestions && (
          <p className="text-destructive text-xs mt-1 px-1">
            Не вдалося завантажити каталог ліків з MIS
          </p>
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
