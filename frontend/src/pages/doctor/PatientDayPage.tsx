import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, Alert, CircularProgress, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Typography, useTheme, Paper } from '@mui/material';
import { ArrowBack, LockOpen, Download } from '@mui/icons-material';
import { episodeApi, clinicalDayApi, hourlyRecordApi, medicalOrderApi, fluidBalanceApi, pdfApi } from '../../api/endpoints';
import { useAuth } from '../../services/AuthContext';
import DoctorDashboard from '../../components/monitoring/DoctorDashboard';
import NurseDashboard from '../../components/monitoring/NurseDashboard';
import type { Episode, ClinicalDay, HourlyRecord, MedicalOrder, FluidBalanceItem } from '../../types';

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
  const [balanceItems, setFluidBalanceItems] = useState<FluidBalanceItem[]>([]);
  const [signConfirm, setSignConfirm] = useState(false);
  const [reopenDialogOpen, setReopenDialogOpen] = useState(false);
  const [reopenReason, setReopenReason] = useState('');
  const [loading, setLoading] = useState(true);

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
      const [recRes, ordRes, balRes] = await Promise.all([
        hourlyRecordApi.getByClinicalDay(day.id),
        medicalOrderApi.getByClinicalDay(day.id),
        fluidBalanceApi.getByClinicalDay(day.id),
      ]);
      setRecords(recRes.data);
      setOrders(ordRes.data);
      setFluidBalanceItems(balRes.data);
    } catch {
      // data stays stale on error
    }
  }, []);

  const handleRefresh = useCallback(() => {
    if (selectedDay) {
      loadDayData(selectedDay);
    }
  }, [selectedDay, loadDayData]);

  useEffect(() => {
    if (selectedDay) {
      loadDayData(selectedDay);
    }
  }, [selectedDay?.id]);

  useEffect(() => {
    document.title = episode ? `ICU — ${episode.patientName}` : 'ICU — Patient';
  }, [episode]);

  const handleSignOff = async () => {
    if (!selectedDay || !user) return;
    try {
      if (user.role === 'NURSE') {
        await clinicalDayApi.signNurse(selectedDay.id, { userId: user.id });
      } else {
        await clinicalDayApi.signDoctor(selectedDay.id, { userId: user.id });
      }
      setSignConfirm(false);
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
      await pdfApi.generate(selectedDay.id);
    } catch {
      // error handled silently
    }
  };

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;
  if (!episode) return <Alert severity="error">Епізод не знайдено</Alert>;

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
          {isNurse ? '← Назад до пацієнтів' : '← Назад до пацієнтів'}
        </Button>
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          {selectedDay && selectedDay.status === 'CLOSED' && (
            <Button size="small" variant="outlined" startIcon={<Download />} onClick={handleGeneratePDF}>
              PDF
            </Button>
          )}
          {canReopen && (
            <Button size="small" variant="outlined" color="warning" startIcon={<LockOpen />} onClick={() => setReopenDialogOpen(true)}>
              Перевідкрити
            </Button>
          )}
          {canSign && !signConfirm && (
            <Button size="small" variant="contained" onClick={() => setSignConfirm(true)} sx={{ fontWeight: 700 }}>
              Підписати добу
            </Button>
          )}
        </Box>
      </Box>

      {signConfirm && (
        <Paper elevation={3} sx={{ p: 2, mb: 2, border: `1px solid ${theme.palette.warning.main}`, borderRadius: 2 }}>
          <Typography variant="body2" sx={{ mb: 1 }}>
            Після підписання доба стане read-only
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button variant="contained" onClick={handleSignOff}>
              Підписати
            </Button>
            <Button variant="outlined" onClick={() => setSignConfirm(false)}>
              Скасувати
            </Button>
          </Box>
        </Paper>
      )}

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
            balanceItems={balanceItems}
            isLocked={isLocked}
            isNurse={isNurse}
            user={user}
            onRefresh={handleRefresh}
          />
        ) : (
          <DoctorDashboard
            episode={episode}
            clinicalDays={clinicalDays}
            selectedDay={selectedDay}
            onSelectDay={setSelectedDay}
            records={records}
            orders={orders}
            balanceItems={balanceItems}
            isLocked={isLocked}
            isNurse={isNurse}
            user={user}
            onRefresh={handleRefresh}
          />
        )
      )}

      <Dialog open={reopenDialogOpen} onClose={() => setReopenDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Повторне відкриття дня {selectedDay?.dayNumber}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2, color: 'text.secondary' }}>
            Ви впевнені, що хочете повторно відкрити цей клінічний день? Це скасує всі підписи.
          </Typography>
          <TextField
            autoFocus
            label="Причина повторного відкриття"
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

    </Box>
  );
}
