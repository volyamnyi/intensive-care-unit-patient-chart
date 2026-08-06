import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Circle, Diamond, RotateCcw, ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/skeleton';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { flowInstanceApi, flowTemplateApi } from '@/api/prosthetics';
import { StatusBadge } from '@/components/prosthetics/StatusBadge';
import type { FlowInstance, FlowTemplate } from '@/prosthetics/types';

export default function ProcessOverview() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { patient, orders } = useProsthetics();
  const [instance, setInstance] = useState<FlowInstance | null>(null);
  const [template, setTemplate] = useState<FlowTemplate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Огляд технологічної карти — Виробництво протезів';
    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        const instRes = await flowInstanceApi.getById(id!);
        const inst = instRes.data;
        setInstance(inst);
        const tplRes = await flowTemplateApi.getById(inst.templateId);
        setTemplate(tplRes.data);
      } catch {
        setError('Не вдалося завантажити дані процесу');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  const totalSteps = template?.stages.reduce((a, s) => a + s.steps.length, 0) ?? 0;
  const completedSteps = instance?.reworkCount !== null && instance?.reworkCount !== undefined 
    ? 0 // We don't have completedSteps in the backend type, need to check
    : 0;
  const progress = totalSteps > 0 ? Math.round((completedSteps / totalSteps) * 100) : 0;

  const formatHours = (minutes: number) => {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return h > 0 ? `${h} год ${m} хв` : `${m} хв`;
  };

  if (loading) {
    return (
      <div className="container mx-auto max-w-[1400px] px-6 py-8">
        <div className="space-y-6">
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-8 w-48" />
          <div className="grid gap-6 xl:grid-cols-[280px_minmax(0,1fr)_300px]">
            <Skeleton className="h-96" />
            <Skeleton className="h-96" />
            <Skeleton className="h-96" />
          </div>
        </div>
      </div>
    );
  }

  if (error || !instance || !template) {
    return (
      <div className="container mx-auto max-w-[1400px] px-6 py-16 text-center">
        <h1 className="font-display text-xl font-semibold">Процес не знайдено</h1>
        <Button className="mt-4" onClick={() => navigate('/prosthetics')}>
          До панелі управління
        </Button>
      </div>
    );
  }

  // Get patient info
  const order = orders.find(o => o.id === instance.orderId);
  const patientName = order ? patient?.pib || order.patientId : instance.patientId;

  return (
    <div className="container mx-auto max-w-[1400px] px-6 py-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-semibold">Огляд технологічної карти</h1>
          <p className="text-sm text-muted-foreground">
            {instance.id} · {template.name} (v{template.templateVersion})
          </p>
        </div>
        <StatusBadge status={instance.status} />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[280px_minmax(0,1fr)_300px]">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Структура процесу</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 text-sm">
            {template.stages.map((stage) => (
              <div key={stage.id}>
                <div className="font-medium">
                  Етап {stage.type}. {stage.name}
                </div>
                <ul className="mt-1 space-y-1 border-l pl-4">
                  {stage.steps.map((step) => (
                    <li key={step.id} className="flex items-start gap-2 text-muted-foreground">
                      <Circle className="mt-0.5 size-3.5" />
                      <span>{step.name}</span>
                    </li>
                  ))}
                  {stage.gate && (
                    <li className="flex items-start gap-2 text-accent">
                      <Diamond className="mt-0.5 size-3.5" />
                      <span>{stage.gate.name}</span>
                    </li>
                  )}
                </ul>
              </div>
            ))}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Діаграма процесу</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {template.stages.map((stage, idx) => (
              <div key={stage.id} className="space-y-3">
                <div className="flex flex-wrap items-center gap-3">
                  <div className="min-w-[220px] flex-1 rounded-md border-2 border-primary bg-card px-4 py-3">
                    <div className="text-xs text-muted-foreground">Етап {stage.type} · {stage.type}</div>
                    <div className="font-medium">{stage.name}</div>
                    <div className="mt-1 text-xs text-muted-foreground">{stage.steps.length} кроків</div>
                  </div>
                  {stage.gate && (
                    <div className="rotate-45 border-2 border-accent bg-accent/10 p-4">
                      <span className="block -rotate-45 text-center text-[10px] leading-tight font-semibold text-accent">
                        QG
                        <br />
                        {stage.type}
                      </span>
                    </div>
                  )}
                </div>
                {idx < template.stages.length - 1 && (
                  <div className="flex items-center gap-2 pl-4 text-xs text-muted-foreground">
                    <span className="h-6 w-px bg-border" />
                    <RotateCcw className="size-3.5" /> петля повернення (rework) на етап {stage.type}
                  </div>
                )}
              </div>
            ))}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Метадані процесу</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div>
              <div className="text-xs text-muted-foreground">Пацієнт</div>
              <div className="font-medium">{patientName}</div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Замовлення</div>
              <div className="font-medium">{order?.orderNumber || instance.orderId}</div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Виконавець</div>
              <div className="font-medium">{instance.assignedUserId?.toString() || '—'}</div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Орієнтовна тривалість</div>
              <div className="font-medium">{formatHours(template.estimatedDurationMin)}</div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Прогрес</div>
              <Progress value={progress} className="mt-2" />
              <div className="mt-1 text-xs text-muted-foreground">
                {completedSteps} / {totalSteps} кроків
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="mt-8 flex flex-wrap justify-between gap-3">
        <Button variant="outline" onClick={() => navigate('/prosthetics')}>
          <ArrowLeft className="mr-2 size-4" />
          Назад
        </Button>
        {instance.status === 'FAILED' || instance.status === 'FAILED_QC' ? (
          <Button
            variant="destructive"
            onClick={() => navigate(`/prosthetics/process/${instance.id}/failed`)}
          >
            Переглянути звіт про провал
          </Button>
        ) : instance.status === 'COMPLETED' ? (
          <Button onClick={() => navigate(`/prosthetics/process/${instance.id}/done`)}>
            Переглянути підсумок
          </Button>
        ) : (
          <Button
            className="bg-accent text-accent-foreground hover:bg-accent/90"
            onClick={() => navigate(`/prosthetics/process/${instance.id}/wizard`)}
          >
            {instance.status === 'NEW' ? 'Розпочати процес' : 'Продовжити виконання'}
          </Button>
        )}
      </div>
    </div>
  );
}