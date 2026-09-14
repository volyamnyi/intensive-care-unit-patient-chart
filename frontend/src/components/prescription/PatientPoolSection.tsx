import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { FileText, RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableRow,
} from '@/components/ui/table';
import { patientApi } from '../../api/platform';
import { prescriptionApi } from '../../api/medication';
import { mapWithConcurrency, PRESCRIPTION_FANOUT_LIMIT } from '../../lib/async';
import type { PatientDto } from '../../types/core';
import type { PageResponse } from '../../types/core';
import type { PrescriptionList } from '../../types/medication';
import {
  MIS_STATUS_LABELS,
  getPatientStatusText,
  getPatientRowClasses,
} from './patientStatus';

const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 400;
const STATUS_FILTERS = ['', 'MOV', 'CMP', 'CNC', 'REJ'];

interface PatientPoolSectionProps {
  onOpenDrawer: (patient: PatientDto, lists: PrescriptionList[]) => void;
  storageKey: string;
}

function statusChipLabel(code: string): string {
  if (code === '') {
    return 'Усі';
  }
  return MIS_STATUS_LABELS[code] ?? code;
}

/**
 * «Всі пацієнти»: server-paginated MIS pool (any stay status) with per-page
 * list loading. One bulk pool fetch per page turn; at most PAGE_SIZE
 * follow-up list requests — never a roster-wide fan-out.
 */
export default function PatientPoolSection({ onOpenDrawer, storageKey }: PatientPoolSectionProps) {
  const [open, setOpen] = useState(() => localStorage.getItem(storageKey) === '1');
  const [query, setQuery] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<PatientDto> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [listsByPatient, setListsByPatient] = useState<Record<number, PrescriptionList[]>>({});
  const [listsLoading, setListsLoading] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    const timer = setTimeout(() => setAppliedQuery(query.trim()), SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [query]);

  const loadPage = useCallback(
    async (pageNumber: number, q: string, statusCode: string) => {
      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;
      setLoading(true);
      setError(null);
      try {
        const res = await patientApi.getPatientPool(
          { query: q === '' ? undefined : q, status: statusCode === '' ? undefined : statusCode, page: pageNumber, size: PAGE_SIZE },
          controller.signal,
        );
        if (controller.signal.aborted) {
          return;
        }
        setData(res.data);
      } catch {
        if (!controller.signal.aborted) {
          setError('Не вдалося завантажити пул пацієнтів');
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    },
    [],
  );

  useEffect(() => {
    if (!open) {
      return;
    }
    void loadPage(page, appliedQuery, status);
    return () => {
      abortRef.current?.abort();
    };
  }, [open, page, appliedQuery, status, loadPage]);

  const pagePatients = useMemo(() => data?.content ?? [], [data]);
  const totalPages = data?.totalPages ?? 0;

  useEffect(() => {
    if (!open || pagePatients.length === 0) {
      return;
    }
    let cancelled = false;
    setListsLoading(true);
    const controller = new AbortController();
    (async () => {
      try {
        const results = await mapWithConcurrency(
          pagePatients,
          PRESCRIPTION_FANOUT_LIMIT,
          async (patient, signal) => {
            try {
              const lr = await prescriptionApi.getByPatient(patient.id, signal);
              return [patient.id, lr.data] as const;
            } catch (err) {
              if (signal?.aborted) {
                throw err;
              }
              return null;
            }
          },
          controller.signal,
        );
        if (cancelled || controller.signal.aborted) {
          return;
        }
        const next: Record<number, PrescriptionList[]> = {};
        for (const row of results) {
          if (row !== null) {
            next[row[0]] = row[1];
          }
        }
        setListsByPatient(next);
      } catch {
        if (!cancelled && !controller.signal.aborted) {
          setListsByPatient({});
        }
      } finally {
        if (!cancelled) {
          setListsLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [open, pagePatients]);

  const selectStatus = (code: string) => {
    setStatus(code);
    setPage(0);
  };

  const applySearch = (value: string) => {
    setQuery(value);
    setPage(0);
  };

  return (
    <div className="mt-4">
      <Button
        variant={open ? 'default' : 'outline'}
        size="sm"
        aria-pressed={open}
        onClick={() => {
          setOpen(v => {
            localStorage.setItem(storageKey, v ? '0' : '1');
            return !v;
          });
        }}
      >
        Всі пацієнти
      </Button>
      {open && (
        <div className="mt-2">
          <div className="mb-2 flex flex-wrap items-center gap-2">
            <Input
              placeholder="Пошук за ПІБ, ID або телефоном"
              value={query}
              onChange={e => applySearch(e.target.value)}
              className="max-w-[260px]"
            />
            <div className="flex items-center gap-1" role="group" aria-label="Статус">
              {STATUS_FILTERS.map(code => (
                <Button
                  key={code === '' ? 'all' : code}
                  variant={status === code ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => selectStatus(code)}
                >
                  {statusChipLabel(code)}
                </Button>
              ))}
            </div>
          </div>
          {loading ? (
            <div className="flex flex-col gap-2" data-testid="pool-loading">
              {[0, 1, 2].map(i => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : error ? (
            <Alert variant="destructive" className="mb-2">
              <AlertDescription>{error}</AlertDescription>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => void loadPage(page, appliedQuery, status)}
              >
                <RotateCcw className="mr-1 size-4" />
                Спробувати ще
              </Button>
            </Alert>
          ) : pagePatients.length === 0 ? (
            <div className="py-2 text-muted-foreground">Пацієнтів не знайдено</div>
          ) : (
            <>
              <Table>
                <TableBody>
                  {pagePatients.map(patient => {
                    const lists = listsByPatient[patient.id] ?? [];
                    return (
                      <TableRow
                        key={patient.id}
                        className={getPatientRowClasses(lists)}
                      >
                        <TableCell>{patient.id}</TableCell>
                        <TableCell>
                          <span className="font-semibold">{patient.fullName}</span>
                        </TableCell>
                        <TableCell>{patient.room || '—'}</TableCell>
                        <TableCell>{patient.bed || '—'}</TableCell>
                        <TableCell>{patient.doctorName || '—'}</TableCell>
                        <TableCell>
                          {getPatientStatusText(patient.patientStatus, lists)}
                        </TableCell>
                        <TableCell>
                          <Button
                            size="sm"
                            className="rounded-full px-1.5 text-[0.8125rem] font-semibold normal-case"
                            variant="outline"
                            onClick={() => onOpenDrawer(patient, lists)}
                          >
                            <FileText className="mr-1 size-4" />
                            Відкрити
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
              <div className="mt-2 flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0 || loading || listsLoading}
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                >
                  ← Назад
                </Button>
                <span className="text-sm text-muted-foreground">
                  Сторінка {page + 1} з {Math.max(totalPages, 1)}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={loading || listsLoading || page + 1 >= totalPages}
                  onClick={() => setPage(p => p + 1)}
                >
                  Далі →
                </Button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
