import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { cn } from '@/lib/utils';
import { Loader2 } from 'lucide-react';
import type { LabResult, LabResultCreateRequest } from '../../types';

const PREDEFINED_TESTS: { code: string; name: string; unit: string; min: number | null; max: number | null }[] = [
  { code: 'Hb', name: 'Hemoglobin', unit: 'g/dL', min: 12, max: 16 },
  { code: 'Ht', name: 'Hematocrit', unit: '%', min: 36, max: 48 },
  { code: 'WBC', name: 'White blood cells', unit: '10⁹/L', min: 4, max: 9 },
  { code: 'Na', name: 'Sodium', unit: 'mmol/L', min: 135, max: 145 },
  { code: 'K', name: 'Potassium', unit: 'mmol/L', min: 3.5, max: 5.1 },
  { code: 'pH', name: 'pH', unit: '', min: 7.35, max: 7.45 },
  { code: 'pCO2', name: 'pCO₂', unit: 'mmHg', min: 35, max: 45 },
  { code: 'pO2', name: 'pO₂', unit: 'mmHg', min: 80, max: 100 },
  { code: 'HCO3', name: 'Bicarbonate', unit: 'mmol/L', min: 22, max: 26 },
  { code: 'BE', name: 'Base excess', unit: 'mmol/L', min: -2, max: 2 },
  { code: 'Lactate', name: 'Lactate', unit: 'mmol/L', min: 0.5, max: 2 },
  { code: 'Glucose', name: 'Glucose', unit: 'mmol/L', min: 3.9, max: 6.1 },
  { code: 'Ca', name: 'Calcium', unit: 'mmol/L', min: 2.1, max: 2.6 },
  { code: 'Cl', name: 'Chloride', unit: 'mmol/L', min: 98, max: 107 },
  { code: 'Cr', name: 'Creatinine', unit: 'μmol/L', min: 62, max: 106 },
  { code: 'Urea', name: 'Urea', unit: 'mmol/L', min: 2.5, max: 8.3 },
  { code: 'ALT', name: 'ALT', unit: 'U/L', min: 0, max: 41 },
  { code: 'AST', name: 'AST', unit: 'U/L', min: 0, max: 40 },
  { code: 'Bilirubin', name: 'Bilirubin', unit: 'μmol/L', min: 3.4, max: 20.5 },
  { code: 'INR', name: 'INR', unit: '', min: 0.8, max: 1.2 },
  { code: 'aPTT', name: 'aPTT', unit: 's', min: 25, max: 36 },
  { code: 'CRP', name: 'CRP', unit: 'mg/L', min: 0, max: 5 },
  { code: 'Procalcitonin', name: 'Procalcitonin', unit: 'ng/mL', min: 0, max: 0.5 },
];

interface LabResultsPanelProps {
  clinicalDayId: string;
  labs: LabResult[];
  isLocked: boolean;
  onCreate: (data: LabResultCreateRequest) => Promise<void>;
}

export default function LabResultsPanel({
  labs, isLocked, onCreate,
}: LabResultsPanelProps) {
  const [selectedCode, setSelectedCode] = useState('');
  const [result, setResult] = useState('');
  const [saving, setSaving] = useState(false);

  const selected = PREDEFINED_TESTS.find((x) => x.code === selectedCode);

  const handleAdd = async () => {
    const code = selectedCode;
    const resVal = result.trim();
    const sel = PREDEFINED_TESTS.find((x) => x.code === code);
    if (!sel || !resVal || isLocked) return;
    try {
      setSaving(true);
      await onCreate({
        testCode: sel.code,
        testName: sel.name,
        result: resVal,
        unit: sel.unit,
        referenceMin: sel.min,
        referenceMax: sel.max,
        measuredAt: new Date().toISOString(),
      });
      setSelectedCode('');
      setResult('');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      {!isLocked && (
        <div className="mb-3 flex flex-col items-start gap-2 sm:flex-row">
          <Select value={selectedCode} onValueChange={(v: string | null) => { if (v !== null) setSelectedCode(v); }}>
            <SelectTrigger aria-label="Тест" className="h-7 min-w-[200px]">
              <SelectValue placeholder="Тест" />
            </SelectTrigger>
            <SelectContent>
              {PREDEFINED_TESTS.map((x) => (
                <SelectItem key={x.code} value={x.code}>{x.name} ({x.code})</SelectItem>
              ))}
            </SelectContent>
          </Select>
          {selected && (
            <span className="self-center text-[11px] text-muted-foreground font-mulish">
              {`Норма: ${selected.min ?? '—'}–${selected.max ?? '—'} ${selected.unit}`}
            </span>
          )}
          <Input
            type="number" placeholder="Результат"
            value={result} onChange={(e) => setResult(e.target.value)}
            className="h-7 w-[130px]"
          />
          <div className="flex items-center">
            <Button size="sm" onClick={handleAdd} disabled={saving || !selected || result.trim() === ''} className="mt-0.5">
              {saving ? <Loader2 className="mr-1 size-3.5 animate-spin" /> : null}
              {saving ? 'Зберігається...' : 'Додати'}
            </Button>
          </div>
        </div>
      )}

      {labs.length === 0 ? (
        <p className="text-xs text-muted-foreground font-mulish">{'Немає лабораторних досліджень'}</p>
      ) : (
        <div className="flex flex-col gap-[3px]">
          {labs.map((l) => (
            <div
              key={l.id}
              className={cn(
                'flex items-center justify-between gap-1 rounded-xl border bg-card p-2 text-card-foreground shadow-sm',
                l.isAbnormal ? 'border-destructive' : '',
              )}
            >
              <div className="min-w-0">
                <p className="text-xs font-semibold font-rubik">
                  {l.testName}
                  {l.isAbnormal && <Badge variant="destructive" className="ml-1 h-[18px] text-[9px] font-bold">Аномалія</Badge>}
                </p>
                <p className="text-[11px] text-muted-foreground font-mulish">
                  {`${l.result} ${l.unit}${(l.referenceMin ?? l.referenceMax) != null ? ` (${l.referenceMin ?? '—'}–${l.referenceMax ?? '—'} ${l.unit})` : ''}`}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
