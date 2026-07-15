import { useState } from 'react';
import { Grid, TextField, Button, Paper, Typography, useTheme } from '@mui/material';
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

interface FieldRange {
  min: number;
  max: number;
  unit: string;
  label: string;
}

const fieldRanges: Record<string, FieldRange> = {
  temperature: { min: 34, max: 42, unit: '°C', label: 'Температура' },
  heartRate: { min: 0, max: 300, unit: 'уд/хв', label: 'ЧСС' },
  respiratoryRate: { min: 0, max: 60, unit: '/хв', label: 'ЧД' },
  systolicBP: { min: 50, max: 250, unit: 'мм рт.ст.', label: 'АТ сист.' },
  diastolicBP: { min: 30, max: 150, unit: 'мм рт.ст.', label: 'АТ діаст.' },
  spo2: { min: 50, max: 100, unit: '%', label: 'SpO2' },
};

export default function VitalSignsForm({ values, onChange, onSave, saving, title }: VitalSignsFormProps) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const handleBlur = (field: string) => {
    setTouched((prev) => ({ ...prev, [field]: true }));
  };

  const validateField = (field: string, value: number | null | undefined): string | null => {
    if (value === null || value === undefined) return null;
    const range = fieldRanges[field];
    if (!range) return null;
    if (value < range.min || value > range.max) {
      return `${range.label}: значення виходить за межі норми (${range.min}-${range.max}${range.unit})`;
    }
    return null;
  };

  const fieldWarning = (field: string, value: number | null | undefined) => {
    if (!touched[field]) return {};
    const warning = validateField(field, value);
    return {
      error: warning !== null,
      helperText: warning,
      onBlur: () => handleBlur(field),
    };
  };

  const handleSave = () => {
    const allTouched: Record<string, boolean> = {};
    Object.keys(fieldRanges).forEach((f) => { allTouched[f] = true; });
    setTouched((prev) => ({ ...prev, ...allTouched }));
    onSave();
  };

  return (
    <Paper sx={{ p: 2.5, border: `1px solid ${isDark ? '#2A2A2A' : '#E8E6E1'}`, boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 8px rgba(0,0,0,0.04)' }}>
      {title && (
        <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>{title}</Typography>
      )}
      <Grid container spacing={1}>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label="АТ сист (мм.рт.ст)"
            value={values.systolicBP ?? ''}
            onChange={(e) => onChange(setNum(values, 'systolicBP', e.target.value))}
            {...fieldWarning('systolicBP', values.systolicBP)}
            slotProps={{ htmlInput: { min: 60, max: 300, step: 1 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label="АТ діас (мм.рт.ст)"
            value={values.diastolicBP ?? ''}
            onChange={(e) => onChange(setNum(values, 'diastolicBP', e.target.value))}
            {...fieldWarning('diastolicBP', values.diastolicBP)}
            slotProps={{ htmlInput: { min: 30, max: 200, step: 1 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label="ЧСС (в 1 хв)"
            value={values.heartRate ?? ''}
            onChange={(e) => onChange(setNum(values, 'heartRate', e.target.value))}
            {...fieldWarning('heartRate', values.heartRate)}
            slotProps={{ htmlInput: { min: 20, max: 300, step: 1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label="SpO2 (%)"
            value={values.spo2 ?? ''}
            onChange={(e) => onChange(setNum(values, 'spo2', e.target.value))}
            {...fieldWarning('spo2', values.spo2)}
            slotProps={{ htmlInput: { min: 0, max: 100, step: 1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label="Темп. тіла (°С)"
            value={values.temperature ?? ''}
            onChange={(e) => onChange(setNum(values, 'temperature', e.target.value))}
            {...fieldWarning('temperature', values.temperature)}
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
            {...fieldWarning('respiratoryRate', values.respiratoryRate)}
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
      <Button variant="contained" sx={{ mt: 2 }} onClick={handleSave} disabled={saving}>
        {saving ? 'Збереження...' : 'Зберегти показники'}
      </Button>
    </Paper>
  );
}
