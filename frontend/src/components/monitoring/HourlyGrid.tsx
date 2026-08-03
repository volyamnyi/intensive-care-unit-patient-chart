import { useState, useCallback, useRef, useEffect, memo } from 'react';
import { Loader2 } from 'lucide-react';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import { medicalOrderApi } from '../../api/endpoints';
import type { ClinicalDay, HourlyRecord, MedicalOrder, OrderExecution } from '../../types';

const HOURS = Array.from({ length: 24 }, (_, i) => (i + 8) % 24);

function medDayPos(h: number): number { return h < 8 ? h + 24 : h; }
function isPastMedDay(h: number, clockHour: number): boolean { return medDayPos(h) < medDayPos(clockHour); }

const VITAL_ROWS: { key: keyof HourlyRecord; label: string; numeric: boolean }[] = [
  { key: 'systolicBP', label: 'АТсист', numeric: true },
  { key: 'diastolicBP', label: 'АТдіас', numeric: true },
  { key: 'heartRate', label: 'ЧСС', numeric: true },
  { key: 'respiratoryRate', label: 'ЧД', numeric: true },
  { key: 'temperature', label: 'Темп', numeric: true },
  { key: 'spo2', label: 'SpO₂', numeric: true },
  { key: 'etco2', label: 'EtCO₂', numeric: true },
  { key: 'fio2', label: 'FiO₂,%', numeric: true },
  { key: 'cvp', label: 'ЦВТ', numeric: true },
  { key: 'gcs', label: 'GCS', numeric: true },
];

const LOSS_ROWS: { key: keyof HourlyRecord; label: string }[] = [
  { key: 'urineOutput', label: 'Сеча' },
  { key: 'gastricOutput', label: 'Зонд' },
  { key: 'drainOutput', label: 'Дренаж' },
  { key: 'stool', label: 'Випорожнення' },
  { key: 'vomit', label: 'Блювота' },
];

const CARE_ROWS: { key: keyof HourlyRecord; label: string; numeric: boolean }[] = [
  { key: 'bedPosition', label: 'Положення у ліжку', numeric: false },
  { key: 'headEnd', label: 'Головний кінець ліжка', numeric: false },
];

const VASOPRESSOR_ROWS: { key: keyof HourlyRecord; label: string }[] = [
  { key: 'dopamine', label: 'Допамін (мкг/кг/хв)' },
  { key: 'dobutamine', label: 'Добутамін (мкг/кг/хв)' },
  { key: 'norepinephrine', label: 'Норадреналін (мкг/кг/хв)' },
  { key: 'epinephrine', label: 'Адреналін (мкг/кг/хв)' },
];

const CRITICAL_RANGES: Partial<Record<string, { min: number; max: number }>> = {
  systolicBP: { min: 90, max: 180 },
  diastolicBP: { min: 60, max: 120 },
  heartRate: { min: 50, max: 130 },
  temperature: { min: 35.5, max: 39.5 },
  spo2: { min: 90, max: 100 },
  respiratoryRate: { min: 10, max: 30 },
  cvp: { min: 2, max: 14 },
  gcs: { min: 8, max: 15 },
};

function isCritical(key: string, val: string): boolean {
  const range = CRITICAL_RANGES[key];
  if (!range) return false;
  const num = Number(val);
  if (Number.isNaN(num)) return false;
  return num < range.min || num > range.max;
}

interface CellProps {
  hour: number;
  rowKey: keyof HourlyRecord;
  numeric: boolean;
  label: string;
  value: string;
  isLocked: boolean;
  isNurse: boolean;
  isLossRow: boolean;
  isDark: boolean;
  isPast: boolean;
  onSave: (hour: number, key: keyof HourlyRecord, raw: string) => void;
}

const Cell = memo(function Cell({
  hour, rowKey, numeric, label, value, isLocked, isNurse, isLossRow, isDark: _isDark, isPast, onSave,
}: CellProps) {
  const [draft, setDraft] = useState(value);
  const focusedRef = useRef(false);
  const valueRef = useRef(value);
  valueRef.current = value;

  useEffect(() => {
    if (!focusedRef.current) {
      setDraft(value);
    }
  }, [value]);

  const readOnly = isLocked || (isNurse && !isLossRow);
  const critical = isCritical(String(rowKey), value);

  return (
    <TableCell className={cn('p-0', critical && 'bg-destructive/10', !critical && isPast && 'bg-success/10 dark:bg-success/20')} style={{ minWidth: 44 }}>
      <Input
        type={numeric ? 'number' : 'text'}
        disabled={readOnly}
        value={draft}
        aria-label={`${label} ${hour}:00`}
        onFocus={() => { focusedRef.current = true; }}
        onChange={(e) => setDraft(e.target.value)}
        onBlur={() => {
          focusedRef.current = false;
          const saved = valueRef.current;
          if (draft !== saved) onSave(hour, rowKey, draft);
        }}
        onKeyDown={(e) => { if (e.key === 'Enter') (e.target as HTMLInputElement).blur(); }}
        className={cn(
          'h-full w-full rounded-none p-0 text-center text-xs',
          critical && 'font-bold border border-destructive',
          !critical && 'border-0',
          readOnly && 'disabled:opacity-100',
          draft && critical && 'text-destructive',
          !draft && 'disabled:text-muted-foreground/60',
        )}
      />
    </TableCell>
  );
});

interface TherapyCellProps {
  order: MedicalOrder;
  hour: number;
  execution: OrderExecution | null;
  isDark: boolean;
  isPast: boolean;
  canPlan: boolean;
  canExecute: boolean;
  isExecuting: boolean;
  onPlan: (orderId: string, hour: number, dose: string) => void;
  onCancel: (orderId: string, hour: number) => void;
  onExecute: (orderId: string, hour: number, actualDose: string) => void;
  onExecuteFinish: (orderId: string, hour: number) => void;
}

const TherapyCell = memo(function TherapyCell({
  order, hour, execution, isDark: _isDark, isPast, canPlan, canExecute, isExecuting,
  onPlan, onCancel, onExecute, onExecuteFinish,
}: TherapyCellProps) {
  const [mode, setMode] = useState<'plan' | 'execute' | 'finish' | null>(null);
  const [doseInput, setDoseInput] = useState('');

  const cancelled = execution?.status === 'CANCELLED';
  const completed = execution?.status === 'COMPLETED' || execution?.status === 'PARTIALLY_COMPLETED';
  const planned = !!execution?.planned && !cancelled && !completed;
  const planFinished = !!execution?.plannedFinished;
  const completedFinished = !!execution?.completedFinished;

  const clickable = !isPast && !isExecuting && !cancelled
    && ((canPlan && (!execution || planned))
      || (canExecute && (planned || (completed && !completedFinished))));

  const handleClick = useCallback(() => {
    if (!clickable) return;
    if (completed) {
      if (canExecute && !completedFinished) setMode('finish');
      return;
    }
    if (planned && canExecute) {
      setDoseInput(execution?.plannedDose || order.dose || '');
      setMode('execute');
      return;
    }
    if (canPlan) {
      setDoseInput(execution?.plannedDose || order.dose || '');
      setMode('plan');
    }
  }, [clickable, completed, canExecute, completedFinished, planned, canPlan, execution, order.dose]);

  const handleConfirm = useCallback(() => {
    if (mode === 'plan' || mode === 'execute') {
      const dose = doseInput.trim();
      if (!dose) return;
      if (mode === 'plan') onPlan(order.id, hour, dose);
      else onExecute(order.id, hour, dose);
      setMode(null);
    }
  }, [mode, doseInput, onPlan, onExecute, order.id, hour]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleConfirm();
    if (e.key === 'Escape') setMode(null);
  }, [handleConfirm]);

  const tooltipText = `${order.drugName} ${hour}:00`
    + (cancelled ? ' (скасовано)' : '')
    + (completed ? ` (виконано${execution?.actualDose ? `, доза ${execution.actualDose}` : ''}${completedFinished ? ', завершено' : ''})` : '')
    + (planned ? ` (план: ${execution?.plannedDose || order.dose || '—'}${planFinished ? ', план завершено' : ''})` : '');

  if (mode === 'plan' || mode === 'execute') {
    return (
      <TableCell className={cn('p-0 text-center bg-muted dark:bg-warning/20')} style={{ minWidth: 44 }}>
        <div className="flex items-center">
          <Input
            autoFocus
            value={doseInput}
            onChange={(e) => setDoseInput(e.target.value)}
            onKeyDown={handleKeyDown}
            onBlur={handleConfirm}
            aria-label={mode === 'plan' ? `Запланувати ${order.drugName} ${hour}:00` : `Виконати ${order.drugName} ${hour}:00`}
            className="h-full w-9 rounded-none border-0 text-center text-xs"
          />
          {mode === 'plan' && execution && !completed && (
            <button
              type="button"
              aria-label={`Скасувати ${order.drugName} ${hour}:00`}
              onClick={() => { setMode(null); onCancel(order.id, hour); }}
              className="px-0.5 text-[10px] text-destructive"
            >
              {'✕'}
            </button>
          )}
        </div>
      </TableCell>
    );
  }

  if (mode === 'finish') {
    return (
      <TableCell className="p-0 text-center bg-[#C8E6C9]" style={{ minWidth: 44 }}>
        <div className="flex items-center justify-center gap-0.5">
          <Button
            size="sm"
            variant="outline"
            className="h-5 px-1 text-[10px]"
            onClick={() => { setMode(null); onExecuteFinish(order.id, hour); }}
          >
            {'Завершити'}
          </Button>
          <button
            type="button"
            aria-label={`Закрити ${order.drugName} ${hour}:00`}
            onClick={() => setMode(null)}
            className="px-0.5 text-[10px] text-muted-foreground"
          >
            {'✕'}
          </button>
        </div>
      </TableCell>
    );
  }

  const bgClass = cancelled
    ? 'bg-[#E1BEE7]'
    : completed
      ? 'bg-[#C8E6C9]'
      : planned
        ? (planFinished ? 'bg-[#E1BEE7]' : 'bg-[#BBDEFB]')
        : (isPast ? 'bg-success/10 dark:bg-success/20' : '');

  return (
    <TableCell
      onClick={handleClick}
      className={cn('p-1 text-center', bgClass)}
      style={{ minWidth: 44, cursor: clickable ? 'pointer' : 'default' }}
    >
      {isExecuting ? <Loader2 className="inline size-3 animate-spin" /> : (
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger>
              <span className={cn('text-sm', cancelled && 'text-[#7B1FA2]', completed && 'text-[#2E7D32]', planned && 'text-[#1565C0]')}>
                {cancelled ? '✕' : completed ? '✓' : planned ? (execution?.plannedDose || order.dose || '') : (isPast ? '✓' : '➚')}
              </span>
            </TooltipTrigger>
            <TooltipContent>
              {tooltipText}
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>
      )}
    </TableCell>
  );
});

function getErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    if (axiosErr.response?.data?.message) return axiosErr.response.data.message;
  }
  return err instanceof Error ? err.message : fallback;
}

function getNextHourISO(): string {
  const now = new Date();
  now.setMinutes(0, 0, 0);
  now.setHours(now.getHours() + 1);
  return now.toISOString().slice(0, 16);
}

function OrderInlineForm({ selectedDay, isLocked, onCreated, onCancel, onError }: {
  selectedDay: ClinicalDay | null;
  isLocked: boolean;
  onCreated: () => void;
  onCancel: () => void;
  onError?: (msg: string) => void;
}) {
  const [form, setForm] = useState({ category: 'MEDICATION', drugName: '', dose: '', unit: '', route: '', frequency: '', startTime: getNextHourISO() });
  const [saving, setSaving] = useState(false);

  const handleCreate = async () => {
    if (!selectedDay || isLocked) return;
    try {
      setSaving(true);
      await medicalOrderApi.create(selectedDay.id, {
        category: form.category,
        drugName: form.drugName,
        dose: form.dose,
        unit: form.unit,
        route: form.route,
        frequency: form.frequency,
        startTime: form.startTime || getNextHourISO(),
      });
      setForm({ category: 'MEDICATION', drugName: '', dose: '', unit: '', route: '', frequency: '', startTime: '' });
      onCreated();
      onCancel();
    } catch (err) {
      onError?.(getErrorMessage(err, 'Не вдалося створити призначення'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-1.5 mt-1">
      <p className="font-bold text-xs mb-1">Нове призначення</p>
      <div className="flex flex-col gap-1">
        <Input placeholder="Препарат" value={form.drugName} onChange={(e) => setForm({ ...form, drugName: e.target.value })} />
        <div className="flex gap-1">
          <Input placeholder="Доза" value={form.dose} onChange={(e) => setForm({ ...form, dose: e.target.value })} />
          <Input placeholder="Од." value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })} />
        </div>
        <div className="flex gap-1">
          <Input placeholder="Шлях" value={form.route} onChange={(e) => setForm({ ...form, route: e.target.value })} />
          <Input placeholder="Частота" value={form.frequency} onChange={(e) => setForm({ ...form, frequency: e.target.value })} />
        </div>
        <Input type="datetime-local" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
        <div>
          <Select value={form.category} onValueChange={(v: string | null) => setForm({ ...form, category: v ?? form.category })}>
            <SelectTrigger className="w-full">
              <SelectValue placeholder="Категорія" />
            </SelectTrigger>
            <SelectContent>
              {['MEDICATION', 'INFUSION', 'LAB', 'MANIPULATION', 'VENTILATION', 'NUTRITION', 'OTHER'].map(c => (
                <SelectItem key={c} value={c}>{c}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>
      <div className="flex gap-1 mt-1">
        <Button onClick={handleCreate} disabled={saving || !form.drugName} size="sm">
          {'Створити'}
        </Button>
        <Button onClick={onCancel} variant="outline" size="sm">
          {'Скасувати'}
        </Button>
      </div>
      {form.startTime && !form.startTime.endsWith(':00') && (
        <p className="text-[10px] text-amber-600 mt-0.5 block">
          {'Призначення почнеться з наступної години'}
        </p>
      )}
    </div>
  );
}

function GroupHeader({ label, nurseEditable }: { label: string; nurseEditable?: boolean }) {
  return (
    <TableRow className="bg-muted/60">
      <TableCell colSpan={25} className="font-extrabold text-xs py-0.5 border-r border-border">
        {label}
        {nurseEditable === false && (
          <span className="ml-1 text-[9px] text-muted-foreground">(тільки лікар)</span>
        )}
      </TableCell>
    </TableRow>
  );
}

export interface HourlyGridProps {
  isMobile: boolean;
  isNurse: boolean;
  isLocked: boolean;
  user: { id: number } | null;
  selectedDay: ClinicalDay | null;
  recByHour: Map<number, HourlyRecord>;
  orders: MedicalOrder[];
  activeOrders: MedicalOrder[];
  executionsByOrder: Record<string, OrderExecution[]>;
  executing: string | null;
  orderFormOpen: boolean;
  realClockHour: number;
  canEditSidebar: boolean;
  onSetOrderFormOpen: (open: boolean) => void;
  onSaveCell: (hour: number, key: keyof HourlyRecord, raw: string) => void;
  onPlanOrder: (orderId: string, hour: number, dose: string) => void;
  onCancelOrder: (orderId: string, hour: number) => void;
  onExecuteOrder: (orderId: string, hour: number, actualDose: string) => void;
  onExecuteFinishOrder: (orderId: string, hour: number) => void;
  onRefresh?: () => void;
  onError?: (msg: string) => void;
  toolbar?: React.ReactNode;
  onHeaderDoubleClick?: () => void;
}

export default function HourlyGrid({
  isMobile, isNurse, isLocked, user, selectedDay,
  recByHour, activeOrders, executionsByOrder, executing, orderFormOpen, realClockHour,
  canEditSidebar, onSetOrderFormOpen, onSaveCell,
  onPlanOrder, onCancelOrder, onExecuteOrder, onExecuteFinishOrder,
  onRefresh, onError, toolbar, onHeaderDoubleClick,
}: HourlyGridProps) {
  const boundValue = (hour: number, key: keyof HourlyRecord): string => {
    const r = recByHour.get(hour);
    const v = r ? r[key] : null;
    if (v === null || v === undefined) return '';
    return String(v);
  };

  return (
    <main className={cn('min-w-0', isMobile ? 'w-full' : 'flex-1')}>
      <div className="overflow-hidden rounded-xl border border-border bg-card">
        {toolbar}
        <div className="overflow-x-auto">
          <Table className="min-w-[1100px]" style={{ tableLayout: 'fixed' }}>
            <TableHeader>
              <TableRow className="bg-muted">
                <TableHead className="font-bold min-w-[130px] border-r border-border">
                  {onHeaderDoubleClick ? (
                    <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger
                          render={(
                            <span
                              className="block w-full cursor-pointer select-none"
                              onDoubleClick={onHeaderDoubleClick}
                              tabIndex={-1}
                            />
                          )}
                        >
                          {'Показник / година'}
                        </TooltipTrigger>
                        <TooltipContent side="right">{'Двічі клацніть, щоб розгорнути'}</TooltipContent>
                      </Tooltip>
                    </TooltipProvider>
                  ) : (
                    'Показник / година'
                  )}
                </TableHead>
              {HOURS.map((h) => (
                <TableHead key={h} className="text-center font-bold text-xs p-1 border-r border-border last:border-r-0">{h}:00</TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            <GroupHeader label="Показники" nurseEditable={!isNurse} />
            {VITAL_ROWS.map((row) => (
              <TableRow key={String(row.key)}>
                <TableCell className="font-semibold text-xs whitespace-nowrap border-r border-border">{row.label}</TableCell>
                {HOURS.map((h) => (
                  <Cell
                    key={h}
                    hour={h}
                    rowKey={row.key}
                    numeric={row.numeric}
                    label={row.label}
                    value={boundValue(h, row.key)}
                    isLocked={isLocked}
                    isNurse={isNurse}
                    isLossRow={false}
                    isDark={false}
                    isPast={isPastMedDay(h, realClockHour)}
                    onSave={onSaveCell}
                  />
                ))}
              </TableRow>
            ))}

            <GroupHeader label="Втрати (мл)" nurseEditable />
            {LOSS_ROWS.map((row) => (
              <TableRow key={String(row.key)}>
                <TableCell className="font-semibold text-xs border-r border-border">{row.label}</TableCell>
                {HOURS.map((h) => (
                  <Cell
                    key={h}
                    hour={h}
                    rowKey={row.key}
                    numeric
                    label={row.label}
                    value={boundValue(h, row.key)}
                    isLocked={isLocked}
                    isNurse={isNurse}
                    isLossRow
                    isDark={false}
                    isPast={isPastMedDay(h, realClockHour)}
                    onSave={onSaveCell}
                  />
                ))}
              </TableRow>
            ))}

            <GroupHeader label="Заходи по догляду" nurseEditable />
            {CARE_ROWS.map((row) => (
              <TableRow key={String(row.key)}>
                <TableCell className="font-semibold text-xs border-r border-border">{row.label}</TableCell>
                {HOURS.map((h) => (
                  <Cell
                    key={h}
                    hour={h}
                    rowKey={row.key}
                    numeric={row.numeric}
                    label={row.label}
                    value={boundValue(h, row.key)}
                    isLocked={isLocked}
                    isNurse={isNurse}
                    isLossRow
                    isDark={false}
                    isPast={isPastMedDay(h, realClockHour)}
                    onSave={onSaveCell}
                  />
                ))}
              </TableRow>
            ))}

            <GroupHeader label="Вазопресорна та інотропна підтримка (мкг/кг/хв)" nurseEditable />
            {VASOPRESSOR_ROWS.map((row) => (
              <TableRow key={String(row.key)}>
                <TableCell className="font-semibold text-xs border-r border-border">{row.label}</TableCell>
                {HOURS.map((h) => (
                  <Cell
                    key={h}
                    hour={h}
                    rowKey={row.key}
                    numeric
                    label={row.label}
                    value={boundValue(h, row.key)}
                    isLocked={isLocked}
                    isNurse={isNurse}
                    isLossRow
                    isDark={false}
                    isPast={isPastMedDay(h, realClockHour)}
                    onSave={onSaveCell}
                  />
                ))}
              </TableRow>
            ))}

            <TableRow className="bg-muted/60">
              <TableCell colSpan={25} className="font-extrabold text-xs py-0.5 border-r border-border">
                <div className="flex justify-between items-center">
                  <span>Терапія (призначення)</span>
                  {canEditSidebar && !isNurse && (
                    <Button size="sm" variant="outline" onClick={() => onSetOrderFormOpen(!orderFormOpen)} className="text-[10px] h-5 py-0">
                      {orderFormOpen ? 'X Сховати' : '+ Нове призначення'}
                    </Button>
                  )}
                </div>
              </TableCell>
            </TableRow>
            {orderFormOpen && (
              <TableRow>
                <TableCell colSpan={25} className="p-0 border-none">
                  <OrderInlineForm
                    selectedDay={selectedDay}
                    isLocked={isLocked}
                    onCreated={onRefresh ?? (() => {})}
                    onCancel={() => onSetOrderFormOpen(false)}
                    onError={onError}
                  />
                </TableCell>
              </TableRow>
            )}
            {activeOrders.length === 0 && !orderFormOpen && (
              <TableRow>
                <TableCell colSpan={25} className="text-center text-muted-foreground py-1">
                  {'Немає призначень'}
                </TableCell>
              </TableRow>
            )}
            {activeOrders.map((order) => (
              <TableRow key={order.id}>
                <TableCell className="text-[10px] whitespace-nowrap overflow-hidden text-ellipsis border-r border-border">
                  {order.drugName} {order.dose}{order.unit}{' '}
                  <span className="text-xs font-bold text-success">{order.status === 'ACTIVE' ? 'Активне' : order.status === 'DRAFT' ? 'Чернетка' : ''}</span>
                </TableCell>
                {HOURS.map((h) => (
                  <TherapyCell
                    key={h}
                    order={order}
                    hour={h}
                    execution={(executionsByOrder[order.id] ?? []).find(e => e.hour === h) ?? null}
                    isDark={false}
                    isPast={isPastMedDay(h, realClockHour)}
                    canPlan={!isLocked && !isNurse && !!user}
                    canExecute={!isLocked && isNurse && !!user}
                    isExecuting={executing === `${order.id}-${h}`}
                    onPlan={onPlanOrder}
                    onCancel={onCancelOrder}
                    onExecute={onExecuteOrder}
                    onExecuteFinish={onExecuteFinishOrder}
                  />
                ))}
              </TableRow>
            ))}
          </TableBody>
          </Table>
        </div>
      </div>
    </main>
  );
}
