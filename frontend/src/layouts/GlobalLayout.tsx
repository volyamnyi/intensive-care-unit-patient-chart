import { Outlet, useNavigate, Link as RouterLink, useLocation } from 'react-router-dom';
import { useMemo } from 'react';
import { UserCircle } from 'lucide-react';
import { useAuth } from '../services/AuthContext';
import { useThemeMode } from '../styles/ThemeContext';
import ThemeToggle from '../components/common/ThemeToggle';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu';

import AppSidebar from '../components/navigation/AppSidebar';
import Breadcrumbs from '../components/navigation/Breadcrumbs';

function roleLabel(role?: string) {
  if (role === 'HEAD_OF_DEPARTMENT') return 'Завідувач відділення';
  if (role === 'DOCTOR') return 'Лікар';
  if (role === 'NURSE') return 'Медсестра';
  if (role === 'ADMINISTRATOR') return 'Адміністратор';
  if (role === 'AUDITOR') return 'Аудитор';
  return role ?? '';
}

interface AppInfo {
  title: string;
  subtitle: string;
  homePath: string;
}

function useAppInfo(): AppInfo {
  const { pathname } = useLocation();
  return useMemo(() => {
    if (pathname.startsWith('/icu/nurse')) {
      return { title: 'ВАІТ', subtitle: 'Карта інтенсивної терапії', homePath: '/icu/nurse' };
    }
    if (pathname.startsWith('/icu')) {
      return { title: 'ВАІТ', subtitle: 'Карта інтенсивної терапії', homePath: '/icu/doctor' };
    }
    if (pathname.startsWith('/prescriptions/nurse')) {
      return { title: 'Призначення', subtitle: 'Виконання лікарських призначень', homePath: '/prescriptions/nurse' };
    }
    if (pathname.startsWith('/prescriptions')) {
      return { title: 'Призначення', subtitle: 'Листок лікарських призначень', homePath: '/prescriptions/doctor' };
    }
    if (pathname.startsWith('/nurse')) {
      return { title: 'ВАІТ', subtitle: 'Карта інтенсивної терапії', homePath: '/icu/nurse' };
    }
    if (pathname.startsWith('/doctor')) {
      return { title: 'ВАІТ', subtitle: 'Карта інтенсивної терапії', homePath: '/icu/doctor' };
    }
    if (pathname.startsWith('/admin')) {
      return { title: 'Адмін', subtitle: 'Адміністративна панель', homePath: '/admin' };
    }
    return { title: 'Superhumans Lviv', subtitle: 'Вибір додатку', homePath: '/select' };
  }, [pathname]);
}

export default function GlobalLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { mode } = useThemeMode();
  const app = useAppInfo();

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <div className="flex min-h-screen bg-background">
      <AppSidebar />

      <div className="flex flex-col flex-1 min-w-0">
        <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
          <div className="flex h-16 items-center px-4">
            <RouterLink to={app.homePath} className="flex flex-1 items-center gap-1.5 no-underline">
              <img
                src={mode === 'dark' ? '/superhumans-white.svg' : '/superhumans.svg'}
                alt="Superhumans"
                className="h-9 w-auto"
              />
              <div className="hidden sm:block">
                <div className="font-rubik text-lg font-extrabold leading-tight tracking-tight text-foreground">
                  {app.title}
                </div>
                <div className="font-mulish text-[10px] leading-none tracking-wide text-muted-foreground uppercase">
                  {app.subtitle}
                </div>
              </div>
            </RouterLink>

            <ThemeToggle />
            <DropdownMenu>
              <DropdownMenuTrigger
                aria-label="Меню користувача"
                className="inline-flex items-center justify-center rounded-full p-2 text-muted-foreground outline-none hover:text-primary hover:bg-primary/8 focus-visible:ring-2 focus-visible:ring-ring"
              >
                <UserCircle className="size-5" />
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem disabled className="font-rubik">{user?.fullName}</DropdownMenuItem>
                <DropdownMenuItem disabled className="font-rubik text-muted-foreground">{roleLabel(user?.role)}</DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleLogout}>Вийти</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>

        <main className="flex-1">
          <div className="mx-auto w-full max-w-7xl px-4 mt-3 mb-4 fade-in-up">
            <Breadcrumbs />
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
