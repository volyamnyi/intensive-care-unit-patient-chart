import { useState } from 'react';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import type { PrescriptionDayPart } from '../../types';

interface PrescriptionExecutionPanelProps {
  dayParts: PrescriptionDayPart[];
  onExecute: (dayPartId: string, actualDose: string, secondPersonLogin: string, secondPersonPassword: string) => Promise<void>;
  executing?: boolean;
}

const periodLabels: Record<string, string> = {
  morning: 'Ранок',
  day: 'День',
  evening: 'Вечір',
  night: 'Ніч',
};

export default function PrescriptionExecutionPanel({ dayParts, onExecute, executing }: PrescriptionExecutionPanelProps) {
  const [actualDoses, setActualDoses] = useState<Record<string, string>>({});
  const [selectedPartId, setSelectedPartId] = useState<string | null>(null);
  const [secondPersonLogin, setSecondPersonLogin] = useState('');
  const [secondPersonPassword, setSecondPersonPassword] = useState('');
  const [secondPersonError, setSecondPersonError] = useState('');

  const plannedParts = dayParts.filter((p) => p.isPlanned && !p.isCompleted);

  const open2fa = (partId: string) => {
    if (!actualDoses[partId]?.trim()) return;
    setSelectedPartId(partId);
    setSecondPersonLogin('');
    setSecondPersonPassword('');
    setSecondPersonError('');
  };

  const close2fa = () => {
    setSelectedPartId(null);
    setSecondPersonError('');
  };

  const commitExecute = async () => {
    if (!selectedPartId || !secondPersonLogin.trim() || !secondPersonPassword.trim()) return;
    setSecondPersonError('');
    try {
      await onExecute(selectedPartId, actualDoses[selectedPartId], secondPersonLogin, secondPersonPassword);
      close2fa();
    } catch (err: any) {
      setSecondPersonError(err?.response?.data?.message || err?.message || 'Помилка 2FA');
    }
  };

  if (plannedParts.length === 0) {
    return <p className="text-muted-foreground">Немає запланованих доз для виконання</p>;
  }

  return (
    <>
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Період</TableHead>
              <TableHead>Планова доза</TableHead>
              <TableHead>Фактична доза</TableHead>
              <TableHead>Дія</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {plannedParts.map((part) => (
              <TableRow key={part.id}>
                <TableCell>{periodLabels[part.period] || part.period}</TableCell>
                <TableCell>{part.dose || '—'}</TableCell>
                <TableCell>
                  <Input
                    placeholder="Фактична доза"
                    value={actualDoses[part.id] || ''}
                    onChange={(e) => setActualDoses((prev) => ({ ...prev, [part.id]: e.target.value }))}
                    disabled={executing}
                    className="w-24"
                  />
                </TableCell>
                <TableCell>
                  <Button
                    size="sm"
                    variant="default"
                    disabled={executing || !actualDoses[part.id]?.trim()}
                    onClick={() => open2fa(part.id)}
                  >
                    Виконати
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Dialog open={Boolean(selectedPartId)} onOpenChange={(open) => { if (!open) close2fa(); }}>
        <DialogContent className="max-w-xs">
          <DialogHeader>
            <DialogTitle>2-факторна авторизація</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground mb-2">
            Для виконання призначення необхідне підтвердження іншою медсестрою.
          </p>
          <div className="flex flex-col gap-1.5 mt-1">
            <Input placeholder="Логін другої особи" value={secondPersonLogin}
              onChange={e => setSecondPersonLogin(e.target.value)}
              disabled={executing} autoFocus />
            <Input placeholder="Пароль" type="password" value={secondPersonPassword}
              onChange={e => setSecondPersonPassword(e.target.value)}
              disabled={executing} />
            {secondPersonError && (
              <p className="text-xs text-destructive">{secondPersonError}</p>
            )}
          </div>
          <DialogFooter>
            <Button size="sm" variant="outline" onClick={close2fa} disabled={executing}>Скасувати</Button>
            <Button size="sm" variant="default"
              disabled={executing || !secondPersonLogin.trim() || !secondPersonPassword.trim()}
              onClick={commitExecute}>Підтвердити</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
