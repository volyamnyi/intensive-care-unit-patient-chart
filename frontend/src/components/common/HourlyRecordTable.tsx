import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, useTheme } from '@mui/material';
import type { HourlyRecord } from '../../types';

interface HourlyRecordTableProps {
  records: HourlyRecord[];
  hours: number[];
}

function medDayPos(h: number): number { return h < 8 ? h + 24 : h; }

export default function HourlyRecordTable({ records, hours }: HourlyRecordTableProps) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const clockHour = new Date().getHours();
  const getRec = (hour: number) => {
    return records.find((r) => {
      const h = Number(String(r.recordTime).substring(11, 13));
      return h === hour;
    });
  };

  return (
    <TableContainer component={Paper} sx={{ border: `1px solid ${isDark ? '#2A2A2A' : '#D0CEC9'}`, boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 8px rgba(0,0,0,0.04)', overflowX: 'auto' }}>
      <Table size="small" sx={{ minWidth: 600 }}>
        <TableHead>
          <TableRow>
            <TableCell>{'Година'}</TableCell>
            <TableCell>{'АТ сист.'}</TableCell>
            <TableCell>{'АТ діас.'}</TableCell>
            <TableCell>{'ЧСС'}</TableCell>
            <TableCell>{'SpO₂'}</TableCell>
            <TableCell>{'Темп.'}</TableCell>
            <TableCell>{'ЦВТ'}</TableCell>
            <TableCell>{'ЧД'}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {hours.map((h) => {
            const r = getRec(h);
            const isPast = medDayPos(h) < medDayPos(clockHour);
            let bg: string;
            if (isPast && r) {
              bg = isDark ? '#1A3A2A' : '#E8F5E9';
            } else if (isPast && !r) {
              bg = isDark ? '#3A1A1A' : '#FFEBEE';
            } else {
              bg = 'inherit';
            }
            return (
              <TableRow key={h} sx={{ bgcolor: bg }}>
                <TableCell sx={{ fontWeight: 600 }}>{h}:00</TableCell>
                <TableCell>{r?.systolicBP ?? '—'}</TableCell>
                <TableCell>{r?.diastolicBP ?? '—'}</TableCell>
                <TableCell>{r?.heartRate ?? '—'}</TableCell>
                <TableCell>{r?.spo2 ?? '—'}</TableCell>
                <TableCell>{r?.temperature ?? '—'}</TableCell>
                <TableCell>{r?.cvp ?? '—'}</TableCell>
                <TableCell>{r?.respiratoryRate ?? '—'}</TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
