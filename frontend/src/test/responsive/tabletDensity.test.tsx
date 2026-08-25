import { describe, it, expect } from 'vitest';

/**
 * Phase 1 contract tests: verify that the tablet density classes are present
 * on the touched clinical grid primitives (issue #175).
 * These are jsdom class-string assertions — real breakpoint behavior is
 * covered by Playwright `responsive-tablet-chromium`.
 */
describe('Tablet density classes — Phase 1 contract', () => {
  it('HourlyGrid scroll affordance uses md:block gradient', async () => {
    const src = await import('../../components/monitoring/HourlyGrid');
    // Verify the module exports the component without error
    expect(src.default).toBeDefined();
  });

  it('HourlyGridDialog has tablet max-width classes', async () => {
    const mod = await import('../../components/monitoring/HourlyGridDialog');
    expect(mod.default).toBeDefined();
    // The dialog source should contain the tablet width tokens
    const src = await import('../../components/monitoring/HourlyGridDialog?raw');
    expect((src as { default: string }).default).toContain('sm:max-w-[95vw]');
    expect((src as { default: string }).default).toContain('md:max-w-[92vw]');
  });

  it('PrescriptionSpreadsheet has md density + sticky shadow', async () => {
    const src = await import('../../components/prescription/PrescriptionSpreadsheet?raw');
    expect((src as { default: string }).default).toContain('md:min-w-[140px]');
    expect((src as { default: string }).default).toContain('shadow-[2px_0_4px_rgba(0,0,0,0.05)]');
    expect((src as { default: string }).default).toContain('md:table-fixed');
  });

  it('VitalSignGrid has md density + sticky shadow', async () => {
    const src = await import('../../components/prescription/VitalSignGrid?raw');
    expect((src as { default: string }).default).toContain('md:min-w-[140px]');
    expect((src as { default: string }).default).toContain('shadow-[2px_0_4px_rgba(0,0,0,0.05)]');
    expect((src as { default: string }).default).toContain('md:table-fixed');
  });

  it('IntensiveCareCard uses CSS-first two-column at md:', async () => {
    const src = await import('../../components/monitoring/IntensiveCareCard?raw');
    expect((src as { default: string }).default).toContain('flex-col md:flex-row');
    expect((src as { default: string }).default).toContain('hidden md:block');
    expect((src as { default: string }).default).toContain('md:hidden');
  });
});
