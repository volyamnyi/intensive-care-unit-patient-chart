import { useState, useRef, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, Button, Autocomplete, Grid, Alert, CircularProgress
} from '@mui/material';
import { patientApi, icuCardApi } from '../../api/endpoints';
import type { Patient } from '../../types';

export default function CreateCardPage() {
  useEffect(() => { document.title = 'ВАІТ — Нова карта'; }, []);
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [patients, setPatients] = useState<Patient[]>([]);
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [loading, setLoading] = useState(false);
  const [diagnosis, setDiagnosis] = useState('');
  const [apacheII, setApacheII] = useState('');
  const [sofa, setSofa] = useState('');
  const [error, setError] = useState('');

  const debounceRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const abortRef = useRef<AbortController>(undefined);

  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
      if (abortRef.current) abortRef.current.abort();
    };
  }, []);

  const performSearch = useCallback(async (query: string) => {
    if (abortRef.current) abortRef.current.abort();

    if (query.length < 2) {
      setPatients([]);
      setError('');
      return;
    }

    const controller = new AbortController();
    abortRef.current = controller;

    setLoading(true);
    setError('');
    try {
      const res = await patientApi.search(query, undefined, undefined, controller.signal);
      setPatients(res.data);
    } catch (err: unknown) {
      if (err instanceof Error && err.name === 'CanceledError') return;
      setError('Помилка пошуку пацієнтів');
    } finally {
      setLoading(false);
    }
  }, []);

  const handleInputChange = (_: unknown, value: string) => {
    setSearch(value);
    if (selectedPatient) setSelectedPatient(null);

    if (debounceRef.current) clearTimeout(debounceRef.current);

    debounceRef.current = setTimeout(() => {
      performSearch(value);
    }, 300);
  };

  const handleCreate = async () => {
    if (!selectedPatient) return;
    try {
      const res = await icuCardApi.create({
        patientId: selectedPatient.patientID,
        patientName: selectedPatient.patientName,
        medicalCardNumber: selectedPatient.patientExternalID1,
        diagnosis,
        apacheIi: apacheII ? parseInt(apacheII) : undefined,
        sofa: sofa ? parseInt(sofa) : undefined,
        patientHeight: selectedPatient.patientHeight,
        patientWeight: selectedPatient.patientWeight,
        bloodGroup: selectedPatient.bloodGroup,
        rhFactor: selectedPatient.rhFactor,
        patientSexCode: selectedPatient.patientSexCode,
        patientBirthDate: selectedPatient.patientBirthDate,
      });
      navigate(`/doctor/card/${res.data.id}/day/${res.data.icuDays?.[0]?.id}`);
    } catch {
      setError('Помилка створення карти');
    }
  };

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto' }}>
      <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700, mb: 3 }}>
        Нова карта інтенсивної терапії
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>{error}</Alert>}

      <Paper sx={{ p: 3, mb: 3, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
        <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2 }}>
          Пошук пацієнта
        </Typography>

        <Autocomplete
          inputValue={search}
          onInputChange={handleInputChange}
          value={selectedPatient}
          onChange={(_, v) => setSelectedPatient(v)}
          options={patients}
          getOptionLabel={(p) => `${p.patientName} (${p.patientExternalID1})`}
          isOptionEqualToValue={(o, v) => o.patientID === v.patientID}
          filterOptions={(x) => x}
          loading={loading}
          noOptionsText={search.length < 2 ? 'Введіть мінімум 2 символи' : 'Пацієнтів не знайдено'}
          renderOption={(props, p) => {
            const { key, ...rest } = props;
            return (
              <Box component="li" key={key} {...rest} sx={{ px: 2, py: 1.5 }}>
                <Box>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>{p.patientName}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {p.patientExternalID1} &middot; {p.patientBirthDate} &middot; {p.patientAddress?.split(',')[0]?.trim()}
                  </Typography>
                </Box>
              </Box>
            );
          }}
          renderInput={(params) => (
            <TextField
              {...params}
              label="ПІБ, телефон або № медкарти"
              slotProps={{
                ...params.slotProps,
                input: {
                  ...params.slotProps?.input,
                  endAdornment: (
                    <>
                      {loading && <CircularProgress size={20} />}
                      {params.slotProps?.input?.endAdornment}
                    </>
                  ),
                },
              }}
            />
          )}
        />
      </Paper>

      {selectedPatient && (
        <Paper sx={{ p: 3, mb: 3, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
          <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2 }}>Дані пацієнта (з МІС)</Typography>
          <Grid container spacing={2}>
            <Grid size={6}><TextField fullWidth label="ПІП" value={selectedPatient.patientName} slotProps={{ input: { readOnly: true } }} /></Grid>
            <Grid size={3}><TextField fullWidth label="Вік" value={selectedPatient.patientBirthDate} slotProps={{ input: { readOnly: true } }} /></Grid>
            <Grid size={3}><TextField fullWidth label="Стать" value={selectedPatient.patientSexCode === 'M' ? 'Чол' : 'Жін'} slotProps={{ input: { readOnly: true } }} /></Grid>
            <Grid size={3}><TextField fullWidth label="Зріст (см)" value={selectedPatient.patientHeight} slotProps={{ input: { readOnly: true } }} /></Grid>
            <Grid size={3}><TextField fullWidth label="Маса (кг)" value={selectedPatient.patientWeight} slotProps={{ input: { readOnly: true } }} /></Grid>
            <Grid size={3}><TextField fullWidth label="Група крові" value={selectedPatient.bloodGroup} slotProps={{ input: { readOnly: true } }} /></Grid>
            <Grid size={3}><TextField fullWidth label="Rezus" value={selectedPatient.rhFactor} slotProps={{ input: { readOnly: true } }} /></Grid>
            <Grid size={6}><TextField fullWidth label="№ медкарти" value={selectedPatient.patientExternalID1} slotProps={{ input: { readOnly: true } }} /></Grid>
          </Grid>
        </Paper>
      )}

      {selectedPatient && (
        <Paper sx={{ p: 3, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
          <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2 }}>Дані поступлення</Typography>
          <Grid container spacing={2}>
            <Grid size={12}>
              <TextField fullWidth label="Діагноз" value={diagnosis} onChange={(e) => setDiagnosis(e.target.value)} multiline rows={2} />
            </Grid>
            <Grid size={6}>
              <TextField fullWidth label="APACHE II" type="number" value={apacheII} onChange={(e) => setApacheII(e.target.value)} />
            </Grid>
            <Grid size={6}>
              <TextField fullWidth label="SOFA" type="number" value={sofa} onChange={(e) => setSofa(e.target.value)} />
            </Grid>
          </Grid>

          <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
            <Button
              variant="contained" size="large" onClick={handleCreate}
              sx={{ bgcolor: '#FF5F33', '&:hover': { bgcolor: '#E8552E' } }}
            >
              Створити карту
            </Button>
            <Button variant="outlined" size="large" onClick={() => navigate('/doctor')}>
              Скасувати
            </Button>
          </Box>
        </Paper>
      )}
    </Box>
  );
}
