import { useState } from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Button, Chip, Typography, Box } from '@mui/material';
import type { PrescriptionDayPart } from '../../types';

interface DayPartPlannerProps {
  dayParts: PrescriptionDayPart[];
  onPlan: (dayPartId: string, dose: string) => void;
  onComplete: (dayPartId: string) => void;
  canPlan?: boolean;
  canComplete?: boolean;
  planning?: boolean;
  completing?: boolean;
}

const periodLabels: Record<string, string> = {
  morning: 'Ранок',
  day: 'День',
  evening: 'Вечір',
  night: 'Ніч',
};

export default function DayPartPlanner({
  dayParts,
  onPlan,
  onComplete,
  canPlan,
  canComplete,
  planning,
  completing,
}: DayPartPlannerProps) {
  const [doses, setDoses] = useState<Record<string, string>>({});

  if (dayParts.length === 0) {
    return <Typography color="text.secondary">Немає запланованих частин доби</Typography>;
  }

  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small" sx={{ minWidth: 500 }}>
        <TableHead>
          <TableRow>
            <TableCell>Період</TableCell>
            <TableCell>Доза</TableCell>
            <TableCell>Статус</TableCell>
            <TableCell>Дії</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {dayParts.map((part) => (
            <TableRow key={part.id}>
              <TableCell>{periodLabels[part.period] || part.period}</TableCell>
              <TableCell>{part.dose || '—'}</TableCell>
              <TableCell>
                <Box sx={{ display: 'flex', gap: 0.5 }}>
                  {part.isCompleted && <Chip label="Виконано" color="success" size="small" />}
                  {part.isPlanned && !part.isCompleted && <Chip label="Заплановано" color="info" size="small" />}
                  {!part.isPlanned && <Chip label="Не заплановано" size="small" />}
                </Box>
              </TableCell>
              <TableCell>
                {!part.isPlanned && canPlan && (
                  <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                    <TextField
                      size="small"
                      label="Доза"
                      value={doses[part.id] || ''}
                      onChange={(e) => setDoses((prev) => ({ ...prev, [part.id]: e.target.value }))}
                      disabled={planning}
                    />
                    <Button
                      size="small"
                      variant="contained"
                      disabled={planning || !doses[part.id]}
                      onClick={() => onPlan(part.id, doses[part.id])}
                    >
                      Запланувати
                    </Button>
                  </Box>
                )}
                {part.isPlanned && !part.isCompleted && canComplete && (
                  <Button
                    size="small"
                    variant="contained"
                    color="success"
                    disabled={completing}
                    onClick={() => onComplete(part.id)}
                  >
                    Завершити
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
