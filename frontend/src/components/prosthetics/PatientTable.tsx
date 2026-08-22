import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import type { ProstheticsPatient } from '@/prosthetics/types';

interface PatientTableProps {
  patients: ProstheticsPatient[];
  selectedId?: string | null;
  onSelect?: (patient: ProstheticsPatient) => void;
  loading?: boolean;
}

type SortKey = 'pib' | 'birthDate' | 'gender';

export default function PatientTable({ patients, selectedId, onSelect, loading }: PatientTableProps) {
  const [sortKey, setSortKey] = useState<SortKey>('pib');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  const toggleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  const sortedPatients = useMemo(() => {
    const list = [...patients];
    list.sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'pib':
          cmp = (a.pib ?? '').localeCompare(b.pib ?? '', 'uk');
          break;
        case 'birthDate':
          cmp = new Date(a.birthDate).getTime() - new Date(b.birthDate).getTime();
          break;
        case 'gender':
          cmp = (a.gender ?? '').localeCompare(b.gender ?? '', 'uk');
          break;
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return list;
  }, [patients, sortKey, sortDir]);

  if (loading) {
    return <p className="text-muted-foreground font-mulish">Завантаження...</p>;
  }

  if (sortedPatients.length === 0) {
    return <p className="text-muted-foreground font-mulish">Немає даних</p>;
  }

  const sortIndicator = (key: SortKey) => (sortKey === key ? (sortDir === 'asc' ? '↑' : '↓') : '');

  return (
    <div className="overflow-x-auto touch-pan-x">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>
              <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('pib')}>
                Пацієнт {sortIndicator('pib')}
              </Button>
            </TableHead>
            <TableHead>
              <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('birthDate')}>
                Дата народження {sortIndicator('birthDate')}
              </Button>
            </TableHead>
            <TableHead className="hidden sm:table-cell">
              <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('gender')}>
                Стать {sortIndicator('gender')}
              </Button>
            </TableHead>
            <TableHead className="text-right">Дія</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sortedPatients.map((patient) => (
            <TableRow
              key={patient.id}
              className={(onSelect ? 'cursor-pointer ' : '') + 'pointer-coarse:min-h-11'}
              onClick={() => onSelect?.(patient)}
            >
              <TableCell className="font-medium">{patient.pib}</TableCell>
              <TableCell className="whitespace-nowrap">
                {new Date(patient.birthDate).toLocaleDateString('uk-UA')}
              </TableCell>
              <TableCell className="hidden sm:table-cell">{patient.gender || '—'}</TableCell>
              <TableCell className="text-right">
                <Button
                  size="sm"
                  variant={selectedId === patient.id ? 'default' : 'outline'}
                  onClick={(e) => {
                    e.stopPropagation();
                    onSelect?.(patient);
                  }}
                >
                  {selectedId === patient.id ? 'Обрано' : 'Обрати'}
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
