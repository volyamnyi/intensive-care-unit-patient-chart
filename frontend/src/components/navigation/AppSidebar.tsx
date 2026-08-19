import { useState, useMemo } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import { useIsMobile } from '../../hooks/useMediaQuery';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import {
  ChevronLeft, ChevronRight,
  Hospital, FileText, AppWindow, Wrench,
} from 'lucide-react';

export interface NavItem {
  label: string;
  to: string;
  icon: React.ReactNode;
}

/** Media query for the tablet band (640–1024px) where the rail should default to icons. */
export const TABLET_RAIL_QUERY = '(min-width:640px) and (max-width:1023.98px)';

/**
 * Permission-aware navigation items, shared between the desktop
 * {@link AppSidebar}, the mobile Sheet and any other surface that needs to
 * render the same links without duplicating the permission logic.
 */
export function useNavItems() {
  const { user, hasPermission } = useAuth();
  return useMemo<NavItem[]>(() => {
    const prefix = user?.role === 'NURSE' ? '/icu/nurse' : '/icu/doctor';
    const rxPrefix = user?.role === 'NURSE' ? '/prescriptions/nurse' : '/prescriptions/doctor';
    return [
      ...(hasPermission('MODULE_ICU_ACCESS')
        ? [{ label: 'Карта інтенсивної терапії', to: prefix, icon: <Hospital className="size-5 text-info" /> }]
        : []),
      ...(hasPermission('MODULE_MEDICATION_ACCESS')
        ? [{ label: 'Листок лікарських призначень', to: rxPrefix, icon: <FileText className="size-5 text-success" /> }]
        : []),
      ...(hasPermission('MODULE_PROSTHETICS_ACCESS')
        ? [{ label: 'Виробництво протезів', to: '/prosthetics', icon: <Wrench className="size-5 text-mint" /> }]
        : []),
      { label: 'Модулі', to: '/select', icon: <AppWindow className="size-4" /> },
    ];
  }, [user?.role, hasPermission]);
}

interface AppNavListProps {
  collapsed?: boolean;
  onNavigate?: () => void;
}

/**
 * The actual list of NavLink items.
 * Renders standalone — used inside the desktop rail, the mobile Sheet, and tests.
 */
export function AppNavList({ collapsed = false, onNavigate }: AppNavListProps) {
  const navItems = useNavItems();
  return (
    <nav className="flex flex-col gap-0.5 p-2 flex-1" aria-label="Головна навігація">
      {navItems.map(item => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === '/select'}
          onClick={onNavigate}
          className={({ isActive }) => cn(
            'flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm transition-colors',
            'hover:bg-accent hover:text-accent-foreground',
            isActive ? 'bg-accent text-accent-foreground font-medium' : 'text-muted-foreground',
            collapsed && 'justify-center px-1',
          )}
          title={collapsed ? item.label : undefined}
        >
          {item.icon}
          {!collapsed && <span>{item.label}</span>}
        </NavLink>
      ))}
    </nav>
  );
}

export default function AppSidebar() {
  const { user } = useAuth();
  const isMobile = useIsMobile();

  // Default collapsed state: icons-only on tablet, expanded on desktop (restored from localStorage).
  const [collapsed, setCollapsed] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false;
    try {
      if (window.matchMedia(TABLET_RAIL_QUERY).matches) return true;
      return localStorage.getItem('app-sidebar-collapsed') === 'true';
    } catch {
      return false;
    }
  });

  const toggle = () => {
    const next = !collapsed;
    setCollapsed(next);
    try { localStorage.setItem('app-sidebar-collapsed', String(next)); } catch {}
  };

  // On the mobile band the entire rail is unmounted — navigation is provided
  // by the hamburger + Sheet in GlobalLayout.
  if (isMobile) return null;

  return (
    <aside
      className={cn(
        'flex flex-col border-r bg-background transition-all duration-200 shrink-0',
        collapsed ? 'w-[60px]' : 'w-[220px]',
      )}
      aria-label="Бічна панель"
    >
      <div className="flex items-center justify-between p-2 border-b">
        {!collapsed && (
          <span className="font-rubik text-sm font-bold px-2">Навігація</span>
        )}
        <Button
          variant="ghost"
          size="icon"
          className="ml-auto shrink-0"
          onClick={toggle}
          aria-label={collapsed ? 'Розгорнути меню' : 'Згорнути меню'}
        >
          {collapsed ? <ChevronRight className="size-4" /> : <ChevronLeft className="size-4" />}
        </Button>
      </div>

      <AppNavList collapsed={collapsed} />

      <div className="p-2 border-t text-center">
        {!collapsed && user && (
          <p className="text-[10px] text-muted-foreground truncate">
            {user.fullName}
          </p>
        )}
      </div>
    </aside>
  );
}
