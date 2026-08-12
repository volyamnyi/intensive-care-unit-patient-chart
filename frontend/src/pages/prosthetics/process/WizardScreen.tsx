import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  ArrowRight,
  Camera,
  Check,
  ClipboardCheck,
  Home,
  PauseCircle,
  PenLine,
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
import { Separator } from '@/components/ui/separator';
import { Textarea } from '@/components/ui/textarea';
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
import { MeasurementForms } from '@/pages/prosthetics/process/MeasurementForms';
import type {
  FlowInstance,
  GateDecision,
  PauseCategory,
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

const STEP_TYPE_LABEL: Record<string, string> = {
  INFORMATION: 'інформація',
  MEASUREMENT: 'вимірювання',
  CHECKLIST: 'чек-лист',
  MEDIA: 'фото/медіа',
  SELECTION: 'вибір',
  COMPOSITE: 'комплексний',
};
// Stage-1 and stage-3 steps contain a run of CHECKBOX elements whose labels are
// prefixed with «Засоби індивідуального захисту: …». They are grouped into a
// single PPE panel with an illustrative image. The data label (full prefix) is
// preserved for grouping + image lookup; the display label is the short name.

const PPE_LABEL = 'Засоби індивідуального захисту';
const PPE_LABEL_LEGACY = 'Засоби індивідуального захисту:';
const PPE_LABEL_PREFIX = 'Засоби індивідуального захисту:';
const isPpeCheckbox = (label: string): boolean =>
  label === PPE_LABEL_LEGACY || label.startsWith(PPE_LABEL_PREFIX);

// Stage-3 PPE items display under their short names; the «Засоби індивідуального
// захисту: …» prefix is already conveyed by the PPE group header.
const PPE_SHORT_LABELS: Record<string, string> = {
  'Засоби індивідуального захисту: нестерильні оглядові нітрилові рукавички':
    'Нестерильні оглядові нітрилові рукавички',
  'Засоби індивідуального захисту: захисні окуляри': 'Захисні окуляри',
  'Засоби індивідуального захисту: респіратор': 'Респіратор',
  'Засоби індивідуального захисту: захисні навушники': 'Захисні навушники',
  'Засоби індивідуального захисту: латексні рукавички підвищеної міцності':
    'Латексні рукавички підвищеної міцності',
  'Засоби індивідуального захисту: м’які тканинні терморукавиці':
    'М’які тканинні терморукавиці',
};
const ppeDisplayLabel = (label: string): string =>
  PPE_SHORT_LABELS[label] ?? (label === PPE_LABEL_LEGACY ? PPE_LABEL : label);

// PPE panel header text. Stage 3 (thermoforming) uses a dedicated header
// («Обробка гільзи засоби індивідуального захисту»); all other stages keep
// the shared default.
const PPE_GROUP_HEADER_DEFAULT = 'Засоби індивідуального захисту';
const PPE_GROUP_HEADER_BY_STAGE: Record<string, string> = {
  'd0000005-0000-0000-0000-000000000005': 'Обробка гільзи засоби індивідуального захисту',
};
const ppeGroupHeader = (stageId?: string): string =>
  (stageId && PPE_GROUP_HEADER_BY_STAGE[stageId]) || PPE_GROUP_HEADER_DEFAULT;

const PPE_IMAGE_BY_LABEL: Record<string, string | undefined> = {
  [PPE_LABEL]: '/ppe/non-sterile_gloves.png',
  [PPE_LABEL_LEGACY]: '/ppe/non-sterile_gloves.png',
  'Засоби індивідуального захисту: захисні окуляри': '/ppe/goggles_resp_ears.png',
  'Засоби індивідуального захисту: респіратор': '/ppe/goggles_resp_ears.png',
  'Засоби індивідуального захисту: захисні навушники': '/ppe/goggles_resp_ears.png',
  'Засоби індивідуального захисту: латексні рукавички підвищеної міцності':
    '/ppe/latex_thermal_gloves.png',
  'Засоби індивідуального захисту: м’які тканинні терморукавиці':
    '/ppe/latex_thermal_gloves.png',
};

const PPE_ALT_BY_LABEL: Record<string, string> = {
  [PPE_LABEL]: 'Засоби індивідуального захисту (рукавички)',
  [PPE_LABEL_LEGACY]: 'Засоби індивідуального захисту (рукавички)',
  'Засоби індивідуального захисту: захисні окуляри':
    'Засоби індивідуального захисту: респіратор із захисним екраном (захисні окуляри, респіратор)',
  'Засоби індивідуального захисту: респіратор': 'Засоби індивідуального захисту: респіратор',
  'Засоби індивідуального захисту: захисні навушники':
    'Засоби індивідуального захисту: захисні навушники',
  'Засоби індивідуального захисту: латексні рукавички підвищеної міцності':
    'Латексні рукавички підвищеної міцності на руках медичної працівниці',
  'Засоби індивідуального захисту: м’які тканинні терморукавиці':
    'М’які тканинні терморукавиці на руках медичної працівниці',
};

function PpeChecklistGroup({
  elements,
  values,
  header,
  onChange,
}: {
  elements: SnapshotElement[];
  values: Record<string, unknown>;
  header: string;
  onChange: (id: string, value: unknown) => void;
}) {
  const first = elements[0];
  const img = first ? PPE_IMAGE_BY_LABEL[first.label] : undefined;
  const alt = first ? PPE_ALT_BY_LABEL[first.label] : undefined;
  return (
    <div className="space-y-3 rounded-md border bg-muted/40 p-4">
      <p className="text-sm font-medium">{header}</p>
      {img && (
        <img src={img} alt={alt ?? header} className="h-40 w-auto rounded-md object-contain" />
      )}
      <div className="space-y-2">
        {elements.map((el) => (
          <div key={el.id} className="flex items-center gap-3 rounded-md border bg-card p-3">
            <Checkbox
              id={el.id}
              checked={values[el.id] === true}
              onCheckedChange={(c) => onChange(el.id, c === true)}
            />
            <Label htmlFor={el.id} className="text-sm">
              {ppeDisplayLabel(el.label)}
              {el.required && <span className="text-accent">*</span>}
            </Label>
          </div>
        ))}
      </div>
    </div>
  );
}

function renderElements(
  els: SnapshotElement[],
  values: Record<string, unknown>,
  stageId: string | undefined,
  stepId: string | undefined,
  onChange: (id: string, value: unknown) => void,
  onUpload: (file: File) => void,
) {
  const out: React.ReactNode[] = [];
  // The «Зняття мірок (з пацієнтом)» step renders the pixel-perfect measurement
  // forms (two anatomical diagrams in a single row). They are data-entry fields
  // whose values are stored under the element keys, gated by the backend
  // «min 3 filled measurements» rule.
  if (stepId === 'e0000002-0000-0000-0000-000000000002') {
    out.push(
      <div key="ppe-measurement" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <div className="flex flex-wrap items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">ЗІЗ</Badge>
        </div>
        <p className="text-sm text-muted-foreground">
          Перед вимірюванням підтвердіть засоби індивідуального захисту.
        </p>
        <Separator />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-[1fr_auto_auto] md:items-start">
          <div className="flex items-center gap-2 rounded-lg border bg-card px-2 py-1">
            <Checkbox
              id="ppe-measurement-non-sterile-gloves"
              checked={values['ppe-measurement-non-sterile-gloves'] === true}
              onCheckedChange={(c) => onChange('ppe-measurement-non-sterile-gloves', c === true)}
            />
            <Label htmlFor="ppe-measurement-non-sterile-gloves" className="text-xs font-medium">
              Нестерильні оглядові нітрилові рукавички
            </Label>
          </div>
          <div className="flex items-center justify-center">
            <ArrowRight className="size-8 text-primary" aria-hidden="true" />
          </div>
          <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
            <img
              src="/ppe/non-sterile_gloves.png"
              alt="Засоби індивідуального захисту: нестерильні нітрилові рукавички"
              className="h-56 w-auto rounded-lg object-contain md:h-64"
            />
          </div>
        </div>
      </div>,
    );
    out.push(
      <MeasurementForms
        key="measurement-forms"
        values={values}
        onChange={(k, v) => onChange(k, v)}
      />,
    );
    return out;
  }
  let i = 0;
  while (i < els.length) {
    if (els[i].elementType === 'STEP_MESSAGE') {
      out.push(
        <p
          key={els[i].id}
          className="rounded-md border-l-4 border-accent bg-muted p-3 text-sm font-medium"
        >
          {els[i].label}
        </p>,
      );
      i += 1;
    } else if (els[i].elementType === 'CHECKBOX' && isPpeCheckbox(els[i].label)) {
      let j = i;
      while (
        j < els.length &&
        els[j].elementType === 'CHECKBOX' &&
        isPpeCheckbox(els[j].label)
      ) {
        j += 1;
      }
      out.push(
        <PpeChecklistGroup
          key={`ppe-${i}`}
          elements={els.slice(i, j)}
          values={values}
          header={ppeGroupHeader(stageId)}
          onChange={onChange}
        />,
      );
      i = j;
    } else {
      out.push(
        <ElementField
          key={els[i].id}
          element={els[i]}
          value={values[els[i].id]}
          error={undefined}
          onChange={(v) => onChange(els[i].id, v)}
          onUpload={onUpload}
        />,
      );
      i += 1;
    }
  }
  return out;
}

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
  const [submitting, setSubmitting] = useState(false);
  const [starting, setStarting] = useState(false);
  const [orderInfo, setOrderInfo] = useState<{ orderNumber: string; patientPib: string } | null>(null);
  const restoredKey = useRef<string | null>(null);
  const prevStepId = useRef<string | null>(null);
  // Retains the measurement-form values captured on «Зняття мірок» so they can
  // be shown read-only in a later step («Перевірка якості гіпсового позитива»).
  const measurementValuesRef = useRef<Record<string, unknown>>({});

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

  // Saved measurement-form values from the earlier «Зняття мірок» step, used to
  // render its read-only copy in later steps. Prefers the backend's persisted
  // prior-step values (survives reload/returning) and falls back to the
  // in-session capture while the user is still progressing through the process.
  const savedMeasurementValues = useMemo<Record<string, unknown>>(() => {
    const raw = instance?.priorStepValues?.['e0000002-0000-0000-0000-000000000002'];
    if (raw) {
      try {
        return JSON.parse(raw) as Record<string, unknown>;
      } catch {
        // ignore corrupted payload
      }
    }
    return measurementValuesRef.current;
  }, [instance?.priorStepValues]);

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

      <div className="grid gap-6">
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
            <div className="space-y-5">
              {step.id === 'e0000005-0000-0000-0000-000000000005' ? (
                <div className="space-y-3">
                  <p className="text-sm text-muted-foreground">
                    Заповнена форма з «ЕТАП 1: Зняття мірок та виготовлення гіпсового
                    негатива → КРОК 1: Зняття мірок (з пацієнтом)»:
                  </p>
                  <MeasurementForms
                    values={savedMeasurementValues}
                    onChange={() => {}}
                    disabled
                  />
                </div>
              ) : (
                renderElements(
                  step.elements,
                  values,
                  instance?.currentStageId ?? undefined,
                  step.id,
                  (id, v) => {
                    if (step.id === 'e0000002-0000-0000-0000-000000000002') {
                      measurementValuesRef.current = {
                        ...measurementValuesRef.current,
                        [id]: v,
                      };
                    }
                    setValues((s) => ({ ...s, [id]: v }));
                  },
                  (file) => void uploadEvidence(step.elements[0], file),
                )
              )}
            </div>
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
