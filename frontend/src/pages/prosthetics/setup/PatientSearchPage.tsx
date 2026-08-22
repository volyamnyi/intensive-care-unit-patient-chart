import { useEffect, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Search, SearchX } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { prostheticsPatientApi } from '@/api/prosthetics';
import type { ProstheticsPatient } from '@/prosthetics/types';
import { SetupSteps } from '@/components/prosthetics/SetupSteps';
import PatientTable from '@/components/prosthetics/PatientTable';

function EmptyState({ icon, title, hint }: { icon: ReactNode; title: string; hint: string }) {
  return (
    <div className="flex min-h-[320px] flex-col items-center justify-center rounded-xl border border-dashed bg-muted/30 px-6 py-10 text-center">
      <div className="mb-3 flex size-11 items-center justify-center rounded-full bg-background text-muted-foreground shadow-sm ring-1 ring-foreground/10">
        {icon}
      </div>
      <p className="text-sm font-medium text-foreground">{title}</p>
      <p className="mt-1 text-xs text-muted-foreground">{hint}</p>
    </div>
  );
}

export default function PatientSearchPage() {
  const navigate = useNavigate();
  const { draft, setDraftField } = useProsthetics();
  const [allPatients, setAllPatients] = useState<ProstheticsPatient[]>([]);
  const [patients, setPatients] = useState<ProstheticsPatient[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Вибір пацієнта — Виробництво протезів';
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    prostheticsPatientApi
      .search()
      .then((res) => {
        if (active) {
          setAllPatients(res.data);
          setPatients(res.data);
        }
      })
      .catch(() => {
        if (active) setError('Не вдалося завантажити пацієнтів');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (query.trim().length < 2) {
      setPatients(allPatients);
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
  }, [query, allPatients]);

  const handleSelect = (patient: ProstheticsPatient) => {
    setDraftField('patientId', patient.id);
    setDraftField('orderId', null);
    navigate('/prosthetics/new/select-order');
  };

  return (
    <div className="container mx-auto max-w-4xl py-8">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics')}>
          <ChevronLeft className="size-4" />
          Назад
        </Button>
        <div>
          <h1 className="font-display text-2xl font-bold">Вибір пацієнта</h1>
          <p className="text-sm text-muted-foreground">Крок 1 з 4 · джерело даних: Doctor Eleks</p>
        </div>
        <SetupSteps current={1} className="mx-auto" />
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

      {error && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Помилка</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {loading ? (
        <div className="space-y-2">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : patients.length === 0 ? (
        query.trim().length >= 2 ? (
          <EmptyState
            icon={<SearchX className="size-5" />}
            title="Пацієнтів не знайдено"
            hint="Перевірте написання або спробуйте інший запит"
          />
        ) : (
          <EmptyState
            icon={<Search className="size-5" />}
            title="Немає пацієнтів"
            hint="У системі немає доступних пацієнтів"
          />
        )
      ) : (
        <PatientTable patients={patients} selectedId={draft.patientId} onSelect={handleSelect} />
      )}

      <div className="sticky bottom-0 z-10 -mx-4 mt-4 flex flex-col gap-3 border-t bg-background/95 px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] backdrop-blur sm:-mx-6 sm:flex-row sm:items-center sm:justify-between sm:px-6 sm:pb-3">
        <Button variant="outline" className="w-full sm:w-auto" onClick={() => navigate('/prosthetics')}>
          Назад
        </Button>
        <Button
          disabled={!draft.patientId}
          className="w-full bg-accent text-accent-foreground hover:bg-accent/90 sm:w-auto"
          onClick={() => navigate('/prosthetics/new/select-order')}
        >
          Далі
        </Button>
      </div>
    </div>
  );
}
