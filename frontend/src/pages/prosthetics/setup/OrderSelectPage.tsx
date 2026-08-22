import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { prostheticsOrderApi } from '@/api/prosthetics';
import type { ProstheticsOrder } from '@/prosthetics/types';
import { SetupSteps } from '@/components/prosthetics/SetupSteps';

export default function OrderSelectPage() {
  const navigate = useNavigate();
  const { draft, setDraftField } = useProsthetics();
  const [orders, setOrders] = useState<ProstheticsOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Вибір замовлення — Виробництво протезів';
  }, []);

  useEffect(() => {
    if (!draft.patientId) {
      navigate('/prosthetics/new/select-patient');
      return;
    }
    const fetchOrders = async () => {
      setLoading(true);
      try {
        const res = await prostheticsOrderApi.listByPatient(draft.patientId!);
        setOrders(res.data);
      } catch {
        setError('Не вдалося завантажити замовлення');
      } finally {
        setLoading(false);
      }
    };
    fetchOrders();
  }, [draft.patientId, navigate]);

  return (
    <div className="container mx-auto max-w-2xl py-8">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/select-patient')}>
          <ChevronLeft className="size-4" />
          Назад
        </Button>
        <div>
          <h1 className="font-display text-2xl font-bold">Вибір замовлення</h1>
          <p className="text-sm text-muted-foreground">Крок 2 з 4</p>
        </div>
        <SetupSteps current={2} className="ml-auto" />
      </div>

      <Card className="mb-4">
        <CardHeader>
          <CardTitle className="text-base">Пацієнт</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 text-sm sm:grid-cols-4">
          <div>
            <div className="text-xs text-muted-foreground">ID пацієнта</div>
            <div className="font-medium">{draft.patientId}</div>
          </div>
        </CardContent>
      </Card>

      {error && (
        <Alert variant="destructive" className="mb-4">
          <AlertTitle>Помилка</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {loading ? (
        <div className="space-y-2">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : orders.length === 0 ? (
        <p className="text-muted-foreground">Немає замовлень для цього пацієнта.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Замовлення</TableHead>
              <TableHead>Тип протеза</TableHead>
              <TableHead>Статус</TableHead>
              <TableHead className="text-right">Дія</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {orders.map((order) => (
              <TableRow key={order.id}>
                <TableCell className="font-medium">#{order.orderNumber}</TableCell>
                <TableCell>{order.productType}</TableCell>
                <TableCell>
                  <Badge variant="outline">{order.status}</Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button
                    size="sm"
                    variant={draft.orderId === order.id ? 'default' : 'outline'}
                    onClick={() => {
                      setDraftField('orderId', order.id);
                      setDraftField('templateId', null);
                      navigate('/prosthetics/new/review-order');
                    }}
                  >
                    {draft.orderId === order.id ? 'Обрано' : 'Обрати'}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <div className="sticky bottom-0 z-10 -mx-4 mt-4 flex flex-col gap-3 border-t bg-background/95 px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] backdrop-blur sm:-mx-6 sm:flex-row sm:items-center sm:justify-between sm:px-6 sm:pb-3">
        <Button variant="outline" className="w-full sm:w-auto" onClick={() => navigate('/prosthetics/new/select-patient')}>
          Назад
        </Button>
        <Button
          disabled={!draft.orderId}
          className="w-full bg-accent text-accent-foreground hover:bg-accent/90 sm:w-auto"
          onClick={() => navigate('/prosthetics/new/review-order')}
        >
          Далі
        </Button>
      </div>
    </div>
  );
}