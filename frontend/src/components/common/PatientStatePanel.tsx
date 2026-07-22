import { useState, useRef, useCallback } from 'react';
import {
  Box, Typography, TextField, Button, MenuItem, Stack, Paper, CircularProgress,
} from '@mui/material';
import type { PatientStateAssessment, PatientStateCreateRequest } from '../../types';
import { useAutoSave } from '../../hooks/useAutoSave';

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
  const formRef = useRef(form);
  formRef.current = form;

  const doCreate = useCallback(async () => {
    const f = formRef.current;
    if (isLocked) return;
    try {
      setSaving(true);
      await onCreate({
        recordHour: Number(f.recordHour),
        consciousness: f.consciousness,
        skin: f.skin,
        edema: f.edema,
        mucousMembranes: f.mucousMembranes,
        peripheralCirculation: f.peripheralCirculation,
        bowelSounds: f.bowelSounds,
        generalCondition: f.generalCondition.trim() || undefined,
        additionalNotes: f.additionalNotes.trim() || undefined,
      });
      setForm((prev) => ({ ...prev, generalCondition: '', additionalNotes: '' }));
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

  const set = (k: string, v: string) => {
    setForm((prev) => ({ ...prev, [k]: v }));
    markDirty();
  };

  const setAndDirty = (e: React.ChangeEvent<HTMLInputElement>, key: string) => {
    setForm((prev) => ({ ...prev, [key]: e.target.value }));
    markDirty();
  };

  const SelectField = ({ label, options, value, onChange }: { label: string; options: string[]; value: string; onChange: (v: string) => void }) => (
    <TextField fullWidth size="small" select label={label} value={value} onChange={(e) => { onChange(e.target.value); markDirty(); }} sx={{ mb: 1 }}>
      {options.map((opt) => (
        <MenuItem key={opt} value={opt}>{optionLabels[opt] || opt}</MenuItem>
      ))}
    </TextField>
  );

  return (
    <Box>
      {!isLocked && (
        <Paper variant="outlined" sx={{ p: 1.5, mb: 1.5 }}>
          <TextField fullWidth size="small" type="number" label={'Година'} value={form.recordHour}
            onChange={(e) => setAndDirty(e as React.ChangeEvent<HTMLInputElement>, 'recordHour')} sx={{ mb: 1 }} />
          <SelectField label={'Свідомість'} options={CONSCIOUSNESS} value={form.consciousness} onChange={(v) => set('consciousness', v)} />
          <SelectField label={'Шкіра'} options={SKIN} value={form.skin} onChange={(v) => set('skin', v)} />
          <SelectField label={'Набряки'} options={EDEMA} value={form.edema} onChange={(v) => set('edema', v)} />
          <SelectField label={'Слизові'} options={MUCOSA} value={form.mucousMembranes} onChange={(v) => set('mucousMembranes', v)} />
          <SelectField label={'Периферійний кровообіг'} options={CIRCULATION} value={form.peripheralCirculation} onChange={(v) => set('peripheralCirculation', v)} />
          <SelectField label={'Перистальтика'} options={BOWEL} value={form.bowelSounds} onChange={(v) => set('bowelSounds', v)} />
          <TextField fullWidth size="small" label={'Загальний стан'} value={form.generalCondition}
            onChange={(e) => setAndDirty(e as React.ChangeEvent<HTMLInputElement>, 'generalCondition')} sx={{ mb: 1 }} />
          <TextField fullWidth size="small" label={'Примітки'} value={form.additionalNotes}
            onChange={(e) => setAndDirty(e as React.ChangeEvent<HTMLInputElement>, 'additionalNotes')} sx={{ mb: 1 }} multiline minRows={2} />
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Button variant="contained" size="small" onClick={handleAdd} disabled={saving}>{'Додати'}</Button>
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
        </Paper>
      )}

      {assessments.length === 0 ? (
        <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>{'Немає оцінок'}</Typography>
      ) : (
        <Stack spacing={0.75}>
          {assessments.map((a) => (
            <Paper key={a.id} variant="outlined" sx={{ p: 1, display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 1 }}>
              <Box sx={{ fontSize: 12 }}>
                <Typography sx={{ fontWeight: 600 }}>{a.recordHour}:00</Typography>
                <Typography sx={{ fontSize: 11, color: 'text.secondary' }}>
                  {[
                    `Свідомість: ${optionLabels[a.consciousness] || a.consciousness}`,
                    `Шкіра: ${optionLabels[a.skin] || a.skin}`,
                    `Набряки: ${optionLabels[a.edema] || a.edema}`,
                  ].join(' · ')}
                  {a.generalCondition ? ` · ${a.generalCondition}` : ''}
                </Typography>
              </Box>
            </Paper>
          ))}
        </Stack>
      )}
    </Box>
  );
}
