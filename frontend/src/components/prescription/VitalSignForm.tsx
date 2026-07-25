import { useState } from 'react';
import { Grid, TextField, Button, Paper, Typography } from '@mui/material';
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
    <Paper sx={{ p: 2 }}>
      <Typography variant="subtitle1" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>
        Життєві показники
      </Typography>
      {latest && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
          {`Останні: Темп ${latest.temperature ?? '-'}, АТ ${latest.systolicBp ?? '-'} / ${latest.diastolicBp ?? '-'}, SpO₂ ${latest.spo2 ?? '-'}%, Пульс ${latest.pulse ?? '-'}, Біль ${latest.painScore ?? '-'}`}
        </Typography>
      )}
      <Grid container spacing={1.5}>
        <Grid size={{ xs: 6, sm: 3 }}>
          <TextField
            fullWidth
            size="small"
            type="number"
            label="Температура (°C)"
            value={values.temperature ?? ''}
            onChange={(e) => setNum('temperature', e.target.value)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 34, max: 42, step: 0.1 } }}
          />
        </Grid>
        <Grid size={{ xs: 6, sm: 3 }}>
          <TextField
            fullWidth
            size="small"
            type="number"
            label="Сист. АТ"
            value={values.systolicBp ?? ''}
            onChange={(e) => setNum('systolicBp', e.target.value)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 50, max: 250, step: 1 } }}
          />
        </Grid>
        <Grid size={{ xs: 6, sm: 3 }}>
          <TextField
            fullWidth
            size="small"
            type="number"
            label="Діаст. АТ"
            value={values.diastolicBp ?? ''}
            onChange={(e) => setNum('diastolicBp', e.target.value)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 30, max: 150, step: 1 } }}
          />
        </Grid>
        <Grid size={{ xs: 6, sm: 3 }}>
          <TextField
            fullWidth
            size="small"
            type="number"
            label="SpO₂ (%)"
            value={values.spo2 ?? ''}
            onChange={(e) => setNum('spo2', e.target.value)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 50, max: 100, step: 1 } }}
          />
        </Grid>
        <Grid size={{ xs: 6, sm: 3 }}>
          <TextField
            fullWidth
            size="small"
            type="number"
            label="Пульс"
            value={values.pulse ?? ''}
            onChange={(e) => setNum('pulse', e.target.value)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 0, max: 300, step: 1 } }}
          />
        </Grid>
        <Grid size={{ xs: 6, sm: 3 }}>
          <TextField
            fullWidth
            size="small"
            type="number"
            label="Біль (0-10)"
            value={values.painScore ?? ''}
            onChange={(e) => setNum('painScore', e.target.value)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 0, max: 10, step: 1 } }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            size="small"
            label="Стул"
            value={values.stool ?? ''}
            onChange={(e) => setValues((prev) => ({ ...prev, stool: e.target.value }))}
            disabled={disabled}
          />
        </Grid>
      </Grid>
      <Button variant="contained" sx={{ mt: 2 }} disabled={disabled || saving} onClick={handleSubmit}>
        {saving ? 'Збереження...' : 'Зберегти'}
      </Button>
    </Paper>
  );
}
