import { useState } from 'react';
import { CheckCircle2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { useThemeMode } from '../../styles/ThemeContext';

import type { MedicalOrder, MedicalOrderCreateRequest } from '../../types/icu';

interface MedicalOrdersPanelProps {
  orders: MedicalOrder[];
  onCreateOrder?: (order: MedicalOrderCreateRequest) => void;
  onExecuteOrder?: (orderId: string) => void;
  onCancelOrder?: (orderId: string) => void;
  canCreate?: boolean;
  canExecute?: boolean;
}

const emptyOrder: MedicalOrderCreateRequest = {
  category: 'MEDICATION',
  drugName: '',
  dose: '',
  unit: '',
  route: '',
  frequency: '',
  startTime: '',
  endTime: '',
};

export default function MedicalOrdersPanel({
  orders, onCreateOrder, onExecuteOrder, onCancelOrder, canCreate, canExecute,
}: MedicalOrdersPanelProps) {
  useThemeMode();
  const statusLabels: Record<string, string> = {
    DRAFT: 'Чернетка',
    ACTIVE: 'Активний',
    COMPLETED: 'Виконаний',
    CANCELLED: 'Скасований',
  };
  const [newOrder, setNewOrder] = useState<MedicalOrderCreateRequest>(emptyOrder);
  const [showForm, setShowForm] = useState(false);
  const showActions = canExecute || !!onCancelOrder;

  const handleCreate = () => {
    if (!onCreateOrder || !newOrder.drugName || !newOrder.dose) return;
    onCreateOrder(newOrder);
    setNewOrder(emptyOrder);
    setShowForm(false);
  };

  const badgeVariant = (status: string) => {
    if (status === 'ACTIVE') return 'default' as const;
    if (status === 'CANCELLED') return 'secondary' as const;
    return 'outline' as const;
  };

  return (
    <>
      {canCreate && (
        <div className="mb-2">
          {showForm ? (
            <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2 mb-2">
              <p className="font-rubik mb-1 text-sm font-medium">
                {'Нове призначення'}
              </p>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-1 items-center">
                <Input placeholder="Категорія"
                  value={newOrder.category}
                  onChange={(e) => setNewOrder({ ...newOrder, category: e.target.value })} />
                <Input placeholder="Препарат"
                  value={newOrder.drugName}
                  onChange={(e) => setNewOrder({ ...newOrder, drugName: e.target.value })} />
                <Input placeholder="Доза"
                  value={newOrder.dose}
                  onChange={(e) => setNewOrder({ ...newOrder, dose: e.target.value })} />
                <Input placeholder="Од."
                  value={newOrder.unit}
                  onChange={(e) => setNewOrder({ ...newOrder, unit: e.target.value })} />
                <Input placeholder="Шлях"
                  value={newOrder.route}
                  onChange={(e) => setNewOrder({ ...newOrder, route: e.target.value })} />
                <Input placeholder="Частота"
                  value={newOrder.frequency}
                  onChange={(e) => setNewOrder({ ...newOrder, frequency: e.target.value })} />
                <Input type="datetime-local" placeholder="Початок"
                  value={newOrder.startTime}
                  onChange={(e) => setNewOrder({ ...newOrder, startTime: e.target.value })} />
                <Input type="datetime-local" placeholder="Кінець"
                  value={newOrder.endTime}
                  onChange={(e) => setNewOrder({ ...newOrder, endTime: e.target.value })} />
                <div className="col-span-full flex gap-1">
                  <Button variant="default" size="sm" onClick={handleCreate}>
                    {'Створити'}
                  </Button>
                  <Button variant="outline" size="sm" onClick={() => setShowForm(false)}>
                    {'Скасувати'}
                  </Button>
                </div>
              </div>
            </div>
          ) : (
            <Button variant="outline" size="sm" onClick={() => setShowForm(true)}>
              {'+ Нове призначення'}
            </Button>
          )}
        </div>
      )}

      <div className="overflow-x-auto rounded-xl border">
        <Table className="min-w-[500px]">
          <TableHeader>
            <TableRow>
              <TableHead>{'Препарат'}</TableHead>
              <TableHead>{'Доза'}</TableHead>
              <TableHead>{'Шлях'}</TableHead>
              <TableHead>{'Статус'}</TableHead>
              {showActions && <TableHead>{''}</TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {orders.length === 0 ? (
              <TableRow>
                <TableCell colSpan={showActions ? 5 : 4} align="center" className="py-3 text-muted-foreground">
                  {'Немає призначень'}
                </TableCell>
              </TableRow>
            ) : (
              orders.map((order) => (
                <TableRow key={order.id}>
                  <TableCell className="font-semibold">{order.drugName}</TableCell>
                  <TableCell>{order.dose} {order.unit}</TableCell>
                  <TableCell>{order.route}</TableCell>
                  <TableCell>
                    <Badge variant={badgeVariant(order.status)}>
                      {statusLabels[order.status] || order.status}
                    </Badge>
                  </TableCell>
                  {showActions && (
                    <TableCell>
                      {order.status === 'ACTIVE' && canExecute && onExecuteOrder && (
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => onExecuteOrder(order.id)}
                        >
                          <CheckCircle2 className="size-4" />
                        </Button>
                      )}
                      {order.status === 'ACTIVE' && !canExecute && onCancelOrder && (
                        <Button size="sm" variant="destructive" onClick={() => onCancelOrder(order.id)}>
                          {'Скасувати'}
                        </Button>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </>
  );
}
