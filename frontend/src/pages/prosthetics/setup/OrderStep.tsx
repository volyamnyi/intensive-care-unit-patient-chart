import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, ChevronRight, FileText } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardTitle, CardDescription } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import type { ProstheticsOrder } from '@/prosthetics/types';

export default function OrderStep() {
  const navigate = useNavigate();
  const { draft, setDraftField } = useProsthetics();
  const [orders, setOrders] = useState<ProstheticsOrder[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    document.title = 'Вибір замовлення — Виробництво протезів';
  }, []);

  useEffect(() => {
    if (!draft.patientId) return;
    const fetchOrders = async () => {
      setLoading(true);
      try {
        const res = await fetch(`/api/prosthesis-manufacturing/orders?patientId=${draft.patientId}`, {
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token')}`,
          },
        });
        if (res.ok) {
          const data: ProstheticsOrder[] = await res.json();
          setOrders(data);
        }
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    };
    fetchOrders();
  }, [draft.patientId]);

  const handleSelect = (orderId: string) => {
    setDraftField('orderId', orderId);
    navigate('/prosthetics/new/template');
  };

  if (!draft.patientId) {
    return (
      <div className="container mx-auto max-w-2xl py-8">
        <div className="mb-6 flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/patient')}>
            <ChevronLeft className="size-4" />
            Назад
          </Button>
          <h1 className="font-display text-2xl font-bold">Вибір замовлення</h1>
        </div>
        <p className="text-muted-foreground">Спочатку оберіть пацієнта.</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-2xl py-8">
      <div className="mb-6 flex items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/patient')}>
          <ChevronLeft className="size-4" />
          Назад
        </Button>
        <h1 className="font-display text-2xl font-bold">Вибір замовлення</h1>
      </div>

      {loading ? (
        <div className="space-y-2">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
        </div>
      ) : orders.length === 0 ? (
        <p className="text-muted-foreground">Немає замовлень для цього пацієнта.</p>
      ) : (
        <div className="space-y-2">
          {orders.map((order) => (
            <Card
              key={order.id}
              className="cursor-pointer transition-colors hover:bg-accent"
              onClick={() => handleSelect(order.id)}
            >
              <CardContent className="pt-4">
                <CardTitle className="flex items-center gap-2 text-base">
                  <FileText className="size-4" />
                  {order.id}
                </CardTitle>
                <CardDescription>
                  {order.productType}
                </CardDescription>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <div className="mt-6 flex justify-end">
        <Button
          onClick={() => navigate('/prosthetics/new/template')}
          disabled={!draft.orderId}
          className="gap-2"
        >
          Продовжити
          <ChevronRight className="size-4" />
        </Button>
      </div>
    </div>
  );
}
