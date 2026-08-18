import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Loader2 } from 'lucide-react';
import type { VentilationSettings, VentilationCreateRequest } from '../../types/icu';

const MODES = ['CMV', 'SIMV', 'PSV', 'BiPAP', 'CPAP', 'APRV', 'PCV', 'VCV'];

interface VentilationPanelProps {
  clinicalDayId: string;
  ventilation: VentilationSettings[];
  isLocked: boolean;
  onCreate: (data: VentilationCreateRequest) => Promise<void>;
}

export default function VentilationPanel({
  ventilation, isLocked, onCreate,
}: VentilationPanelProps) {
  const [form, setForm] = useState({
    recordHour: new Date().getHours(),
    mode: 'CMV',
    fio2: '',
    peep: '',
    respiratoryRate: '',
    tidalVolume: '',
    plateauPressure: '',
  });
  const [saving, setSaving] = useState(false);

  const num = (v: string) => (v.trim() === '' ? undefined : Number(v));

  const handleAdd = async () => {
    if (isLocked) return;
    try {
      setSaving(true);
      await onCreate({
        recordHour: Number(form.recordHour),
        mode: form.mode,
        fio2: num(form.fio2),
        peep: num(form.peep),
        respiratoryRate: num(form.respiratoryRate),
        tidalVolume: num(form.tidalVolume),
        plateauPressure: num(form.plateauPressure),
      });
      setForm((prev) => ({ ...prev, fio2: '', peep: '', respiratoryRate: '', tidalVolume: '', plateauPressure: '' }));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      {!isLocked && (
        <div className="mb-3 rounded-xl border bg-card p-3 text-card-foreground shadow-sm">
          <div className="flex flex-col flex-wrap items-center gap-2 sm:flex-row">
            <Input
              type="number" placeholder="Година"
              value={form.recordHour} onChange={(e) => setForm((prev) => ({ ...prev, recordHour: Number(e.target.value) }))}
              className="h-7 w-full sm:w-[110px]"
            />
            <Select value={form.mode} onValueChange={(v: string | null) => setForm((prev) => ({ ...prev, mode: v ?? prev.mode }))}>
              <SelectTrigger aria-label="Режим" className="h-7 w-full sm:w-[130px]">
                <SelectValue placeholder="Режим" />
              </SelectTrigger>
              <SelectContent>
                {MODES.map((m) => <SelectItem key={m} value={m}>{m}</SelectItem>)}
              </SelectContent>
            </Select>
            <Input type="number" placeholder="FiO₂ %" value={form.fio2}
              onChange={(e) => setForm((prev) => ({ ...prev, fio2: e.target.value }))} className="h-7 w-[calc(50%-8px)] sm:w-[100px]" />
            <Input type="number" placeholder="PEEP" value={form.peep}
              onChange={(e) => setForm((prev) => ({ ...prev, peep: e.target.value }))} className="h-7 w-[calc(50%-8px)] sm:w-[100px]" />
            <Input type="number" placeholder="ЧД" value={form.respiratoryRate}
              onChange={(e) => setForm((prev) => ({ ...prev, respiratoryRate: e.target.value }))} className="h-7 w-[calc(50%-8px)] sm:w-[100px]" />
            <Input type="number" placeholder="Vt" value={form.tidalVolume}
              onChange={(e) => setForm((prev) => ({ ...prev, tidalVolume: e.target.value }))} className="h-7 w-[calc(50%-8px)] sm:w-[100px]" />
            <Input type="number" placeholder="Pplat" value={form.plateauPressure}
              onChange={(e) => setForm((prev) => ({ ...prev, plateauPressure: e.target.value }))} className="h-7 w-[calc(50%-8px)] sm:w-[100px]" />
            <div className="flex items-center">
              <Button size="sm" onClick={handleAdd} disabled={saving} className="w-full sm:w-auto">
                {saving ? <Loader2 className="mr-1 size-3.5 animate-spin" /> : null}
                {saving ? 'Зберігається...' : 'Додати'}
              </Button>
            </div>
          </div>
        </div>
      )}

      {ventilation.length === 0 ? (
        <p className="text-xs text-muted-foreground font-mulish">{'Немає налаштувань вентиляції'}</p>
      ) : (
        <div className="flex flex-col gap-[3px]">
          {ventilation.map((v) => (
            <div key={v.id} className="flex items-center justify-between gap-1 rounded-xl border bg-card p-2 text-card-foreground shadow-sm">
              <div className="text-xs">
                <p className="font-semibold font-rubik">{v.recordHour}:00 · {v.mode || '—'}</p>
                <p className="text-[11px] text-muted-foreground font-mulish">
                  {[
                    v.fio2 != null ? `FiO₂ ${v.fio2}%` : null,
                    v.peep != null ? `PEEP ${v.peep}` : null,
                    v.respiratoryRate != null ? `ЧД ${v.respiratoryRate}` : null,
                    v.tidalVolume != null ? `Vt ${v.tidalVolume}` : null,
                    v.plateauPressure != null ? `Pplat ${v.plateauPressure}` : null,
                  ].filter(Boolean).join(' · ')}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
