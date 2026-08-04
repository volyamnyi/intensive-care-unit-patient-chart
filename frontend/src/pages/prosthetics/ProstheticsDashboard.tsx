import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Syringe, Plus, FileText } from 'lucide-react';
import { Card, CardContent, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/services/AuthContext';

export default function ProstheticsDashboard() {
  const { user, selectApp } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    document.title = 'Виробництво протезів — Superhumans Lviv';
    selectApp('prosthetics');
  }, [selectApp]);

  const handleNewProcess = () => {
    navigate('/prosthetics/new/patient');
  };

  return (
    <div className="container mx-auto py-6">
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Syringe className="size-8 text-mint" />
          <h1 className="font-display text-2xl font-bold">Виробництво протезів</h1>
        </div>
        <Button onClick={handleNewProcess} className="gap-2">
          <Plus className="size-4" />
          Новий процес
        </Button>
      </div>

      <p className="mb-4 text-muted-foreground">
        Вітаю, {user?.fullName ?? ''}. Оберіть дію для початку роботи.
      </p>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Card
          className="cursor-pointer"
          onClick={handleNewProcess}
        >
          <CardContent className="pt-6">
            <div className="mb-3 flex items-center justify-center">
              <Plus className="size-10 text-mint" />
            </div>
            <CardTitle className="font-display text-center text-lg">
              Новий процес створення
            </CardTitle>
            <CardDescription>
              Створити новий технологічний процес для пацієнта
            </CardDescription>
          </CardContent>
        </Card>

        <Card
          className="cursor-pointer"
          onClick={() => navigate('/prosthetics')}
        >
          <CardContent className="pt-6">
            <div className="mb-3 flex items-center justify-center">
              <FileText className="size-10 text-mint" />
            </div>
            <CardTitle className="font-display text-center text-lg">
              Існуючі процеси
            </CardTitle>
            <CardDescription>
              Переглянути та керувати активними процесами
            </CardDescription>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
