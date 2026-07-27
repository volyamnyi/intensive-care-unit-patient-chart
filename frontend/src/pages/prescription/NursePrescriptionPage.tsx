import { useEffect, useState, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Box, Typography, Button, CircularProgress, Alert, useTheme, Paper,
} from '@mui/material';
import { prescriptionApi } from '../../api/endpoints';
import PatientSearch from '../../components/common/PatientSearch';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PrescriptionList, PatientDto } from '../../types';

export default function NursePrescriptionPage() {
  const theme = useTheme();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const patientIdParam = searchParams.get('patientId');
  const patientId = patientIdParam ? Number(patientIdParam) : null;

  const [prescriptions, setPrescriptions] = useState<PrescriptionList[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadPrescriptions = useCallback(async () => {
    if (patientId === null) return;
    setLoading(true);
    setError(null);
    try {
      const res = await prescriptionApi.getByPatient(patientId);
      setPrescriptions(res.data);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити призначення'));
    } finally {
      setLoading(false);
    }
  }, [patientId]);

  useEffect(() => {
    loadPrescriptions();
  }, [loadPrescriptions]);

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary }}>
            Виконання призначень
          </Typography>
          <Typography variant="body2" sx={{ color: theme.palette.text.secondary, mt: 0.5 }}>
            {patientId ? `Пацієнт ID: ${patientId}` : 'Оберіть пацієнта'}
          </Typography>
        </Box>
      </Box>

      {error && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={
            /(modified|conflict|version|змінено|конфлікт|edited)/i.test(error) ? (
              <Button color="inherit" size="small" onClick={() => window.location.reload()}>
                Оновити сторінку
              </Button>
            ) : undefined
          }
        >
          {error}
        </Alert>
      )}

      {patientId === null ? (
        <Box sx={{ maxWidth: 500, mx: 'auto', mt: 4 }}>
          <PatientSearch onSelect={(p: PatientDto) => setSearchParams({ patientId: String(p.id) })} label="Пошук пацієнта" />
        </Box>
      ) : loading ? (
        <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
      ) : (
        <Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <Button size="small" onClick={() => { setSearchParams({}); setPrescriptions([]); }}>
              ← Змінити пацієнта
            </Button>
          </Box>
          {prescriptions.length === 0 ? (
            <Alert severity="info">У пацієнта немає листків призначень</Alert>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {prescriptions.map(p => (
                <Paper key={p.id} sx={{
                  p: 2, cursor: 'pointer', display: 'flex', justifyContent: 'space-between',
                  alignItems: 'center', '&:hover': { bgcolor: 'action.hover' },
                }}
                  onClick={() => navigate(`/prescriptions/nurse/${p.id}`)}
                >
                  <Box>
                    <Typography variant="body1" sx={{ fontWeight: 600 }}>{p.documentName}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      Пацієнт ID: {p.patientId} · {p.status === 'Finished' ? 'Закрито' : 'Відкрито'}
                    </Typography>
                  </Box>
                  <Button size="small">Відкрити</Button>
                </Paper>
              ))}
            </Box>
          )}
        </Box>
      )}
    </Box>
  );
}
