import { useEffect, useMemo, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, Loader2, Plus, Minus, Trash2, X, Undo2, Eraser } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import type { PrescriptionDayPart, PrescriptionInteractionsResponse, PairInteractionWarning as PairInteraction } from '../../types/medication';
import type { GridItem } from './PrescriptionGrid';
import { gridDateMeta, shouldMarkAddedCell } from './prescriptionDateMeta';

const SEVERITY_ORDER: Record<string, number> = { critical: 3, high: 2, medium: 1 };
const SEVERITY_LABELS: Record<string, string> = { critical: 'критично', high: 'високо', medium: 'помірно' };

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

// Inclusive day list of an overlap period (capped for safety, ISO dates).
function rangeDays(start: string, end: string): string[] {
  const out: string[] = [];
  const d = new Date(start);
  const endD = new Date(end);
  for (let i = 0; i < 62 && d <= endD; i++) {
    out.push(d.toISOString().slice(0, 10));
    d.setDate(d.getDate() + 1);
  }
  return out;
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

// «−» removes the LAST non-deleted day of the row: the dayId behind the
// maximum dayDate among the item's cells. Computed from domain data at click
// time (never a hardcoded index); ties resolve to the first-seen dayId.
function lastDayIdOf(item: GridItem): string | undefined {
  let best: { dayDate: string; dayId: string } | undefined;
  item.cells.forEach((dp) => {
    if (!dp.dayDate || !dp.dayId) return;
    if (!best || dp.dayDate > best.dayDate) best = { dayDate: dp.dayDate, dayId: dp.dayId };
  });
  return best?.dayId;
}

function dayCountOf(item: GridItem): number {
  const dates = new Set<string>();
  item.cells.forEach((dp) => {
    if (dp.dayDate) dates.add(dp.dayDate);
  });
  return dates.size;
}

export interface DayContextMenuState {
  clientX: number;
  clientY: number;
  cellLabel: string;
  cancelEnabled: boolean;
  restoreEnabled: boolean;
  cancelAssignmentEnabled: boolean;
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
  onCancelMedication: (dayPartId: string) => Promise<void>;
  onRestoreToPlanned: (dayPartId: string) => Promise<void>;
  onCancelAssignment: (dayPartId: string) => Promise<void>;
  onOpenExecute: (dp: PrescriptionDayPart, el: HTMLElement) => void;
  onOpenDeleteConfirm: (itemId: string, el: HTMLElement) => void;
  /** Server-computed interaction warnings (#304); null = not fetched. */
  interactions?: PrescriptionInteractionsResponse | null;
}

export default function PrescriptionSpreadsheet({
  canEdit, isDoctor, isNurse, gridItems, visibleDates, allDates, viewStart, daysToShow,
  loading, onShiftLeft, onShiftRight, onAddDay, onRemoveDay, onPlan, onCancelMedication, onRestoreToPlanned, onCancelAssignment, onOpenExecute, onOpenDeleteConfirm,
  interactions,
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

  // Interaction warnings (#304): per-item pairs (from both sides), the ids of
  // warned items, and the set of warned (item, date) cells with a red border.
  const itemPairs = useMemo(() => {
    const map = new Map<string, PairInteraction[]>();
    if (interactions) {
      for (const w of interactions.warnings) {
        for (const p of w.interactions) {
          if (!map.has(w.itemId)) map.set(w.itemId, []);
          if (!map.has(p.otherItemId)) map.set(p.otherItemId, []);
          map.get(w.itemId)!.push(p);
          map.get(p.otherItemId)!.push({ ...p, otherItemId: w.itemId, otherNameUk: w.nameUk });
        }
      }
    }
    return map;
  }, [interactions]);
  const warnedItemIds = useMemo(() => new Set(itemPairs.keys()), [itemPairs]);
  const warnedDates = useMemo(() => {
    const map = new Map<string, Set<string>>();
    if (interactions) {
      for (const w of interactions.warnings) {
        for (const p of w.interactions) {
          const days = rangeDays(p.overlapStart, p.overlapEnd);
          if (!map.has(w.itemId)) map.set(w.itemId, new Set());
          if (!map.has(p.otherItemId)) map.set(p.otherItemId, new Set());
          for (const d of days) {
            map.get(w.itemId)!.add(d);
            map.get(p.otherItemId)!.add(d);
          }
        }
      }
    }
    return map;
  }, [interactions]);

  // Derived added/removed-day metadata (no persistence): per-item added dates
  // beyond the 21-day base window, union header flags, and gap edge markers.
  const dateMeta = useMemo(() => gridDateMeta(
    gridItems.map((g) => ({
      id: g.id,
      medicineName: g.medicineName,
      dayDates: (g.dayParts ?? []).map((dp) => dp.dayDate).filter((d): d is string => Boolean(d)),
    })),
  ), [gridItems]);

  const openDayMenu = (e: React.MouseEvent, date: string, dp: PrescriptionDayPart) => {
    e.preventDefault();
    if (!canMenu) return;
    const cancelEnabled = Boolean(dp.isPlanned && !dp.isPlannedFinished && !dp.isCompleted && !dp.isCompletedFinished);
    const restoreEnabled = Boolean(dp.isPlannedFinished && !dp.isCompleted && !dp.isCompletedFinished);
    const cancelAssignmentEnabled = Boolean((dp.isPlanned || dp.isPlannedFinished) && !dp.isCompleted && !dp.isCompletedFinished);
    setDayMenu({
      clientX: e.clientX,
      clientY: e.clientY,
      cellLabel: `${formatDate(date)} · ${PERIOD_FULL[dp.period] ?? dp.period}`,
      cancelEnabled,
      restoreEnabled,
      cancelAssignmentEnabled,
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

  const handleCancelFromMenu = async () => {
    const dp = dayMenu?.cancelDayPart;
    if (dp) await onCancelMedication(dp.id);
    closeDayMenu();
  };

  const handleRestoreFromMenu = async () => {
    const dp = dayMenu?.cancelDayPart;
    if (dp) await onRestoreToPlanned(dp.id);
    closeDayMenu();
  };

  const handleCancelAssignmentFromMenu = async () => {
    const dp = dayMenu?.cancelDayPart;
    if (dp) await onCancelAssignment(dp.id);
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
          <div className="flex items-center gap-0.5">
            <div className="size-3.5 rounded-sm bg-muted" style={{ border: '1px solid #ccc' }} title="Дні, додані кнопкою «+» понад базовий 21-денний курс" />
            <span className="text-[10px]">Доданий день</span>
          </div>
          <div className="flex items-center gap-0.5">
            <div className="h-3.5 w-1 rounded-sm" style={{ backgroundColor: '#94a3b8' }} title="Межа пропущеного (видаленого) дня" />
            <span className="text-[10px]">Пропущений день</span>
          </div>
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
        <div className="relative rounded-xl border border-border bg-card text-card-foreground shadow-sm overflow-auto">
          {/* Tablet scroll affordance: right-edge gradient fade */}
          <div aria-hidden className="pointer-events-none sticky right-0 top-0 z-[5] float-right h-full w-8 bg-gradient-to-l from-border/40 to-transparent" />
          <table className="w-full border-collapse md:table-fixed" style={{ minWidth: 200 + visibleDates.length * 300 }}>
            <thead>
              <tr>
                <th
                  className="sticky left-0 bg-card z-20 min-w-[180px] md:min-w-[140px] p-1.5 md:p-1 border border-border text-left shadow-[2px_0_4px_rgba(0,0,0,0.05)]"
                  style={{ borderRightWidth: 2, borderRightColor: '#94a3b8' }}
                >
                  <span className="text-[10px] font-bold">Препарат / Метод</span>
                </th>
                {visibleDates.map((date, dateIdx) => {
                  const addedNames = dateMeta.headerAdded.get(date);
                  const gapAfter = dateMeta.headerGapAfter.has(date);
                  return (
                    <th
                      key={date}
                      colSpan={4}
                      className={`p-1 border border-border text-center ${addedNames ? 'bg-accent' : 'bg-muted'}`}
                      data-added-day={addedNames ? 'true' : undefined}
                      data-removed-gap={gapAfter ? 'true' : undefined}
                      title={
                        addedNames
                          ? `Доданий день для: ${addedNames.join(', ')}`
                          : gapAfter
                            ? `Пропущений день перед: ${formatDate(date)}`
                            : undefined
                      }
                      style={{
                        ...(dateIdx < visibleDates.length - 1
                          ? { borderRightWidth: 2, borderRightColor: '#94a3b8' }
                          : null),
                        ...(gapAfter ? { borderLeftWidth: 2, borderLeftColor: '#94a3b8' } : null),
                      }}
                    >
                      <span className="text-[10px] font-bold">
                        {formatDate(date)}
                      </span>
                    </th>
                  );
                })}
                {canEdit && isDoctor && <th className="w-16 border border-border" />}
              </tr>
              <tr>
                <th
                  className="sticky left-0 bg-card z-20 border border-border md:min-w-[140px] shadow-[2px_0_4px_rgba(0,0,0,0.05)]"
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
                    className="sticky left-0 bg-card z-10 p-1 min-w-[180px] md:min-w-[140px] border border-border shadow-[2px_0_4px_rgba(0,0,0,0.05)]"
                    style={{ borderRightWidth: 2, borderRightColor: '#94a3b8' }}
                  >
                    <div className="flex items-start gap-0.5">
                      {canEdit && isDoctor && (
                        <Button
                          variant="ghost"
                          size="icon-xs"
                          aria-label="Видалити препарат"
                          title="Видалити препарат"
                          onClick={(e) => onOpenDeleteConfirm(item.id, e.currentTarget as HTMLElement)}
                        >
                          <Trash2 className="size-3" />
                        </Button>
                      )}
                      <div className="flex-1 min-w-0">
                        <p className={warnedItemIds.has(item.id) ? 'text-sm font-semibold interaction-warn' : 'text-sm font-semibold'}>
                          {item.medicineName}
                        </p>
                        <p className="text-[10px] text-muted-foreground">
                          {item.medicineMethod || ''}{item.regime ? ` • ${item.regime}` : ''}
                        </p>
                      </div>
                    </div>
                  </td>

                  {visibleDates.map((date, dateIdx) =>
                    PERIODS.map(period => {
                      const dp = item.cells.get(dayPartKey(date, period));
                      const bg = cellBg(dp);
                      const label = cellLabel(dp);
                      const isEditing = editingCell === (dp?.id);
                      // Inactive (white) cells of added days get the muted marker;
                      // status colors always win.
                      const markedAdded = shouldMarkAddedCell(dateMeta.perItem.get(item.id), date, dp);
                      const isWarnedCell = !!(
                        dp && dp.isPlanned && !dp.isPlannedFinished && !dp.isCompleted && !dp.isCompletedFinished
                          && warnedDates.get(item.id)?.has(date)
                      );

                      const onClick = (e: React.MouseEvent) => {
                        if (!dp || !canEdit) return;
                        if (dp.isCompleted || dp.isCompletedFinished) return;
                        if (isDoctor) { startEdit(dp); return; }
                        if (isNurse && dp.isPlanned) { onOpenExecute(dp, e.currentTarget as HTMLElement); return; }
                      };

                      return (
                        <td
                          key={`${date}-${period}`}
                          className={markedAdded ? 'bg-muted' : undefined}
                          data-added-day={markedAdded ? 'true' : undefined}
                          data-interaction-warn={isWarnedCell ? 'true' : undefined}
                          style={{
                            width: 68, height: 32, cursor: bg === '#fff' || !dp ? 'default' : 'pointer',
                            backgroundColor: markedAdded ? undefined : bg, textAlign: 'center', verticalAlign: 'middle',
                            position: 'relative',
                            border: isWarnedCell ? '2px solid #EF4444' : '1px solid var(--color-border)',
                            ...(isWarnedCell ? { borderRightColor: '#EF4444', borderBottomColor: '#EF4444', borderLeftColor: '#EF4444', borderTopColor: '#EF4444' } : null),
                            ...(period === 'night' && dateIdx < visibleDates.length - 1
                              ? { borderRightWidth: 2, borderRightColor: '#94a3b8' }
                              : null),
                          }}
                          onClick={onClick}
                          onContextMenuCapture={dp ? (e) => openDayMenu(e, date, dp) : undefined}
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
                                    <p>{`${PERIOD_FULL[dp.period]}: ${dp.dose ?? '—'}`}</p>
                                    {(() => {
                                      const pairs = (itemPairs.get(item.id) ?? [])
                                        .filter(p => rangeDays(p.overlapStart, p.overlapEnd).includes(date))
                                        .sort((x, y) => (SEVERITY_ORDER[y.severity] ?? 0) - (SEVERITY_ORDER[x.severity] ?? 0));
                                      if (pairs.length === 0) return null;
                                      return (
                                        <div className="mt-1 border-t border-current/20 pt-1">
                                          <p className="text-[10px] font-bold">Взаємодії: {item.medicineName}</p>
                                          {pairs.map((p, i) => (
                                            <p key={i} className="text-[10px]">
                                              ⚠ {p.otherNameUk} ({SEVERITY_LABELS[p.severity] ?? p.severity}) — {p.interactionText}
                                            </p>
                                          ))}
                                        </div>
                                      );
                                    })()}
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
                      <div className="flex items-center justify-center">
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
                        <Button
                          variant="ghost"
                          size="icon-xs"
                          aria-label="Видалити день"
                          title="Видалити день"
                          disabled={!onRemoveDay || dayCountOf(item) <= 1 || !lastDayIdOf(item)}
                          onClick={() => {
                            const dayId = lastDayIdOf(item);
                            if (dayId) onRemoveDay?.(item.id, dayId);
                          }}
                        >
                          <Minus className="size-3" />
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
                onClick={handleCancelFromMenu}
              >
                <X className="size-3.5 text-muted-foreground" />
                Відмінити препарат
              </button>
            )}
            {dayMenu.restoreEnabled && dayMenu.cancelDayPart && (
              <button
                type="button"
                role="menuitem"
                className="w-full flex items-center gap-1.5 px-2 py-1.5 text-left rounded-md hover:bg-muted"
                onClick={handleRestoreFromMenu}
              >
                <Undo2 className="size-3.5 text-muted-foreground" />
                Повернути у Заплановано
              </button>
            )}
            {dayMenu.cancelAssignmentEnabled && dayMenu.cancelDayPart && (
              <button
                type="button"
                role="menuitem"
                className="w-full flex items-center gap-1.5 px-2 py-1.5 text-left rounded-md hover:bg-muted text-destructive"
                onClick={handleCancelAssignmentFromMenu}
              >
                <Eraser className="size-3.5" />
                Відмінити це призначення
              </button>
            )}
          </div>
        </div>
      )}
    </>
  );
}
