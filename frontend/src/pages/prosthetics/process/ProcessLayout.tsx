import { NavLink, Outlet, useParams } from 'react-router-dom';
import { LayoutDashboard, History } from 'lucide-react';
import { useIsMobile } from '../../../hooks/useMediaQuery';
import { cn } from '@/lib/utils';

const items = [
  { title: 'Огляд', url: '', icon: LayoutDashboard },
  { title: 'Історія', url: 'history', icon: History },
];

export default function ProcessLayout() {
  const { id } = useParams<{ id: string }>();
  const isMobile = useIsMobile();

  if (isMobile) {
    return (
      <div className="flex h-screen w-full flex-col">
        <nav
          className="flex shrink-0 items-center gap-1 overflow-x-auto border-b bg-muted/30 px-2 py-1 touch-pan-x"
          aria-label="Навігація процесу"
        >
          {items.map((item) => (
            <NavLink
              key={item.title}
              to={item.url}
              end
              className={({ isActive }) =>
                cn(
                  'flex min-h-11 shrink-0 items-center gap-2 rounded-md px-3 text-sm',
                  isActive
                    ? 'bg-mint/10 text-mint font-medium'
                    : 'text-muted-foreground hover:bg-accent hover:text-foreground',
                )
              }
            >
              <item.icon className="size-4" />
              <span>{item.title}</span>
            </NavLink>
          ))}
        </nav>
        <main className="flex-1 overflow-y-auto p-4">
          <p className="mb-2 text-xs text-muted-foreground">Процес #{id}</p>
          <Outlet />
        </main>
      </div>
    );
  }

  return (
    <div className="flex h-screen w-full">
      <main className="flex-1 overflow-y-auto p-4 sm:p-6">
        <Outlet />
      </main>
      <nav className="flex w-56 flex-col gap-1 overflow-y-auto border-l bg-muted/30 p-3">
        <div className="mb-4 text-sm text-muted-foreground">
          Процес #{id}
        </div>
        {items.map((item) => (
          <NavLink
            key={item.title}
            to={item.url}
            end
            className={({ isActive }) =>
              `flex min-h-11 sm:min-h-0 items-center gap-2 rounded-md px-3 py-2 text-sm ${
                isActive
                  ? 'bg-mint/10 text-mint font-medium'
                  : 'text-muted-foreground hover:bg-accent hover:text-foreground'
              }`
            }
          >
            <item.icon className="size-4" />
            <span>{item.title}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
