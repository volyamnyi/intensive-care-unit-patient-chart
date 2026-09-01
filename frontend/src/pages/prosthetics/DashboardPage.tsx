import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, RefreshCw, ClipboardCheck, PauseCircle, CheckCircle2, XCircle, Wrench } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { flowInstanceApi } from '@/api/prosthetics';
import type { FlowInstance, FlowInstanceStatus } from '@/prosthetics/types';

const STATUS_LABELS: Record<FlowInstanceStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' | 'link' }> = {
  NEW: { label: 'Новий', variant: 'default' },
  IN_PROGRESS: { label: 'В процесі', variant: 'default' },
  PAUSED: { label: 'Призупинено', variant: 'outline' },
  BLOCKED_PATIENT: { label: 'Заблоковано (пацієнт)', variant: 'destructive' },
  BLOCKED_MATERIAL: { label: 'Заблоковано (матеріали)', variant: 'destructive' },
  WAITING_REVIEW: { label: 'Очікує перевірки', variant: 'outline' },
  CORRECTION: { label: 'Корекція', variant: 'outline' },
  FAILED_QC: { label: 'Не пройшов QA', variant: 'destructive' },
  COMPLETED: { label: 'Завершено', variant: 'default' },
  FAILED: { label: 'Завершено з помилкою', variant: 'destructive' },
  BRANCHED: { label: 'Розгалужено', variant: 'outline' },
};

const STATUS_FILTERS: { value: FlowInstanceStatus; label: string }[] = [
  { value: 'IN_PROGRESS', label: 'Активні' },
  { value: 'PAUSED', label: 'Призупинені' },
  { value: 'COMPLETED', label: 'Завершені' },
  { value: 'FAILED', label: 'Провалені' },
];

const ACTIVE_STATUSES: FlowInstanceStatus[] = ['NEW', 'IN_PROGRESS', 'WAITING_REVIEW', 'CORRECTION'];
const PAUSED_STATUSES: FlowInstanceStatus[] = ['PAUSED', 'BLOCKED_PATIENT', 'BLOCKED_MATERIAL'];
const FAILED_STATUSES: FlowInstanceStatus[] = ['FAILED', 'FAILED_QC', 'BRANCHED'];

function matchesFilter(instance: FlowInstance, filter: FlowInstanceStatus | 'all'): boolean {
  if (filter === 'all') return true;
  if (filter === 'IN_PROGRESS') return ACTIVE_STATUSES.includes(instance.status);
  if (filter === 'PAUSED') return PAUSED_STATUSES.includes(instance.status);
  if (filter === 'FAILED') return FAILED_STATUSES.includes(instance.status);
  return instance.status === filter;
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const { resetDraft } = useProsthetics();
  const [instances, setInstances] = useState<FlowInstance[]>([]);
  const [activeFilter, setActiveFilter] = useState<FlowInstanceStatus | 'all'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Виробництво протезів — Superhumans Lviv';
  }, []);

  const fetchInstances = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await flowInstanceApi.list();
      setInstances(res.data);
    } catch {
      setError('Не вдалося завантажити процеси');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchInstances();
  }, [fetchInstances]);

  useEffect(() => {
    resetDraft();
  }, [resetDraft]);

  const counts = useMemo(
    () => ({
      active: instances.filter((i) => ACTIVE_STATUSES.includes(i.status)).length,
      paused: instances.filter((i) => PAUSED_STATUSES.includes(i.status)).length,
      completed: instances.filter((i) => i.status === 'COMPLETED').length,
      failed: instances.filter((i) => FAILED_STATUSES.includes(i.status)).length,
    }),
    [instances],
  );

  const filtered = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    return instances.filter((i) => {
      if (!matchesFilter(i, activeFilter)) return false;
      if (!query) return true;
      return (
        (i.orderNumber ?? '').toLowerCase().includes(query) ||
        (i.patientPib ?? '').toLowerCase().includes(query) ||
        (i.templateName ?? '').toLowerCase().includes(query) ||
        i.id.toLowerCase().includes(query)
      );
    });
  }, [instances, activeFilter, searchQuery]);

  const handleCreate = () => {
    resetDraft();
    navigate('/prosthetics/new/select-patient');
  };

  const openInstance = (instance: FlowInstance) => {
    if (instance.status === 'COMPLETED') {
      navigate(`/prosthetics/process/${instance.id}/done`);
    } else if (instance.status === 'FAILED' || instance.status === 'FAILED_QC' || instance.status === 'BRANCHED') {
      navigate(`/prosthetics/process/${instance.id}/failed`);
    } else {
      navigate(`/prosthetics/process/${instance.id}/wizard`);
    }
  };

  const statCards = [
    { key: 'active', label: 'Активні', value: counts.active, icon: ClipboardCheck, color: 'text-sky-500' },
    { key: 'paused', label: 'Призупинені', value: counts.paused, icon: PauseCircle, color: 'text-yellow-500' },
    { key: 'completed', label: 'Завершені', value: counts.completed, icon: CheckCircle2, color: 'text-green-600' },
    { key: 'failed', label: 'Провалені', value: counts.failed, icon: XCircle, color: 'text-red-500' },
  ];

  return (
    <div className="container mx-auto py-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-3">
          <Wrench className="size-8 text-mint" />
          <h1 className="font-display text-2xl font-bold">Виробництво протезів</h1>
        </div>
        <Button onClick={handleCreate} className="gap-2">
          <Plus className="size-4" />
          Новий процес
        </Button>
      </div>

      {error && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Помилка</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <div className="mb-6 grid grid-cols-2 gap-4 xl:grid-cols-4">
        {statCards.map((card) => (
          <Card key={card.key}>
            <CardContent className="flex items-center gap-3 py-4">
              <card.icon className={`size-8 ${card.color}`} />
              <div>
                <div className="font-display text-2xl font-bold leading-none">{card.value}</div>
                <div className="mt-1 text-xs text-muted-foreground">{card.label}</div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="mb-4 flex gap-2">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
          <Input
            placeholder="Пошук за номером замовлення або пацієнтом..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>
        <Button variant="ghost" size="sm" onClick={() => void fetchInstances()}>
          <RefreshCw className="size-4" />
        </Button>
      </div>

      <Tabs value={activeFilter === 'all' ? 'all' : activeFilter} onValueChange={(v) => setActiveFilter(v === 'all' ? 'all' : v as FlowInstanceStatus)}>
        <TabsList className="mb-4">
          <TabsTrigger value="all">Всі</TabsTrigger>
          {STATUS_FILTERS.map((f) => (
            <TabsTrigger key={f.value} value={f.value}>{f.label}</TabsTrigger>
          ))}
        </TabsList>

        {loading ? (
          <div className="space-y-2">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-12 text-center">
            <p className="text-muted-foreground">Немає процесів за поточним фільтром</p>
          </div>
        ) : (
          <div className="overflow-x-auto touch-pan-x">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Процес</TableHead>
                  <TableHead>Пацієнт</TableHead>
                  <TableHead>Замовлення</TableHead>
                  <TableHead>Шаблон</TableHead>
                  <TableHead>Поточний етап</TableHead>
                  <TableHead>Поточний крок</TableHead>
                  <TableHead>Статус</TableHead>
                  <TableHead>Оновлено</TableHead>
                  <TableHead className="text-right">Дії</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((instance) => {
                  const statusInfo = STATUS_LABELS[instance.status];
                  return (
                    <TableRow
                      key={instance.id}
                      className="cursor-pointer"
                      onClick={() => openInstance(instance)}
                    >
                      <TableCell className="font-mono text-xs">#{instance.id.slice(0, 8)}</TableCell>
                      <TableCell>{instance.patientPib ?? '—'}</TableCell>
                      <TableCell>{instance.orderNumber ?? '—'}</TableCell>
                      <TableCell>{instance.templateName ?? '—'}</TableCell>
                      <TableCell>{instance.currentStageName ?? '—'}</TableCell>
                      <TableCell>{instance.currentStepName ?? '—'}</TableCell>
                      <TableCell>
                        <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
                      </TableCell>
                      <TableCell>{new Date(instance.updatedAt).toLocaleDateString('uk-UA')}</TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            openInstance(instance);
                          }}
                        >
                          Відкрити
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        )}
      </Tabs>
    </div>
  );
}
