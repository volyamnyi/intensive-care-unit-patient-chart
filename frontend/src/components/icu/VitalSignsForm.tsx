import { useState } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import type { HourlyRecordCreateRequest } from '../../types/icu';
import { CLINICAL_RANGES } from '../../lib/clinicalRanges';

interface VitalSignsFormProps {
  values: HourlyRecordCreateRequest;
  onChange: (values: HourlyRecordCreateRequest) => void;
  onSave: () => void;
  saving?: boolean;
  title?: string;
  disabled?: boolean;
}

const setNum = (prev: HourlyRecordCreateRequest, field: keyof HourlyRecordCreateRequest, val: string) => ({
  ...prev,
  [field]: val === '' ? null : Number(val),
});

const setStr = (prev: HourlyRecordCreateRequest, field: keyof HourlyRecordCreateRequest, val: string) => ({
  ...prev,
  [field]: val,
});

type FieldRange = { min: number; max: number; unit: string; label: string };
const fieldRanges: Record<string, FieldRange> = CLINICAL_RANGES;

export default function VitalSignsForm({ values, onChange, onSave, saving, title, disabled }: VitalSignsFormProps) {
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const handleBlur = (field: string) => {
    setTouched((prev) => ({ ...prev, [field]: true }));
  };

  const validateField = (field: string, value: number | null | undefined): string | null => {
    if (value === null || value === undefined) return null;
    const range = fieldRanges[field];
    if (!range) return null;
    if (value < range.min || value > range.max) {
      return `${range.label} має бути в діапазоні ${range.min}–${range.max} ${range.unit}`;
    }
    return null;
  };

  const fieldWarning = (field: string, value: number | null | undefined) => {
    if (!touched[field]) return null;
    return validateField(field, value);
  };

  const handleSave = () => {
    const allTouched: Record<string, boolean> = {};
    Object.keys(fieldRanges).forEach((f) => { allTouched[f] = true; });
    setTouched((prev) => ({ ...prev, ...allTouched }));
    onSave();
  };

  return (
    <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2.5">
      {title && (
        <h3 className="font-rubik text-base font-semibold mb-1.5">{title}</h3>
      )}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-1.5">
        <div>
          <label htmlFor="systolicBP" className="text-sm text-muted-foreground mb-1 block">АТ сист. (мм рт.ст.)</label>
          <Input id="systolicBP" type="number" value={values.systolicBP ?? ''}
            onChange={(e) => onChange(setNum(values, 'systolicBP', e.target.value))}
            onBlur={() => handleBlur('systolicBP')}
            disabled={disabled}
            min={CLINICAL_RANGES.systolicBP.min} max={CLINICAL_RANGES.systolicBP.max} step={1}
            className={fieldWarning('systolicBP', values.systolicBP) ? 'border-destructive' : ''}
          />
          {fieldWarning('systolicBP', values.systolicBP) && <p className="text-xs text-destructive mt-0.5">{fieldWarning('systolicBP', values.systolicBP)}</p>}
        </div>
        <div>
          <label htmlFor="diastolicBP" className="text-sm text-muted-foreground mb-1 block">АТ діас. (мм рт.ст.)</label>
          <Input id="diastolicBP" type="number" value={values.diastolicBP ?? ''}
            onChange={(e) => onChange(setNum(values, 'diastolicBP', e.target.value))}
            onBlur={() => handleBlur('diastolicBP')}
            disabled={disabled}
            min={CLINICAL_RANGES.diastolicBP.min} max={CLINICAL_RANGES.diastolicBP.max} step={1}
            className={fieldWarning('diastolicBP', values.diastolicBP) ? 'border-destructive' : ''}
          />
          {fieldWarning('diastolicBP', values.diastolicBP) && <p className="text-xs text-destructive mt-0.5">{fieldWarning('diastolicBP', values.diastolicBP)}</p>}
        </div>
        <div>
          <label htmlFor="heartRate" className="text-sm text-muted-foreground mb-1 block">ЧСС (уд/хв)</label>
          <Input id="heartRate" type="number" value={values.heartRate ?? ''}
            onChange={(e) => onChange(setNum(values, 'heartRate', e.target.value))}
            onBlur={() => handleBlur('heartRate')}
            disabled={disabled}
            min={CLINICAL_RANGES.heartRate.min} max={CLINICAL_RANGES.heartRate.max} step={1}
            className={fieldWarning('heartRate', values.heartRate) ? 'border-destructive' : ''}
          />
          {fieldWarning('heartRate', values.heartRate) && <p className="text-xs text-destructive mt-0.5">{fieldWarning('heartRate', values.heartRate)}</p>}
        </div>
        <div>
          <label htmlFor="spo2" className="text-sm text-muted-foreground mb-1 block">SpO₂ (%)</label>
          <Input id="spo2" type="number" value={values.spo2 ?? ''}
            onChange={(e) => onChange(setNum(values, 'spo2', e.target.value))}
            onBlur={() => handleBlur('spo2')}
            disabled={disabled}
            min={CLINICAL_RANGES.spo2.min} max={CLINICAL_RANGES.spo2.max} step={1}
            className={fieldWarning('spo2', values.spo2) ? 'border-destructive' : ''}
          />
          {fieldWarning('spo2', values.spo2) && <p className="text-xs text-destructive mt-0.5">{fieldWarning('spo2', values.spo2)}</p>}
        </div>
        <div>
          <label htmlFor="temperature" className="text-sm text-muted-foreground mb-1 block">Температура (°C)</label>
          <Input id="temperature" type="number" value={values.temperature ?? ''}
            onChange={(e) => onChange(setNum(values, 'temperature', e.target.value))}
            onBlur={() => handleBlur('temperature')}
            disabled={disabled}
            min={CLINICAL_RANGES.temperature.min} max={CLINICAL_RANGES.temperature.max} step={0.1}
            className={fieldWarning('temperature', values.temperature) ? 'border-destructive' : ''}
          />
          {fieldWarning('temperature', values.temperature) && <p className="text-xs text-destructive mt-0.5">{fieldWarning('temperature', values.temperature)}</p>}
        </div>
        <div>
          <label htmlFor="cvp" className="text-sm text-muted-foreground mb-1 block">ЦВТ (мм рт.ст.)</label>
          <Input id="cvp" type="number" value={values.cvp ?? ''}
            onChange={(e) => onChange(setNum(values, 'cvp', e.target.value))}
            disabled={disabled}
            min={0} max={50} step={1}
          />
        </div>
        <div>
          <label htmlFor="respiratoryRate" className="text-sm text-muted-foreground mb-1 block">ЧД (дих/хв)</label>
          <Input id="respiratoryRate" type="number" value={values.respiratoryRate ?? ''}
            onChange={(e) => onChange(setNum(values, 'respiratoryRate', e.target.value))}
            onBlur={() => handleBlur('respiratoryRate')}
            disabled={disabled}
            min={CLINICAL_RANGES.respiratoryRate.min} max={CLINICAL_RANGES.respiratoryRate.max} step={1}
            className={fieldWarning('respiratoryRate', values.respiratoryRate) ? 'border-destructive' : ''}
          />
          {fieldWarning('respiratoryRate', values.respiratoryRate) && <p className="text-xs text-destructive mt-0.5">{fieldWarning('respiratoryRate', values.respiratoryRate)}</p>}
        </div>
        <div>
          <label htmlFor="consciousness" className="text-sm text-muted-foreground mb-1 block">Свідомість</label>
          <Input id="consciousness" value={values.consciousness ?? ''}
            onChange={(e) => onChange(setStr(values, 'consciousness', e.target.value))}
            disabled={disabled}
          />
        </div>
        <div>
          <label htmlFor="etco2" className="text-sm text-muted-foreground mb-1 block">EtCO₂ (мм рт.ст.)</label>
          <Input id="etco2" type="number" value={values.etco2 ?? ''}
            onChange={(e) => onChange(setNum(values, 'etco2', e.target.value))}
            disabled={disabled}
            min={0} max={100} step={1}
          />
        </div>
        <div>
          <label htmlFor="fio2" className="text-sm text-muted-foreground mb-1 block">FiO₂ (%)</label>
          <Input id="fio2" type="number" value={values.fio2 ?? ''}
            onChange={(e) => onChange(setNum(values, 'fio2', e.target.value))}
            disabled={disabled}
            min={21} max={100} step={1}
          />
        </div>
        <div>
          <label htmlFor="urineOutput" className="text-sm text-muted-foreground mb-1 block">Діурез (мл)</label>
          <Input id="urineOutput" type="number" value={values.urineOutput ?? ''}
            onChange={(e) => onChange(setNum(values, 'urineOutput', e.target.value))}
            disabled={disabled}
            min={0} max={2000} step={10}
          />
        </div>
        <div>
          <label htmlFor="drainOutput" className="text-sm text-muted-foreground mb-1 block">Дренаж (мл)</label>
          <Input id="drainOutput" type="number" value={values.drainOutput ?? ''}
            onChange={(e) => onChange(setNum(values, 'drainOutput', e.target.value))}
            disabled={disabled}
            min={0} max={5000} step={10}
          />
        </div>
        <div>
          <label htmlFor="painScore" className="text-sm text-muted-foreground mb-1 block">Біль (0-10)</label>
          <Input id="painScore" type="number" value={values.painScore ?? ''}
            onChange={(e) => onChange(setNum(values, 'painScore', e.target.value))}
            disabled={disabled}
            min={0} max={10} step={1}
          />
        </div>
      </div>
      <div className="col-span-full mt-2">
        <label htmlFor="notes" className="text-sm text-muted-foreground mb-1 block">Примітки</label>
        <textarea id="notes"
          className="flex min-h-[60px] w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-base transition-colors outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring disabled:pointer-events-none disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50 md:text-sm dark:bg-input/30 dark:disabled:bg-input/80"
          value={values.notes ?? ''}
          onChange={(e) => onChange(setStr(values, 'notes', e.target.value))}
          disabled={disabled}
          rows={2}
        />
      </div>
      <Button variant="default" className="mt-2" onClick={handleSave} disabled={disabled || saving}>
        {saving ? 'Збереження...' : 'Зберегти'}
      </Button>
    </div>
  );
}
