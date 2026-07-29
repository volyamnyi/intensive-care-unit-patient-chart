/* eslint-disable react/only-export-components */
import React, { createContext, useContext, useState, useRef, useEffect, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { useThemeMode } from '../../styles/ThemeContext';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface SidebarContextValue {
  open: boolean;
  setOpen: (open: boolean) => void;
  toggleSidebar: () => void;
  sidebarWidth: number;
  setSidebarWidth: (width: number | ((prev: number) => number)) => void;
  minWidth: number;
  maxWidth: number;
  isMobile: boolean;
  startResize: (clientX: number) => void;
}

const SidebarContext = createContext<SidebarContextValue | null>(null);

export interface SidebarProviderProps {
  children: React.ReactNode;
  defaultOpen?: boolean;
  defaultWidth?: number;
  minWidth?: number;
  maxWidth?: number;
  persistKey?: string;
}

export interface SidebarProps {
  children: React.ReactNode;
  side?: 'left' | 'right';
  collapsible?: 'none' | 'offcanvas' | 'icon';
}

function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia(query).matches;
  });

  useEffect(() => {
    const mq = window.matchMedia(query);
    const handler = (e: MediaQueryListEvent) => setMatches(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, [query]);

  return matches;
}

export function SidebarProvider({
  children,
  defaultOpen = true,
  defaultWidth = 300,
  minWidth = 200,
  maxWidth = 600,
  persistKey = 'icu-sidebar',
}: SidebarProviderProps) {
  const [open, setOpenState] = useState(() => {
    if (typeof window === 'undefined') return defaultOpen;
    const cached = localStorage.getItem(`${persistKey}-open`);
    return cached !== null ? cached === 'true' : defaultOpen;
  });
  const [sidebarWidth, setSidebarWidthState] = useState(() => {
    if (typeof window === 'undefined') return defaultWidth;
    const cached = localStorage.getItem(`${persistKey}-width`);
    return cached !== null ? Math.min(maxWidth, Math.max(minWidth, Number(cached))) : defaultWidth;
  });
  const isMobile = useMediaQuery('(max-width:1200px)');
  const dragState = useRef<{ startX: number; startWidth: number } | null>(null);
  const sidebarWidthRef = useRef(sidebarWidth);
  sidebarWidthRef.current = sidebarWidth;

  const setOpen = useCallback((val: boolean) => {
    setOpenState(val);
    try { localStorage.setItem(`${persistKey}-open`, String(val)); } catch {}
  }, [persistKey]);

  const setSidebarWidth = useCallback((w: number | ((prev: number) => number)) => {
    setSidebarWidthState((prev) => {
      const next = typeof w === 'function' ? w(prev) : w;
      try { localStorage.setItem(`${persistKey}-width`, String(next)); } catch {}
      return next;
    });
  }, [persistKey]);

  const toggleSidebar = useCallback(() => setOpen(!open), [open, setOpen]);

  const startResize = useCallback((clientX: number) => {
    dragState.current = { startX: clientX, startWidth: sidebarWidthRef.current };
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  }, []);

  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      if (!dragState.current) return;
      const { startX, startWidth } = dragState.current;
      const delta = e.clientX - startX;
      const next = Math.min(maxWidth, Math.max(minWidth, startWidth - delta));
      setSidebarWidth(next);
    };
    const onUp = () => {
      if (!dragState.current) return;
      dragState.current = null;
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
  }, [minWidth, maxWidth, setSidebarWidth]);

  return (
    <SidebarContext.Provider
      value={{
        open, setOpen, toggleSidebar,
        sidebarWidth, setSidebarWidth,
        minWidth, maxWidth, isMobile,
        startResize,
      }}
    >
      {children}
    </SidebarContext.Provider>
  );
}

export function useSidebar() {
  const ctx = useContext(SidebarContext);
  if (!ctx) throw new Error('useSidebar must be used within SidebarProvider');
  return ctx;
}

export function Sidebar({ children }: SidebarProps) {
  const { sidebarWidth, isMobile } = useSidebar();

  return (
    <aside
      style={{
        width: isMobile ? '100%' : sidebarWidth,
        maxHeight: isMobile ? 'none' : 'calc(100vh - 160px)',
        paddingLeft: isMobile ? 0 : undefined,
      }}
      className={cn(
        'flex shrink-0 flex-col gap-1 overflow-y-auto',
        isMobile ? 'static w-full' : 'sticky top-0 self-start',
        '[&::-webkit-scrollbar]:w-[6px]',
        '[&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-muted-foreground/30',
      )}
    >
      {children}
    </aside>
  );
}

export function SidebarRail() {
  const { startResize, isMobile, toggleSidebar } = useSidebar();

  if (isMobile) return null;

  return (
    <div
      onMouseDown={(e) => {
        e.preventDefault();
        startResize(e.clientX);
      }}
      onDoubleClick={toggleSidebar}
      role="separator"
      aria-label="Зміна ширини бічної панелі"
      className={cn(
        'absolute left-0 top-0 bottom-0 z-[5] flex w-5 cursor-col-resize items-center justify-center',
        'hover:bg-black/5 dark:hover:bg-white/5',
        'active:bg-black/8 dark:active:bg-white/8',
        'after:block after:h-full after:w-[3px] after:rounded-full after:bg-border after:transition-colors after:duration-150',
        'hover:after:bg-primary active:after:bg-primary active:after:w-[4px]',
      )}
    />
  );
}

export function SidebarHeader({
  children, className, ...props
}: { children: React.ReactNode; className?: string; [k: string]: unknown }) {
  const { mode } = useThemeMode();
  const isDark = mode === 'dark';

  return (
    <div
      className={cn(
        'sticky top-0 z-[1] rounded-xl border p-3',
        isDark ? 'border-[#2A2A2A] bg-[#1A1A1A]' : 'border-[#D0CEC9] bg-[#FAFAF8]',
        className,
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export function SidebarContent({
  children, className, ...props
}: { children: React.ReactNode; className?: string; [k: string]: unknown }) {
  return (
    <div className={cn('flex flex-col gap-1', className)} {...props}>
      {children}
    </div>
  );
}

export function SidebarGroup({
  label, count, children, className, ...props
}: { label: string; count?: number; children: React.ReactNode; className?: string; [k: string]: unknown }) {
  const { mode } = useThemeMode();
  const isDark = mode === 'dark';

  return (
    <div className={cn('rounded-xl border p-3', isDark ? 'border-[#2A2A2A]' : 'border-[#D0CEC9]', className)} {...props}>
      <p className={cn('mb-1 flex items-center gap-0.5 text-[13px] font-bold font-rubik', className)}>
        {label}
        {count !== undefined && (
          <span className="rounded-md bg-muted px-[3px] py-[1px] text-[10px] font-normal text-muted-foreground">
            {count}
          </span>
        )}
      </p>
      {children}
    </div>
  );
}

export function SidebarTrigger({
  className, ...props
}: { className?: string; [k: string]: unknown }) {
  const { open, toggleSidebar } = useSidebar();
  return (
    <Button variant="ghost" size="icon-sm" onClick={toggleSidebar} className={className} {...props}>
      {open ? <ChevronRight className="size-4" /> : <ChevronLeft className="size-4" />}
    </Button>
  );
}
