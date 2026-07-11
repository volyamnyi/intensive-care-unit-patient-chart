import { useState, useEffect } from 'react';
import {
  Box, Paper, Typography, Grid, TextField, Button, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Alert, CircularProgress,
  Select, MenuItem, FormControl, InputLabel, IconButton
} from '@mui/material';
import { CheckCircle } from '@mui/icons-material';
import { icuCardApi, icuDayApi, prescriptionApi } from '../../api/endpoints';
import type { IcuCard, IcuDay, HourlyVital, Prescription, FluidBalance } from '../../types';

const HOURS = [8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 1, 2, 3, 4, 5, 6, 7];

export default function NurseDashboardPage() {
  const [cards, setCards] = useState<IcuCard[]>([]);
  const [selectedCard, setSelectedCard] = useState<IcuCard | null>(null);
  const [selectedDay, setSelectedDay] = useState<IcuDay | null>(null);
  const [currentHour, setCurrentHour] = useState(HOURS.find(h => h <= new Date().getHours()) || 8);
  const [vitals, setVitals] = useState<HourlyVital[]>([]);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [balance, setBalance] = useState<FluidBalance | null>(null);
  const [loading, setLoading] = useState(true);

  const [vitalForm, setVitalForm] = useState({
    systolicBp: '', diastolicBp: '', heartRate: '', spo2: '',
    temperature: '', cvp: '', respiratoryRate: ''
  });
  const [outputForm, setOutputForm] = useState({ urine: '', tube: '', drainage: '', stool: false });

  useEffect(() => {
    icuCardApi.getActive()
      .then((res) => {
        setCards(res.data);
        if (res.data.length > 0) {
          setSelectedCard(res.data[0]);
          const activeDay = res.data[0].icuDays?.find(d => d.status === 'ACTIVE');
          if (activeDay) setSelectedDay(activeDay);
        }
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedDay) return;
    const cardId = selectedCard?.id;
    if (!cardId) return;
    Promise.all([
      icuDayApi.getVitals(selectedDay.id),
      prescriptionApi.getByCard(cardId),
      icuDayApi.getBalance(selectedDay.id),
    ]).then(([v, p, b]) => {
      setVitals(v.data);
      setPrescriptions(p.data);
      setBalance(b.data);
      const existing = v.data.find(v => v.hour === currentHour);
      if (existing) {
        setVitalForm({
          systolicBp: existing.systolicBp?.toString() || '',
          diastolicBp: existing.diastolicBp?.toString() || '',
          heartRate: existing.heartRate?.toString() || '',
          spo2: existing.spo2?.toString() || '',
          temperature: existing.temperature?.toString() || '',
          cvp: existing.cvp?.toString() || '',
          respiratoryRate: existing.respiratoryRate?.toString() || '',
        });
      }
    });
  }, [selectedDay, currentHour]);

  const handleSaveVitals = async () => {
    if (!selectedDay) return;
    await icuDayApi.saveVitals(selectedDay.id, currentHour, {
      systolicBp: vitalForm.systolicBp ? Number(vitalForm.systolicBp) : null,
      diastolicBp: vitalForm.diastolicBp ? Number(vitalForm.diastolicBp) : null,
      heartRate: vitalForm.heartRate ? Number(vitalForm.heartRate) : null,
      spo2: vitalForm.spo2 ? Number(vitalForm.spo2) : null,
      temperature: vitalForm.temperature ? Number(vitalForm.temperature) : null,
      cvp: vitalForm.cvp ? Number(vitalForm.cvp) : null,
      respiratoryRate: vitalForm.respiratoryRate ? Number(vitalForm.respiratoryRate) : null,
    });
    const res = await icuDayApi.getVitals(selectedDay.id);
    setVitals(res.data);
  };

  const handleExecutePrescription = async (prescriptionId: number, dose: string) => {
    if (!selectedDay) return;
    const vol = parseInt(dose.replace(/[^0-9]/g, '')) || 0;
    await prescriptionApi.execute(prescriptionId, selectedDay.id, currentHour, vol);
    const prescRes = await prescriptionApi.getByCard(selectedCard!.id);
    setPrescriptions(prescRes.data);
  };

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;
  if (cards.length === 0) return <Alert severity="info" sx={{ borderRadius: 2 }}>Немає активних пацієнтів</Alert>;

  const currentHourIndex = HOURS.indexOf(currentHour);

  return (
    <Box>
      <FormControl fullWidth sx={{ mb: 2 }}>
        <InputLabel>Пацієнт</InputLabel>
        <Select value={selectedCard?.id || ''} label="Пацієнт"
          onChange={(e) => {
            const card = cards.find(c => c.id === Number(e.target.value))!;
            setSelectedCard(card);
            const activeDay = card.icuDays?.find(d => d.status === 'ACTIVE');
            if (activeDay) setSelectedDay(activeDay);
          }}>
          {cards.map((c) => (
            <MenuItem key={c.id} value={c.id}>{c.patientName} (доба {c.icuDays?.find(d => d.status === 'ACTIVE')?.dayNumber || '-'})</MenuItem>
          ))}
        </Select>
      </FormControl>

      {selectedDay && (
        <>
          <Paper sx={{ p: 1.5, mb: 2, display: 'flex', gap: 0.5, overflow: 'auto', border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
            {HOURS.map((h, i) => {
              const filled = vitals.find(v => v.hour === h);
              let color = '';
              let textColor = '#1F1F1F';
              let border = '1px solid #E8E6E1';
              if (i === currentHourIndex) {
                color = '#B6CECA';
                textColor = '#1F1F1F';
                border = '2px solid #8AAB9E';
              } else if (i < currentHourIndex) {
                color = filled ? '#F0F7F3' : '#FFF5F3';
                textColor = filled ? '#1F6B4C' : '#C42E1A';
                border = filled ? '1px solid #D4E8DE' : '1px solid #FFD6CC';
              }
              return (
                <Box key={h} onClick={() => setCurrentHour(h)}
                  sx={{
                    minWidth: 48, textAlign: 'center', py: 1, borderRadius: 2, cursor: 'pointer',
                    bgcolor: color, color: textColor, fontWeight: 700, fontSize: 13,
                    border, transition: 'all 0.2s ease',
                    '&:hover': { transform: 'translateY(-1px)', boxShadow: '0 2px 8px rgba(0,0,0,0.08)' },
                  }}>
                  {h}:00<br />
                  {i < currentHourIndex && (filled ? '✓' : '✗')}
                  {i === currentHourIndex && '▶'}
                </Box>
              );
            })}
          </Paper>

          <Grid container spacing={2}>
            <Grid item xs={12} md={7}>
              <Paper sx={{ p: 2.5, mb: 2, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
                <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>Показники — {currentHour}:00</Typography>
                <Grid container spacing={1}>
                  <Grid item xs={4}><TextField fullWidth size="small" label="АТ сист (мм.рт.ст)" value={vitalForm.systolicBp} onChange={(e) => setVitalForm({ ...vitalForm, systolicBp: e.target.value })} /></Grid>
                  <Grid item xs={4}><TextField fullWidth size="small" label="АТ діас (мм.рт.ст)" value={vitalForm.diastolicBp} onChange={(e) => setVitalForm({ ...vitalForm, diastolicBp: e.target.value })} /></Grid>
                  <Grid item xs={4}><TextField fullWidth size="small" label="ЧСС (в 1 хв)" value={vitalForm.heartRate} onChange={(e) => setVitalForm({ ...vitalForm, heartRate: e.target.value })} /></Grid>
                  <Grid item xs={3}><TextField fullWidth size="small" label="SpO2 (%)" value={vitalForm.spo2} onChange={(e) => setVitalForm({ ...vitalForm, spo2: e.target.value })} /></Grid>
                  <Grid item xs={3}><TextField fullWidth size="small" label="Темп. тіла (°С)" value={vitalForm.temperature} onChange={(e) => setVitalForm({ ...vitalForm, temperature: e.target.value })} /></Grid>
                  <Grid item xs={3}><TextField fullWidth size="small" label="ЦВТ (мм.вод.ст)" value={vitalForm.cvp} onChange={(e) => setVitalForm({ ...vitalForm, cvp: e.target.value })} /></Grid>
                  <Grid item xs={3}><TextField fullWidth size="small" label="ЧД (в 1 хв)" value={vitalForm.respiratoryRate} onChange={(e) => setVitalForm({ ...vitalForm, respiratoryRate: e.target.value })} /></Grid>
                </Grid>
                <Button variant="contained" sx={{ mt: 2 }} onClick={handleSaveVitals}>Зберегти показники</Button>
              </Paper>

              <Paper sx={{ p: 2.5, mb: 2, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
                <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>Втрати рідини — {currentHour}:00</Typography>
                <Grid container spacing={1}>
                  <Grid item xs={4}><TextField fullWidth size="small" label="Сеча (мл)" value={outputForm.urine} onChange={(e) => setOutputForm({ ...outputForm, urine: e.target.value })} /></Grid>
                  <Grid item xs={4}><TextField fullWidth size="small" label="Зонд (мл)" value={outputForm.tube} onChange={(e) => setOutputForm({ ...outputForm, tube: e.target.value })} /></Grid>
                  <Grid item xs={4}><TextField fullWidth size="small" label="Дренаж (мл)" value={outputForm.drainage} onChange={(e) => setOutputForm({ ...outputForm, drainage: e.target.value })} /></Grid>
                  <Grid item xs={12}>
                    <FormControl size="small">
                      <InputLabel>Випорожнення</InputLabel>
                      <Select value={outputForm.stool ? 'yes' : 'no'} label="Випорожнення"
                        onChange={(e) => setOutputForm({ ...outputForm, stool: e.target.value === 'yes' })}>
                        <MenuItem value="no">Ні</MenuItem>
                        <MenuItem value="yes">Так</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>
                </Grid>
                <Button variant="contained" sx={{ mt: 2 }}>Зберегти втрати</Button>
              </Paper>

              <Paper sx={{ p: 2.5, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
                <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>Призначення лікаря</Typography>
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Препарат</TableCell><TableCell>Доза</TableCell><TableCell>Виконання</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {prescriptions.filter(p => p.status === 'ACTIVE').map((p) => (
                        <TableRow key={p.id}>
                          <TableCell sx={{ fontWeight: 600 }}>{p.medication}</TableCell>
                          <TableCell>{p.dose}</TableCell>
                          <TableCell>
                            <IconButton onClick={() => handleExecutePrescription(p.id, p.dose)} sx={{ color: '#1F6B4C', '&:hover': { bgcolor: '#F0F7F3' } }}>
                              <CheckCircle />
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Paper>
            </Grid>

            <Grid item xs={12} md={5}>
              <Paper sx={{ p: 2.5, mb: 2, bgcolor: '#F5FBF8', border: '1px solid #D4E8DE', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
                <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2 }}>Баланс рідини</Typography>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
                  <Typography color="text.secondary">Надійшло:</Typography>
                  <Typography fontWeight={700}>{balance?.totalIntake || 0} мл</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
                  <Typography color="text.secondary">Виділено:</Typography>
                  <Typography fontWeight={700}>{balance?.totalOutput || 0} мл</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
                  <Typography color="text.secondary">Добовий баланс:</Typography>
                  <Typography fontWeight={700} color={balance?.dailyBalance && balance.dailyBalance < 0 ? '#C42E1A' : '#1F6B4C'}>
                    {balance?.dailyBalance || 0} мл
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography color="text.secondary">Кумулятивний баланс:</Typography>
                  <Typography fontWeight={700} color={balance?.cumulativeBalance && balance.cumulativeBalance < 0 ? '#C42E1A' : '#1F6B4C'}>
                    {balance?.cumulativeBalance || 0} мл
                  </Typography>
                </Box>
              </Paper>

              <Paper sx={{ p: 2.5, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
                <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1 }}>Шкала Брейдена</Typography>
                <Typography variant="body2" color="text.secondary">
                  Заповнюється 2 рази на добу (08:00, 20:00)
                </Typography>
              </Paper>
            </Grid>
          </Grid>
        </>
      )}
    </Box>
  );
}
