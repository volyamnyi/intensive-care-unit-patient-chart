import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, Typography, Box } from '@mui/material';
import type { PrescriptionList } from '../../types';

interface PrescriptionTableProps {
  prescriptions: PrescriptionList[];
  onSelect?: (prescription: PrescriptionList) => void;
  loading?: boolean;
}

const statusColors: Record<string, 'default' | 'success' | 'info' | 'warning'> = {
  Saved: 'info',
  Finished: 'success',
};

const statusLabels: Record<string, string> = {
  Saved: 'Збережено',
  Finished: 'Закрито',
};

export default function PrescriptionTable({ prescriptions, onSelect, loading }: PrescriptionTableProps) {
  if (loading) {
    return <Typography color="text.secondary">Завантаження...</Typography>;
  }

  if (prescriptions.length === 0) {
    return <Typography color="text.secondary">Немає призначень</Typography>;
  }

  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small" sx={{ minWidth: 600 }}>
        <TableHead>
          <TableRow>
            <TableCell>Документ</TableCell>
            <TableCell>Пацієнт ID</TableCell>
            <TableCell>Статус</TableCell>
            <TableCell></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {prescriptions.map((prescription) => (
            <TableRow
              key={prescription.id}
              hover={!!onSelect}
              onClick={() => onSelect?.(prescription)}
              sx={{ cursor: onSelect ? 'pointer' : 'default' }}
            >
              <TableCell sx={{ fontWeight: 600 }}>{prescription.documentName}</TableCell>
              <TableCell>{prescription.patientId}</TableCell>
              <TableCell>
                <Chip
                  label={statusLabels[prescription.status] || prescription.status}
                  color={statusColors[prescription.status] || 'default'}
                  size="small"
                />
              </TableCell>
              <TableCell>
                {onSelect && (
                  <Box
                    sx={{ color: '#FF8C66', fontWeight: 600, fontSize: 13, cursor: 'pointer', minHeight: 44, display: 'flex', alignItems: 'center' }}
                    role="button"
                    tabIndex={0}
                    onClick={(e) => { e.stopPropagation(); onSelect(prescription); }}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.stopPropagation(); onSelect(prescription); } }}
                  >
                    Відкрити
                  </Box>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
