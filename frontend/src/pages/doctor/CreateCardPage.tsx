import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Paper, Typography, Button, Grid, TextField, Alert, useTheme } from '@mui/material';
import { episodeApi } from '../../api/endpoints';
import PatientSearch from '../../components/common/PatientSearch';
import type { PatientDto } from '../../types';

export default function CreateCardPage() {
  useEffect(() => { document.title = 'ВАІТ — Нова карта'; }, []);
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
      setError('Помилка створення карти');
    }
  };

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto' }}>
      <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary, mb: 3 }}>
        Нова карта інтенсивної терапії
      </Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2, color: theme.palette.text.primary }}>
          Пошук пацієнта
        </Typography>
        <PatientSearch onSelect={setSelectedPatient} />
      </Paper>
      {selectedPatient && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2, color: theme.palette.text.primary }}>
            Дані пацієнта (з МІС)
          </Typography>
          <Grid container spacing={2}>
            <Grid size={6}>
              <TextField fullWidth label="ПІП" value={selectedPatient.fullName} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label="Дата народження" value={selectedPatient.birthDate} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label="Стать" value={selectedPatient.sexCode === 'M' ? 'Чол' : 'Жін'} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label="Зріст (см)" value={selectedPatient.height ?? ''} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label="Маса (кг)" value={selectedPatient.weight ?? ''} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label="Група крові" value={selectedPatient.bloodGroup} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={3}>
              <TextField fullWidth label="Rezus" value={selectedPatient.rhFactor} slotProps={{ input: { readOnly: true } }} />
            </Grid>
            <Grid size={6}>
              <TextField fullWidth label="№ медкарти" value={selectedPatient.externalId1} slotProps={{ input: { readOnly: true } }} />
            </Grid>
          </Grid>
        </Paper>
      )}
      {selectedPatient && (
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button variant="contained" size="large" onClick={handleCreate}>
            Створити карту
          </Button>
          <Button variant="outlined" size="large" onClick={() => navigate('/doctor')}>
            Скасувати
          </Button>
        </Box>
      )}
    </Box>
  );
}
