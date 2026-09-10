import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Activity, ClipboardCheck, PauseCircle, CheckCircle2, XCircle,
  AlertTriangle, Repeat, Timer, Hourglass, RefreshCw, Wrench, Users, BellRing, X,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { productionApi } from '@/api/prosthetics';
import { useAuth } from '@/services/AuthContext';
import { getErrorMessage } from '@/utils/errorMessage';
import { FLAG_LABELS, STATUS_LABELS, formatDurationSeconds } from '@/lib/production';
import ProductionWorkItemDrawer from '@/components/prosthetics/ProductionWorkItemDrawer';
import ProductionNormativeSettings from '@/components/prosthetics/ProductionNormativeSettings';
import type {
  FlowInstanceStatus,
  ProductionListParams,
  ProductionPage as ProductionPageData,
  ProductionQuality,
  ProductionSort,
  ProductionSummary,
  ProductionTeamRow,
  ProductionWorkItem,
} from '@/prosthetics/types';

const ALL_STATUSES: FlowInstanceStatus[] = [
  'NEW', 'IN_PROGRESS', 'PAUSED', 'BLOCKED_PATIENT', 'BLOCKED_MATERIAL',
  'COMPLETED', 'FAILED', 'BRANCHED',
];

const QUALITY_OPTIONS: { value: ProductionQuality; label: string }[] = [
  { value: 'ALL', label: 'Будь-яка якість' },
  { value: 'CLEAN', label: 'Без браку' },
  { value: 'BRAK', label: 'Є брак' },
  { value: 'REPEAT_BRAK', label: 'Повторний брак' },
  { value: 'REWORK', label: 'Доопрацювання' },
];

const SORT_OPTIONS: { value: ProductionSort; label: string }[] = [
  { value: 'NEWEST', label: 'Найновіші' },
  { value: 'OLDEST', label: 'Найстаріші' },
  { value: 'LONGEST', label: 'Найдовше в роботі' },
  { value: 'MOST_BRAK', label: 'Найбільше браків' },
  { value: 'MOST_IDLE', label: 'Найбільше очікування' },
  { value: 'MOST_DEVIATION', label: 'Найбільше відхилення' },
];

const PAGE_SIZES = [10, 20, 50];

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('uk-UA');
}

interface Filters {
  scope: 'all' | 'mine';
  status: FlowInstanceStatus | 'all';
  quality: ProductionQuality;
  sort: ProductionSort;
  dateFrom: string;
  dateTo: string;
  page: number;
  size: number;
}

const DEFAULT_FILTERS: Filters = {
  scope: 'all',
  status: 'all',
  quality: 'ALL',
  sort: 'NEWEST',
  dateFrom: '',
  dateTo: '',
  page: 0,
  size: 20,
};

type Section = 'items' | 'team' | 'attention';

function WorkItemCells({ item, showFlags }: { item: ProductionWorkItem; showFlags: boolean }) {
  const statusInfo = item.status ? STATUS_LABELS[item.status] : null;
  return (
    <>
      <TableCell className="sticky left-0 bg-card font-medium">
        {item.prosthetistFullName ?? '—'}
      </TableCell>
      <TableCell>
        <div className="font-medium">{item.productCode ?? '—'}</div>
        {item.orderNumber && (
          <div className="font-mono text-xs text-muted-foreground">{item.orderNumber}</div>
        )}
      </TableCell>
      <TableCell>{item.patientPib ?? '—'}</TableCell>
      <TableCell>{item.prosthesisType ?? item.productType ?? '—'}</TableCell>
      <TableCell>{item.currentStageName ?? '—'}</TableCell>
      <TableCell>{item.currentStepName ?? '—'}</TableCell>
      <TableCell>
        {statusInfo ? <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge> : '—'}
      </TableCell>
      <TableCell className="text-right tabular-nums">
        {formatDurationSeconds(item.activeSeconds)}
      </TableCell>
      <TableCell className="text-right tabular-nums">
        {formatDurationSeconds(item.idleSeconds)}
      </TableCell>
      <TableCell className="text-right tabular-nums">{item.brakCount}</TableCell>
      <TableCell className="text-right tabular-nums">{item.reworkCount}</TableCell>
      <TableCell>{formatDate(item.startTime)}</TableCell>
      {showFlags && (
        <TableCell>
          <div className="flex flex-wrap gap-1">
            {item.attentionFlags.map((flag) => (
              <Badge key={flag} variant={flag === 'FAILED' || flag === 'OVERDUE' ? 'destructive' : 'outline'}>
                {FLAG_LABELS[flag] ?? flag}
              </Badge>
            ))}
          </div>
        </TableCell>
      )}
    </>
  );
}

export default function ProductionPage() {
  const navigate = useNavigate();
  const { user, hasPermission } = useAuth();
  const canViewAll = hasPermission('PROSTHETICS_PRODUCTION_VIEW_ALL');

  const [section, setSection] = useState<Section>('items');
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS);
  const [drill, setDrill] = useState<{ id: number; name: string } | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [page, setPage] = useState<ProductionPageData<ProductionWorkItem> | null>(null);
  const [summary, setSummary] = useState<ProductionSummary | null>(null);
  const [team, setTeam] = useState<ProductionTeamRow[] | null>(null);
  const [attention, setAttention] = useState<ProductionWorkItem[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [teamLoading, setTeamLoading] = useState(false);
  const [attentionLoading, setAttentionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    document.title = 'Моніторинг виробництва — Superhumans Lviv';
  }, []);

  const patchFilters = useCallback((patch: Partial<Filters>) => {
    setFilters((prev) => ({ ...prev, ...patch, page: patch.page ?? 0 }));
  }, []);

  const fetchAll = useCallback(async () => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    setLoading(true);
    setSummaryLoading(true);
    setError(null);
    setDenied(false);
    const params: ProductionListParams = {
      ...(canViewAll && drill ? { assigneeId: drill.id }
        : canViewAll && filters.scope === 'mine' && user?.id != null ? { assigneeId: user.id }
        : {}),
      ...(filters.status !== 'all' ? { status: filters.status } : {}),
      quality: filters.quality,
      ...(filters.dateFrom ? { dateFrom: `${filters.dateFrom}T00:00:00` } : {}),
      ...(filters.dateTo ? { dateTo: `${filters.dateTo}T23:59:59` } : {}),
      sort: filters.sort,
      page: filters.page,
      size: filters.size,
    };
    try {
      const [listRes, summaryRes] = await Promise.all([
        productionApi.list(params, controller.signal),
        productionApi.summary(controller.signal),
      ]);
      setPage(listRes.data);
      setSummary(summaryRes.data);
    } catch (err) {
      if ((err as { code?: string })?.code === 'ERR_CANCELED') return;
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 403) {
        setDenied(true);
      } else {
        setError(getErrorMessage(err, 'Не вдалося завантажити моніторинг виробництва'));
      }
    } finally {
      setLoading(false);
      setSummaryLoading(false);
    }
  }, [canViewAll, drill, filters, user?.id]);

  useEffect(() => {
    void fetchAll();
    return () => abortRef.current?.abort();
  }, [fetchAll]);

  const fetchTeam = useCallback(async () => {
    if (!canViewAll) return;
    const controller = new AbortController();
    setTeamLoading(true);
    setError(null);
    try {
      const res = await productionApi.team(controller.signal);
      setTeam(res.data);
    } catch (err) {
      if ((err as { code?: string })?.code === 'ERR_CANCELED') return;
      setError(getErrorMessage(err, 'Не вдалося завантажити навантаження команди'));
    } finally {
      setTeamLoading(false);
    }
  }, [canViewAll]);

  const fetchAttention = useCallback(async () => {
    const controller = new AbortController();
    setAttentionLoading(true);
    setError(null);
    try {
      const res = await productionApi.attention(controller.signal);
      setAttention(res.data);
    } catch (err) {
      if ((err as { code?: string })?.code === 'ERR_CANCELED') return;
      setError(getErrorMessage(err, 'Не вдалося завантажити чергу уваги'));
    } finally {
      setAttentionLoading(false);
    }
  }, []);

  const openSection = (next: Section) => {
    setSection(next);
    if (next === 'team' && team === null) void fetchTeam();
    if (next === 'attention' && attention === null) void fetchAttention();
  };

  const openDrill = (row: ProductionTeamRow) => {
    setDrill({ id: row.userId, name: row.fullName ?? `ID ${row.userId}` });
    setFilters((prev) => ({ ...prev, page: 0 }));
    setSection('items');
  };

  const openProcess = (instanceId: string, status: FlowInstanceStatus | null) => {
    setSelectedId(null);
    if (status === 'COMPLETED') {
      navigate(`/prosthetics/process/${instanceId}/done`);
    } else if (status === 'FAILED' || status === 'BRANCHED') {
      navigate(`/prosthetics/process/${instanceId}/failed`);
    } else {
      navigate(`/prosthetics/process/${instanceId}/wizard`);
    }
  };

  const summaryCards = useMemo(() => {
    const s = summary;
    return [
      { key: 'inWork', label: 'В роботі', value: s ? String(s.inWork) : '—', icon: Wrench, color: 'text-mint' },
      { key: 'active', label: 'Активні', value: s ? String(s.active) : '—', icon: ClipboardCheck, color: 'text-sky-500' },
      { key: 'paused', label: 'На паузі', value: s ? String(s.paused) : '—', icon: PauseCircle, color: 'text-yellow-500' },
      { key: 'completed', label: 'Завершені', value: s ? String(s.completed) : '—', icon: CheckCircle2, color: 'text-green-600' },
      { key: 'failed', label: 'Провалені', value: s ? String(s.failed) : '—', icon: XCircle, color: 'text-red-500' },
      { key: 'brak', label: 'З браком', value: s ? String(s.brakItems) : '—', icon: AlertTriangle, color: 'text-orange-500' },
      { key: 'rework', label: 'Доопрацювання', value: s ? String(s.reworkItems) : '—', icon: Repeat, color: 'text-violet-500' },
      { key: 'avgActive', label: 'Сер. активний час', value: s?.avgActiveSeconds != null ? formatDurationSeconds(s.avgActiveSeconds) : '—', icon: Timer, color: 'text-info' },
      { key: 'avgElapsed', label: 'Сер. час в роботі', value: s?.avgElapsedSeconds != null ? formatDurationSeconds(s.avgElapsedSeconds) : '—', icon: Hourglass, color: 'text-muted-foreground' },
    ];
  }, [summary]);

  const rows = page?.content ?? [];
  const totalPages = page?.totalPages ?? 0;
  const totalElements = page?.totalElements ?? 0;

  return (
    <div className="container mx-auto py-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-3">
          <Activity className="size-8 text-mint" />
          <h1 className="font-display text-2xl font-bold">Моніторинг виробництва</h1>
        </div>
        <Button variant="ghost" size="sm" onClick={() => {
          if (section === 'team') void fetchTeam();
          else if (section === 'attention') void fetchAttention();
          else void fetchAll();
        }}>
          <RefreshCw className="size-4" />
          Оновити
        </Button>
      </div>

      {denied && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Немає доступу</AlertTitle>
          <AlertDescription>
            Для перегляду моніторингу потрібен дозвіл PROSTHETICS_PRODUCTION_VIEW.
          </AlertDescription>
        </Alert>
      )}

      {error && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Помилка</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-2">
            {error}
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                if (section === 'team') void fetchTeam();
                else if (section === 'attention') void fetchAttention();
                else void fetchAll();
              }}
            >
              Спробувати знову
            </Button>
          </AlertDescription>
        </Alert>
      )}

      <div className="mb-6 grid grid-cols-2 gap-4 md:grid-cols-3">
        {summaryCards.map((card) =>
          summaryLoading ? (
            <Skeleton key={card.key} className="h-[76px] w-full" />
          ) : (
            <Card key={card.key}>
              <CardContent className="flex items-center gap-3 py-4">
                <card.icon className={`size-8 ${card.color}`} />
                <div>
                  <div className="font-display text-2xl font-bold leading-none">{card.value}</div>
                  <div className="mt-1 text-xs text-muted-foreground">{card.label}</div>
                </div>
              </CardContent>
            </Card>
          ),
        )}
      </div>

      <Tabs value={section} onValueChange={(v) => openSection(v as Section)}>
        <TabsList className="mb-4">
          <TabsTrigger value="items">Вироби</TabsTrigger>
          {canViewAll && (
            <TabsTrigger value="team">
              <Users className="mr-1.5 size-4" />
              Команда
            </TabsTrigger>
          )}
          <TabsTrigger value="attention">
            <BellRing className="mr-1.5 size-4" />
            Потребують уваги
            {attention !== null && attention.length > 0 && (
              <Badge variant="destructive" className="ml-1.5">{attention.length}</Badge>
            )}
          </TabsTrigger>
        </TabsList>

        {section === 'items' && (
          <>
            <div className="mb-4 flex flex-wrap items-center gap-2">
              {canViewAll && (
                <Select
                  value={filters.scope}
                  onValueChange={(v) => patchFilters({ scope: v as Filters['scope'] })}
                >
                  <SelectTrigger className="w-44" aria-label="Чиї вироби">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">Всі протезисти</SelectItem>
                    <SelectItem value="mine">Тільки мої</SelectItem>
                  </SelectContent>
                </Select>
              )}
              {drill && (
                <Button variant="secondary" size="sm" className="gap-1.5" onClick={() => setDrill(null)}>
                  Протезист: {drill.name}
                  <X className="size-3.5" />
                </Button>
              )}
              <Select
                value={filters.status}
                onValueChange={(v) => patchFilters({ status: v as Filters['status'] })}
              >
                <SelectTrigger className="w-44" aria-label="Статус">
                  <SelectValue placeholder="Статус" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Всі статуси</SelectItem>
                  {ALL_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {STATUS_LABELS[s].label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select
                value={filters.quality}
                onValueChange={(v) => patchFilters({ quality: v as ProductionQuality })}
              >
                <SelectTrigger className="w-44" aria-label="Якість">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {QUALITY_OPTIONS.map((o) => (
                    <SelectItem key={o.value} value={o.value}>
                      {o.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select
                value={filters.sort}
                onValueChange={(v) => patchFilters({ sort: v as ProductionSort })}
              >
                <SelectTrigger className="w-48" aria-label="Сортування">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SORT_OPTIONS.map((o) => (
                    <SelectItem key={o.value} value={o.value}>
                      {o.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Input
                type="date"
                aria-label="Дата від"
                className="w-40"
                value={filters.dateFrom}
                onChange={(e) => patchFilters({ dateFrom: e.target.value })}
              />
              <Input
                type="date"
                aria-label="Дата до"
                className="w-40"
                value={filters.dateTo}
                onChange={(e) => patchFilters({ dateTo: e.target.value })}
              />
            </div>

            {loading ? (
              <div className="space-y-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : rows.length === 0 ? (
              <div className="py-12 text-center">
                <p className="text-muted-foreground">Немає виробів за поточними фільтрами</p>
              </div>
            ) : (
              <>
                <div className="overflow-x-auto touch-pan-x">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="sticky left-0 bg-card">Протезист</TableHead>
                        <TableHead>Виріб</TableHead>
                        <TableHead>Пацієнт</TableHead>
                        <TableHead>Тип</TableHead>
                        <TableHead>Етап</TableHead>
                        <TableHead>Крок</TableHead>
                        <TableHead>Статус</TableHead>
                        <TableHead className="text-right">Активний час</TableHead>
                        <TableHead className="text-right">Очікування</TableHead>
                        <TableHead className="text-right">Браки</TableHead>
                        <TableHead className="text-right">Rework</TableHead>
                        <TableHead>Старт</TableHead>
                        <TableHead className="text-right">Дії</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {rows.map((item) => (
                        <TableRow key={item.instanceId}>
                          <WorkItemCells item={item} showFlags={false} />
                          <TableCell className="text-right">
                            <Button variant="ghost" size="sm" onClick={() => setSelectedId(item.instanceId)}>
                              Відкрити
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
                <div className="mt-4 flex flex-wrap items-center justify-between gap-2">
                  <p className="text-sm text-muted-foreground">
                    Сторінка {filters.page + 1} з {Math.max(totalPages, 1)} · Всього {totalElements}
                  </p>
                  <div className="flex items-center gap-2">
                    <Select
                      value={String(filters.size)}
                      onValueChange={(v) => patchFilters({ size: Number(v) })}
                    >
                      <SelectTrigger className="w-24" aria-label="Розмір сторінки">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {PAGE_SIZES.map((s) => (
                          <SelectItem key={s} value={String(s)}>
                            {s}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={filters.page <= 0}
                      onClick={() => patchFilters({ page: filters.page - 1 })}
                    >
                      Назад
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={filters.page + 1 >= totalPages}
                      onClick={() => patchFilters({ page: filters.page + 1 })}
                    >
                      Далі
                    </Button>
                  </div>
                </div>
              </>
            )}
          </>
        )}

        {section === 'team' && canViewAll && (
          teamLoading || team === null ? (
            <div className="space-y-2">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          ) : team.length === 0 ? (
            <div className="py-12 text-center">
              <p className="text-muted-foreground">Немає даних про навантаження команди</p>
            </div>
          ) : (
            <div className="overflow-x-auto touch-pan-x">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="sticky left-0 bg-card">Протезист</TableHead>
                    <TableHead className="text-right">В роботі</TableHead>
                    <TableHead className="text-right">На паузі</TableHead>
                    <TableHead className="text-right">Завершені</TableHead>
                    <TableHead className="text-right">Провалені</TableHead>
                    <TableHead className="text-right">Браки</TableHead>
                    <TableHead className="text-right">Rework</TableHead>
                    <TableHead className="text-right">Прострочені</TableHead>
                    <TableHead className="text-right">Дії</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {team.map((row) => (
                    <TableRow key={row.userId}>
                      <TableCell className="sticky left-0 bg-card font-medium">
                        {row.fullName ?? `ID ${row.userId}`}
                      </TableCell>
                      <TableCell className="text-right tabular-nums">{row.inWork}</TableCell>
                      <TableCell className="text-right tabular-nums">{row.paused}</TableCell>
                      <TableCell className="text-right tabular-nums">{row.completed}</TableCell>
                      <TableCell className="text-right tabular-nums">{row.failed}</TableCell>
                      <TableCell className="text-right tabular-nums">{row.brakItems}</TableCell>
                      <TableCell className="text-right tabular-nums">{row.reworkItems}</TableCell>
                      <TableCell className="text-right tabular-nums">{row.overdueItems}</TableCell>
                      <TableCell className="text-right">
                        <Button variant="ghost" size="sm" onClick={() => openDrill(row)}>
                          Вироби
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )
        )}

        {section === 'attention' && (
          attentionLoading || attention === null ? (
            <div className="space-y-2">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          ) : attention.length === 0 ? (
            <div className="py-12 text-center">
              <p className="text-muted-foreground">Проблемних виробів немає — усе в нормі</p>
            </div>
          ) : (
            <div className="overflow-x-auto touch-pan-x">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="sticky left-0 bg-card">Протезист</TableHead>
                    <TableHead>Виріб</TableHead>
                    <TableHead>Пацієнт</TableHead>
                    <TableHead>Тип</TableHead>
                    <TableHead>Етап</TableHead>
                    <TableHead>Крок</TableHead>
                    <TableHead>Статус</TableHead>
                    <TableHead className="text-right">Активний час</TableHead>
                    <TableHead className="text-right">Очікування</TableHead>
                    <TableHead className="text-right">Браки</TableHead>
                    <TableHead className="text-right">Rework</TableHead>
                    <TableHead>Старт</TableHead>
                    <TableHead>Прапорці</TableHead>
                    <TableHead className="text-right">Дії</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {attention.map((item) => (
                    <TableRow key={item.instanceId}>
                      <WorkItemCells item={item} showFlags />
                      <TableCell className="text-right">
                        <Button variant="ghost" size="sm" onClick={() => setSelectedId(item.instanceId)}>
                          Відкрити
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )
        )}
      </Tabs>

      {canViewAll && <ProductionNormativeSettings />}
      <ProductionWorkItemDrawer
        instanceId={selectedId}
        onClose={() => setSelectedId(null)}
        onOpenProcess={openProcess}
      />
    </div>
  );
}
