import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Calendar, ClipboardList, FileText, Play, User, Wrench } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardTitle, CardDescription } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { StatusBadge } from '@/components/prosthetics/StatusBadge';
import type { FlowInstance } from '@/prosthetics/types';
import { flowInstanceApi } from '@/api/prosthetics';

const RESUMABLE_STATUSES = ['IN_PROGRESS', 'PAUSED', 'WAITING_REVIEW', 'CORRECTION', 'NEW'];

export default function ProcessDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [instance, setInstance] = useState<FlowInstance | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (id) {
      flowInstanceApi
        .getById(id)
        .then((res) => setInstance(res.data))
        .catch(() => setInstance(null))
        .finally(() => setLoading(false));
    }
  }, [id]);

  useEffect(() => {
    document.title = instance
      ? `Процес #${instance.id} — Виробництво протезів`
      : 'Перегляд процесу — Виробництво протезів';
  }, [instance]);

  if (loading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-48 w-full" />
      </div>
    );
  }

  if (!instance) {
    return <p className="text-muted-foreground">Процес не знайдено.</p>;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-display text-2xl font-bold">
          Процес створення протезу
        </h1>
        <div className="flex items-center gap-3">
          <StatusBadge status={instance.status} />
          {RESUMABLE_STATUSES.includes(instance.status) && (
            <Button
              size="sm"
              onClick={() => navigate(`/prosthetics/process/${instance.id}/wizard`)}
            >
              <Play className="size-4" />
              {instance.status === 'NEW' ? 'Розпочати процес' : 'Продовжити виконання'}
            </Button>
          )}
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardContent className="pt-6">
            <CardTitle className="mb-2 flex items-center gap-2 text-sm font-medium">
              <User className="size-4 text-muted-foreground" />
              Пацієнт
            </CardTitle>
            <CardDescription>
              {instance.patientPib ?? instance.patientId}
            </CardDescription>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <CardTitle className="mb-2 flex items-center gap-2 text-sm font-medium">
              <ClipboardList className="size-4 text-muted-foreground" />
              Замовлення
            </CardTitle>
            <CardDescription>
              {instance.orderNumber ?? instance.orderId}
            </CardDescription>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <CardTitle className="mb-2 flex items-center gap-2 text-sm font-medium">
              <Calendar className="size-4 text-muted-foreground" />
              Створено
            </CardTitle>
            <CardDescription>
              {new Date(instance.createdAt).toLocaleString('uk-UA')}
            </CardDescription>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <CardTitle className="mb-2 flex items-center gap-2 text-sm font-medium">
              <FileText className="size-4 text-muted-foreground" />
              Шаблон
            </CardTitle>
            <CardDescription>
              {instance.templateName ?? instance.templateId}
            </CardDescription>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <CardTitle className="mb-2 flex items-center gap-2 text-sm font-medium">
              <Wrench className="size-4 text-muted-foreground" />
              Поточний етап
            </CardTitle>
            <CardDescription>
              {instance.currentStageName ?? '—'}
            </CardDescription>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <CardTitle className="mb-2 flex items-center gap-2 text-sm font-medium">
              <Wrench className="size-4 text-muted-foreground" />
              Поточний крок
            </CardTitle>
            <CardDescription>
              {instance.currentStepName ?? '—'}
            </CardDescription>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
