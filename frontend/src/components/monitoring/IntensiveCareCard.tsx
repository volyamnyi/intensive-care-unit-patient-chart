import React, { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import {
  Box, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TextField, Tooltip, useTheme, CircularProgress, useMediaQuery,
  List, ListItem, ListItemText, Button, Stack, MenuItem, Input,
} from '@mui/material';
import {
  Person, NoteAdd, ScaleOutlined,
} from '@mui/icons-material';
import type {
  Episode, ClinicalDay, HourlyRecord, MedicalOrder, FluidBalanceItem,
  HourlyRecordCreateRequest, MedicalNoteCreateRequest,
  LabResult, LabResultCreateRequest, VentilationSettings, VentilationCreateRequest,
  PatientStateAssessment, PatientStateCreateRequest,
} from '../../types';
import {
  hourlyRecordApi, orderExecutionApi, medicalNoteApi, clinicalScaleApi,
  ventilationApi, labResultApi, patientStateApi, medicalOrderApi,
} from '../../api/endpoints';
import LabResultsPanel from '../common/LabResultsPanel';
import VentilationPanel from '../common/VentilationPanel';
import PatientStatePanel from '../common/PatientStatePanel';
import { useAutoSave } from '../../hooks/useAutoSave';
import {
  SidebarProvider, Sidebar, SidebarRail, SidebarHeader, SidebarContent, SidebarGroup,
} from '../ui/Sidebar';

interface UserLike {
  id: number;
}

const HOURS = Array.from({ length: 24 }, (_, i) => (i + 8) % 24);

function medDayPos(h: number): number { return h < 8 ? h + 24 : h; }
function isPastMedDay(h: number, clockHour: number): boolean { return medDayPos(h) < medDayPos(clockHour); }

const VITAL_ROWS: { key: keyof HourlyRecord; label: string; numeric: boolean }[] = [
  { key: 'systolicBP', label: 'АТсист', numeric: true },
  { key: 'diastolicBP', label: 'АТдіас', numeric: true },
  { key: 'heartRate', label: 'ЧСС', numeric: true },
  { key: 'spo2', label: 'SpO2', numeric: true },
  { key: 'temperature', label: 'Темп', numeric: true },
  { key: 'cvp', label: 'ЦВТ', numeric: true },
  { key: 'respiratoryRate', label: 'ЧД', numeric: true },
  { key: 'consciousness', label: 'Свідомість', numeric: false },
];

const LOSS_ROWS: { key: keyof HourlyRecord; label: string }[] = [
  { key: 'urineOutput', label: 'Сеча' },
  { key: 'drainOutput', label: 'Дренаж' },
  { key: 'stool', label: 'Випорожнення' },
  { key: 'vomit', label: 'Блювота' },
];



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

const CRITICAL_RANGES: Partial<Record<string, { min: number; max: number }>> = {
  systolicBP: { min: 90, max: 180 },
  diastolicBP: { min: 60, max: 120 },
  heartRate: { min: 50, max: 130 },
  temperature: { min: 35.5, max: 39.5 },
  spo2: { min: 90, max: 100 },
  respiratoryRate: { min: 10, max: 30 },
  cvp: { min: 2, max: 14 },
};

function isCritical(key: string, val: string): boolean {
  const range = CRITICAL_RANGES[key];
  if (!range) return false;
  const num = Number(val);
  if (Number.isNaN(num)) return false;
  return num < range.min || num > range.max;
}

const Cell = React.memo(function Cell({
  hour, rowKey, numeric, label, value, isLocked, isNurse, isLossRow, isDark, isPast, onSave,
}: CellProps) {
  const [draft, setDraft] = useState(value);
  const focusedRef = useRef(false);
  const valueRef = useRef(value);
  valueRef.current = value;

  // Don't sync value→draft while focused (prevents draft loss on concurrent save + refresh)
  useEffect(() => {
    if (!focusedRef.current) {
      setDraft(value);
    }
  }, [value]);

  const readOnly = isLocked || (isNurse && !isLossRow);
  const critical = isCritical(String(rowKey), value);
  const bg = critical ? (isDark ? '#3A1A1A' : '#FFE0E0') : (isPast ? (isDark ? '#16241C' : '#F1F8F3') : 'inherit');

  return (
    <TableCell sx={{ p: 0, bgcolor: bg, minWidth: 44 }}>
      <TextField
        fullWidth
        size="small"
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
        sx={{
          width: '100%',
          '& fieldset': { border: critical ? '1px solid #FF5252' : 'none' },
          '& input': { padding: '4px 6px', fontSize: 12, textAlign: 'center', fontWeight: critical ? 700 : 400 },
          '& input:disabled': { color: value ? (critical ? '#D32F2F' : 'inherit') : '#B0B0B0', WebkitTextFillColor: value ? (critical ? '#D32F2F' : 'inherit') : '#B0B0B0' },
        }}
      />
    </TableCell>
  );
});

interface TherapyCellProps {
  order: MedicalOrder;
  hour: number;
  isDark: boolean;
  isPast: boolean;
  canExecute: boolean;
  isExecuting: boolean;
  onToggle: (orderId: string, hour: number, actualDose: string) => void;
}

const TherapyCell = React.memo(function TherapyCell({
  order, hour, isDark, isPast, canExecute, isExecuting, onToggle,
}: TherapyCellProps) {
  const bg = isPast ? (isDark ? '#16241C' : '#F1F8F3') : 'inherit';
  const [editing, setEditing] = useState(false);
  const [doseInput, setDoseInput] = useState('');

  const handleClick = useCallback(() => {
    if (!canExecute || isPast || isExecuting) return;
    setDoseInput(order.dose || '');
    setEditing(true);
  }, [canExecute, isPast, isExecuting, order.dose]);

  const handleConfirm = useCallback(() => {
    if (!doseInput.trim()) return;
    onToggle(order.id, hour, doseInput);
    setEditing(false);
  }, [onToggle, order.id, hour, doseInput]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleConfirm();
    if (e.key === 'Escape') setEditing(false);
  }, [handleConfirm]);

  if (editing) {
    return (
      <TableCell sx={{ p: 0, minWidth: 44, textAlign: 'center', bgcolor: isDark ? '#1A1A1A' : '#FFFDE7' }}>
        <Input
          autoFocus
          value={doseInput}
          onChange={(e) => setDoseInput(e.target.value)}
          onKeyDown={handleKeyDown}
          onBlur={handleConfirm}
          sx={{ width: 36, fontSize: 11, textAlign: 'center', '& input': { textAlign: 'center', p: '4px 2px' } }}
        />
      </TableCell>
    );
  }

  return (
    <TableCell
      onClick={handleClick}
      sx={{ p: '4px 6px', bgcolor: bg, minWidth: 44, textAlign: 'center', cursor: canExecute && !isPast ? 'pointer' : 'default' }}
    >
      {isExecuting ? <CircularProgress size={12} /> : (
        <Tooltip title={`${order.drugName}${isPast ? ' (виконано)' : ''}`}>
          <Box component="span" sx={{ fontSize: 13, color: isPast ? '#4CAF50' : 'inherit' }}>
            {isPast ? '\u2713' : '\u279A'}
          </Box>
        </Tooltip>
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
    <Box sx={{ p: 1.5, border: '1px solid', borderColor: 'divider', borderRadius: 2, bgcolor: 'background.paper', mt: 1 }}>
      <Typography sx={{ fontWeight: 700, fontSize: 12, mb: 1 }}>Нове призначення</Typography>
      <Stack spacing={1}>
        <TextField fullWidth size="small" label="Препарат" value={form.drugName} onChange={(e) => setForm({ ...form, drugName: e.target.value })} />
        <Stack direction="row" spacing={1}>
          <TextField fullWidth size="small" label="Доза" value={form.dose} onChange={(e) => setForm({ ...form, dose: e.target.value })} />
          <TextField fullWidth size="small" label="Од." value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })} />
        </Stack>
        <Stack direction="row" spacing={1}>
          <TextField fullWidth size="small" label="Шлях" value={form.route} onChange={(e) => setForm({ ...form, route: e.target.value })} />
          <TextField fullWidth size="small" label="Частота" value={form.frequency} onChange={(e) => setForm({ ...form, frequency: e.target.value })} />
        </Stack>
        <TextField fullWidth size="small" label="Початок" type="datetime-local" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
        <TextField fullWidth size="small" label="Категорія" select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
          {['MEDICATION', 'INFUSION', 'LAB', 'MANIPULATION', 'VENTILATION', 'NUTRITION', 'OTHER'].map(c => (
            <MenuItem key={c} value={c}>{c}</MenuItem>
          ))}
        </TextField>
      </Stack>
      <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
        <Button variant="contained" onClick={handleCreate} disabled={saving || !form.drugName} size="small">
          {'Створити'}
        </Button>
        <Button onClick={onCancel} size="small">
          {'Скасувати'}
        </Button>
      </Stack>
      {form.startTime && !form.startTime.endsWith(':00') && (
        <Typography variant="caption" color="warning.main" sx={{ mt: 0.5, display: 'block' }}>
          {'Призначення почнеться з наступної години'}
        </Typography>
      )}
    </Box>
  );
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
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const bd = `1px solid ${isDark ? '#2A2A2A' : '#D0CEC9'}`;
  const [executing, setExecuting] = useState<string | null>(null);

  // Sidebar section data (self-fetched so the card is fully self-contained)
  const [notes, setNotes] = useState<{ id: string; text: string; authorId?: string | null; role?: string | null; createdAt?: string | null }[]>([]);
  const [noteText, setNoteText] = useState('');
  const noteTextRef = useRef('');
  const [savingNote, setSavingNote] = useState(false);
  const selectedDayRef = useRef(selectedDay);
  selectedDayRef.current = selectedDay;

  // Track IDs of records created locally (before refresh completes), preventing 409 race
  const localRecordMap = useRef<Map<number, { id: string; version: number }>>(new Map());
  useEffect(() => {
    localRecordMap.current.clear();
  }, [records]);

  const notifyParent = onFeedback ?? (() => {});

  useEffect(() => {
    setNoteText('');
    noteTextRef.current = '';
  }, [selectedDay?.id]);

  const saveCurrentNote = useCallback(async () => {
    const text = noteTextRef.current.trim();
    const day = selectedDayRef.current;
    if (!day || isLocked || !text) return;
    try {
      setSavingNote(true);
      await medicalNoteApi.create(day.id, {
        text,
        noteType: 'CLINICAL',
        role: isNurse ? 'NURSE' : 'DOCTOR',
      } as unknown as MedicalNoteCreateRequest);
      setNoteText('');
      noteTextRef.current = '';
      const refreshed = await medicalNoteApi.getByClinicalDay(day.id);
      setNotes(refreshed.data as unknown as { id: string; text: string; authorId?: string }[]);
    } catch (err) {
      notifyParent(getErrorMessage(err, 'Не вдалося зберегти нотатку'), 'error');
    } finally {
      setSavingNote(false);
    }
  }, [isLocked, isNurse, notifyParent]);

  const { status: autoSaveStatus, markDirty, saveNow } = useAutoSave({
    onSave: saveCurrentNote,
    delay: 2000,
    enabled: !!selectedDay && !isLocked,
  });

  const handleNoteChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const v = e.target.value;
    setNoteText(v);
    noteTextRef.current = v;
    markDirty();
  };
  const [scales, setScales] = useState<{ id: string; name?: string; result: string }[]>([]);
  const [ventilation, setVentilation] = useState<{ id: string; mode?: string; [k: string]: unknown }[]>([]);
  const [labs, setLabs] = useState<{ id: string; testName?: string; result?: string }[]>([]);
  const [patientState, setPatientState] = useState<{ id: string; assessment?: string }[]>([]);
  const [loadingSidebar, setLoadingSidebar] = useState(false);

  const canEditSidebar = !isLocked;

  const refreshSidebar = async () => {
    if (!selectedDay) return;
    try {
      const [n, s, v, l, p] = await Promise.all([
        medicalNoteApi.getByClinicalDay(selectedDay.id).then(r => r.data ?? []).catch(() => []),
        clinicalScaleApi.getResultsByClinicalDay(selectedDay.id).then(r => r.data ?? []).catch(() => []),
        ventilationApi.getByClinicalDay(selectedDay.id).then(r => r.data ?? []).catch(() => []),
        labResultApi.getByClinicalDay(selectedDay.id).then(r => r.data ?? []).catch(() => []),
        patientStateApi.getByClinicalDay(selectedDay.id).then(r => r.data ?? []).catch(() => []),
      ]);
      setNotes(n as unknown as { id: string; text: string; authorId?: string | null; role?: string | null; createdAt?: string | null }[]);
      setScales(s as unknown as { id: string; name?: string; result: string }[]);
      setVentilation(v as unknown as { id: string; mode?: string; [k: string]: unknown }[]);
      setLabs(l as unknown as { id: string; testName?: string; result?: string }[]);
      setPatientState(p as unknown as { id: string; assessment?: string }[]);
    } catch (err) {
      notifyParent(getErrorMessage(err, 'Не вдалося оновити бічну панель'), 'error');
    }
  };

  const createLab = async (data: LabResultCreateRequest) => {
    if (!selectedDay || isLocked) return;
    try {
      await labResultApi.create(selectedDay.id, data);
      await refreshSidebar();
    } catch (err) {
      notifyParent(getErrorMessage(err, 'Не вдалося створити лаб. результат'), 'error');
    }
  };
  const createVentilation = async (data: VentilationCreateRequest) => {
    if (!selectedDay || isLocked) return;
    try {
      await ventilationApi.create(selectedDay.id, data);
      await refreshSidebar();
    } catch (err) {
      notifyParent(getErrorMessage(err, 'Не вдалося додати ШВЛ'), 'error');
    }
  };
  const createPatientState = async (data: PatientStateCreateRequest) => {
    if (!selectedDay || isLocked) return;
    try {
      await patientStateApi.create(selectedDay.id, data);
      await refreshSidebar();
    } catch (err) {
      notifyParent(getErrorMessage(err, 'Не вдалося зберегти стан пацієнта'), 'error');
    }
  };

  const [orderFormOpen, setOrderFormOpen] = useState(false);

  useEffect(() => {
    if (!selectedDay) return;
    setLoadingSidebar(true);
    refreshSidebar().finally(() => setLoadingSidebar(false));
  }, [selectedDay]);

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
      const found = scales.find(s => s.name?.toLowerCase() === name.toLowerCase() || s.name?.includes(name));
      return found ? { name, result: found.result } : null;
    }).filter(Boolean) as { name: string; result: string }[];
  }, [scales]);

  const boundValue = (hour: number, key: keyof HourlyRecord): string => {
    const r = recByHour.get(hour);
    const v = r ? r[key] : null;
    if (v === null || v === undefined) return '';
    return String(v);
  };

  const saveCell = useCallback(async (hour: number, key: keyof HourlyRecord, raw: string) => {
    if (!selectedDay || isLocked) return;
    const numeric = key !== 'consciousness' && key !== 'stool' && key !== 'vomit';
    const value = raw.trim() === '' ? null : numeric ? Number(raw) : raw;
    const existing: { id: string; version: number } | undefined = recByHour.get(hour) || localRecordMap.current.get(hour);
    const recTime = `${new Date().toISOString().split('T')[0]}T${String(hour).padStart(2, '0')}:00:00`;
    try {
      if (existing) {
        const patch: Partial<HourlyRecordCreateRequest> & { version: number } = { version: existing.version };
        if (value !== null) {
          (patch as Record<string, unknown>)[key] = value;
        }
        await hourlyRecordApi.update(existing.id, patch);
      } else {
        if (value !== null) {
          const res = await hourlyRecordApi.create(selectedDay.id, {
            recordTime: recTime,
            [key]: value,
          } as HourlyRecordCreateRequest);
          localRecordMap.current.set(hour, { id: res.data.id, version: res.data.version });
        }
      }
      onRefresh?.();
    } catch (err) {
      notifyParent(getErrorMessage(err, 'Не вдалося зберегти показник'), 'error');
    }
  }, [selectedDay, isLocked, recByHour, onRefresh, notifyParent]);

  const activeOrders = orders.filter(o => o.status === 'ACTIVE' || o.status === 'DRAFT');

  const toggleOrder = useCallback(async (orderId: string, hour: number, actualDose: string) => {
    if (!user || !selectedDay) return;
    const execTime = `${new Date().toISOString().split('T')[0]}T${String(hour).padStart(2, '0')}:00:00`;
    try {
      setExecuting(`${orderId}-${hour}`);
      await orderExecutionApi.create(orderId, {
        executedBy: user.id,
        executedAt: execTime,
        actualDose,
      });
    } catch (err) {
      notifyParent(getErrorMessage(err, 'Не вдалося виконати призначення'), 'error');
    } finally {
      setExecuting(null);
    }
  }, [user, selectedDay, notifyParent]);

  const totalIntake = balanceItems.reduce((s, i) => s + (i.intake || 0), 0);
  const totalOutput = balanceItems.reduce((s, i) => s + (i.output || 0), 0);
  const dailyBalance = totalIntake - totalOutput;
  const cumulativeBalance = balanceItems[balanceItems.length - 1]?.cumulativeBalance ?? 0;

  const isMobile = useMediaQuery('(max-width:1200px)');

  const EmptyState = ({ icon, text }: { icon: React.ReactNode; text: string }) => (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 1.5, opacity: 0.6 }}>
      {icon}
      <Typography sx={{ fontSize: 11, color: 'text.secondary', mt: 0.5 }}>{text}</Typography>
    </Box>
  );

  return (
    <SidebarProvider defaultWidth={300} minWidth={200} maxWidth={600}>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', flexDirection: isMobile ? 'column' : 'row', position: 'relative' }}>
        {/* Left column: main table */}
        <Box component="main" sx={{ flex: 1, minWidth: 0, width: isMobile ? '100%' : 'auto' }}>
          <TableContainer sx={{ border: bd, borderRadius: 2, overflowX: 'auto', bgcolor: isDark ? '#141414' : '#fff' }}>
            <Table size="small" sx={{ tableLayout: 'fixed', minWidth: isMobile ? 900 : 1100 }}>
              <TableHead>
                <TableRow sx={{ bgcolor: isDark ? '#1A1A1A' : '#F4F2ED' }}>
                  <TableCell sx={{ fontWeight: 700, minWidth: 130, borderRight: bd }}>Показник / година</TableCell>
                  {HOURS.map((h) => (
                    <TableCell key={h} sx={{ textAlign: 'center', fontWeight: 700, fontSize: 11, p: '4px 2px', borderRight: h === 23 ? bd : 'none' }}>{h}:00</TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                <GroupHeader label="Показники" nurseEditable={!isNurse} />
                {VITAL_ROWS.map((row) => (
                  <TableRow key={String(row.key)}>
                    <TableCell sx={{ fontWeight: 600, fontSize: 11, borderRight: bd, whiteSpace: 'nowrap' }}>{row.label}</TableCell>
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
                        isDark={isDark}
                        isPast={isPastMedDay(h, realClockHour)}
                        onSave={saveCell}
                      />
                    ))}
                  </TableRow>
                ))}

                <GroupHeader label="Втрати (мл)" nurseEditable />
                {LOSS_ROWS.map((row) => (
                  <TableRow key={String(row.key)}>
                    <TableCell sx={{ fontWeight: 600, fontSize: 11, borderRight: bd }}>{row.label}</TableCell>
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
                        isDark={isDark}
                        isPast={isPastMedDay(h, realClockHour)}
                        onSave={saveCell}
                      />
                    ))}
                  </TableRow>
                ))}

                <TableRow sx={{ bgcolor: isDark ? '#202020' : '#EDEBE6' }}>
                  <TableCell colSpan={25} sx={{ fontWeight: 800, fontSize: 11, py: 0.5, borderRight: `1px solid ${isDark ? '#2A2A2A' : '#D0CEC9'}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>Терапія (призначення)</span>
                    {canEditSidebar && !isNurse && (
                      <Button size="small" variant="outlined" onClick={() => setOrderFormOpen(v => !v)} sx={{ fontSize: 10, py: 0, minHeight: 22 }}>
                        {orderFormOpen ? 'X Сховати' : '+ Нове призначення'}
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
                {orderFormOpen && (
                  <TableRow>
                    <TableCell colSpan={25} sx={{ p: 0, border: 'none' }}>
                      <OrderInlineForm
                        selectedDay={selectedDay}
                        isLocked={isLocked}
                        onCreated={onRefresh ?? (() => {})}
                        onCancel={() => setOrderFormOpen(false)}
                        onError={(msg) => notifyParent(msg, 'error')}
                      />
                    </TableCell>
                  </TableRow>
                )}
                {activeOrders.length === 0 && !orderFormOpen && (
                  <TableRow>
                    <TableCell colSpan={25} sx={{ textAlign: 'center', color: 'text.secondary', py: 1 }}>
                      {'Немає призначень'}
                    </TableCell>
                  </TableRow>
                )}
                {activeOrders.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell sx={{ fontSize: 10, borderRight: bd, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {order.drugName} {order.dose}{order.unit}{' '}
                      <Box component="span" sx={{ fontSize: 9, color: 'success.main', fontWeight: 700 }}>{order.status === 'ACTIVE' ? 'Активне' : order.status === 'DRAFT' ? 'Чернетка' : ''}</Box>
                    </TableCell>
                    {HOURS.map((h) => (
                      <TherapyCell
                        key={h}
                        order={order}
                        hour={h}
                        isDark={isDark}
                        isPast={isPastMedDay(h, realClockHour)}
                        canExecute={!isLocked && !!user}
                        isExecuting={executing === `${order.id}-${h}`}
                        onToggle={toggleOrder}
                      />
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>

        <Sidebar side="right" collapsible="none">
          <SidebarRail />
          <SidebarHeader>
            <Typography sx={{ fontWeight: 700, fontSize: 13, mb: 0.75, display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <Person sx={{ fontSize: 16 }} /> Пацієнт
            </Typography>
            <Typography sx={{ fontSize: 14, fontWeight: 600 }}>{episode.patientName || '\u2014'}</Typography>
            <Typography sx={{ fontSize: 12, color: 'text.secondary', mt: 0.5 }}>
              Діагноз: {episode.admissionDiagnosis || '\u2014'}
            </Typography>
            <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>
              {[episode.ward, episode.bedNumber].filter(Boolean).join(' / ') || '\u2014'}
              {episode.heightCm ? ` \u00B7 ${episode.heightCm} см` : ''}
            </Typography>
            {selectedDay?.weightKg && (
              <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>Вага: {selectedDay.weightKg} кг</Typography>
            )}
            {keyScales.length > 0 && (
              <Box sx={{ mt: 0.75, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {keyScales.map(s => (
                  <Box key={s.name} sx={{ fontSize: 11, bgcolor: isDark ? '#333' : '#E8E8E4', borderRadius: 1, px: 0.75, py: 0.25 }}>
                    <b>{s.name}</b>: {s.result}
                  </Box>
                ))}
              </Box>
            )}
          </SidebarHeader>
          <SidebarContent>
            <SidebarGroup label="Баланс рідини">
              <Box sx={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, py: 0.25 }}>
                <span>{'Надійшло:'}</span><b>{totalIntake} {'мл'}</b>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, py: 0.25 }}>
                <span>{'Виділено:'}</span><b>{totalOutput} {'мл'}</b>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, py: 0.25, borderTop: `1px solid ${isDark ? '#2A2A2A' : '#E0DED9'}`, mt: 0.25, pt: 0.5 }}>
                <span>{'Добовий баланс:'}</span>
                <Box component="b" sx={{ color: dailyBalance < 0 ? '#D32F2F' : '#2E7D32' }}>{dailyBalance >= 0 ? '+' : ''}{dailyBalance}</Box>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, py: 0.25 }}>
                <span>{'Кумулятивний баланс:'}</span>
                <Box component="b" sx={{ color: cumulativeBalance < 0 ? '#D32F2F' : '#2E7D32' }}>{cumulativeBalance >= 0 ? '+' : ''}{cumulativeBalance}</Box>
              </Box>
            </SidebarGroup>

            <SidebarGroup label="Нотатки" count={notes.length}>
              {notes.length === 0 ? (
                <EmptyState icon={<NoteAdd sx={{ fontSize: 24 }} />} text={'Немає нотаток. Додайте нову нотатку вище.'} />
              ) : (
                <List dense sx={{ py: 0 }}>
                  {notes.map((n) => (
                    <ListItem key={n.id} sx={{ px: 0, flexDirection: 'column', alignItems: 'flex-start' }}>
                      <ListItemText
                        primary={<Typography component="span" sx={{ fontSize: 12 }}>{n.text}</Typography>}
                        secondary={
                          <Typography component="span" sx={{ fontSize: 10, color: 'text.secondary' }}>
                            {[n.role, n.createdAt ? new Date(n.createdAt).toLocaleString('uk-UA') : null]
                              .filter(Boolean)
                              .join(' \u00B7 ')}
                          </Typography>
                        }
                      />
                    </ListItem>
                  ))}
                </List>
              )}
              {canEditSidebar && (
                <Stack spacing={0.5} sx={{ mt: 0.5 }}>
                  <TextField
                    size="small"
                    label={'Нова нотатка'}
                    value={noteText}
                    onChange={handleNoteChange}
                    multiline
                    minRows={2}
                    slotProps={{ input: { 'aria-label': 'Нова нотатка' } }}
                  />
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Button size="small" variant="outlined" onClick={saveNow} disabled={savingNote || !noteText.trim()}>
                      {'Додати нотатку'}
                    </Button>
                    {autoSaveStatus === 'saving' && (
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                        <CircularProgress size={10} />
                        <Typography variant="caption" color="text.secondary">{'Зберігається...'}</Typography>
                      </Box>
                    )}
                    {autoSaveStatus === 'saved' && (
                      <Typography variant="caption" color="success.main">{'Збережено'}</Typography>
                    )}
                    {autoSaveStatus === 'error' && (
                      <Typography variant="caption" color="error">{'Помилка'}</Typography>
                    )}
                  </Stack>
                </Stack>
              )}
            </SidebarGroup>

            <SidebarGroup label="Шкали" count={scales.length}>
              {scales.length === 0 ? (
                <EmptyState icon={<ScaleOutlined sx={{ fontSize: 24 }} />} text={'Немає даних шкал'} />
              ) : (
                <List dense sx={{ py: 0 }}>
                  {scales.map((s) => (
                    <ListItem key={s.id} sx={{ px: 0 }}>
                      <ListItemText primary={<Typography component="span" sx={{ fontSize: 12 }}>{`${s.name ?? ''}: ${s.result}`}</Typography>} />
                    </ListItem>
                  ))}
                </List>
              )}
            </SidebarGroup>

            <SidebarGroup label="ШВЛ" count={ventilation.length}>
              <VentilationPanel
                clinicalDayId={selectedDay?.id ?? ''}
                ventilation={ventilation as unknown as VentilationSettings[]}
                isLocked={isLocked}
                onCreate={createVentilation}
              />
            </SidebarGroup>

            <SidebarGroup label="Лабораторні результати" count={labs.length}>
              <LabResultsPanel
                clinicalDayId={selectedDay?.id ?? ''}
                labs={labs as unknown as LabResult[]}
                isLocked={isLocked}
                onCreate={createLab}
              />
            </SidebarGroup>

            <SidebarGroup label="Стан пацієнта" count={patientState.length}>
              <PatientStatePanel
                clinicalDayId={selectedDay?.id ?? ''}
                assessments={patientState as unknown as PatientStateAssessment[]}
                isLocked={isLocked}
                onCreate={createPatientState}
              />
            </SidebarGroup>

            {loadingSidebar && (
              <Box sx={{ display: 'flex', justifyContent: 'center' }}><CircularProgress size={16} /></Box>
            )}
          </SidebarContent>
        </Sidebar>
      </Box>
    </SidebarProvider>
  );
}

function GroupHeader({ label, nurseEditable }: { label: string; nurseEditable?: boolean }) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  return (
    <TableRow sx={{ bgcolor: isDark ? '#202020' : '#EDEBE6' }}>
      <TableCell colSpan={25} sx={{ fontWeight: 800, fontSize: 11, py: 0.5, borderRight: `1px solid ${isDark ? '#2A2A2A' : '#D0CEC9'}` }}>
        {label}
        {nurseEditable === false && (
          <Box component="span" sx={{ ml: 1, fontSize: 9, color: 'text.secondary' }}>(тільки лікар)</Box>
        )}
      </TableCell>
    </TableRow>
  );
}
