import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Trash2 } from 'lucide-react';
import type { PrescriptionItem } from '../../types/medication';

interface PrescriptionItemTableProps {
  items: PrescriptionItem[];
  onDelete?: (item: PrescriptionItem) => void;
  canEdit?: boolean;
}

export default function PrescriptionItemTable({ items, onDelete, canEdit }: PrescriptionItemTableProps) {
  if (items.length === 0) {
    return <p className="text-muted-foreground">Немає препаратів у призначенні</p>;
  }

  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Препарат</TableHead>
            <TableHead>Метод</TableHead>
            <TableHead>Режим</TableHead>
            <TableHead>Статус</TableHead>
            {canEdit && onDelete && <TableHead></TableHead>}
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((item) => (
            <TableRow key={item.id}>
              <TableCell className="font-semibold">{item.medicineName}</TableCell>
              <TableCell>{item.medicineMethod || '—'}</TableCell>
              <TableCell>{item.regime || '—'}</TableCell>
              <TableCell>{item.status}</TableCell>
              {canEdit && onDelete && (
                <TableCell>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Видалити ${item.medicineName}`}
                    onClick={() => onDelete(item)}
                  >
                    <Trash2 className="size-4" />
                  </Button>
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
