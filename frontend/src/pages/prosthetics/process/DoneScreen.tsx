import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { CheckCircle2, Download, Diamond, Info, Layers, Package } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/skeleton';
import { ProcessStat } from '@/components/prosthetics/ProcessStat';
import { flowInstanceApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';
import type {
  FlowInstance,
  GateDecisionResponse,
  ResourceUsageResponse,
  SnapshotTemplate,
  StepExecution,
} from '@/prosthetics/types';

const GATE_DECISION_LABELS: Record<string, string> = {
  PASS: 'Пройдено',
  REWORK: 'Доопрацювання',
  FAIL: 'Провалено',
};

function formatHours(seconds: number | null | undefined) {
  const totalMin = Math.round((seconds ?? 0) / 60);
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  return h > 0 ? `${h} год ${m} хв` : `${m} хв`;
}

export default function DoneScreen() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [instance, setInstance] = useState<FlowInstance | null>(null);
  const [snapshot, setSnapshot] = useState<SnapshotTemplate | null>(null);
  const [executions, setExecutions] = useState<StepExecution[]>([]);
  const [decisions, setDecisions] = useState<GateDecisionResponse[]>([]);
  const [resources, setResources] = useState<ResourceUsageResponse[]>([]);
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
        const [instRes, snapRes, execRes, gateRes, resRes] = await Promise.all([
          flowInstanceApi.getById(id),
          flowInstanceApi.getSnapshot(id),
          flowInstanceApi.listExecutions(id),
          flowInstanceApi.listGateDecisions(id),
          flowInstanceApi.listResources(id),
        ]);
        setInstance(instRes.data);
        setSnapshot(snapRes.data);
        setExecutions(execRes.data);
        setDecisions(gateRes.data);
        setResources(resRes.data);
      } catch (err) {
        setError(getErrorMessage(err, 'Не вдалося завантажити підсумок'));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  const stageTimeline = useMemo(() => {
    if (!snapshot) return [];
    const doneStepIds = new Set(
      executions.filter((e) => e.status === 'COMPLETED').map((e) => e.stepId),
    );
    return snapshot.stages.map((stage) => {
      const done = stage.steps.filter((s) => doneStepIds.has(s.id)).length;
      return { stage, done, total: stage.steps.length };
    });
  }, [snapshot, executions]);

  const totalSteps = snapshot?.stages.reduce((a, s) => a + s.steps.length, 0) ?? 0;

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
        <ProcessStat label="Активний час" value={formatHours(instance.totalActiveSeconds)} />
        <ProcessStat label="Кроків виконано" value={String(totalSteps)} />
        <ProcessStat label="Доопрацювань" value={String(instance.reworkCount ?? 0)} />
      </div>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Layers className="size-4 text-muted-foreground" /> Етапи виготовлення
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          {stageTimeline.map(({ stage, done, total }, idx) => (
            <div key={stage.id} className="rounded-md border p-3">
              <div className="flex items-center justify-between">
                <span className="font-medium">
                  {idx + 1}. {stage.name}
                </span>
                <span className="text-xs text-muted-foreground">
                  {done}/{total} кроків
                </span>
              </div>
              <Progress
                className="mt-2"
                value={total === 0 ? 0 : (done / total) * 100}
              />
              {done > 0 && (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {stage.steps
                    .filter((s) =>
                      new Set(
                        executions
                          .filter((e) => e.status === 'COMPLETED')
                          .map((e) => e.stepId),
                      ).has(s.id),
                    )
                    .map((s) => (
                      <span key={s.id} className="rounded bg-muted px-2 py-0.5 text-xs">
                        {s.name}
                      </span>
                    ))}
                </div>
              )}
            </div>
          ))}
        </CardContent>
      </Card>

      {decisions.length > 0 && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Diamond className="size-4 text-muted-foreground" /> Контрольні точки
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Точка контролю</TableHead>
                  <TableHead>Рішення</TableHead>
                  <TableHead>Коментар</TableHead>
                  <TableHead className="text-right">Дата</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {decisions.map((d) => (
                  <TableRow key={d.id}>
                    <TableCell>{d.gateName ?? '—'}</TableCell>
                    <TableCell>
                      <span className="rounded bg-muted px-2 py-0.5 text-xs">
                        {GATE_DECISION_LABELS[d.decision] ?? d.decision}
                      </span>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{d.comment ?? '—'}</TableCell>
                    <TableCell className="text-right text-muted-foreground">
                      {new Date(d.decidedAt).toLocaleString('uk-UA')}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {resources.length > 0 && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Package className="size-4 text-muted-foreground" /> Витрачені ресурси
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Матеріал</TableHead>
                  <TableHead className="text-right">Кількість</TableHead>
                  <TableHead className="text-right">Час</TableHead>
                  <TableHead>Крок</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {resources.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell>{r.material}</TableCell>
                    <TableCell className="text-right">
                      {r.qty != null ? `${r.qty} ${r.unit ?? ''}` : '—'}
                    </TableCell>
                    <TableCell className="text-right">{r.minutes ?? 0} хв</TableCell>
                    <TableCell className="text-muted-foreground">{r.stepName ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Info className="size-4 text-muted-foreground" /> Метадані процесу
          </CardTitle>
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
        <Button variant="outline" className="gap-2" disabled={exporting} onClick={() => void exportPdf()}>
          <Download className="size-4" /> Експортувати PDF
        </Button>
        <Button onClick={() => navigate('/prosthetics')}>До панелі управління</Button>
      </div>
    </div>
  );
}
