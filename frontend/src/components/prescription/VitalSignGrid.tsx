import { useState, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import { ChevronLeft, ChevronRight, Loader2 } from 'lucide-react';
import type { VitalSignEntry } from '../../types/medication';

const PERIODS = ['morning', 'day', 'evening', 'night'] as const;
const PERIOD_LABELS: Record<string, string> = {
  morning: 'Р', day: 'Д', evening: 'В', night: 'Н',
};
const PERIOD_FULL: Record<string, string> = {
  morning: 'Ранок', day: 'День', evening: 'Вечір', night: 'Ніч',
};

function formatDate(iso: string) {
  const d = new Date(iso);
  return d.toLocaleDateString('uk-UA', { day: '2-digit', month: 'short' });
}

interface VitalParam {
  key: string;
  label: string;
  unit: string;
  numeric: boolean;
  min?: number;
  max?: number;
  step?: number;
}

const VITAL_PARAMS: VitalParam[] = [
  { key: 'temperature', label: 'Температура', unit: '°C', numeric: true, min: 34, max: 42, step: 0.1 },
  { key: 'systolicBp', label: 'АТ сист.', unit: 'мм рт.ст.', numeric: true, min: 50, max: 250, step: 1 },
  { key: 'diastolicBp', label: 'АТ діаст.', unit: 'мм рт.ст.', numeric: true, min: 30, max: 150, step: 1 },
  { key: 'spo2', label: 'SpO₂', unit: '%', numeric: true, min: 50, max: 100, step: 1 },
  { key: 'pulse', label: 'Пульс', unit: 'уд/хв', numeric: true, min: 0, max: 300, step: 1 },
  { key: 'painScore', label: 'Біль', unit: '0-10', numeric: true, min: 0, max: 10, step: 1 },
  { key: 'stool', label: 'Стул', unit: '', numeric: false },
];

interface VitalSignGridProps {
  days: { id: string; dayDate: string; entries: VitalSignEntry[] }[];
  canEdit: boolean;
  isDoctor: boolean;
  onCellUpdate: (dayId: string, period: string, paramKey: string, value: string) => Promise<void>;
  loading?: boolean;
}

function entryValue(entry: VitalSignEntry | undefined, key: string): string {
  if (!entry) return '';
  const v = (entry as unknown as Record<string, unknown>)[key];
  if (v === null || v === undefined) return '';
  return String(v);
}

function dayPartKey(dayId: string, period: string) { return `${dayId}|${period}`; }

export default function VitalSignGrid({
  days, canEdit, isDoctor, onCellUpdate, loading,
}: VitalSignGridProps) {
  const today = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const startIdx = Math.max(0, days.findIndex(d => d.dayDate >= today));
  const [viewStart, setViewStart] = useState(Math.max(0, startIdx));
  const daysToShow = 7;
  const visibleDays = days.slice(viewStart, viewStart + daysToShow);

  const shiftLeft = () => setViewStart(Math.max(0, viewStart - daysToShow));
  const shiftRight = () => {
    if (viewStart + daysToShow < days.length) setViewStart(viewStart + daysToShow);
  };

  const entriesByDayPeriod = useMemo(() => {
    const map = new Map<string, VitalSignEntry>();
    for (const day of days) {
      for (const entry of day.entries) {
        map.set(dayPartKey(day.id, entry.period), entry);
      }
    }
    return map;
  }, [days]);

  const [editingCell, setEditingCell] = useState<string | null>(null);
  const [editingValue, setEditingValue] = useState('');

  const startEdit = (dayId: string, period: string, paramKey: string, currentValue: string) => {
    if (!canEdit || !isDoctor) return;
    setEditingCell(`${dayId}|${period}|${paramKey}`);
    setEditingValue(currentValue);
  };

  const commitEdit = async (dayId: string, period: string, paramKey: string) => {
    const value = editingValue.trim();
    setEditingCell(null);
    setEditingValue('');
    if (!value) return;
    await onCellUpdate(dayId, period, paramKey, value);
  };

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2 rounded-xl border bg-card p-3 text-card-foreground shadow-sm">
        <Button variant="ghost" size="icon-sm" onClick={shiftLeft} disabled={viewStart === 0}>
          <ChevronLeft className="size-4" />
        </Button>
        <span className="min-w-[120px] text-center text-sm font-semibold font-rubik">
          {visibleDays.length > 0
            ? `${formatDate(visibleDays[0].dayDate)} — ${formatDate(visibleDays[visibleDays.length - 1].dayDate)}`
            : 'Немає даних'}
        </span>
        <Button variant="ghost" size="icon-sm" onClick={shiftRight} disabled={viewStart + daysToShow >= days.length}>
          <ChevronRight className="size-4" />
        </Button>
      </div>

      {loading ? (
        <Loader2 className="mx-auto mt-4 size-6 animate-spin text-primary" />
      ) : days.length === 0 ? (
        <div className="rounded-xl border bg-card p-4 text-center text-card-foreground shadow-sm">
          <p className="text-muted-foreground font-mulish">Немає даних життєвих показників</p>
        </div>
      ) : (
        <div className="overflow-auto rounded-xl border bg-card shadow-sm">
          <table className="border-collapse" style={{ minWidth: 200 + visibleDays.length * 300 }}>
            <thead>
              <tr>
                <th className="sticky left-0 z-[2] min-w-[180px] bg-card p-[6px_8px] text-left">
                  <span className="text-xs font-bold font-rubik">Показник</span>
                </th>
                {visibleDays.map(day => (
                  <th key={day.id} colSpan={4} className="bg-muted p-[4px_2px] text-center">
                    <span className="text-xs font-bold font-rubik">{formatDate(day.dayDate)}</span>
                  </th>
                ))}
              </tr>
              <tr>
                <th className="sticky left-0 z-[2] bg-card" />
                {visibleDays.map(day =>
                  PERIODS.map(p => (
                    <th key={`${day.id}-${p}`} className="w-[68px] p-[2px] text-[10px] text-muted-foreground font-mulish">
                      {PERIOD_LABELS[p]}
                    </th>
                  ))
                )}
              </tr>
            </thead>
            <tbody>
              {VITAL_PARAMS.map(param => (
                <tr key={param.key}>
                  <td className="sticky left-0 z-[1] min-w-[180px] bg-card p-[4px_8px]">
                    <span className="text-sm font-semibold font-rubik">{param.label}</span>
                    {param.unit && (
                      <span className="block text-xs text-muted-foreground font-mulish">{param.unit}</span>
                    )}
                  </td>

                  {visibleDays.map(day =>
                    PERIODS.map(period => {
                      const entry = entriesByDayPeriod.get(dayPartKey(day.id, period));
                      const value = entryValue(entry, param.key);
                      const cellKey = `${day.id}|${period}|${param.key}`;
                      const isEditing = editingCell === cellKey;

                      const onClick = () => {
                        if (!canEdit || !isDoctor) return;
                        startEdit(day.id, period, param.key, value);
                      };

                      return (
                        <td
                          key={cellKey}
                          className={cn(
                            'relative h-8 w-[68px] bg-white text-center align-middle',
                            canEdit && isDoctor ? 'cursor-pointer' : 'cursor-default',
                          )}
                          onClick={onClick}
                        >
                          {isEditing ? (
                            <form
                              onSubmit={e => { e.preventDefault(); commitEdit(day.id, period, param.key); }}
                              className="absolute inset-0 z-[3] flex"
                            >
                              <input
                                autoFocus
                                value={editingValue}
                                onChange={e => setEditingValue(e.target.value)}
                                onBlur={() => commitEdit(day.id, period, param.key)}
                                className="w-full border-2 border-[#1976d2] p-0 text-center text-[11px] outline-none"
                              />
                            </form>
                          ) : (
                            <Tooltip>
                              <TooltipTrigger>
                                <span
                                  className={cn(
                                    'inline-block leading-[32px] select-none text-[10px]',
                                    value ? 'font-semibold text-[#1565c0]' : 'text-muted-foreground',
                                  )}
                                >
                                  {value || ''}
                                </span>
                              </TooltipTrigger>
                              <TooltipContent side="top">
                                <span className="text-xs">
                                  {entry ? `${PERIOD_FULL[entry.period]}: ${value || '—'}` : '—'}
                                </span>
                              </TooltipContent>
                            </Tooltip>
                          )}
                        </td>
                      );
                    })
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
