import { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { cn } from '@/lib/utils';
import { SidebarProvider } from '../ui/Sidebar';
import { useAutoSave } from '../../hooks/useAutoSave';
import { hourlyRecordApi, orderExecutionApi, medicalNoteApi, clinicalScaleApi, ventilationApi, labResultApi, patientStateApi } from '../../api/endpoints';
import HourlyGrid from './HourlyGrid';
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
}

export default function IntensiveCareCard({
  episode, selectedDay, records, orders, balanceItems, isNurse, isLocked, user, onRefresh, onFeedback,
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

  const notifyParentRef = useRef(onFeedback ?? (() => {}));
  notifyParentRef.current = onFeedback ?? (() => {});

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

  const saveCell = useCallback(async (hour: number, key: keyof HourlyRecord, raw: string) => {
    if (!selectedDay || isLocked) return;
    const textKeys: (keyof HourlyRecord)[] = ['consciousness', 'stool', 'vomit', 'bedPosition', 'headEnd'];
    const numeric = !textKeys.includes(key);
    const value = raw.trim() === '' ? null : numeric ? Number(raw) : raw;
    const existing: { id: string; version: number } | undefined = recByHour.get(hour) || localRecordMap.current.get(hour);
    const recTime = `${new Date().toISOString().split('T')[0]}T${String(hour).padStart(2, '0')}:00:00`;
    try {
      if (existing) {
        const patch: Partial<HourlyRecordCreateRequest> & { version: number } = { version: existing.version };
        if (value !== null) { (patch as Record<string, unknown>)[key] = value; }
        await hourlyRecordApi.update(existing.id, patch);
      } else if (value !== null) {
        const res = await hourlyRecordApi.create(selectedDay.id, { recordTime: recTime, [key]: value } as HourlyRecordCreateRequest);
        localRecordMap.current.set(hour, { id: res.data.id, version: res.data.version });
      }
      onRefresh?.();
    } catch (err) {
      notifyParentRef.current(getErrorMessage(err, 'Не вдалося зберегти показник'), 'error');
    }
  }, [selectedDay, isLocked, recByHour, onRefresh]);

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
  ) => {
    try {
      setExecuting(key);
      await action();
      await refreshExecutions([orderId]);
      onRefresh?.();
    } catch (err) {
      notifyParentRef.current(getErrorMessage(err, failMessage), 'error');
    } finally {
      setExecuting(null);
    }
  }, [refreshExecutions, onRefresh]);

  const handlePlanOrder = useCallback((orderId: string, hour: number, dose: string) =>
    runExecutionAction(`${orderId}-${hour}`, orderId,
      () => orderExecutionApi.plan(orderId, { hour, dose }), 'Не вдалося запланувати виконання'),
  [runExecutionAction]);

  const handleCancelOrder = useCallback((orderId: string, hour: number) =>
    runExecutionAction(`${orderId}-${hour}`, orderId,
      () => orderExecutionApi.cancel(orderId, { hour }), 'Не вдалося скасувати виконання'),
  [runExecutionAction]);

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

  return (
    <SidebarProvider defaultWidth={300} minWidth={200} maxWidth={600}>
      <div className={cn('flex items-start relative', isMobile ? 'flex-col' : 'flex-row')}>
        <HourlyGrid
          isMobile={isMobile}
          isNurse={isNurse}
          isLocked={isLocked}
          user={user}
          selectedDay={selectedDay}
          recByHour={recByHour}
          orders={orders}
          activeOrders={activeOrders}
          executionsByOrder={executionsByOrder}
          executing={executing}
          orderFormOpen={orderFormOpen}
          realClockHour={realClockHour}
          canEditSidebar={canEditSidebar}
          onSetOrderFormOpen={setOrderFormOpen}
          onSaveCell={saveCell}
          onPlanOrder={handlePlanOrder}
          onCancelOrder={handleCancelOrder}
          onExecuteOrder={handleExecuteOrder}
          onExecuteFinishOrder={handleExecuteFinishOrder}
          onRefresh={onRefresh}
          onError={(msg) => notifyParentRef.current(msg, 'error')}
        />
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
    </SidebarProvider>
  );
}
