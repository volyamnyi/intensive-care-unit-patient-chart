import { useState, useEffect, useRef } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Loader2 } from 'lucide-react';
import type { MedicineCatalogItem, PrescriptionItemAddRequest, AllergyItem } from '../../types/medication';
import AllergyWarning from './AllergyWarning';

interface PrescriptionItemFormProps {
  onSubmit: (data: PrescriptionItemAddRequest) => void;
  onSearchMedicine: (keyword: string) => Promise<MedicineCatalogItem[]>;
  allergies?: AllergyItem[];
  disabled?: boolean;
}

export default function PrescriptionItemForm({ onSubmit, onSearchMedicine, allergies, disabled }: PrescriptionItemFormProps) {
  const [medicine, setMedicine] = useState<MedicineCatalogItem | null>(null);
  const [medicineMethod, setMedicineMethod] = useState('');
  const [regime, setRegime] = useState('');
  const [options, setOptions] = useState<MedicineCatalogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [open, setOpen] = useState(false);
  const abortRef = useRef<AbortController | null>(null);
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

  useEffect(() => {
    if (inputValue.length < 2) {
      setOptions([]);
      return;
    }
    const controller = new AbortController();
    abortRef.current = controller;
    setLoading(true);
    const timeout = setTimeout(() => {
      onSearchMedicine(inputValue)
        .then((res) => {
          if (!controller.signal.aborted) {
            setOptions(res);
            setOpen(true);
          }
        })
        .catch(() => setOptions([]))
        .finally(() => {
          if (!controller.signal.aborted) {
            setLoading(false);
          }
        });
    }, 300);
    return () => {
      clearTimeout(timeout);
      controller.abort();
    };
  }, [inputValue, onSearchMedicine]);

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
    setOptions([]);
  };

  const medicineName = medicine?.name || '';

  return (
    <div>
      <div className="grid grid-cols-12 gap-2 items-start">
        <div className="col-span-12 sm:col-span-4 relative" ref={wrapperRef}>
          <div className="relative">
            <Input
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onFocus={() => { if (options.length > 0) setOpen(true); }}
              placeholder="Препарат"
              disabled={disabled}
            />
            {loading && (
              <Loader2 className="absolute right-2 top-1/2 -translate-y-1/2 size-4 animate-spin text-muted-foreground" />
            )}
          </div>
          {open && options.length > 0 && (
            <div className="absolute z-50 mt-1 w-full rounded-lg border bg-popover text-popover-foreground shadow-md max-h-48 overflow-auto">
              {options.map((option) => (
                <div
                  key={option.id || option.name}
                  className="px-2 py-1.5 text-sm cursor-pointer hover:bg-accent hover:text-accent-foreground"
                  onClick={() => {
                    setMedicine(option);
                    setInputValue(option.name);
                    setOpen(false);
                  }}
                >
                  {option.name}
                </div>
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
          <Button variant="default" size="sm" disabled={disabled || !medicine} onClick={handleSubmit}>
            Додати
          </Button>
        </div>
      </div>
      <AllergyWarning medicineName={medicineName} allergies={allergies ?? []} />
    </div>
  );
}
