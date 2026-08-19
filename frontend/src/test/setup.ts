import '@testing-library/jest-dom';
import { afterEach } from 'vitest';

// jsdom does not implement matchMedia — needed by Sidebar / useMediaQuery hooks.
// Tests opt in to a matching query via (globalThis as any).setMatchMediaQuery(query).
let matchMediaTarget: string | null = null;

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    get matches() {
      return matchMediaTarget === query;
    },
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
});

// Test helper: make exactly one media query report matches === true.
// Pass null to restore the default (no query matches).
(globalThis as any).setMatchMediaQuery = (query: string | null) => {
  matchMediaTarget = query;
};

afterEach(() => {
  matchMediaTarget = null;
});
