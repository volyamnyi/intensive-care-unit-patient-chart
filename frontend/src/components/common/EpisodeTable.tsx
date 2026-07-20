import { useTranslation } from 'react-i18next';
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

export default function EpisodeTable({ episodes, onSelect, loading }: EpisodeTableProps) {
  const { t } = useTranslation();
  const statusLabels: Record<string, string> = {
    DRAFT: t('episodeTable.statusDraft'),
    ACTIVE: t('episodeTable.statusActive'),
    COMPLETED: t('episodeTable.statusCompleted'),
    ARCHIVED: t('episodeTable.statusArchived'),
  };

  if (loading) {
    return <Typography color="text.secondary">{t('episodeTable.loading')}</Typography>;
  }

  if (episodes.length === 0) {
    return <Typography color="text.secondary">{t('episodeTable.empty')}</Typography>;
  }

  return (
    <TableContainer>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>{t('episodeTable.tableHeaders.patient')}</TableCell>
            <TableCell>Ward/Bed</TableCell>
            <TableCell>Diagnosis</TableCell>
            <TableCell>{t('episodeTable.tableHeaders.admissionDate')}</TableCell>
            <TableCell>{t('episodeTable.tableHeaders.dischargeDate')}</TableCell>
            <TableCell>{t('episodeTable.tableHeaders.status')}</TableCell>
            <TableCell>{t('episodeTable.tableHeaders.actions')}</TableCell>
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
              <TableCell sx={{ fontSize: 12 }}>
                {[ep.ward, ep.bedNumber].filter(Boolean).join(' / ') || '—'}
              </TableCell>
              <TableCell sx={{ fontSize: 12, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {ep.admissionDiagnosis || '—'}
              </TableCell>
              <TableCell>
                {new Date(ep.admissionDate).toLocaleDateString('uk-UA')}
              </TableCell>
              <TableCell>
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
                    sx={{ color: '#FF8C66', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}
                    role="button"
                    tabIndex={0}
                    onClick={(e) => { e.stopPropagation(); onSelect(ep); }}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.stopPropagation(); onSelect(ep); } }}
                  >
                    {t('episodeTable.openAction')}
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
