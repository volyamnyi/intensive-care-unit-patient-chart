import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Typography, Tabs, Tab, Button, Chip, Grid, Alert, CircularProgress, Paper, Dialog, DialogTitle, DialogContent, DialogActions, TextField, useTheme } from '@mui/material';
import { ArrowBack, LockOpen } from '@mui/icons-material';
import { episodeApi, clinicalDayApi, hourlyRecordApi, medicalOrderApi, medicalNoteApi, clinicalScaleApi, fluidBalanceApi } from '../../api/endpoints';
import { useAuth } from '../../services/AuthContext';
import ClinicalDayTimeline from '../../components/common/ClinicalDayTimeline';
import HourlyRecordTable from '../../components/common/HourlyRecordTable';
import VitalSignsForm from '../../components/common/VitalSignsForm';
import HourSelector from '../../components/common/HourSelector';
import MedicalOrdersPanel from '../../components/common/MedicalOrdersPanel';
import MedicalNotesPanel from '../../components/common/MedicalNotesPanel';
import ScaleResultsPanel from '../../components/common/ScaleResultsPanel';
import FluidBalancePanel from '../../components/common/FluidBalancePanel';
import SignDialog from '../../components/common/SignDialog';
import type { Episode, ClinicalDay, HourlyRecord, HourlyRecordCreateRequest, MedicalOrder, MedicalNote, ScaleResult, ClinicalScale, FluidBalanceItem, MedicalOrderCreateRequest } from '../../types';

const HOURS = [8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 1, 2, 3, 4, 5, 6, 7];

export default function PatientDayPage() {
  const { episodeId } = useParams();
  const navigate = useNavigate();
  const theme = useTheme();
  const { user } = useAuth();

  const [episode, setEpisode] = useState<Episode | null>(null);
  const [clinicalDays, setClinicalDays] = useState<ClinicalDay[]>([]);
  const [selectedDay, setSelectedDay] = useState<ClinicalDay | null>(null);
  const [records, setRecords] = useState<HourlyRecord[]>([]);
  const [orders, setOrders] = useState<MedicalOrder[]>([]);
  const [notes, setNotes] = useState<MedicalNote[]>([]);
  const [scaleResults, setScaleResults] = useState<ScaleResult[]>([]);
  const [availableScales, setAvailableScales] = useState<ClinicalScale[]>([]);
  const [balanceItems, setBalanceItems] = useState<FluidBalanceItem[]>([]);
  const [tab, setTab] = useState(0);
  const [currentHour, setCurrentHour] = useState(HOURS.find(h => h <= new Date().getHours()) || 8);
  const [vitalForm, setVitalForm] = useState<HourlyRecordCreateRequest>({ recordTime: '' });
  const [signDialogOpen, setSignDialogOpen] = useState(false);
  const [reopenDialogOpen, setReopenDialogOpen] = useState(false);
  const [reopenReason, setReopenReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [autoSaving, setAutoSaving] = useState(false);
  const lastManualSave = useRef(0);

  useEffect(() => {
    if (!episodeId) return;
    Promise.all([
      episodeApi.getById(episodeId),
      episodeApi.getClinicalDays(episodeId),
    ]).then(([epRes, daysRes]) => {
      setEpisode(epRes.data);
      setClinicalDays(daysRes.data);
      const isDoctorRole = user?.role === 'DOCTOR' || user?.role === 'HEAD_OF_DEPARTMENT';
      const target = isDoctorRole
        ? daysRes.data.find(d => d.status === 'NURSE_SIGNED')
        : daysRes.data.find(d => d.status === 'OPEN' || d.status === 'REOPENED');
      setSelectedDay(target || daysRes.data[0] || null);
    }).finally(() => setLoading(false));
  }, [episodeId, user]);

  const loadDayData = useCallback(async (day: ClinicalDay) => {
    try {
      const [recRes, ordRes, noteRes, scaleRes, balRes] = await Promise.all([
        hourlyRecordApi.getByClinicalDay(day.id),
        medicalOrderApi.getByClinicalDay(day.id),
        medicalNoteApi.getByClinicalDay(day.id),
        clinicalScaleApi.getResultsByClinicalDay(day.id),
        fluidBalanceApi.getByClinicalDay(day.id),
      ]);
      setRecords(recRes.data);
      setOrders(ordRes.data);
      setNotes(noteRes.data);
      setScaleResults(scaleRes.data);
      setBalanceItems(balRes.data);
    } catch {
      // data stays stale on error
    }
  }, []);

  useEffect(() => {
    if (selectedDay) {
      loadDayData(selectedDay);
    }
  }, [selectedDay?.id]);

  useEffect(() => {
    clinicalScaleApi.getAvailable().then(res => setAvailableScales(res.data)).catch(() => {});
  }, []);

  useEffect(() => {
    document.title = episode ? `ВАІТ — ${episode.patientName}` : 'ВАІТ — Пацієнт';
  }, [episode]);

  useEffect(() => {
    const hasData = vitalForm.systolicBP || vitalForm.diastolicBP || vitalForm.heartRate
      || vitalForm.spo2 || vitalForm.temperature || vitalForm.consciousness;
    if (!selectedDay || !hasData) return;
    if (Date.now() - lastManualSave.current < 5000) return;
    const timer = setTimeout(async () => {
      try {
        setAutoSaving(true);
        const recTime = `${new Date().toISOString().split('T')[0]}T${String(currentHour).padStart(2, '0')}:00:00`;
        await hourlyRecordApi.create(selectedDay.id, { ...vitalForm, recordTime: recTime });
        const res = await hourlyRecordApi.getByClinicalDay(selectedDay.id);
        setRecords(res.data);
      } catch {
        // auto-save failed silently
      } finally {
        setAutoSaving(false);
      }
    }, 3000);
    return () => clearTimeout(timer);
  }, [vitalForm, selectedDay, currentHour]);

  const handleSaveVitals = async () => {
    if (!selectedDay) return;
    lastManualSave.current = Date.now();
    try {
      const recTime = `${new Date().toISOString().split('T')[0]}T${String(currentHour).padStart(2, '0')}:00:00`;
      await hourlyRecordApi.create(selectedDay.id, { ...vitalForm, recordTime: recTime });
      const res = await hourlyRecordApi.getByClinicalDay(selectedDay.id);
      setRecords(res.data);
    } catch {
      // error handled silently
    }
  };

  const handleCreateOrder = async (order: MedicalOrderCreateRequest) => {
    if (!selectedDay) return;
    try {
      await medicalOrderApi.create(selectedDay.id, order);
      const res = await medicalOrderApi.getByClinicalDay(selectedDay.id);
      setOrders(res.data);
    } catch {
      // error handled silently
    }
  };

  const handleCancelOrder = async (orderId: string) => {
    const order = orders.find(o => o.id === orderId);
    if (!order || !selectedDay) return;
    try {
      await medicalOrderApi.cancel(orderId, { version: order.version });
      const res = await medicalOrderApi.getByClinicalDay(selectedDay.id);
      setOrders(res.data);
    } catch {
      // error handled silently
    }
  };

  const handleCreateNote = async (text: string, noteType: string) => {
    if (!selectedDay) return;
    try {
      await medicalNoteApi.create(selectedDay.id, { text, noteType });
      const res = await medicalNoteApi.getByClinicalDay(selectedDay.id);
      setNotes(res.data);
    } catch {
      // error handled silently
    }
  };

  const handleCreateScaleResult = async (scaleId: string, result: string) => {
    if (!selectedDay) return;
    try {
      await clinicalScaleApi.createResult(selectedDay.id, { scaleId, result });
      const res = await clinicalScaleApi.getResultsByClinicalDay(selectedDay.id);
      setScaleResults(res.data);
    } catch {
      // error handled silently
    }
  };

  const handleRecalculateBalance = async () => {
    if (!selectedDay) return;
    try {
      const res = await fluidBalanceApi.recalculate(selectedDay.id);
      setBalanceItems(res.data);
    } catch {
      // error handled silently
    }
  };

  const handleSignOff = async () => {
    if (!selectedDay || !user) return;
    try {
      if (user.role === 'NURSE') {
        await clinicalDayApi.signNurse(selectedDay.id, { userId: user.id });
      } else {
        await clinicalDayApi.signDoctor(selectedDay.id, { userId: user.id });
      }
      setSignDialogOpen(false);
      const daysRes = await episodeApi.getClinicalDays(episodeId!);
      setClinicalDays(daysRes.data);
      const updated = daysRes.data.find(d => d.id === selectedDay.id);
      if (updated) setSelectedDay(updated);
    } catch {
      // error handled silently
    }
  };

  const handleReopen = async () => {
    if (!selectedDay || !user || !reopenReason.trim()) return;
    try {
      await clinicalDayApi.reopen(selectedDay.id, { reason: reopenReason.trim(), version: selectedDay.version });
      setReopenDialogOpen(false);
      setReopenReason('');
      const daysRes = await episodeApi.getClinicalDays(episodeId!);
      setClinicalDays(daysRes.data);
      const updated = daysRes.data.find(d => d.id === selectedDay.id);
      if (updated) setSelectedDay(updated);
    } catch {
      // error handled silently
    }
  };

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;
  if (!episode) return <Alert severity="error">Епізод не знайдено</Alert>;

  const filledHours = records.map(r => {
    const match = r.recordTime.match(/(\d{2}):00/);
    return match ? parseInt(match[1]) : -1;
  }).filter(h => h >= 0);

  const isNurse = user?.role === 'NURSE';
  const isDoctor = user?.role === 'DOCTOR' || user?.role === 'HEAD_OF_DEPARTMENT';
  const isHod = user?.role === 'HEAD_OF_DEPARTMENT';
  const canSign = selectedDay && (
    (isNurse && selectedDay.status === 'OPEN') ||
    (isDoctor && selectedDay.status === 'NURSE_SIGNED')
  );
  const canReopen = isHod && selectedDay && (
    selectedDay.status === 'NURSE_SIGNED' || selectedDay.status === 'DOCTOR_SIGNED'
  );

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 1 }}>
        <Box>
          <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary }}>
            {episode.patientName || 'Пацієнт'}
          </Typography>
          <Typography variant="body2" sx={{ color: theme.palette.text.secondary, mt: 0.5 }}>
            {selectedDay ? `Доба №${selectedDay.dayNumber}` : 'Немає відкритої доби'}
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button variant="outlined" startIcon={<ArrowBack />} onClick={() => navigate(isNurse ? '/nurse' : '/doctor')}>
            Назад
          </Button>
          {canSign && (
            <Button variant="contained" onClick={() => setSignDialogOpen(true)}>
              Підписати добу
            </Button>
          )}
          {canReopen && (
            <Button variant="outlined" color="warning" startIcon={<LockOpen />} onClick={() => setReopenDialogOpen(true)}>
              Перевідкрити
            </Button>
          )}
        </Box>
      </Box>

      <Box sx={{ display: 'flex', gap: 1, mb: 3, flexWrap: 'wrap' }}>
        <Chip label={`${selectedDay?.status === 'OPEN' ? 'Відкрита' : selectedDay?.status === 'NURSE_SIGNED' ? 'Підписана медсестрою' : selectedDay?.status === 'DOCTOR_SIGNED' ? 'Підписана' : selectedDay?.status === 'REOPENED' ? 'Перевідкрита' : 'Закрита'}`}
          color={selectedDay?.status === 'OPEN' ? 'warning' : selectedDay?.status === 'NURSE_SIGNED' ? 'info' : selectedDay?.status === 'DOCTOR_SIGNED' ? 'success' : 'warning'} size="small" />
        <Chip label={`№ ${episode.id?.slice(0, 8)}`} variant="outlined" size="small" />
      </Box>

      <Paper sx={{ p: 2, mb: 3 }}>
        <ClinicalDayTimeline
          days={clinicalDays}
          selectedDayId={selectedDay?.id}
          onSelectDay={(day) => setSelectedDay(day)}
        />
      </Paper>

      {selectedDay && (
        <>
          <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2, mt: 0 }}>
            <Tab label="Вітальні" />
            <Tab label="Призначення" />
            <Tab label="Шкали" />
            <Tab label="Нотатки" />
            <Tab label="Баланс" />
          </Tabs>

          {tab === 0 && (
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 7 }}>
                <HourSelector
                  hours={HOURS}
                  currentHour={currentHour}
                  onSelect={setCurrentHour}
                  filledHours={filledHours}
                />
                <VitalSignsForm
                  values={vitalForm}
                  onChange={setVitalForm}
                  onSave={handleSaveVitals}
                  saving={autoSaving}
                  title={`Показники — ${currentHour}:00`}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 5 }}>
                <HourlyRecordTable records={records} hours={HOURS} />
              </Grid>
            </Grid>
          )}

          {tab === 1 && (
            <MedicalOrdersPanel
              orders={orders}
              onCreateOrder={isDoctor ? handleCreateOrder : undefined}
              onCancelOrder={isDoctor ? handleCancelOrder : undefined}
              canCreate={isDoctor}
              canExecute={isNurse}
            />
          )}

          {tab === 2 && (
            <ScaleResultsPanel
              results={scaleResults}
              availableScales={availableScales}
              onCreateResult={isNurse ? undefined : handleCreateScaleResult}
            />
          )}

          {tab === 3 && (
            <MedicalNotesPanel
              notes={notes}
              onCreateNote={handleCreateNote}
            />
          )}

          {tab === 4 && (
            <FluidBalancePanel
              items={balanceItems}
              onRecalculate={handleRecalculateBalance}
            />
          )}
        </>
      )}

      <Dialog open={reopenDialogOpen} onClose={() => setReopenDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Перевідкрити добу №{selectedDay?.dayNumber}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2, color: 'text.secondary' }}>
            Ви впевнені, що хочете перевідкрити цю добу? Підписи будуть скасовані.
          </Typography>
          <TextField
            autoFocus
            label="Причина перевідкриття"
            fullWidth
            multiline
            rows={3}
            value={reopenReason}
            onChange={e => setReopenReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReopenDialogOpen(false)}>Скасувати</Button>
          <Button onClick={handleReopen} variant="contained" color="warning" disabled={!reopenReason.trim()}>
            Перевідкрити
          </Button>
        </DialogActions>
      </Dialog>

      <SignDialog
        open={signDialogOpen}
        onClose={() => setSignDialogOpen(false)}
        onConfirm={handleSignOff}
        dayNumber={selectedDay?.dayNumber || 0}
        role={user?.role}
      />
    </Box>
  );
}
