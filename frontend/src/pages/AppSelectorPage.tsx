import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Hospital, FileText, Shield, Syringe } from 'lucide-react';
import { Card, CardContent, CardTitle, CardDescription } from '@/components/ui/card';
import { useAuth } from '../services/AuthContext';

const cards = [
  {
    app: 'icu' as const,
    title: 'Карта інтенсивної терапії',
    subtitle: 'Відділення анестезіології та інтенсивної терапії',
    icon: <Hospital className="size-12" />,
    color: '#1976d2',
    path: '/icu/doctor',
  },
  {
    app: 'prescriptions' as const,
    title: 'Листок лікарських призначень',
    subtitle: 'Форма 003-4/о — медикаментозні призначення',
    icon: <FileText className="size-12" />,
    color: '#2e7d32',
    path: '/prescriptions/doctor',
  },
  {
    app: 'prosthetics' as const,
    title: 'Виробництво протезів',
    subtitle: 'Технологічні процеси протезування',
    icon: <Syringe className="size-12" />,
    color: '#059669',
    path: '/prosthetics',
  },
];

export default function AppSelectorPage() {
  const { user, selectApp, hasRole } = useAuth();
  const navigate = useNavigate();
  useEffect(() => { document.title = 'Вибір додатку — Superhumans Lviv'; }, []);

  const handleSelect = (card: (typeof cards)[0]) => {
    selectApp(card.app);
    let target = card.path;
    if (target === '/icu/doctor' && hasRole('NURSE')) {
      target = '/icu/nurse';
    }
    if (target === '/prescriptions/doctor' && hasRole('NURSE')) {
      target = '/prescriptions/nurse';
    }
    navigate(target, { replace: true });
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background p-3">
      <h1 className="font-rubik mb-1 text-2xl font-extrabold">
        Superhumans Lviv
      </h1>
      <p className="mb-4 text-muted-foreground">
        {`Вітаємо, ${user?.fullName ?? ''}`}
      </p>
      <h2 className="mb-3 text-base font-semibold">
        Оберіть додаток для роботи
      </h2>
      <div className="flex max-w-[700px] flex-wrap justify-center gap-3">
        {cards.map((card) => (
          <Card key={card.app} className="w-[300px] cursor-pointer" onClick={() => handleSelect(card)}>
            <CardContent className="flex flex-col items-center p-4 text-center">
              <div className="mb-1" style={{ color: card.color }}>{card.icon}</div>
              <CardTitle className="font-rubik text-base font-semibold">{card.title}</CardTitle>
              <CardDescription>{card.subtitle}</CardDescription>
            </CardContent>
          </Card>
        ))}
        {hasRole('ADMINISTRATOR') && (
          <Card className="w-[300px] cursor-pointer" onClick={() => navigate('/admin', { replace: true })}>
            <CardContent className="flex flex-col items-center p-4 text-center">
              <div className="mb-1" style={{ color: '#7b1fa2' }}>
                <Shield className="size-12" />
              </div>
              <CardTitle className="font-rubik text-base font-semibold">Адміністративна панель</CardTitle>
              <CardDescription>Керування доступом, аудит, налаштування</CardDescription>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
