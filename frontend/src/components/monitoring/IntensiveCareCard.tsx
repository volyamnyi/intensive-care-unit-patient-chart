import { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { Maximize2, RefreshCw } from 'lucide-react';
import { cn } from '@/lib/utils';
import { SidebarProvider } from '../ui/Sidebar';
import { useAutoSave } from '../../hooks/useAutoSave';
import { hourlyRecordApi, orderExecutionApi, medicalNoteApi, clinicalScaleApi, ventilationApi, labResultApi, patientStateApi } from '../../api/endpoints';
import { Button } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import HourlyGrid, { type HourlyGridProps } from './HourlyGrid';
import HourlyGridDialog from './HourlyGridDialog';
import PatientSidebar from './PatientSidebar';
import type { Episode, ClinicalDay, HourlyRecord, MedicalOrder, FluidBalanceItem, ClinicalScale, ScaleResult, HourlyRecordCreateRequest, MedicalNoteCreateRequest, LabResultCreateRequest, VentilationCreateRequest, PatientStateCreateRequest, OrderExecution } from '../../types';

interface UserLike { id: number; }

function getErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    if (axiosErr.response?.data?.message) return axiosErr.response.data.message;
  }
  return err instanceof Error ? err.message : fallback;
}

interface UndoEntry {
  hour: number;
  key: keyof HourlyRecord;
  prevValue: number | string;
  newValue: number | string;
  version: number;
  id: string;
}

interface IntensiveCareCardProps {
  episode: Episode;
  selectedDay: ClinicalDay | null;
  records: HourlyRecord[];
  orders: MedicalOrder[];
  balanceItems: FluidBalanceItem[];
  isNurse: boolean;
  isLocked: boolean;
  user: UserLike | null;
  onRefresh?: () => void;
  onFeedback?: (message: string, severity: 'success' | 'error') => void;
  loading?: boolean;
}

export default function IntensiveCareCard({
  episode, selectedDay, records, orders, balanceItems, isNurse, isLocked, user, onRefresh, onFeedback, loading,
}: IntensiveCareCardProps) {
  const [executing, setExecuting] = useState<string | null>(null);
  const [notes, setNotes] = useState<{ id: string; text: string; authorId?: string | null; role?: string | null; createdAt?: string | null }[]>([]);
  const [noteText, setNoteText] = useState('');
  const noteTextRef = useRef('');
  const [savingNote, setSavingNote] = useState(false);
  const selectedDayRef = useRef(selectedDay);
  selectedDayRef.current = selectedDay;

  const localRecordMap = useRef<Map<number, { id: string; version: number }>>(new Map());
  useEffect(() => { localRecordMap.current.clear(); }, [records]);

  const [undoStack, setUndoStack] = useState<UndoEntry[]>([]);
  const undoStackRef = useRef<UndoEntry[]>([]);
  undoStackRef.current = undoStack;
  useEffect(() => { setUndoStack([]); }, [selectedDay?.id]);

  const notifyParentRef = useRef(onFeedback ?? (() => {}));
  notifyParentRef.current = onFeedback ?? (() => {});

  const [gridFeedback, setGridFeedback] = useState<{ message: string; severity: 'success' | 'error' } | null>(null);
  const [gridExpanded, setGridExpanded] = useState(false);
  const [conflict, setConflict] = useState<{ hour: number; key: keyof HourlyRecord; raw: string } | null>(null);
  const retryRef = useRef<{ hour: number; key: keyof HourlyRecord; raw: string } | null>(null);
  useEffect(() => {
    if (gridExpanded) setGridFeedback(null);
  }, [gridExpanded]);

  useEffect(() => { setNoteText(''); noteTextRef.current = ''; }, [selectedDay?.id]);

  const saveCurrentNote = useCallback(async () => {
    const text = noteTextRef.current.trim();
    const day = selectedDayRef.current;
    if (!day || isLocked || !text) return;
    try {
      setSavingNote(true);
      await medicalNoteApi.create(day.id, {
        text, noteType: 'CLINICAL', role: isNurse ? 'NURSE' : 'DOCTOR',
      } as unknown as MedicalNoteCreateRequest);
      setNoteText(''); noteTextRef.current = '';
      const refreshed = await medicalNoteApi.getByClinicalDay(day.id);
      setNotes(refreshed.data as unknown as { id: string; text: string; authorId?: string }[]);
    } catch (err) {
      notifyParentRef.current(getErrorMessage(err, 'Не вдалося зберегти нотатку'), 'error');
    } finally { setSavingNote(false); }
  }, [isLocked, isNurse]);

  const { status: autoSaveStatus, markDirty, saveNow } = useAutoSave({
    onSave: saveCurrentNote, delay: 2000, enabled: !!selectedDay && !isLocked,
  });

  const handleNoteChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const v = e.target.value; setNoteText(v); noteTextRef.current = v; markDirty();
  };

  const [scales, setScales] = useState<ScaleResult[]>([]);
  const [availableScales, setAvailableScales] = useState<ClinicalScale[]>([]);
  const [ventilation, setVentilation] = useState<{ id: string; mode?: string; [k: string]: unknown }[]>([]);
  const [labs, setLabs] = useState<{ id: string; testName?: string; result?: string }[]>([]);
  const [patientState, setPatientState] = useState<{ id: string; assessment?: string }[]>([]);
  const [loadingSidebar, setLoadingSidebar] = useState(false);
  const canEditSidebar = !isLocked;

  const refreshSidebar = useCallback(async () => {
    if (!selectedDay) return;
    try {
      const [n, s, es, a, v, l, p] = await Promise.all([
        medicalNoteApi.getByClinicalDay(selectedDay.id).then(r => r.data ?? []).catch(() => []),
        clinicalScaleApi.getResultsByClinicalDay(selectedDay.id).then(r => r.data ?? []).catch(() => []),
        clinicalScaleApi.getResultsByEpisode(episode.id).then(r => r.data ?? []).catch(() => []),
        clinicalScaleApi.getAvailable().then(r => r.data ?? []).catch(() => []),
        ventilationApi.getByClinicalDay(selectedDay.id).then(r => r.data ?? []).catch(() => []),
        labResultApi.getByClinicalDay(selectedDay.id).then(r => r.data ?? []).catch(() => []),
        patientStateApi.getByClinicalDay(selectedDay.id).then(r => r.data ?? []).catch(() => []),
      ]);
      setNotes(n as unknown as { id: string; text: string; authorId?: string | null; role?: string | null; createdAt?: string | null }[]);
      const allScales = [...(s ?? []), ...(es ?? [])];
      setScales(allScales);
      setAvailableScales(a as unknown as ClinicalScale[]);
      setVentilation(v as unknown as { id: string; mode?: string; [k: string]: unknown }[]);
      setLabs(l as unknown as { id: string; testName?: string; result?: string }[]);
      setPatientState(p as unknown as { id: string; assessment?: string }[]);
    } catch (err) {
      notifyParentRef.current(getErrorMessage(err, 'Не вдалося оновити бічну панель'), 'error');
    }
  }, [selectedDay, episode.id]);

  const createLab = async (data: LabResultCreateRequest) => {
    if (!selectedDay || isLocked) return;
    try { await labResultApi.create(selectedDay.id, data); await refreshSidebar(); }
    catch (err) { notifyParentRef.current(getErrorMessage(err, 'Не вдалося створити лаб. результат'), 'error'); }
  };
  const createVentilation = async (data: VentilationCreateRequest) => {
    if (!selectedDay || isLocked) return;
    try { await ventilationApi.create(selectedDay.id, data); await refreshSidebar(); }
    catch (err) { notifyParentRef.current(getErrorMessage(err, 'Не вдалося додати ШВЛ'), 'error'); }
  };
  const createPatientState = async (data: PatientStateCreateRequest) => {
    if (!selectedDay || isLocked) return;
    try { await patientStateApi.create(selectedDay.id, data); await refreshSidebar(); }
    catch (err) { notifyParentRef.current(getErrorMessage(err, 'Не вдалося зберегти стан пацієнта'), 'error'); }
  };
  const createScale = async (scaleId: string, result: string) => {
    if (!selectedDay || isLocked) return;
    try {
      const scale = availableScales.find(s => s.id === scaleId);
      if (scale && /APACHE|SOFA/i.test(scale.name)) {
        await clinicalScaleApi.createEpisodeResult(episode.id, { scaleId, result });
      } else {
        await clinicalScaleApi.createResult(selectedDay.id, { scaleId, result });
      }
      await refreshSidebar();
    } catch (err) {
      notifyParentRef.current(getErrorMessage(err, 'Не вдалося зберегти шкалу'), 'error');
    }
  };
  const calculateScale = async (scaleId: string, rawData: Record<string, unknown>) => {
    if (!selectedDay || isLocked) return;
    try {
      await clinicalScaleApi.calculateAndSave(episode.id, scaleId, rawData, selectedDay.id);
      await refreshSidebar();
    } catch (err) {
      notifyParentRef.current(getErrorMessage(err, 'Не вдалося розрахувати шкалу'), 'error');
    }
  };

  const [orderFormOpen, setOrderFormOpen] = useState(false);
  const expandTriggerRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!selectedDay) return;
    setLoadingSidebar(true);
    refreshSidebar().finally(() => setLoadingSidebar(false));
  }, [selectedDay, refreshSidebar]);

  const recByHour = useMemo(() => {
    const map = new Map<number, HourlyRecord>();
    for (const r of records) {
      const h = Number(String(r.recordTime).substring(11, 13));
      if (!Number.isNaN(h)) map.set(h, r);
    }
    return map;
  }, [records]);

  const realClockHour = new Date().getHours();

  const keyScales = useMemo(() => {
    const names = ['APACHE II', 'SOFA', 'RASS', 'CAM-ICU', 'Браден'];
    return names.map(name => {
      const found = scales.find(s =>
        (s.scaleName || '')?.toLowerCase() === name.toLowerCase()
        || (s.scaleName || '')?.includes(name)
      );
      return found ? { name, result: found.result } : null;
    }).filter(Boolean) as { name: string; result: string }[];
  }, [scales]);

  const pushUndo = useCallback((entry: UndoEntry) => {
    setUndoStack(prev => [...prev.slice(-19), entry]);
  }, []);

  const undoLastChange = useCallback(async () => {
    if (!selectedDay || isLocked) return;
    const entry = undoStackRef.current[undoStackRef.current.length - 1];
    if (!entry) return;
    setUndoStack(prev => prev.slice(0, -1));
    try {
      const patch: Partial<HourlyRecordCreateRequest> & { version: number } = { version: entry.version };
      (patch as Record<string, unknown>)[entry.key] = entry.prevValue;
      const res = await hourlyRecordApi.update(entry.id, patch);
      localRecordMap.current.set(entry.hour, { id: res.data.id, version: res.data.version });
      onRefresh?.();
      setGridFeedback({ message: 'Зміну скасовано', severity: 'success' });
    } catch (err) {
      setUndoStack(prev => [...prev, entry]);
      const is409 = !!(err && typeof err === 'object' && 'response' in err
        && (err as { response?: { status?: number } }).response?.status === 409);
      if (is409) {
        setConflict({ hour: entry.hour, key: entry.key, raw: String(entry.newValue) });
        setGridFeedback({ message: 'Запис змінено іншим користувачем', severity: 'error' });
      } else {
        const message = getErrorMessage(err, 'Не вдалося скасувати зміну');
        setGridFeedback({ message, severity: 'error' });
        notifyParentRef.current(message, 'error');
      }
    }
  }, [selectedDay, isLocked, onRefresh]);

  const saveCell = useCallback(async (hour: number, key: keyof HourlyRecord, raw: string) => {
    if (!selectedDay || isLocked) return;
    const textKeys: (keyof HourlyRecord)[] = ['consciousness', 'stool', 'vomit', 'bedPosition', 'headEnd'];
    const numeric = !textKeys.includes(key);
    const value = raw.trim() === '' ? null : numeric ? Number(raw) : raw;
    const existing: { id: string; version: number } | undefined = recByHour.get(hour) || localRecordMap.current.get(hour);
    const prevRecord = existing ? (recByHour.get(hour) ?? null) : null;
    const prevValue: number | string | null =
      prevRecord && prevRecord[key] != null ? (prevRecord[key] as number | string) : null;
    const recTime = `${new Date().toISOString().split('T')[0]}T${String(hour).padStart(2, '0')}:00:00`;
    try {
      if (existing) {
        const patch: Partial<HourlyRecordCreateRequest> & { version: number } = { version: existing.version };
        if (value !== null) { (patch as Record<string, unknown>)[key] = value; }
        const res = await hourlyRecordApi.update(existing.id, patch);
        localRecordMap.current.set(hour, { id: existing.id, version: res.data.version });
        if (value !== null && prevValue !== null && prevValue !== value) {
          pushUndo({ hour, key, prevValue, newValue: value, version: res.data.version, id: existing.id });
        }
      } else if (value !== null) {
        const res = await hourlyRecordApi.create(selectedDay.id, { recordTime: recTime, [key]: value } as HourlyRecordCreateRequest);
        localRecordMap.current.set(hour, { id: res.data.id, version: res.data.version });
      }
      onRefresh?.();
      setGridFeedback({ message: `Збережено ${String(hour).padStart(2, '0')}:00`, severity: 'success' });
    } catch (err) {
      const is409 = !!(err && typeof err === 'object' && 'response' in err
        && (err as { response?: { status?: number } }).response?.status === 409);
      if (is409) {
        setConflict({ hour, key, raw });
        setGridFeedback({ message: 'Запис змінено іншим користувачем', severity: 'error' });
        if (!gridExpanded) notifyParentRef.current('Запис змінено іншим користувачем', 'error');
        return;
      }
      const message = getErrorMessage(err, 'Не вдалося зберегти показник');
      setGridFeedback({ message, severity: 'error' });
      notifyParentRef.current(message, 'error');
    }
  }, [selectedDay, isLocked, recByHour, onRefresh, gridExpanded, pushUndo]);

  const saveCellRef = useRef(saveCell);
  saveCellRef.current = saveCell;

  const resolveConflict = useCallback((keep: boolean) => {
    if (!conflict) return;
    if (keep) retryRef.current = conflict;
    setConflict(null);
    onRefresh?.();
  }, [conflict, onRefresh]);

  useEffect(() => {
    const pending = retryRef.current;
    if (!pending) return;
    retryRef.current = null;
    void saveCellRef.current(pending.hour, pending.key, pending.raw);
  }, [records]);

  const activeOrders = orders.filter(o => o.status === 'ACTIVE' || o.status === 'DRAFT');
  const [executionsByOrder, setExecutionsByOrder] = useState<Record<string, OrderExecution[]>>({});

  const refreshExecutions = useCallback(async (orderIds: string[]) => {
    const unique = [...new Set(orderIds)];
    if (unique.length === 0) return;
    try {
      const results = await Promise.all(
        unique.map(id => orderExecutionApi.getByOrder(id).then(r => r.data ?? []).catch(() => [] as OrderExecution[])),
      );
      setExecutionsByOrder(prev => {
        const next = { ...prev };
        unique.forEach((id, i) => { next[id] = results[i]; });
        return next;
      });
    } catch (err) {
      notifyParentRef.current(getErrorMessage(err, 'Не вдалося завантажити виконання'), 'error');
    }
  }, []);

  useEffect(() => {
    refreshExecutions(activeOrders.map(o => o.id));
  }, [selectedDay?.id, orders]);

  const runExecutionAction = useCallback(async (
    key: string, orderId: string, action: () => Promise<unknown>, failMessage: string,
  ): Promise<boolean> => {
    try {
      setExecuting(key);
      await action();
      await refreshExecutions([orderId]);
      onRefresh?.();
      return true;
    } catch (err) {
      notifyParentRef.current(getErrorMessage(err, failMessage), 'error');
      return false;
    } finally {
      setExecuting(null);
    }
  }, [refreshExecutions, onRefresh]);

  const handlePlanOrder = useCallback((orderId: string, hour: number, dose: string) =>
    runExecutionAction(`${orderId}-${hour}`, orderId,
      () => orderExecutionApi.plan(orderId, { hour, dose }), 'Не вдалося запланувати виконання'),
  [runExecutionAction]);

  const [undoToast, setUndoToast] = useState<{ orderId: string; hour: number; dose: string } | null>(null);
  const undoToastRef = useRef<{ orderId: string; hour: number; dose: string } | null>(null);
  undoToastRef.current = undoToast;
  const undoToastTimerRef = useRef<number | null>(null);
  const showUndoToast = useCallback((toast: { orderId: string; hour: number; dose: string }) => {
    setUndoToast(toast);
    if (undoToastTimerRef.current !== null) window.clearTimeout(undoToastTimerRef.current);
    undoToastTimerRef.current = window.setTimeout(() => setUndoToast(null), 5000);
  }, []);
  useEffect(() => () => {
    if (undoToastTimerRef.current !== null) window.clearTimeout(undoToastTimerRef.current);
  }, []);

  const handleCancelOrder = useCallback(async (orderId: string, hour: number) => {
    const execution = (executionsByOrder[orderId] ?? []).find(e => e.hour === hour);
    const dose = execution?.plannedDose || orders.find(o => o.id === orderId)?.dose || '';
    const ok = await runExecutionAction(`${orderId}-${hour}`, orderId,
      () => orderExecutionApi.cancel(orderId, { hour }), 'Не вдалося скасувати виконання');
    if (ok && dose) showUndoToast({ orderId, hour, dose });
  }, [runExecutionAction, executionsByOrder, orders, showUndoToast]);

  const restoreExecution = useCallback(async () => {
    const toast = undoToastRef.current;
    if (!toast) return;
    if (undoToastTimerRef.current !== null) window.clearTimeout(undoToastTimerRef.current);
    setUndoToast(null);
    await runExecutionAction(`undo-${toast.orderId}-${toast.hour}`, toast.orderId,
      () => orderExecutionApi.plan(toast.orderId, { hour: toast.hour, dose: toast.dose }),
      'Не вдалося відновити виконання');
  }, [runExecutionAction]);

  const handleExecuteOrder = useCallback((orderId: string, hour: number, actualDose: string) =>
    runExecutionAction(`${orderId}-${hour}`, orderId,
      () => orderExecutionApi.execute(orderId, { hour, actualDose }), 'Не вдалося виконати призначення'),
  [runExecutionAction]);

  const handleExecuteFinishOrder = useCallback((orderId: string, hour: number) =>
    runExecutionAction(`${orderId}-${hour}`, orderId,
      () => orderExecutionApi.executeFinish(orderId, { hour }), 'Не вдалося завершити виконання'),
  [runExecutionAction]);

  const totalIntake = balanceItems.reduce((s, i) => s + (i.intake || 0), 0);
  const totalOutput = balanceItems.reduce((s, i) => s + (i.output || 0), 0);
  const dailyBalance = totalIntake - totalOutput;
  const cumulativeBalance = balanceItems[balanceItems.length - 1]?.cumulativeBalance ?? 0;

  const [isMobile, setIsMobile] = useState(false);
  useEffect(() => {
    const mq = window.matchMedia('(max-width:1200px)');
    setIsMobile(mq.matches);
    const handler = (e: MediaQueryListEvent) => setIsMobile(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  const gridProps: HourlyGridProps = {
    isMobile,
    isNurse,
    isLocked,
    user,
    selectedDay,
    recByHour,
    orders,
    activeOrders,
    executionsByOrder,
    executing,
    orderFormOpen,
    realClockHour,
    canEditSidebar,
    onSetOrderFormOpen: setOrderFormOpen,
    onSaveCell: saveCell,
    onPlanOrder: handlePlanOrder,
    onCancelOrder: handleCancelOrder,
    onExecuteOrder: handleExecuteOrder,
    onExecuteFinishOrder: handleExecuteFinishOrder,
    onRefresh,
    onError: (msg) => notifyParentRef.current(msg, 'error'),
  };

  const gridToolbar = (
    <div className="flex h-9 items-center justify-between gap-2 border-b border-border bg-card px-2">
      <span className="text-sm font-semibold text-card-foreground">{'Погодинна карта'}</span>
      <div className="flex items-center gap-1">
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger
              render={(
                <Button
                  variant="outline"
                  size="sm"
                  ref={expandTriggerRef}
                  onClick={() => setGridExpanded(true)}
                  disabled={!selectedDay || gridExpanded}
                  aria-label="Розгорнути на весь екран"
                  aria-keyshortcuts="Alt+Enter"
                >
                  <Maximize2 className="size-3.5" />
                  {'На весь екран'}
                </Button>
              )}
            />
            <TooltipContent side="bottom">{'На весь екран (Alt+Enter)'}</TooltipContent>
          </Tooltip>
        </TooltipProvider>
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger
              render={(
                <Button
                  variant="ghost"
                  size="icon-sm"
                  onClick={onRefresh}
                  disabled={!selectedDay}
                  aria-label="Оновити показники"
                >
                  <RefreshCw className="size-4" />
                </Button>
              )}
            />
            <TooltipContent side="bottom">{'Оновити показники'}</TooltipContent>
          </Tooltip>
        </TooltipProvider>
      </div>
    </div>
  );

  return (
    <>
      <SidebarProvider defaultWidth={300} minWidth={200} maxWidth={600}>
      <div className={cn('flex items-start relative', isMobile ? 'flex-col' : 'flex-row')}>
        <HourlyGrid
          {...gridProps}
          toolbar={gridToolbar}
          onHeaderDoubleClick={() => setGridExpanded(true)}
        />
        <div className={cn(isMobile && 'hidden')}>
          <PatientSidebar
            episode={episode}
            selectedDay={selectedDay}
            isLocked={isLocked}
            records={records}
            notes={notes}
            noteText={noteText}
            autoSaveStatus={autoSaveStatus}
            savingNote={savingNote}
            scales={scales}
            ventilation={ventilation}
            labs={labs}
            patientState={patientState}
            loadingSidebar={loadingSidebar}
            balanceItems={balanceItems}
            totalIntake={totalIntake}
            totalOutput={totalOutput}
            dailyBalance={dailyBalance}
            cumulativeBalance={cumulativeBalance}
            keyScales={keyScales}
            canEditSidebar={canEditSidebar}
            onNoteChange={handleNoteChange}
            onSaveNote={saveNow}
            onCreateLab={createLab}
            onCreateVentilation={createVentilation}
            onCreatePatientState={createPatientState}
            availableScales={availableScales}
            onCreateScale={createScale}
            onCalculateScale={calculateScale}
            episodeId={episode.id}
          />
        </div>
      </div>
    </SidebarProvider>
    <HourlyGridDialog
      open={gridExpanded}
      onOpenChange={setGridExpanded}
      episode={episode}
      selectedDay={selectedDay}
      isLocked={isLocked}
      saveStatus={autoSaveStatus}
      onRefresh={onRefresh}
      feedback={gridFeedback}
      finalFocusRef={expandTriggerRef}
      conflict={conflict}
      onResolveConflict={resolveConflict}
      loading={loading}
      recByHour={recByHour}
      undoCount={undoStack.length}
      onUndo={undoLastChange}
      undoToast={undoToast}
      onUndoExecution={restoreExecution}
    >
      {selectedDay && (
        <HourlyGrid {...gridProps} sticky={!isMobile} bare />
      )}
      </HourlyGridDialog>
    </>
  );
}
