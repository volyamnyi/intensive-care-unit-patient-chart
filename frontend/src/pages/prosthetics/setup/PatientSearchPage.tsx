import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { prostheticsPatientApi } from '@/api/prosthetics';
import type { ProstheticsPatient } from '@/prosthetics/types';
import { SetupSteps } from '@/components/prosthetics/SetupSteps';

export default function PatientSearchPage() {
  const navigate = useNavigate();
  const { draft, setDraftField } = useProsthetics();
  const [query, setQuery] = useState('');
  const [patients, setPatients] = useState<ProstheticsPatient[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Вибір пацієнта — Виробництво протезів';
  }, []);

  useEffect(() => {
    if (query.length < 2) {
      setPatients([]);
      return;
    }
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const res = await prostheticsPatientApi.search(query, controller.signal);
        setPatients(res.data);
      } catch {
        setError('Помилка пошуку');
      } finally {
        setLoading(false);
      }
    }, 300);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [query]);

  return (
    <div className="container mx-auto max-w-2xl py-8">
      <div className="mb-6 flex items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics')}>
          <ChevronLeft className="size-4" />
          Назад
        </Button>
        <div>
          <h1 className="font-display text-2xl font-bold">Вибір пацієнта</h1>
          <p className="text-sm text-muted-foreground">Крок 1 з 4 · джерело даних: Doctor Eleks</p>
        </div>
        <SetupSteps current={1} className="ml-auto" />
      </div>

      <div className="mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
          <Input
            placeholder="Пошук пацієнта за ПІБ або номером координати..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="pl-10"
          />
        </div>
      </div>

      {error && <p className="text-destructive mb-4">{error}</p>}

      <div className="space-y-2">
        {loading ? (
          <div className="space-y-2">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        ) : patients.length === 0 && query.length >= 2 ? (
          <p className="text-muted-foreground">Пацієнтів не знайдено</p>
        ) : patients.length > 0 ? (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ПІБ</TableHead>
                <TableHead>Дата народження</TableHead>
                <TableHead>Статус</TableHead>
                <TableHead className="text-right">Дія</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {patients.map((patient) => (
                <TableRow key={patient.id}>
                  <TableCell className="font-medium">{patient.pib}</TableCell>
                  <TableCell>{new Date(patient.birthDate).toLocaleDateString('uk-UA')}</TableCell>
                  <TableCell>
                    <Badge variant="outline">Активний</Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      size="sm"
                      variant={draft.patientId === patient.id ? 'default' : 'outline'}
                      onClick={() => {
                        setDraftField('patientId', patient.id);
                        setDraftField('orderId', null);
                        navigate('/prosthetics/new/select-order');
                      }}
                    >
                      {draft.patientId === patient.id ? 'Обрано' : 'Обрати'}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        ) : query.length < 2 ? (
          <div className="py-12 text-center">
            <p className="text-muted-foreground">Введіть ім'я або номер для пошуку</p>
          </div>
        ) : null}
      </div>

      <div className="mt-6 flex justify-between">
        <Button variant="outline" onClick={() => navigate('/prosthetics')}>
          Назад
        </Button>
        <Button
          disabled={!draft.patientId}
          className="bg-accent text-accent-foreground hover:bg-accent/90"
          onClick={() => navigate('/prosthetics/new/select-order')}
        >
          Далі
        </Button>
      </div>
    </div>
  );
}
