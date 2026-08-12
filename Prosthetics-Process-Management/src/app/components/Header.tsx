import { Bell, User, LogOut, ChevronRight } from "lucide-react";

interface Props {
  user: string;
  onLogout: () => void;
  breadcrumb?: string[];
}

export function Header({ user, onLogout, breadcrumb }: Props) {
  return (
    <header className="bg-gray-900 text-white px-6 py-3 flex items-center justify-between shrink-0">
      <div className="flex items-center gap-4">
        <div className="w-8 h-8 bg-gray-600 rounded flex items-center justify-center text-xs font-bold">П</div>
        <span className="text-sm text-gray-300">Prosthetics Process Management</span>
        {breadcrumb && breadcrumb.length > 0 && (
          <div className="flex items-center gap-1 text-gray-400 text-xs ml-2">
            {breadcrumb.map((crumb, i) => (
              <span key={i} className="flex items-center gap-1">
                {i > 0 && <ChevronRight size={12} />}
                <span className={i === breadcrumb.length - 1 ? "text-gray-200" : ""}>{crumb}</span>
              </span>
            ))}
          </div>
        )}
      </div>
      <div className="flex items-center gap-4">
        <button className="relative text-gray-300 hover:text-white">
          <Bell size={18} />
          <span className="absolute -top-1 -right-1 bg-gray-500 text-white rounded-full w-4 h-4 text-xs flex items-center justify-center">3</span>
        </button>
        <div className="flex items-center gap-2 text-sm text-gray-300">
          <User size={16} />
          <span>{user}</span>
        </div>
        <button onClick={onLogout} className="flex items-center gap-1 text-gray-400 hover:text-white text-sm">
          <LogOut size={16} />
          <span>Вийти</span>
        </button>
      </div>
    </header>
  );
}
