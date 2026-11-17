import * as React from 'react';
import { toast } from 'sonner';

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { DatetimeInput } from '@/components/ui/datetime-input';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { prescriptionApi } from '@/api/medication';
import { actualTimeErrorMessage, currentLocalIso, formatActualTime, validateActualTime } from '@/lib/actualTime';
import type { PrescriptionDayPart } from '@/types/medication';

const REASON_MAX_LENGTH = 500;

export interface ActualTimeCorrectionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The executed day part being corrected (carries executionId/version/times). */
  dayPart: PrescriptionDayPart | null;
  /** Lower bound of the list period (yyyy-MM-dd) - the item's first day. */
  minDate?: string;
  /** Upper bound of the list period (yyyy-MM-dd) - the cell's day (+1 day allowed). */
  maxDate?: string;
  /** Called after a successful correction so the grid can refresh. */
  onUpdated?: () => void | Promise<void>;
}

/**
 * Correction of an already recorded actual administration/dispensing time
 * (#444/#446). The current value is always shown read-only, the reason is
 * mandatory and the optimistic-lock version travels with the request - a stale
 * version yields 409 and never a silent overwrite.
 */
export default function ActualTimeCorrectionDialog({
  open,
  onOpenChange,
  dayPart,
  minDate,
  maxDate,
  onUpdated,
}: ActualTimeCorrectionDialogProps) {
  const [administeredAt, setAdministeredAt] = React.useState<string | null>(null);
  const [dispensedAt, setDispensedAt] = React.useState<string | null>(null);
  const [reason, setReason] = React.useState('');
  const [saving, setSaving] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [conflict, setConflict] = React.useState(false);

  // Re-seed the form whenever another execution is opened for correction.
  React.useEffect(() => {
    if (!open) return;
    setAdministeredAt(dayPart?.administeredAt ?? null);
    setDispensedAt(dayPart?.dispensedAt ?? null);
    setReason('');
    setError(null);
    setConflict(false);
  }, [open, dayPart?.id, dayPart?.administeredAt, dayPart?.dispensedAt]);

  const validation = validateActualTime(administeredAt, {
    minDate,
    maxDate: maxDate ?? dayPart?.dayDate ?? undefined,
    nowIso: currentLocalIso(),
    dispensedIso: dispensedAt ?? undefined,
  });
  const timeError = administeredAt ? actualTimeErrorMessage(validation) : null;
  const reasonError = reason.length > REASON_MAX_LENGTH ? 'Причина задовга' : null;
  const executionId = dayPart?.executionId ?? null;
  const version = dayPart?.executionVersion ?? null;
  const canSubmit = Boolean(
    executionId && version !== null && administeredAt && !timeError && reason.trim() && !reasonError,
  );

  const submit = async () => {
    if (!canSubmit || !executionId || version === null || !administeredAt) return;
    setSaving(true);
    setError(null);
    setConflict(false);
    try {
      await prescriptionApi.updateActualTime(executionId, {
        administeredAt,
        dispensedAt: dispensedAt ?? undefined,
        reason: reason.trim(),
        version,
      });
      toast.success('Фактичний час оновлено');
      onOpenChange(false);
      await onUpdated?.();
    } catch (err: any) {
      const status = err?.response?.status;
      if (status === 409) {
        setConflict(true);
      } else {
        setError(
          err?.response?.data?.message
          ?? err?.message
          ?? 'Не вдалося оновити фактичний час',
        );
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(next) => { if (!saving) onOpenChange(next); }}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Коригування фактичного часу</DialogTitle>
          <DialogDescription>
            Медично значуща зміна фіксується з причиною та автором. Планові дата,
            період і доза не змінюються.
          </DialogDescription>
        </DialogHeader>

        {conflict && (
          <Alert variant="destructive">
            <AlertTitle>Дані змінено іншим користувачем</AlertTitle>
            <AlertDescription>
              Оновіть сторінку, щоб побачити актуальний фактичний час, і повторіть коригування.
            </AlertDescription>
          </Alert>
        )}

        <div className="flex flex-col gap-3">
          <div className="rounded-md border border-border p-2 text-xs">
            <p className="font-medium">Поточні значення</p>
            <p>Введено: {formatActualTime(dayPart?.administeredAt) || '—'}</p>
            <p>Видано: {formatActualTime(dayPart?.dispensedAt) || '—'}</p>
            {dayPart?.administeredAtManual && <p>(уточнено вручну)</p>}
          </div>

          <div className="flex flex-col gap-1">
            <p className="text-xs font-medium">Новий фактичний час введення</p>
            <DatetimeInput
              value={administeredAt}
              onChange={setAdministeredAt}
              dateAriaLabel="Нова дата введення"
              timeAriaLabel="Новий час введення"
              min={minDate}
              max={maxDate}
              invalid={Boolean(timeError)}
            />
          </div>

          <div className="flex flex-col gap-1">
            <p className="text-xs font-medium">Новий фактичний час видачі (необовʼязково)</p>
            <DatetimeInput
              value={dispensedAt}
              onChange={setDispensedAt}
              dateAriaLabel="Нова дата видачі"
              timeAriaLabel="Новий час видачі"
              min={minDate}
              max={maxDate}
              invalid={Boolean(timeError)}
            />
            {dayPart?.dispensedAt && (
              <p className="text-[11px] text-muted-foreground">
                Очищення поля видаляє зафіксований час видачі.
              </p>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <label htmlFor="actual-time-reason" className="text-xs font-medium">
              Причина коригування
            </label>
            <Textarea
              id="actual-time-reason"
              value={reason}
              maxLength={REASON_MAX_LENGTH + 50}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Наприклад: дозу введено о 10:30, а не о 10:00"
              aria-invalid={Boolean(reasonError)}
            />
            <div className="flex justify-between text-[11px]">
              <span className={reasonError ? 'text-destructive' : 'text-muted-foreground'}>
                {reasonError ?? 'Обовʼязкове поле'}
              </span>
              <span className="text-muted-foreground">{reason.length}/{REASON_MAX_LENGTH}</span>
            </div>
          </div>

          {timeError && (
            <p className="text-xs text-destructive" role="alert">{timeError}</p>
          )}
          {error && (
            <p className="text-xs text-destructive" role="alert">{error}</p>
          )}
        </div>

        <DialogFooter>
          <Button size="sm" variant="outline" onClick={() => onOpenChange(false)} disabled={saving}>
            Скасувати
          </Button>
          <Button size="sm" variant="default" onClick={submit} disabled={!canSubmit || saving}>
            Зберегти
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
