import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, Alert, CircularProgress, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Typography, useTheme } from '@mui/material';
import { ArrowBack, LockOpen, Download } from '@mui/icons-material';
import { episodeApi, clinicalDayApi, hourlyRecordApi, medicalOrderApi, orderExecutionApi, medicalNoteApi, clinicalScaleApi, fluidBalanceApi, labResultApi, ventilationApi, patientStateApi } from '../../api/endpoints';
import { useAuth } from '../../services/AuthContext';
import { useTranslation } from 'react-i18next';
import DoctorDashboard from '../../components/monitoring/DoctorDashboard';
import NurseDashboard from '../../components/monitoring/NurseDashboard';
import SignDialog from '../../components/common/SignDialog';
import type { Episode, ClinicalDay, HourlyRecord, HourlyRecordCreateRequest, MedicalOrder, MedicalNote, ScaleResult, ClinicalScale, FluidBalanceItem, LabResult, VentilationSettings, MedicalOrderCreateRequest, PatientStateAssessment } from '../../types';

export default function PatientDayPage() {
  const { t } = useTranslation();
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
  const [balanceItems, setFluidBalanceItems] = useState<FluidBalanceItem[]>([]);
  const [labResults, setLabResults] = useState<LabResult[]>([]);
  const [ventilationSettings, setVentilationSettings] = useState<VentilationSettings[]>([]);
  const [patientStateAssessments, setPatientStateAssessments] = useState<PatientStateAssessment[]>([]);
  const [currentHour, setCurrentHour] = useState(new Date().getHours());
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
      const [recRes, ordRes, noteRes, scaleRes, balRes, labRes, ventRes, psRes] = await Promise.all([
        hourlyRecordApi.getByClinicalDay(day.id),
        medicalOrderApi.getByClinicalDay(day.id),
        medicalNoteApi.getByClinicalDay(day.id),
        clinicalScaleApi.getResultsByClinicalDay(day.id),
        fluidBalanceApi.getByClinicalDay(day.id),
        labResultApi.getByClinicalDay(day.id),
        ventilationApi.getByClinicalDay(day.id),
        patientStateApi.getByClinicalDay(day.id),
      ]);
      setRecords(recRes.data);
      setOrders(ordRes.data);
      setNotes(noteRes.data);
      setScaleResults(scaleRes.data);
      setFluidBalanceItems(balRes.data);
      setLabResults(labRes.data);
      setVentilationSettings(ventRes.data);
      setPatientStateAssessments(psRes.data);
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
    document.title = episode ? `ICU — ${episode.patientName}` : 'ICU — Patient';
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

  const handleExecuteOrder = async (orderId: string) => {
    if (!selectedDay || !user) return;
    try {
      await orderExecutionApi.create(orderId, {
        executedBy: user.id,
        executedAt: new Date().toISOString(),
        actualDose: '',
      });
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

  const handleSaveVentilation = async (data: { recordHour: number; mode: string; fio2: string; peep: string; tidalVolume: string; minuteVolume: string; pinsp: string; psupport: string; triggerType: string; ieRatio: string; respiratoryRate: string; plateauPressure: string; meanAirwayPressure: string }) => {
    if (!selectedDay) return;
    try {
      await ventilationApi.create(selectedDay.id, {
        recordHour: data.recordHour,
        mode: data.mode || undefined,
        fio2: data.fio2 ? Number(data.fio2) : undefined,
        peep: data.peep ? Number(data.peep) : undefined,
        tidalVolume: data.tidalVolume ? Number(data.tidalVolume) : undefined,
        minuteVolume: data.minuteVolume ? Number(data.minuteVolume) : undefined,
        pinsp: data.pinsp ? Number(data.pinsp) : undefined,
        psupport: data.psupport ? Number(data.psupport) : undefined,
        triggerType: data.triggerType || undefined,
        ieRatio: data.ieRatio || undefined,
        respiratoryRate: data.respiratoryRate ? Number(data.respiratoryRate) : undefined,
        plateauPressure: data.plateauPressure ? Number(data.plateauPressure) : undefined,
        meanAirwayPressure: data.meanAirwayPressure ? Number(data.meanAirwayPressure) : undefined,
      });
      const res = await ventilationApi.getByClinicalDay(selectedDay.id);
      setVentilationSettings(res.data);
    } catch {
      // error handled silently
    }
  };

  const handleCreateLabResult = async (data: { testCode: string; testName: string; result: string; unit: string; referenceMin: number | null; referenceMax: number | null; measuredAt: string }) => {
    if (!selectedDay) return;
    try {
      await labResultApi.create(selectedDay.id, data);
      const res = await labResultApi.getByClinicalDay(selectedDay.id);
      setLabResults(res.data);
    } catch {
      // error handled silently
    }
  };

  const handleSavePatientState = async (data: { recordHour: number; consciousness: string; skin: string; edema: string; mucousMembranes: string; peripheralCirculation: string; bowelSounds: string; generalCondition: string; additionalNotes: string }) => {
    if (!selectedDay) return;
    try {
      const existing = patientStateAssessments.find(a => a.recordHour === data.recordHour);
      if (existing) {
        await patientStateApi.update(existing.id, {
          consciousness: data.consciousness || undefined,
          skin: data.skin || undefined,
          edema: data.edema || undefined,
          mucousMembranes: data.mucousMembranes || undefined,
          peripheralCirculation: data.peripheralCirculation || undefined,
          bowelSounds: data.bowelSounds || undefined,
          generalCondition: data.generalCondition || undefined,
          additionalNotes: data.additionalNotes || undefined,
          version: existing.version,
        });
      } else {
        await patientStateApi.create(selectedDay.id, {
          recordHour: data.recordHour,
          consciousness: data.consciousness || undefined,
          skin: data.skin || undefined,
          edema: data.edema || undefined,
          mucousMembranes: data.mucousMembranes || undefined,
          peripheralCirculation: data.peripheralCirculation || undefined,
          bowelSounds: data.bowelSounds || undefined,
          generalCondition: data.generalCondition || undefined,
          additionalNotes: data.additionalNotes || undefined,
        });
      }
      const res = await patientStateApi.getByClinicalDay(selectedDay.id);
      setPatientStateAssessments(res.data);
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
      setFluidBalanceItems(res.data);
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

  const handleGeneratePDF = async () => {
    if (!selectedDay) return;
    try {
      await clinicalDayApi.generatePdf(selectedDay.id);
    } catch {
      // error handled silently
    }
  };

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;
  if (!episode) return <Alert severity="error">{t('doctor.patientDay.episodeNotFound')}</Alert>;

  const isNurse = user?.role === 'NURSE';
  const isDoctor = user?.role === 'DOCTOR' || user?.role === 'HEAD_OF_DEPARTMENT';
  const isHod = user?.role === 'HEAD_OF_DEPARTMENT';
  const isLocked = selectedDay ? selectedDay.status !== 'OPEN' && selectedDay.status !== 'REOPENED' : true;
  const canSign = selectedDay && (
    (isNurse && selectedDay.status === 'OPEN') ||
    (isDoctor && selectedDay.status === 'NURSE_SIGNED')
  );
  const canReopen = isHod && selectedDay && (
    selectedDay.status === 'NURSE_SIGNED' || selectedDay.status === 'DOCTOR_SIGNED'
  );

  return (
    <Box>
      {/* Float action bar */}
      <Box sx={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        mb: 1, flexWrap: 'wrap', gap: 0.5,
      }}>
        <Button
          variant="text"
          size="small"
          startIcon={<ArrowBack />}
          onClick={() => navigate(isNurse ? '/nurse' : '/doctor')}
          sx={{ color: theme.palette.text.secondary, fontSize: 13 }}
        >
          {t('doctor.patientDay.backButton')}
        </Button>
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          {selectedDay && selectedDay.status === 'CLOSED' && (
            <Button size="small" variant="outlined" startIcon={<Download />} onClick={handleGeneratePDF}>
              PDF
            </Button>
          )}
          {canReopen && (
            <Button size="small" variant="outlined" color="warning" startIcon={<LockOpen />} onClick={() => setReopenDialogOpen(true)}>
              {t('doctor.patientDay.reopenButton')}
            </Button>
          )}
          {canSign && (
            <Button size="small" variant="contained" onClick={() => setSignDialogOpen(true)} sx={{ fontWeight: 700 }}>
              {t('doctor.patientDay.signOffButton')}
            </Button>
          )}
        </Box>
      </Box>

      {/* Main dashboard — role-specific view */}
      {selectedDay && (
        isNurse ? (
          <NurseDashboard
            episode={episode}
            clinicalDays={clinicalDays}
            selectedDay={selectedDay}
            onSelectDay={setSelectedDay}
            records={records}
            orders={orders}
            notes={notes}
            scaleResults={scaleResults}
            availableScales={availableScales}
            balanceItems={balanceItems}
            currentHour={currentHour}
            onSetCurrentHour={setCurrentHour}
            vitalForm={vitalForm}
            onVitalFormChange={setVitalForm}
            onSaveVitals={handleSaveVitals}
            autoSaving={autoSaving}
            onExecuteOrder={handleExecuteOrder}
            onCreateNote={handleCreateNote}
            onRecalculateBalance={handleRecalculateBalance}
            isLocked={isLocked}
            isNurse={isNurse}
            isDoctor={isDoctor}
            user={user}
          />
        ) : (
          <DoctorDashboard
            episode={episode}
            clinicalDays={clinicalDays}
            selectedDay={selectedDay}
            onSelectDay={setSelectedDay}
            records={records}
            orders={orders}
            notes={notes}
            scaleResults={scaleResults}
            availableScales={availableScales}
            balanceItems={balanceItems}
            currentHour={currentHour}
            onSetCurrentHour={setCurrentHour}
            vitalForm={vitalForm}
            onVitalFormChange={setVitalForm}
            onSaveVitals={handleSaveVitals}
            autoSaving={autoSaving}
            onCreateOrder={handleCreateOrder}
            onCancelOrder={handleCancelOrder}
            onCreateNote={handleCreateNote}
            labResults={labResults}
            ventilationSettings={ventilationSettings}
            patientStateAssessments={patientStateAssessments}
            onCreateLabResult={handleCreateLabResult}
            onSaveVentilation={handleSaveVentilation}
            onSavePatientState={handleSavePatientState}
            onCreateScaleResult={handleCreateScaleResult}
            onRecalculateBalance={handleRecalculateBalance}
            isLocked={isLocked}
            isNurse={isNurse}
            isDoctor={isDoctor}
            user={user}
          />
        )
      )}

      <Dialog open={reopenDialogOpen} onClose={() => setReopenDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{t('doctor.patientDay.reopenDialog.title', { dayNumber: selectedDay?.dayNumber })}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2, color: 'text.secondary' }}>
            {t('doctor.patientDay.reopenDialog.text')}
          </Typography>
          <TextField
            autoFocus
            label={t('doctor.patientDay.reopenDialog.reasonLabel')}
            fullWidth
            multiline
            rows={3}
            value={reopenReason}
            onChange={e => setReopenReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReopenDialogOpen(false)}>{t('doctor.patientDay.reopenDialog.cancelButton')}</Button>
          <Button onClick={handleReopen} variant="contained" color="warning" disabled={!reopenReason.trim()}>
            {t('doctor.patientDay.reopenDialog.reopenButton')}
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
