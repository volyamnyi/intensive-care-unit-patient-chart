import { Grid, TextField, Button, Paper, Typography } from '@mui/material';
import type { HourlyRecordCreateRequest } from '../../types';

interface VitalSignsFormProps {
  values: HourlyRecordCreateRequest;
  onChange: (values: HourlyRecordCreateRequest) => void;
  onSave: () => void;
  saving?: boolean;
  title?: string;
}

const setNum = (prev: HourlyRecordCreateRequest, field: keyof HourlyRecordCreateRequest, val: string) => ({
  ...prev,
  [field]: val === '' ? null : Number(val),
});

const setStr = (prev: HourlyRecordCreateRequest, field: keyof HourlyRecordCreateRequest, val: string) => ({
  ...prev,
  [field]: val,
});

export default function VitalSignsForm({ values, onChange, onSave, saving, title }: VitalSignsFormProps) {
  return (
    <Paper sx={{ p: 2.5, border: '1px solid #2A2A2A', boxShadow: '0 2px 12px rgba(0,0,0,0.2)' }}>
      {title && (
        <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>{title}</Typography>
      )}
      <Grid container spacing={1}>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label="АТ сист (мм.рт.ст)"
            value={values.systolicBP ?? ''}
            onChange={(e) => onChange(setNum(values, 'systolicBP', e.target.value))}
            slotProps={{ htmlInput: { min: 60, max: 300, step: 1 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label="АТ діас (мм.рт.ст)"
            value={values.diastolicBP ?? ''}
            onChange={(e) => onChange(setNum(values, 'diastolicBP', e.target.value))}
            slotProps={{ htmlInput: { min: 30, max: 200, step: 1 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label="ЧСС (в 1 хв)"
            value={values.heartRate ?? ''}
            onChange={(e) => onChange(setNum(values, 'heartRate', e.target.value))}
            slotProps={{ htmlInput: { min: 20, max: 300, step: 1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label="SpO2 (%)"
            value={values.spo2 ?? ''}
            onChange={(e) => onChange(setNum(values, 'spo2', e.target.value))}
            slotProps={{ htmlInput: { min: 0, max: 100, step: 1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label="Темп. тіла (°С)"
            value={values.temperature ?? ''}
            onChange={(e) => onChange(setNum(values, 'temperature', e.target.value))}
            slotProps={{ htmlInput: { min: 30, max: 45, step: 0.1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label="ЦВТ (мм.вод.ст)"
            value={values.cvp ?? ''}
            onChange={(e) => onChange(setNum(values, 'cvp', e.target.value))}
            slotProps={{ htmlInput: { min: 0, max: 50, step: 1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label="ЧД (в 1 хв)"
            value={values.respiratoryRate ?? ''}
            onChange={(e) => onChange(setNum(values, 'respiratoryRate', e.target.value))}
            slotProps={{ htmlInput: { min: 4, max: 80, step: 1 } }} />
        </Grid>
        <Grid size={6}>
          <TextField fullWidth size="small" label="Свідомість"
            value={values.consciousness ?? ''}
            onChange={(e) => onChange(setStr(values, 'consciousness', e.target.value))} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label="etCO2 (мм.рт.ст)"
            value={values.etco2 ?? ''}
            onChange={(e) => onChange(setNum(values, 'etco2', e.target.value))}
            slotProps={{ htmlInput: { min: 0, max: 100, step: 1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label="FiO2 (%)"
            value={values.fio2 ?? ''}
            onChange={(e) => onChange(setNum(values, 'fio2', e.target.value))}
            slotProps={{ htmlInput: { min: 21, max: 100, step: 1 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label="Діурез (мл/год)"
            value={values.urineOutput ?? ''}
            onChange={(e) => onChange(setNum(values, 'urineOutput', e.target.value))}
            slotProps={{ htmlInput: { min: 0, max: 2000, step: 10 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label="Дренаж (мл)"
            value={values.drainOutput ?? ''}
            onChange={(e) => onChange(setNum(values, 'drainOutput', e.target.value))}
            slotProps={{ htmlInput: { min: 0, max: 5000, step: 10 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label="Біль (0-10)"
            value={values.painScore ?? ''}
            onChange={(e) => onChange(setNum(values, 'painScore', e.target.value))}
            slotProps={{ htmlInput: { min: 0, max: 10, step: 1 } }} />
        </Grid>
        <Grid size={12}>
          <TextField fullWidth size="small" multiline minRows={2} label="Нотатки"
            value={values.notes ?? ''}
            onChange={(e) => onChange(setStr(values, 'notes', e.target.value))} />
        </Grid>
      </Grid>
      <Button variant="contained" sx={{ mt: 2 }} onClick={onSave} disabled={saving}>
        {saving ? 'Збереження...' : 'Зберегти показники'}
      </Button>
    </Paper>
  );
}
