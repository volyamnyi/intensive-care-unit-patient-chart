import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box, Typography, TextField, Button, MenuItem, Stack, Paper,
} from '@mui/material';
import type { PatientStateAssessment, PatientStateCreateRequest } from '../../types';

const CONSCIOUSNESS = ['alert', 'drowsy', 'sopor', 'coma', 'sedation'];
const SKIN = ['normal', 'dry', 'cyanotic', 'jaundiced', 'pale', 'rash', 'marbling'];
const EDEMA = ['none', 'mild', 'moderate', 'severe'];
const MUCOSA = ['normal', 'dry', 'moist', 'pale', 'cyanotic'];
const CIRCULATION = ['normal', 'weak', 'bound', 'cold'];
const BOWEL = ['normal', 'hypoactive', 'absent', 'hyperactive'];

const tkey = (ns: string, v: string) => `${ns}.${v}`;

interface PatientStatePanelProps {
  clinicalDayId: string;
  assessments: PatientStateAssessment[];
  isLocked: boolean;
  onCreate: (data: PatientStateCreateRequest) => Promise<void>;
}

export default function PatientStatePanel({
  assessments, isLocked, onCreate,
}: PatientStatePanelProps) {
  const { t } = useTranslation();
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

  const set = (k: string, v: string) => setForm({ ...form, [k]: v });

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
      setForm({ ...form, generalCondition: '', additionalNotes: '' });
    } finally {
      setSaving(false);
    }
  };

  const SelectField = ({ label, options, value, onChange }: { label: string; options: string[]; value: string; onChange: (v: string) => void }) => (
    <TextField fullWidth size="small" select label={label} value={value} onChange={(e) => onChange(e.target.value)} sx={{ mb: 1 }}>
      {options.map((opt) => (
        <MenuItem key={opt} value={opt}>{t(tkey('patientState', opt))}</MenuItem>
      ))}
    </TextField>
  );

  return (
    <Box>
      {!isLocked && (
        <Paper variant="outlined" sx={{ p: 1.5, mb: 1.5 }}>
          <TextField fullWidth size="small" type="number" label={t('patientState.hourLabel')} value={form.recordHour}
            onChange={(e) => set('recordHour', e.target.value)} sx={{ mb: 1 }} />
          <SelectField label={t('patientState.consciousnessLabel')} options={CONSCIOUSNESS} value={form.consciousness} onChange={(v) => set('consciousness', v)} />
          <SelectField label={t('patientState.skinLabel')} options={SKIN} value={form.skin} onChange={(v) => set('skin', v)} />
          <SelectField label={t('patientState.edemaLabel')} options={EDEMA} value={form.edema} onChange={(v) => set('edema', v)} />
          <SelectField label={t('patientState.mucousLabel')} options={MUCOSA} value={form.mucousMembranes} onChange={(v) => set('mucousMembranes', v)} />
          <SelectField label={t('patientState.circulationLabel')} options={CIRCULATION} value={form.peripheralCirculation} onChange={(v) => set('peripheralCirculation', v)} />
          <SelectField label={t('patientState.bowelLabel')} options={BOWEL} value={form.bowelSounds} onChange={(v) => set('bowelSounds', v)} />
          <TextField fullWidth size="small" label={t('patientState.generalLabel')} value={form.generalCondition}
            onChange={(e) => set('generalCondition', e.target.value)} sx={{ mb: 1 }} />
          <TextField fullWidth size="small" label={t('patientState.notesLabel')} value={form.additionalNotes}
            onChange={(e) => set('additionalNotes', e.target.value)} sx={{ mb: 1 }} multiline minRows={2} />
          <Button variant="contained" size="small" onClick={handleAdd} disabled={saving}>{t('patientState.addButton')}</Button>
        </Paper>
      )}

      {assessments.length === 0 ? (
        <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>{t('patientState.empty')}</Typography>
      ) : (
        <Stack spacing={0.75}>
          {assessments.map((a) => (
            <Paper key={a.id} variant="outlined" sx={{ p: 1, display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 1 }}>
              <Box sx={{ fontSize: 12 }}>
                <Typography sx={{ fontWeight: 600 }}>{a.recordHour}:00</Typography>
                <Typography sx={{ fontSize: 11, color: 'text.secondary' }}>
                  {[
                    `${t('patientState.consciousnessLabel')}: ${t(tkey('patientState', a.consciousness))}`,
                    `${t('patientState.skinLabel')}: ${t(tkey('patientState', a.skin))}`,
                    `${t('patientState.edemaLabel')}: ${t(tkey('patientState', a.edema))}`,
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
