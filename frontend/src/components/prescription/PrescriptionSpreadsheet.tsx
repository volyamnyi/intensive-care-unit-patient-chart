import { useState } from 'react';
import { ChevronLeft, ChevronRight, Loader2, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import type { PrescriptionDayPart } from '../../types';
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
  onPlan: (dayPartId: string, dose: string) => Promise<void>;
  onCancel: (dayPartId: string) => Promise<void>;
  onOpenExecute: (dp: PrescriptionDayPart, el: HTMLElement) => void;
  onOpenDeleteConfirm: (itemId: string, el: HTMLElement) => void;
}

export default function PrescriptionSpreadsheet({
  canEdit, isDoctor, isNurse, gridItems, visibleDates, allDates, viewStart, daysToShow,
  loading, onShiftLeft, onShiftRight, onPlan, onCancel, onOpenExecute, onOpenDeleteConfirm,
}: PrescriptionSpreadsheetProps) {
  const [editingCell, setEditingCell] = useState<string | null>(null);
  const [editingDose, setEditingDose] = useState('');

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

  const doctorCancel = async (dp: PrescriptionDayPart) => {
    if (!canEdit || !isDoctor || !dp.isPlanned || dp.isCompleted) return;
    await onCancel(dp.id);
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
        <div className="rounded-xl border bg-card text-card-foreground shadow-sm overflow-auto">
          <table className="w-full border-collapse" style={{ minWidth: 200 + visibleDates.length * 300 }}>
            <thead>
              <tr>
                <th className="sticky left-0 bg-card z-20 min-w-[180px] p-1.5 border-b border-r text-left">
                  <span className="text-[10px] font-bold">Препарат / Метод</span>
                </th>
                {visibleDates.map(date => (
                  <th key={date} colSpan={4} className="p-1 border-b bg-muted text-center">
                    <span className="text-[10px] font-bold">
                      {formatDate(date)}
                    </span>
                  </th>
                ))}
                {canEdit && isDoctor && <th className="w-10 border-b" />}
              </tr>
              <tr>
                <th className="sticky left-0 bg-card z-20 border-b border-r" />
                {visibleDates.map(date =>
                  PERIODS.map(p => (
                    <th key={`${date}-${p}`}
                      className="w-[68px] text-[10px] text-muted-foreground p-0.5 border-b">
                      {PERIOD_LABELS[p]}
                    </th>
                  ))
                )}
                {canEdit && isDoctor && <th className="border-b" />}
              </tr>
            </thead>
            <tbody>
              {gridItems.map(item => (
                <tr key={item.id}>
                  <td className="sticky left-0 bg-card z-10 p-1 min-w-[180px] border-b border-r">
                    <p className="text-sm font-semibold">
                      {item.medicineName}
                    </p>
                    <p className="text-[10px] text-muted-foreground">
                      {item.medicineMethod || ''}{item.regime ? ` • ${item.regime}` : ''}
                    </p>
                  </td>

                  {visibleDates.map(date =>
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

                      const onAuxClick = (e: React.MouseEvent) => {
                        e.preventDefault();
                        if (!dp || !canEdit) return;
                        if (isDoctor && dp.isPlanned && !dp.isCompleted) doctorCancel(dp);
                      };

                      return (
                        <td key={`${date}-${period}`} style={{
                          width: 68, height: 32, cursor: bg === '#fff' || !dp ? 'default' : 'pointer',
                          backgroundColor: bg, textAlign: 'center', verticalAlign: 'middle',
                          position: 'relative', borderBottom: '1px solid hsl(var(--border))',
                        }}
                          onClick={onClick}
                          onAuxClick={onAuxClick}
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
                    <td className="w-10 text-center border-b">
                      <Button variant="ghost" size="icon-xs" onClick={(e) => onOpenDeleteConfirm(item.id, e.currentTarget as HTMLElement)}>
                        <Trash2 className="size-3" />
                      </Button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
