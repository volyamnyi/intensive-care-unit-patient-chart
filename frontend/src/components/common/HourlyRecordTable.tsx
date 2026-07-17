import { useTranslation } from 'react-i18next';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, useTheme } from '@mui/material';
import type { HourlyRecord } from '../../types';

interface HourlyRecordTableProps {
  records: HourlyRecord[];
  hours: number[];
}

export default function HourlyRecordTable({ records, hours }: HourlyRecordTableProps) {
  const { t } = useTranslation();
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const getRec = (hour: number) => {
    return records.find((r) => {
      const h = Number(String(r.recordTime).substring(11, 13));
      return h === hour;
    });
  };

  return (
    <TableContainer component={Paper} sx={{ border: `1px solid ${isDark ? '#2A2A2A' : '#D0CEC9'}`, boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 8px rgba(0,0,0,0.04)' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>{t('hourlyRecordTable.columns.hour')}</TableCell>
            <TableCell>{t('hourlyRecordTable.columns.systolicBP')}</TableCell>
            <TableCell>{t('hourlyRecordTable.columns.diastolicBP')}</TableCell>
            <TableCell>{t('hourlyRecordTable.columns.heartRate')}</TableCell>
            <TableCell>{t('hourlyRecordTable.columns.spo2')}</TableCell>
            <TableCell>{t('hourlyRecordTable.columns.temperature')}</TableCell>
            <TableCell>{t('hourlyRecordTable.columns.cvp')}</TableCell>
            <TableCell>{t('hourlyRecordTable.columns.respiratoryRate')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {hours.map((h) => {
            const r = getRec(h);
            const isPast = h < new Date().getHours();
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
                <TableCell>{r?.systolicBP ?? t('hourlyRecordTable.emptyValue')}</TableCell>
                <TableCell>{r?.diastolicBP ?? t('hourlyRecordTable.emptyValue')}</TableCell>
                <TableCell>{r?.heartRate ?? t('hourlyRecordTable.emptyValue')}</TableCell>
                <TableCell>{r?.spo2 ?? t('hourlyRecordTable.emptyValue')}</TableCell>
                <TableCell>{r?.temperature ?? t('hourlyRecordTable.emptyValue')}</TableCell>
                <TableCell>{r?.cvp ?? t('hourlyRecordTable.emptyValue')}</TableCell>
                <TableCell>{r?.respiratoryRate ?? t('hourlyRecordTable.emptyValue')}</TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
