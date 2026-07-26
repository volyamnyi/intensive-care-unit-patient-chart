import { useState } from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Button, Checkbox, FormControlLabel, Typography, Box } from '@mui/material';
import type { PrescriptionDayPart } from '../../types';

interface PrescriptionExecutionPanelProps {
  dayParts: PrescriptionDayPart[];
  onExecute: (dayPartId: string, actualDose: string, requires2pAuth: boolean, secondPersonId?: string) => void;
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
  const [requires2p, setRequires2p] = useState<Record<string, boolean>>({});
  const [secondPersonIds, setSecondPersonIds] = useState<Record<string, string>>({});

  const plannedParts = dayParts.filter((p) => p.isPlanned && !p.isCompleted);

  if (plannedParts.length === 0) {
    return <Typography color="text.secondary">Немає запланованих доз для виконання</Typography>;
  }

  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small" sx={{ minWidth: 500 }}>
        <TableHead>
          <TableRow>
            <TableCell>Період</TableCell>
            <TableCell>Планова доза</TableCell>
            <TableCell>Фактична доза</TableCell>
            <TableCell>2П авторизація</TableCell>
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
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        size="small"
                        checked={requires2p[part.id] || false}
                        onChange={(e) => setRequires2p((prev) => ({ ...prev, [part.id]: e.target.checked }))}
                        disabled={executing}
                      />
                    }
                    label="Потрібна"
                  />
                  {requires2p[part.id] && (
                    <TextField
                      size="small"
                      label="ID другої особи"
                      value={secondPersonIds[part.id] || ''}
                      onChange={(e) => setSecondPersonIds((prev) => ({ ...prev, [part.id]: e.target.value }))}
                      disabled={executing}
                    />
                  )}
                </Box>
              </TableCell>
              <TableCell>
                <Button
                  size="small"
                  variant="contained"
                  color="success"
                  disabled={executing || !actualDoses[part.id]}
                  onClick={() =>
                    onExecute(
                      part.id,
                      actualDoses[part.id],
                      requires2p[part.id] || false,
                      requires2p[part.id] ? secondPersonIds[part.id] : undefined
                    )
                  }
                >
                  Виконати
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
