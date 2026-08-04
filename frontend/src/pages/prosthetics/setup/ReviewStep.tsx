import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Save, Send } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardTitle, CardDescription } from '@/components/ui/card';
import { useProsthetics } from '@/prosthetics/ProstheticsContext';

export default function ReviewStep() {
  const navigate = useNavigate();
  const { draft, resetDraft, patient } = useProsthetics();

  useEffect(() => {
    document.title = 'Перевірка даних — Виробництво протезів';
  }, []);

  if (!draft.patientId || !draft.orderId || !draft.templateId) {
    return (
      <div className="container mx-auto max-w-2xl py-8">
        <div className="mb-6 flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/patient')}>
            <ChevronLeft className="size-4" />
            Назад
          </Button>
          <h1 className="font-display text-2xl font-bold">Перевірка даних</h1>
        </div>
        <p className="text-muted-foreground">
          Будь ласка, заповніть усі попередні кроки.
        </p>
      </div>
    );
  }

  const handleSubmit = async () => {
    try {
      await fetch('/api/prosthesis-manufacturing/instances', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify({
          orderId: draft.orderId,
          templateId: draft.templateId,
          patientId: draft.patientId,
        }),
      });
      resetDraft();
      navigate('/prosthetics');
    } catch {
      // handle error
    }
  };

  return (
    <div className="container mx-auto max-w-2xl py-8">
      <div className="mb-6 flex items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate('/prosthetics/new/template')}>
          <ChevronLeft className="size-4" />
          Назад
        </Button>
        <h1 className="font-display text-2xl font-bold">Перевірка даних</h1>
      </div>

      <div className="mb-6 space-y-3">
        <Card>
          <CardContent className="pt-4">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Пацієнт
            </CardTitle>
            <CardDescription className="text-base">
              {patient?.fullName || draft.patientId}
            </CardDescription>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-4">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Замовлення
            </CardTitle>
            <CardDescription className="text-base">
              {draft.orderId}
            </CardDescription>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-4">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Шаблон процесу
            </CardTitle>
            <CardDescription className="text-base">
              {draft.templateId}
            </CardDescription>
          </CardContent>
        </Card>
      </div>

      <div className="flex justify-end gap-3">
        <Button variant="outline" onClick={resetDraft}>
          <Save className="size-4 mr-2" />
          Зберегти черновик
        </Button>
        <Button onClick={handleSubmit} className="gap-2">
          <Send className="size-4" />
          Створити процес
        </Button>
      </div>
    </div>
  );
}
