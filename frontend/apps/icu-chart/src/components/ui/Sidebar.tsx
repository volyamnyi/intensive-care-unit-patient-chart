/* eslint-disable react/only-export-components */
import React, { createContext, useContext, useState, useRef, useEffect, useCallback } from 'react';
import { Box, IconButton, Typography, useMediaQuery, useTheme } from '@mui/material';
import { ChevronLeft, ChevronRight } from '@mui/icons-material';

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
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  return (
    <Box
      component="aside"
      sx={{
        width: isMobile ? '100%' : sidebarWidth,
        flexShrink: 0,
        maxHeight: isMobile ? 'none' : 'calc(100vh - 160px)',
        overflowY: 'auto',
        display: 'flex',
        flexDirection: 'column',
        gap: 1,
        pl: isMobile ? 0 : 2.5,
        position: isMobile ? 'static' : 'sticky',
        top: 0, alignSelf: 'flex-start',
        '&::-webkit-scrollbar': { width: 6 },
        '&::-webkit-scrollbar-thumb': {
          bgcolor: isDark ? '#333' : '#CCC',
          borderRadius: 3,
        },
      }}
    >
      {children}
    </Box>
  );
}

export function SidebarRail() {
  const { startResize, isMobile, toggleSidebar } = useSidebar();
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  if (isMobile) return null;

  return (
    <Box
      onMouseDown={(e) => {
        e.preventDefault();
        startResize(e.clientX);
      }}
      onDoubleClick={toggleSidebar}
      role="separator"
      aria-label="Зміна ширини бічної панелі"
      sx={{
        position: 'absolute', left: 0, top: 0, bottom: 0, width: 20,
        cursor: 'col-resize', zIndex: 5,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        '&::after': {
          content: '""', display: 'block',
          width: 3, height: '100%',
          bgcolor: 'divider', borderRadius: 1.5,
          transition: 'background-color 0.15s',
        },
        '&:hover': { bgcolor: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.04)' },
        '&:hover::after': { bgcolor: 'primary.main' },
        '&:active': { bgcolor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)' },
        '&:active::after': { bgcolor: 'primary.main', width: 4 },
      }}
    />
  );
}

export function SidebarHeader({
  children, sx, ...props
}: { children: React.ReactNode; sx?: object; [k: string]: unknown }) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const bd = `1px solid ${isDark ? '#2A2A2A' : '#D0CEC9'}`;
  return (
    <Box
      sx={{
        p: 1.5, border: bd, borderRadius: 2,
        bgcolor: isDark ? '#1A1A1A' : '#FAFAF8',
        position: 'sticky', top: 0, zIndex: 1,
        ...(sx ?? {}),
      }}
      {...props}
    >
      {children}
    </Box>
  );
}

export function SidebarContent({
  children, sx, ...props
}: { children: React.ReactNode; sx?: object; [k: string]: unknown }) {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, ...(sx ?? {}) }} {...props}>
      {children}
    </Box>
  );
}

export function SidebarGroup({
  label, count, children, sx, ...props
}: { label: string; count?: number; children: React.ReactNode; sx?: object; [k: string]: unknown }) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const bd = `1px solid ${isDark ? '#2A2A2A' : '#D0CEC9'}`;
  return (
    <Box sx={{ p: 1.5, border: bd, borderRadius: 2, ...(sx ?? {}) }} {...props}>
      <Typography sx={{ fontWeight: 700, fontSize: 13, mb: 1, display: 'flex', alignItems: 'center', gap: 0.5 }}>
        {label}
        {count !== undefined && (
          <Box
            component="span"
            sx={{
              fontSize: 10, fontWeight: 400, color: 'text.secondary',
              bgcolor: 'action.hover', px: 0.75, py: 0.125, borderRadius: 1,
            }}
          >
            {count}
          </Box>
        )}
      </Typography>
      {children}
    </Box>
  );
}

export function SidebarTrigger({
  sx, ...props
}: { sx?: object; [k: string]: unknown }) {
  const { open, toggleSidebar } = useSidebar();
  return (
    <IconButton onClick={toggleSidebar} size="small" sx={{ ...(sx ?? {}) }} {...props}>
      {open ? <ChevronRight /> : <ChevronLeft />}
    </IconButton>
  );
}
