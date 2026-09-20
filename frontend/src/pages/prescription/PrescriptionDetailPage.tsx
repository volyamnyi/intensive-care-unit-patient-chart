import { useEffect, useState, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { Loader2, X, RefreshCw, Download, Printer } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Alert, AlertAction, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'

import { prescriptionApi, vitalSignApi } from '../../api/medication';
import { printPdfBlob } from '../../lib/printPdf'
import { useAuth } from '../../services/AuthContext'
import PrescriptionGrid, { type GridProps } from '../../components/prescription/PrescriptionGrid'
import VitalSignGrid from '../../components/prescription/VitalSignGrid'
import ClosePrescriptionDialog from '../../components/prescription/ClosePrescriptionDialog'
import { getErrorMessage } from '../../utils/errorMessage'
import type { PrescriptionList, PrescriptionItem } from '../../types/medication';

export default function PrescriptionDetailPage() {
  useEffect(() => { document.title = 'Призначення — Деталі' }, [])
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const isNurseUser = user?.role === 'NURSE'

  const [prescription, setPrescription] = useState<PrescriptionList | null>(null)
  const [items, setItems] = useState<PrescriptionItem[]>([])
  const [interactions, setInteractions] = useState<import('../../types/medication').PrescriptionInteractionsResponse | null>(null)
  const [vitalDays, setVitalDays] = useState<{ id: string; dayDate: string; entries: import('../../types/medication').VitalSignEntry[] }[]>([])
  const [loading, setLoading] = useState(false)
  const [vitalLoading, setVitalLoading] = useState(false)
  const [closing, setClosing] = useState(false)
  const [closeDialogOpen, setCloseDialogOpen] = useState(false)
  const [pdfBusy, setPdfBusy] = useState(false)
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

  // Interaction warnings are non-blocking: a fetch failure never hides the grid.
  const loadInteractions = useCallback(async (listId: string) => {
    try {
      const res = await prescriptionApi.getInteractions(listId)
      setInteractions(res.data)
    } catch { /* non-blocking */ }
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
        void loadInteractions(res.data.id)
        void loadVitalGrid(res.data.id)
      })
      .catch((err) => setError(getErrorMessage(err, 'Не вдалося завантажити листок призначень')))
      .finally(() => setLoading(false))
  }, [id, loadItems, loadInteractions, loadVitalGrid])

  // Any mutation of the planned set changes the interaction warnings — refetch both.
  const reloadAfterMutation = useCallback(async (listId: string) => {
    await loadItems(listId)
    await loadInteractions(listId)
  }, [loadItems, loadInteractions])

  const handlePlan = async (dayPartId: string, dose: string) => {
    setError(null)
    try {
      await prescriptionApi.planDose(dayPartId, dose)
      if (id) await reloadAfterMutation(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося запланувати дозу'))
    }
  }

  const handleCancelMedication = async (dayPartId: string) => {
    try {
      await prescriptionApi.cancelMedication(dayPartId)
      if (id) await reloadAfterMutation(id)
      toast.success('Препарат відмінено')
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося відмінити препарат'))
    }
  }

  const handleRestoreToPlanned = async (dayPartId: string) => {
    try {
      await prescriptionApi.restoreToPlanned(dayPartId)
      if (id) await reloadAfterMutation(id)
      toast.success('Повернуто у Заплановано')
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося повернути у заплановані'))
    }
  }

  const handleAddItem = async (data: { medicineName: string; medicineMethod?: string; regime?: string; medicineAtcCode?: string | null }) => {
    if (!id) return
    setError(null)
    try {
      await prescriptionApi.addItem(id, data)
      await reloadAfterMutation(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося додати препарат'))
    }
  }

  const handleRemoveItem = async (itemId: string) => {
    setError(null)
    try {
      await prescriptionApi.removeItem(itemId)
      const item = items.find(i => i.id === itemId)
      if (item && id) await reloadAfterMutation(id)
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося видалити препарат'))
    }
  }

  const handleAddDay = async (itemId: string) => {
    if (!id) return
    try {
      await prescriptionApi.addItemDay(itemId)
      await reloadAfterMutation(id)
      toast.success('День додано')
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося додати день'))
    }
  }

  const handleCancelAssignment = async (dayPartId: string) => {
    try {
      await prescriptionApi.cancelAssignment(dayPartId)
      if (id) await reloadAfterMutation(id)
      toast.success('Призначення відмінено')
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося відмінити призначення'))
    }
  }

  const handleRemoveDay = async (itemId: string, dayId: string) => {
    if (!id) return
    try {
      await prescriptionApi.removeItemDay(itemId, dayId)
      await reloadAfterMutation(id)
      toast.success('День видалено')
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося видалити день'))
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
      if (id) await reloadAfterMutation(id)
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

  // Form №003-4/о PDF batch (Phase 17): backend returns every sheet at once
  // (ZIP, one PDF per form page); printing goes page by page from PDF bytes,
  // never from the HTML grid.
  const handleDownloadPdf = async () => {
    if (!id || pdfBusy) return
    setPdfBusy(true)
    try {
      const res = await prescriptionApi.getPdfZip(id)
      const url = window.URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = url
      a.download = `prescription-${id}.zip`
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
      toast.success('PDF завантажено')
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося завантажити PDF'))
    } finally {
      setPdfBusy(false)
    }
  }

  const handlePrintPdf = async () => {
    if (!id || pdfBusy) return
    setPdfBusy(true)
    try {
      const info = await prescriptionApi.getPdfInfo(id)
      for (let i = 0; i < info.data.pages; i++) {
        const res = await prescriptionApi.getPdfPage(id, i)
        await printPdfBlob(res.data)
      }
      toast.success(`PDF надіслано на друк (${info.data.pages} стор.)`)
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося надрукувати PDF'))
    } finally {
      setPdfBusy(false)
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
        <div className="flex items-center gap-1">
          <Button variant="outline" size="sm" onClick={handleDownloadPdf} disabled={pdfBusy}>
            <Download />
            Завантажити PDF
          </Button>
          <Button variant="outline" size="sm" onClick={handlePrintPdf} disabled={pdfBusy}>
            <Printer />
            Друкувати PDF
          </Button>
          {!isNurseUser && !isFinished && (
            <Button variant="secondary" onClick={() => setCloseDialogOpen(true)}>
              <X />
              Закрити листок
            </Button>
          )}
        </div>
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

      {interactions?.missingAtc?.present && (
        <Alert variant="warning" className="mb-2">
          <AlertTitle>Не вдалося перевірити взаємодії частини препаратів</AlertTitle>
          <AlertDescription>
            Немає ATC-коду: {interactions.missingAtc.names.join(', ')}.
            Попередження про взаємодії може бути неповним.
          </AlertDescription>
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
        onRemoveDay={isNurseUser ? undefined : handleRemoveDay}
        onExecute={isNurseUser ? handleExecute : undefined}
        onAddItem={isNurseUser ? async () => {} : handleAddItem}
        onRemoveItem={isNurseUser ? async () => {} : handleRemoveItem}
        onSearchMedicine={(keyword, signal) => prescriptionApi.getMedicineCatalog(keyword, signal).then(r => r.data)}
        loading={loading}
        interactions={interactions}
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
