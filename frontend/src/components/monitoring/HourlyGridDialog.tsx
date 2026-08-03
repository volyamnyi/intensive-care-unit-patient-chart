import { useEffect, useState, type ReactNode } from 'react';
import { Clock, RefreshCw, X } from 'lucide-react';
import { Dialog, DialogClose, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type { ClinicalDay, Episode } from '../../types';

const STATUS_STYLES: Record<string, string> = {
  OPEN: 'bg-warning/10 text-warning border-warning',
  NURSE_SIGNED: 'bg-info/10 text-info border-info',
  DOCTOR_SIGNED: 'bg-success/10 text-success border-success',
  CLOSED: 'bg-muted text-muted-foreground border-border',
  REOPENED: 'bg-warning/10 text-warning border-warning',
};

const STATUS_LABELS: Record<string, string> = {
  OPEN: 'Відкрито',
  NURSE_SIGNED: 'Підписано м/с',
  DOCTOR_SIGNED: 'Підписано лікарем',
  CLOSED: 'Закрито',
  REOPENED: 'Перевідкрито',
};

function formatDayRange(startDateTime?: string | null, endDateTime?: string | null): string {
  if (!startDateTime) return '';
  const start = new Date(startDateTime);
  const date = start.toLocaleDateString('uk-UA', { day: '2-digit', month: '2-digit', year: 'numeric' });
  const time = (d: Date) => d.toLocaleTimeString('uk-UA', { hour: '2-digit', minute: '2-digit' });
  const end = endDateTime ? time(new Date(endDateTime)) : '24:00';
  return `${date} · ${time(start)}–${end}`;
}

function saveStatusLabel(status: string, isLocked: boolean): string {
  if (isLocked) return 'Доба підписана — перегляд';
  switch (status) {
    case 'saving': return 'Зберігається…';
    case 'saved': return 'Збережено';
    case 'error': return 'Помилка збереження';
    default: return 'Зміни зберігаються при введенні';
  }
}

function useClock(): string {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 30_000);
    return () => clearInterval(id);
  }, []);
  return now.toLocaleTimeString('uk-UA', { hour: '2-digit', minute: '2-digit' });
}

interface HourlyGridDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  episode: Episode | null;
  selectedDay: ClinicalDay | null;
  isLocked: boolean;
  saveStatus: string;
  onRefresh?: () => void;
  children: ReactNode;
}

export default function HourlyGridDialog({
  open, onOpenChange, episode, selectedDay, isLocked, saveStatus, onRefresh, children,
}: HourlyGridDialogProps) {
  const time = useClock();
  const patientName = episode?.patientName || 'Пацієнт';
  const status = selectedDay?.status ?? '';

  return (
    <Dialog open={open} onOpenChange={onOpenChange} disablePointerDismissal>
      <DialogContent
        showCloseButton={false}
        style={{ inset: 0, translate: 'none' }}
        className="grid w-full max-w-none grid-rows-[auto_auto_minmax(0,1fr)_auto] gap-0 rounded-none bg-card p-0 ring-0 sm:max-w-none print:hidden"
      >
        <DialogTitle className="sr-only">{`Погодинна карта — ${patientName}`}</DialogTitle>

        <header className="flex h-12 min-w-0 items-center justify-between gap-2 border-b border-border bg-card px-3">
          <div className="flex min-w-0 items-center gap-2">
            <span className="truncate text-sm font-semibold text-card-foreground">{patientName}</span>
            {selectedDay && (
              <Badge variant="outline" className="shrink-0 border-border text-muted-foreground">
                {`Доба ${selectedDay.dayNumber}`}
              </Badge>
            )}
            {status && (
              <Badge variant="outline" className={cn('shrink-0', STATUS_STYLES[status] ?? 'border-border text-muted-foreground')}>
                {STATUS_LABELS[status] ?? status}
              </Badge>
            )}
          </div>
          <div className="flex shrink-0 items-center gap-1">
            <span className="hidden items-center gap-1 text-xs text-muted-foreground sm:flex">
              <Clock className="size-3.5" />
              {time}
            </span>
            <Button variant="ghost" size="icon-sm" aria-label="Оновити дані" onClick={onRefresh}>
              <RefreshCw className="size-4" />
            </Button>
            <DialogClose render={<Button variant="ghost" size="icon-sm" aria-label="Закрити (Esc)" />}>
              <X className="size-4" />
            </DialogClose>
          </div>
        </header>

        <div className="flex h-9 min-w-0 items-center justify-between gap-2 border-b border-border bg-card px-2">
          <span className="text-sm font-semibold text-card-foreground">{'Погодинна карта'}</span>
          <span className="text-xs text-muted-foreground">
            {formatDayRange(selectedDay?.startDateTime, selectedDay?.endDateTime)}
          </span>
        </div>

        <div className="h-full min-h-0 overflow-y-auto">
          {children}
        </div>

        <footer className="flex h-11 min-w-0 items-center justify-between gap-2 border-t border-border bg-card px-3">
          <span className="truncate text-xs text-muted-foreground">{saveStatusLabel(saveStatus, isLocked)}</span>
          <div className="flex shrink-0 items-center gap-1.5">
            <DialogClose render={<Button aria-label="Закрити карту">{'Назад до карти'}</Button>} />
            <DialogClose render={<Button variant="ghost" size="icon-sm" aria-label="Закрити (Esc)" />}>
              <X className="size-4" />
            </DialogClose>
          </div>
        </footer>
      </DialogContent>
    </Dialog>
  );
}
