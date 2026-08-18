import { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, Users, ClipboardList, UserCheck, AlertTriangle, CheckSquare, Table, LayoutDashboard, Stethoscope, Bed, RefreshCw, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Alert } from '@/components/ui/alert'
import { departmentApi } from '../../api/icu';
import EpisodeTable from '../../components/icu/EpisodeTable'
import DepartmentPatientCard from '../../components/icu/DepartmentPatientCard'
import type { DepartmentStats, DepartmentPatient } from '../../types/icu';

const initialStats: DepartmentStats = {
  activePatients: 0,
  openDays: 0,
  nurseSignedDays: 0,
  doctorSignedDays: 0,
  closedDays: 0,
  totalBeds: 12,
  occupiedBeds: 0,
  activeDoctors: 0,
  activeNurses: 0,
}

export default function DepartmentDashboardPage() {
  const navigate = useNavigate()
  const [patients, setPatients] = useState<DepartmentPatient[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [stats, setStats] = useState<DepartmentStats>(initialStats)
  const [viewMode, setViewMode] = useState<'cards' | 'table'>('cards')
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const fetchData = useCallback(() => {
    Promise.all([
      departmentApi.getPatients(),
      departmentApi.getStats(),
    ])
      .then(([patRes, statsRes]) => {
        setPatients(patRes.data)
        setStats(statsRes.data)
        setLastUpdated(new Date())
      })
      .catch(() => {
        setPatients([])
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    document.title = 'ВАІТ — Завідувач відділення'
  }, [])

  useEffect(() => {
    fetchData()
    timerRef.current = setInterval(fetchData, 30000)
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [fetchData])

  const filteredPatients = patients.filter((p) =>
    (p.patientName ?? '').toLowerCase().includes(search.toLowerCase())
  )

  const statCards = [
    { label: 'Активних пацієнтів', value: stats.activePatients, icon: <Users />, color: '#1976d2' },
    { label: 'Відкритих днів', value: stats.openDays, icon: <ClipboardList />, color: '#ed6c02' },
    { label: 'Підписано медсестрою', value: stats.nurseSignedDays, icon: <UserCheck />, color: '#0288d1' },
    { label: 'Підписано лікарем', value: stats.doctorSignedDays, icon: <AlertTriangle />, color: '#2e7d32' },
    { label: 'Зайнято ліжок', value: `${stats.occupiedBeds} / ${stats.totalBeds}`, icon: <Bed />, color: '#5c6bc0' },
    { label: 'Активні лікарі', value: stats.activeDoctors, icon: <Stethoscope />, color: '#7b1fa2' },
    { label: 'Активні медсестри', value: stats.activeNurses, icon: <Users />, color: '#00796b' },
    { label: 'Закрито днів', value: stats.closedDays, icon: <CheckSquare />, color: '#455a64' },
  ]

  if (loading) return <Loader2 role="progressbar" aria-label="Loading" className="mx-auto mt-4 size-6 animate-spin text-primary" />

  return (
    <div>
      <h1 className="font-rubik text-2xl font-extrabold text-foreground mb-0.5">
        Відділення анестезіології та інтенсивної терапії
      </h1>
      <div className="mb-3 flex items-center gap-1">
        <p className="text-sm text-muted-foreground">
          Оглядова панель завідувача
        </p>
        <div className="flex-1" />
        {lastUpdated && (
          <span className="text-xs text-muted-foreground/60">
            Оновлено: {lastUpdated.toLocaleTimeString('uk-UA')}
          </span>
        )}
        <Button variant="ghost" size="icon-sm" onClick={fetchData}>
          <RefreshCw />
        </Button>
      </div>

      <div className="mb-3 grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
        {statCards.map((card) => (
          <div key={card.label} className="rounded-xl border bg-card text-card-foreground shadow-sm p-4 text-center">
            <div className="mb-0.5 flex justify-center" style={{ color: card.color }}>{card.icon}</div>
            <div className="font-rubik text-3xl font-extrabold" style={{ color: card.color }}>
              {card.value}
            </div>
            <p className="text-xs text-muted-foreground">
              {card.label}
            </p>
          </div>
        ))}
      </div>

      <div className="mb-2 flex flex-wrap items-center justify-between gap-1">
        <h2 className="font-rubik text-lg font-semibold text-foreground">
          Активні пацієнти
        </h2>
        <div className="flex items-center gap-1 rounded-lg border bg-muted p-1">
          <Button
            variant={viewMode === 'cards' ? 'default' : 'outline'}
            size="xs"
            onClick={() => setViewMode('cards')}
          >
            <LayoutDashboard />
            Картки
          </Button>
          <Button
            variant={viewMode === 'table' ? 'default' : 'outline'}
            size="xs"
            onClick={() => setViewMode('table')}
          >
            <Table />
            Таблиця
          </Button>
        </div>
      </div>

      <div className="relative mb-2">
        <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Пошук пацієнта за ПІБ..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-8"
        />
      </div>

      {filteredPatients.length === 0 && !loading ? (
        <Alert>
          {search ? 'Немає пацієнтів за запитом' : 'Немає активних пацієнтів'}
        </Alert>
      ) : viewMode === 'cards' ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filteredPatients.map((p) => (
            <DepartmentPatientCard patient={p} key={p.id} />
          ))}
        </div>
      ) : (
        <EpisodeTable
          episodes={filteredPatients.map((p) => ({
            id: p.id,
            patientId: p.patientId,
            patientName: p.patientName,
            hospitalizationId: p.hospitalizationId,
            departmentId: p.departmentId,
            admissionDate: p.admissionDate,
            dischargeDate: p.dischargeDate,
            status: p.status as 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'ARCHIVED',
            heightCm: null,
            ward: p.ward,
            bedNumber: p.bedNumber,
            admissionDiagnosis: p.admissionDiagnosis,
            attendingDoctorId: p.attendingDoctorId,
            createdBy: 0,
            createdAt: p.admissionDate,
            updatedBy: 0,
            updatedAt: p.admissionDate,
            version: 0,
          }))}
          onSelect={(ep) => navigate('/icu/doctor/episode/' + ep.id)}
        />
      )}
    </div>
  )
}
