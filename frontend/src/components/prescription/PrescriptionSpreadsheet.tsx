import { useEffect, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, Loader2, Plus, Trash2, X, CalendarDays } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import type { PrescriptionDayPart } from '../../types/medication';
import type { GridItem } from './PrescriptionGrid';

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

function cellBg(part: PrescriptionDayPart | undefined) {
  if (!part) return '#fff';
  if (part.isCompletedFinished) return '#C8E6C9';
  if (part.isCompleted) return '#C8E6C9';
  if (part.isPlannedFinished) return '#E1BEE7';
  if (part.isPlanned) return '#BBDEFB';
  return '#fff';
}

function cellLabel(part: PrescriptionDayPart | undefined) {
  if (!part) return '';
  if (part.isCompletedFinished) return '✓';
  if (part.isCompleted) return '✓';
  if (part.isPlannedFinished) return '✕';
  if (part.isPlanned) return part.dose ?? '+';
  return '';
}

function dayPartKey(date: string, period: string) { return `${date}|${period}`; }

export interface DayContextMenuState {
  clientX: number;
  clientY: number;
  itemId: string;
  dayId?: string;
  cellLabel: string;
  cancelEnabled: boolean;
  cancelDayPart: PrescriptionDayPart | null;
}

export interface PrescriptionSpreadsheetProps {
  canEdit: boolean;
  isDoctor: boolean;
  isNurse: boolean;
  gridItems: GridItem[];
  visibleDates: string[];
  allDates: string[];
  viewStart: number;
  daysToShow: number;
  loading?: boolean;
  onShiftLeft: () => void;
  onShiftRight: () => void;
  onAddDay?: (itemId: string) => Promise<void> | void;
  onRemoveDay?: (itemId: string, dayId: string) => Promise<void> | void;
  onPlan: (dayPartId: string, dose: string) => Promise<void>;
  onCancel: (dayPartId: string) => Promise<void>;
  onOpenExecute: (dp: PrescriptionDayPart, el: HTMLElement) => void;
  onOpenDeleteConfirm: (itemId: string, el: HTMLElement) => void;
}

export default function PrescriptionSpreadsheet({
  canEdit, isDoctor, isNurse, gridItems, visibleDates, allDates, viewStart, daysToShow,
  loading, onShiftLeft, onShiftRight, onAddDay, onRemoveDay, onPlan, onCancel, onOpenExecute, onOpenDeleteConfirm,
}: PrescriptionSpreadsheetProps) {
  const [editingCell, setEditingCell] = useState<string | null>(null);
  const [editingDose, setEditingDose] = useState('');
  const [dayMenu, setDayMenu] = useState<DayContextMenuState | null>(null);

  const startEdit = (dp: PrescriptionDayPart) => {
    if (!canEdit || !isDoctor) return;
    setEditingCell(dp.id);
    setEditingDose(dp.dose ?? '');
  };

  const commitEdit = async (dp: PrescriptionDayPart) => {
    const dose = editingDose.trim();
    setEditingCell(null);
    setEditingDose('');
    if (!dose || dose === (dp.dose ?? '')) return;
    await onPlan(dp.id, dose);
  };

  const canMenu = canEdit && isDoctor;

  const openDayMenu = (e: React.MouseEvent, item: GridItem, date: string, dp: PrescriptionDayPart) => {
    e.preventDefault();
    if (!canMenu) return;
    const cancelEnabled = Boolean(dp.isPlanned && !dp.isCompleted);
    setDayMenu({
      clientX: e.clientX,
      clientY: e.clientY,
      itemId: item.id,
      dayId: dp.dayId,
      cellLabel: `${formatDate(date)} · ${PERIOD_FULL[dp.period] ?? dp.period}`,
      cancelEnabled,
      cancelDayPart: dp,
    });
  };

  const closeDayMenu = () => setDayMenu(null);

  const dayMenuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!dayMenu) return;
    const onDocPointerDown = (e: PointerEvent) => {
      if (dayMenuRef.current && !dayMenuRef.current.contains(e.target as Node)) closeDayMenu();
    };
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') closeDayMenu(); };
    const onScroll = () => closeDayMenu();
    document.addEventListener('pointerdown', onDocPointerDown);
    document.addEventListener('keydown', onKey);
    window.addEventListener('scroll', onScroll, true);
    return () => {
      document.removeEventListener('pointerdown', onDocPointerDown);
      document.removeEventListener('keydown', onKey);
      window.removeEventListener('scroll', onScroll, true);
    };
  }, [dayMenu]);

  const handlePlanFromMenu = async () => {
    const dp = dayMenu?.cancelDayPart;
    if (dp) await onCancel(dp.id);
    closeDayMenu();
  };

  const handleRemoveDayFromMenu = async () => {
    const itemId = dayMenu?.itemId;
    const dayId = dayMenu?.dayId;
    if (itemId && dayId) await onRemoveDay?.(itemId, dayId);
    closeDayMenu();
  };

  return (
    <>
      <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-1.5 flex items-center gap-2 flex-wrap">
        <Button variant="ghost" size="icon-xs" onClick={onShiftLeft} disabled={viewStart === 0}>
          <ChevronLeft className="size-3" />
        </Button>
        <p className="font-semibold text-sm min-w-[120px] text-center">
          {visibleDates.length > 0
            ? `${formatDate(visibleDates[0])} — ${formatDate(visibleDates[visibleDates.length - 1])}`
            : 'Немає даних'}
        </p>
        <Button variant="ghost" size="icon-xs" onClick={onShiftRight} disabled={viewStart + daysToShow >= allDates.length}>
          <ChevronRight className="size-3" />
        </Button>
        <div className="flex-1" />
        <div className="flex gap-1.5 flex-wrap">
          {[
            ['#BBDEFB', 'Заплановано'],
            ['#C8E6C9', 'Виконано'],
            ['#E1BEE7', 'Відмінено'],
          ].map(([color, label]) => (
            <div key={label} className="flex items-center gap-0.5">
              <div className="size-3.5 rounded-sm" style={{ backgroundColor: color, border: '1px solid #ccc' }} />
              <span className="text-[10px]">{label}</span>
            </div>
          ))}
        </div>
      </div>

      {loading ? (
        <Loader2 className="size-6 animate-spin text-primary mx-auto mt-4" />
      ) : gridItems.length === 0 ? (
        <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-4 text-center">
          <p className="text-muted-foreground">
            {isDoctor
              ? 'Немає препаратів. Додайте препарат щоб розпочати.'
              : 'Немає призначень для виконання.'}
          </p>
        </div>
      ) : (
        <div className="rounded-xl border border-border bg-card text-card-foreground shadow-sm overflow-auto">
          <table className="w-full border-collapse" style={{ minWidth: 200 + visibleDates.length * 300 }}>
            <thead>
              <tr>
                <th
                  className="sticky left-0 bg-card z-20 min-w-[180px] p-1.5 border border-border text-left"
                  style={{ borderRightWidth: 2, borderRightColor: '#94a3b8' }}
                >
                  <span className="text-[10px] font-bold">Препарат / Метод</span>
                </th>
                {visibleDates.map((date, dateIdx) => (
                  <th
                    key={date}
                    colSpan={4}
                    className="p-1 border border-border bg-muted text-center"
                    style={
                      dateIdx < visibleDates.length - 1
                        ? { borderRightWidth: 2, borderRightColor: '#94a3b8' }
                        : undefined
                    }
                  >
                    <span className="text-[10px] font-bold">
                      {formatDate(date)}
                    </span>
                  </th>
                ))}
                {canEdit && isDoctor && <th className="w-16 border border-border" />}
              </tr>
              <tr>
                <th
                  className="sticky left-0 bg-card z-20 border border-border"
                  style={{ borderRightWidth: 2, borderRightColor: '#94a3b8', borderBottomWidth: 2, borderBottomColor: '#94a3b8' }}
                />
                {visibleDates.map((date, dateIdx) =>
                  PERIODS.map(p => {
                    const dayEdge = p === 'night' && dateIdx < visibleDates.length - 1;
                    return (
                      <th
                        key={`${date}-${p}`}
                        className="w-[68px] text-[10px] text-muted-foreground p-0.5 border border-border"
                        style={{
                          borderBottomWidth: 2,
                          borderBottomColor: '#94a3b8',
                          ...(dayEdge ? { borderRightWidth: 2, borderRightColor: '#94a3b8' } : null),
                        }}
                      >
                        {PERIOD_LABELS[p]}
                      </th>
                    );
                  })
                )}
                {canEdit && isDoctor && (
                  <th
                    className="border border-border"
                    style={{ borderBottomWidth: 2, borderBottomColor: '#94a3b8' }}
                  />
                )}
              </tr>
            </thead>
            <tbody>
              {gridItems.map(item => (
                <tr key={item.id}>
                  <td
                    className="sticky left-0 bg-card z-10 p-1 min-w-[180px] border border-border"
                    style={{ borderRightWidth: 2, borderRightColor: '#94a3b8' }}
                  >
                    <p className="text-sm font-semibold">
                      {item.medicineName}
                    </p>
                    <p className="text-[10px] text-muted-foreground">
                      {item.medicineMethod || ''}{item.regime ? ` • ${item.regime}` : ''}
                    </p>
                  </td>

                  {visibleDates.map((date, dateIdx) =>
                    PERIODS.map(period => {
                      const dp = item.cells.get(dayPartKey(date, period));
                      const bg = cellBg(dp);
                      const label = cellLabel(dp);
                      const isEditing = editingCell === (dp?.id);

                      const onClick = (e: React.MouseEvent) => {
                        if (!dp || !canEdit) return;
                        if (dp.isCompleted || dp.isCompletedFinished) return;
                        if (isDoctor) { startEdit(dp); return; }
                        if (isNurse && dp.isPlanned) { onOpenExecute(dp, e.currentTarget as HTMLElement); return; }
                      };

                      return (
                        <td
                          key={`${date}-${period}`}
                          style={{
                            width: 68, height: 32, cursor: bg === '#fff' || !dp ? 'default' : 'pointer',
                            backgroundColor: bg, textAlign: 'center', verticalAlign: 'middle',
                            position: 'relative',
                            border: '1px solid var(--color-border)',
                            ...(period === 'night' && dateIdx < visibleDates.length - 1
                              ? { borderRightWidth: 2, borderRightColor: '#94a3b8' }
                              : null),
                          }}
                          onClick={onClick}
                          onContextMenu={dp ? (e) => openDayMenu(e, item, date, dp) : undefined}
                        >
                          {isEditing ? (
                            <form onSubmit={e => { e.preventDefault(); if (dp) commitEdit(dp); }}
                              className="absolute inset-0 z-30 flex">
                              <input autoFocus value={editingDose}
                                onChange={e => setEditingDose(e.target.value)}
                                onBlur={() => dp && commitEdit(dp)}
                                style={{
                                  width: '100%', border: '2px solid #1976d2',
                                  textAlign: 'center', fontSize: 11, padding: 0, outline: 'none',
                                }} />
                            </form>
                          ) : (
                            <TooltipProvider>
                              <Tooltip>
                                <TooltipTrigger>
                                  <span className="text-[10px] leading-[32px] select-none"
                                    style={{
                                      color: dp?.isPlanned ? '#1565c0' : dp?.isCompleted ? '#2e7d32' : undefined,
                                      fontWeight: dp?.isPlanned || dp?.isCompleted ? 600 : 400,
                                    }}>
                                    {label}
                                  </span>
                                </TooltipTrigger>
                                {dp && (
                                  <TooltipContent>
                                    {`${PERIOD_FULL[dp.period]}: ${dp.dose ?? '—'}`}
                                  </TooltipContent>
                                )}
                              </Tooltip>
                            </TooltipProvider>
                          )}
                        </td>
                      );
                    })
                  )}

                  {canEdit && isDoctor && (
                    <td className="w-16 text-center border border-border">
                      <div className="flex items-center justify-center gap-0.5">
                        <Button
                          variant="ghost"
                          size="icon-xs"
                          aria-label="Додати день"
                          title="Додати день"
                          disabled={!onAddDay}
                          onClick={() => onAddDay?.(item.id)}
                        >
                          <Plus className="size-3" />
                        </Button>
                        <Button variant="ghost" size="icon-xs" onClick={(e) => onOpenDeleteConfirm(item.id, e.currentTarget as HTMLElement)}>
                          <Trash2 className="size-3" />
                        </Button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {dayMenu && (
        <div
          ref={dayMenuRef}
          className="fixed z-50"
          style={{ top: dayMenu.clientY, left: dayMenu.clientX }}
          role="menu"
          aria-label="Контекстне меню дня"
        >
          <div className="rounded-xl border bg-popover text-popover-foreground shadow-md p-1 min-w-[200px] text-sm">
            <p className="px-2 py-1 text-[10px] font-bold text-muted-foreground uppercase">
              {dayMenu.cellLabel}
            </p>
            {dayMenu.cancelEnabled && dayMenu.cancelDayPart && (
              <button
                type="button"
                role="menuitem"
                className="w-full flex items-center gap-1.5 px-2 py-1.5 text-left rounded-md hover:bg-muted"
                onClick={handlePlanFromMenu}
              >
                <X className="size-3.5 text-muted-foreground" />
                Скасувати дозу
              </button>
            )}
            {dayMenu.dayId && onRemoveDay && (
              <button
                type="button"
                role="menuitem"
                className="w-full flex items-center gap-1.5 px-2 py-1.5 text-left rounded-md hover:bg-muted text-destructive"
                onClick={handleRemoveDayFromMenu}
              >
                <CalendarDays className="size-3.5" />
                Видалити цей день
              </button>
            )}
          </div>
        </div>
      )}
    </>
  );
}
