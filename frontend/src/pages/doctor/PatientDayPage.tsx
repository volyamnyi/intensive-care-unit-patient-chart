import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, Alert, CircularProgress, TextField, Typography, useTheme, Paper } from '@mui/material';
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
  const [reopenOpen, setReopenOpen] = useState(false);
  const [reopenReason, setReopenReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [dayLoading, setDayLoading] = useState(false);
  const [signingLoading, setSigningLoading] = useState(false);
  const [reopenLoading, setReopenLoading] = useState(false);
  const [feedback, setFeedback] = useState<{ message: string; severity: 'success' | 'error' } | null>(null);

  useEffect(() => {
    if (!episodeId) return;
    setLoading(true);
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
    }).catch(() => {
      // Episode not found — component renders "Епізод не знайдено" via !episode state
    }).finally(() => setLoading(false));
  }, [episodeId, user]);

  const loadDayData = useCallback(async (day: ClinicalDay) => {
    setDayLoading(true);
    try {
      const [recRes, ordRes, balRes] = await Promise.all([
        hourlyRecordApi.getByClinicalDay(day.id),
        medicalOrderApi.getByClinicalDay(day.id),
        fluidBalanceApi.getByClinicalDay(day.id),
      ]);
      setRecords(recRes.data);
      setOrders(ordRes.data);
      setFluidBalanceItems(balRes.data);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Не вдалося завантажити дані доби';
      setFeedback({ message: msg, severity: 'error' });
    } finally {
      setDayLoading(false);
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
  }, [selectedDay, loadDayData]);

  useEffect(() => {
    document.title = episode ? `ICU — ${episode.patientName}` : 'ICU — Patient';
  }, [episode]);

  const handleSignOff = async () => {
    if (!selectedDay || !user) return;
    setSigningLoading(true);
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
      setFeedback({ message: 'Добу підписано', severity: 'success' });
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Не вдалося підписати добу';
      setFeedback({ message: msg, severity: 'error' });
    } finally {
      setSigningLoading(false);
    }
  };

  const handleReopen = async () => {
    if (!selectedDay || !user || !reopenReason.trim()) return;
    setReopenLoading(true);
    try {
      await clinicalDayApi.reopen(selectedDay.id, { reason: reopenReason.trim(), version: selectedDay.version });
      setReopenOpen(false);
      setReopenReason('');
      const daysRes = await episodeApi.getClinicalDays(episodeId!);
      setClinicalDays(daysRes.data);
      const updated = daysRes.data.find(d => d.id === selectedDay.id);
      if (updated) setSelectedDay(updated);
      setFeedback({ message: 'Добу перевідкрито', severity: 'success' });
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Не вдалося перевідкрити добу';
      setFeedback({ message: msg, severity: 'error' });
    } finally {
      setReopenLoading(false);
    }
  };

  const handleGeneratePDF = async () => {
    if (!selectedDay) return;
    try {
      await pdfApi.generate(selectedDay.id);
      setFeedback({ message: 'PDF успішно згенеровано', severity: 'success' });
    } catch {
      setFeedback({ message: 'Не вдалося згенерувати PDF', severity: 'error' });
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
        <Box sx={{ display: 'flex', gap: 0.5, alignItems: 'center' }}>
          {dayLoading && <CircularProgress size={16} sx={{ mr: 0.5 }} />}
          {selectedDay && selectedDay.status === 'CLOSED' && (
            <Button size="small" variant="outlined" startIcon={<Download />} onClick={handleGeneratePDF}>
              PDF
            </Button>
          )}
          {canReopen && (
            <Button size="small" variant="outlined" color="warning" startIcon={<LockOpen />} onClick={() => setReopenOpen(v => !v)} disabled={reopenLoading}>
              {reopenOpen ? 'Скасувати' : 'Перевідкрити'}
            </Button>
          )}
          {canSign && !signConfirm && (
            <Button size="small" variant="contained" onClick={() => setSignConfirm(true)} sx={{ fontWeight: 700 }} disabled={signingLoading}>
              {signingLoading ? <CircularProgress size={14} sx={{ mr: 0.5 }} /> : null}
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
            <Button variant="contained" onClick={handleSignOff} disabled={signingLoading}>
              {signingLoading ? <CircularProgress size={14} sx={{ mr: 0.5 }} /> : null}
              Підписати
            </Button>
            <Button variant="outlined" onClick={() => setSignConfirm(false)} disabled={signingLoading}>
              Скасувати
            </Button>
          </Box>
        </Paper>
      )}

      {/* Main dashboard — role-specific view */}
      {selectedDay && (
        <Box sx={{ position: 'relative' }}>
          {dayLoading && (
            <Box sx={{
              position: 'absolute', inset: 0, zIndex: 10,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              bgcolor: theme.palette.mode === 'dark' ? 'rgba(0,0,0,0.4)' : 'rgba(255,255,255,0.6)',
              borderRadius: 2,
            }}>
              <CircularProgress size={24} />
            </Box>
          )}
          {isNurse ? (
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
              onFeedback={(msg, sev) => setFeedback({ message: msg, severity: sev })}
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
              onFeedback={(msg, sev) => setFeedback({ message: msg, severity: sev })}
            />
          )}
        </Box>
      )}

      {reopenOpen && selectedDay && (
        <Paper elevation={3} sx={{ p: 2, mb: 2, border: '1px solid #FF9800', borderRadius: 2 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
            Повторне відкриття дня {selectedDay.dayNumber}
          </Typography>
          <Typography variant="body2" sx={{ mb: 2, color: 'text.secondary' }}>
            Це скасує всі підписи. Вкажіть причину:
          </Typography>
          <TextField
            autoFocus
            label="Причина"
            fullWidth
            multiline
            rows={2}
            value={reopenReason}
            onChange={e => setReopenReason(e.target.value)}
          />
          <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
            <Button variant="contained" color="warning" onClick={handleReopen} disabled={!reopenReason.trim() || reopenLoading}>
              {reopenLoading ? <CircularProgress size={14} sx={{ mr: 0.5 }} /> : null}
              Перевідкрити
            </Button>
            <Button variant="outlined" onClick={() => setReopenOpen(false)} disabled={reopenLoading}>
              Скасувати
            </Button>
          </Box>
        </Paper>
      )}

      {feedback && (
        <Box sx={{ position: 'fixed', top: 0, left: 0, right: 0, zIndex: 9999, display: 'flex', justifyContent: 'center', pt: 1 }}>
          <Alert severity={feedback.severity} variant="filled" sx={{ fontSize: 13 }} onClose={() => setFeedback(null)}>
            {feedback.message}
          </Alert>
        </Box>
      )}
    </Box>
  );
}
