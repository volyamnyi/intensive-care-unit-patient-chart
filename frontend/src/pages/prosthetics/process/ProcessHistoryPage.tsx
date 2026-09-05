import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  CheckCircle2,
  ChevronLeft,
  Circle,
  Clock,
  PauseCircle,
  Play,
  XCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { flowInstanceApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';
import type {
  BrakEvent,
  FlowInstance,
  SnapshotTemplate,
  StepExecution,
} from '@/prosthetics/types';

type HistoryEventKind = 'created' | 'start' | 'step' | 'pause' | 'resume' | 'brak' | 'end';

interface HistoryEvent {
  id: string;
  kind: HistoryEventKind;
  title: string;
  description?: string;
  detail?: string;
  timestamp: string;
}

const KIND_LABELS: Record<string, string> = {
  created: 'Процес створено',
  start: 'Процес розпочато',
  step: 'Крок',
  pause: 'Пауза',
  resume: 'Відновлення',
  brak: 'Брак',
  end: 'Завершення',
};

function eventIcon(kind: HistoryEventKind, detail?: string) {
  if (kind === 'brak') return <XCircle className="size-4" />;
  if (kind === 'created' || kind === 'start' || kind === 'resume') {
    return <Play className="size-4" />;
  }
  if (kind === 'pause') return <PauseCircle className="size-4" />;
  if (kind === 'end') {
    return detail === 'COMPLETED' ? <CheckCircle2 className="size-4" /> : <XCircle className="size-4" />;
  }
  if (kind === 'step') {
    if (detail === 'COMPLETED') return <CheckCircle2 className="size-4" />;
    if (detail === 'CANCELLED') return <XCircle className="size-4" />;
    return <Circle className="size-4" />;
  }
  return <Clock className="size-4" />;
}

function eventColor(kind: HistoryEventKind, detail?: string) {
  if (kind === 'brak') return 'bg-destructive/10 text-destructive border-destructive/40';
  if (kind === 'step' && detail === 'COMPLETED') return 'bg-success/10 text-success border-success/40';
  if (kind === 'pause') return 'bg-warning/10 text-warning border-warning/40';
  if (kind === 'end' && detail === 'COMPLETED') return 'bg-success/10 text-success border-success/40';
  if (kind === 'end') return 'bg-destructive/10 text-destructive border-destructive/40';
  return 'bg-muted text-muted-foreground border-border';
}

function toStepMap(snapshot: SnapshotTemplate): Map<string, string> {
  const map = new Map<string, string>();
  for (const stage of snapshot.stages) {
    for (const step of stage.steps) {
      map.set(step.id, step.name);
    }
  }
  return map;
}

function buildEvents(
  instance: FlowInstance,
  executions: StepExecution[],
  stepNames: Map<string, string>,
  brakEvents: BrakEvent[] = [],
): HistoryEvent[] {
  const events: HistoryEvent[] = [
    {
      id: 'created',
      kind: 'created',
      title: 'Процес створено',
      description: `${instance.orderNumber ?? instance.orderId} · ${instance.templateName ?? ''}`,
      timestamp: instance.createdAt,
    },
  ];
  if (instance.startTime) {
    events.push({
      id: 'start',
      kind: 'start',
      title: 'Процес розпочато',
      description: 'Перший крок технологічного маршруту',
      timestamp: instance.startTime,
    });
  }
  for (const execution of executions) {
    const stepName = stepNames.get(execution.stepId) ?? execution.stepId;
    if (execution.startedAt) {
      events.push({
        id: `exec-${execution.id}-start`,
        kind: 'step',
        title: `Крок розпочато: ${stepName}`,
        description: `Спроба ${execution.attemptNumber ?? 1}`,
        timestamp: execution.startedAt,
      });
    }
    if (execution.completedAt) {
      const done = execution.status === 'COMPLETED';
      events.push({
        id: `exec-${execution.id}-end`,
        kind: 'step',
        title: done ? `Крок завершено: ${stepName}` : `Крок скасовано: ${stepName}`,
        description: done
          ? `Тривалість ${Math.round((execution.activeSeconds ?? 0) / 60)} хв`
          : 'Повернення до попереднього кроку',
        detail: done ? 'COMPLETED' : 'CANCELLED',
        timestamp: execution.completedAt,
      });
    }
  }
  for (const brak of brakEvents) {
    const parts: string[] = [];
    if (brak.softTissueMisalignment) parts.push('м’які тканини');
    if (brak.painDiscomfort) parts.push('біль/дискомфорт');
    events.push({
      id: `brak-${brak.id}`,
      kind: 'brak',
      title: `Брак: ${parts.length ? parts.join(', ') : 'примітка'}`,
      description: `${brak.note ?? ''}${brak.returnStageName ? ` → ${brak.returnStageName}` : ''}${brak.newInstanceId ? ` (гілка ${brak.newInstanceId.slice(0, 8)})` : ''}`.trim() || undefined,
      detail: 'BRAK',
      timestamp: brak.createdAt ?? instance.createdAt,
    });
  }
  if (instance.pausedAt) {
    events.push({
      id: 'pause',
      kind: 'pause',
      title: 'Роботу призупинено',
      description: instance.pauseCategory ?? undefined,
      timestamp: instance.pausedAt,
    });
  }
  if (instance.resumedAt) {
    events.push({
      id: 'resume',
      kind: 'resume',
      title: 'Роботу відновлено',
      timestamp: instance.resumedAt,
    });
  }
  if (instance.endTime) {
    const done = instance.status === 'COMPLETED';
    events.push({
      id: 'end',
      kind: 'end',
      title: done ? 'Процес завершено успішно' : 'Процес зупинено (брак)',
      description: done ? undefined : instance.failReason ?? undefined,
      detail: done ? 'COMPLETED' : 'FAILED',
      timestamp: instance.endTime,
    });
  }
  return events
    .filter((e) => Boolean(e.timestamp))
    .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
}

const FILTER_OPTIONS: { value: string; label: string }[] = [
  { value: 'all', label: 'Всі події' },
  { value: 'step', label: 'Кроки' },
  { value: 'brak', label: 'Брак' },
  { value: 'pause', label: 'Паузи / відновлення' },
  { value: 'end', label: 'Завершення' },
];

export default function ProcessHistoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [instance, setInstance] = useState<FlowInstance | null>(null);
  const [snapshot, setSnapshot] = useState<SnapshotTemplate | null>(null);
  const [executions, setExecutions] = useState<StepExecution[]>([]);
  const [brakEvents, setBrakEvents] = useState<BrakEvent[]>([]);
  const [filter, setFilter] = useState<string | null>('all');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Історія процесу — Виробництво протезів';
    const load = async () => {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const [instRes, snapRes, execRes] = await Promise.all([
          flowInstanceApi.getById(id),
          flowInstanceApi.getSnapshot(id),
          flowInstanceApi.listExecutions(id),
        ]);
        setInstance(instRes.data);
        setSnapshot(snapRes.data);
        setExecutions(execRes.data);
        try {
          const brakRes = await flowInstanceApi.getBrakEvents(id);
          setBrakEvents(brakRes.data);
        } catch {
          setBrakEvents([]);
        }
      } catch (err) {
        setError(getErrorMessage(err, 'Не вдалося завантажити історію процесу'));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  const events = useMemo(() => {
    if (!instance || !snapshot) return [];
    return buildEvents(instance, executions, toStepMap(snapshot), brakEvents);
  }, [instance, snapshot, executions, brakEvents]);

  const visible = useMemo(
    () => {
      const f = filter ?? 'all';
      return f === 'all' ? events : events.filter((e) => e.kind === f || (f === 'pause' && (e.kind === 'pause' || e.kind === 'resume')));
    },
    [events, filter],
  );

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-96" />
      </div>
    );
  }

  if (error || !instance || !snapshot) {
    return (
      <div className="py-16 text-center">
        <h1 className="font-display text-xl font-semibold">Історія недоступна</h1>
        <p className="mt-2 text-sm text-muted-foreground">{error ?? 'Процес не знайдено'}</p>
        <Button className="mt-4" onClick={() => navigate(`/prosthetics/process/${id}`)}>
          До огляду процесу
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-semibold">Аудит-лог процесу</h1>
          <p className="text-sm text-muted-foreground">
            {instance.id.slice(0, 8)} · {instance.orderNumber ?? instance.orderId} ·{' '}
            {instance.patientPib ?? instance.patientId}
          </p>
        </div>
        <Button variant="outline" className="gap-2" onClick={() => navigate(`/prosthetics/process/${id}`)}>
          <ChevronLeft className="size-4" /> Огляд процесу
        </Button>
      </div>

      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="text-sm text-muted-foreground">
              Подій: {events.length}
            </div>
            <Select value={filter} onValueChange={setFilter}>
              <SelectTrigger className="w-56">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {FILTER_OPTIONS.map((o) => (
                  <SelectItem key={o.value} value={o.value}>
                    {o.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Clock className="size-4" /> Хронологія подій
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="relative">
            <div className="absolute left-[17px] top-2 bottom-2 w-px bg-border" />
            <div className="space-y-4">
              {visible.map((event) => (
                <div key={event.id} className="flex gap-4">
                  <div
                    className={`z-10 flex size-9 shrink-0 items-center justify-center rounded-full border-2 ${eventColor(event.kind, event.detail)}`}
                  >
                    {eventIcon(event.kind, event.detail)}
                  </div>
                  <div className="flex-1 rounded-md border bg-card p-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="text-sm font-medium">{event.title}</span>
                      <span className="shrink-0 text-xs text-muted-foreground">
                        {new Date(event.timestamp).toLocaleString('uk-UA')}
                      </span>
                    </div>
                    {event.description && (
                      <div className="mt-1 text-xs text-muted-foreground">{event.description}</div>
                    )}
                    <span className="mt-2 inline-block rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">
                      {KIND_LABELS[event.kind]}
                    </span>
                  </div>
                </div>
              ))}
              {visible.length === 0 && (
                <p className="py-8 text-center text-sm text-muted-foreground">
                  Немає подій за поточним фільтром
                </p>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
        <span>Події сортуються від найстарішої до найновішої</span>
      </div>
    </div>
  );
}
