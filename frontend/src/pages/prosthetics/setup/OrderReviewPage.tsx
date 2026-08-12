import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, FileText, Loader2, Package } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { prostheticsOrderApi, prostheticsPatientApi, flowInstanceApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';
import type { ProstheticsOrder, ProstheticsPatient } from '@/prosthetics/types';
import { SetupSteps } from '@/components/prosthetics/SetupSteps';

interface OrderWithPatient extends ProstheticsOrder {
  patient: ProstheticsPatient | null;
}

const ACTIVE_DUPLICATE_STATUSES = ['NEW', 'IN_PROGRESS', 'PAUSED', 'BLOCKED_PATIENT', 'BLOCKED_MATERIAL', 'WAITING_REVIEW', 'CORRECTION'];

function parseMaterials(raw: string | null | undefined): string[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as unknown;
    return Array.isArray(parsed) ? parsed.filter((m): m is string => typeof m === 'string') : [];
  } catch {
    return [];
  }
}

function limbSideLabel(value: string | undefined): string {
  const side = (value ?? '').toUpperCase();
  if (side === 'LEFT') return 'Лівий';
  if (side === 'RIGHT') return 'Правий';
  return value || '—';
}

export default function OrderReviewPage() {
  const navigate = useNavigate();
  const { draft } = useProsthetics();
  const [order, setOrder] = useState<OrderWithPatient | null>(null);
  const [documentUrl, setDocumentUrl] = useState<string | null>(null);
  const [docLoaded, setDocLoaded] = useState(false);
  const [docError, setDocError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [startError, setStartError] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    document.title = 'Перевірка замовлення — Виробництво протезів';
  }, []);

  useEffect(() => {
    if (!draft.orderId) {
      navigate('/prosthetics/new/select-order');
      return;
    }

    const fetchData = async () => {
      setLoading(true);
      setDocLoaded(false);
      setDocError(null);
      setDocumentUrl(null);
      try {
        const orderRes = await prostheticsOrderApi.getById(draft.orderId!);
        
        let patient: ProstheticsPatient | null = null;
        if (orderRes.data.patientId) {
          try {
            const patientRes = await prostheticsPatientApi.getById(orderRes.data.patientId);
            patient = patientRes.data;
          } catch {
            // ignore patient fetch errors
          }
        }
        
        setOrder({ ...orderRes.data, patient });
        
        // Auto-load document
        try {
          const blob = await prostheticsOrderApi.getDocument(draft.orderId!);
          const url = window.URL.createObjectURL(blob.data);
          setDocumentUrl(url);
          setDocLoaded(true);
        } catch (err: unknown) {
          const axiosError = err as { response?: { data?: { message?: string } } };
          setDocError(axiosError.response?.data?.message || 'Не вдалося завантажити замовлення на протез');
          setDocLoaded(true);
        }
      } catch {
        setError('Не вдалося завантажити дані');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [draft.orderId, navigate]);

  const handleStart = async () => {
    if (!draft.orderId) return;
    setChecking(true);
    setStartError(null);
    try {
      const res = await flowInstanceApi.list();
      const duplicate = res.data.some(
        (i) => i.orderId === draft.orderId && ACTIVE_DUPLICATE_STATUSES.includes(i.status),
      );
      if (duplicate) {
        setStartError(
          'Для цього замовлення вже існує процес у роботі. Знайдіть його на панелі управління.',
        );
        return;
      }
      navigate('/prosthetics/new/select-template');
    } catch (err) {
      setStartError(getErrorMessage(err, 'Не вдалося перевірити наявність процесу для замовлення'));
    } finally {
      setChecking(false);
    }
  };

  if (!draft.orderId) {
    return (
      <div className="container mx-auto max-w-2xl py-8">
        <div className="mb-6 flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/select-order')}>
            <ChevronLeft className="size-4" />
            Назад
          </Button>
          <h1 className="font-display text-2xl font-bold">Перевірка замовлення</h1>
        </div>
        <p className="text-muted-foreground">Необхідно обрати замовлення.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="container mx-auto max-w-2xl py-8">
        <div className="mb-6 flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/select-order')}>
            <ChevronLeft className="size-4" />
            Назад
          </Button>
          <h1 className="font-display text-2xl font-bold">Перевірка замовлення</h1>
        </div>
        <p className="text-muted-foreground">Завантаження даних...</p>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="container mx-auto max-w-2xl py-8">
        <div className="mb-6 flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/select-order')}>
            <ChevronLeft className="size-4" />
            Назад
          </Button>
          <h1 className="font-display text-2xl font-bold">Перевірка замовлення</h1>
        </div>
        <Alert variant="destructive">
          <AlertTitle>Помилка</AlertTitle>
          <AlertDescription>{error || 'Дані не знайдено'}</AlertDescription>
        </Alert>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-2xl py-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold">Перевірка замовлення</h1>
          <p className="text-sm text-muted-foreground">Крок 3 з 4 · {order?.orderNumber}</p>
        </div>
        <SetupSteps current={3} />
      </div>

      <Tabs defaultValue="document">
        <TabsList className="mb-4">
          <TabsTrigger value="details">Деталі</TabsTrigger>
          <TabsTrigger value="document">Замовлення на протез</TabsTrigger>
          <TabsTrigger value="materials">Матеріали</TabsTrigger>
        </TabsList>

        <TabsContent value="details">
          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Пацієнт</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-base">{order.patient?.pib || '—'}</p>
                <p className="text-sm text-muted-foreground">ID: {order.patientId}</p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Замовлення</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <p className="text-base">Замовлення #{order.orderNumber}</p>
                <p className="text-muted-foreground">{order.productType}</p>
                <p className="text-sm text-muted-foreground">
                  Рівень ампутації: {order.amputationLevel || '—'}
                </p>
                <p className="text-sm text-muted-foreground">Бік: {limbSideLabel(order.limbSide)}</p>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="document">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>Замовлення на протез</CardTitle>
              <CardDescription>Технічні вимоги до виготовлення</CardDescription>
            </CardHeader>
            <CardContent>
              {docLoaded && documentUrl ? (
                <div className="space-y-3">
                  <iframe
                    src={documentUrl}
                    title="Замовлення на протез (PDF)"
                    className="w-full min-h-[520px] rounded-md border bg-white"
                  />
                  <a href={documentUrl} target="_blank" rel="noopener noreferrer" className="block">
                    <Button variant="outline" className="w-full">
                      <FileText className="mr-2 size-4" />
                      Завантажити замовлення на протез
                    </Button>
                  </a>
                </div>
              ) : docError ? (
                <Alert variant="destructive" className="w-full">
                  <AlertTitle>Помилка завантаження</AlertTitle>
                  <AlertDescription>{docError}</AlertDescription>
                </Alert>
              ) : (
                <div className="flex items-center justify-center min-h-[200px] rounded-md border border-dashed bg-muted/60">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Loader2 className="size-4 animate-spin" /> Завантаження замовлення на протез…
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="materials">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Package className="size-4" />
                Специфікація матеріалів (BOM)
              </CardTitle>
              <CardDescription>Список матеріалів для виготовлення</CardDescription>
            </CardHeader>
            <CardContent>
              {parseMaterials(order.materials).length > 0 ? (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>#</TableHead>
                      <TableHead>Матеріал</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {parseMaterials(order.materials).map((material, i) => (
                      <TableRow key={`${material}-${i}`}>
                        <TableCell>{i + 1}</TableCell>
                        <TableCell>{material}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              ) : (
                <p className="text-muted-foreground">
                  Специфікація матеріалів відсутня. Дані будуть додані на етапі виконання.
                </p>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {startError && (
        <Alert variant="destructive" className="mt-4">
          <AlertTitle>Неможливо розпочати</AlertTitle>
          <AlertDescription>{startError}</AlertDescription>
        </Alert>
      )}

      <div className="mt-8 flex flex-wrap justify-between gap-3">
        <Button variant="outline" onClick={() => navigate('/prosthetics/new/select-order')}>
          Назад
        </Button>
        <div className="flex gap-3">
          <Button variant="ghost" onClick={() => navigate('/prosthetics')}>
            До головного меню
          </Button>
          <Button
            disabled={!docLoaded || checking}
            className="bg-accent text-accent-foreground hover:bg-accent/90"
            onClick={() => void handleStart()}
          >
            {checking ? (
              <>
                <Loader2 className="mr-2 size-4 animate-spin" /> Перевірка…
              </>
            ) : docLoaded ? (
              'Старт'
            ) : (
              'Очікування замовлення на протез…'
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}