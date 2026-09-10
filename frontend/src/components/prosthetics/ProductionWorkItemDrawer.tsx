import { useCallback, useEffect, useRef, useState } from 'react';
import { ExternalLink, FileText } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet';
import { productionApi } from '@/api/prosthetics';
import { useAuth } from '@/services/AuthContext';
import { getErrorMessage } from '@/utils/errorMessage';
import { formatDurationSeconds } from '@/pages/prosthetics/ProductionPage';
import type { FlowInstanceStatus, ProductionDetail } from '@/prosthetics/types';

const STATUS_LABELS: Record<FlowInstanceStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' | 'link' }> = {
  NEW: { label: 'Новий', variant: 'default' },
  IN_PROGRESS: { label: 'В процесі', variant: 'default' },
  PAUSED: { label: 'Призупинено', variant: 'outline' },
  BLOCKED_PATIENT: { label: 'Заблоковано (пацієнт)', variant: 'destructive' },
  BLOCKED_MATERIAL: { label: 'Заблоковано (матеріали)', variant: 'destructive' },
  COMPLETED: { label: 'Завершено', variant: 'default' },
  FAILED: { label: 'Завершено з помилкою', variant: 'destructive' },
  BRANCHED: { label: 'Розгалужено', variant: 'outline' },
};

function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('uk-UA');
}

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-xs text-muted-foreground">{label}</span>
      <span className="text-sm">{value ?? '—'}</span>
    </div>
  );
}

interface Props {
  instanceId: string | null;
  onClose: () => void;
  onOpenProcess: (instanceId: string, status: FlowInstanceStatus | null) => void;
}

export default function ProductionWorkItemDrawer({ instanceId, onClose, onOpenProcess }: Props) {
  const { hasPermission } = useAuth();
  const canSeeQuality = hasPermission('PROSTHETICS_PRODUCTION_QUALITY_VIEW');
  const [detail, setDetail] = useState<ProductionDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const fetchDetail = useCallback(async () => {
    if (!instanceId) return;
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    setLoading(true);
    setError(null);
    try {
      const res = await productionApi.detail(instanceId, controller.signal);
      setDetail(res.data);
    } catch (err) {
      if ((err as { code?: string })?.code === 'ERR_CANCELED') return;
      setError(getErrorMessage(err, 'Не вдалося завантажити деталі виробу'));
    } finally {
      setLoading(false);
    }
  }, [instanceId]);

  useEffect(() => {
    setDetail(null);
    void fetchDetail();
    return () => abortRef.current?.abort();
  }, [fetchDetail]);

  const workItem = detail?.workItem ?? null;
  const statusInfo = workItem?.status ? STATUS_LABELS[workItem.status] : null;

  return (
    <Sheet open={instanceId !== null} onOpenChange={(open) => { if (!open) onClose(); }}>
      <SheetContent side="right" className="w-[92vw] max-w-[560px] overflow-y-auto">
        <SheetHeader className="pr-8 text-left">
          <SheetTitle>{workItem?.productCode ?? 'Виріб'}</SheetTitle>
          <SheetDescription>
            {[workItem?.prosthetistFullName, workItem?.currentStageName]
              .filter(Boolean)
              .join(' · ') || 'Деталі виробу'}
          </SheetDescription>
        </SheetHeader>

        {loading && (
          <div className="mt-4 space-y-2">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
          </div>
        )}

        {error && !loading && (
          <Alert variant="destructive" className="mt-4">
            <AlertTitle>Помилка</AlertTitle>
            <AlertDescription className="flex flex-wrap items-center gap-2">
              {error}
              <Button variant="outline" size="sm" onClick={() => void fetchDetail()}>
                Спробувати знову
              </Button>
            </AlertDescription>
          </Alert>
        )}

        {!loading && !error && detail && workItem && (
          <div className="mt-4 space-y-4 pb-6">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Виріб</CardTitle>
              </CardHeader>
              <CardContent className="grid grid-cols-2 gap-3">
                <Field label="Статус" value={statusInfo ? <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge> : '—'} />
                <Field label="Протезист" value={workItem.prosthetistFullName} />
                <Field label="Етап" value={workItem.currentStageName} />
                <Field label="Крок" value={workItem.currentStepName} />
                <Field label="Старт" value={formatDateTime(workItem.startTime)} />
                <Field label="Завершення" value={formatDateTime(workItem.endTime)} />
                <div className="col-span-2">
                  <Button
                    variant="outline"
                    size="sm"
                    className="gap-2"
                    onClick={() => onOpenProcess(workItem.instanceId, workItem.status)}
                  >
                    <ExternalLink className="size-4" />
                    Відкрити процес
                  </Button>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Пацієнт</CardTitle>
              </CardHeader>
              <CardContent>
                {detail.patientDetailsVisible && detail.patient ? (
                  <div className="grid grid-cols-2 gap-3">
                    <Field label="ПІБ" value={detail.patient.pib} />
                    <Field label="ID" value={detail.patient.id} />
                    <Field label="Стать" value={detail.patient.gender} />
                    <Field
                      label="Дата народження"
                      value={detail.patient.birthDate ? new Date(detail.patient.birthDate).toLocaleDateString('uk-UA') : null}
                    />
                    <Field label="Зріст, см" value={detail.patient.heightCm} />
                    <Field label="Вага, кг" value={detail.patient.weightKg} />
                    <Field label="Мобільність" value={detail.matchedDocument?.mobilityLevel} />
                    <Field label="Адреса" value={detail.patient.residence} />
                    <Field label="Примітка" value={detail.matchedDocument?.note} />
                  </div>
                ) : (
                  <>
                    <Field label="ПІБ" value={detail.patient?.pib} />
                    <Alert className="mt-3">
                      <AlertDescription>
                        Деталі приховано — потрібен дозвіл PROSTHETICS_PRODUCTION_PATIENT_VIEW.
                      </AlertDescription>
                    </Alert>
                  </>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Замовлення</CardTitle>
              </CardHeader>
              <CardContent className="grid grid-cols-2 gap-3">
                <Field label="Номер" value={detail.order?.orderNumber} />
                <Field label="Код виробу" value={detail.order?.productCode} />
                <Field label="Назва виробу" value={detail.matchedDocument?.productName} />
                <Field
                  label="Дата замовлення"
                  value={detail.matchedDocument?.orderDate
                    ? new Date(detail.matchedDocument.orderDate).toLocaleDateString('uk-UA')
                    : detail.order?.prescriptionDate
                      ? new Date(detail.order.prescriptionDate).toLocaleDateString('uk-UA')
                      : null}
                />
                <Field label="Шаблон документа" value={detail.matchedDocument?.documentTemplateName} />
                <Field
                  label="Документ створено"
                  value={formatDateTime(detail.matchedDocument?.documentCreationDate)}
                />
                {detail.patientDetailsVisible && detail.matchedDocument?.documentUrl ? (
                  <div className="col-span-2">
                    <Button
                      variant="outline"
                      size="sm"
                      className="gap-2"
                      onClick={() => window.open(detail.matchedDocument?.documentUrl as string, '_blank', 'noopener')}
                    >
                      <FileText className="size-4" />
                      Відкрити документ
                    </Button>
                  </div>
                ) : null}
                {detail.documentsUnknown && (
                  <div className="col-span-2 text-sm text-muted-foreground">
                    Документи MIS недоступні для цього пацієнта.
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Час виробництва</CardTitle>
              </CardHeader>
              <CardContent className="grid grid-cols-2 gap-3">
                <Field label="Календарний час" value={formatDurationSeconds(workItem.elapsedSeconds)} />
                <Field label="Активний час" value={formatDurationSeconds(workItem.activeSeconds)} />
                <Field label="Очікування" value={formatDurationSeconds(workItem.idleSeconds)} />
                <Field
                  label="Норматив"
                  value={workItem.expectedActiveSeconds != null
                    ? formatDurationSeconds(workItem.expectedActiveSeconds)
                    : 'не задано'}
                />
                <Field
                  label="Відхилення"
                  value={workItem.activeDeviationSeconds != null ? (
                    <span className={workItem.activeDeviationSeconds > 0 ? 'text-red-500' : 'text-green-600'}>
                      {workItem.activeDeviationSeconds > 0 ? '+' : ''}
                      {formatDurationSeconds(workItem.activeDeviationSeconds)}
                    </span>
                  ) : '—'}
                />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Хронологія кроків</CardTitle>
              </CardHeader>
              <CardContent>
                {detail.timeline.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Кроки ще не розпочато.</p>
                ) : (
                  <ol className="space-y-3">
                    {detail.timeline.map((exec) => (
                      <li key={exec.id} className="border-l-2 border-muted pl-3">
                        <div className="flex flex-wrap items-center gap-2">
                          <Badge variant={exec.status === 'COMPLETED' ? 'default' : 'outline'}>
                            {exec.status}
                          </Badge>
                          <span className="font-mono text-xs text-muted-foreground">
                            {exec.stepId.slice(0, 8)} · спроба {exec.attemptNumber}
                          </span>
                        </div>
                        <div className="mt-1 text-xs text-muted-foreground">
                          {formatDateTime(exec.startedAt)}
                          {exec.completedAt ? ` → ${formatDateTime(exec.completedAt)}` : ''}
                          {' · '}
                          активно {formatDurationSeconds(exec.activeSeconds)}
                        </div>
                        {exec.note && <p className="mt-1 text-sm">{exec.note}</p>}
                      </li>
                    ))}
                  </ol>
                )}
              </CardContent>
            </Card>

            {canSeeQuality && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Якість</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="grid grid-cols-3 gap-3">
                    <Field label="Браки" value={String(workItem.brakCount)} />
                    <Field label="Rework" value={String(workItem.reworkCount)} />
                    <Field label="Провал" value={workItem.failed ? 'Так' : 'Ні'} />
                  </div>
                  {detail.brakEvents.length > 0 && (
                    <div>
                      <p className="mb-2 text-sm font-medium">Браки</p>
                      <ol className="space-y-2">
                        {detail.brakEvents.map((brak) => (
                          <li key={brak.id} className="rounded-lg border p-2 text-sm">
                            <div>{brak.note ?? 'Без опису'}</div>
                            <div className="mt-1 text-xs text-muted-foreground">
                              {brak.returnStageName ? `Повернено: ${brak.returnStageName}. ` : ''}
                              {formatDateTime(brak.createdAt)}
                            </div>
                          </li>
                        ))}
                      </ol>
                    </div>
                  )}
                  {detail.branches.length > 0 && (
                    <div>
                      <p className="mb-2 text-sm font-medium">Повторні проходження</p>
                      <ol className="space-y-2">
                        {detail.branches.map((branch) => (
                          <li key={branch.id} className="rounded-lg border p-2 text-sm">
                            <div className="flex flex-wrap items-center gap-2">
                              <Badge variant="outline">
                                {branch.status ? STATUS_LABELS[branch.status]?.label ?? branch.status : '—'}
                              </Badge>
                              <span className="font-mono text-xs text-muted-foreground">
                                #{branch.id.slice(0, 8)}
                              </span>
                            </div>
                          </li>
                        ))}
                      </ol>
                    </div>
                  )}
                </CardContent>
              </Card>
            )}
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
