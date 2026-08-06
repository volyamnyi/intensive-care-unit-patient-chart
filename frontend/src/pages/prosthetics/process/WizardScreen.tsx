import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Camera,
  Check,
  ClipboardCheck,
  ClipboardList,
  Home,
  PauseCircle,
  PenLine,
  Plus,
  Save,
  Timer,
  Upload,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Progress } from '@/components/ui/progress';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { flowInstanceApi, prostheticsOrderApi, prostheticsPatientApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';
import { useAuth } from '@/services/AuthContext';
import { StatusBadge } from '@/components/prosthetics/StatusBadge';
import { QualityGatePanel } from '@/components/prosthetics/QualityGatePanel';
import { computeProgress, fmt, validateElementValues } from '@/prosthetics/validation';
import type {
  FlowInstance,
  GateDecision,
  PauseCategory,
  ResourceUsageRequest,
  SnapshotElement,
  SnapshotStage,
  SnapshotStep,
  SnapshotTemplate,
} from '@/prosthetics/types';

const PAUSE_OPTIONS: { value: PauseCategory; label: string }[] = [
  { value: 'PATIENT', label: 'Очікування пацієнта' },
  { value: 'MATERIAL', label: 'Відсутні матеріали' },
  { value: 'TECH_IDLE', label: 'Технологічний простій (сушіння/полімеризація)' },
];

const RESOURCE_UNITS = ['шт', 'кг', 'л', 'м²'];

const STEP_TYPE_LABEL: Record<string, string> = {
  INFORMATION: 'інформація',
  MEASUREMENT: 'вимірювання',
  CHECKLIST: 'чек-лист',
  MEDIA: 'фото/медіа',
  SELECTION: 'вибір',
  COMPOSITE: 'комплексний',
};

export default function WizardScreen() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { hasRole } = useAuth();

  const [instance, setInstance] = useState<FlowInstance | null>(null);
  const [snapshot, setSnapshot] = useState<SnapshotTemplate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [values, setValues] = useState<Record<string, unknown>>({});
  const [touched, setTouched] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [pauseOpen, setPauseOpen] = useState(false);
  const [pauseCategory, setPauseCategory] = useState<PauseCategory>('PATIENT');
  const [material, setMaterial] = useState({ material: '', qty: '', unit: 'шт', minutes: '' });
  const [resources, setResources] = useState<ResourceUsageRequest[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [starting, setStarting] = useState(false);
  const [orderInfo, setOrderInfo] = useState<{ orderNumber: string; patientPib: string } | null>(null);
  const restoredKey = useRef<string | null>(null);
  const prevStepId = useRef<string | null>(null);

  useEffect(() => {
    document.title = 'Виконання кроку — Wizard техпроцесу';
  }, []);

  const applyInstance = useCallback(
    (next: FlowInstance) => {
      setInstance(next);
      if (next.status === 'COMPLETED') {
        toast.success('Процес успішно завершено');
        navigate(`/prosthetics/process/${next.id}/done`, { replace: true });
      } else if (next.status === 'FAILED' || next.status === 'FAILED_QC') {
        navigate(`/prosthetics/process/${next.id}/failed`, { replace: true });
      }
    },
    [navigate],
  );

  useEffect(() => {
    const load = async () => {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const instRes = await flowInstanceApi.getById(id);
        const snapRes = await flowInstanceApi.getSnapshot(id);
        setSnapshot(snapRes.data);
        let inst = instRes.data;
        if (inst.status === 'NEW') {
          setStarting(true);
          const started = await flowInstanceApi.start(id);
          inst = started.data;
        }
        setInstance(inst);
      } catch (err) {
        setError(getErrorMessage(err, 'Не вдалося завантажити процес'));
      } finally {
        setStarting(false);
        setLoading(false);
      }
    };
    load();
  }, [id]);

  useEffect(() => {
    if (!instance) return;
    let cancelled = false;
    (async () => {
      try {
        const order = await prostheticsOrderApi.getById(instance.orderId);
        if (cancelled) return;
        if (instance.patientPib) {
          setOrderInfo({ orderNumber: order.data.orderNumber, patientPib: instance.patientPib });
          return;
        }
        const patient = await prostheticsPatientApi.getById(order.data.patientId);
        if (!cancelled) {
          setOrderInfo({ orderNumber: order.data.orderNumber, patientPib: patient.data.pib });
        }
      } catch {
        // header info is non-critical
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [instance]);

  useEffect(() => {
    if (!instance || instance.status !== 'IN_PROGRESS' || !instance.currentExecutionId) return;
    const key = `${instance.id}:${instance.currentExecutionId}`;
    if (restoredKey.current === key) return;
    restoredKey.current = key;
    flowInstanceApi
      .listExecutions(instance.id)
      .then((res) => {
        const current = res.data.find((e) => e.id === instance.currentExecutionId);
        if (!current) return;
        if (current.values) {
          try {
            setValues(JSON.parse(current.values) as Record<string, unknown>);
          } catch {
            // ignore corrupted draft
          }
        }
        if (current.startedAt) {
          const elapsed = Math.max(
            0,
            Math.floor((Date.now() - new Date(current.startedAt).getTime()) / 1000),
          );
          if (elapsed > 0) setSeconds(elapsed);
        }
      })
      .catch(() => {
        // draft restore is best-effort
      });
  }, [instance]);

  const stage = useMemo<SnapshotStage | null>(() => {
    if (!snapshot || !instance?.currentStageId) return null;
    return snapshot.stages.find((s) => s.id === instance.currentStageId) ?? null;
  }, [snapshot, instance?.currentStageId]);

  const step = useMemo<SnapshotStep | null>(() => {
    if (!stage || !instance?.currentStepId) return null;
    return stage.steps.find((s) => s.id === instance.currentStepId) ?? null;
  }, [stage, instance?.currentStepId]);

  const stageIndex = useMemo(
    () => (snapshot ? Math.max(0, snapshot.stages.findIndex((s) => s.id === instance?.currentStageId)) : 0),
    [snapshot, instance?.currentStageId],
  );

  const totalSteps = snapshot?.stages.reduce((a, s) => a + s.steps.length, 0) ?? 0;

  const stepsDone = useMemo(() => {
    if (!snapshot || !stage) return 0;
    const prior = snapshot.stages.slice(0, stageIndex).reduce((a, s) => a + s.steps.length, 0);
    const currentIdx = Math.max(
      0,
      stage.steps.findIndex((s) => s.id === instance?.currentStepId),
    );
    return prior + currentIdx;
  }, [snapshot, stage, stageIndex, instance?.currentStepId]);

  const stepIndexInStage = useMemo(
    () => Math.max(0, stage?.steps.findIndex((s) => s.id === instance?.currentStepId) ?? 0),
    [stage, instance?.currentStepId],
  );

  const progress = computeProgress(stepsDone, totalSteps);

  const isLastStepOfStage = stepIndexInStage === (stage?.steps.length ?? 0) - 1 && stage != null;
  const isLastStage = stageIndex === (snapshot?.stages.length ?? 1) - 1;
  const nextStageHasGate = !isLastStage && !!snapshot?.stages[stageIndex + 1]?.gate;
  const canGoBack = stepIndexInStage > 0 && !!stage?.steps[stepIndexInStage - 1]?.allowBackward;
  const ctaLabel = isLastStepOfStage && isLastStage
    ? 'Завершити процес'
    : isLastStepOfStage && nextStageHasGate
      ? 'Контроль якості →'
      : 'Готово →';

  useEffect(() => {
    const current = step?.id ?? null;
    if (prevStepId.current !== null && current !== prevStepId.current) {
      setValues({});
      setTouched(false);
      setSeconds(0);
      setResources([]);
      setMaterial({ material: '', qty: '', unit: 'шт', minutes: '' });
    }
    prevStepId.current = current;
  }, [step?.id]);

  const timerRunning = instance?.status === 'IN_PROGRESS' && !!step;

  useEffect(() => {
    if (!timerRunning) return;
    const t = setInterval(() => setSeconds((s) => s + 1), 1000);
    return () => clearInterval(t);
  }, [timerRunning]);

  const invalid = useMemo(
    () => validateElementValues(step?.elements ?? [], values),
    [step, values],
  );

  const blocked = Object.keys(invalid).length > 0;

  const completeStep = async () => {
    setTouched(true);
    if (blocked) {
      toast.error("Заповніть усі обов'язкові поля кроку.");
      return;
    }
    if (!instance?.currentExecutionId) {
      toast.error('Активне виконання кроку не знайдено.');
      return;
    }
    setSubmitting(true);
    try {
      const res = await flowInstanceApi.completeStep(instance.id, instance.currentExecutionId, {
        values: JSON.stringify(values),
        resources: resources.length > 0 ? resources : undefined,
      });
      toast.success(step ? `Крок "${step.name}" завершено` : 'Крок завершено');
      applyInstance(res.data);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося завершити крок'));
    } finally {
      setSubmitting(false);
    }
  };

  const saveDraft = async () => {
    if (!instance?.currentExecutionId) {
      toast.error('Активне виконання кроку не знайдено.');
      return;
    }
    setSubmitting(true);
    try {
      await flowInstanceApi.saveDraft(instance.id, instance.currentExecutionId, {
        values: JSON.stringify(values),
        resources: resources.length > 0 ? resources : undefined,
      });
      toast.success('Чернетку збережено');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося зберегти чернетку'));
    } finally {
      setSubmitting(false);
    }
  };

  const goBack = async () => {
    if (!instance) return;
    setSubmitting(true);
    try {
      const res = await flowInstanceApi.backward(instance.id);
      setInstance(res.data);
      toast.info('Повернуто до попереднього кроку');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося повернутися до попереднього кроку'));
    } finally {
      setSubmitting(false);
    }
  };

  const decideGate = async (
    decision: GateDecision,
    comment?: string,
    criteriaConfirmed?: string[],
  ) => {
    if (!instance || !stage?.gate) return;
    setSubmitting(true);
    try {
      const res = await flowInstanceApi.decideGate(instance.id, stage.gate.id, {
        decision,
        criteriaConfirmed,
        comment,
      });
      if (decision === 'PASS') toast.success('Контрольну точку пройдено');
      else if (decision === 'REWORK') toast.warning('Створено петлю повернення на доопрацювання');
      else toast.error('Процес позначено як провалений');
      applyInstance(res.data);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося зберегти рішення контрольної точки'));
    } finally {
      setSubmitting(false);
    }
  };

  const confirmPause = async () => {
    if (!instance) return;
    setSubmitting(true);
    try {
      const res = await flowInstanceApi.pause(instance.id, { category: pauseCategory });
      setInstance(res.data);
      setPauseOpen(false);
      toast.info('Роботу призупинено');
      navigate(`/prosthetics/process/${instance.id}`);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося призупинити процес'));
    } finally {
      setSubmitting(false);
    }
  };

  const resumeInstance = async () => {
    if (!instance) return;
    setSubmitting(true);
    try {
      const res = await flowInstanceApi.resume(instance.id);
      setInstance(res.data);
      toast.success('Роботу відновлено');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося відновити процес'));
    } finally {
      setSubmitting(false);
    }
  };

  const uploadEvidence = async (e: SnapshotElement, file: File) => {
    if (!instance?.currentExecutionId) {
      toast.error('Активне виконання кроку не знайдено.');
      return;
    }
    try {
      const res = await flowInstanceApi.uploadEvidence(instance.id, instance.currentExecutionId, file);
      setValues((s) => ({ ...s, [e.id]: res.data.fileName }));
      toast.success('Файл завантажено');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося завантажити файл'));
    }
  };

  if (loading || starting) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-16 w-full" />
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_380px]">
          <Skeleton className="h-96" />
          <Skeleton className="h-64" />
        </div>
      </div>
    );
  }

  if (error || !instance || !snapshot) {
    return (
      <div className="py-16 text-center">
        <h1 className="font-display text-xl font-semibold">Крок недоступний</h1>
        <p className="mt-2 text-sm text-muted-foreground">{error ?? 'Процес не знайдено'}</p>
        <Button className="mt-4" onClick={() => navigate('/prosthetics')}>
          До панелі управління
        </Button>
      </div>
    );
  }

  if (instance.status === 'PAUSED') {
    return (
      <div className="mx-auto max-w-xl py-16 text-center">
        <PauseCircle className="mx-auto size-12 text-warning" />
        <h1 className="mt-4 font-display text-xl font-semibold">Процес призупинено</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Причина: {PAUSE_OPTIONS.find((o) => o.value === instance.pauseCategory)?.label ?? instance.pauseCategory}
        </p>
        <div className="mt-6 flex justify-center gap-3">
          <Button variant="outline" onClick={() => navigate(`/prosthetics/process/${instance.id}`)}>
            До огляду процесу
          </Button>
          <Button disabled={submitting} onClick={() => void resumeInstance()}>
            Продовжити роботу
          </Button>
        </div>
      </div>
    );
  }

  if (instance.status === 'COMPLETED') {
    return (
      <div className="py-16 text-center">
        <h1 className="font-display text-xl font-semibold">Процес завершено</h1>
        <Button className="mt-4" onClick={() => navigate(`/prosthetics/process/${instance.id}/done`)}>
          Переглянути підсумок
        </Button>
      </div>
    );
  }

  if (instance.status === 'FAILED' || instance.status === 'FAILED_QC') {
    return (
      <div className="py-16 text-center">
        <h1 className="font-display text-xl font-semibold">Процес зупинено (брак)</h1>
        <Button className="mt-4" onClick={() => navigate(`/prosthetics/process/${instance.id}/failed`)}>
          Переглянути звіт про провал
        </Button>
      </div>
    );
  }

  if (instance.status === 'NEW') {
    return (
      <div className="py-16 text-center">
        <h1 className="font-display text-xl font-semibold">Процес готовий до запуску</h1>
        <Button
          className="mt-4"
          disabled={submitting}
          onClick={() => void (async () => {
            setSubmitting(true);
            try {
              const res = await flowInstanceApi.start(instance.id);
              setInstance(res.data);
            } catch (err) {
              toast.error(getErrorMessage(err, 'Не вдалося запустити процес'));
            } finally {
              setSubmitting(false);
            }
          })()}
        >
          Розпочати процес
        </Button>
      </div>
    );
  }

  if (instance.status === 'WAITING_REVIEW') {
    if (!stage?.gate) {
      return (
        <div className="py-16 text-center">
          <h1 className="font-display text-xl font-semibold">Очікування перевірки</h1>
          <p className="mt-2 text-sm text-muted-foreground">Контрольна точка не знайдена.</p>
          <Button className="mt-4" onClick={() => navigate(`/prosthetics/process/${instance.id}`)}>
            До огляду процесу
          </Button>
        </div>
      );
    }
    return (
      <QualityGatePanel
        instance={instance}
        stage={stage}
        isApprover={hasRole('PROSTHETICS_ADMINISTRATOR')}
        submitting={submitting}
        onPass={(criteriaConfirmed) => void decideGate('PASS', undefined, criteriaConfirmed)}
        onRework={(comment) => void decideGate('REWORK', comment)}
        onFail={(comment) => void decideGate('FAIL', comment)}
      />
    );
  }

  if (!stage || !step) {
    return (
      <div className="py-16 text-center">
        <h1 className="font-display text-xl font-semibold">Крок недоступний</h1>
        <Button className="mt-4" onClick={() => navigate(`/prosthetics/process/${instance.id}`)}>
          До огляду процесу
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="sticky top-0 z-20 -mx-6 border-b bg-card/95 px-6 py-3 backdrop-blur">
        <div className="flex flex-wrap items-center gap-4">
          <div>
            <div className="font-display text-sm font-semibold">{snapshot.name}</div>
            <div className="text-xs text-muted-foreground">
              {orderInfo ? `${orderInfo.patientPib} · ${orderInfo.orderNumber} · ` : ''}
              {snapshot.productType} · {snapshot.amputationLevel ?? ''} {snapshot.limbSide ?? ''} · #{instance.id.slice(0, 8)}
            </div>
          </div>
          <StatusBadge status={instance.status} />
          <div className="ml-auto flex items-center gap-2 rounded-md bg-primary px-3 py-1.5 font-mono text-sm text-primary-foreground">
            <Timer className="size-4" /> {fmt(seconds)}
          </div>
        </div>
        <div className="mt-3">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>
              Етап {stageIndex + 1} з {snapshot.stages.length}: {stage.name}
            </span>
            <span>
              Крок {stepIndexInStage + 1} з {stage.steps.length} · загалом {stepsDone}/{totalSteps}
            </span>
          </div>
          <Progress value={progress} className="mt-2" />
        </div>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          {snapshot.stages.map((s, i) => (
            <span
              key={s.id}
              className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs ${
                i === stageIndex ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'
              }`}
            >
              {s.gate != null && <ClipboardCheck className="size-3" />}
              {i + 1}. {s.name}
            </span>
          ))}
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_380px]">
        <Card>
          <CardHeader>
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="outline">{STEP_TYPE_LABEL[step.stepType] ?? step.stepType}</Badge>
              {step.mandatory && (
                <Badge className="border-transparent bg-accent text-accent-foreground">Обов'язковий</Badge>
              )}
              {step.normDurationMin != null && (
                <span className="text-xs text-muted-foreground">норматив: {step.normDurationMin} хв</span>
              )}
            </div>
            <CardTitle className="mt-2 text-xl">{step.name}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="rounded-md border-l-4 border-mint bg-muted p-4 text-sm">
              Виконайте крок та заповніть обов&apos;язкові поля. Після підтвердження крок буде
              зафіксовано в журналі процесу.
            </div>

            <div className="space-y-5">
              {step.elements.map((e) => (
                <ElementField
                  key={e.id}
                  element={e}
                  value={values[e.id]}
                  error={touched ? invalid[e.id] : undefined}
                  onChange={(v) => setValues((s) => ({ ...s, [e.id]: v }))}
                  onUpload={(file) => void uploadEvidence(e, file)}
                />
              ))}
            </div>
          </CardContent>
        </Card>

        <Card className="h-fit">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <ClipboardList className="size-4" /> Витрати ресурсів
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-2">
              <Input
                placeholder="Матеріал"
                value={material.material}
                onChange={(ev) => setMaterial({ ...material, material: ev.target.value })}
                className="col-span-2"
              />
              <Input
                placeholder="Кількість"
                value={material.qty}
                onChange={(ev) => setMaterial({ ...material, qty: ev.target.value })}
              />
              <Select value={material.unit} onValueChange={(v) => setMaterial({ ...material, unit: v ?? 'шт' })}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {RESOURCE_UNITS.map((u) => (
                    <SelectItem key={u} value={u}>
                      {u}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Input
                placeholder="Час, хв"
                value={material.minutes}
                onChange={(ev) => setMaterial({ ...material, minutes: ev.target.value })}
              />
              <Button
                variant="outline"
                onClick={() => {
                  if (!material.material.trim()) return;
                  setResources((s) => [
                    ...s,
                    {
                      material: material.material.trim(),
                      quantity: material.qty ? Number(material.qty) : null,
                      unit: material.unit,
                      minutes: material.minutes ? Number(material.minutes) : null,
                    },
                  ]);
                  setMaterial({ material: '', qty: '', unit: 'шт', minutes: '' });
                }}
              >
                <Plus className="size-4" /> Додати
              </Button>
            </div>

            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Матеріал</TableHead>
                  <TableHead>К-сть</TableHead>
                  <TableHead>Час</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {resources.map((r, i) => (
                  <TableRow key={`${r.material}-${i}`}>
                    <TableCell>{r.material}</TableCell>
                    <TableCell>
                      {r.quantity ?? '—'} {r.unit}
                    </TableCell>
                    <TableCell>{r.minutes ?? 0} хв</TableCell>
                  </TableRow>
                ))}
                {resources.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={3} className="text-center text-muted-foreground">
                      Витрат не зафіксовано
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>

      <div className="sticky bottom-0 z-20 -mx-6 flex flex-wrap items-center gap-3 border-t bg-card px-6 py-3">
        <Button variant="outline" disabled={!canGoBack || submitting} onClick={() => void goBack()}>
          <ArrowLeft className="size-4" /> Попередній
        </Button>
        <Button variant="outline" disabled={submitting} onClick={() => void saveDraft()}>
          <Save className="size-4" /> Зберегти чернетку
        </Button>
        <Button variant="ghost" onClick={() => setPauseOpen(true)}>
          <PauseCircle className="size-4" /> Пауза
        </Button>
        <Button variant="ghost" onClick={() => navigate('/prosthetics')}>
          <Home className="size-4" /> До головного меню
        </Button>
        <Button
          className="ml-auto bg-accent text-accent-foreground hover:bg-accent/90"
          disabled={(touched && blocked) || submitting}
          onClick={() => void completeStep()}
        >
          <Check className="size-4" /> {ctaLabel}
        </Button>
      </div>

      <Dialog open={pauseOpen} onOpenChange={setPauseOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Призупинення роботи</DialogTitle>
            <DialogDescription>
              Оберіть причину паузи — вона буде зафіксована в журналі аудиту.
            </DialogDescription>
          </DialogHeader>
          <RadioGroup value={pauseCategory} onValueChange={(v) => setPauseCategory(v as PauseCategory)} className="gap-3">
            {PAUSE_OPTIONS.map((o) => (
              <div key={o.value} className="flex items-center gap-2">
                <RadioGroupItem value={o.value} id={o.value} />
                <Label htmlFor={o.value}>{o.label}</Label>
              </div>
            ))}
          </RadioGroup>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPauseOpen(false)}>
              Скасувати
            </Button>
            <Button disabled={submitting} onClick={() => void confirmPause()}>
              Призупинити
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function ElementField({
  element,
  value,
  error,
  onChange,
  onUpload,
}: {
  element: SnapshotElement;
  value: unknown;
  error?: string | undefined;
  onChange: (v: unknown) => void;
  onUpload: (file: File) => void;
}) {
  const label = (
    <Label htmlFor={element.id} id={`${element.id}-label`} className="text-sm">
      {element.label}
      {element.unit ? `, ${element.unit}` : ''}
      {element.required && <span className="text-accent">*</span>}
    </Label>
  );

  const errClass = error ? 'border-destructive ring-1 ring-destructive' : '';

  return (
    <div className="space-y-2">
      {element.elementType !== 'CHECKBOX' && label}
      {element.elementType === 'CHECKBOX' && (
        <div className="flex items-center gap-3 rounded-md border p-3">
          <Checkbox
            id={element.id}
            checked={value === true}
            onCheckedChange={(c) => onChange(c === true)}
          />
          {label}
        </div>
      )}
      {element.elementType === 'TEXT_INPUT' && (
        <Input
          id={element.id}
          className={errClass}
          value={(value as string) ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
      {element.elementType === 'NUMERIC_INPUT' && (
        <Input
          id={element.id}
          type="number"
          inputMode="decimal"
          className={errClass}
          value={(value as string) ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
      {element.elementType === 'TEXTAREA' && (
        <Textarea
          id={element.id}
          className={errClass}
          rows={3}
          value={(value as string) ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
      {element.elementType === 'DROPDOWN' && (
        <Select value={(value as string) ?? ''} onValueChange={(v) => onChange(v)}>
          <SelectTrigger id={element.id} className={errClass}>
            <SelectValue placeholder="Оберіть значення" />
          </SelectTrigger>
          <SelectContent>
            {(element.options ?? []).map((o) => (
              <SelectItem key={o} value={o}>
                {o}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      )}
      {element.elementType === 'RADIO' && (
        <RadioGroup value={(value as string) ?? ''} onValueChange={(v) => onChange(v)} className="gap-2">
          {(element.options ?? []).map((o) => (
            <div key={o} className="flex items-center gap-2">
              <RadioGroupItem value={o} id={`${element.id}-${o}`} />
              <Label htmlFor={`${element.id}-${o}`}>{o}</Label>
            </div>
          ))}
        </RadioGroup>
      )}
      {element.elementType === 'DATE_PICKER' && (
        <Input
          id={element.id}
          type="date"
          className={errClass}
          value={(value as string) ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
      {(element.elementType === 'IMAGE_UPLOAD' || element.elementType === 'FILE_UPLOAD') && (
        <div
          className={`flex w-full items-center justify-center gap-2 rounded-md border border-dashed p-6 text-sm text-muted-foreground ${errClass}`}
        >
          {value ? (
            <div className="flex items-center gap-2">
              <span className="flex items-center gap-2">
                <Check className="size-4 text-success" />
                Завантажено: {String(value)}
              </span>
              <Button type="button" variant="outline" size="sm" onClick={() => onChange(undefined)}>
                Прибрати
              </Button>
            </div>
          ) : (
            <label className="flex w-full cursor-pointer items-center justify-center gap-2">
              <Input
                type="file"
                className="hidden"
                accept={element.mimeTypes?.join(',') ?? undefined}
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) onUpload(file);
                  e.target.value = '';
                }}
              />
              <span className="flex items-center gap-2">
                {element.elementType === 'IMAGE_UPLOAD' ? (
                  <Camera className="size-4" />
                ) : (
                  <Upload className="size-4" />
                )}
                Зробити фото або завантажити файл
              </span>
            </label>
          )}
        </div>
      )}
      {element.elementType === 'SIGNATURE_CAPTURE' && (
        <button
          type="button"
          onClick={() => onChange(value ? undefined : 'signed')}
          className={`flex h-28 w-full items-center justify-center gap-2 rounded-md border border-dashed text-sm text-muted-foreground transition-colors hover:bg-muted ${errClass}`}
        >
          <PenLine className="size-4" />
          {value ? 'Підпис отримано' : 'Область для електронного підпису'}
        </button>
      )}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}
