import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, Typography, Box } from '@mui/material';
import type { Episode } from '../../types';

interface EpisodeTableProps {
  episodes: Episode[];
  onSelect?: (episode: Episode) => void;
  loading?: boolean;
}

const statusColors: Record<string, 'default' | 'success' | 'info' | 'warning'> = {
  DRAFT: 'default',
  ACTIVE: 'success',
  COMPLETED: 'info',
  ARCHIVED: 'warning',
};

const statusLabels: Record<string, string> = {
  DRAFT: 'Чернетка',
  ACTIVE: 'Активний',
  COMPLETED: 'Завершений',
  ARCHIVED: 'Архівний',
};

export default function EpisodeTable({ episodes, onSelect, loading }: EpisodeTableProps) {
  if (loading) {
    return <Typography color="text.secondary">Завантаження...</Typography>;
  }

  if (episodes.length === 0) {
    return <Typography color="text.secondary">Немає даних</Typography>;
  }

  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small" sx={{ minWidth: 650 }}>
        <TableHead>
          <TableRow>
            <TableCell>Пацієнт</TableCell>
            <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>Палата/Ліжко</TableCell>
            <TableCell sx={{ display: { xs: 'none', md: 'table-cell' } }}>Діагноз</TableCell>
            <TableCell>Дата госпіталізації</TableCell>
            <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>Дата виписки</TableCell>
            <TableCell>Статус</TableCell>
            <TableCell>Дії</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {episodes.map((ep) => (
            <TableRow
              key={ep.id}
              hover={!!onSelect}
              onClick={() => onSelect?.(ep)}
              sx={{ cursor: onSelect ? 'pointer' : 'default' }}
            >
              <TableCell sx={{ fontWeight: 600 }}>
                {ep.patientName || ep.patientId}
              </TableCell>
              <TableCell sx={{ fontSize: 12, display: { xs: 'none', sm: 'table-cell' } }}>
                {[ep.ward, ep.bedNumber].filter(Boolean).join(' / ') || '—'}
              </TableCell>
              <TableCell sx={{ fontSize: 12, display: { xs: 'none', md: 'table-cell' }, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {ep.admissionDiagnosis || '—'}
              </TableCell>
              <TableCell sx={{ whiteSpace: 'nowrap' }}>
                {new Date(ep.admissionDate).toLocaleDateString('uk-UA')}
              </TableCell>
              <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' }, whiteSpace: 'nowrap' }}>
                {ep.dischargeDate ? new Date(ep.dischargeDate).toLocaleDateString('uk-UA') : '-'}
              </TableCell>
              <TableCell>
                <Chip
                  label={statusLabels[ep.status] || ep.status}
                  color={statusColors[ep.status] || 'default'}
                  size="small"
                />
              </TableCell>
              <TableCell>
                {onSelect && (
                  <Box
                    sx={{ color: '#FF8C66', fontWeight: 600, fontSize: 13, cursor: 'pointer', minHeight: 44, display: 'flex', alignItems: 'center' }}
                    role="button"
                    tabIndex={0}
                    onClick={(e) => { e.stopPropagation(); onSelect(ep); }}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.stopPropagation(); onSelect(ep); } }}
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
