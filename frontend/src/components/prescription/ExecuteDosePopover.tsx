import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
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
        <div
          className="fixed z-50"
          style={{
            top: execAnchor ? execAnchor.getBoundingClientRect().bottom + 4 : 0,
            left: execAnchor ? execAnchor.getBoundingClientRect().left : 0,
          }}
        >
          <div className="rounded-xl border bg-popover text-popover-foreground shadow-md p-2 min-w-[260px] flex flex-col gap-1.5">
            <p className="text-sm font-medium">
              {execDp ? `${PERIOD_FULL[execDp.period]}: ${execDp.dose ?? '—'}` : 'Виконання дози'}
            </p>
            <Input placeholder="Фактична доза" value={execDose}
              onChange={e => onExecDoseChange(e.target.value)} autoFocus />
            <div className="flex gap-1 justify-end">
              <Button size="sm" variant="outline" onClick={onCloseExecute}>Скасувати</Button>
              <Button size="sm" variant="default"
                disabled={!execDose.trim()}
                onClick={onProceedTo2fa}>Продовжити</Button>
            </div>
          </div>
        </div>
      )}

      <Dialog open={show2fa} onOpenChange={(open) => { if (!executing && !open) onShow2faChange(false); }}>
        <DialogContent className="max-w-xs">
          <DialogTitle>2-факторна авторизація</DialogTitle>
          <DialogDescription>
            Для виконання призначення необхідне підтвердження іншою медсестрою.
            Увійдіть під обліковим записом другої особи.
          </DialogDescription>
          <div className="flex flex-col gap-1.5 mt-1">
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
