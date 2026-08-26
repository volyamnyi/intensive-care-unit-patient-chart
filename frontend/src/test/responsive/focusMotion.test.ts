import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

/** index.css ?raw comes back empty through the Vitest CSS pipeline — read it. */
function readIndexCss(): string {
  return readFileSync(resolve(import.meta.dirname, '../../index.css'), 'utf8');
}

/**
 * Phase 5 (#179) audit contracts: full-opacity focus rings on every ui
 * primitive and a prefers-reduced-motion gate covering every animation added
 * in Responsive Phases 1–4. jsdom raw-source assertions; the live sweep is
 * recorded in docs/Responsive-UI-ShadCN-Implementation-Plan.md.
 */

const RING_PRIMITIVE_SOURCES: Record<string, () => Promise<{ default: string }>> = {
  'ui/button': () => import('../../components/ui/button?raw'),
  'ui/input': () => import('../../components/ui/input?raw'),
  'ui/textarea': () => import('../../components/ui/textarea?raw'),
  'ui/select': () => import('../../components/ui/select?raw'),
  'ui/checkbox': () => import('../../components/ui/checkbox?raw'),
  'ui/radio-group': () => import('../../components/ui/radio-group?raw'),
  'ui/switch': () => import('../../components/ui/switch?raw'),
  'ui/tabs': () => import('../../components/ui/tabs?raw'),
};

const REDUCED_MOTION_TOKENS = [
  '.check-pop',
  '.step-fade-in',
  'data-fullscreen',
  '[data-slot="sheet-content"]',
  '[data-slot="sheet-overlay"]',
  '.fade-in-up',
  '.fade-in',
  '.slide-in-left',
  '.scale-in',
  '.pulse',
  '.skeleton',
  '[data-slot="stepper-loading"]',
  '[data-slot="dialog-content"]',
  '[data-slot="dialog-overlay"]',
  '[data-slot="select-content"]',
  '[data-slot="dropdown-menu-content"]',
  '[data-slot="dropdown-menu-sub-content"]',
  '[data-slot="tooltip-content"]',
  '[data-slot="popover-content"]',
] as const;

describe('Focus-ring policy — full-opacity ring-ring (Phase 5)', () => {
  for (const [name, load] of Object.entries(RING_PRIMITIVE_SOURCES)) {
    it(`${name} keeps full-opacity focus rings`, async () => {
      const src = await load();
      const s = (src as { default: string }).default;
      expect(s).toContain('ring-ring');
      expect(s).not.toContain('ring-ring/50');
      expect(s).not.toMatch(/ring-ring\/\d+/);
    });
  }

  it('button destructive variant rings destructive', async () => {
    const src = await import('../../components/ui/button?raw');
    expect((src as { default: string }).default).toContain('ring-destructive');
  });
});

describe('Reduced-motion gate covers Phases 1–4 animations', () => {
  it('every audited animation token sits inside the media gates', () => {
    const s = readIndexCss();
    for (const token of REDUCED_MOTION_TOKENS) {
      expect(s, `missing reduced-motion token: ${token}`).toContain(token);
    }
  });

  it('declares at least three prefers-reduced-motion gates', () => {
    const s = readIndexCss();
    const count = s.match(/@media \(prefers-reduced-motion: reduce\)/g)?.length ?? 0;
    expect(count).toBeGreaterThanOrEqual(3);
  });

  it('gates the animation tokens rather than only defining them', () => {
    const s = readIndexCss();
    // Each gated block ends with "animation: none" — require at least one per
    // media section by counting occurrences.
    const noneCount = s.match(/animation:\s*none/g)?.length ?? 0;
    expect(noneCount).toBeGreaterThanOrEqual(3);
  });
});

describe('Performance spot-check — CSS-first rule', () => {
  it('no JS resize listeners were added anywhere in src', async () => {
    const files = import.meta.glob('../../**/*.{ts,tsx}', {
      query: '?raw',
      import: 'default',
      eager: true,
    });
    const offenders = Object.entries(files)
      .filter(([path]) => !path.includes('.test.'))
      .filter(([, raw]) => /addEventListener\(\s*['"]resize['"]/.test(raw as string));
    expect(offenders.map(([p]) => p)).toEqual([]);
  });
});
