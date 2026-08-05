import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Package, Check, Clock, Layers } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import { flowTemplateApi } from '@/api/prosthetics';
import { flowInstanceApi } from '@/api/prosthetics';
import type { FlowTemplate } from '@/prosthetics/types';
import { SetupSteps } from '@/components/prosthetics/SetupSteps';

export default function TemplateSelectPage() {
  const navigate = useNavigate();
  const { draft, setDraftField, resetDraft } = useProsthetics();
  const [templates, setTemplates] = useState<FlowTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  const filters = useMemo(() => ({
    productType: 'протез',
    amputationLevel: 'both',
    limbSide: 'both',
  }), []);

  useEffect(() => {
    document.title = 'Вибір шаблону — Виробництво протезів';
  }, []);

  useEffect(() => {
    if (!draft.orderId) {
      navigate('/prosthetics/new/select-order');
      return;
    }
    const fetchTemplates = async () => {
      setLoading(true);
      try {
        const res = await flowTemplateApi.list({
          status: 'ACTIVE',
          productType: filters.productType,
          amputationLevel: filters.amputationLevel,
          limbSide: filters.limbSide,
        });
        setTemplates(res.data);
      } catch {
        setError('Не вдалося завантажити шаблони');
      } finally {
        setLoading(false);
      }
    };
    fetchTemplates();
  }, [draft.orderId, navigate, filters]);

  const handleSelect = async () => {
    if (!selectedTemplateId || !draft.orderId) return;
    setCreating(true);
    setError(null);
    try {
      setDraftField('templateId', selectedTemplateId);
      const res = await flowInstanceApi.create({
        orderId: draft.orderId,
        templateId: selectedTemplateId,
      });
      resetDraft();
      navigate(`/prosthetics/process/${res.data.id}`);
    } catch (err: any) {
      if (err.response?.status === 409) {
        setError('Процес для цього замовлення вже існує');
      } else {
        setError(err.response?.data?.message || 'Не вдалося створити процес');
      }
    } finally {
      setCreating(false);
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
          <div>
            <h1 className="font-display text-2xl font-bold">Вибір шаблону</h1>
            <p className="text-sm text-muted-foreground">Крок 4 з 4</p>
          </div>
          <SetupSteps current={4} className="ml-auto" />
        </div>
        <p className="text-muted-foreground">Необхідно обрати замовлення.</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-2xl py-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold">Вибір технологічного маршруту</h1>
          <p className="text-sm text-muted-foreground">Крок 4 з 4</p>
        </div>
        <SetupSteps current={4} />
      </div>

      <Card className="mb-6 border-mint bg-mint/40">
        <CardContent className="grid gap-3 text-sm sm:grid-cols-4">
          <div>
            <div className="text-xs text-mint-foreground/60">Пацієнт</div>
            <div className="font-medium">{draft.patientId || '—'}</div>
          </div>
          <div>
            <div className="text-xs text-mint-foreground/60">Замовлення</div>
            <div className="font-medium">{draft.orderId || '—'}</div>
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
        <Skeleton className="h-64 w-full" />
      ) : templates.length > 0 ? (
        <div className="space-y-3">
          {templates.map((template) => (
            <Card
              key={template.id}
              className={`cursor-pointer transition-colors ${selectedTemplateId === template.id ? 'ring-2 ring-primary' : 'hover:bg-accent'}`}
              onClick={() => setSelectedTemplateId(template.id)}
            >
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between gap-2">
                  <CardTitle className="text-base flex items-center gap-2">
                    <Package className="size-4" />
                    {template.name}
                    {selectedTemplateId === template.id && <Check className="size-4 text-primary" />}
                  </CardTitle>
                  <Badge className="border-transparent bg-green-500 text-white">Активний</Badge>
                </div>
                <CardDescription>
                  {template.description || 'Без опису'}
                </CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-4">
                <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
                  <span className="inline-flex items-center gap-1">
                    <Layers className="size-3.5" /> {template.stages.length} етапів
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <Clock className="size-3.5" /> ~{Math.round(template.estimatedDurationMin / 60)} год
                  </span>
                  <span>Версія {template.templateVersion}</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Badge variant="outline">{template.productType}</Badge>
                  <Badge variant="outline">
                    {template.amputationLevel === 'above' ? 'Вище коліна' : 'Нижче коліна'}
                  </Badge>
                  <Badge variant="outline">
                    {template.limbSide === 'left' ? 'Лівий' : 'Правий'}
                  </Badge>
                </div>
                <Button
                  className="mt-auto bg-accent text-accent-foreground hover:bg-accent/90"
                  onClick={handleSelect}
                  disabled={creating || selectedTemplateId !== template.id}
                >
                  {creating && selectedTemplateId === template.id ? (
                    <>
                      <span className="animate-spin mr-2">⏳</span>
                      Створення…
                    </>
                  ) : 'Обрати'}
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <div className="py-12 text-center">
          <p className="text-muted-foreground">Немає доступних шаблонів</p>
        </div>
      )}

      <div className="mt-6 flex gap-3">
        <Button variant="outline" onClick={() => navigate('/prosthetics/new/review-order')}>
          Назад
        </Button>
        <Button variant="ghost" onClick={() => navigate('/prosthetics')}>
          Скасувати
        </Button>
      </div>
    </div>
  );
}