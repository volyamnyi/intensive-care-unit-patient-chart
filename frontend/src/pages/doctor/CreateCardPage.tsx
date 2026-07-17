import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Paper, Typography, Button, Grid, TextField, Alert, useTheme } from '@mui/material';
import { episodeApi } from '../../api/endpoints';
import PatientSearch from '../../components/common/PatientSearch';
import type { PatientDto } from '../../types';
import { useTranslation } from 'react-i18next';

export default function CreateCardPage() {
  const { t } = useTranslation();
  useEffect(() => { document.title = t('doctor.createCard.title'); }, []);
  const navigate = useNavigate();
  const theme = useTheme();
  const [selectedPatient, setSelectedPatient] = useState<PatientDto | null>(null);
  const [error, setError] = useState('');

  const handleCreate = async () => {
    if (!selectedPatient) return;
    try {
      const res = await episodeApi.create({
        patientId: selectedPatient.id,
        admissionDate: new Date().toISOString(),
      });
      navigate('/doctor/episode/' + res.data.id);
    } catch {
      setError(t('doctor.createCard.error'));
    }
  };

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto' }}>
      <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary, mb: 3 }}>
        {t('doctor.createCard.heading')}
      </Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2, color: theme.palette.text.primary }}>
          {t('doctor.createCard.patientSearch')}
        </Typography>
        <PatientSearch onSelect={setSelectedPatient} />
      </Paper>
      {selectedPatient && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2, color: theme.palette.text.primary }}>
            {t('doctor.createCard.patientData')}
          </Typography>
          <Grid container spacing={2}>
            <Grid size={6}>
              <TextField fullWidth label={t('doctor.createCard.fullName')} value={selectedPatient.fullName} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label={t('doctor.createCard.birthDate')} value={selectedPatient.birthDate} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label={t('doctor.createCard.sex')} value={selectedPatient.sexCode === 'M' ? t('doctor.createCard.male') : t('doctor.createCard.female')} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label={t('doctor.createCard.height')} value={selectedPatient.height ?? ''} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label={t('doctor.createCard.weight')} value={selectedPatient.weight ?? ''} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label={t('doctor.createCard.bloodGroup')} value={selectedPatient.bloodGroup} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label={t('doctor.createCard.rhFactor')} value={selectedPatient.rhFactor} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={6}>
              <TextField fullWidth label={t('doctor.createCard.medicalCardNumber')} value={selectedPatient.externalId1} slotProps={{ input: { readOnly: true } }} />
            </Grid>
          </Grid>
        </Paper>
      )}
      {selectedPatient && (
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button variant="contained" size="large" onClick={handleCreate}>
            {t('doctor.createCard.createButton')}
          </Button>
          <Button variant="outlined" size="large" onClick={() => navigate('/doctor')}>
            {t('doctor.createCard.cancelButton')}
          </Button>
        </Box>
      )}
    </Box>
  );
}
