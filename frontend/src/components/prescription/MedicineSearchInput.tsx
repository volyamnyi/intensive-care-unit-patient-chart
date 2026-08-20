import { useState, useCallback } from 'react';
import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import type { MedicineCatalogItem, AllergyItem } from '../../types/medication';

const fallbackCatalog: MedicineCatalogItem[] = [
  { id: 1, name: 'Paracetamol', categoryRef: 1, ptgCode: '1', isHighRisk: false },
  { id: 3, name: 'Morphine', categoryRef: 14, ptgCode: '4', isHighRisk: true },
  { id: 5, name: 'Ceftriaxone', categoryRef: 2, ptgCode: '6', isHighRisk: false },
  { id: 6, name: 'Metronidazole', categoryRef: 2, ptgCode: '2,3', isHighRisk: false },
  { id: 7, name: 'Omeprazole', categoryRef: 3, ptgCode: '1', isHighRisk: false },
  { id: 8, name: 'Heparin', categoryRef: 5, ptgCode: '5', isHighRisk: false },
  { id: 9, name: 'Norepinephrine', categoryRef: 13, ptgCode: '3', isHighRisk: true },
  { id: 10, name: 'Dopamine', categoryRef: 13, ptgCode: '3', isHighRisk: true },
  { id: 11, name: 'NaCl 0.9%', categoryRef: 8, ptgCode: null, isHighRisk: false },
  { id: 12, name: 'Glucose 5%', categoryRef: 8, ptgCode: null, isHighRisk: false },
  { id: 13, name: 'Midazolam', categoryRef: 14, ptgCode: '4', isHighRisk: true },
  { id: 14, name: 'Propofol', categoryRef: 14, ptgCode: '4', isHighRisk: true },
  { id: 15, name: 'Dexamethasone', categoryRef: 1, ptgCode: '1', isHighRisk: false },
  { id: 16, name: 'Insulin', categoryRef: 6, ptgCode: null, isHighRisk: false },
  { id: 19, name: 'Ondansetron', categoryRef: 4, ptgCode: '2', isHighRisk: false },
  { id: 20, name: 'Pantoprazole', categoryRef: 3, ptgCode: '1', isHighRisk: false },
];

export interface MedicineSearchInputProps {
  canEdit: boolean;
  isDoctor: boolean;
  allergies: AllergyItem[];
  onAddItem: (data: { medicineName: string; medicineMethod?: string; regime?: string }) => Promise<void>;
  onSearchMedicine: (keyword: string) => Promise<MedicineCatalogItem[]>;
}

export default function MedicineSearchInput({
  canEdit, isDoctor, allergies, onAddItem, onSearchMedicine,
}: MedicineSearchInputProps) {
  const [medSearch, setMedSearch] = useState('');
  const [medOptions, setMedOptions] = useState<MedicineCatalogItem[]>([]);
  const [selectedMed, setSelectedMed] = useState<MedicineCatalogItem | undefined>(undefined);
  const [newMethod, setNewMethod] = useState('');
  const [newRegime, setNewRegime] = useState('');
  const [addingDrug, setAddingDrug] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);

  const handleMedSearch = useCallback(async (q: string) => {
    setMedSearch(q);
    if (q.length < 2) { setMedOptions([]); setShowSuggestions(false); return; }
    try {
      const res = await onSearchMedicine(q);
      const opts = res.length > 0 ? res : fallbackCatalog.filter(m =>
        m.name.toLowerCase().includes(q.toLowerCase()));
      setMedOptions(opts);
      setShowSuggestions(opts.length > 0);
    } catch {
      const opts = fallbackCatalog.filter(m =>
        m.name.toLowerCase().includes(q.toLowerCase()));
      setMedOptions(opts);
      setShowSuggestions(opts.length > 0);
    }
  }, [onSearchMedicine]);

  const selectMedicine = (med: MedicineCatalogItem) => {
    setSelectedMed(med);
    setMedSearch(med.name);
    setShowSuggestions(false);
  };

  const handleAddDrug = async () => {
    // Resolve the drug name: prefer the selected suggestion, else fall back to
    // the user-typed text so «Додати» is not locked behind a dropdown click.
    const medName = selectedMed?.name?.trim() || medSearch.trim();
    if (!medName) return;
    const allergy = allergies.find(a =>
      a.allergenName.toLowerCase() === medName.toLowerCase()
    );
    if (allergy) {
      alert(`У пацієнта алергія на препарат "${allergy.allergenName}"!`);
      return;
    }
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
          onBlur={() => setTimeout(() => setShowSuggestions(false), 150)}
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
