import { describe, it, expect, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useMediaQuery, useIsMobile } from './useMediaQuery';

interface MockMediaQueryList {
  matches: boolean;
  media: string;
  onchange: null;
  addListener: () => void;
  removeListener: () => void;
  addEventListener: (_type: string, cb: (e: MediaQueryListEvent) => void) => void;
  removeEventListener: (_type: string, cb: (e: MediaQueryListEvent) => void) => void;
  dispatchEvent: () => false;
}

function mockMatchMedia(matches: boolean) {
  const listeners = new Set<(e: MediaQueryListEvent) => void>();
  const mq: MockMediaQueryList = {
    matches,
    media: '',
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: (_type, cb) => {
      listeners.add(cb);
    },
    removeEventListener: (_type, cb) => {
      listeners.delete(cb);
    },
    dispatchEvent: () => false,
  };
  return {
    mq,
    emit: (next: boolean) => {
      mq.matches = next;
      listeners.forEach((cb) => cb({ matches: next } as MediaQueryListEvent));
    },
    install: () => {
      const original = window.matchMedia;
      window.matchMedia = vi.fn(() => mq) as unknown as typeof window.matchMedia;
      return () => {
        window.matchMedia = original;
      };
    },
  };
}

describe('useMediaQuery', () => {
  it('returns true when the query matches', () => {
    const { install } = mockMatchMedia(true);
    const restore = install();
    const { result } = renderHook(() => useMediaQuery('(max-width: 639.98px)'));
    expect(result.current).toBe(true);
    restore();
  });

  it('returns false when the query does not match', () => {
    const { install } = mockMatchMedia(false);
    const restore = install();
    const { result } = renderHook(() => useMediaQuery('(max-width: 639.98px)'));
    expect(result.current).toBe(false);
    restore();
  });

  it('re-renders when the media query state changes', async () => {
    const { emit, install } = mockMatchMedia(false);
    const restore = install();
    const { result } = renderHook(() => useMediaQuery('(max-width: 639.98px)'));
    expect(result.current).toBe(false);
    emit(true);
    await waitFor(() => expect(result.current).toBe(true));
    emit(false);
    await waitFor(() => expect(result.current).toBe(false));
    restore();
  });
});

describe('useIsMobile', () => {
  it('is true below the mobile breakpoint', () => {
    const { install } = mockMatchMedia(true);
    const restore = install();
    const { result } = renderHook(() => useIsMobile());
    expect(result.current).toBe(true);
    restore();
  });

  it('is false at or above the mobile breakpoint', () => {
    const { install } = mockMatchMedia(false);
    const restore = install();
    const { result } = renderHook(() => useIsMobile());
    expect(result.current).toBe(false);
    restore();
  });

  it('subscribes to the under-640px query', () => {
    const { install } = mockMatchMedia(false);
    const restore = install();
    renderHook(() => useIsMobile());
    expect(window.matchMedia).toHaveBeenCalledWith('(max-width: 639.98px)');
    restore();
  });
});