import { NavLink, Outlet, useParams } from 'react-router-dom';
import { LayoutDashboard, History, FileCheck, BarChart3 } from 'lucide-react';

const items = [
  { title: 'Огляд', url: '', icon: LayoutDashboard },
  { title: 'Історія', url: 'history', icon: History },
  { title: 'Документи', url: 'documents', icon: FileCheck },
  { title: 'Статистика', url: 'stats', icon: BarChart3 },
];

export default function ProcessLayout() {
  const { id } = useParams<{ id: string }>();

  return (
    <div className="flex h-screen w-full">
      <nav className="flex w-56 flex-col gap-1 overflow-y-auto border-r bg-muted/30 p-3">
        <div className="mb-4 text-sm text-muted-foreground">
          Процес #{id}
        </div>
        {items.map((item) => (
          <NavLink
            key={item.title}
            to={item.url}
            end
            className={({ isActive }) =>
              `flex items-center gap-2 rounded-md px-3 py-2 text-sm ${
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
      <main className="flex-1 overflow-y-auto p-6">
        <Outlet />
      </main>
    </div>
  );
}
