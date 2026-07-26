import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { ThemeModeProvider, useThemeMode } from '../../styles/ThemeContext';

describe('ThemeModeProvider', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('defaults to dark mode when no localStorage value', () => {
    const { result } = renderHook(() => useThemeMode(), {
      wrapper: ThemeModeProvider,
    });
    expect(result.current.mode).toBe('dark');
  });

  it('reads light mode from localStorage', () => {
    localStorage.setItem('themeMode', 'light');
    const { result } = renderHook(() => useThemeMode(), {
      wrapper: ThemeModeProvider,
    });
    expect(result.current.mode).toBe('light');
  });

  it('reads dark mode from localStorage', () => {
    localStorage.setItem('themeMode', 'dark');
    const { result } = renderHook(() => useThemeMode(), {
      wrapper: ThemeModeProvider,
    });
    expect(result.current.mode).toBe('dark');
  });

  it('ignores invalid localStorage values and defaults to dark', () => {
    localStorage.setItem('themeMode', 'invalid');
    const { result } = renderHook(() => useThemeMode(), {
      wrapper: ThemeModeProvider,
    });
    expect(result.current.mode).toBe('dark');
  });

  it('toggles from dark to light', () => {
    localStorage.setItem('themeMode', 'dark');
    const { result } = renderHook(() => useThemeMode(), {
      wrapper: ThemeModeProvider,
    });
    act(() => {
      result.current.toggleTheme();
    });
    expect(result.current.mode).toBe('light');
    expect(localStorage.getItem('themeMode')).toBe('light');
  });

  it('toggles from light to dark', () => {
    localStorage.setItem('themeMode', 'light');
    const { result } = renderHook(() => useThemeMode(), {
      wrapper: ThemeModeProvider,
    });
    act(() => {
      result.current.toggleTheme();
    });
    expect(result.current.mode).toBe('dark');
    expect(localStorage.getItem('themeMode')).toBe('dark');
  });

  it('provides a theme object', () => {
    const { result } = renderHook(() => useThemeMode(), {
      wrapper: ThemeModeProvider,
    });
    expect(result.current.theme).toBeDefined();
    expect(result.current.theme.palette).toBeDefined();
  });

  it('dark mode theme has dark primary background', () => {
    const { result } = renderHook(() => useThemeMode(), {
      wrapper: ThemeModeProvider,
    });
    expect(result.current.theme.palette.background.default).toBe('#0D0D0D');
  });

  it('light mode theme has light primary background', () => {
    localStorage.setItem('themeMode', 'light');
    const { result } = renderHook(() => useThemeMode(), {
      wrapper: ThemeModeProvider,
    });
    expect(result.current.theme.palette.background.default).toBe('#FAFAF8');
  });

  it('persists theme mode change to localStorage', () => {
    localStorage.setItem('themeMode', 'dark');
    const { result } = renderHook(() => useThemeMode(), {
      wrapper: ThemeModeProvider,
    });
    act(() => {
      result.current.toggleTheme();
    });
    expect(localStorage.getItem('themeMode')).toBe('light');
  });
});

describe('useThemeMode outside provider', () => {
  it('throws error when used outside ThemeModeProvider', () => {
    expect(() => {
      renderHook(() => useThemeMode());
    }).toThrow('useThemeMode must be used within ThemeModeProvider');
  });
});
