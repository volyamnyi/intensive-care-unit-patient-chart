import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Box, Typography, Button, CircularProgress, Alert, Divider, useTheme } from '@mui/material';
import { Close } from '@mui/icons-material';
import { prescriptionApi, vitalSignApi } from '../../api/endpoints';
import { useAuth } from '../../services/AuthContext';
import PrescriptionGrid, { type GridProps } from '../../components/prescription/PrescriptionGrid';
import VitalSignForm from '../../components/prescription/VitalSignForm';
import ClosePrescriptionDialog from '../../components/prescription/ClosePrescriptionDialog';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PrescriptionList, PrescriptionItem, AllergyItem, VitalSignEntry } from '../../types';

export default function PrescriptionDetailPage() {
  const theme = useTheme();
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const isNurseUser = user?.role === 'NURSE';

  const [prescription, setPrescription] = useState<PrescriptionList | null>(null);
  const [items, setItems] = useState<PrescriptionItem[]>([]);
  const [allergies, setAllergies] = useState<AllergyItem[]>([]);
  const [latestVitalSign, setLatestVitalSign] = useState<VitalSignEntry | null>(null);
  const [loading, setLoading] = useState(false);
  const [closing, setClosing] = useState(false);
  const [closeDialogOpen, setCloseDialogOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isFinished = prescription?.status === 'Finished';

  const loadItems = useCallback(async (listId: string) => {
    try {
      const res = await prescriptionApi.getItems(listId);
      setItems(res.data);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити препарати'));
    }
  }, []);

  const loadAllergies = useCallback(async (patientId: number) => {
    try {
      const res = await prescriptionApi.getAllergies(patientId);
      setAllergies(res.data);
    } catch {
      // allergies are optional
    }
  }, []);

  const loadLatestVitalSign = useCallback(async (listId: string) => {
    try {
      const daysRes = await vitalSignApi.getByPrescriptionList(listId);
      if (daysRes.data.length === 0) return;
      const entriesRes = await vitalSignApi.getEntries(daysRes.data[0].id);
      const filled = entriesRes.data.filter((e) => e.temperature != null || e.systolicBp != null || e.pulse != null);
      if (filled.length > 0) setLatestVitalSign(filled[filled.length - 1]);
    } catch { /* vital signs optional */ }
  }, []);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError(null);
    prescriptionApi.getById(id)
      .then((res) => {
        setPrescription(res.data);
        void loadItems(res.data.id);
        void loadAllergies(res.data.patientId);
        void loadLatestVitalSign(res.data.id);
      })
      .catch((err) => setError(getErrorMessage(err, 'Не вдалося завантажити листок призначень')))
      .finally(() => setLoading(false));
  }, [id, loadItems, loadAllergies, loadLatestVitalSign]);

  const handlePlan = async (dayPartId: string, dose: string) => {
    setError(null);
    try {
      await prescriptionApi.planDose(dayPartId, dose);
      if (id) await loadItems(id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося запланувати дозу'));
    }
  };

  const handleCancel = async (dayPartId: string) => {
    setError(null);
    try {
      await prescriptionApi.cancelDose(dayPartId);
      if (id) await loadItems(id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося скасувати дозу'));
    }
  };

  const handleAddItem = async (data: { medicineName: string; medicineMethod?: string; regime?: string }) => {
    if (!id) return;
    setError(null);
    try {
      await prescriptionApi.addItem(id, data);
      await loadItems(id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося додати препарат'));
    }
  };

  const handleRemoveItem = async (itemId: string) => {
    setError(null);
    try {
      await prescriptionApi.removeItem(itemId);
      const item = items.find(i => i.id === itemId);
      if (item && id) await loadItems(id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося видалити препарат'));
    }
  };

  const handleVitalSignSubmit = async (data: { temperature?: number; systolicBp?: number; diastolicBp?: number; spo2?: number; pulse?: number; stool?: string; painScore?: number }) => {
    if (!id) return;
    setError(null);
    try {
      await vitalSignApi.create({ ...data, prescriptionListId: id });
      await loadLatestVitalSign(id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося зберегти показники'));
    }
  };

  const handleExecute: GridProps['onExecute'] = async (dayPartId, actualDose, requires2p, secondPersonId) => {
    setError(null);
    try {
      await prescriptionApi.executeDose(dayPartId, { actualDose, requires2pAuth: requires2p, secondPersonId });
      if (id) await loadItems(id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося виконати дозу'));
    }
  };

  const handleClose = async () => {
    if (!id) return;
    setClosing(true);
    setError(null);
    try {
      const res = await prescriptionApi.close(id);
      setPrescription(res.data);
      setCloseDialogOpen(false);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося закрити листок'));
    } finally {
      setClosing(false);
    }
  };

  if (loading && !prescription) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;
  if (!prescription) return <Alert severity="info">Листок призначень не знайдено</Alert>;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2, alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary }}>
            {prescription.documentName}
          </Typography>
          <Typography variant="body2" sx={{ color: theme.palette.text.secondary, mt: 0.5 }}>
            Пацієнт ID: {prescription.patientId} · Статус: {prescription.status === 'Finished' ? 'Закрито' : 'Відкрито'}
          </Typography>
        </Box>
        {!isNurseUser && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {!isFinished && (
              <Button variant="contained" color="warning" startIcon={<Close />} onClick={() => setCloseDialogOpen(true)}>
                Закрити листок
              </Button>
            )}
          </Box>
        )}
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

      <PrescriptionGrid
        items={items}
        canEdit={!isFinished}
        isDoctor={!isNurseUser}
        isNurse={isNurseUser}
        onPlan={handlePlan}
        onCancel={handleCancel}
        onExecute={isNurseUser ? handleExecute : undefined}
        onAddItem={isNurseUser ? async () => {} : handleAddItem}
        onRemoveItem={isNurseUser ? async () => {} : handleRemoveItem}
        onSearchMedicine={(keyword) => prescriptionApi.getMedicineCatalog(keyword).then(r => r.data)}
        allergies={isNurseUser ? [] : allergies}
        loading={loading}
      />

      {!isNurseUser && (
        <>
          <Divider sx={{ my: 3 }} />

          <VitalSignForm latest={latestVitalSign} onSubmit={handleVitalSignSubmit} disabled={isFinished} />

          <ClosePrescriptionDialog
            open={closeDialogOpen}
            onClose={() => setCloseDialogOpen(false)}
            onConfirm={handleClose}
            allCompleted={false}
            closing={closing}
          />
        </>
      )}
    </Box>
  );
}
