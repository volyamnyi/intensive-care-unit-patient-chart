import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Box, Typography, Button, CircularProgress, Alert, useTheme, Paper,
} from '@mui/material';
import { prescriptionApi } from '../../api/endpoints';
import PatientSearch from '../../components/common/PatientSearch';
import PrescriptionGrid, { type GridProps } from '../../components/prescription/PrescriptionGrid';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PrescriptionList, PrescriptionItem, PatientDto } from '../../types';

export default function NursePrescriptionPage() {
  const theme = useTheme();
  const [searchParams, setSearchParams] = useSearchParams();
  const patientIdParam = searchParams.get('patientId');
  const patientId = patientIdParam ? Number(patientIdParam) : null;

  const [prescriptions, setPrescriptions] = useState<PrescriptionList[]>([]);
  const [selected, setSelected] = useState<PrescriptionList | null>(null);
  const [items, setItems] = useState<PrescriptionItem[]>([]);
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

  const handleSelect = async (p: PrescriptionList) => {
    setSelected(p);
    setError(null);
    try {
      const res = await prescriptionApi.getItems(p.id);
      setItems(res.data);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити препарати'));
    }
  };

  const handlePlan = async (dayPartId: string, dose: string) => {
    setError(null);
    try {
      await prescriptionApi.planDose(dayPartId, dose);
      if (selected) await handleSelect(selected);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося запланувати'));
    }
  };

  const handleComplete = async (dayPartId: string) => {
    setError(null);
    try {
      await prescriptionApi.completeDose(dayPartId);
      if (selected) await handleSelect(selected);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завершити'));
    }
  };

  const handleExecute: GridProps['onExecute'] = async (dayPartId, actualDose, requires2p, secondPersonId) => {
    setError(null);
    try {
      await prescriptionApi.executeDose(dayPartId, { actualDose, requires2pAuth: requires2p, secondPersonId });
      if (selected) await handleSelect(selected);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося виконати дозу'));
    }
  };

  const isFinished = selected?.status === 'Finished';

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

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {/* patient search */}
      {patientId === null ? (
        <Box sx={{ maxWidth: 500, mx: 'auto', mt: 4 }}>
          <PatientSearch onSelect={(p: PatientDto) => setSearchParams({ patientId: String(p.id) })} label="Пошук пацієнта" />
        </Box>
      ) : selected ? (
        /* detail view with grid */
        <Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2, alignItems: 'center' }}>
            <Typography variant="body1" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 600 }}>
              {selected.documentName}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button size="small" onClick={() => { setSelected(null); setItems([]); }}>
                ← До списку листків
              </Button>
              <Button size="small" onClick={() => { setSearchParams({}); setPrescriptions([]); setSelected(null); }}>
                ← Змінити пацієнта
              </Button>
            </Box>
          </Box>

          <PrescriptionGrid
            items={items}
            canEdit={!isFinished}
            isDoctor={false}
            isNurse
            onPlan={handlePlan}
            onComplete={handleComplete}
            onExecute={handleExecute}
            onAddItem={async () => {}}
            onRemoveItem={async () => {}}
            onSearchMedicine={(q) => prescriptionApi.getMedicineCatalog(q).then(r => r.data)}
            allergies={[]}
            loading={loading}
          />
        </Box>
      ) : loading ? (
        <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
      ) : (
        /* list view */
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
                  onClick={() => { void handleSelect(p); }}
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
