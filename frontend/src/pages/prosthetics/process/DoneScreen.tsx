import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { CheckCircle2, Download } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { flowInstanceApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';
import type { FlowInstance, SnapshotTemplate } from '@/prosthetics/types';

function formatHours(seconds: number | null | undefined) {
  const totalMin = Math.round((seconds ?? 0) / 60);
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  return h > 0 ? `${h} год ${m} хв` : `${m} хв`;
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-card p-4 text-center">
      <div className="font-display text-xl font-semibold">{value}</div>
      <div className="text-xs text-muted-foreground">{label}</div>
    </div>
  );
}

export default function DoneScreen() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [instance, setInstance] = useState<FlowInstance | null>(null);
  const [snapshot, setSnapshot] = useState<SnapshotTemplate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  const exportPdf = async () => {
    if (!instance) return;
    setExporting(true);
    try {
      const res = await flowInstanceApi.generateReport(instance.id);
      const url = window.URL.createObjectURL(res.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `report_${instance.id}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('PDF-звіт сформовано');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося сформувати PDF-звіт'));
    } finally {
      setExporting(false);
    }
  };

  useEffect(() => {
    document.title = 'Процес завершено — підсумок виготовлення';
    const load = async () => {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const [instRes, snapRes] = await Promise.all([
          flowInstanceApi.getById(id),
          flowInstanceApi.getSnapshot(id),
        ]);
        setInstance(instRes.data);
        setSnapshot(snapRes.data);
      } catch (err) {
        setError(getErrorMessage(err, 'Не вдалося завантажити підсумок'));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="mx-auto size-14 rounded-full" />
        <Skeleton className="mx-auto h-8 w-72" />
        <div className="grid gap-4 sm:grid-cols-3">
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
        </div>
      </div>
    );
  }

  if (error || !instance || !snapshot) {
    return (
      <div className="py-16 text-center">
        <h1 className="font-display text-xl font-semibold">Підсумок недоступний</h1>
        <p className="mt-2 text-sm text-muted-foreground">{error ?? 'Процес не знайдено'}</p>
        <Button className="mt-4" onClick={() => navigate('/prosthetics')}>
          До панелі управління
        </Button>
      </div>
    );
  }

  const totalSteps = snapshot.stages.reduce((a, s) => a + s.steps.length, 0);

  return (
    <div className="mx-auto max-w-3xl">
      <div className="flex flex-col items-center text-center">
        <CheckCircle2 className="size-14 text-success" />
        <h1 className="mt-4 font-display text-2xl font-semibold">Процес успішно завершено</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {instance.id} · {snapshot.name} (v{snapshot.version})
        </p>
      </div>

      <div className="mt-8 grid gap-4 sm:grid-cols-3">
        <Stat label="Активний час" value={formatHours(instance.totalActiveSeconds)} />
        <Stat label="Кроків виконано" value={String(totalSteps)} />
        <Stat label="Доопрацювань" value={String(instance.reworkCount ?? 0)} />
      </div>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-base">Метадані процесу</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <div className="flex justify-between gap-4 border-b pb-2 last:border-0">
            <span className="text-muted-foreground">Створено</span>
            <span>{new Date(instance.createdAt).toLocaleString('uk-UA')}</span>
          </div>
          <div className="flex justify-between gap-4 border-b pb-2 last:border-0">
            <span className="text-muted-foreground">Завершено</span>
            <span>
              {instance.endTime ? new Date(instance.endTime).toLocaleString('uk-UA') : '—'}
            </span>
          </div>
          <div className="flex justify-between gap-4 border-b pb-2 last:border-0">
            <span className="text-muted-foreground">Тип виробу</span>
            <span>
              {snapshot.productType} · {snapshot.amputationLevel ?? ''} {snapshot.limbSide ?? ''}
            </span>
          </div>
          <div className="flex justify-between gap-4 border-b pb-2 last:border-0">
            <span className="text-muted-foreground">Час простою</span>
            <span>{formatHours(instance.totalIdleSeconds)}</span>
          </div>
        </CardContent>
      </Card>

      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <Button variant="outline" onClick={() => navigate(`/prosthetics/process/${instance.id}`)}>
          Технологічна карта
        </Button>
        <Button variant="outline" disabled={exporting} onClick={() => void exportPdf()}>
          <Download className="size-4" /> Експортувати PDF
        </Button>
        <Button onClick={() => navigate('/prosthetics')}>До панелі управління</Button>
      </div>
    </div>
  );
}
