import { useMemo, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import type { Episode } from '../../types/icu';

interface EpisodeTableProps {
  episodes: Episode[];
  onSelect?: (episode: Episode) => void;
  loading?: boolean;
}

type SortKey = 'patientName' | 'ward' | 'admissionDiagnosis' | 'admissionDate' | 'dischargeDate' | 'status';

const statusColors: Record<string, 'default' | 'secondary' | 'destructive' | 'outline' | 'ghost' | 'link'> = {
  DRAFT: 'default',
  ACTIVE: 'default',
  COMPLETED: 'secondary',
  ARCHIVED: 'outline',
};

const statusLabels: Record<string, string> = {
  DRAFT: 'Чернетка',
  ACTIVE: 'Активний',
  COMPLETED: 'Завершений',
  ARCHIVED: 'Архівний',
};

export default function EpisodeTable({ episodes, onSelect, loading }: EpisodeTableProps) {
  const [sortKey, setSortKey] = useState<SortKey>('admissionDate');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');

  const toggleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir(d => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  const sortedEpisodes = useMemo(() => {
    const list = [...episodes];
    list.sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'patientName':
          cmp = (a.patientName ?? '').localeCompare(b.patientName ?? '', 'uk');
          break;
        case 'ward': {
          const wardA = [a.ward, a.bedNumber].filter(Boolean).join(' / ');
          const wardB = [b.ward, b.bedNumber].filter(Boolean).join(' / ');
          cmp = wardA.localeCompare(wardB, 'uk');
          break;
        }
        case 'admissionDiagnosis':
          cmp = (a.admissionDiagnosis ?? '').localeCompare(b.admissionDiagnosis ?? '', 'uk');
          break;
        case 'admissionDate':
          cmp = new Date(a.admissionDate).getTime() - new Date(b.admissionDate).getTime();
          break;
        case 'dischargeDate': {
          const da = a.dischargeDate ? new Date(a.dischargeDate).getTime() : Number.MAX_SAFE_INTEGER;
          const db = b.dischargeDate ? new Date(b.dischargeDate).getTime() : Number.MAX_SAFE_INTEGER;
          cmp = da - db;
          break;
        }
        case 'status':
          cmp = (statusLabels[a.status] || a.status).localeCompare(statusLabels[b.status] || b.status, 'uk');
          break;
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return list;
  }, [episodes, sortKey, sortDir]);

  if (loading) {
    return <p className="text-muted-foreground font-mulish">Завантаження...</p>;
  }

  if (sortedEpisodes.length === 0) {
    return <p className="text-muted-foreground font-mulish">Немає даних</p>;
  }

  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>
              <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('patientName')}>
                Пацієнт {sortKey === 'patientName' && (sortDir === 'asc' ? '↑' : '↓')}
              </Button>
            </TableHead>
            <TableHead className="hidden sm:table-cell">
              <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('ward')}>
                Палата/Ліжко {sortKey === 'ward' && (sortDir === 'asc' ? '↑' : '↓')}
              </Button>
            </TableHead>
            <TableHead className="hidden md:table-cell">
              <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('admissionDiagnosis')}>
                Діагноз {sortKey === 'admissionDiagnosis' && (sortDir === 'asc' ? '↑' : '↓')}
              </Button>
            </TableHead>
            <TableHead>
              <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('admissionDate')}>
                Дата госпіталізації {sortKey === 'admissionDate' && (sortDir === 'asc' ? '↑' : '↓')}
              </Button>
            </TableHead>
            <TableHead className="hidden sm:table-cell">
              <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('dischargeDate')}>
                Дата виписки {sortKey === 'dischargeDate' && (sortDir === 'asc' ? '↑' : '↓')}
              </Button>
            </TableHead>
            <TableHead>
              <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('status')}>
                Статус {sortKey === 'status' && (sortDir === 'asc' ? '↑' : '↓')}
              </Button>
            </TableHead>
            <TableHead>Дії</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sortedEpisodes.map((ep) => (
            <TableRow
              key={ep.id}
              className={(onSelect ? 'cursor-pointer ' : '') + 'pointer-coarse:min-h-11'}
              onClick={() => onSelect?.(ep)}
            >
              <TableCell className="font-semibold">
                {ep.patientName || ep.patientId}
              </TableCell>
              <TableCell className="hidden text-xs sm:table-cell">
                {[ep.ward, ep.bedNumber].filter(Boolean).join(' / ') || '—'}
              </TableCell>
              <TableCell className="hidden max-w-[200px] truncate text-xs md:table-cell">
                {ep.admissionDiagnosis || '—'}
              </TableCell>
              <TableCell className="whitespace-nowrap">
                {new Date(ep.admissionDate).toLocaleDateString('uk-UA')}
              </TableCell>
              <TableCell className="hidden whitespace-nowrap sm:table-cell">
                {ep.dischargeDate ? new Date(ep.dischargeDate).toLocaleDateString('uk-UA') : '-'}
              </TableCell>
              <TableCell>
                <Badge variant={statusColors[ep.status] || 'default'}>
                  {statusLabels[ep.status] || ep.status}
                </Badge>
              </TableCell>
              <TableCell>
                {onSelect && (
                  <button
                    type="button"
                    className="flex min-h-[44px] cursor-pointer items-center text-[13px] font-semibold border-none bg-transparent p-0"
                    style={{ color: '#FF8C66' }}
                    onClick={(e) => { e.stopPropagation(); onSelect(ep); }}
                  >
                    Відкрити
                  </button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
