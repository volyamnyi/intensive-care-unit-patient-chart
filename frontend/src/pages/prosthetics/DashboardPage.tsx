import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Syringe, Plus, Search, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { flowInstanceApi } from '@/api/prosthetics';
import type { FlowInstance, FlowInstanceStatus } from '@/prosthetics/types';

const STATUS_LABELS: Record<FlowInstanceStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' | 'link' }> = {
  NEW: { label: 'Новий', variant: 'default' },
  IN_PROGRESS: { label: 'В процесі', variant: 'default' },
  PAUSED: { label: 'Припущено', variant: 'outline' },
  BLOCKED_PATIENT: { label: 'Заблоковано (пацієнт)', variant: 'destructive' },
  BLOCKED_MATERIAL: { label: 'Заблоковано (матеріали)', variant: 'destructive' },
  WAITING_REVIEW: { label: 'Очікує перевірки', variant: 'outline' },
  CORRECTION: { label: 'Корекція', variant: 'outline' },
  FAILED_QC: { label: 'Не пройшов QA', variant: 'destructive' },
  COMPLETED: { label: 'Завершено', variant: 'default' },
  FAILED: { label: 'Завершено з помилкою', variant: 'destructive' },
};

const STATUS_FILTERS: { value: FlowInstanceStatus; label: string }[] = [
  { value: 'IN_PROGRESS', label: 'Активні' },
  { value: 'PAUSED', label: 'Припущені' },
  { value: 'COMPLETED', label: 'Завершені' },
  { value: 'FAILED', label: 'Провалені' },
];

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
    const fetchInstances = async () => {
      setLoading(true);
      setError(null);
      try {
        const params: Record<string, string> = {};
        if (activeFilter !== 'all') {
          params.status = activeFilter;
        }
        if (searchQuery) {
          params.query = searchQuery;
        }
        const res = await flowInstanceApi.list(params);
        setInstances(res.data);
      } catch {
        setError('Не вдалося завантажити процеси');
      } finally {
        setLoading(false);
      }
    };
    fetchInstances();
  }, [activeFilter, searchQuery]);

  useEffect(() => {
    resetDraft();
  }, [resetDraft]);

  const handleCreate = () => {
    resetDraft();
    navigate('/prosthetics/new/select-patient');
  };

  return (
    <div className="container mx-auto py-6">
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Syringe className="size-8 text-mint" />
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
        <Button variant="ghost" size="sm">
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
        ) : instances.length === 0 ? (
          <div className="py-12 text-center">
            <p className="text-muted-foreground">Немає процесів за поточним фільтром</p>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Номер замовлення</TableHead>
                <TableHead>Пацієнт</TableHead>
                <TableHead>Статус</TableHead>
                <TableHead>Створено</TableHead>
                <TableHead className="text-right">Дії</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {instances.map((instance) => {
                const statusInfo = STATUS_LABELS[instance.status];
                return (
              <TableRow key={instance.id}>
                <TableCell>{instance.orderId}</TableCell>
                <TableCell>{instance.patientId}</TableCell>
                <TableCell>
                  <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
                </TableCell>
                <TableCell>{new Date(instance.createdAt).toLocaleDateString('uk-UA')}</TableCell>
                <TableCell className="text-right">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => navigate(`/prosthetics/process/${instance.id}`)}
                  >
                    Відкрити
                  </Button>
                </TableCell>
              </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </Tabs>
    </div>
  );
}