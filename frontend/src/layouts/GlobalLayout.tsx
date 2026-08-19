import { Outlet, useNavigate, Link as RouterLink, useLocation } from 'react-router-dom';
import { useEffect, useMemo, useState } from 'react';
import { Menu, UserCircle } from 'lucide-react';
import { useAuth } from '../services/AuthContext';
import { useThemeMode } from '../styles/ThemeContext';
import { useIsMobile } from '../hooks/useMediaQuery';
import ThemeToggle from '../components/common/ThemeToggle';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';

import AppSidebar, { AppNavList } from '../components/navigation/AppSidebar';
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
    if (pathname.startsWith('/prosthetics')) {
      return { title: 'Виробництво', subtitle: 'Виробництво протезів', homePath: '/prosthetics' };
    }
    return { title: 'Superhumans Lviv', subtitle: 'Вибір додатку', homePath: '/select' };
  }, [pathname]);
}

function MobileNavSheet({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const { user } = useAuth();
  return (
    <Sheet open={open} onOpenChange={onOpenChange} swipeDirection="left">
      <SheetContent side="left" className="w-[75vw] max-w-[320px] gap-0 p-3">
        <SheetHeader className="mb-2 pr-8">
          <SheetTitle>Навігація</SheetTitle>
        </SheetHeader>
        <AppNavList onNavigate={() => onOpenChange(false)} />
        {user && (
          <p className="border-t pt-2 text-xs text-muted-foreground truncate">
            {user.fullName} · {roleLabel(user.role)}
          </p>
        )}
      </SheetContent>
    </Sheet>
  );
}

export default function GlobalLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { mode } = useThemeMode();
  const app = useAppInfo();
  const isMobile = useIsMobile();
  const [navSheetOpen, setNavSheetOpen] = useState(false);

  const { pathname } = useLocation();
  useEffect(() => { setNavSheetOpen(false); }, [pathname]);

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <div className="flex min-h-screen bg-background">
      <AppSidebar />

      <div className="flex flex-col flex-1 min-w-0">
        <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
          <div className="flex h-16 items-center gap-2 px-[max(1rem,env(safe-area-inset-left))] sm:px-6">
            {isMobile && (
              <Button
                variant="ghost"
                className="size-11 shrink-0"
                aria-label="Відкрити навігацію"
                onClick={() => setNavSheetOpen(true)}
              >
                <Menu className="size-5" />
              </Button>
            )}
            <RouterLink to={app.homePath} className="flex min-w-0 flex-1 items-center gap-1.5 no-underline">
              <img
                src={mode === 'dark' ? '/superhumans-white.svg' : '/superhumans.svg'}
                alt="Superhumans"
                className="h-9 w-auto shrink-0"
              />
              <div className="min-w-0 flex-1">
                <div className="font-rubik text-lg font-extrabold leading-tight tracking-tight text-foreground truncate">
                  {app.title}
                </div>
                <div className="font-mulish text-[10px] leading-none tracking-wide text-muted-foreground uppercase truncate">
                  {app.subtitle}
                </div>
              </div>
            </RouterLink>

            <ThemeToggle />
            <DropdownMenu>
              <DropdownMenuTrigger
                aria-label="Меню користувача"
                className="inline-flex size-11 shrink-0 items-center justify-center rounded-full text-muted-foreground outline-none hover:text-primary hover:bg-primary/8 focus-visible:ring-2 focus-visible:ring-ring sm:size-auto sm:p-2"
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

        {isMobile && <MobileNavSheet open={navSheetOpen} onOpenChange={setNavSheetOpen} />}

        <main className="flex-1">
          <div className="mx-auto w-full max-w-7xl px-4 mt-3 mb-4 fade-in-up sm:px-6">
            <Breadcrumbs />
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
