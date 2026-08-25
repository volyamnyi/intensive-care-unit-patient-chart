import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Popover as PopoverPrimitive } from '@base-ui/react/popover';
import type { PrescriptionDayPart } from '../../types/medication';

const PERIOD_FULL: Record<string, string> = {
  morning: 'Ранок', day: 'День', evening: 'Вечір', night: 'Ніч',
};

export interface ExecuteDosePopoverProps {
  execAnchor: HTMLElement | null;
  execDp: PrescriptionDayPart | null;
  execDose: string;
  onExecDoseChange: (dose: string) => void;
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
  executing, show2fa,
  secondPersonLogin, onSecondPersonLoginChange,
  secondPersonPassword, onSecondPersonPasswordChange,
  secondPersonError,
  onCloseExecute, onProceedTo2fa, onCommitExecute, onShow2faChange,
}: ExecuteDosePopoverProps) {
  return (
    <>
      {Boolean(execAnchor) && !show2fa && (
        <PopoverPrimitive.Root
          open
          onOpenChange={(_open, _eventDetails) => {
            if (!_open) onCloseExecute();
          }}
          anchor={execAnchor ?? undefined}
        >
          <PopoverPrimitive.Portal>
            <PopoverPrimitive.Positioner align="start" sideOffset={4}>
              <PopoverPrimitive.Popup
                data-slot="popover-content"
                className="z-50 flex min-w-[260px] flex-col gap-1.5 rounded-xl border border-border bg-popover p-2 text-sm text-popover-foreground shadow-md outline-none data-open:animate-in data-open:fade-in-0 data-open:zoom-in-95 data-closed:animate-out data-closed:fade-out-0"
              >
                <p className="text-sm font-medium">
                  {execDp ? `${PERIOD_FULL[execDp.period]}: ${execDp.dose ?? '—'}` : 'Виконання дози'}
                </p>
                <Input placeholder="Фактична доза" value={execDose}
                  onChange={(e: MouseEvent<HTMLInputElement>) => onExecDoseChange((e.target as HTMLInputElement).value)} autoFocus />
                <div className="flex justify-end gap-1">
                  <Button size="sm" variant="outline" onClick={onCloseExecute}>Скасувати</Button>
                  <Button size="sm" variant="default"
                    disabled={!execDose.trim()}
                    onClick={onProceedTo2fa}>Продовжити</Button>
                </div>
              </PopoverPrimitive.Popup>
            </PopoverPrimitive.Positioner>
          </PopoverPrimitive.Portal>
        </PopoverPrimitive.Root>
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
