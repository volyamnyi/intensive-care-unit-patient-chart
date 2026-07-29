import { useState } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import type { VitalSignEntry, VitalSignEntryCreateRequest } from '../../types';

interface VitalSignFormProps {
  latest?: VitalSignEntry | null;
  onSubmit: (data: VitalSignEntryCreateRequest) => void;
  disabled?: boolean;
  saving?: boolean;
}

const emptyValues: VitalSignEntryCreateRequest = {
  temperature: undefined,
  systolicBp: undefined,
  diastolicBp: undefined,
  spo2: undefined,
  pulse: undefined,
  stool: '',
  painScore: undefined,
};

export default function VitalSignForm({ latest, onSubmit, disabled, saving }: VitalSignFormProps) {
  const [values, setValues] = useState<VitalSignEntryCreateRequest>(emptyValues);

  const setNum = (field: keyof VitalSignEntryCreateRequest, val: string) => {
    setValues((prev) => ({ ...prev, [field]: val === '' ? undefined : Number(val) }));
  };

  const handleSubmit = () => {
    const payload: VitalSignEntryCreateRequest = {};
    if (values.temperature !== undefined) payload.temperature = values.temperature;
    if (values.systolicBp !== undefined) payload.systolicBp = values.systolicBp;
    if (values.diastolicBp !== undefined) payload.diastolicBp = values.diastolicBp;
    if (values.spo2 !== undefined) payload.spo2 = values.spo2;
    if (values.pulse !== undefined) payload.pulse = values.pulse;
    if (values.stool && values.stool.trim() !== '') payload.stool = values.stool.trim();
    if (values.painScore !== undefined) payload.painScore = values.painScore;
    onSubmit(payload);
    setValues(emptyValues);
  };

  return (
    <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2">
      <h4 className="font-rubik text-sm font-medium mb-1.5">
        Життєві показники
      </h4>
      {latest && (
        <p className="text-sm text-muted-foreground mb-1.5">
          {`Останні: Темп ${latest.temperature ?? '-'}, АТ ${latest.systolicBp ?? '-'} / ${latest.diastolicBp ?? '-'}, SpO₂ ${latest.spo2 ?? '-'}%, Пульс ${latest.pulse ?? '-'}, Біль ${latest.painScore ?? '-'}`}
        </p>
      )}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-1.5">
        <div>
          <label className="text-sm text-muted-foreground mb-1 block">Температура (°C)</label>
          <Input type="number" value={values.temperature ?? ''}
            onChange={(e) => setNum('temperature', e.target.value)}
            disabled={disabled}
            min={34} max={42} step={0.1}
          />
        </div>
        <div>
          <label className="text-sm text-muted-foreground mb-1 block">Сист. АТ</label>
          <Input type="number" value={values.systolicBp ?? ''}
            onChange={(e) => setNum('systolicBp', e.target.value)}
            disabled={disabled}
            min={50} max={250} step={1}
          />
        </div>
        <div>
          <label className="text-sm text-muted-foreground mb-1 block">Діаст. АТ</label>
          <Input type="number" value={values.diastolicBp ?? ''}
            onChange={(e) => setNum('diastolicBp', e.target.value)}
            disabled={disabled}
            min={30} max={150} step={1}
          />
        </div>
        <div>
          <label className="text-sm text-muted-foreground mb-1 block">SpO₂ (%)</label>
          <Input type="number" value={values.spo2 ?? ''}
            onChange={(e) => setNum('spo2', e.target.value)}
            disabled={disabled}
            min={50} max={100} step={1}
          />
        </div>
        <div>
          <label htmlFor="pulse" className="text-sm text-muted-foreground mb-1 block">Пульс</label>
          <Input id="pulse" type="number" value={values.pulse ?? ''}
            onChange={(e) => setNum('pulse', e.target.value)}
            disabled={disabled}
            min={0} max={300} step={1}
          />
        </div>
        <div>
          <label className="text-sm text-muted-foreground mb-1 block">Біль (0-10)</label>
          <Input type="number" value={values.painScore ?? ''}
            onChange={(e) => setNum('painScore', e.target.value)}
            disabled={disabled}
            min={0} max={10} step={1}
          />
        </div>
        <div className="col-span-2">
          <label className="text-sm text-muted-foreground mb-1 block">Стул</label>
          <Input value={values.stool ?? ''}
            onChange={(e) => setValues((prev) => ({ ...prev, stool: e.target.value }))}
            disabled={disabled}
          />
        </div>
      </div>
      <Button variant="default" className="mt-2" disabled={disabled || saving} onClick={handleSubmit}>
        {saving ? 'Збереження...' : 'Зберегти'}
      </Button>
    </div>
  );
}
