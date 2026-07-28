import { useState } from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Button, Dialog, DialogTitle, DialogContent, DialogActions, Typography, Box } from '@mui/material';
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
    return <Typography color="text.secondary">Немає запланованих доз для виконання</Typography>;
  }

  return (
    <>
      <TableContainer sx={{ overflowX: 'auto' }}>
        <Table size="small" sx={{ minWidth: 500 }}>
          <TableHead>
            <TableRow>
              <TableCell>Період</TableCell>
              <TableCell>Планова доза</TableCell>
              <TableCell>Фактична доза</TableCell>
              <TableCell>Дія</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {plannedParts.map((part) => (
              <TableRow key={part.id}>
                <TableCell>{periodLabels[part.period] || part.period}</TableCell>
                <TableCell>{part.dose || '—'}</TableCell>
                <TableCell>
                  <TextField
                    size="small"
                    label="Фактична доза"
                    value={actualDoses[part.id] || ''}
                    onChange={(e) => setActualDoses((prev) => ({ ...prev, [part.id]: e.target.value }))}
                    disabled={executing}
                  />
                </TableCell>
                <TableCell>
                  <Button
                    size="small"
                    variant="contained"
                    color="success"
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
      </TableContainer>

      <Dialog open={Boolean(selectedPartId)} onClose={close2fa} maxWidth="xs" fullWidth>
        <DialogTitle>2-факторна авторизація</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Для виконання призначення необхідне підтвердження іншою медсестрою.
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mt: 1 }}>
            <TextField size="small" label="Логін другої особи" value={secondPersonLogin}
              onChange={e => setSecondPersonLogin(e.target.value)}
              disabled={executing} autoFocus />
            <TextField size="small" label="Пароль" type="password" value={secondPersonPassword}
              onChange={e => setSecondPersonPassword(e.target.value)}
              disabled={executing} />
            {secondPersonError && (
              <Typography variant="caption" color="error">{secondPersonError}</Typography>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button size="small" onClick={close2fa} disabled={executing}>Скасувати</Button>
          <Button size="small" variant="contained" color="success"
            disabled={executing || !secondPersonLogin.trim() || !secondPersonPassword.trim()}
            onClick={commitExecute}>Підтвердити</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
