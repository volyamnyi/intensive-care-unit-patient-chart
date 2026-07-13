import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, Tabs, Tab, Button, Chip, Grid, TextField,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Alert, Dialog, DialogTitle, DialogContent, DialogActions, CircularProgress
} from '@mui/material';
import { ArrowBack } from '@mui/icons-material';
import { icuCardApi, icuDayApi, prescriptionApi } from '../../api/endpoints';
import type { IcuCard, IcuDay, HourlyVital, Prescription, ScaleAssessment, ClinicalNote } from '../../types';

interface TabPanelProps { children: React.ReactNode; value: number; index: number; }
function TabPanel({ children, value, index }: TabPanelProps) {
  return value === index ? <Box sx={{ py: 2 }}>{children}</Box> : null;
}

export default function PatientDayPage() {
  const { cardId, dayId } = useParams();
  const navigate = useNavigate();
  const [card, setCard] = useState<IcuCard | null>(null);
  const [day, setDay] = useState<IcuDay | null>(null);
  const [vitals, setVitals] = useState<HourlyVital[]>([]);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [scales, setScales] = useState<ScaleAssessment[]>([]);
  const [notes, setNotes] = useState<ClinicalNote[]>([]);
  const [tab, setTab] = useState(0);
  const [loading, setLoading] = useState(true);
  const [signDialogOpen, setSignDialogOpen] = useState(false);
  const [newPrescription, setNewPrescription] = useState<{ medication: string; dose: string; route: string; frequency: string; startHour: number; endHour: number; type: 'THERAPY' | 'LAB' }>({ medication: '', dose: '', route: '', frequency: '', startHour: 8, endHour: 20, type: 'THERAPY' });
  const [newNote, setNewNote] = useState('');

  useEffect(() => {
    if (!cardId) return;
    Promise.all([
      icuCardApi.getById(Number(cardId)),
      dayId ? icuDayApi.getVitals(Number(dayId)) : Promise.resolve({ data: [] }),
      dayId ? icuDayApi.getScales(Number(dayId)) : Promise.resolve({ data: [] }),
      dayId ? icuDayApi.getNotes(Number(dayId)) : Promise.resolve({ data: [] }),
      prescriptionApi.getByCard(Number(cardId)),
    ]).then(([cardRes, vitalsRes, scalesRes, notesRes, prescRes]) => {
      setCard(cardRes.data);
      setVitals(vitalsRes.data);
      setScales(scalesRes.data);
      setNotes(notesRes.data);
      setPrescriptions(prescRes.data);
      const foundDay = cardRes.data.icuDays?.find((d) => d.id === Number(dayId));
      setDay(foundDay || null);
    }).catch((err) => {
      console.error('Failed to load patient day data:', err);
    }).finally(() => setLoading(false));
  }, [cardId, dayId]);

  useEffect(() => {
    document.title = card ? `ВАІТ — ${card.patientName}` : 'ВАІТ — Пацієнт';
  }, [card]);

  const handleCreatePrescription = async () => {
    if (!cardId) return;
    await prescriptionApi.create(Number(cardId), newPrescription);
    const res = await prescriptionApi.getByCard(Number(cardId));
    setPrescriptions(res.data);
    setNewPrescription({ medication: '', dose: '', route: '', frequency: '', startHour: 8, endHour: 20, type: 'THERAPY' });
  };

  const handleAddNote = async () => {
    if (!dayId || !newNote.trim()) return;
    await icuDayApi.addNote(Number(dayId), { content: newNote, noteType: 'DOCTOR_NOTE' });
    const res = await icuDayApi.getNotes(Number(dayId));
    setNotes(res.data);
    setNewNote('');
  };

  const handleSignOff = async () => {
    if (!dayId) return;
    await icuDayApi.signOff(Number(dayId));
    setSignDialogOpen(false);
    navigate('/doctor');
  };

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;
  if (!card || !day) return <Alert severity="error">Карту або добу не знайдено</Alert>;

  const isSignable = day.status === 'ACTIVE';
  const currentHour = new Date().getHours();
  const dayHours = [8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 1, 2, 3, 4, 5, 6, 7];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box>
          <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700 }}>{card.patientName}</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Доба №{day.dayNumber} | {day.date}
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button variant="outlined" startIcon={<ArrowBack />} onClick={() => navigate('/doctor')}>Назад</Button>
          {isSignable && (
            <Button
              variant="contained" onClick={() => setSignDialogOpen(true)}
              sx={{ bgcolor: '#FF5F33', '&:hover': { bgcolor: '#E8552E' } }}
            >
              Підписати добу
            </Button>
          )}
        </Box>
      </Box>

      <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
        <Chip label={`Статус: ${day.status === 'ACTIVE' ? 'Активна' : day.status === 'SIGNED' ? 'Підписана' : 'Архівна'}`}
          color={day.status === 'ACTIVE' ? 'success' : day.status === 'SIGNED' ? 'info' : 'default'} size="small" />
        <Chip label={`APACHE II: ${card.apacheIi}`} variant="outlined" size="small" />
        <Chip label={`SOFA: ${card.sofa}`} variant="outlined" size="small" />
        <Chip label={`№ ${card.medicalCardNumber}`} variant="outlined" size="small" />
      </Box>

      <Paper sx={{ p: 2, mb: 2, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
        <Grid container spacing={2}>
          <Grid size={6}><Typography variant="body2"><strong>Діагноз:</strong> {card.diagnosis}</Typography></Grid>
          <Grid size={3}><Typography variant="body2"><strong>Надійшов:</strong> {new Date(card.admissionDate).toLocaleDateString('uk-UA')}</Typography></Grid>
          <Grid size={3}><Typography variant="body2"><strong>Тривалість:</strong> {card.icuDays?.length || 0} днів</Typography></Grid>
        </Grid>
      </Paper>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 1 }}>
        <Tab label="Вітальні показники" />
        <Tab label="Призначення" />
        <Tab label="Шкали" />
        <Tab label="Нотатки" />
      </Tabs>

      <TabPanel value={tab} index={0}>
        <TableContainer component={Paper} sx={{ border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Година</TableCell>
                <TableCell>АТ сист</TableCell>
                <TableCell>АТ діас</TableCell>
                <TableCell>ЧСС</TableCell>
                <TableCell>SpO2</TableCell>
                <TableCell>Темп</TableCell>
                <TableCell>ЦВТ</TableCell>
                <TableCell>ЧД</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {dayHours.map((h) => {
                const v = vitals.find((v) => v.hour === h);
                const isPast = h < currentHour;
                const bg = isPast && v ? '#F0F7F3' : isPast && !v ? '#FFF5F3' : 'inherit';
                return (
                  <TableRow key={h} sx={{ bgcolor: bg }}>
                    <TableCell sx={{ fontWeight: 600 }}>{h}:00</TableCell>
                    <TableCell>{v?.systolicBp ?? '-'}</TableCell>
                    <TableCell>{v?.diastolicBp ?? '-'}</TableCell>
                    <TableCell>{v?.heartRate ?? '-'}</TableCell>
                    <TableCell>{v?.spo2 ?? '-'}</TableCell>
                    <TableCell>{v?.temperature ?? '-'}</TableCell>
                    <TableCell>{v?.cvp ?? '-'}</TableCell>
                    <TableCell>{v?.respiratoryRate ?? '-'}</TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      <TabPanel value={tab} index={1}>
        <Box sx={{ mb: 2 }}>
          <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1 }}>Нове призначення</Typography>
          <Grid container spacing={1} sx={{ alignItems: 'center' }}>
            <Grid size={2}>
              <TextField fullWidth size="small" select label="Тип" value={newPrescription.type} onChange={(e) => setNewPrescription({ ...newPrescription, type: e.target.value as 'THERAPY' | 'LAB' })} slotProps={{ select: { native: true } }}>
                <option value="THERAPY">Терапія</option>
                <option value="LAB">Лаб. дослідження</option>
              </TextField>
            </Grid>
            <Grid size={3}><TextField fullWidth size="small" label="Препарат / дослідження" value={newPrescription.medication} onChange={(e) => setNewPrescription({ ...newPrescription, medication: e.target.value })} /></Grid>
            <Grid size={2}><TextField fullWidth size="small" label="Доза" value={newPrescription.dose} onChange={(e) => setNewPrescription({ ...newPrescription, dose: e.target.value })} /></Grid>
            <Grid size={2}><TextField fullWidth size="small" label="Шлях" value={newPrescription.route} onChange={(e) => setNewPrescription({ ...newPrescription, route: e.target.value })} /></Grid>
            <Grid size={2}><TextField fullWidth size="small" label="Год. від" type="number" value={newPrescription.startHour} onChange={(e) => setNewPrescription({ ...newPrescription, startHour: Number(e.target.value) })} /></Grid>
            <Grid size={1}><Button variant="contained" size="small" onClick={handleCreatePrescription} sx={{ minWidth: 40 }}>+</Button></Grid>
          </Grid>
        </Box>

        <Typography variant="subtitle2" sx={{ mb: 1, fontFamily: '"Rubik", sans-serif' }}>Терапія</Typography>

        <TableContainer component={Paper} sx={{ border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Препарат</TableCell><TableCell>Доза</TableCell>
                <TableCell>Шлях</TableCell><TableCell>Години</TableCell>
                <TableCell>Статус</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {prescriptions.filter(p => p.type === 'THERAPY').map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{p.medication}</TableCell>
                  <TableCell>{p.dose}</TableCell>
                  <TableCell>{p.route}</TableCell>
                  <TableCell>{p.startHour}:00-{p.endHour}:00</TableCell>
                  <TableCell>
                    <Chip label={p.status === 'ACTIVE' ? 'Активне' : p.status === 'STOPPED' ? 'Зупинено' : 'Закінчено'}
                      size="small" color={p.status === 'ACTIVE' ? 'success' : 'default'} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>

        <Typography variant="subtitle2" sx={{ mt: 2, mb: 1, fontFamily: '"Rubik", sans-serif' }}>Лабораторні дослідження</Typography>
        <TableContainer component={Paper} sx={{ border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Дослідження</TableCell><TableCell>Години</TableCell>
                <TableCell>Статус</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {prescriptions.filter(p => p.type === 'LAB').map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{p.medication}</TableCell>
                  <TableCell>{p.startHour}:00-{p.endHour}:00</TableCell>
                  <TableCell>
                    <Chip label={p.status === 'ACTIVE' ? 'Активне' : p.status === 'STOPPED' ? 'Зупинено' : 'Закінчено'}
                      size="small" color={p.status === 'ACTIVE' ? 'success' : 'default'} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      <TabPanel value={tab} index={3}>
        <Box sx={{ mb: 2 }}>
          <TextField fullWidth multiline minRows={3} label="Нова нотатка" value={newNote}
            onChange={(e) => setNewNote(e.target.value)} />
          <Button variant="contained" sx={{ mt: 1 }} onClick={handleAddNote}>Додати нотатку</Button>
        </Box>
        {notes.length === 0 ? (
          <Typography color="text.secondary">Немає нотаток</Typography>
        ) : (
          notes.map((n) => (
            <Paper key={n.id} sx={{ p: 2, mb: 1, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
              <Typography variant="body2" color="text.secondary">
                {n.createdBy} · {new Date(n.createdAt).toLocaleString('uk-UA')}
              </Typography>
              <Typography sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>{n.content}</Typography>
            </Paper>
          ))
        )}
      </TabPanel>

      <TabPanel value={tab} index={2}>
        <Grid container spacing={2}>
          {['APACHE_II', 'SOFA', 'RASS', 'CAM_ICU', 'BRADEN'].map((type) => {
            const scale = scales.find((s) => s.scaleType === type);
            const fallback = type === 'APACHE_II' ? day?.apacheIi : type === 'SOFA' ? day?.sofa : undefined;
            const names: Record<string, string> = {
              APACHE_II: 'APACHE II', SOFA: 'SOFA',
              RASS: 'RASS (Richmond Agitation-Sedation Scale)',
              CAM_ICU: 'CAM-ICU (Delirium Assessment)',
              BRADEN: 'Шкала Брейдена',
            };
            return (
              <Grid size={{ xs: 12, md: 6 }} key={type}>
                <Paper sx={{ p: 2, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
                  <Typography variant="subtitle1" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 600 }}>{names[type]}</Typography>
                  {scale ? (
                    <Typography variant="body1" sx={{ mt: 0.5 }}>Бал: <strong>{scale.score}</strong> (год. {scale.hour}:00)</Typography>
                  ) : fallback != null ? (
                    <Typography variant="body1" sx={{ mt: 0.5 }}>Бал: <strong>{fallback}</strong> (з карти)</Typography>
                  ) : (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>Не заповнено</Typography>
                  )}
                </Paper>
              </Grid>
            );
          })}
        </Grid>
      </TabPanel>

      <Dialog open={signDialogOpen} onClose={() => setSignDialogOpen(false)}>
        <DialogTitle sx={{ fontFamily: '"Rubik", sans-serif' }}>Підписання доби №{day.dayNumber}</DialogTitle>
        <DialogContent>
          <Typography>Після підписання доба стане read-only.</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Буде згенеровано PDF та відправлено в МІС.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSignDialogOpen(false)}>Скасувати</Button>
          <Button
            variant="contained" onClick={handleSignOff}
            sx={{ bgcolor: '#FF5F33', '&:hover': { bgcolor: '#E8552E' } }}
          >
            Підписати
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
