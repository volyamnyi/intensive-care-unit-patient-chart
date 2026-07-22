import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, Typography } from '@mui/material';
import type { AuditLog } from '../../types';

interface AuditLogTableProps {
  logs: AuditLog[];
  loading?: boolean;
}

const actionColors: Record<string, 'default' | 'success' | 'error' | 'info' | 'warning'> = {
  CREATE: 'success',
  UPDATE: 'info',
  DELETE: 'error',
  ACTION: 'warning',
};

export default function AuditLogTable({ logs, loading }: AuditLogTableProps) {
  if (loading) {
    return <Typography color="text.secondary">Завантаження...</Typography>;
  }

  if (logs.length === 0) {
    return <Typography color="text.secondary">Немає записів аудиту</Typography>;
  }

  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small" sx={{ minWidth: 600 }}>
        <TableHead>
          <TableRow>
            <TableCell>Час</TableCell>
            <TableCell>Користувач</TableCell>
            <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>Сутність</TableCell>
            <TableCell>Дія</TableCell>
            <TableCell sx={{ display: { xs: 'none', md: 'table-cell' } }}>Зміни</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {logs.map((log) => (
            <TableRow key={log.id}>
              <TableCell sx={{ whiteSpace: 'nowrap' }}>
                {new Date(log.timestamp).toLocaleString('uk-UA')}
              </TableCell>
              <TableCell>{log.userId ?? '-'}</TableCell>
              <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>
                {log.entity}
                {log.entityId && <Typography variant="caption" color="text.secondary" sx={{ ml: 0.5 }}>#{log.entityId.slice(0, 8)}</Typography>}
              </TableCell>
              <TableCell>
                <Chip
                  label={log.action}
                  color={actionColors[log.action] || 'default'}
                  size="small"
                />
              </TableCell>
              <TableCell sx={{ display: { xs: 'none', md: 'table-cell' } }}>
                <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
                  {log.oldValue && `- ${log.oldValue}`}
                  {log.newValue && `\n+ ${log.newValue}`}
                </Typography>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
