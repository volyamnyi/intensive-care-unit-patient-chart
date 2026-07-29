import { useState, useMemo } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../services/AuthContext';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import {
  ChevronLeft, ChevronRight,
  Hospital, FileText, AppWindow,
} from 'lucide-react';

interface NavItem {
  label: string;
  to: string;
  icon: React.ReactNode;
}

export default function AppSidebar() {
  const { user } = useAuth();
  const [collapsed, setCollapsed] = useState(() => {
    if (typeof window === 'undefined') return false;
    const cached = localStorage.getItem('app-sidebar-collapsed');
    return cached === 'true';
  });

  const navItems = useMemo<NavItem[]>(() => {
    const prefix = user?.role === 'NURSE' ? '/prescriptions/icu/nurse' : '/prescriptions/icu/doctor';
    const rxPrefix = user?.role === 'NURSE' ? '/prescriptions/nurse' : '/prescriptions/doctor';
    return [
      { label: 'Карта інтенсивної терапії', to: prefix, icon: <Hospital className="size-5 text-info" /> },
      { label: 'Листок лікарських призначень', to: rxPrefix, icon: <FileText className="size-5 text-success" /> },
      { label: 'Модулі', to: '/select', icon: <AppWindow className="size-4" /> },
    ];
  }, [user?.role]);

  const toggle = () => {
    const next = !collapsed;
    setCollapsed(next);
    try { localStorage.setItem('app-sidebar-collapsed', String(next)); } catch {}
  };

  return (
    <aside
      className={cn(
        'flex flex-col border-r bg-background transition-all duration-200',
        collapsed ? 'w-[60px]' : 'w-[220px]',
        'hidden md:flex',
      )}
    >
      <div className="flex items-center justify-between p-2 border-b">
        {!collapsed && (
          <span className="font-rubik text-sm font-bold px-2">Навігація</span>
        )}
        <Button
          variant="ghost"
          size="icon-xs"
          onClick={toggle}
          className="ml-auto"
          aria-label={collapsed ? 'Розгорнути меню' : 'Згорнути меню'}
        >
          {collapsed ? <ChevronRight className="size-4" /> : <ChevronLeft className="size-4" />}
        </Button>
      </div>

      <nav className="flex flex-col gap-0.5 p-2 flex-1" aria-label="Головна навігація">
        {navItems.map(item => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/select'}
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
