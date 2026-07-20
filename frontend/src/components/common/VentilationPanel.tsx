import { useState } from 'react';
import {
  Box, Typography, TextField, Button, MenuItem, Stack, Paper,
} from '@mui/material';
import type { VentilationSettings, VentilationCreateRequest } from '../../types';

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
      setForm({ ...form, fio2: '', peep: '', respiratoryRate: '', tidalVolume: '', plateauPressure: '' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box>
      {!isLocked && (
        <Paper variant="outlined" sx={{ p: 1.5, mb: 1.5 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ flexWrap: 'wrap', alignItems: 'center' }}>
            <TextField size="small" type="number" label={'Година'}
              value={form.recordHour} onChange={(e) => setForm({ ...form, recordHour: Number(e.target.value) })}
              sx={{ width: 110 }} />
            <TextField size="small" select label={'Режим'}
              value={form.mode} onChange={(e) => setForm({ ...form, mode: e.target.value })}
              sx={{ width: 130 }}>
              {MODES.map((m) => <MenuItem key={m} value={m}>{m}</MenuItem>)}
            </TextField>
            <TextField size="small" type="number" label="FiO₂ %" value={form.fio2}
              onChange={(e) => setForm({ ...form, fio2: e.target.value })} sx={{ width: 100 }} />
            <TextField size="small" type="number" label="PEEP" value={form.peep}
              onChange={(e) => setForm({ ...form, peep: e.target.value })} sx={{ width: 100 }} />
            <TextField size="small" type="number" label={'ЧД'} value={form.respiratoryRate}
              onChange={(e) => setForm({ ...form, respiratoryRate: e.target.value })} sx={{ width: 100 }} />
            <TextField size="small" type="number" label={'Vt'} value={form.tidalVolume}
              onChange={(e) => setForm({ ...form, tidalVolume: e.target.value })} sx={{ width: 100 }} />
            <TextField size="small" type="number" label={'Pplat'} value={form.plateauPressure}
              onChange={(e) => setForm({ ...form, plateauPressure: e.target.value })} sx={{ width: 100 }} />
            <Button variant="contained" size="small" onClick={handleAdd} disabled={saving}>{'Додати'}</Button>
          </Stack>
        </Paper>
      )}

      {ventilation.length === 0 ? (
        <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>{'Немає налаштувань вентиляції'}</Typography>
      ) : (
        <Stack spacing={0.75}>
          {ventilation.map((v) => (
            <Paper key={v.id} variant="outlined" sx={{ p: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1 }}>
              <Box sx={{ fontSize: 12 }}>
                <Typography sx={{ fontWeight: 600 }}>{v.recordHour}:00 · {v.mode || '—'}</Typography>
                <Typography sx={{ fontSize: 11, color: 'text.secondary' }}>
                  {[
                    v.fio2 != null ? `FiO₂ ${v.fio2}%` : null,
                    v.peep != null ? `PEEP ${v.peep}` : null,
                    v.respiratoryRate != null ? `ЧД ${v.respiratoryRate}` : null,
                    v.tidalVolume != null ? `Vt ${v.tidalVolume}` : null,
                    v.plateauPressure != null ? `Pplat ${v.plateauPressure}` : null,
                  ].filter(Boolean).join(' · ')}
                </Typography>
              </Box>
            </Paper>
          ))}
        </Stack>
      )}
    </Box>
  );
}
