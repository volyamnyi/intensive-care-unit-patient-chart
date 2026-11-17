import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { DatetimeInput } from '@/components/ui/datetime-input';
import { Dialog, DialogContent, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import {
  Popover,
  PopoverContent,
  PopoverPortal,
  PopoverPositioner,
} from '@/components/ui/popover';
import type { PrescriptionDayPart } from '../../types/medication';

const PERIOD_FULL: Record<string, string> = {
  morning: 'Ранок', day: 'День', evening: 'Вечір', night: 'Ніч',
};

export interface ExecuteDosePopoverProps {
  execAnchor: HTMLElement | null;
  execDp: PrescriptionDayPart | null;
  execDose: string;
  onExecDoseChange: (dose: string) => void;
  /** Actual administration time (ISO local, seconds always present) - #443/#446. */
  execAdministeredAt: string | null;
  onExecAdministeredAtChange: (value: string | null) => void;
  /** Whether the dispensing time is captured separately (opt-in, #443/#446). */
  execDispensedEnabled: boolean;
  onExecDispensedEnabledChange: (enabled: boolean) => void;
  execDispensedAt: string | null;
  onExecDispensedAtChange: (value: string | null) => void;
  /** Inline client-side validation message (V1-V4 mirror); blocks «Продовжити». */
  execTimeError: string | null;
  /** Reset the administration time to the current local time. */
  onExecTimeNow: () => void;
  /** Lower/upper bound of the list period (yyyy-MM-dd) for the date picker. */
  execMinDate?: string;
  execMaxDate?: string;
  executing: boolean;
  show2fa: boolean;
  secondPersonLogin: string;
  onSecondPersonLoginChange: (login: string) => void;
  secondPersonPassword: string;
  onSecondPersonPasswordChange: (password: string) => void;
  secondPersonError: string;
  onCloseExecute: () => void;
  onProceedTo2fa: () => void;
  onCommitExecute: () => void;
  onShow2faChange: (open: boolean) => void;
}

export default function ExecuteDosePopover({
  execAnchor, execDp, execDose, onExecDoseChange,
  execAdministeredAt, onExecAdministeredAtChange,
  execDispensedEnabled, onExecDispensedEnabledChange,
  execDispensedAt, onExecDispensedAtChange,
  execTimeError, onExecTimeNow, execMinDate, execMaxDate,
  executing, show2fa,
  secondPersonLogin, onSecondPersonLoginChange,
  secondPersonPassword, onSecondPersonPasswordChange,
  secondPersonError,
  onCloseExecute, onProceedTo2fa, onCommitExecute, onShow2faChange,
}: ExecuteDosePopoverProps) {
  return (
    <>
      {Boolean(execAnchor) && !show2fa && (
        <Popover
          open
          onOpenChange={(_open: boolean) => {
            if (!_open) onCloseExecute();
          }}
        >
          <PopoverPortal>
            <PopoverPositioner anchor={execAnchor ?? undefined} align="start" sideOffset={4}>
              <PopoverContent className="z-50 flex min-w-[260px] flex-col gap-1.5 rounded-xl p-2">
                <p className="text-sm font-medium">
                  {execDp ? `${PERIOD_FULL[execDp.period]}: ${execDp.dose ?? '—'}` : 'Виконання дози'}
                </p>
                <Input placeholder="Фактична доза" value={execDose}
                  onChange={e => onExecDoseChange(e.target.value)} autoFocus />

                {/* Actual administration/dispensing time (#443/#446): a medical
                    fact, not a planned value - kept in its own section so the
                    planned context above stays unambiguous. */}
                <div className="mt-1 rounded-md border border-border p-2">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-xs font-medium">Фактичний час введення</p>
                    <Button size="sm" variant="ghost" type="button" onClick={onExecTimeNow}>
                      Зараз
                    </Button>
                  </div>
                  <DatetimeInput
                    value={execAdministeredAt}
                    onChange={onExecAdministeredAtChange}
                    dateAriaLabel="Дата введення"
                    timeAriaLabel="Час введення"
                    min={execMinDate}
                    max={execMaxDate}
                    invalid={Boolean(execTimeError)}
                  />
                  <label className="mt-1.5 flex items-center gap-1.5 text-xs">
                    <Checkbox
                      checked={execDispensedEnabled}
                      onCheckedChange={(v) => onExecDispensedEnabledChange(v === true)}
                    />
                    Окремо фіксувати час видачі
                  </label>
                  {execDispensedEnabled && (
                    <div className="mt-1.5">
                      <DatetimeInput
                        value={execDispensedAt}
                        onChange={onExecDispensedAtChange}
                        dateAriaLabel="Дата видачі"
                        timeAriaLabel="Час видачі"
                        min={execMinDate}
                        max={execMaxDate}
                        invalid={Boolean(execTimeError)}
                      />
                    </div>
                  )}
                  {execTimeError && (
                    <p className="mt-1 text-xs text-destructive" role="alert">{execTimeError}</p>
                  )}
                </div>

                <div className="flex justify-end gap-1">
                  <Button size="sm" variant="outline" onClick={onCloseExecute}>Скасувати</Button>
                  <Button size="sm" variant="default"
                    disabled={!execDose.trim() || Boolean(execTimeError)}
                    onClick={onProceedTo2fa}>Продовжити</Button>
                </div>
              </PopoverContent>
            </PopoverPositioner>
          </PopoverPortal>
        </Popover>
      )}

      <Dialog open={show2fa} onOpenChange={(open) => { if (!executing && !open) onShow2faChange(false); }}>
        <DialogContent className="max-w-xs">
          <DialogTitle>2-факторна авторизація</DialogTitle>
          <DialogDescription>
            Для виконання призначення необхідне підтвердження іншою медсестрою.
            Увійдіть під обліковим записом другої особи.
          </DialogDescription>
          <div className="mt-1 flex flex-col gap-1.5">
            <Input placeholder="Логін другої особи" value={secondPersonLogin}
              onChange={e => onSecondPersonLoginChange(e.target.value)}
              disabled={executing} autoFocus />
            <Input placeholder="Пароль" type="password" value={secondPersonPassword}
              onChange={e => onSecondPersonPasswordChange(e.target.value)}
              disabled={executing} />
            {secondPersonError && (
              <p className="text-xs text-destructive">{secondPersonError}</p>
            )}
          </div>
          <DialogFooter>
            <Button size="sm" variant="outline" onClick={() => onShow2faChange(false)} disabled={executing}>Скасувати</Button>
            <Button size="sm" variant="default"
              disabled={executing || !secondPersonLogin.trim() || !secondPersonPassword.trim()}
              onClick={onCommitExecute}>Підтвердити</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
