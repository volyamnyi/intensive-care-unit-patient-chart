import { useState, useEffect, useRef } from 'react';
import { Hospital, FileText, Plus, Eye, EyeOff } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card } from '@/components/ui/card';
import { useAuth } from '../services/AuthContext';

const platformApps = [
  { icon: <Hospital className="size-5 text-info" />, label: 'Карта інтенсивної терапії' },
  { icon: <FileText className="size-5 text-success" />, label: 'Листок лікарських призначень' },
];

export default function LoginPage() {
  useEffect(() => { document.title = 'Вхід — Superhumans Lviv'; }, []);
  const { login } = useAuth();
  const [loginField, setLoginField] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const formRef = useRef<HTMLFormElement>(null);

  const handleButtonClick = () => {
    formRef.current?.requestSubmit();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await login({ login: loginField, password });
      window.location.href = '/';
    } catch {
      setError('Невірний логін або пароль');
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-background">
      <div className="pointer-events-none absolute -top-[30%] -right-[10%] size-[60%] rounded-full bg-gradient-radial from-primary/8 to-transparent to-70%" />
      <div className="pointer-events-none absolute -bottom-[20%] -left-[10%] size-[50%] rounded-full bg-gradient-radial from-primary/6 to-transparent to-70%" />

      <Card className="fade-in-up relative flex w-[calc(100%-32px)] flex-col overflow-hidden border-border bg-card shadow-lg sm:w-[800px] sm:flex-row">
        <div className="flex flex-col justify-center p-3 sm:p-4 bg-accent sm:border-r sm:border-border">
          <img
            src="/superhumans.svg"
            alt="Superhumans"
            className="mb-2.5 h-10 w-auto dark:hidden"
          />
          <img
            src="/superhumans-white.svg"
            alt="Superhumans"
            className="mb-2.5 hidden h-10 w-auto dark:block"
          />
          <div className="font-rubik mb-2.5 text-[15px] font-bold leading-relaxed text-foreground">
            Веб додаток до Медичної інформаційної системи
          </div>

          <div className="mb-1.5 text-[10px] font-semibold uppercase tracking-[0.5px] text-muted-foreground">
            Модулі платформи
          </div>
          <div className="mb-3 flex flex-col gap-1">
            {platformApps.map((app) => (
              <div
                key={app.label}
                className="flex items-center gap-1.5 rounded-xl border border-border bg-card/80 p-1.5 shadow-none"
              >
                {app.icon}
                <span className="text-xs font-semibold text-muted-foreground">
                  {app.label}
                </span>
              </div>
            ))}
            <div className="flex items-center gap-1.5 rounded-xl border border-dashed border-border/60 p-1.5 shadow-none">
              <Plus className="size-5 text-muted-foreground/70" />
              <span className="text-xs italic text-muted-foreground/70">
                Ще більше модулів
              </span>
            </div>
          </div>

          <div className="text-[11px] font-bold uppercase tracking-[0.5px] text-primary">
            Вхід до системи
          </div>
        </div>

        <div className="flex flex-1 flex-col justify-center p-3 sm:p-4">
          <div className="font-rubik mb-0.5 text-base font-bold text-foreground">
            Ласкаво просимо
          </div>
          <div className="mb-3 text-[13px] text-muted-foreground">
            Увійдіть, щоб продовжити роботу
          </div>

          {error && (
            <Alert variant="destructive" className="mb-2 rounded-xl">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          <form onSubmit={handleSubmit} ref={formRef} className="flex flex-col gap-3.5">
            <div>
              <label htmlFor="login" className="mb-1 block text-xs font-medium text-muted-foreground">Логін</label>
              <Input
                id="login"
                value={loginField}
                onChange={(e) => setLoginField(e.target.value)}
                required
              />
            </div>
            <div>
              <label htmlFor="password" className="mb-1 block text-xs font-medium text-muted-foreground">Пароль</label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="pr-10"
                  required
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="absolute right-1 top-1/2 -translate-y-1/2"
                  onClick={() => setShowPassword((p) => !p)}
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                </Button>
              </div>
            </div>
            <Button
              type="submit"
              className="w-full"
              size="lg"
              onClick={handleButtonClick}
            >
              Увійти
            </Button>
          </form>
        </div>
      </Card>
    </div>
  );
}
