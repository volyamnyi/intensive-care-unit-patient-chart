import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import type { HourlyRecord } from '../../types/icu';

interface HourlyRecordTableProps {
  records: HourlyRecord[];
  hours: number[];
}

function medDayPos(h: number): number { return h < 8 ? h + 24 : h; }

export default function HourlyRecordTable({ records, hours }: HourlyRecordTableProps) {
  const clockHour = new Date().getHours();
  const getRec = (hour: number) => {
    return records.find((r) => {
      const h = Number(String(r.recordTime).substring(11, 13));
      return h === hour;
    });
  };

  return (
    <div className="overflow-x-auto rounded-xl border border-border shadow-sm">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="font-rubik font-semibold">Година</TableHead>
            <TableHead className="font-rubik font-semibold">АТ сист.</TableHead>
            <TableHead className="font-rubik font-semibold">АТ діас.</TableHead>
            <TableHead className="font-rubik font-semibold">ЧСС</TableHead>
            <TableHead className="font-rubik font-semibold">SpO₂</TableHead>
            <TableHead className="font-rubik font-semibold">Темп.</TableHead>
            <TableHead className="font-rubik font-semibold">ЦВТ</TableHead>
            <TableHead className="font-rubik font-semibold">ЧД</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {hours.map((h) => {
            const r = getRec(h);
            const isPast = medDayPos(h) < medDayPos(clockHour);
            const bg = isPast && r ? 'bg-success/10 dark:bg-success/20'
              : isPast && !r ? 'bg-destructive/10 dark:bg-destructive/20'
              : '';
            return (
              <TableRow key={h} className={bg}>
                <TableCell className="font-semibold">{h}:00</TableCell>
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
    </div>
  );
}
