import { useState, useEffect, useRef } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useMedicineSearch } from './useMedicineSearch';
import type { MedicineCatalogItem, PrescriptionItemAddRequest } from '../../types/medication';

interface PrescriptionItemFormProps {
  onSubmit: (data: PrescriptionItemAddRequest) => void;
  onSearchMedicine?: (keyword: string, signal?: AbortSignal) => Promise<MedicineCatalogItem[]>;
  disabled?: boolean;
}

export default function PrescriptionItemForm({ onSubmit, onSearchMedicine, disabled }: PrescriptionItemFormProps) {
  const [medicine, setMedicine] = useState<MedicineCatalogItem | null>(null);
  const [medicineMethod, setMedicineMethod] = useState('');
  const [regime, setRegime] = useState('');
  const [inputValue, setInputValue] = useState('');
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const { options, loading, error: searchError, active, retry } = useMedicineSearch(inputValue, onSearchMedicine);

  useEffect(() => {
    if (!loading) setOpen(active);
  }, [loading, active]);

  const handleSubmit = () => {
    if (!medicine) return;
    onSubmit({
      medicineName: medicine.name,
      medicineMethod: medicineMethod || undefined,
      regime: regime || undefined,
    });
    setMedicine(null);
    setMedicineMethod('');
    setRegime('');
    setInputValue('');
    setOpen(false);
  };

  const isEmpty = !loading && !searchError && options.length === 0;

  return (
    <div>
      <div className="grid grid-cols-12 gap-2 items-start">
        <div className="col-span-12 sm:col-span-4 relative" ref={wrapperRef}>
          <div className="relative">
            <Input
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onFocus={() => { if (active) setOpen(true); }}
              placeholder="Препарат"
              disabled={disabled}
            />
            {loading && (
              <Loader2 className="absolute right-2 top-1/2 -translate-y-1/2 size-4 animate-spin text-muted-foreground" />
            )}
          </div>
          {open && (
            <div className="absolute z-50 mt-1 w-full rounded-lg border bg-popover text-popover-foreground shadow-md max-h-48 overflow-auto">
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
              {!loading && !searchError && isEmpty && (
                <div className="flex min-h-11 items-center px-2 text-xs text-muted-foreground">Нічого не знайдено</div>
              )}
              {!loading && !searchError && options.map((option) => (
                <button
                  key={option.id || option.name}
                  type="button"
                  disabled={option.itemKindIsDisabled === true}
                  aria-disabled={option.itemKindIsDisabled ? true : undefined}
                  className={cn(
                    'flex w-full min-h-11 items-center px-2 text-left text-sm',
                    option.itemKindIsDisabled === true
                      ? 'cursor-not-allowed text-muted-foreground/60'
                      : 'cursor-pointer hover:bg-accent hover:text-accent-foreground',
                    medicine?.id === option.id && 'bg-accent'
                  )}
                  onClick={() => {
                    if (option.itemKindIsDisabled === true) return;
                    setMedicine(option);
                    setInputValue(option.name);
                    setOpen(false);
                  }}
                >
                  {option.name}
                  {option.itemKindIsDisabled === true && (
                    <span className="ml-1 text-muted-foreground/60 text-xs">(вимкнено)</span>
                  )}
                  {option.isHighRisk === true && (
                    <span className="ml-1 text-destructive text-xs">(HR)</span>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>
        <div className="col-span-6 sm:col-span-3">
          <Input
            placeholder="Спосіб введення"
            value={medicineMethod}
            onChange={(e) => setMedicineMethod(e.target.value)}
            disabled={disabled}
          />
        </div>
        <div className="col-span-6 sm:col-span-3">
          <Input
            placeholder="Режим"
            value={regime}
            onChange={(e) => setRegime(e.target.value)}
            disabled={disabled}
          />
        </div>
        <div className="col-span-12 sm:col-span-2">
          <Button variant="default" size="sm" className="min-h-11" disabled={disabled || !medicine} onClick={handleSubmit}>
            Додати
          </Button>
        </div>
      </div>
    </div>
  );
}
