import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Box, Typography, Button, CircularProgress, Alert, Paper, useTheme } from '@mui/material';
import { prescriptionApi } from '../../api/endpoints';
import PrescriptionTable from '../../components/prescription/PrescriptionTable';
import PrescriptionExecutionPanel from '../../components/prescription/PrescriptionExecutionPanel';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PrescriptionList, PrescriptionItem, PrescriptionDayPart } from '../../types';

export default function NursePrescriptionPage() {
  const theme = useTheme();
  const [searchParams] = useSearchParams();
  const patientIdParam = searchParams.get('patientId');
  const patientId = patientIdParam ? Number(patientIdParam) : null;

  const [prescriptions, setPrescriptions] = useState<PrescriptionList[]>([]);
  const [selected, setSelected] = useState<PrescriptionList | null>(null);
  const [items, setItems] = useState<PrescriptionItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [executing, setExecuting] = useState(false);
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

  const loadItems = useCallback(async (listId: string) => {
    try {
      const res = await prescriptionApi.getItems(listId);
      setItems(res.data);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити препарати'));
    }
  }, []);

  const handleSelect = (prescription: PrescriptionList) => {
    setSelected(prescription);
    setItems([]);
    void loadItems(prescription.id);
  };

  const handleExecute = async (dayPartId: string, actualDose: string, requires2pAuth: boolean, secondPersonId?: string) => {
    setExecuting(true);
    setError(null);
    try {
      await prescriptionApi.executeDose(dayPartId, { actualDose, requires2pAuth, secondPersonId });
      if (selected) await loadItems(selected.id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося виконати дозу'));
    } finally {
      setExecuting(false);
    }
  };

  const dayParts = items.flatMap((item) => item.dayParts ?? [] as PrescriptionDayPart[]);

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary }}>
          Виконання призначень
        </Typography>
        <Typography variant="body2" sx={{ color: theme.palette.text.secondary, mt: 0.5 }}>
          {patientId ? `Пацієнт ID: ${patientId}` : 'Оберіть пацієнта для перегляду призначень'}
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {patientId === null ? (
        <Alert severity="info">Для виконання призначень перейдіть з картки пацієнта.</Alert>
      ) : loading ? (
        <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
      ) : (
        <PrescriptionTable prescriptions={prescriptions} onSelect={handleSelect} />
      )}

      {selected && (
        <Paper sx={{ p: 2, mt: 3 }}>
          <Typography variant="subtitle1" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>
            {selected.documentName}
          </Typography>
          <Button variant="outlined" size="small" sx={{ mb: 2 }} onClick={() => { setSelected(null); setItems([]); }}>
            Назад до списку
          </Button>
          <PrescriptionExecutionPanel dayParts={dayParts} onExecute={handleExecute} executing={executing} />
        </Paper>
      )}
    </Box>
  );
}
