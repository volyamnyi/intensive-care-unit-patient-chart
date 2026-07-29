import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';

interface PrescriptionList {
  id: string;
  documentName: string;
  patientId: string;
  status: string;
}

interface PrescriptionTableProps {
  prescriptions: PrescriptionList[];
  onSelect?: (prescription: PrescriptionList) => void;
  loading?: boolean;
}

const statusVariant: Record<string, 'secondary' | 'default' | 'outline'> = {
  Saved: 'secondary',
  Finished: 'default',
};

const statusLabels: Record<string, string> = {
  Saved: 'Збережено',
  Finished: 'Закрито',
};

export default function PrescriptionTable({ prescriptions, onSelect, loading }: PrescriptionTableProps) {
  if (loading) {
    return <p className="text-muted-foreground">Завантаження...</p>;
  }

  if (prescriptions.length === 0) {
    return <p className="text-muted-foreground">Немає призначень</p>;
  }

  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Документ</TableHead>
            <TableHead>Пацієнт ID</TableHead>
            <TableHead>Статус</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {prescriptions.map((prescription) => (
            <TableRow
              key={prescription.id}
              className={onSelect ? 'cursor-pointer hover:bg-accent' : ''}
              onClick={() => onSelect?.(prescription)}
            >
              <TableCell className="font-semibold">{prescription.documentName}</TableCell>
              <TableCell>{prescription.patientId}</TableCell>
              <TableCell>
                <Badge variant={statusVariant[prescription.status] || 'outline'}>
                  {statusLabels[prescription.status] || prescription.status}
                </Badge>
              </TableCell>
              <TableCell>
                {onSelect && (
                  <span
                    className="text-primary font-semibold text-xs cursor-pointer flex items-center min-h-11"
                    role="button"
                    tabIndex={0}
                    onClick={(e) => { e.stopPropagation(); onSelect(prescription); }}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.stopPropagation(); onSelect(prescription); } }}
                  >
                    Відкрити
                  </span>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
