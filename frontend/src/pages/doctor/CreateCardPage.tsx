import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Alert } from '@/components/ui/alert'
import { episodeApi } from '../../api/endpoints'
import PatientSearch from '../../components/common/PatientSearch'
import type { PatientDto } from '../../types'

export default function CreateCardPage() {
  useEffect(() => { document.title = 'ВАІТ — Нова карта' }, [])
  const navigate = useNavigate()
  const [selectedPatient, setSelectedPatient] = useState<PatientDto | null>(null)
  const [ward, setWard] = useState('')
  const [bedNumber, setBedNumber] = useState('')
  const [admissionDiagnosis, setAdmissionDiagnosis] = useState('')
  const [error, setError] = useState('')

  const handleCreate = async () => {
    if (!selectedPatient) return
    try {
      const res = await episodeApi.create({
        patientId: selectedPatient.id,
        admissionDate: new Date().toISOString(),
        heightCm: selectedPatient.height ?? undefined,
        ward: ward || undefined,
        bedNumber: bedNumber || undefined,
        admissionDiagnosis: admissionDiagnosis || undefined,
      })
      navigate('/icu/doctor/episode/' + res.data.id)
    } catch {
      setError('Помилка створення карти')
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="font-rubik text-2xl font-extrabold text-foreground mb-3">
        Нова карта інтенсивної терапії
      </h1>
      {error && <Alert variant="destructive" className="mb-2">{error}</Alert>}
      <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-6 mb-3">
        <h2 className="font-rubik text-lg font-semibold mb-2 text-foreground">
          Пошук пацієнта
        </h2>
        <PatientSearch onSelect={setSelectedPatient} />
      </div>
      {selectedPatient && (
        <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-6 mb-3">
          <h2 className="font-rubik text-lg font-semibold mb-2 text-foreground">
            Дані пацієнта (з МІС)
          </h2>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div className="col-span-2 sm:col-span-2">
              <label htmlFor="fullName" className="text-sm font-medium text-muted-foreground mb-1 block">ПІП</label>
              <Input id="fullName" value={selectedPatient.fullName} readOnly />
            </div>
            <div className="col-span-1">
              <label htmlFor="birthDate" className="text-sm font-medium text-muted-foreground mb-1 block">Дата народження</label>
              <Input id="birthDate" value={selectedPatient.birthDate} readOnly />
            </div>
            <div className="col-span-1">
              <label htmlFor="gender" className="text-sm font-medium text-muted-foreground mb-1 block">Стать</label>
              <Input id="gender" value={selectedPatient.sexCode === 'M' ? 'Чол' : 'Жін'} readOnly />
            </div>
            <div className="col-span-1">
              <label htmlFor="height" className="text-sm font-medium text-muted-foreground mb-1 block">Зріст (см)</label>
              <Input id="height" value={selectedPatient.height ?? ''} readOnly />
            </div>
            <div className="col-span-1">
              <label htmlFor="weight" className="text-sm font-medium text-muted-foreground mb-1 block">Маса (кг)</label>
              <Input id="weight" value={selectedPatient.weight ?? ''} readOnly />
            </div>
            <div className="col-span-1">
              <label className="text-sm font-medium text-muted-foreground mb-1 block">Ідеальна маса (кг)</label>
              <Input value={
                selectedPatient.height
                  ? (selectedPatient.sexCode === 'M'
                    ? (50 + 0.91 * (selectedPatient.height - 152.4)).toFixed(1)
                    : (45.5 + 0.91 * (selectedPatient.height - 152.4)).toFixed(1))
                  : ''
              } readOnly />
            </div>
            <div className="col-span-1">
              <label htmlFor="bloodGroup" className="text-sm font-medium text-muted-foreground mb-1 block">Група крові</label>
              <Input id="bloodGroup" value={selectedPatient.bloodGroup} readOnly />
            </div>
            <div className="col-span-1">
              <label htmlFor="rhFactor" className="text-sm font-medium text-muted-foreground mb-1 block">Rezus</label>
              <Input id="rhFactor" value={selectedPatient.rhFactor} readOnly />
            </div>
            <div className="col-span-2 sm:col-span-2">
              <label htmlFor="cardNumber" className="text-sm font-medium text-muted-foreground mb-1 block">№ медкарти</label>
              <Input id="cardNumber" value={selectedPatient.externalId1} readOnly />
            </div>
          </div>
        </div>
      )}
      {selectedPatient && (
        <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-6 mb-3">
          <h2 className="font-rubik text-lg font-semibold mb-2 text-foreground">
            Деталі госпіталізації
          </h2>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-6">
            <div className="col-span-3 sm:col-span-2">
              <label className="text-sm font-medium text-muted-foreground mb-1 block">Палата</label>
              <Input value={ward} onChange={e => setWard(e.target.value)} placeholder="напр. ВАІТ-1" />
            </div>
            <div className="col-span-1 sm:col-span-1">
              <label className="text-sm font-medium text-muted-foreground mb-1 block">Ліжко</label>
              <Input value={bedNumber} onChange={e => setBedNumber(e.target.value)} placeholder="напр. 101A" />
            </div>
            <div className="col-span-3 sm:col-span-3">
              <label className="text-sm font-medium text-muted-foreground mb-1 block">Діагноз при госпіталізації</label>
              <Input value={admissionDiagnosis} onChange={e => setAdmissionDiagnosis(e.target.value)}
                placeholder="напр. Позалікарняна пневмонія" />
            </div>
          </div>
        </div>
      )}
      {selectedPatient && (
        <div className="flex gap-2">
          <Button size="lg" onClick={handleCreate}>
            Створити карту
          </Button>
          <Button variant="outline" size="lg" onClick={() => navigate('/icu/doctor')}>
            Скасувати
          </Button>
        </div>
      )}
    </div>
  )
}
