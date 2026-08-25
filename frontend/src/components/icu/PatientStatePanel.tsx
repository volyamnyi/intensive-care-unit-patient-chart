import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Loader2 } from 'lucide-react';
import type { PatientStateAssessment, PatientStateCreateRequest } from '../../types/icu';

const CONSCIOUSNESS = ['alert', 'drowsy', 'sopor', 'coma', 'sedation'];
const SKIN = ['normal', 'dry', 'cyanotic', 'jaundiced', 'pale', 'rash', 'marbling'];
const EDEMA = ['none', 'mild', 'moderate', 'severe'];
const MUCOSA = ['normal', 'dry', 'moist', 'pale', 'cyanotic'];
const CIRCULATION = ['normal', 'weak', 'bound', 'cold'];
const BOWEL = ['normal', 'hypoactive', 'absent', 'hyperactive'];

const optionLabels: Record<string, string> = {
  alert: 'Ясна', drowsy: 'Сонливість', sopor: 'Сопор', coma: 'Кома', sedation: 'Седація',
  normal: 'Норма', dry: 'Суха', cyanotic: 'Ціаноз', jaundiced: 'Жовтяниця', pale: 'Бліда', rash: 'Висип', marbling: 'Мармуровість',
  none: 'Немає', mild: 'Легкий', moderate: 'Помірний', severe: 'Тяжкий',
  moist: 'Волога', weak: 'Слабкий', bound: 'Напружений', cold: 'Холодна',
  hypoactive: 'Гіпоактивна', absent: 'Відсутня', hyperactive: 'Гіперактивна',
};

interface PatientStatePanelProps {
  clinicalDayId: string;
  assessments: PatientStateAssessment[];
  isLocked: boolean;
  onCreate: (data: PatientStateCreateRequest) => Promise<void>;
}

export default function PatientStatePanel({
  assessments, isLocked, onCreate,
}: PatientStatePanelProps) {
  const [form, setForm] = useState({
    recordHour: new Date().getHours(),
    consciousness: 'alert',
    skin: 'normal',
    edema: 'none',
    mucousMembranes: 'normal',
    peripheralCirculation: 'normal',
    bowelSounds: 'normal',
    generalCondition: '',
    additionalNotes: '',
  });
  const [saving, setSaving] = useState(false);

  const handleAdd = async () => {
    if (isLocked) return;
    try {
      setSaving(true);
      await onCreate({
        recordHour: Number(form.recordHour),
        consciousness: form.consciousness,
        skin: form.skin,
        edema: form.edema,
        mucousMembranes: form.mucousMembranes,
        peripheralCirculation: form.peripheralCirculation,
        bowelSounds: form.bowelSounds,
        generalCondition: form.generalCondition.trim() || undefined,
        additionalNotes: form.additionalNotes.trim() || undefined,
      });
      setForm((prev) => ({ ...prev, generalCondition: '', additionalNotes: '' }));
    } finally {
      setSaving(false);
    }
  };

  const set = (k: string, v: string) => {
    setForm((prev) => ({ ...prev, [k]: v }));
  };

  const SelectField = ({ label, options, value, onChange }: { label: string; options: string[]; value: string; onChange: (v: string) => void }) => (
    <div className="mb-1">
      <p className="mb-0.5 text-xs font-medium text-muted-foreground font-mulish">{label}</p>
      <Select value={value} onValueChange={(v: string | null) => { if (v !== null) onChange(v); }}>
        <SelectTrigger aria-label={label} className="h-7 w-full pointer-coarse:min-h-11">
          <SelectValue placeholder={label} />
        </SelectTrigger>
        <SelectContent>
          {options.map((opt) => (
            <SelectItem key={opt} value={opt}>{optionLabels[opt] || opt}</SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );

  return (
    <div>
      {!isLocked && (
        <div className="mb-3 rounded-xl border bg-card p-3 text-card-foreground shadow-sm">
          <Input type="number" placeholder="Година" value={form.recordHour}
            onChange={(e) => setForm((prev) => ({ ...prev, recordHour: Number(e.target.value) }))} className="mb-1 h-7 pointer-coarse:min-h-11" />
          <div className="grid grid-cols-1 gap-1.5 md:grid-cols-2">
            <SelectField label={'Свідомість'} options={CONSCIOUSNESS} value={form.consciousness} onChange={(v) => set('consciousness', v)} />
            <SelectField label={'Шкіра'} options={SKIN} value={form.skin} onChange={(v) => set('skin', v)} />
            <SelectField label={'Набряки'} options={EDEMA} value={form.edema} onChange={(v) => set('edema', v)} />
            <SelectField label={'Слизові'} options={MUCOSA} value={form.mucousMembranes} onChange={(v) => set('mucousMembranes', v)} />
            <SelectField label={'Периферійний кровообіг'} options={CIRCULATION} value={form.peripheralCirculation} onChange={(v) => set('peripheralCirculation', v)} />
            <SelectField label={'Перистальтика'} options={BOWEL} value={form.bowelSounds} onChange={(v) => set('bowelSounds', v)} />
          </div>
          <Input placeholder={'Загальний стан'} value={form.generalCondition}
            onChange={(e) => setForm((prev) => ({ ...prev, generalCondition: e.target.value }))} className="mb-1 h-7 pointer-coarse:min-h-11" />
          <textarea placeholder={'Примітки'} value={form.additionalNotes}
            onChange={(e) => setForm((prev) => ({ ...prev, additionalNotes: e.target.value }))}
            className="mb-1 h-7 w-full min-h-[2.5rem] md:min-h-[72px] rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring md:text-sm dark:bg-input/30"
            rows={2}
          />
          <div className="flex flex-row gap-1 items-center">
            <Button size="sm" onClick={handleAdd} disabled={saving}>
              {saving ? <Loader2 className="mr-1 size-3.5 animate-spin" /> : null}
              {saving ? 'Зберігається...' : 'Додати'}
            </Button>
          </div>
        </div>
      )}

      {assessments.length === 0 ? (
        <p className="text-xs text-muted-foreground font-mulish">{'Немає оцінок'}</p>
      ) : (
        <div className="flex flex-col gap-[3px]">
          {assessments.map((a) => (
            <div key={a.id} className="flex items-start justify-between gap-1 rounded-xl border bg-card p-2 text-card-foreground shadow-sm">
              <div className="text-xs">
                <p className="font-semibold font-rubik">{a.recordHour}:00</p>
                <p className="text-[11px] text-muted-foreground font-mulish">
                  {[
                    `Свідомість: ${optionLabels[a.consciousness] || a.consciousness}`,
                    `Шкіра: ${optionLabels[a.skin] || a.skin}`,
                    `Набряки: ${optionLabels[a.edema] || a.edema}`,
                  ].join(' · ')}
                  {a.generalCondition ? ` · ${a.generalCondition}` : ''}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
