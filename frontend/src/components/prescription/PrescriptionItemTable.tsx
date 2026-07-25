import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, IconButton, Typography } from '@mui/material';
import { Delete } from '@mui/icons-material';
import type { PrescriptionItem } from '../../types';

interface PrescriptionItemTableProps {
  items: PrescriptionItem[];
  onDelete?: (item: PrescriptionItem) => void;
  canEdit?: boolean;
}

export default function PrescriptionItemTable({ items, onDelete, canEdit }: PrescriptionItemTableProps) {
  if (items.length === 0) {
    return <Typography color="text.secondary">Немає препаратів у призначенні</Typography>;
  }

  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small" sx={{ minWidth: 500 }}>
        <TableHead>
          <TableRow>
            <TableCell>Препарат</TableCell>
            <TableCell>Метод</TableCell>
            <TableCell>Режим</TableCell>
            <TableCell>Статус</TableCell>
            {canEdit && onDelete && <TableCell></TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {items.map((item) => (
            <TableRow key={item.id}>
              <TableCell sx={{ fontWeight: 600 }}>{item.medicineName}</TableCell>
              <TableCell>{item.medicineMethod || '—'}</TableCell>
              <TableCell>{item.regime || '—'}</TableCell>
              <TableCell>{item.status}</TableCell>
              {canEdit && onDelete && (
                <TableCell>
                  <IconButton
                    size="small"
                    aria-label={`Видалити ${item.medicineName}`}
                    onClick={() => onDelete(item)}
                  >
                    <Delete />
                  </IconButton>
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
