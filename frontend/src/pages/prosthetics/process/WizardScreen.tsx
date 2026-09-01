import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Camera,
  Check,
  CircleAlert,
  ClipboardCheck,
  Home,
  PauseCircle,
  PenLine,
  Timer,
  Upload,
  X,
  XCircle,
} from 'lucide-react';
import { toast } from 'sonner';
import { Alert, AlertAction, AlertDescription, AlertTitle } from '@/components/ui/alert';
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
import { cn } from '@/lib/utils';
import { useAuth } from '@/services/AuthContext';
import { StatusBadge } from '@/components/prosthetics/StatusBadge';
import { QualityGatePanel } from '@/components/prosthetics/QualityGatePanel';
import { computeProgress, fmt, validateElementValues } from '@/prosthetics/validation';
import { MeasurementForms } from '@/pages/prosthetics/process/MeasurementForms';
import { FAILURE_CATEGORIES } from '@/prosthetics/failureCategories';
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

// The «Зняття мірок (з пацієнтом)» step renders its ЗІЗ confirmation as a
// hardcoded wizard field (not a DB element), so it is validated explicitly.
const MEASUREMENT_STEP_ID = 'e0000002-0000-0000-0000-000000000002';
const PPE_MEASUREMENT_GLOVES_KEY = 'ppe-measurement-non-sterile-gloves';
const PPE_MEASUREMENT_GLOVES_LABEL = 'Нестерильні оглядові нітрилові рукавички';

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

// A checkbox row whose ENTIRE surface toggles the checkbox: the row itself is
// a <label htmlFor>, so native label activation covers the padding/gap/text
// areas, and clicking the Base UI checkbox span stays a single toggle (label
// activation is suppressed for interactive content per the HTML spec).
function CheckboxRow({
  id,
  checked,
  onChange,
  variant = 'card',
  className,
  labelClassName,
  children,
}: {
  id: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  variant?: 'card' | 'muted' | 'plain';
  className?: string;
  labelClassName?: string;
  children: ReactNode;
}) {
  const variants: Record<typeof variant, { row: string; label: string }> = {
    card: {
      row: 'rounded-lg border bg-card p-4 transition-colors hover:bg-muted/40',
      label: 'text-sm font-medium',
    },
    muted: { row: 'rounded-md border bg-muted/40 p-3', label: 'text-sm' },
    plain: { row: 'rounded-md border p-3', label: 'text-sm' },
  };
  return (
    <label
      htmlFor={id}
      className={cn('flex cursor-pointer items-center gap-3', variants[variant].row, className)}
    >
      <Checkbox id={id} checked={checked} onCheckedChange={onChange} />
      <span data-slot="label" className={cn(variants[variant].label, labelClassName)}>
        {children}
      </span>
    </label>
  );
}

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
          <CheckboxRow
            key={el.id}
            id={el.id}
            checked={values[el.id] === true}
            onChange={(c) => onChange(el.id, c)}
            variant="muted"
            className="bg-card transition-colors hover:bg-muted/40"
          >
            {ppeDisplayLabel(el.label)}
            {el.required && <span className="text-accent">*</span>}
          </CheckboxRow>
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
  errors: Record<string, string>,
) {
  const out: React.ReactNode[] = [];
  // The «Зняття мірок (з пацієнтом)» step renders the pixel-perfect measurement
  // forms (two anatomical diagrams in a single row). They are data-entry fields
  // whose values are stored under the element keys, gated by the backend
  // «min 3 filled measurements» rule.
  if (stepId === 'e0000002-0000-0000-0000-000000000002') {
    const stepMessage = els.find((el) => el.elementType === 'STEP_MESSAGE');
    if (stepMessage) {
      out.push(
        <p
          key={stepMessage.id}
          className="rounded-md border-l-4 border-accent bg-muted p-3 text-sm font-medium"
        >
          {stepMessage.label}
        </p>,
      );
    }
    out.push(
      <div key="ppe-measurement" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <div className="flex flex-wrap items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">1І1</Badge>
        </div>
        <p className="text-sm text-muted-foreground">
          Перед вимірюванням підтвердіть засоби індивідуального захисту.
        </p>
        <Separator />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-[1fr_auto_auto] md:items-start">
          <div className="flex flex-col gap-1.5">
            <CheckboxRow
              id={PPE_MEASUREMENT_GLOVES_KEY}
              checked={values[PPE_MEASUREMENT_GLOVES_KEY] === true}
              onChange={(c) => onChange(PPE_MEASUREMENT_GLOVES_KEY, c)}
              className={
                errors[PPE_MEASUREMENT_GLOVES_KEY] ? 'border-destructive ring-1 ring-destructive' : ''
              }
            >
              {PPE_MEASUREMENT_GLOVES_LABEL}
            </CheckboxRow>
            {errors[PPE_MEASUREMENT_GLOVES_KEY] && (
              <p className="px-1 text-xs text-destructive">{errors[PPE_MEASUREMENT_GLOVES_KEY]}</p>
            )}
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
  if (stepId === 'e0000003-0000-0000-0000-000000000003') {
    const stepMessage = els.find((el) => el.elementType === 'STEP_MESSAGE');
    if (stepMessage) {
      out.push(
        <p
          key={stepMessage.id}
          className="rounded-md border-l-4 border-accent bg-muted p-3 text-sm font-medium"
        >
          {stepMessage.label}
        </p>,
      );
    }
    out.push(
      <div key="plaster-confirmation" className="space-y-3 rounded-xl border bg-muted/40 p-5">
        <p className="text-sm font-semibold uppercase tracking-wide">ПІДТВЕРДЖЕННЯ ВИРОБНИЦТВА</p>
        <CheckboxRow
          id="plaster-negative-confirmed"
          checked={values['f0000004-0000-0000-0000-000000000001'] === true}
          onChange={(c) => onChange('f0000004-0000-0000-0000-000000000001', c)}
        >
          Гіпсовий негатив виготовлено
        </CheckboxRow>
        <p className="text-xs text-muted-foreground">
          Після відмітки переходьте до наступного кроку.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000004-0000-0000-0000-000000000004') {
      const stepMessage = els.find((el) => el.elementType === 'STEP_MESSAGE');
      if (stepMessage) {
        out.push(
          <p
            key={stepMessage.id}
            className="rounded-md border-l-4 border-accent bg-muted p-3 text-sm font-medium"
          >
            {stepMessage.label}
          </p>,
        );
      }
      out.push(
        <div key="plaster-quality-check" className="space-y-3 rounded-xl border bg-muted/40 p-5">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРКА ЯКОСТІ</p>
          <CheckboxRow
            id="plaster-quality-checked"
            checked={values['f0000005-0000-0000-0000-000000000001'] === true}
            onChange={(c) => onChange('f0000005-0000-0000-0000-000000000001', c)}
          >
            Гіпсовий негатив перевірено на відповідність антропометричним даним
          </CheckboxRow>
          <p className="text-xs text-muted-foreground">
            Після відмітки переходьте до наступного кроку.
          </p>
        </div>,
      );
      return out;
    }
  if (stepId === 'e0000011-0000-0000-0000-000000000001') {
    out.push(
      <div key="positive-production" className="space-y-3 rounded-xl border bg-muted/40 p-5">
        <p className="text-sm font-semibold uppercase tracking-wide">ПІДТВЕРДЖЕННЯ ВИРОБНИЦТВА</p>
        <CheckboxRow
          id="plaster-positive-confirmed"
          checked={values['f0000013-0000-0000-0000-000000000001'] === true}
          onChange={(c) => onChange('f0000013-0000-0000-0000-000000000001', c)}
        >
          Гіпсовий позитив виготовлено
        </CheckboxRow>
        <p className="text-xs text-muted-foreground">
          Після відмітки переходьте до наступного кроку.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000060-0000-0000-0000-000000000001') {
    out.push(
      <div
        key="kit-form-new"
        className="space-y-5 rounded-xl border bg-muted/40 p-5"
        style={{ display: 'contents' }}
      >
        <p className="text-sm font-semibold uppercase tracking-wide" style={{ display: 'none' }}>КОМПЛЕКТАЦІЯ</p>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2" style={{ display: 'none' }}>
          <div className="space-y-2">
            <Label htmlFor="kit-hand" className="text-sm font-medium">Кисть</Label>
            <Input
              id="kit-hand"
              value={(values['f0000061-0000-0000-0000-000000000001'] as string) ?? ''}
              onChange={(e) => onChange('f0000061-0000-0000-0000-000000000001', e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="kit-hook" className="text-sm font-medium">Гак</Label>
            <Input
              id="kit-hook"
              value={(values['f0000062-0000-0000-0000-000000000002'] as string) ?? ''}
              onChange={(e) => onChange('f0000062-0000-0000-0000-000000000002', e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="kit-wrist-unit" className="text-sm font-medium">Блок зап'ястья</Label>
            <Input
              id="kit-wrist-unit"
              value={(values['f0000063-0000-0000-0000-000000000003'] as string) ?? ''}
              onChange={(e) => onChange('f0000063-0000-0000-0000-000000000003', e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="kit-bandage" className="text-sm font-medium">Бандаж</Label>
            <Input
              id="kit-bandage"
              value={(values['f0000064-0000-0000-0000-000000000004'] as string) ?? ''}
              onChange={(e) => onChange('f0000064-0000-0000-0000-000000000004', e.target.value)}
            />
          </div>
          <div className="md:col-span-2 space-y-2">
            <Label htmlFor="kit-other" className="text-sm font-medium">Інші компоненти</Label>
            <Textarea
              id="kit-other"
              rows={3}
              value={(values['f0000065-0000-0000-0000-000000000005'] as string) ?? ''}
              onChange={(e) => onChange('f0000065-0000-0000-0000-000000000005', e.target.value)}
            />
          </div>
        </div>
        <CheckboxRow
          id="kit-formed"
          checked={values['f0000066-0000-0000-0000-000000000006'] === true}
          onChange={(c) => onChange('f0000066-0000-0000-0000-000000000006', c)}
        >
          Комплектацію сформовано (лист для збірки комплектації на склад)
        </CheckboxRow>
        <p className="text-xs text-muted-foreground" style={{ display: 'none' }}>
          Після відмітки переходьте до наступного кроку.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000041-0000-0000-0000-000000000001') {
    out.push(
      <div key="thermoforming-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <div className="flex items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">5І5</Badge>
        </div>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <div className="space-y-3 rounded-lg border bg-card p-4">
            <p className="text-sm font-medium">Обробка гільзи</p>
            <p className="text-xs text-muted-foreground">Засоби індивідуального захисту:</p>
            <div className="space-y-2">
              <CheckboxRow
                id="thermoforming-goggles"
                checked={values['f0000044-0000-0000-0000-000000000001'] === true}
                onChange={(c) => onChange('f0000044-0000-0000-0000-000000000001', c)}
                variant="muted"
              >
                Захисні окуляри
              </CheckboxRow>
              <CheckboxRow
                id="thermoforming-respirator"
                checked={values['f0000045-0000-0000-0000-000000000001'] === true}
                onChange={(c) => onChange('f0000045-0000-0000-0000-000000000001', c)}
                variant="muted"
              >
                Респіратор
              </CheckboxRow>
              <CheckboxRow
                id="thermoforming-earmuffs"
                checked={values['f0000046-0000-0000-0000-000000000001'] === true}
                onChange={(c) => onChange('f0000046-0000-0000-0000-000000000001', c)}
                variant="muted"
              >
                Захисні навушники
              </CheckboxRow>
            </div>
            <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
              <img
                src="/ppe/goggles_resp_ears.png"
                alt="Засоби індивідуального захисту: захисні окуляри, респіратор, захисні навушники"
                className="h-56 w-auto rounded-lg object-contain md:h-64"
              />
            </div>
          </div>

          <div className="space-y-3 rounded-lg border bg-card p-4">
            <p className="text-sm font-medium">Термоформування</p>
            <p className="text-xs text-muted-foreground">Засоби індивідуального захисту:</p>
            <div className="space-y-2">
              <CheckboxRow
                id="thermoforming-latex-gloves"
                checked={values['f0000047-0000-0000-0000-000000000001'] === true}
                onChange={(c) => onChange('f0000047-0000-0000-0000-000000000001', c)}
                variant="muted"
              >
                Латексні рукавички підвищеної міцності
              </CheckboxRow>
              <CheckboxRow
                id="thermoforming-thermal-gloves"
                checked={values['f0000048-0000-0000-0000-000000000001'] === true}
                onChange={(c) => onChange('f0000048-0000-0000-0000-000000000001', c)}
                variant="muted"
              >
                М’які тканинні терморукавиці
              </CheckboxRow>
            </div>
            <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
              <img
                src="/ppe/latex_thermal_gloves.png"
                alt="Засоби індивідуального захисту: латексні рукавички підвищеної міцності, м'які тканинні терморукавиці"
                className="h-56 w-auto rounded-lg object-contain md:h-64"
              />
            </div>
          </div>
        </div>

        <Separator />

        <div className="space-y-3">
          <p className="text-sm font-medium">Перехід до наступного кроку після відмітки:</p>
          <CheckboxRow
            id="thermoforming-sleeve-formed"
            checked={values['f0000042-0000-0000-0000-000000000001'] === true}
            onChange={(c) => onChange('f0000042-0000-0000-0000-000000000001', c)}
          >
            Гільза сформована
          </CheckboxRow>
          <CheckboxRow
            id="thermoforming-edges-polished"
            checked={values['f0000049-0000-0000-0000-000000000001'] === true}
            onChange={(c) => onChange('f0000049-0000-0000-0000-000000000001', c)}
          >
            Краї заокруглені та відполіровані, зроблено отвір для примірки
          </CheckboxRow>
        </div>

        <p className="text-xs text-muted-foreground">
          Перехід до наступного кроку можливий після відмітки усіх чекбоксів вище.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000069-0000-0000-0000-000000000009') {
    out.push(
      <div key="inner-sleeve-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <div className="flex items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">5І5</Badge>
        </div>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <div className="space-y-3 rounded-lg border bg-card p-4">
            <p className="text-sm font-medium">Обробка гільзи</p>
            <p className="text-xs text-muted-foreground">Засоби індивідуального захисту:</p>
            <div className="space-y-2">
              <CheckboxRow
                id="inner-sleeve-goggles"
                checked={values['f0000071-0000-0000-0000-000000000011'] === true}
                onChange={(c) => onChange('f0000071-0000-0000-0000-000000000011', c)}
                variant="muted"
              >
                Захисні окуляри
              </CheckboxRow>
              <CheckboxRow
                id="inner-sleeve-respirator"
                checked={values['f0000072-0000-0000-0000-000000000012'] === true}
                onChange={(c) => onChange('f0000072-0000-0000-0000-000000000012', c)}
                variant="muted"
              >
                Респіратор
              </CheckboxRow>
              <CheckboxRow
                id="inner-sleeve-earmuffs"
                checked={values['f0000073-0000-0000-0000-000000000013'] === true}
                onChange={(c) => onChange('f0000073-0000-0000-0000-000000000013', c)}
                variant="muted"
              >
                Захисні навушники
              </CheckboxRow>
            </div>
            <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
              <img
                src="/ppe/goggles_resp_ears.png"
                alt="Засоби індивідуального захисту: захисні окуляри, респіратор, захисні навушники"
                className="h-56 w-auto rounded-lg object-contain md:h-64"
              />
            </div>
          </div>

          <div className="space-y-3 rounded-lg border bg-card p-4">
            <p className="text-sm font-medium">Термоформування</p>
            <p className="text-xs text-muted-foreground">Засоби індивідуального захисту:</p>
            <div className="space-y-2">
              <CheckboxRow
                id="inner-sleeve-latex-gloves"
                checked={values['f0000074-0000-0000-0000-000000000014'] === true}
                onChange={(c) => onChange('f0000074-0000-0000-0000-000000000014', c)}
                variant="muted"
              >
                Латексні рукавички підвищеної міцності
              </CheckboxRow>
              <CheckboxRow
                id="inner-sleeve-thermal-gloves"
                checked={values['f0000075-0000-0000-0000-000000000015'] === true}
                onChange={(c) => onChange('f0000075-0000-0000-0000-000000000015', c)}
                variant="muted"
              >
                М’які тканинні терморукавиці
              </CheckboxRow>
            </div>
            <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
              <img
                src="/ppe/latex_thermal_gloves.png"
                alt="Засоби індивідуального захисту: латексні рукавички підвищеної міцності, м'які тканинні терморукавиці"
                className="h-56 w-auto rounded-lg object-contain md:h-64"
              />
            </div>
          </div>
        </div>

        <Separator />

        <div className="space-y-3">
          <p className="text-sm font-medium">Перехід до наступного кроку після відмітки:</p>
          <CheckboxRow
            id="inner-sleeve-formed"
            checked={values['f0000076-0000-0000-0000-000000000016'] === true}
            onChange={(c) => onChange('f0000076-0000-0000-0000-000000000016', c)}
          >
            Гільза сформована
          </CheckboxRow>
          <CheckboxRow
            id="inner-sleeve-edges-polished"
            checked={values['f0000077-0000-0000-0000-000000000017'] === true}
            onChange={(c) => onChange('f0000077-0000-0000-0000-000000000017', c)}
          >
            Краї заокруглені та відполіровані
          </CheckboxRow>
        </div>

        <p className="text-xs text-muted-foreground">
          Перехід до наступного кроку можливий після відмітки усіх чекбоксів вище.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000070-0000-0000-0000-000000000010') {
    const stepMessage = els.find((el) => el.elementType === 'STEP_MESSAGE');
    out.push(
      <div key="inner-fitting-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        {stepMessage && (
          <p className="rounded-md border-l-4 border-accent bg-muted p-3 text-sm font-medium">
            {stepMessage.label}
          </p>
        )}
        <div className="flex items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">1І1</Badge>
        </div>

        <p className="text-sm font-medium">Засоби індивідуального захисту:</p>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-[1fr_auto_auto] md:items-start">
          <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
            <img
              src="/ppe/non-sterile_gloves.png"
              alt="Засоби індивідуального захисту: нестерильні оглядові нітрилові рукавички"
              className="h-56 w-auto rounded-lg object-contain md:h-64"
            />
          </div>
          <div className="flex items-center justify-center">
            <ArrowLeft className="size-8 text-primary" aria-hidden="true" />
          </div>
          <CheckboxRow
            id="inner-fitting-nitrile-gloves"
            checked={values['f0000079-0000-0000-0000-000000000019'] === true}
            onChange={(c) => onChange('f0000079-0000-0000-0000-000000000019', c)}
          >
            Нестерильні оглядові нітрилові рукавички
          </CheckboxRow>
        </div>

        <Separator />

        <div className="space-y-3">
          <p className="text-sm font-medium">Перехід до наступного кроку після відмітки:</p>
          <CheckboxRow
              id="inner-fitting-sleeve-checked"
              checked={values['f0000078-0000-0000-0000-000000000018'] === true}
              onChange={(c) => onChange('f0000078-0000-0000-0000-000000000018', c)}
            >
              Постійну внутрішню гільзу перевірено на відповідність фактичним антропометричним даним пацієнта
            </CheckboxRow>
          <CheckboxRow
              id="inner-fitting-axes-set"
              checked={values['f0000080-0000-0000-0000-000000000020'] === true}
              onChange={(c) => onChange('f0000080-0000-0000-0000-000000000020', c)}
            >
              Задано необхідні вісі для виготовлення формоутворюючої моделі
            </CheckboxRow>
        </div>

        <p className="text-xs text-muted-foreground">
          Перехід до наступного кроку можливий після відмітки усіх чекбоксів вище.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000071-0000-0000-0000-000000000011') {
    out.push(
      <div key="outer-sleeve-model-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <CheckboxRow
          id="outer-sleeve-model-ready"
          checked={values['f0000082-0000-0000-0000-000000000022'] === true}
          onChange={(c) => onChange('f0000082-0000-0000-0000-000000000022', c)}
        >
          Формоутворююча модель готова
        </CheckboxRow>

        <p className="text-xs text-muted-foreground">
          Перехід до наступного кроку можливий після відмітки усіх чекбоксів вище.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000072-0000-0000-0000-000000000012') {
    out.push(
      <div key="lamination-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <div className="flex flex-wrap items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">2І2</Badge>
        </div>
        <p className="text-sm text-muted-foreground">
          Перед ламінацією підтвердіть засоби індивідуального захисту.
        </p>
        <Separator />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-[1fr_auto_auto] md:items-start">
          <div className="space-y-2">
            <CheckboxRow
              id="lamination-nitrile-gloves"
              checked={values['f0000083-0000-0000-0000-000000000023'] === true}
              onChange={(c) => onChange('f0000083-0000-0000-0000-000000000023', c)}
            >
              Нестерильні оглядові нітрилові рукавички
            </CheckboxRow>
            <CheckboxRow
              id="lamination-respirator"
              checked={values['f0000084-0000-0000-0000-000000000024'] === true}
              onChange={(c) => onChange('f0000084-0000-0000-0000-000000000024', c)}
            >
              Респіратор
            </CheckboxRow>
          </div>
          <div className="flex items-center justify-center">
            <ArrowRight className="size-8 text-primary" aria-hidden="true" />
          </div>
          <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
            <img
              src="/ppe/non-sterile_gloves_resp.png"
              alt="Засоби індивідуального захисту: нестерильні оглядові нітрилові рукавички, респіратор"
              className="h-56 w-auto rounded-lg object-contain md:h-64"
            />
          </div>
        </div>
        <div className="space-y-3">
          <p className="text-sm font-semibold uppercase tracking-wide">ПІДТВЕРДЖЕННЯ ВИРОБНИЦТВА</p>
          <CheckboxRow
            id="lamination-confirmed"
            checked={values['f0000085-0000-0000-0000-000000000025'] === true}
            onChange={(c) => onChange('f0000085-0000-0000-0000-000000000025', c)}
          >
            Гільза заламінована
          </CheckboxRow>
          <p className="text-xs text-muted-foreground">
            Після відмітки процес можна завершити.
          </p>
        </div>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000073-0000-0000-0000-000000000013') {
    out.push(
      <div key="processing-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <div className="flex flex-wrap items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">ЗІЗ</Badge>
        </div>
        <p className="text-sm text-muted-foreground">
          Перед обробкою підтвердіть засоби індивідуального захисту.
        </p>
        <Separator />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-[1fr_auto_auto] md:items-start">
          <div className="space-y-2">
            <CheckboxRow
              id="processing-safety-glasses"
              checked={values['f0000086-0000-0000-0000-000000000026'] === true}
              onChange={(c) => onChange('f0000086-0000-0000-0000-000000000026', c)}
            >
              Захисні окуляри
            </CheckboxRow>
            <CheckboxRow
              id="processing-respirator"
              checked={values['f0000087-0000-0000-0000-000000000027'] === true}
              onChange={(c) => onChange('f0000087-0000-0000-0000-000000000027', c)}
            >
              Респіратор
            </CheckboxRow>
            <CheckboxRow
              id="processing-headphones"
              checked={values['f0000088-0000-0000-0000-000000000028'] === true}
              onChange={(c) => onChange('f0000088-0000-0000-0000-000000000028', c)}
            >
              Захисні навушники
            </CheckboxRow>
          </div>
          <div className="flex items-center justify-center">
            <ArrowRight className="size-8 text-primary" aria-hidden="true" />
          </div>
          <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
            <img
              src="/ppe/goggles_resp_ears.png"
              alt="Засоби індивідуального захисту: захисні окуляри, респіратор, захисні навушники"
              className="h-56 w-auto rounded-lg object-contain md:h-64"
            />
          </div>
        </div>
        <div className="space-y-3">
          <p className="text-sm font-semibold uppercase tracking-wide">ПІДТВЕРДЖЕННЯ ВИРОБНИЦТВА</p>
          <CheckboxRow
            id="processing-confirmed"
            checked={values['f0000089-0000-0000-0000-000000000029'] === true}
            onChange={(c) => onChange('f0000089-0000-0000-0000-000000000029', c)}
          >
            Краї заокруглені та відполіровані
          </CheckboxRow>
          <p className="text-xs text-muted-foreground">
            Після відмітки процес можна завершити.
          </p>
        </div>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000074-0000-0000-0000-000000000014') {
    out.push(
      <div key="assembly-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <div className="space-y-3">
          <p className="text-sm font-semibold uppercase tracking-wide">ПІДТВЕРДЖЕННЯ ВИРОБНИЦТВА</p>
          <CheckboxRow
              id="assembly-confirmed"
              checked={values['f0000090-0000-0000-0000-000000000030'] === true}
              onChange={(c) => onChange('f0000090-0000-0000-0000-000000000030', c)}
            >
              Протез складено відповідно до ТТП
            </CheckboxRow>
          <p className="text-xs text-muted-foreground">
            Після відмітки переходьте до наступного кроку.
          </p>
        </div>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000075-0000-0000-0000-000000000015') {
    out.push(
      <div key="fastening-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <div className="space-y-3">
          <p className="text-sm font-semibold uppercase tracking-wide">ПІДТВЕРДЖЕННЯ ВИРОБНИЦТВА</p>
          <CheckboxRow
              id="fastening-confirmed"
              checked={values['f0000091-0000-0000-0000-000000000031'] === true}
              onChange={(c) => onChange('f0000091-0000-0000-0000-000000000031', c)}
            >
              Система кріплення протеза виготовлена та зафіксована на протезі
            </CheckboxRow>
          <p className="text-xs text-muted-foreground">
            Після відмітки процес можна завершити.
          </p>
        </div>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000076-0000-0000-0000-000000000016') {
    out.push(
      <div key="final-assembly-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <div className="space-y-3">
          <p className="text-sm font-semibold uppercase tracking-wide">ПІДТВЕРДЖЕННЯ ВИРОБНИЦТВА</p>
          <div className="space-y-2">
            <CheckboxRow
              id="final-assembly-rivets"
              checked={values['f0000092-0000-0000-0000-000000000032'] === true}
              onChange={(c) => onChange('f0000092-0000-0000-0000-000000000032', c)}
            >
              Заклепки на протезі щільно підтягнуті, обтиснуті до повного профілю, не мають гострих країв та задирок
            </CheckboxRow>
            <CheckboxRow
              id="final-assembly-hinges"
              checked={values['f0000093-0000-0000-0000-000000000033'] === true}
              onChange={(c) => onChange('f0000093-0000-0000-0000-000000000033', c)}
            >
              Шарнірні з&apos;єднання забезпечують безшумне, легке, плавне переміщення складових частин, що з&apos;єднуються
            </CheckboxRow>
            <CheckboxRow
              id="final-assembly-components"
              checked={values['f0000094-0000-0000-0000-000000000034'] === true}
              onChange={(c) => onChange('f0000094-0000-0000-0000-000000000034', c)}
            >
              Всі компоненти надійно з&apos;єднані між собою, протез готовий до фінального налаштування кріплення на пацієнті
            </CheckboxRow>
          </div>
          <p className="text-xs text-muted-foreground">
            Після відмітки переходьте до наступного кроку.
          </p>
        </div>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000077-0000-0000-0000-000000000017') {
    const stepMessage = els.find((el) => el.elementType === 'STEP_MESSAGE');
    out.push(
<div key="final-fitting-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        {stepMessage && (
          <p className="rounded-md border-l-4 border-accent bg-muted p-3 text-sm font-medium">
            {stepMessage.label}
          </p>
)}
        <div className="flex flex-wrap items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">1І1</Badge>
        </div>
        <p className="text-sm text-muted-foreground">
          Перед приміркою підтвердіть засоби індивідуального захисту.
        </p>
        <Separator />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-[1fr_auto_auto] md:items-start">
          <CheckboxRow
            id="final-fitting-nitrile-gloves"
            checked={values['f0000095-0000-0000-0000-000000000035'] === true}
            onChange={(c) => onChange('f0000095-0000-0000-0000-000000000035', c)}
          >
            Нестерильні оглядові нітрилові рукавички
          </CheckboxRow>
          <div className="flex items-center justify-center">
            <ArrowRight className="size-8 text-primary" aria-hidden="true" />
          </div>
          <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
            <img
              src="/ppe/non-sterile_gloves.png"
              alt="Засоби індивідуального захисту: нестерильні оглядові нітрилові рукавички"
              className="h-56 w-auto rounded-lg object-contain md:h-64"
            />
          </div>
        </div>
        <Separator />
        <div className="space-y-3">
          <p className="text-sm font-medium">Перехід до наступного кроку після відмітки:</p>
          <CheckboxRow
              id="final-fitting-fastening"
              checked={values['f0000096-0000-0000-0000-000000000036'] === true}
              onChange={(c) => onChange('f0000096-0000-0000-0000-000000000036', c)}
            >
              Кріплення надійно зафіксовано на протезі
            </CheckboxRow>
          <CheckboxRow
              id="final-fitting-cables"
              checked={values['f0000097-0000-0000-0000-000000000037'] === true}
              onChange={(c) => onChange('f0000097-0000-0000-0000-000000000037', c)}
            >
              Перевірено екскурсію тяг та спрацьовування термінальних пристроїв протеза
            </CheckboxRow>
        </div>
        <p className="text-xs text-muted-foreground">
          Перехід до наступного кроку можливий після відмітки усіх чекбоксів вище.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000078-0000-0000-0000-000000000018') {
    const stepMessages = els.filter((el) => el.elementType === 'STEP_MESSAGE');
    out.push(
      <div key="prosthesis-handover-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        {stepMessages.map((el) => (
          <p
            key={el.id}
            className="rounded-md border-l-4 border-accent bg-muted p-3 text-sm font-medium"
          >
            {el.label}
          </p>
        ))}
        <div className="flex flex-wrap items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">1І1</Badge>
        </div>
        <p className="text-sm text-muted-foreground">
          Перед видачею підтвердіть засоби індивідуального захисту.
        </p>
        <Separator />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-[1fr_auto_auto] md:items-start">
          <CheckboxRow
              id="handover-nitrile-gloves"
              checked={values['f0000101-0000-0000-0000-000000000041'] === true}
              onChange={(c) => onChange('f0000101-0000-0000-0000-000000000041', c)}
            >
              Нестерильні оглядові нітрилові рукавички
            </CheckboxRow>
          <div className="flex items-center justify-center">
            <ArrowRight className="size-8 text-primary" aria-hidden="true" />
          </div>
          <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
            <img
              src="/ppe/non-sterile_gloves.png"
              alt="Засоби індивідуального захисту: нестерильні оглядові нітрилові рукавички"
              className="h-56 w-auto rounded-lg object-contain md:h-64"
            />
          </div>
        </div>
        <Separator />
        <div className="space-y-3">
          <p className="text-sm font-medium">Перехід до наступного кроку після відмітки:</p>
          <CheckboxRow
              id="handover-passive-rotation"
              checked={values['f0000102-0000-0000-0000-000000000042'] === true}
              onChange={(c) => onChange('f0000102-0000-0000-0000-000000000042', c)}
            >
              У протезі забезпечується пасивна ротація кисті відносно гільзи передпліччя
            </CheckboxRow>
          <CheckboxRow
              id="handover-soft-tissues"
              checked={values['f0000103-0000-0000-0000-000000000043'] === true}
              onChange={(c) => onChange('f0000103-0000-0000-0000-000000000043', c)}
            >
              При згинанні в ліктьовому суглобі м&apos;які тканини кукси утримуються в гільзі, не нависають над її верхнім краєм і не затискаються нею
            </CheckboxRow>
        </div>
        <p className="text-xs text-muted-foreground">
          Перехід до наступного кроку можливий після відмітки усіх чекбоксів вище.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000079-0000-0000-0000-000000000019') {
    const stepMessages = els.filter((el) => el.elementType === 'STEP_MESSAGE');
    out.push(
      <div key="prosthesis-marking-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        {stepMessages.map((el) => (
          <p
            key={el.id}
            className="rounded-md border-l-4 border-accent bg-muted p-3 text-sm font-medium"
          >
            {el.label}
          </p>
        ))}
        <div className="flex flex-wrap items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">1І1</Badge>
        </div>
        <p className="text-sm text-muted-foreground">
          Перед маркуванням підтвердіть засоби індивідуального захисту.
        </p>
        <Separator />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-[1fr_auto_auto] md:items-start">
          <CheckboxRow
              id="marking-nitrile-gloves"
              checked={values['f0000105-0000-0000-0000-000000000045'] === true}
              onChange={(c) => onChange('f0000105-0000-0000-0000-000000000045', c)}
            >
              Нестерильні оглядові нітрилові рукавички
            </CheckboxRow>
          <div className="flex items-center justify-center">
            <ArrowRight className="size-8 text-primary" aria-hidden="true" />
          </div>
          <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
            <img
              src="/ppe/non-sterile_gloves.png"
              alt="Засоби індивідуального захисту: нестерильні оглядові нітрилові рукавички"
              className="h-56 w-auto rounded-lg object-contain md:h-64"
            />
          </div>
        </div>
        <Separator />
        <div className="space-y-3">
          <p className="text-sm font-medium">Процес завершується після відмітки:</p>
          <CheckboxRow
              id="marking-final-inspection"
              checked={values['f0000106-0000-0000-0000-000000000046'] === true}
              onChange={(c) => onChange('f0000106-0000-0000-0000-000000000046', c)}
            >
              Протез фінально оглянутий ззовні на предмет пошкоджень чи несправностей
            </CheckboxRow>
          <CheckboxRow
              id="marking-applied"
              checked={values['f0000107-0000-0000-0000-000000000047'] === true}
              onChange={(c) => onChange('f0000107-0000-0000-0000-000000000047', c)}
            >
              На протез нанесено маркування
            </CheckboxRow>
        </div>
        <p className="text-xs text-muted-foreground">
          Після відмітки процес можна завершити.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000042-0000-0000-0000-000000000002') {
    const stepMessage = els.find((el) => el.elementType === 'STEP_MESSAGE');
    if (stepMessage) {
      out.push(
        <p
          key={stepMessage.id}
          className="rounded-md border-l-4 border-accent bg-muted p-3 text-sm font-medium"
        >
          {stepMessage.label}
        </p>,
      );
    }
    out.push(
      <div key="fitting-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        <div className="flex items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">1І1</Badge>
        </div>

        <p className="text-sm font-medium">Засоби індивідуального захисту:</p>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-[1fr_auto_auto] md:items-start">
          <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
            <img
              src="/ppe/non-sterile_gloves.png"
              alt="Засоби індивідуального захисту: нестерильні оглядові нітрилові рукавички"
              className="h-56 w-auto rounded-lg object-contain md:h-64"
            />
          </div>
          <div className="flex items-center justify-center">
            <ArrowLeft className="size-8 text-primary" aria-hidden="true" />
          </div>
          <CheckboxRow
              id="fitting-nitrile-gloves"
              checked={values['f0000050-0000-0000-0000-000000000001'] === true}
              onChange={(c) => onChange('f0000050-0000-0000-0000-000000000001', c)}
            >
              Нестерильні оглядові нітрилові рукавички
            </CheckboxRow>
        </div>

        <Separator />

        <div className="space-y-3">
          <p className="text-sm font-medium">Перехід до наступного кроку після відмітки:</p>
          <CheckboxRow
              id="fitting-sleeve-checked"
              checked={values['f0000043-0000-0000-0000-000000000001'] === true}
              onChange={(c) => onChange('f0000043-0000-0000-0000-000000000001', c)}
            >
              Тестову гільзу перевірено на відповідність фактичним антропометричним даним пацієнта
            </CheckboxRow>
        </div>

        <p className="text-xs text-muted-foreground">
          Перехід до наступного кроку можливий після відмітки усіх чекбоксів вище.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000068-0000-0000-0000-000000000008') {
    const stepMessage = els.find((el) => el.elementType === 'STEP_MESSAGE');
    out.push(
      <div key="prototype-fitting-step" className="space-y-5 rounded-xl border bg-muted/40 p-5">
        {stepMessage && (
          <p className="rounded-md border-l-4 border-accent bg-muted p-3 text-sm font-medium">
            {stepMessage.label}
          </p>
        )}
        <div className="flex items-center gap-2">
          <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРТЕ ВСЕ НЕОБХІДНЕ</p>
          <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">1І1</Badge>
        </div>

        <p className="text-sm font-medium">Засоби індивідуального захисту:</p>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-[1fr_auto_auto] md:items-start">
          <div className="flex items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-4">
            <img
              src="/ppe/non-sterile_gloves.png"
              alt="Засоби індивідуального захисту: нестерильні оглядові нітрилові рукавички"
              className="h-56 w-auto rounded-lg object-contain md:h-64"
            />
          </div>
          <div className="flex items-center justify-center">
            <ArrowLeft className="size-8 text-primary" aria-hidden="true" />
          </div>
          <CheckboxRow
              id="prototype-fitting-nitrile-gloves"
              checked={values['f0000069-0000-0000-0000-000000000009'] === true}
              onChange={(c) => onChange('f0000069-0000-0000-0000-000000000009', c)}
            >
              Нестерильні оглядові нітрилові рукавички
            </CheckboxRow>
        </div>

        <Separator />

        <div className="space-y-3">
          <p className="text-sm font-medium">Перехід до наступного етапу після відмітки:</p>
          <CheckboxRow
              id="prototype-fitting-tested"
              checked={values['f0000070-0000-0000-0000-000000000010'] === true}
              onChange={(c) => onChange('f0000070-0000-0000-0000-000000000010', c)}
            >
              Проведено тестування протеза на пацієнті: перевірено посадку,
              функціональність та правильність взаємного розташування всіх елементів.
            </CheckboxRow>
        </div>

        <p className="text-xs text-muted-foreground">
          Перехід до наступного етапу можливий після відмітки усіх чекбоксів вище.
        </p>
      </div>,
    );
    return out;
  }
  if (stepId === 'e0000005-0000-0000-0000-000000000005') {
    out.push(
      <div key="positive-quality-check" className="space-y-3 rounded-xl border bg-muted/40 p-5">
        <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРКА ЯКОСТІ</p>
        <CheckboxRow
          id="plaster-positive-quality-checked"
          checked={values['f0000006-0000-0000-0000-000000000001'] === true}
          onChange={(c) => onChange('f0000006-0000-0000-0000-000000000001', c)}
        >
          Гіпсовий позитив перевірено на відповідність бланку замірів
        </CheckboxRow>
        <p className="text-xs text-muted-foreground">
          Після відмітки переходьте до наступного етапу.
        </p>
      </div>,
    );
    return out;
  }
  const positiveQualityStepTitle = 'Перевірка якості гіпсового позитива на відповідність бланку замірів';
  const hasPositiveQualityTitle = els.some(
    (el) => (el.label ?? '').includes(positiveQualityStepTitle),
  );
  if (!out.length && hasPositiveQualityTitle) {
    out.push(
      <div key="positive-quality-check-fallback" className="space-y-3 rounded-xl border bg-muted/40 p-5">
        <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРКА ЯКОСТІ</p>
        <CheckboxRow
            id="plaster-positive-quality-checked-fallback"
            checked={values['f0000006-0000-0000-0000-000000000001'] === true}
            onChange={(c) => onChange('f0000006-0000-0000-0000-000000000001', c)}
          >
            Гіпсовий позитив перевірено на відповідність бланку замірів
          </CheckboxRow>
        <p className="text-xs text-muted-foreground">
          Після відмітки переходьте до наступного етапу.
        </p>
      </div>,
    );
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
      const el = els[i];
      out.push(
        <ElementField
          key={el.id}
          element={el}
          value={values[el.id]}
          error={errors[el.id]}
          onChange={(v) => onChange(el.id, v)}
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
  const [failOpen, setFailOpen] = useState(false);
  const [validationAlertDismissed, setValidationAlertDismissed] = useState(false);
  const [failCategory, setFailCategory] = useState('');
  const [failDescription, setFailDescription] = useState('');
  const [failFiles, setFailFiles] = useState<string[]>([]);
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
      } else if (next.status === 'FAILED' || next.status === 'FAILED_QC' || next.status === 'BRANCHED') {
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
    const baseline = instance.totalActiveSeconds ?? 0;
    // Seed with the process-accumulated active time (all completed steps) so the
    // timer reflects the whole process and never flashes to zero on navigation.
    setSeconds(baseline);
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
          setSeconds(baseline + elapsed);
        }
      })
      .catch(() => {
        // draft restore is best-effort; the timer keeps the process baseline
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
  const prevStep = useMemo(() => {
    if (stepIndexInStage > 0) {
      return stage?.steps[stepIndexInStage - 1];
    }
    if (stageIndex > 0) {
      const prevStage = snapshot?.stages[stageIndex - 1];
      const prevSteps = prevStage?.steps;
      return prevSteps ? prevSteps[prevSteps.length - 1] : undefined;
    }
    return undefined;
  }, [stepIndexInStage, stage, stageIndex, snapshot]);
  const canGoBack = !!prevStep?.allowBackward;
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
      // The timer keeps running across step navigation — it measures the whole
      // process (start → end), so it is NOT reset here. It stops only on Пауза,
      // «Позначити процес як провалений» or full completion.
      setValidationAlertDismissed(false);
    }
    prevStepId.current = current;
  }, [step?.id]);

  const timerRunning = instance?.status === 'IN_PROGRESS' && !!step;

  useEffect(() => {
    if (!timerRunning) return;
    const t = setInterval(() => setSeconds((s) => s + 1), 1000);
    return () => clearInterval(t);
  }, [timerRunning]);

  const invalid = useMemo(() => {
    const base = validateElementValues(step?.elements ?? [], values);
    if (step?.id === MEASUREMENT_STEP_ID && values[PPE_MEASUREMENT_GLOVES_KEY] !== true) {
      base[PPE_MEASUREMENT_GLOVES_KEY] = "Обов'язкове підтвердження";
    }
    return base;
  }, [step, values]);

  const blocked = Object.keys(invalid).length > 0;

  const missingItems = Object.entries(invalid).map(([id, msg]) => ({
    id,
    label:
      step?.elements.find((e) => e.id === id)?.label ??
      (id === PPE_MEASUREMENT_GLOVES_KEY ? PPE_MEASUREMENT_GLOVES_LABEL : 'Поле кроку'),
    msg,
  }));

  const completeStep = async (overrideValues?: Record<string, unknown>) => {
    setTouched(true);
    const payload = overrideValues ?? values;
    // For non-conditional steps, validate; for conditional (mandatory false) allow empty
    const effectiveInvalid = overrideValues ? {} : invalid;
    const isBlocked = Object.keys(effectiveInvalid).length > 0;
    if (isBlocked) {
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
        values: JSON.stringify(payload),
      });
      toast.success(step ? `Крок "${step.name}" завершено` : 'Крок завершено');
      applyInstance(res.data);
      window.location.href = `/prosthetics/process/${instance.id}/wizard`;
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося завершити крок'));
    } finally {
      setSubmitting(false);
    }
  };

  const skipConditionalInsert = async () => {
    // For TP-LL-02 step 7.1 (mandatory false) — complete with empty values
    await completeStep({});
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

  const uploadFailEvidence = async (file: File) => {
    if (!instance?.currentExecutionId) {
      toast.error('Активне виконання кроку не знайдено.');
      return;
    }
    try {
      const res = await flowInstanceApi.uploadEvidence(instance.id, instance.currentExecutionId, file);
      setFailFiles((s) => [...s, res.data.fileName]);
      toast.success('Файл завантажено');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося завантажити файл'));
    }
  };

  const confirmFail = async () => {
    if (!instance) return;
    if (!failCategory || !failDescription.trim()) {
      toast.error('Оберіть категорію провалу та вкажіть опис причини.');
      return;
    }
    setSubmitting(true);
    try {
      await flowInstanceApi.fail(instance.id, {
        category: failCategory,
        description: failDescription.trim(),
      });
      toast.error('Процес позначено як провалений');
      navigate(`/prosthetics/process/${instance.id}/failed`, { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося позначити процес як провалений'));
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

  if (instance.status === 'FAILED' || instance.status === 'FAILED_QC' || instance.status === 'BRANCHED') {
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
      <div className="sticky top-0 z-20 -mx-4 border-b bg-card/95 px-4 py-3 backdrop-blur sm:-mx-6 sm:px-6">
        <div className="flex flex-wrap items-center gap-4">
          <div>
            <div className="font-display text-sm font-semibold">{snapshot.name}</div>
            <div className="text-xs text-muted-foreground">
              {orderInfo ? `${orderInfo.patientPib} · ${orderInfo.orderNumber} · ` : ''}
              {snapshot.productType} · {snapshot.amputationLevel ?? ''} {snapshot.limbSide ?? ''} · #{instance.id.slice(0, 8)}
            </div>
          </div>
          <StatusBadge status={instance.status} />
          <div
            className="ml-auto flex items-center gap-2 rounded-md bg-primary px-3 py-1.5 font-mono text-sm text-primary-foreground tabular-nums select-none"
            title="Накопичений час виконання процесу"
          >
            <Timer className="size-4" /> {fmt(seconds)}
          </div>
        </div>
        <div className="mt-3">
          <div className="flex flex-wrap items-center justify-between gap-x-2 text-xs text-muted-foreground">
            <span className="tabular-nums">
              Етап {stageIndex + 1} з {snapshot.stages.length}: {stage.name}
            </span>
            <span className="tabular-nums">
              Крок {stepIndexInStage + 1} з {stage.steps.length} · загалом {stepsDone}/{totalSteps}
            </span>
          </div>
          <Progress value={progress} className="mt-2" />
        </div>
        <div className="mt-3 flex items-center gap-2 overflow-x-auto pb-1 touch-pan-x">
          {snapshot.stages.map((s, i) => (
            <span
              key={s.id}
              className={`inline-flex shrink-0 items-center gap-1 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs transition-colors duration-200 ${
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
            <CardTitle className="mt-2 flex flex-wrap items-center gap-2 font-display text-xl">
              <Badge variant="secondary" className="h-5 px-1.5 text-[10px]">
                Крок {stepIndexInStage + 1}
              </Badge>
              <span>
                КРОК {stepIndexInStage + 1}: {step.name}
              </span>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            {touched && blocked && !validationAlertDismissed && missingItems.length > 0 && (
              <Alert variant="destructive" className="fade-in border-destructive/40 bg-destructive/5">
                <AlertTriangle className="size-4" />
                <AlertTitle>Заповніть обов&apos;язкові поля кроку</AlertTitle>
                <AlertDescription className="text-destructive/90">
                  <ul className="mt-1 space-y-1">
                    {missingItems.slice(0, 5).map(({ id, label, msg }) => (
                      <li key={id} className="flex items-start gap-1.5 text-xs">
                        <CircleAlert className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
                        <span>{label}{msg !== "Поле обов'язкове" ? ` — ${msg}` : ''}</span>
                      </li>
                    ))}
                    {missingItems.length > 5 && (
                      <li className="text-xs text-muted-foreground">
                        …та ще {missingItems.length - 5} {missingItems.length - 5 === 1 ? 'поле' : 'полів'}
                      </li>
                    )}
                  </ul>
                </AlertDescription>
                <AlertAction>
                  <Button
                    variant="ghost"
                    size="icon-xs"
                    aria-label="Сховати перелік"
                    onClick={() => setValidationAlertDismissed(true)}
                  >
                    <X className="size-3.5" />
                  </Button>
                </AlertAction>
              </Alert>
            )}
            <div key={step.id} className="step-fade-in space-y-5">
              {step.id === 'e0000005-0000-0000-0000-000000000005' ? (
                <div className="space-y-5">
                  <div className="space-y-3 rounded-xl border bg-muted/40 p-5">
                    <p className="text-sm font-semibold uppercase tracking-wide">ПЕРЕВІРКА ЯКОСТІ</p>
                    <CheckboxRow
                      id="plaster-positive-quality-checked"
                      checked={values['f0000006-0000-0000-0000-000000000001'] === true}
                      onChange={(c) =>
                        setValues((s) => ({
                          ...s,
                          'f0000006-0000-0000-0000-000000000001': c,
                        }))
                      }
                    >
                      Гіпсовий позитив перевірено на відповідність бланку замірів
                    </CheckboxRow>
                    <p className="text-xs text-muted-foreground">
                      Після відмітки переходьте до наступного етапу.
                    </p>
                  </div>
                  <div>
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
                  touched ? invalid : {},
                )
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="sticky bottom-0 z-20 -mx-4 flex flex-wrap items-center gap-3 border-t bg-card px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] sm:-mx-6 sm:px-6 sm:pb-3">
        <Button variant="outline" className="min-h-11" disabled={!canGoBack || submitting} onClick={() => void goBack()}>
          <ArrowLeft className="size-4" /> Попередній
        </Button>
        <Button variant="ghost" className="min-h-11" onClick={() => setPauseOpen(true)}>
          <PauseCircle className="size-4" /> Пауза
        </Button>
        <Button variant="ghost" className="min-h-11 text-destructive hover:bg-destructive/10" onClick={() => setFailOpen(true)}>
          <XCircle className="size-4" /> Позначити процес як провалений
        </Button>
        <Button variant="ghost" className="min-h-11" onClick={() => navigate('/prosthetics')}>
          <Home className="size-4" /> До головного меню
        </Button>
        {step?.id === 'e0000029-0000-0000-0000-000000000029' && (
          <Button
            variant="secondary"
            className="min-h-11 w-full sm:w-auto"
            disabled={submitting}
            onClick={() => void skipConditionalInsert()}
          >
            Пом&apos;якшуючий вкладиш не потрібен
          </Button>
        )}
        <Button
          className="ml-auto min-h-11 w-full bg-accent text-accent-foreground shadow-sm hover:bg-accent/90 hover:shadow-md sm:w-auto"
          disabled={(touched && blocked) || submitting}
          onClick={() => void completeStep()}
        >
          <Check className="size-4" /> {ctaLabel}
        </Button>
      </div>

      <Dialog open={pauseOpen} onOpenChange={setPauseOpen}>
        <DialogContent mobileFullscreen>
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

      <Dialog open={failOpen} onOpenChange={setFailOpen}>
        <DialogContent mobileFullscreen>
          <DialogHeader>
            <DialogTitle>Позначити процес як провалений</DialogTitle>
            <DialogDescription>
              Буде створено знімок провалу (Failure Snapshot), а процес перейде до вкладки
              «Провалені». Дію неможливо скасувати.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="fail-category" className="text-sm">
                Категорія провалу<span className="text-accent">*</span>
              </Label>
              <Select value={failCategory} onValueChange={(v) => setFailCategory(v ?? '')}>
                <SelectTrigger id="fail-category">
                  <SelectValue placeholder="Оберіть категорію з довідника" />
                </SelectTrigger>
                <SelectContent>
                  {FAILURE_CATEGORIES.map((c) => (
                    <SelectItem key={c.value} value={c.value}>
                      {c.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="fail-description" className="text-sm">
                Детальний опис причини<span className="text-accent">*</span>
              </Label>
              <Textarea
                id="fail-description"
                rows={4}
                placeholder="Опишіть, що сталося під час виконання кроку…"
                value={failDescription}
                onChange={(e) => setFailDescription(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label className="text-sm">Файли (опційно)</Label>
              <label className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-md border border-dashed p-4 text-sm text-muted-foreground transition-colors hover:bg-muted">
                <Input
                  type="file"
                  className="hidden"
                  multiple
                  onChange={(e) => {
                    const files = Array.from(e.target.files ?? []);
                    files.forEach((f) => void uploadFailEvidence(f));
                    e.target.value = '';
                  }}
                />
                <Upload className="size-4" />
                Додати фото або файли
              </label>
              {failFiles.length > 0 && (
                <ul className="space-y-1">
                  {failFiles.map((name) => (
                    <li key={name} className="flex items-center gap-2 text-xs text-muted-foreground">
                      <Check className="size-3.5 text-success" />
                      {name}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setFailOpen(false)}>
              Скасувати
            </Button>
            <Button variant="destructive" disabled={submitting} onClick={() => void confirmFail()}>
              <XCircle className="size-4" /> Позначити як провалений
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
        <CheckboxRow
          id={element.id}
          checked={value === true}
          onChange={onChange}
          variant="plain"
          className={errClass}
        >
          {element.label}
          {element.unit ? `, ${element.unit}` : ''}
          {element.required && <span className="text-accent">*</span>}
        </CheckboxRow>
      )}
      {element.elementType === 'TEXT_INPUT' && (
        <Input
          id={element.id}
          className={errClass}
          aria-invalid={error ? true : undefined}
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
          aria-invalid={error ? true : undefined}
          value={(value as string) ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
      {element.elementType === 'TEXTAREA' && (
        <Textarea
          id={element.id}
          className={errClass}
          aria-invalid={error ? true : undefined}
          rows={3}
          value={(value as string) ?? ''}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
      {element.elementType === 'DROPDOWN' && (
        <Select value={(value as string) ?? ''} onValueChange={(v) => onChange(v)}>
          <SelectTrigger id={element.id} className={errClass} aria-invalid={error ? true : undefined}>
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
