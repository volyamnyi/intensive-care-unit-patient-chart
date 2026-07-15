import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper } from '@mui/material';
import type { HourlyRecord } from '../../types';

interface HourlyRecordTableProps {
  records: HourlyRecord[];
  hours: number[];
}

export default function HourlyRecordTable({ records, hours }: HourlyRecordTableProps) {
  const getRec = (hour: number) => {
    const h = hour < 10 ? `0${hour}:00` : `${hour}:00`;
    return records.find((r) => r.recordTime.includes(h));
  };

  return (
    <TableContainer component={Paper} sx={{ border: '1px solid #2A2A2A', boxShadow: '0 2px 12px rgba(0,0,0,0.2)' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Година</TableCell>
            <TableCell>АТ сист</TableCell>
            <TableCell>АТ діас</TableCell>
            <TableCell>ЧСС</TableCell>
            <TableCell>SpO2</TableCell>
            <TableCell>Темп</TableCell>
            <TableCell>ЦВТ</TableCell>
            <TableCell>ЧД</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {hours.map((h) => {
            const r = getRec(h);
            const isPast = h < new Date().getHours();
            const bg = isPast && r ? '#1A3A2A' : isPast && !r ? '#3A1A1A' : 'inherit';
            return (
              <TableRow key={h} sx={{ bgcolor: bg }}>
                <TableCell sx={{ fontWeight: 600 }}>{h}:00</TableCell>
                <TableCell>{r?.systolicBP ?? '-'}</TableCell>
                <TableCell>{r?.diastolicBP ?? '-'}</TableCell>
                <TableCell>{r?.heartRate ?? '-'}</TableCell>
                <TableCell>{r?.spo2 ?? '-'}</TableCell>
                <TableCell>{r?.temperature ?? '-'}</TableCell>
                <TableCell>{r?.cvp ?? '-'}</TableCell>
                <TableCell>{r?.respiratoryRate ?? '-'}</TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
