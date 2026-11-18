import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Check, Clock, FileText, Layers, Loader2, Package } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '@/components/ui/table';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { flowTemplateApi, flowInstanceApi, prostheticsOrderApi, prostheticsPatientApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';
import type { FlowTemplate, ProstheticsOrder, ProstheticsPatient } from '@/prosthetics/types';
import { SetupSteps } from '@/components/prosthetics/SetupSteps';

interface OrderWithPatient extends ProstheticsOrder {
  patient: ProstheticsPatient | null;
}

const ACTIVE_DUPLICATE_STATUSES = ['NEW', 'IN_PROGRESS', 'PAUSED', 'BLOCKED_PATIENT', 'BLOCKED_MATERIAL'];

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

type RouteMode = 'auto' | 'exact' | 'fallback' | 'none';

export default function OrderReviewPage() {
  const navigate = useNavigate();
  const { draft, setDraftField, resetDraft } = useProsthetics();
  const [order, setOrder] = useState<OrderWithPatient | null>(null);
  const [documentUrl, setDocumentUrl] = useState<string | null>(null);
  const [docTemplateName, setDocTemplateName] = useState<string | null>(null);
  const [docLoaded, setDocLoaded] = useState(false);
  const [docError, setDocError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [startError, setStartError] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);
  const [templates, setTemplates] = useState<FlowTemplate[]>([]);
  const [templatesLoading, setTemplatesLoading] = useState(false);
  const [templatesError, setTemplatesError] = useState<string | null>(null);
  const [routeMode, setRouteMode] = useState<RouteMode>('none');
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Перевірка замовлення — Виробництво протезів';
  }, []);

  // The MIS document picked on step 2 (draft.misDocumentUrl) takes
  // precedence: it feeds the «Замовлення на протез» tab directly without
  // resolving anything through the local order.
  const draftDocUrl = draft.misDocumentUrl ?? null;
  const draftDocTemplate = draft.misDocumentTemplateName ?? null;

  useEffect(() => {
    if (!draft.orderId && !draftDocUrl) {
      navigate('/prosthetics/new/select-order');
      return;
    }

    const loadTemplates = async (orderData: ProstheticsOrder) => {
      setTemplatesLoading(true);
      setTemplatesError(null);
      setTemplates([]);
      setRouteMode('none');
      setSelectedTemplateId(null);
      // A restored selection may belong to a different order/template set.
      // Only a fresh unique exact match is auto-selected.
      setDraftField('templateId', null);
      try {
        const exact = orderData.amputationLevel
          ? (
              await flowTemplateApi.list({
                status: 'ACTIVE',
                productType: orderData.productType,
                amputationLevel: orderData.amputationLevel,
              })
            ).data
          : [];
        if (exact.length > 0) {
          setTemplates(exact);
          if (exact.length === 1) {
            setRouteMode('auto');
            setSelectedTemplateId(exact[0].id);
            setDraftField('templateId', exact[0].id);
          } else {
            setRouteMode('exact');
          }
          return;
        }

        // No exact match: the productType-only list is a manual fallback and
        // must never be used for automatic selection (decision contract #449).
        const fallbackRes = await flowTemplateApi.list({
          status: 'ACTIVE',
          productType: orderData.productType,
        });
        const fallback = fallbackRes.data;
        setTemplates(fallback);
        setRouteMode(fallback.length > 0 ? 'fallback' : 'none');
      } catch {
        setTemplatesError('Не вдалося завантажити шаблони');
        setRouteMode('none');
      } finally {
        setTemplatesLoading(false);
      }
    };

    const fetchData = async () => {
      setLoading(true);
      setDocLoaded(false);
      setDocError(null);
      setDocumentUrl(null);
      try {
        if (draftDocUrl) {
          setDocumentUrl(draftDocUrl);
          setDocTemplateName(draftDocTemplate);
          setDocLoaded(true);
        }

        if (draft.orderId) {
          const orderRes = await prostheticsOrderApi.getById(draft.orderId);

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
          void loadTemplates(orderRes.data);

          if (!draftDocUrl) {
            try {
              const urlRes = await prostheticsOrderApi.getDocumentUrl(draft.orderId);
              setDocumentUrl(urlRes.data.documentUrl);
              setDocTemplateName(urlRes.data.documentTemplateName ?? null);
              setDocLoaded(true);
            } catch (err: unknown) {
              const axiosError = err as { response?: { data?: { message?: string } } };
              setDocError(axiosError.response?.data?.message || 'Не вдалося завантажити замовлення на протез');
              setDocLoaded(true);
            }
          }
        } else {
          setOrder(null);
        }
      } catch {
        setError('Не вдалося завантажити дані');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [draft.orderId, draftDocUrl, navigate]);

  const handleSelectTemplate = (templateId: string) => {
    setSelectedTemplateId(templateId);
    setDraftField('templateId', templateId);
  };

  const handleStart = async () => {
    if (!draft.orderId || !selectedTemplateId) return;
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
      const created = await flowInstanceApi.create({
        orderId: draft.orderId,
        templateId: selectedTemplateId,
      });
      resetDraft();
      navigate(`/prosthetics/process/${created.data.id}`);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 400 || status === 409) {
        setStartError(getErrorMessage(err, 'Процес для цього замовлення вже існує'));
      } else {
        setStartError(getErrorMessage(err, 'Не вдалося створити процес'));
      }
    } finally {
      setChecking(false);
    }
  };

  if (!draft.orderId && !draftDocUrl) {
    return (
      <div className="max-w-page-narrow mx-auto py-8">
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
      <div className="max-w-page-narrow mx-auto py-8">
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

  if (error || (!order && !draftDocUrl)) {
    return (
      <div className="max-w-page-narrow mx-auto py-8">
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
    <div className="max-w-page-narrow mx-auto py-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold">Перевірка замовлення</h1>
          <p className="text-sm text-muted-foreground">
            Крок 3 з 3{order ? ` · ${order.orderNumber}` : draftDocTemplate ? ` · ${draftDocTemplate}` : ''}
          </p>
        </div>
        <SetupSteps current={3} />
      </div>

      {draft.orderId && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Технологічний маршрут</CardTitle>
            <CardDescription>
              {routeMode === 'auto'
                ? 'Автоматично обрано єдиний сумісний активний шаблон'
                : routeMode === 'fallback'
                  ? 'Точного збігу за рівнем ампутації немає — показано шаблони лише за типом виробу'
                  : routeMode === 'exact'
                    ? 'Доступно кілька сумісних маршрутів — оберіть один'
                    : 'Завантаження доступних маршрутів'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {templatesLoading ? (
              <Skeleton className="h-24 w-full" />
            ) : templatesError ? (
              <Alert variant="destructive">
                <AlertTitle>Помилка</AlertTitle>
                <AlertDescription>{templatesError}</AlertDescription>
              </Alert>
            ) : templates.length === 0 ? (
              <Alert variant="destructive">
                <AlertTitle>Немає доступних маршрутів</AlertTitle>
                <AlertDescription>
                  Для цього замовлення немає активного шаблону. Створення процесу неможливе.
                </AlertDescription>
              </Alert>
            ) : (
              <div className="space-y-3" role="radiogroup" aria-label="Технологічні маршрути">
                {templates.map((template) => {
                  const selected = selectedTemplateId === template.id;
                  return (
                    <label
                      key={template.id}
                      className={`block cursor-pointer rounded-lg border p-3 transition-colors ${
                        selected ? 'border-primary bg-primary/5 ring-1 ring-primary' : 'hover:bg-muted/50'
                      }`}
                    >
                      <input
                        type="radio"
                        name="prosthetics-route"
                        className="sr-only"
                        checked={selected}
                        onChange={() => handleSelectTemplate(template.id)}
                      />
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="font-medium">{template.name}</div>
                          <div className="text-sm text-muted-foreground">
                            {template.description || 'Без опису'}
                          </div>
                          <div className="mt-2 flex flex-wrap gap-3 text-xs text-muted-foreground">
                            <span className="inline-flex items-center gap-1">
                              <Layers className="size-3.5" /> {template.stages.length} етапів
                            </span>
                            <span className="inline-flex items-center gap-1">
                              <Clock className="size-3.5" /> ~{Math.round(template.estimatedDurationMin / 60)} год
                            </span>
                            <span>Версія {template.templateVersion}</span>
                          </div>
                          <div className="mt-2 flex flex-wrap gap-2">
                            <Badge variant="outline">{template.productType}</Badge>
                            <Badge variant="outline">{template.amputationLevel}</Badge>
                            <Badge variant="outline">{template.limbSide}</Badge>
                            {routeMode === 'auto' && selected && (
                              <Badge variant="secondary">Автоматично обрано</Badge>
                            )}
                            {routeMode === 'fallback' && (
                              <Badge variant="secondary">За типом виробу</Badge>
                            )}
                          </div>
                        </div>
                        {selected && <Check className="size-4 shrink-0 text-primary" />}
                      </div>
                    </label>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <Tabs defaultValue="document">
        <TabsList className="mb-4">
          <TabsTrigger value="details">Деталі</TabsTrigger>
          <TabsTrigger value="document">Замовлення на протез</TabsTrigger>
          <TabsTrigger value="materials">Матеріали</TabsTrigger>
        </TabsList>

        <TabsContent value="details">
          {order ? (
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
          ) : (
            <p className="text-muted-foreground">
              Локальне замовлення не обрано — показано документ MIS, обраний на кроці 2.
            </p>
          )}
        </TabsContent>

        <TabsContent value="document">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>Замовлення на протез</CardTitle>
              <CardDescription>
                {docTemplateName ? `MIS: ${docTemplateName}` : 'Технічні вимоги до виготовлення'}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {docLoaded && documentUrl ? (
                <div className="space-y-3">
                  <iframe
                    src={documentUrl}
                    title="Замовлення на протез (MIS)"
                    className="w-full min-h-[520px] rounded-md border bg-white"
                  />
                  <a href={documentUrl} target="_blank" rel="noopener noreferrer" className="block">
                    <Button variant="outline" className="w-full">
                      <FileText className="mr-2 size-4" />
                      Відкрити замовлення на протез у MIS
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
              {order && parseMaterials(order.materials).length > 0 ? (
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

      <div className="sticky bottom-0 z-10 -mx-4 mt-8 flex flex-col gap-3 border-t bg-background/95 px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] backdrop-blur sm:-mx-6 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between sm:px-6 sm:pb-3">
        <Button variant="outline" className="w-full sm:w-auto" onClick={() => navigate('/prosthetics/new/select-order')}>
          Назад
        </Button>
        <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
          <Button variant="ghost" className="w-full sm:w-auto" onClick={() => navigate('/prosthetics')}>
            До головного меню
          </Button>
          {draft.orderId ? (
            <Button
              disabled={
                !docLoaded ||
                templatesLoading ||
                !!templatesError ||
                !selectedTemplateId ||
                routeMode === 'none' ||
                checking
              }
              className="w-full bg-accent text-accent-foreground hover:bg-accent/90 sm:w-auto"
              onClick={() => void handleStart()}
            >
              {checking ? (
                <>
                  <Loader2 className="mr-2 size-4 animate-spin" /> Перевірка…
                </>
              ) : !docLoaded ? (
                'Очікування замовлення на протез…'
              ) : templatesLoading ? (
                'Завантаження маршрутів…'
              ) : templatesError ? (
                'Помилка завантаження маршрутів'
              ) : routeMode === 'none' ? (
                'Немає доступних маршрутів'
              ) : !selectedTemplateId ? (
                'Оберіть маршрут'
              ) : (
                'Старт'
              )}
            </Button>
          ) : (
            <p className="text-sm text-muted-foreground sm:self-center">
              Для старту процесу оберіть локальне замовлення на кроці 2.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
