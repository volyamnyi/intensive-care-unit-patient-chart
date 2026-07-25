import { useEffect, useState, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Box, Typography, Button, CircularProgress, Alert, useTheme } from '@mui/material';
import { Add } from '@mui/icons-material';
import { prescriptionApi } from '../../api/endpoints';
import PrescriptionTable from '../../components/prescription/PrescriptionTable';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PrescriptionList } from '../../types';

export default function PrescriptionPage() {
  const theme = useTheme();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const patientIdParam = searchParams.get('patientId');
  const patientId = patientIdParam ? Number(patientIdParam) : null;

  const [prescriptions, setPrescriptions] = useState<PrescriptionList[]>([]);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadPrescriptions = useCallback(async () => {
    if (patientId === null) {
      setPrescriptions([]);
      return;
    }
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

  const handleCreate = async () => {
    if (patientId === null) return;
    setCreating(true);
    setError(null);
    try {
      const res = await prescriptionApi.create({ patientId: String(patientId) });
      navigate(`/prescriptions/doctor/${res.data.id}`);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося створити листок призначень'));
    } finally {
      setCreating(false);
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3, alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary }}>
            Листки призначень
          </Typography>
          <Typography variant="body2" sx={{ color: theme.palette.text.secondary, mt: 0.5 }}>
            {patientId ? `Пацієнт ID: ${patientId}` : 'Оберіть пацієнта для перегляду призначень'}
          </Typography>
        </Box>
        {patientId && (
          <Button variant="contained" onClick={handleCreate} startIcon={<Add />} disabled={creating}>
            {creating ? 'Створення...' : 'Новий листок'}
          </Button>
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {patientId === null ? (
        <Alert severity="info">Для перегляду призначень перейдіть з картки пацієнта.</Alert>
      ) : loading ? (
        <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
      ) : (
        <PrescriptionTable
          prescriptions={prescriptions}
          onSelect={(p) => navigate(`/prescriptions/doctor/${p.id}`)}
        />
      )}
    </Box>
  );
}
