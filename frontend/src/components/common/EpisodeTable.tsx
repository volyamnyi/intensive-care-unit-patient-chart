import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import type { Episode } from '../../types';

interface EpisodeTableProps {
  episodes: Episode[];
  onSelect?: (episode: Episode) => void;
  loading?: boolean;
}

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
  if (loading) {
    return <p className="text-muted-foreground font-mulish">Завантаження...</p>;
  }

  if (episodes.length === 0) {
    return <p className="text-muted-foreground font-mulish">Немає даних</p>;
  }

  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Пацієнт</TableHead>
            <TableHead className="hidden sm:table-cell">Палата/Ліжко</TableHead>
            <TableHead className="hidden md:table-cell">Діагноз</TableHead>
            <TableHead>Дата госпіталізації</TableHead>
            <TableHead className="hidden sm:table-cell">Дата виписки</TableHead>
            <TableHead>Статус</TableHead>
            <TableHead>Дії</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {episodes.map((ep) => (
            <TableRow
              key={ep.id}
              className={onSelect ? 'cursor-pointer' : ''}
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
