import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box, Typography, Button, CircularProgress, Alert, Paper, Divider, useTheme,
} from '@mui/material';
import { Close } from '@mui/icons-material';
import { prescriptionApi, vitalSignApi } from '../../api/endpoints';
import PrescriptionItemForm from '../../components/prescription/PrescriptionItemForm';
import PrescriptionItemTable from '../../components/prescription/PrescriptionItemTable';
import DayPartPlanner from '../../components/prescription/DayPartPlanner';
import { allDayPartsCompleted } from '../../utils/prescriptionDayParts';
import VitalSignForm from '../../components/prescription/VitalSignForm';
import ClosePrescriptionDialog from '../../components/prescription/ClosePrescriptionDialog';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PrescriptionList, PrescriptionItem, AllergyItem, PrescriptionDayPart, VitalSignEntry } from '../../types';

export default function PrescriptionDetailPage() {
  const theme = useTheme();
  const { id } = useParams<{ id: string }>();

  const [prescription, setPrescription] = useState<PrescriptionList | null>(null);
  const [items, setItems] = useState<PrescriptionItem[]>([]);
  const [allergies, setAllergies] = useState<AllergyItem[]>([]);
  const [latestVitalSign, setLatestVitalSign] = useState<VitalSignEntry | null>(null);
  const [loading, setLoading] = useState(false);
  const [addingItem, setAddingItem] = useState(false);
  const [planning, setPlanning] = useState(false);
  const [completing, setCompleting] = useState(false);
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
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити алергії'));
    }
  }, []);

  const loadLatestVitalSign = useCallback(async (listId: string) => {
    try {
      const daysRes = await vitalSignApi.getByPrescriptionList(listId);
      if (daysRes.data.length === 0) return;
      const entriesRes = await vitalSignApi.getEntries(daysRes.data[0].id);
      const filled = entriesRes.data.filter((e) => e.temperature != null || e.systolicBp != null || e.pulse != null);
      if (filled.length > 0) {
        setLatestVitalSign(filled[filled.length - 1]);
      }
    } catch {
      // vital signs are optional; ignore errors
    }
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

  const handleAddItem = async (data: { medicineName: string; medicineMethod?: string; regime?: string }) => {
    if (!id) return;
    setAddingItem(true);
    setError(null);
    try {
      await prescriptionApi.addItem(id, data);
      await loadItems(id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося додати препарат'));
    } finally {
      setAddingItem(false);
    }
  };

  const handleDeleteItem = async (item: PrescriptionItem) => {
    setError(null);
    try {
      await prescriptionApi.removeItem(item.id);
      await loadItems(item.listId);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося видалити препарат'));
    }
  };

  const handlePlan = async (dayPartId: string, dose: string) => {
    setPlanning(true);
    setError(null);
    try {
      await prescriptionApi.planDose(dayPartId, dose);
      if (id) await loadItems(id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося запланувати дозу'));
    } finally {
      setPlanning(false);
    }
  };

  const handleComplete = async (dayPartId: string) => {
    setCompleting(true);
    setError(null);
    try {
      await prescriptionApi.completeDose(dayPartId);
      if (id) await loadItems(id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завершити дозу'));
    } finally {
      setCompleting(false);
    }
  };

  const handleVitalSignSubmit = async (data: { temperature?: number; systolicBp?: number; diastolicBp?: number; spo2?: number; pulse?: number; stool?: string; painScore?: number }) => {
    if (!id) return;
    setError(null);
    try {
      await vitalSignApi.create({ ...data, prescriptionListId: id });
      await loadLatestVitalSign(id);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося зберегти життєві показники'));
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
      setError(getErrorMessage(err, 'Не вдалося закрити листок призначень'));
    } finally {
      setClosing(false);
    }
  };

  const dayParts = items.flatMap((item) => item.dayParts ?? [] as PrescriptionDayPart[]);
  const allCompleted = allDayPartsCompleted(dayParts);

  if (loading && !prescription) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;
  if (!prescription) return <Alert severity="info">Листок призначень не знайдено</Alert>;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3, alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary }}>
            {prescription.documentName}
          </Typography>
          <Typography variant="body2" sx={{ color: theme.palette.text.secondary, mt: 0.5 }}>
            {`Пацієнт ID: ${prescription.patientId} • Статус: ${prescription.status === 'Finished' ? 'Закрито' : 'Збережено'}`}
          </Typography>
        </Box>
        {!isFinished && (
          <Button variant="contained" color="warning" startIcon={<Close />} onClick={() => setCloseDialogOpen(true)}>
            Закрити
          </Button>
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Paper sx={{ p: 2, mb: 2 }}>
        <Typography variant="subtitle1" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>Препарати</Typography>
        {!isFinished && (
          <PrescriptionItemForm
            onSubmit={handleAddItem}
            onSearchMedicine={(keyword) => prescriptionApi.getMedicineCatalog(keyword).then((res) => res.data)}
            allergies={allergies}
            disabled={addingItem}
          />
        )}
        <Box sx={{ mt: 2 }}>
          <PrescriptionItemTable
            items={items}
            onDelete={isFinished ? undefined : handleDeleteItem}
            canEdit={!isFinished}
          />
        </Box>
      </Paper>

      <Paper sx={{ p: 2, mb: 2 }}>
        <Typography variant="subtitle1" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>Планування частин доби</Typography>
        <DayPartPlanner
          dayParts={dayParts}
          onPlan={handlePlan}
          onComplete={handleComplete}
          canPlan={!isFinished}
          canComplete={!isFinished}
          planning={planning}
          completing={completing}
        />
      </Paper>

      <Divider sx={{ my: 2 }} />

      <VitalSignForm latest={latestVitalSign} onSubmit={handleVitalSignSubmit} disabled={isFinished} />

      <ClosePrescriptionDialog
        open={closeDialogOpen}
        onClose={() => setCloseDialogOpen(false)}
        onConfirm={handleClose}
        allCompleted={allCompleted}
        closing={closing}
      />
    </Box>
  );
}
