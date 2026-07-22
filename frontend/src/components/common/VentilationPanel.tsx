import { useState, useRef, useCallback } from 'react';
import {
  Box, Typography, TextField, Button, MenuItem, Stack, Paper, CircularProgress,
} from '@mui/material';
import type { VentilationSettings, VentilationCreateRequest } from '../../types';
import { useAutoSave } from '../../hooks/useAutoSave';

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
  const formRef = useRef(form);
  formRef.current = form;

  const num = (v: string) => (v.trim() === '' ? undefined : Number(v));

  const doCreate = useCallback(async () => {
    const f = formRef.current;
    if (isLocked) return;
    try {
      setSaving(true);
      await onCreate({
        recordHour: Number(f.recordHour),
        mode: f.mode,
        fio2: num(f.fio2),
        peep: num(f.peep),
        respiratoryRate: num(f.respiratoryRate),
        tidalVolume: num(f.tidalVolume),
        plateauPressure: num(f.plateauPressure),
      });
      setForm((prev) => ({ ...prev, fio2: '', peep: '', respiratoryRate: '', tidalVolume: '', plateauPressure: '' }));
    } finally {
      setSaving(false);
    }
  }, [isLocked, onCreate]);

  const { status: autoSaveStatus, markDirty, saveNow } = useAutoSave({
    onSave: doCreate,
    delay: 10000,
    enabled: !isLocked,
  });

  const handleAdd = async () => {
    await saveNow();
  };

  const setFormAndDirty = (upd: Partial<typeof form>) => {
    setForm((prev) => ({ ...prev, ...upd }));
    markDirty();
  };

  return (
    <Box>
      {!isLocked && (
        <Paper variant="outlined" sx={{ p: 1.5, mb: 1.5 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ flexWrap: 'wrap', alignItems: 'center' }}>
            <TextField size="small" type="number" label={'Година'}
              value={form.recordHour} onChange={(e) => setFormAndDirty({ recordHour: Number(e.target.value) })}
              sx={{ width: { xs: '100%', sm: 110 } }} />
            <TextField size="small" select label={'Режим'}
              value={form.mode} onChange={(e) => setFormAndDirty({ mode: e.target.value })}
              sx={{ width: { xs: '100%', sm: 130 } }}>
              {MODES.map((m) => <MenuItem key={m} value={m}>{m}</MenuItem>)}
            </TextField>
            <TextField size="small" type="number" label="FiO₂ %" value={form.fio2}
              onChange={(e) => setFormAndDirty({ fio2: e.target.value })} sx={{ width: { xs: 'calc(50% - 8px)', sm: 100 } }} />
            <TextField size="small" type="number" label="PEEP" value={form.peep}
              onChange={(e) => setFormAndDirty({ peep: e.target.value })} sx={{ width: { xs: 'calc(50% - 8px)', sm: 100 } }} />
            <TextField size="small" type="number" label={'ЧД'} value={form.respiratoryRate}
              onChange={(e) => setFormAndDirty({ respiratoryRate: e.target.value })} sx={{ width: { xs: 'calc(50% - 8px)', sm: 100 } }} />
            <TextField size="small" type="number" label={'Vt'} value={form.tidalVolume}
              onChange={(e) => setFormAndDirty({ tidalVolume: e.target.value })} sx={{ width: { xs: 'calc(50% - 8px)', sm: 100 } }} />
            <TextField size="small" type="number" label={'Pplat'} value={form.plateauPressure}
              onChange={(e) => setFormAndDirty({ plateauPressure: e.target.value })} sx={{ width: { xs: 'calc(50% - 8px)', sm: 100 } }} />
            <Stack spacing={0.5} alignItems="center">
              <Button variant="contained" size="small" onClick={handleAdd} disabled={saving} sx={{ width: { xs: '100%', sm: 'auto' } }}>{'Додати'}</Button>
              {autoSaveStatus === 'saving' && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <CircularProgress size={8} />
                  <Typography variant="caption" color="text.secondary">{'Зберігається...'}</Typography>
                </Box>
              )}
              {autoSaveStatus === 'saved' && (
                <Typography variant="caption" color="success.main">{'Збережено'}</Typography>
              )}
              {autoSaveStatus === 'error' && (
                <Typography variant="caption" color="error">{'Помилка'}</Typography>
              )}
            </Stack>
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
