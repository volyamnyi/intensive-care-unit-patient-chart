import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { XCircle, Lock, Download, RefreshCcw, ClipboardList, AlertTriangle, Info, Package } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { ProcessStat } from '@/components/prosthetics/ProcessStat';
import { flowInstanceApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';
import type {
  FailureSnapshot as FailureSnapshotData,
  FlowInstance,
  ResourceUsageResponse,
  SnapshotTemplate,
  StepExecution,
} from '@/prosthetics/types';
import { FAILURE_CATEGORY_LABELS } from '@/prosthetics/failureCategories';

function formatHours(seconds: number | null | undefined) {
  const totalMin = Math.round((seconds ?? 0) / 60);
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  return h > 0 ? `${h} год ${m} хв` : `${m} хв`;
}

export default function FailedScreen() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [instance, setInstance] = useState<FlowInstance | null>(null);
  const [snapshot, setSnapshot] = useState<SnapshotTemplate | null>(null);
  const [failure, setFailure] = useState<FailureSnapshotData | null>(null);
  const [executions, setExecutions] = useState<StepExecution[]>([]);
  const [resources, setResources] = useState<ResourceUsageResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);
  const [creatingReplacement, setCreatingReplacement] = useState(false);
  const [replacementOpen, setReplacementOpen] = useState(false);

  useEffect(() => {
    document.title = 'Процес зупинено — звіт про брак';
    const load = async () => {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const [instRes, snapRes, execRes, resRes] = await Promise.all([
          flowInstanceApi.getById(id),
          flowInstanceApi.getSnapshot(id),
          flowInstanceApi.listExecutions(id),
          flowInstanceApi.listResources(id),
        ]);
        setInstance(instRes.data);
        setSnapshot(snapRes.data);
        setExecutions(execRes.data);
        setResources(resRes.data);
        try {
          const failRes = await flowInstanceApi.getFailureSnapshot(id);
          setFailure(failRes.data);
        } catch {
          setFailure(null);
        }
      } catch (err) {
        setError(getErrorMessage(err, 'Не вдалося завантажити звіт'));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  const completedSteps = useMemo(() => {
    if (!snapshot) return 0;
    const done = new Set(executions.filter((e) => e.status === 'COMPLETED').map((e) => e.stepId));
    return snapshot.stages.reduce((acc, stage) => acc + stage.steps.filter((s) => done.has(s.id)).length, 0);
  }, [snapshot, executions]);

  const totalSteps = snapshot?.stages.reduce((a, s) => a + s.steps.length, 0) ?? 0;

  const exportPdf = async () => {
    if (!instance) return;
    setExporting(true);
    try {
      const res = await flowInstanceApi.generateReport(instance.id);
      const url = window.URL.createObjectURL(res.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `failure_report_${instance.id}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('PDF-звіт про брак сформовано');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося сформувати PDF-звіт'));
    } finally {
      setExporting(false);
    }
  };

  const createReplacement = async () => {
    if (!instance) return;
    setCreatingReplacement(true);
    try {
      const res = await flowInstanceApi.replacement(instance.id);
      setReplacementOpen(false);
      toast.success('Створено замінювальний процес');
      navigate(`/prosthetics/process/${res.data.id}`, { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося створити замінювальний процес'));
    } finally {
      setCreatingReplacement(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="mx-auto size-14 rounded-full" />
        <Skeleton className="mx-auto h-8 w-72" />
        <Skeleton className="h-32" />
        <Skeleton className="h-40" />
      </div>
    );
  }

  if (error || !instance || !snapshot) {
    return (
      <div className="py-16 text-center">
        <h1 className="font-display text-xl font-semibold">Звіт недоступний</h1>
        <p className="mt-2 text-sm text-muted-foreground">{error ?? 'Процес не знайдено'}</p>
        <Button className="mt-4" onClick={() => navigate('/prosthetics')}>
          До панелі управління
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex flex-col items-center text-center">
        <XCircle className="size-14 text-destructive" />
        <h1 className="mt-4 font-display text-2xl font-semibold">Процес зупинено (брак)</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {instance.id} · {snapshot.name} (v{snapshot.version})
        </p>
      </div>

      <div className="mt-6 flex items-center justify-center gap-2 rounded-md border bg-muted px-3 py-2 text-xs text-muted-foreground">
        <Lock className="size-3.5" />
        Незмінний запис — лише для читання
      </div>

      <Card className="mt-6 border-destructive">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <AlertTriangle className="size-4 text-muted-foreground" /> Причина провалу
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          {failure?.category && (
            <div>
              <div className="text-xs text-muted-foreground">Категорія</div>
              <div className="font-medium">
                {FAILURE_CATEGORY_LABELS[failure.category] ?? failure.category}
              </div>
            </div>
          )}
          <div>
            <div className="text-xs text-muted-foreground">Опис</div>
            <div className="font-medium">{instance.failReason ?? failure?.description ?? '—'}</div>
          </div>
        </CardContent>
      </Card>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <ProcessStat label="Кроків виконано" value={`${completedSteps}/${totalSteps}`} />
        <ProcessStat label="Активний час" value={formatHours(instance.totalActiveSeconds)} />
      </div>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <ClipboardList className="size-4" /> Виконані кроки
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          {snapshot.stages.length === 0 ? (
            <p className="text-muted-foreground">Кроків не виконано.</p>
          ) : (
            snapshot.stages.map((stage) => {
              const stageDone = stage.steps.filter(
                (s) => new Set(executions.filter((e) => e.status === 'COMPLETED').map((e) => e.stepId)).has(s.id),
              ).length;
              return (
                <div key={stage.id} className="rounded-md border p-3">
                  <div className="flex items-center justify-between">
                    <span className="font-medium">{stage.name}</span>
                    <span className="text-xs text-muted-foreground">
                      {stageDone}/{stage.steps.length} кроків
                    </span>
                  </div>
                  {stageDone > 0 && (
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {stage.steps
                        .filter((s) => new Set(executions.filter((e) => e.status === 'COMPLETED').map((e) => e.stepId)).has(s.id))
                        .map((s) => (
                          <span key={s.id} className="rounded bg-muted px-2 py-0.5 text-xs">
                            {s.name}
                          </span>
                        ))}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </CardContent>
      </Card>

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
            <span className="text-muted-foreground">Зупинено</span>
            <span>
              {instance.endTime ? new Date(instance.endTime).toLocaleString('uk-UA') : '—'}
            </span>
          </div>
          <div className="flex justify-between gap-4 border-b pb-2 last:border-0">
            <span className="text-muted-foreground">Активний час</span>
            <span>{formatHours(instance.totalActiveSeconds)}</span>
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
        <Button variant="destructive" className="gap-2" onClick={() => setReplacementOpen(true)}>
          <RefreshCcw className="size-4" /> Створити замінювальний процес
        </Button>
        <Button onClick={() => navigate('/prosthetics')}>До панелі управління</Button>
      </div>

      <Dialog open={replacementOpen} onOpenChange={setReplacementOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Замінювальний процес</DialogTitle>
            <DialogDescription>
              Буде створено новий процес для замовлення {instance.orderNumber ?? instance.orderId} з
              тим самим шаблоном. Провалений процес залишиться в історії як незмінний запис.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setReplacementOpen(false)}>
              Скасувати
            </Button>
            <Button variant="destructive" disabled={creatingReplacement} onClick={() => void createReplacement()}>
              {creatingReplacement ? 'Створення…' : 'Створити замінювальний процес'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
