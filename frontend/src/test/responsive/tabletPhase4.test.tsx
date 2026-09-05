import { describe, it, expect } from 'vitest';

/**
 * Phase 4 contract tests: prosthetics & admin tablet pass (issue #178).
 * jsdom class-string assertions — real breakpoint behavior is covered by
 * `tests/specs/responsive/tablet-admin.spec.ts` and
 * `tests/specs/responsive/tablet-prosthetics.spec.ts`.
 */
describe('Tablet prosthetics & admin — Phase 4 contract', () => {
  it('ProcessLayout collapses to an icon rail in the tablet band', async () => {
    const mod = await import('@/pages/prosthetics/process/ProcessLayout');
    expect(mod.default).toBeDefined();
    const src = await import('@/pages/prosthetics/process/ProcessLayout?raw');
    const s = (src as { default: string }).default;
    expect(s).toContain('w-14 shrink-0');
    expect(s).toContain('lg:w-56 lg:p-3');
    expect(s).toContain('hidden lg:inline');
    expect(s).toContain('lg:block');
  });

  it('SetupSteps hides inactive step labels below md', async () => {
    const src = await import('../../components/prosthetics/SetupSteps?raw');
    expect((src as { default: string }).default).toContain('hidden md:inline');
  });

  it('WizardScreen keeps wrapping toolbars and scrollable chip strips', async () => {
    const src = await import('@/pages/prosthetics/process/WizardScreen?raw');
    const s = (src as { default: string }).default;
    expect(s).toContain('flex-wrap');
    expect(s).toContain('overflow-x-auto');
  });

  it('AdminPage keeps sticky matrix column, column floor, and stat tiers', async () => {
    const src = await import('../../pages/admin/AdminPage?raw');
    const s = (src as { default: string }).default;
    expect(s).toContain('sticky left-0 z-10 min-w-[260px] bg-card');
    expect(s).toContain('min-w-[72px] text-center');
    expect(s).toContain('grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-4');
    expect(s).toContain('overflow-x-auto touch-pan-x');
  });
});
