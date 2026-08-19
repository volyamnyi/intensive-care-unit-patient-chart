import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Lock, Download, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Alert } from '@/components/ui/alert'
import { useThemeMode } from '../../styles/ThemeContext'
import { patientApi, settingsApi } from '../../api/platform';
import { episodeApi, clinicalDayApi, hourlyRecordApi, medicalOrderApi, fluidBalanceApi, pdfApi } from '../../api/icu';
import { useAuth } from '../../services/AuthContext'
import DoctorDashboard from '../../components/monitoring/DoctorDashboard'
import NurseDashboard from '../../components/monitoring/NurseDashboard'
import DocumentHeader from '../../components/icu/DocumentHeader'
import type { Episode, ClinicalDay, HourlyRecord, MedicalOrder, FluidBalanceItem } from '../../types/icu';

export default function PatientDayPage() {
  const { episodeId } = useParams()
  const navigate = useNavigate()
  const { mode } = useThemeMode()
  const { user } = useAuth()

  const [episode, setEpisode] = useState<Episode | null>(null)
  const [clinicalDays, setClinicalDays] = useState<ClinicalDay[]>([])
  const [selectedDay, setSelectedDay] = useState<ClinicalDay | null>(null)
  const [records, setRecords] = useState<HourlyRecord[]>([])
  const [orders, setOrders] = useState<MedicalOrder[]>([])
  const [balanceItems, setFluidBalanceItems] = useState<FluidBalanceItem[]>([])
  const [signConfirm, setSignConfirm] = useState(false)
  const [reopenOpen, setReopenOpen] = useState(false)
  const [reopenReason, setReopenReason] = useState('')
  const [loading, setLoading] = useState(true)
  const [institutionName, setInstitutionName] = useState('')
  const [institutionEdrpou, setInstitutionEdrpou] = useState('')
  const [patientBirthDate, setPatientBirthDate] = useState<string | null>(null)
  const [dayLoading, setDayLoading] = useState(false)
  const [signingLoading, setSigningLoading] = useState(false)
  const [reopenLoading, setReopenLoading] = useState(false)
  const [feedback, setFeedback] = useState<{ message: string; severity: 'success' | 'error' } | null>(null)

  useEffect(() => {
    if (!episodeId) return
    setLoading(true)
    Promise.all([
      episodeApi.getById(episodeId),
      episodeApi.getClinicalDays(episodeId),
      settingsApi.getByKey('institution_name').catch(() => null),
      settingsApi.getByKey('institution_edrpou').catch(() => null),
    ]).then(([epRes, daysRes, nameRes, edrpouRes]) => {
      setEpisode(epRes.data)
      setClinicalDays(daysRes.data)
      setInstitutionName(nameRes?.data?.value ?? '')
      setInstitutionEdrpou(edrpouRes?.data?.value ?? '')
      if (epRes.data.patientId) {
        patientApi.getById(String(epRes.data.patientId))
          .then(pRes => setPatientBirthDate(pRes.data.birthDate ?? null))
          .catch(() => setPatientBirthDate(null))
      }
      const isDoctorRole = user?.role === 'DOCTOR' || user?.role === 'HEAD_OF_DEPARTMENT'
      const target = isDoctorRole
        ? daysRes.data.find(d => d.status === 'NURSE_SIGNED')
        : daysRes.data.find(d => d.status === 'OPEN' || d.status === 'REOPENED')
      setSelectedDay(target || daysRes.data[0] || null)
    }).catch((err) => {
      console.warn('Failed to load episode:', err)
    }).finally(() => setLoading(false))
  }, [episodeId, user])

  const loadDayData = useCallback(async (day: ClinicalDay) => {
    setDayLoading(true)
    try {
      const [recRes, ordRes, balRes] = await Promise.all([
        hourlyRecordApi.getByClinicalDay(day.id),
        medicalOrderApi.getByClinicalDay(day.id),
        fluidBalanceApi.getByClinicalDay(day.id),
      ])
      setRecords(recRes.data)
      setOrders(ordRes.data)
      setFluidBalanceItems(balRes.data)
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Не вдалося завантажити дані доби'
      setFeedback({ message: msg, severity: 'error' })
    } finally {
      setDayLoading(false)
    }
  }, [])

  const handleRefresh = useCallback(() => {
    if (selectedDay) {
      loadDayData(selectedDay)
    }
  }, [selectedDay, loadDayData])

  useEffect(() => {
    if (selectedDay) {
      loadDayData(selectedDay)
    }
  }, [selectedDay, loadDayData])

  useEffect(() => {
    document.title = episode ? `ICU — ${episode.patientName}` : 'ICU — Patient'
  }, [episode])

  const handleSignOff = async () => {
    if (!selectedDay || !user) return
    setSigningLoading(true)
    try {
      if (user.role === 'NURSE') {
        await clinicalDayApi.signNurse(selectedDay.id, { userId: user.id })
      } else {
        await clinicalDayApi.signDoctor(selectedDay.id, { userId: user.id })
      }
      setSignConfirm(false)
      const daysRes = await episodeApi.getClinicalDays(episodeId!)
      setClinicalDays(daysRes.data)
      const updated = daysRes.data.find(d => d.id === selectedDay.id)
      if (updated) setSelectedDay(updated)
      setFeedback({ message: 'Добу підписано', severity: 'success' })
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Не вдалося підписати добу'
      setFeedback({ message: msg, severity: 'error' })
    } finally {
      setSigningLoading(false)
    }
  }

  const handleReopen = async () => {
    if (!selectedDay || !user || !reopenReason.trim()) return
    setReopenLoading(true)
    try {
      await clinicalDayApi.reopen(selectedDay.id, { reason: reopenReason.trim(), version: selectedDay.version })
      setReopenOpen(false)
      setReopenReason('')
      const daysRes = await episodeApi.getClinicalDays(episodeId!)
      setClinicalDays(daysRes.data)
      const updated = daysRes.data.find(d => d.id === selectedDay.id)
      if (updated) setSelectedDay(updated)
      setFeedback({ message: 'Добу перевідкрито', severity: 'success' })
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Не вдалося перевідкрити добу'
      setFeedback({ message: msg, severity: 'error' })
    } finally {
      setReopenLoading(false)
    }
  }

  const handleGeneratePDF = async () => {
    if (!selectedDay) return
    try {
      await pdfApi.generate(selectedDay.id)
      setFeedback({ message: 'PDF успішно згенеровано', severity: 'success' })
    } catch {
      setFeedback({ message: 'Не вдалося згенерувати PDF', severity: 'error' })
    }
  }

  if (loading) return <Loader2 role="progressbar" aria-label="Loading" className="mx-auto mt-4 size-6 animate-spin text-primary" />
  if (!episode) return <Alert variant="destructive">Епізод не знайдено</Alert>

  const isNurse = user?.role === 'NURSE'
  const isDoctor = user?.role === 'DOCTOR' || user?.role === 'HEAD_OF_DEPARTMENT'
  const isHod = user?.role === 'HEAD_OF_DEPARTMENT'
  const isLocked = selectedDay ? selectedDay.status !== 'OPEN' && selectedDay.status !== 'REOPENED' : true
  const patientAge = (() => {
    if (!patientBirthDate) return null
    const birth = new Date(patientBirthDate)
    const ref = selectedDay?.startDateTime ? new Date(selectedDay.startDateTime) : new Date()
    if (Number.isNaN(birth.getTime()) || Number.isNaN(ref.getTime())) return null
    let years = ref.getFullYear() - birth.getFullYear()
    const m = ref.getMonth() - birth.getMonth()
    if (m < 0 || (m === 0 && ref.getDate() < birth.getDate())) years -= 1
    return years >= 0 ? `${years} р.` : null
  })()
  const canSign = selectedDay && (
    (isNurse && selectedDay.status === 'OPEN') ||
    (isDoctor && selectedDay.status === 'NURSE_SIGNED')
  )
  const canReopen = isHod && selectedDay && (
    selectedDay.status === 'NURSE_SIGNED' || selectedDay.status === 'DOCTOR_SIGNED'
  )

  return (
    <div>
      <DocumentHeader
        institutionName={institutionName}
        institutionEdrpou={institutionEdrpou}
        patientName={episode.patientName}
        patientAge={patientAge}
        dayNumber={selectedDay?.dayNumber ?? null}
        dayDate={selectedDay?.startDateTime ?? episode.admissionDate}
        cardNumber={episode.hospitalizationId ?? episode.id.slice(0, 8).toUpperCase()}
      />
      {/* Float action bar */}
      <div className="no-print mb-1 flex flex-wrap items-center justify-between gap-0.5">
        <Button
          variant="link"
          size="sm"
          onClick={() => navigate(isNurse ? '/icu/nurse' : '/icu/doctor')}
          className="text-muted-foreground text-xs"
        >
          <ArrowLeft />
          {isNurse ? '← Назад до пацієнтів' : '← Назад до пацієнтів'}
        </Button>
        <div className="flex items-center gap-0.5">
          {dayLoading && <Loader2 className="mr-0.5 size-4 animate-spin" />}
          {selectedDay && selectedDay.status === 'CLOSED' && (
            <Button size="sm" variant="outline" onClick={handleGeneratePDF}>
              <Download />
              PDF
            </Button>
          )}
          {canReopen && (
            <Button size="sm" variant="outline" className="text-amber-600 border-amber-300 hover:bg-amber-50 dark:text-amber-400 dark:border-amber-700 dark:hover:bg-amber-950" onClick={() => setReopenOpen(v => !v)} disabled={reopenLoading}>
              <Lock />
              {reopenOpen ? 'Скасувати' : 'Перевідкрити'}
            </Button>
          )}
          {canSign && !signConfirm && (
            <Button size="sm" onClick={() => setSignConfirm(true)} disabled={signingLoading} className="font-bold">
              {signingLoading && <Loader2 className="mr-0.5 size-3.5 animate-spin" />}
              Підписати добу
            </Button>
          )}
        </div>
      </div>

      {signConfirm && (
        <div className="rounded-xl border border-amber-500/50 bg-card text-card-foreground shadow-md p-4 mb-2">
          <p className="text-sm mb-2">
            Після підписання доба стане read-only
          </p>
          <div className="flex gap-1">
            <Button onClick={handleSignOff} disabled={signingLoading}>
              {signingLoading && <Loader2 className="mr-0.5 size-3.5 animate-spin" />}
              Підписати
            </Button>
            <Button variant="outline" onClick={() => setSignConfirm(false)} disabled={signingLoading}>
              Скасувати
            </Button>
          </div>
        </div>
      )}

      {/* Main dashboard — role-specific view */}
      {selectedDay && (
        <div className="relative">
          {dayLoading && (
            <div className={`
              absolute inset-0 z-10 flex items-center justify-center rounded-xl
              ${mode === 'dark' ? 'bg-black/40' : 'bg-white/60'}
            `}>
              <Loader2 className="size-6 animate-spin text-primary" />
            </div>
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
              dayLoading={dayLoading}
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
              dayLoading={dayLoading}
            />
          )}
        </div>
      )}

      {reopenOpen && selectedDay && (
        <div className="rounded-xl border border-amber-500/50 bg-card text-card-foreground shadow-md p-4 mb-2">
          <p className="font-semibold text-sm mb-1">
            Повторне відкриття дня {selectedDay.dayNumber}
          </p>
          <p className="text-sm text-muted-foreground mb-2">
            Це скасує всі підписи. Вкажіть причину:
          </p>
          <textarea
            autoFocus
            placeholder="Причина"
            value={reopenReason}
            onChange={e => setReopenReason(e.target.value)}
            rows={2}
            className="h-8 w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-1 text-base transition-colors outline-none file:inline-flex file:h-6 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring disabled:pointer-events-none disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 md:text-sm dark:bg-input/30 dark:disabled:bg-input/80 dark:aria-invalid:border-destructive/50 dark:aria-invalid:ring-destructive/40"
          />
          <div className="flex gap-1 mt-1">
            <Button variant="secondary" onClick={handleReopen} disabled={!reopenReason.trim() || reopenLoading}>
              {reopenLoading && <Loader2 className="mr-0.5 size-3.5 animate-spin" />}
              Перевідкрити
            </Button>
            <Button variant="outline" onClick={() => setReopenOpen(false)} disabled={reopenLoading}>
              Скасувати
            </Button>
          </div>
        </div>
      )}

      {feedback && (
        <div className="fixed top-0 left-0 right-0 z-[9999] flex justify-center pt-1">
          <Alert variant={feedback.severity === 'error' ? 'destructive' : 'default'} className="text-xs shadow-lg">
            {feedback.message}
          </Alert>
        </div>
      )}
    </div>
  )
}
