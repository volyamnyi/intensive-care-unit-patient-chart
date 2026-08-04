import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, ChevronRight, FileText } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardTitle, CardDescription } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';
import type { FlowTemplate } from '@/prosthetics/types';

export default function TemplateStep() {
  const navigate = useNavigate();
  const { draft, setDraftField } = useProsthetics();
  const [templates, setTemplates] = useState<FlowTemplate[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    document.title = 'Вибір шаблону процесу — Виробництво протезів';
  }, []);

  useEffect(() => {
    const fetchTemplates = async () => {
      try {
        const res = await fetch('/api/prosthesis-manufacturing/templates', {
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token')}`,
          },
        });
        if (res.ok) {
          const data: FlowTemplate[] = await res.json();
          setTemplates(data);
        }
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    };
    fetchTemplates();
  }, []);

  const handleSelect = (templateId: string) => {
    setDraftField('templateId', templateId);
    navigate('/prosthetics/new/review');
  };

  if (!draft.orderId) {
    return (
      <div className="container mx-auto max-w-2xl py-8">
        <div className="mb-6 flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/order')}>
            <ChevronLeft className="size-4" />
            Назад
          </Button>
          <h1 className="font-display text-2xl font-bold">Вибір шаблону</h1>
        </div>
        <p className="text-muted-foreground">Спочатку оберіть замовлення.</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-2xl py-8">
      <div className="mb-6 flex items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/order')}>
          <ChevronLeft className="size-4" />
          Назад
        </Button>
        <h1 className="font-display text-2xl font-bold">Вибір шаблону процесу</h1>
      </div>

      {loading ? (
        <div className="space-y-2">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
        </div>
      ) : templates.length === 0 ? (
        <p className="text-muted-foreground">Немає доступних шаблонів процесів.</p>
      ) : (
        <div className="space-y-2">
          {templates.map((template) => (
            <Card
              key={template.id}
              className="cursor-pointer transition-colors hover:bg-accent"
              onClick={() => handleSelect(template.id)}
            >
              <CardContent className="pt-4">
                <CardTitle className="flex items-center gap-2 text-base">
                  <FileText className="size-4" />
                  {template.name}
                </CardTitle>
                <CardDescription>
                  {template.description || 'Без опису'}
                </CardDescription>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <div className="mt-6 flex justify-end">
        <Button
          onClick={() => navigate('/prosthetics/new/review')}
          disabled={!draft.templateId}
          className="gap-2"
        >
          Продовжити
          <ChevronRight className="size-4" />
        </Button>
      </div>
    </div>
  );
}
