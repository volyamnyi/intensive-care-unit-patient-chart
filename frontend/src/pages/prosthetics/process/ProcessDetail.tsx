import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Syringe, Calendar, User, FileText } from 'lucide-react';
import { Card, CardContent, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import type { FlowInstance } from '@/prosthetics/types';
import { flowInstanceApi } from '@/api/prosthetics';

export default function ProcessDetail() {
  const { id } = useParams<{ id: string }>();
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

  const getBadgeVariant = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'default' as const;
      case 'PAUSED':
        return 'secondary' as const;
      case 'FAILED':
        return 'destructive' as const;
      default:
        return 'outline' as const;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-2xl font-bold">
          Процес створення протезу
        </h1>
        <Badge variant={getBadgeVariant(instance.status)}>
          {instance.status}
        </Badge>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardContent className="pt-6">
            <CardTitle className="mb-2 flex items-center gap-2 text-sm font-medium">
              <User className="size-4 text-muted-foreground" />
              Пацієнт
            </CardTitle>
            <CardDescription>
              {instance.patientId}
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
              {instance.templateId}
            </CardDescription>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <CardTitle className="mb-2 flex items-center gap-2 text-sm font-medium">
              <Syringe className="size-4 text-muted-foreground" />
              Статус
            </CardTitle>
            <CardDescription>
              {instance.status}
            </CardDescription>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
