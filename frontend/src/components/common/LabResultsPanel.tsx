import { useState, useRef, useCallback } from 'react';
import {
  Box, Typography, TextField, Button, MenuItem, Chip, Stack,
  Paper, CircularProgress,
} from '@mui/material';
import type { LabResult, LabResultCreateRequest } from '../../types';
import { useAutoSave } from '../../hooks/useAutoSave';

const PREDEFINED_TESTS: { code: string; name: string; unit: string; min: number | null; max: number | null }[] = [
  { code: 'Hb', name: 'Hemoglobin', unit: 'g/dL', min: 12, max: 16 },
  { code: 'Ht', name: 'Hematocrit', unit: '%', min: 36, max: 48 },
  { code: 'WBC', name: 'White blood cells', unit: '10⁹/L', min: 4, max: 9 },
  { code: 'Na', name: 'Sodium', unit: 'mmol/L', min: 135, max: 145 },
  { code: 'K', name: 'Potassium', unit: 'mmol/L', min: 3.5, max: 5.1 },
  { code: 'pH', name: 'pH', unit: '', min: 7.35, max: 7.45 },
  { code: 'pCO2', name: 'pCO₂', unit: 'mmHg', min: 35, max: 45 },
  { code: 'pO2', name: 'pO₂', unit: 'mmHg', min: 80, max: 100 },
  { code: 'HCO3', name: 'Bicarbonate', unit: 'mmol/L', min: 22, max: 26 },
  { code: 'BE', name: 'Base excess', unit: 'mmol/L', min: -2, max: 2 },
  { code: 'Lactate', name: 'Lactate', unit: 'mmol/L', min: 0.5, max: 2 },
  { code: 'Glucose', name: 'Glucose', unit: 'mmol/L', min: 3.9, max: 6.1 },
  { code: 'Ca', name: 'Calcium', unit: 'mmol/L', min: 2.1, max: 2.6 },
  { code: 'Cl', name: 'Chloride', unit: 'mmol/L', min: 98, max: 107 },
  { code: 'Cr', name: 'Creatinine', unit: 'μmol/L', min: 62, max: 106 },
  { code: 'Urea', name: 'Urea', unit: 'mmol/L', min: 2.5, max: 8.3 },
  { code: 'ALT', name: 'ALT', unit: 'U/L', min: 0, max: 41 },
  { code: 'AST', name: 'AST', unit: 'U/L', min: 0, max: 40 },
  { code: 'Bilirubin', name: 'Bilirubin', unit: 'μmol/L', min: 3.4, max: 20.5 },
  { code: 'INR', name: 'INR', unit: '', min: 0.8, max: 1.2 },
  { code: 'aPTT', name: 'aPTT', unit: 's', min: 25, max: 36 },
  { code: 'CRP', name: 'CRP', unit: 'mg/L', min: 0, max: 5 },
  { code: 'Procalcitonin', name: 'Procalcitonin', unit: 'ng/mL', min: 0, max: 0.5 },
];

interface LabResultsPanelProps {
  clinicalDayId: string;
  labs: LabResult[];
  isLocked: boolean;
  onCreate: (data: LabResultCreateRequest) => Promise<void>;
}

export default function LabResultsPanel({
  labs, isLocked, onCreate,
}: LabResultsPanelProps) {
  const [selectedCode, setSelectedCode] = useState('');
  const [result, setResult] = useState('');
  const [saving, setSaving] = useState(false);
  const selectedCodeRef = useRef('');
  const resultRef = useRef('');

  const selected = PREDEFINED_TESTS.find((x) => x.code === selectedCode);

  const doCreate = useCallback(async () => {
    const code = selectedCodeRef.current;
    const resVal = resultRef.current.trim();
    const sel = PREDEFINED_TESTS.find((x) => x.code === code);
    if (!sel || !resVal || isLocked) return;
    try {
      setSaving(true);
      await onCreate({
        testCode: sel.code,
        testName: sel.name,
        result: resVal,
        unit: sel.unit,
        referenceMin: sel.min,
        referenceMax: sel.max,
        measuredAt: new Date().toISOString(),
      });
      setSelectedCode('');
      setResult('');
      selectedCodeRef.current = '';
      resultRef.current = '';
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

  const handleCodeChange = (v: string) => {
    setSelectedCode(v);
    selectedCodeRef.current = v;
    markDirty();
  };

  const handleResultChange = (v: string) => {
    setResult(v);
    resultRef.current = v;
    markDirty();
  };

  return (
    <Box>
      {!isLocked && (
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mb: 1.5, alignItems: 'flex-start' }}>
          <TextField
            select size="small" label={'Тест'}
            value={selectedCode} onChange={(e) => handleCodeChange(e.target.value)}
            sx={{ minWidth: 200 }}
          >
            {PREDEFINED_TESTS.map((x) => (
              <MenuItem key={x.code} value={x.code}>{x.name} ({x.code})</MenuItem>
            ))}
          </TextField>
          {selected && (
            <Typography sx={{ fontSize: 11, color: 'text.secondary', alignSelf: 'center' }}>
              {`Норма: ${selected.min ?? '—'}–${selected.max ?? '—'} ${selected.unit}`}
            </Typography>
          )}
          <TextField
            size="small" type="number" label={'Результат'}
            value={result} onChange={(e) => handleResultChange(e.target.value)}
            sx={{ width: 130 }}
          />
          <Stack spacing={0.5} alignItems="center">
            <Button variant="contained" size="small" onClick={handleAdd} disabled={saving || !selected || result.trim() === ''} sx={{ mt: 0.5 }}>
              {'Додати'}
            </Button>
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
      )}

      {labs.length === 0 ? (
        <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>{'Немає лабораторних досліджень'}</Typography>
      ) : (
        <Stack spacing={0.75}>
          {labs.map((l) => (
            <Paper
              key={l.id}
              variant="outlined"
              sx={{ p: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, borderColor: l.isAbnormal ? 'error.main' : 'divider' }}
            >
              <Box sx={{ minWidth: 0 }}>
                <Typography sx={{ fontSize: 12, fontWeight: 600 }}>
                  {l.testName}
                  {l.isAbnormal && <Chip label={'Аномалія'} color="error" size="small" sx={{ ml: 1, height: 18, fontSize: 9, fontWeight: 700 }} />}
                </Typography>
                <Typography sx={{ fontSize: 11, color: 'text.secondary' }}>
                  {`${l.result} ${l.unit}${(l.referenceMin ?? l.referenceMax) != null ? ` (${l.referenceMin ?? '—'}–${l.referenceMax ?? '—'} ${l.unit})` : ''}`}
                </Typography>
              </Box>
            </Paper>
          ))}
        </Stack>
      )}
    </Box>
  );
}
