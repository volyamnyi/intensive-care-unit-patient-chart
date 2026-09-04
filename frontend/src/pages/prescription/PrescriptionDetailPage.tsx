import { useEffect, useState, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { Loader2, X, RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Alert, AlertAction } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'

import { prescriptionApi, vitalSignApi } from '../../api/medication';
import { useAuth } from '../../services/AuthContext'
import PrescriptionGrid, { type GridProps } from '../../components/prescription/PrescriptionGrid'
import VitalSignGrid from '../../components/prescription/VitalSignGrid'
import ClosePrescriptionDialog from '../../components/prescription/ClosePrescriptionDialog'
import { getErrorMessage } from '../../utils/errorMessage'
import type { PrescriptionList, PrescriptionItem, AllergyItem } from '../../types/medication';

export default function PrescriptionDetailPage() {
  useEffect(() => { document.title = 'Призначення — Деталі' }, [])
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const isNurseUser = user?.role === 'NURSE'

  const [prescription, setPrescription] = useState<PrescriptionList | null>(null)
  const [items, setItems] = useState<PrescriptionItem[]>([])
  const [allergies, setAllergies] = useState<AllergyItem[]>([])
  const [vitalDays, setVitalDays] = useState<{ id: string; dayDate: string; entries: import('../../types/medication').VitalSignEntry[] }[]>([])
  const [loading, setLoading] = useState(false)
  const [vitalLoading, setVitalLoading] = useState(false)
  const [closing, setClosing] = useState(false)
  const [closeDialogOpen, setCloseDialogOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const isFinished = prescription?.status === 'Finished'

  const loadItems = useCallback(async (listId: string) => {
    try {
      const res = await prescriptionApi.getItems(listId)
      setItems(res.data)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити препарати'))
    }
  }, [])

  const loadAllergies = useCallback(async (patientId: number) => {
    try {
      const res = await prescriptionApi.getAllergies(patientId)
      setAllergies(res.data)
    } catch {
      // allergies are optional
    }
  }, [])

  const loadVitalGrid = useCallback(async (listId: string) => {
    try {
      setVitalLoading(true)
      const res = await vitalSignApi.getGrid(listId)
      setVitalDays(res.data)
    } catch { /* vital signs optional */
    } finally {
      setVitalLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!id) return
    setLoading(true)
    setError(null)
    prescriptionApi.getById(id)
      .then((res) => {
        setPrescription(res.data)
        void loadItems(res.data.id)
        void loadAllergies(res.data.patientId)
        void loadVitalGrid(res.data.id)
      })
      .catch((err) => setError(getErrorMessage(err, 'Не вдалося завантажити листок призначень')))
      .finally(() => setLoading(false))
  }, [id, loadItems, loadAllergies, loadVitalGrid])

  const handlePlan = async (dayPartId: string, dose: string) => {
    setError(null)
    try {
      await prescriptionApi.planDose(dayPartId, dose)
      if (id) await loadItems(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося запланувати дозу'))
    }
  }

  const handleCancelMedication = async (dayPartId: string) => {
    setError(null)
    try {
      await prescriptionApi.cancelMedication(dayPartId)
      if (id) await loadItems(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося відмінити препарат'))
    }
  }

  const handleRestoreToPlanned = async (dayPartId: string) => {
    setError(null)
    try {
      await prescriptionApi.restoreToPlanned(dayPartId)
      if (id) await loadItems(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося повернути у заплановані'))
    }
  }

  const handleAddItem = async (data: { medicineName: string; medicineMethod?: string; regime?: string }) => {
    if (!id) return
    setError(null)
    try {
      await prescriptionApi.addItem(id, data)
      await loadItems(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося додати препарат'))
    }
  }

  const handleRemoveItem = async (itemId: string) => {
    setError(null)
    try {
      await prescriptionApi.removeItem(itemId)
      const item = items.find(i => i.id === itemId)
      if (item && id) await loadItems(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося видалити препарат'))
    }
  }

  const handleAddDay = async (itemId: string) => {
    if (!id) return
    setError(null)
    try {
      await prescriptionApi.addItemDay(itemId)
      await loadItems(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося додати день'))
    }
  }

  const handleCancelAssignment = async (dayPartId: string) => {
    setError(null)
    try {
      await prescriptionApi.cancelAssignment(dayPartId)
      if (id) await loadItems(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося відмінити призначення'))
    }
  }

  const handleCellUpdate = async (dayId: string, period: string, paramKey: string, value: string) => {
    if (!id) return
    setError(null)
    const numericKey = paramKey !== 'stool'
    const numValue = numericKey ? (value ? Number(value) : null) : (value || null)
    try {
      await vitalSignApi.updateCell(dayId, period, {
        [paramKey]: numValue,
      } as Record<string, unknown> as { temperature?: number; systolicBp?: number; diastolicBp?: number; spo2?: number; pulse?: number; stool?: string; painScore?: number })
      await loadVitalGrid(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося зберегти показник'))
    }
  }

  const handleExecute: GridProps['onExecute'] = async (dayPartId, actualDose, secondPersonLogin, secondPersonPassword) => {
    setError(null)
    try {
      await prescriptionApi.executeDose(dayPartId, { actualDose, secondPersonLogin, secondPersonPassword })
      if (id) await loadItems(id)
    } catch (err) {
      throw err
    }
  }

  const handleClose = async () => {
    if (!id) return
    setClosing(true)
    setError(null)
    try {
      const res = await prescriptionApi.close(id)
      setPrescription(res.data)
      setCloseDialogOpen(false)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося закрити листок'))
    } finally {
      setClosing(false)
    }
  }

  if (loading && !prescription) return <Loader2 className="mx-auto mt-4 size-6 animate-spin text-primary" />
  if (!prescription) return <Alert>Листок призначень не знайдено</Alert>

  return (
    <div>
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="font-rubik text-2xl font-extrabold text-foreground">
            {prescription.documentName}
          </h1>
          <p className="mt-0.5 text-sm text-muted-foreground">
            Пацієнт ID: {prescription.patientId} · Статус: {prescription.status === 'Finished' ? 'Закрито' : 'Відкрито'}
          </p>
        </div>
        {!isNurseUser && (
          <div className="flex items-center gap-1">
            {!isFinished && (
              <Button variant="secondary" onClick={() => setCloseDialogOpen(true)}>
                <X />
                Закрити листок
              </Button>
            )}
          </div>
        )}
      </div>

      {error && (
        <Alert variant="destructive" className="mb-2">
          {error}
          {/(modified|conflict|version|змінено|конфлікт|edited)/i.test(error) && (
            <AlertAction>
              <Button variant="ghost" size="xs" onClick={() => window.location.reload()}>
                <RefreshCw />
                Оновити сторінку
              </Button>
            </AlertAction>
          )}
        </Alert>
      )}

      <PrescriptionGrid
        items={items}
        canEdit={!isFinished}
        isDoctor={!isNurseUser}
        isNurse={isNurseUser}
        onPlan={handlePlan}
        onCancelMedication={handleCancelMedication}
        onRestoreToPlanned={handleRestoreToPlanned}
        onCancelAssignment={handleCancelAssignment}
        onAddDay={isNurseUser ? undefined : handleAddDay}
        onExecute={isNurseUser ? handleExecute : undefined}
        onAddItem={isNurseUser ? async () => {} : handleAddItem}
        onRemoveItem={isNurseUser ? async () => {} : handleRemoveItem}
        onSearchMedicine={(keyword) => prescriptionApi.getMedicineCatalog(keyword).then(r => r.data)}
        allergies={isNurseUser ? [] : allergies}
        loading={loading}
      />

      {!isNurseUser && (
        <>
          <Separator className="my-3" />

          <h2 className="font-rubik text-lg font-semibold mb-1.5">
            Життєві показники
          </h2>
          <VitalSignGrid
            days={vitalDays}
            canEdit={!isFinished}
            isDoctor={true}
            onCellUpdate={handleCellUpdate}
            loading={vitalLoading}
          />

          <ClosePrescriptionDialog
            open={closeDialogOpen}
            onClose={() => setCloseDialogOpen(false)}
            onConfirm={handleClose}
            allCompleted={false}
            closing={closing}
          />
        </>
      )}
    </div>
  )
}
