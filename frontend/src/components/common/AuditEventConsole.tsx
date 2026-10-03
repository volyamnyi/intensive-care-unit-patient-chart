import { useCallback, useState } from 'react';
import { History, Search, ChevronLeft, ChevronRight, ShieldCheck, ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { auditEventsApi } from '../../api/platform';
import { getErrorMessage } from '../../utils/errorMessage';
import type { AuditEventDetail, AuditEventSummary, AuditObjectHistory } from '../../types/core';

const MODULES = ['platform', 'icu', 'medication', 'prosthetics'];
const OUTCOMES = ['SUCCESS', 'FAILURE', 'DENIED', 'PARTIAL', 'CANCELLED'];
const PAGE_SIZE = 20;

const outcomeVariantMap: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  SUCCESS: 'default',
  FAILURE: 'destructive',
  DENIED: 'destructive',
  PARTIAL: 'secondary',
  CANCELLED: 'outline',
};

function formatTime(iso: string) {
  return new Date(iso).toLocaleString('uk-UA');
}

export function AuditEventDetailCard({ detail, onSelect }: {
  detail: AuditEventDetail;
  onSelect: (auditId: string) => void;
}) {
  return (
    <div className="mt-2 rounded-lg border p-2.5" data-testid="audit-event-detail">
      <div className="flex flex-wrap items-center gap-1.5">
        <Badge variant={outcomeVariantMap[detail.outcome] || 'default'}>{detail.outcome}</Badge>
        <span className="font-mono text-xs">{detail.action}</span>
        <span className="text-xs text-muted-foreground">
          {detail.module} · {detail.functionalArea} · {formatTime(detail.occurredAt)}
        </span>
        {detail.integrityVerified ? (
          <Badge variant="default" className="gap-1">
            <ShieldCheck className="size-3" /> цілісність підтверджено
          </Badge>
        ) : (
          <Badge variant="destructive" className="gap-1">
            <ShieldAlert className="size-3" /> цілісність НЕ підтверджено
          </Badge>
        )}
      </div>
      <div className="mt-1.5 grid gap-1 text-xs sm:grid-cols-2">
        <div>Актор: <b>{detail.actorLogin ?? detail.actorType}</b>{detail.actorDisplayName ? ` (${detail.actorDisplayName})` : ''}</div>
        <div>Джерело: <b>{detail.source ?? '—'}</b>{detail.httpMethod ? ` · ${detail.httpMethod} ${detail.routeTemplate ?? ''}` : ''}</div>
        <div>Ціль: <b>{detail.targetType ?? '—'}{detail.targetId ? ` #${detail.targetId.slice(0, 8)}` : ''}</b></div>
        <div>Кореляція: <span className="font-mono">{detail.correlationId?.slice(0, 8) ?? '—'}</span> · Дія UI: <span className="font-mono">{detail.userActionId?.slice(0, 8) ?? '—'}</span></div>
        {detail.errorCode && <div>Помилка: <b className="font-mono">{detail.errorCode}</b></div>}
        {detail.parentAuditId && (
          <div>Батьківська подія:{' '}
            <button type="button" className="font-mono text-primary underline" onClick={() => onSelect(detail.parentAuditId as string)}>
              {detail.parentAuditId.slice(0, 8)}
            </button>
          </div>
        )}
      </div>
      {detail.changes.length > 0 && (
        <div className="mt-1.5">
          <div className="text-xs font-medium">Зміни{!detail.restrictedDetail && ' (значення приховано — потрібен AUDIT_SECURITY_ACCESS)'}:</div>
          <ul className="mt-0.5 space-y-0.5 text-xs">
            {detail.changes.map((c, i) => (
              <li key={i} className="font-mono">
                {c.field} [{c.type}]
                {c.valuesRedacted ? ' — ***' : (
                  <>{c.oldValue != null && <> − {String(c.oldValue)}</>}{c.newValue != null && <> + {String(c.newValue)}</>}</>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
      {detail.targets.length > 0 && (
        <div className="mt-1.5 text-xs">
          <span className="font-medium">Повʼязані обʼєкти: </span>
          {detail.targets.map((t, i) => (
            <span key={i} className="mr-1.5">
              <Badge variant="outline">{t.relationType}</Badge> {t.entityType} #{t.entityId.slice(0, 8)}
            </span>
          ))}
        </div>
      )}
      {detail.children.length > 0 && (
        <div className="mt-1.5 text-xs">
          <span className="font-medium">Дочірні події ({detail.children.length}): </span>
          {detail.children.map((c) => (
            <button key={c.auditId} type="button" className="mr-1.5 font-mono text-primary underline" onClick={() => onSelect(c.auditId)}>
              {c.action} #{c.auditId.slice(0, 8)}
            </button>
          ))}
        </div>
      )}
      {detail.externalCalls.length > 0 && (
        <div className="mt-1.5 text-xs">
          <span className="font-medium">Зовнішні виклики: </span>
          {detail.externalCalls.map((call, i) => (
            <span key={i} className="mr-1.5 font-mono">{call.integration}.{call.operation} [{call.outcome || '—'}]</span>
          ))}
        </div>
      )}
    </div>
  );
}

export function AuditObjectHistoryView({ history, onSelect }: {
  history: AuditObjectHistory;
  onSelect: (auditId: string) => void;
}) {
  return (
    <div className="mt-2 rounded-lg border p-2.5" data-testid="audit-object-history">
      <div className="text-sm font-medium">
        Історія {history.entityType} #{history.entityId.slice(0, 8)} — подій: {history.eventCount}
      </div>
      {history.events.length === 0 ? (
        <p className="mt-1 text-sm text-muted-foreground">Подій не знайдено</p>
      ) : (
        <ol className="mt-1.5 space-y-1">
          {history.events.map((e) => (
            <li key={e.auditId} className="flex flex-wrap items-center gap-1.5 text-xs">
              <span className="text-muted-foreground">{formatTime(e.occurredAt)}</span>
              <Badge variant={e.parentAuditId ? 'secondary' : 'default'}>
                {e.parentAuditId ? 'дочірня' : 'коренева'}
              </Badge>
              <button type="button" className="font-mono text-primary underline" onClick={() => onSelect(e.auditId)}>
                {e.action}
              </button>
              <span className="text-muted-foreground">{e.actorLogin ?? e.actorType} · {e.outcome}</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

interface Filters {
  module: string;
  action: string;
  outcome: string;
  actorLogin: string;
  correlationId: string;
  occurredFrom: string;
  occurredTo: string;
}

const EMPTY_FILTERS: Filters = {
  module: '', action: '', outcome: '', actorLogin: '', correlationId: '', occurredFrom: '', occurredTo: '',
};

/**
 * Audit v2 console (F7): combined-filter search with stable pagination,
 * event detail card (diff, relations, children, integrity) and object
 * history lookup. Console access itself is audited server-side (P12–P13).
 */
export default function AuditEventConsole() {
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);
  const [rows, setRows] = useState<AuditEventSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searched, setSearched] = useState(false);
  const [detail, setDetail] = useState<AuditEventDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [history, setHistory] = useState<AuditObjectHistory | null>(null);
  const [historyType, setHistoryType] = useState('');
  const [historyId, setHistoryId] = useState('');

  const set = (key: keyof Filters) => (
    e: React.ChangeEvent<HTMLInputElement>,
  ) => setFilters((prev) => ({ ...prev, [key]: e.target.value }));

  const search = useCallback(async (nextPage = 0) => {
    setLoading(true);
    setError(null);
    try {
      const res = await auditEventsApi.search({
        module: filters.module || undefined,
        action: filters.action || undefined,
        outcome: filters.outcome || undefined,
        actorLogin: filters.actorLogin || undefined,
        correlationId: filters.correlationId || undefined,
        occurredFrom: filters.occurredFrom ? new Date(filters.occurredFrom).toISOString() : undefined,
        occurredTo: filters.occurredTo ? new Date(filters.occurredTo).toISOString() : undefined,
        page: nextPage,
        size: PAGE_SIZE,
      });
      const data = res.data;
      setRows(data.content ?? []);
      setPage(data.number ?? nextPage);
      setTotalPages(data.totalPages ?? 0);
      setTotalElements(data.totalElements ?? 0);
      setSearched(true);
      setDetail(null);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити події аудиту'));
    } finally {
      setLoading(false);
    }
  }, [filters]);

  const loadDetail = useCallback(async (auditId: string) => {
    setDetailLoading(true);
    try {
      const res = await auditEventsApi.detail(auditId);
      setDetail(res.data);
      setHistory(null);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити картку події'));
    } finally {
      setDetailLoading(false);
    }
  }, []);

  const loadHistory = useCallback(async () => {
    if (!historyType || !historyId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await auditEventsApi.objectHistory(historyType, historyId);
      setHistory(res.data);
      setDetail(null);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити історію обʼєкта'));
    } finally {
      setLoading(false);
    }
  }, [historyType, historyId]);

  return (
    <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2.5" data-testid="audit-event-console">
      <div className="mb-1.5 flex items-center gap-1.5">
        <History className="size-4" />
        <h2 className="font-rubik text-base font-medium">Події (Audit v2)</h2>
      </div>
      <div className="flex flex-wrap gap-1">
        <Select value={filters.module || 'all'} onValueChange={(v) => setFilters((p) => ({ ...p, module: v === 'all' ? '' : v }))}>
          <SelectTrigger className="w-full sm:w-[160px]" aria-label="Модуль">
            <SelectValue placeholder="Модуль" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Всі модулі</SelectItem>
            {MODULES.map((m) => <SelectItem key={m} value={m}>{m}</SelectItem>)}
          </SelectContent>
        </Select>
        <Input placeholder="Дія (точний код)" value={filters.action} onChange={set('action')} className="w-full sm:w-[220px]" />
        <Select value={filters.outcome || 'all'} onValueChange={(v) => setFilters((p) => ({ ...p, outcome: v === 'all' ? '' : v }))}>
          <SelectTrigger className="w-full sm:w-[160px]" aria-label="Результат">
            <SelectValue placeholder="Результат" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Будь-який результат</SelectItem>
            {OUTCOMES.map((o) => <SelectItem key={o} value={o}>{o}</SelectItem>)}
          </SelectContent>
        </Select>
        <Input placeholder="Логін актора" value={filters.actorLogin} onChange={set('actorLogin')} className="w-full sm:w-[170px]" />
        <Input placeholder="Correlation ID" value={filters.correlationId} onChange={set('correlationId')} className="w-full sm:w-[220px] font-mono" />
        <Input type="datetime-local" aria-label="Період з" value={filters.occurredFrom} onChange={set('occurredFrom')} className="w-full sm:w-[200px]" />
        <Input type="datetime-local" aria-label="Період по" value={filters.occurredTo} onChange={set('occurredTo')} className="w-full sm:w-[200px]" />
        <Button size="sm" onClick={() => search(0)} disabled={loading}>
          <Search className="mr-1 size-4" /> Пошук
        </Button>
      </div>
      {error && <Alert variant="destructive" className="mt-2"><AlertDescription>{error}</AlertDescription></Alert>}
      {searched && !loading && (
        <div className="mt-1.5 text-xs text-muted-foreground">Знайдено подій: {totalElements}</div>
      )}
      {rows.length > 0 && (
        <>
          <div className="overflow-x-auto touch-pan-x">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Час</TableHead>
                  <TableHead>Актор</TableHead>
                  <TableHead>Дія</TableHead>
                  <TableHead className="hidden sm:table-cell">Ціль</TableHead>
                  <TableHead>Результат</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.auditId} className="cursor-pointer" onClick={() => loadDetail(row.auditId)}>
                    <TableCell className="whitespace-nowrap">{formatTime(row.occurredAt)}</TableCell>
                    <TableCell>{row.actorLogin ?? row.actorType}</TableCell>
                    <TableCell><Badge variant="outline">{row.action}</Badge></TableCell>
                    <TableCell className="hidden sm:table-cell">
                      {row.targetType ? `${row.targetType} #${row.targetId?.slice(0, 8)}` : '—'}
                    </TableCell>
                    <TableCell>
                      <Badge variant={outcomeVariantMap[row.outcome] || 'default'}>{row.outcome}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <div className="mt-1.5 flex items-center gap-2 text-sm">
            <Button size="sm" variant="outline" disabled={page <= 0 || loading} onClick={() => search(page - 1)}>
              <ChevronLeft className="size-4" />
            </Button>
            <span className="text-xs text-muted-foreground">Сторінка {page + 1} із {Math.max(totalPages, 1)}</span>
            <Button size="sm" variant="outline" disabled={page + 1 >= totalPages || loading} onClick={() => search(page + 1)}>
              <ChevronRight className="size-4" />
            </Button>
          </div>
        </>
      )}
      {detailLoading && <p className="mt-2 text-sm text-muted-foreground">Завантаження картки…</p>}
      {detail && <AuditEventDetailCard detail={detail} onSelect={loadDetail} />}
      <div className="mt-2 flex flex-wrap items-center gap-1 border-t pt-2">
        <span className="text-xs font-medium">Історія обʼєкта:</span>
        <Input placeholder="Тип (напр. Episode)" value={historyType} onChange={(e) => setHistoryType(e.target.value)} className="w-full sm:w-[200px]" />
        <Input placeholder="ID обʼєкта" value={historyId} onChange={(e) => setHistoryId(e.target.value)} className="w-full sm:w-[300px] font-mono" />
        <Button size="sm" variant="outline" onClick={loadHistory} disabled={loading || !historyType || !historyId}>
          Показати
        </Button>
      </div>
      {history && <AuditObjectHistoryView history={history} onSelect={loadDetail} />}
    </div>
  );
}
