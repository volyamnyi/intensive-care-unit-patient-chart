import { useEffect, useRef } from 'react';
import { cn } from '@/lib/utils';
import type { ClinicalDay } from '../../types/icu';

interface ClinicalDayTimelineProps {
  days: ClinicalDay[];
  selectedDayId?: string;
  onSelectDay: (day: ClinicalDay) => void;
}

const STATUS_BG: Record<string, string> = {
  OPEN: 'bg-success/10',
  NURSE_SIGNED: 'bg-info/10',
  DOCTOR_SIGNED: 'bg-success/10',
  CLOSED: 'bg-muted',
  REOPENED: 'bg-warning/10',
};

const STATUS_BORDER: Record<string, string> = {
  OPEN: 'border-success',
  NURSE_SIGNED: 'border-info',
  DOCTOR_SIGNED: 'border-success',
  CLOSED: 'border-border',
  REOPENED: 'border-warning',
};

function getStatusLabel(status: string): string {
  switch (status) {
    case 'OPEN': return 'Відкрито';
    case 'NURSE_SIGNED': return 'Підписано м/с';
    case 'DOCTOR_SIGNED': return 'Підписано лікарем';
    case 'CLOSED': return 'Закрито';
    case 'REOPENED': return 'Перевідкрито';
    default: return status;
  }
}

function getStatusTextColor(status: string): string {
  switch (status) {
    case 'OPEN': return 'text-success';
    case 'NURSE_SIGNED': return 'text-info';
    case 'DOCTOR_SIGNED': return 'text-success';
    case 'REOPENED': return 'text-warning';
    default: return 'text-muted-foreground';
  }
}

export default function ClinicalDayTimeline({ days, selectedDayId, onSelectDay }: ClinicalDayTimelineProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!selectedDayId || !containerRef.current) return;
    const el = containerRef.current.querySelector<HTMLElement>(`[data-day-id="${selectedDayId}"]`);
    if (el && typeof el.scrollIntoView === 'function') {
      el.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    }
  }, [selectedDayId]);

  if (days.length === 0) {
    return (
      <div className="py-2 text-center">
        <span className="text-[13px] text-muted-foreground font-mulish">Немає клінічних днів</span>
      </div>
    );
  }

  const sortedDays = [...days].sort((a, b) => a.dayNumber - b.dayNumber);

  return (
    <div ref={containerRef} className="flex gap-1 overflow-auto py-1">
      {sortedDays.map((day) => {
        const isSelected = day.id === selectedDayId;
        const bgClass = STATUS_BG[day.status] || 'bg-muted';
        const borderClass = STATUS_BORDER[day.status] || 'border-border';
        return (
          <div
            key={day.id}
            data-day-id={day.id}
            onClick={() => onSelectDay(day)}
            tabIndex={0}
            role="button"
            aria-current={isSelected ? 'true' : undefined}
            className={cn(
              'min-w-[90px] cursor-pointer rounded-xl px-3 py-[5px] text-center text-[13px] font-bold transition-all duration-200 hover:translate-y-[-2px] hover:shadow-[0_2px_8px_rgba(0,0,0,0.08)]',
              bgClass,
              isSelected ? 'border-2 border-primary' : `border ${borderClass}`,
            )}
          >
            <span className="block font-bold font-rubik">Доба {day.dayNumber}</span>
            <span className="mt-0.5 block text-xs text-muted-foreground font-mulish">
              {new Date(day.startDateTime).toLocaleDateString('uk-UA', { day: 'numeric', month: 'short' })}
            </span>
            <span className={cn('mt-0.5 block text-[9px] font-semibold', getStatusTextColor(day.status))}>
              {getStatusLabel(day.status)}
            </span>
          </div>
        );
      })}
    </div>
  );
}
