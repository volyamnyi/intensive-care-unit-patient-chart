import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { flowInstanceApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';
import type { FlowInstance, SnapshotTemplate } from '@/prosthetics/types';

export default function FailedScreen() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [instance, setInstance] = useState<FlowInstance | null>(null);
  const [snapshot, setSnapshot] = useState<SnapshotTemplate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Процес зупинено — звіт про брак';
    const load = async () => {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const [instRes, snapRes] = await Promise.all([
          flowInstanceApi.getById(id),
          flowInstanceApi.getSnapshot(id),
        ]);
        setInstance(instRes.data);
        setSnapshot(snapRes.data);
      } catch (err) {
        setError(getErrorMessage(err, 'Не вдалося завантажити звіт'));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="mx-auto size-14 rounded-full" />
        <Skeleton className="mx-auto h-8 w-72" />
        <Skeleton className="h-32" />
      </div>
    );
  }

  if (error || !instance || !snapshot) {
    return (
      <div className="py-16 text-center">
        <h1 className="font-display text-xl font-semibold">Звіт недоступний</h1>
        <p className="mt-2 text-sm text-muted-foreground">{error ?? 'Процес не знайдено'}</p>
        <Button className="mt-4" onClick={() => navigate('/prosthetics')}>
          До панелі управління
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <div className="flex flex-col items-center text-center">
        <XCircle className="size-14 text-destructive" />
        <h1 className="mt-4 font-display text-2xl font-semibold">Процес зупинено (брак)</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {instance.id} · {snapshot.name} (v{snapshot.version})
        </p>
      </div>

      <Card className="mt-8 border-destructive">
        <CardHeader>
          <CardTitle className="text-base">Причина</CardTitle>
        </CardHeader>
        <CardContent className="text-sm">{instance.failReason ?? '—'}</CardContent>
      </Card>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-base">Метадані процесу</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <div className="flex justify-between gap-4 border-b pb-2 last:border-0">
            <span className="text-muted-foreground">Створено</span>
            <span>{new Date(instance.createdAt).toLocaleString('uk-UA')}</span>
          </div>
          <div className="flex justify-between gap-4 border-b pb-2 last:border-0">
            <span className="text-muted-foreground">Зупинено</span>
            <span>
              {instance.endTime ? new Date(instance.endTime).toLocaleString('uk-UA') : '—'}
            </span>
          </div>
          <div className="flex justify-between gap-4 border-b pb-2 last:border-0">
            <span className="text-muted-foreground">Доопрацювань до зупинки</span>
            <span>{instance.reworkCount ?? 0}</span>
          </div>
        </CardContent>
      </Card>

      <div className="mt-8 flex justify-center gap-3">
        <Button variant="outline" onClick={() => navigate(`/prosthetics/process/${instance.id}`)}>
          Технологічна карта
        </Button>
        <Button onClick={() => navigate('/prosthetics')}>До панелі управління</Button>
      </div>
    </div>
  );
}
