import { useEffect, useCallback, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { Clock, Loader2, Lock, RefreshCw, TriangleAlert, X } from 'lucide-react';
import { Dialog, DialogClose, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { countCriticalTotal, pluralCritical } from './criticalRanges';
import type { ClinicalDay, Episode, HourlyRecord } from '../../types';

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

function saveStatusColor(status: string, isLocked: boolean): string {
  if (isLocked) return 'text-muted-foreground';
  switch (status) {
    case 'saved': return 'text-[#2E7D32] dark:text-[#81C784]';
    case 'error': return 'text-destructive';
    default: return 'text-muted-foreground';
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
  feedback?: { message: string; severity: 'success' | 'error' } | null;
  finalFocusRef?: React.RefObject<HTMLElement | null>;
  conflict?: { hour: number; key: string; raw: string } | null;
  onResolveConflict?: (keep: boolean) => void;
  loading?: boolean;
  recByHour?: Map<number, HourlyRecord>;
  children: ReactNode;
}

export default function HourlyGridDialog({
  open, onOpenChange, episode, selectedDay, isLocked, saveStatus, onRefresh, feedback, finalFocusRef,
  conflict, onResolveConflict, loading, recByHour, children,
}: HourlyGridDialogProps) {
  const time = useClock();
  const patientName = episode?.patientName || 'Пацієнт';
  const status = selectedDay?.status ?? '';
  const [origin, setOrigin] = useState<string | undefined>(undefined);
  const popupRef = useRef<HTMLDivElement>(null);
  const filledHours = recByHour?.size ?? 0;
  const criticalCount = recByHour ? countCriticalTotal(recByHour) : 0;
  const focusFirstCritical = useCallback(() => {
    const cell = popupRef.current?.querySelector<HTMLElement>('[data-critical="true"]');
    if (!cell) return;
    cell.scrollIntoView({ block: 'center', inline: 'center' });
    const input = cell.querySelector<HTMLInputElement>('input');
    if (input && !input.disabled) input.focus();
    else cell.focus();
  }, []);

  // Початковий фокус — на кнопці закриття (✕ у шапці, перший у DOM), навмисне відхилення
  // від APG-дефолту «перше фокусоване поле»: клітинки таблиці редагуються одразу після Tab,
  // а випадковий ввід у першу клітинку (ЧСС 0:00) до прочитання стану пацієнта — небезпечний
  // для даних (WCAG 2.4.3).
  const handleInitialFocus = () =>
    popupRef.current?.querySelector<HTMLElement>('[aria-label="Закрити вікно (Esc)"]') ?? null;

  const handleOpenChange = (next: boolean) => {
    if (next === open) return;
    if (!next && document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
    onOpenChange(next);
  };

  useLayoutEffect(() => {
    if (!open) return;
    const trigger = document.querySelector('[aria-label="Розгорнути на весь екран"]');
    if (trigger) {
      const rect = trigger.getBoundingClientRect();
      setOrigin(`${Math.round(rect.left + rect.width / 2)}px ${Math.round(rect.top + rect.height / 2)}px`);
    }
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const container = document.querySelector('main[class*="flex-1"] > div[class*="overflow-hidden"]');
    container?.classList.add('ring-2', 'ring-primary');
    const id = window.setTimeout(() => {
      container?.classList.remove('ring-2', 'ring-primary');
    }, 150);
    return () => {
      window.clearTimeout(id);
      container?.classList.remove('ring-2', 'ring-primary');
    };
  }, [open]);

  return (
    <Dialog open={open} onOpenChange={handleOpenChange} disablePointerDismissal>
      <DialogContent
        ref={popupRef}
        showCloseButton={false}
        data-fullscreen="true"
        aria-modal="true"
        initialFocus={handleInitialFocus}
        finalFocus={finalFocusRef ?? undefined}
        style={{ inset: 0, translate: 'none', transformOrigin: origin }}
        className="grid w-full max-w-none grid-rows-[auto_auto_minmax(0,1fr)_auto] gap-0 rounded-none bg-card p-0 ring-0 sm:max-w-none print:hidden"
      >
        <DialogTitle className="sr-only">{`Погодинна карта — ${patientName}`}</DialogTitle>

        <header className="flex min-h-12 min-w-0 items-center justify-between gap-2 border-b border-border bg-card px-3 pt-[env(safe-area-inset-top)]">
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
            {criticalCount > 0 && (
              <Button
                variant="outline"
                size="sm"
                aria-label="Показати перше критичне значення"
                onClick={focusFirstCritical}
                className="h-7 shrink-0 gap-1.5 border-warning/60 px-2 text-xs text-warning"
              >
                <TriangleAlert className="size-3.5" />
                {`${criticalCount} ${pluralCritical(criticalCount)}`}
              </Button>
            )}
            <span className="hidden items-center gap-1 text-xs text-muted-foreground sm:flex">
              <Clock className="size-3.5" />
              {time}
            </span>
            <Button variant="ghost" size="icon-sm" aria-label="Оновити показники" onClick={onRefresh}>
              <RefreshCw className="size-4" />
            </Button>
            <DialogClose render={<Button variant="ghost" size="icon-sm" aria-label="Закрити вікно (Esc)" />}>
              <X className="size-4" />
            </DialogClose>
          </div>
        </header>

        <div className="min-w-0">
          <div className="flex h-9 min-w-0 items-center justify-between gap-2 border-b border-border bg-card px-2">
            <span className="text-sm font-semibold text-card-foreground">{'Погодинна карта'}</span>
            <span role="status" aria-live="polite" className="sr-only">{feedback?.message}</span>
            <span className="flex items-center gap-2 text-xs text-muted-foreground">
              {loading && (
                <span role="img" aria-label="Завантаження" className="inline-flex">
                  <Loader2 aria-hidden="true" className="size-3.5 animate-spin" />
                </span>
              )}
              {formatDayRange(selectedDay?.startDateTime, selectedDay?.endDateTime)}
            </span>
          </div>
          {conflict && (
            <div role="status" aria-live="polite" className="flex min-h-9 min-w-0 items-center gap-2 border-b border-border bg-warning/10 px-3 py-1.5">
              <TriangleAlert className="size-4 shrink-0 text-warning" />
              <span className="min-w-0 flex-1 truncate text-xs font-medium text-warning">
                {`Запис змінено іншим користувачем (${conflict.key} ${conflict.hour}:00)`}
              </span>
              <Button size="sm" variant="outline" className="h-6 shrink-0 px-2 text-[11px]" onClick={() => onResolveConflict?.(false)}>
                {'Оновити дані'}
              </Button>
              <Button size="sm" className="h-6 shrink-0 px-2 text-[11px]" onClick={() => onResolveConflict?.(true)}>
                {'Залишити мій варіант'}
              </Button>
            </div>
          )}
          {isLocked && (
            <div className="flex h-8 min-w-0 items-center gap-2 border-b border-border bg-muted/40 px-3">
              <Lock className="size-3.5 shrink-0 text-muted-foreground" />
              <span className="truncate text-xs font-medium text-muted-foreground">{'Доба підписана — перегляд'}</span>
            </div>
          )}
        </div>

        <div className="h-full min-h-0 overflow-y-auto">
          {children}
        </div>

        <footer className="flex min-h-11 min-w-0 items-center justify-between gap-2 border-t border-border bg-card px-3 pb-[env(safe-area-inset-bottom)]">
          <div className="flex min-w-0 items-center gap-3">
            <span className="shrink-0 text-xs text-muted-foreground">{`Заповнено ${filledHours}/24 год`}</span>
            <span role="status" aria-live="polite" className={cn('truncate text-xs', saveStatusColor(saveStatus, isLocked))}>
              {isLocked ? 'Перегляд підписаної доби' : saveStatusLabel(saveStatus, isLocked)}
            </span>
          </div>
          <div className="flex shrink-0 items-center gap-1.5">
            <DialogClose render={<Button aria-label="Закрити карту">{'Назад до карти'}</Button>} />
            <DialogClose render={<Button variant="ghost" size="icon-sm" aria-label="Закрити вікно (Esc)" />}>
              <X className="size-4" />
            </DialogClose>
          </div>
        </footer>
      </DialogContent>
    </Dialog>
  );
}
