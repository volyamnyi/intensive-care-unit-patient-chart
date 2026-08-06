import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, FileText, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { prostheticsOrderApi } from '@/api/prosthetics';
import type { ProstheticsOrder, ProstheticsPatient } from '@/prosthetics/types';
import { SetupSteps } from '@/components/prosthetics/SetupSteps';

interface OrderWithPatient extends ProstheticsOrder {
  patient: ProstheticsPatient | null;
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
            const patientRes = await fetch(`/api/prosthesis-manufacturing/patients/${orderRes.data.patientId}`, {
              headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
            });
            if (patientRes.ok) {
              patient = await patientRes.json();
            }
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
          setDocError(axiosError.response?.data?.message || 'Не вдалося завантажити рецепт');
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

  const handleStart = () => {
    if (!draft.orderId) return;
    navigate('/prosthetics/new/select-template');
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
          <TabsTrigger value="document">Рецепт</TabsTrigger>
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
                  Рівень ампутації: {order.amputationLevel === 'above' ? 'Вище коліна' : 'Нижче коліна'}
                </p>
                <p className="text-sm text-muted-foreground">Бік: {order.limbSide === 'left' ? 'Лівий' : 'Правий'}</p>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="document">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>Рецепт протезу</CardTitle>
              <CardDescription>Технічні вимоги до виготовлення</CardDescription>
            </CardHeader>
            <CardContent>
              {docLoaded && documentUrl ? (
                <div className="space-y-3">
                  <iframe
                    src={documentUrl}
                    title="Рецепт протезу (PDF)"
                    className="w-full min-h-[520px] rounded-md border bg-white"
                  />
                  <a href={documentUrl} target="_blank" rel="noopener noreferrer" className="block">
                    <Button variant="outline" className="w-full">
                      <FileText className="mr-2 size-4" />
                      Завантажити рецепт
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
                    <Loader2 className="size-4 animate-spin" /> Завантаження рецепта…
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="materials">
          <Card>
            <CardHeader>
              <CardTitle>Специфікація матеріалів (BOM)</CardTitle>
              <CardDescription>Список матеріалів для виготовлення</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground">Специфікація матеріалів відсутня. Дані будуть додані на етапі виконання.</p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      <div className="mt-8 flex flex-wrap justify-between gap-3">
        <Button variant="outline" onClick={() => navigate('/prosthetics/new/select-order')}>
          Назад
        </Button>
        <div className="flex gap-3">
          <Button variant="ghost" onClick={() => navigate('/prosthetics')}>
            До головного меню
          </Button>
          <Button
            disabled={!docLoaded}
            className="bg-accent text-accent-foreground hover:bg-accent/90"
            onClick={handleStart}
          >
            {docLoaded ? 'Старт' : 'Очікування рецепта…'}
          </Button>
        </div>
      </div>
    </div>
  );
}