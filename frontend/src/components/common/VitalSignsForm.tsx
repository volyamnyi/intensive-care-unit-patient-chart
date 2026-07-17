import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Grid, TextField, Button, Paper, Typography, useTheme } from '@mui/material';
import type { HourlyRecordCreateRequest } from '../../types';
import { CLINICAL_RANGES } from '../../constants/clinicalRanges';

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
  const { t } = useTranslation();
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
      return t('vitalSigns.validationWarning', { label: range.label, min: range.min, max: range.max, unit: range.unit });
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
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.systolicBP')}
            value={values.systolicBP ?? ''}
            onChange={(e) => onChange(setNum(values, 'systolicBP', e.target.value))}
            {...fieldWarning('systolicBP', values.systolicBP)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: CLINICAL_RANGES.systolicBP.min, max: CLINICAL_RANGES.systolicBP.max, step: 1 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.diastolicBP')}
            value={values.diastolicBP ?? ''}
            onChange={(e) => onChange(setNum(values, 'diastolicBP', e.target.value))}
            {...fieldWarning('diastolicBP', values.diastolicBP)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: CLINICAL_RANGES.diastolicBP.min, max: CLINICAL_RANGES.diastolicBP.max, step: 1 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.heartRate')}
            value={values.heartRate ?? ''}
            onChange={(e) => onChange(setNum(values, 'heartRate', e.target.value))}
            {...fieldWarning('heartRate', values.heartRate)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: CLINICAL_RANGES.heartRate.min, max: CLINICAL_RANGES.heartRate.max, step: 1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.spo2')}
            value={values.spo2 ?? ''}
            onChange={(e) => onChange(setNum(values, 'spo2', e.target.value))}
            {...fieldWarning('spo2', values.spo2)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: CLINICAL_RANGES.spo2.min, max: CLINICAL_RANGES.spo2.max, step: 1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.temperature')}
            value={values.temperature ?? ''}
            onChange={(e) => onChange(setNum(values, 'temperature', e.target.value))}
            {...fieldWarning('temperature', values.temperature)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: CLINICAL_RANGES.temperature.min, max: CLINICAL_RANGES.temperature.max, step: 0.1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.cvp')}
            value={values.cvp ?? ''}
            onChange={(e) => onChange(setNum(values, 'cvp', e.target.value))}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 0, max: 50, step: 1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.respiratoryRate')}
            value={values.respiratoryRate ?? ''}
            onChange={(e) => onChange(setNum(values, 'respiratoryRate', e.target.value))}
            {...fieldWarning('respiratoryRate', values.respiratoryRate)}
            disabled={disabled}
            slotProps={{ htmlInput: { min: CLINICAL_RANGES.respiratoryRate.min, max: CLINICAL_RANGES.respiratoryRate.max, step: 1 } }} />
        </Grid>
        <Grid size={6}>
          <TextField fullWidth size="small" label={t('vitalSigns.consciousness')}
            value={values.consciousness ?? ''}
            onChange={(e) => onChange(setStr(values, 'consciousness', e.target.value))}
            disabled={disabled} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.etco2')}
            value={values.etco2 ?? ''}
            onChange={(e) => onChange(setNum(values, 'etco2', e.target.value))}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 0, max: 100, step: 1 } }} />
        </Grid>
        <Grid size={3}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.fio2')}
            value={values.fio2 ?? ''}
            onChange={(e) => onChange(setNum(values, 'fio2', e.target.value))}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 21, max: 100, step: 1 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.urineOutput')}
            value={values.urineOutput ?? ''}
            onChange={(e) => onChange(setNum(values, 'urineOutput', e.target.value))}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 0, max: 2000, step: 10 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.drainOutput')}
            value={values.drainOutput ?? ''}
            onChange={(e) => onChange(setNum(values, 'drainOutput', e.target.value))}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 0, max: 5000, step: 10 } }} />
        </Grid>
        <Grid size={4}>
          <TextField fullWidth size="small" type="number" label={t('vitalSigns.painScore')}
            value={values.painScore ?? ''}
            onChange={(e) => onChange(setNum(values, 'painScore', e.target.value))}
            disabled={disabled}
            slotProps={{ htmlInput: { min: 0, max: 10, step: 1 } }} />
        </Grid>
        <Grid size={12}>
          <TextField fullWidth size="small" multiline minRows={2} label={t('vitalSigns.notes')}
            value={values.notes ?? ''}
            onChange={(e) => onChange(setStr(values, 'notes', e.target.value))}
            disabled={disabled} />
        </Grid>
      </Grid>
      <Button variant="contained" sx={{ mt: 2 }} onClick={handleSave} disabled={disabled || saving}>
        {saving ? t('vitalSigns.savingButton') : t('vitalSigns.saveButton')}
      </Button>
    </Paper>
  );
}
