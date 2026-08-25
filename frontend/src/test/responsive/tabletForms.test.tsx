import { describe, it, expect } from 'vitest';

/**
 * Phase 3 contract tests: verify that the adaptive form/dialog layout classes
 * are present on the touched ICU surfaces and the shared dialog primitive
 * (issue #177). These are jsdom class-string assertions — real breakpoint
 * behavior is covered by Playwright `responsive-tablet-chromium`
 * (`tests/specs/responsive/tablet-forms.spec.ts`).
 */
describe('Tablet forms & dialogs classes — Phase 3 contract', () => {
  it('SofaForm gains sm/md/lg column tiers + touch sizing', async () => {
    const mod = await import('../../components/icu/scales/SofaForm');
    expect(mod.default).toBeDefined();
    const src = await import('../../components/icu/scales/SofaForm?raw');
    expect((src as { default: string }).default).toContain(
      'grid grid-cols-1 gap-1.5 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4',
    );
    expect((src as { default: string }).default).toContain('pointer-coarse:min-h-11');
  });

  it('ApacheIiForm keeps its 4-tier column ladder', async () => {
    const src = await import('../../components/icu/scales/ApacheIiForm?raw');
    expect((src as { default: string }).default).toContain(
      'grid grid-cols-1 gap-1.5 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4',
    );
    expect((src as { default: string }).default).toContain('pointer-coarse:min-h-11');
  });

  it('BradenForm keeps its 3-tier column ladder', async () => {
    const src = await import('../../components/icu/scales/BradenForm?raw');
    expect((src as { default: string }).default).toContain(
      'grid grid-cols-1 gap-1.5 sm:grid-cols-2 md:grid-cols-3',
    );
  });

  it('VitalSignsForm has explicit md tier + taller tablet textarea', async () => {
    const src = await import('../../components/icu/VitalSignsForm?raw');
    expect((src as { default: string }).default).toContain(
      'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-1.5',
    );
    expect((src as { default: string }).default).toContain('min-h-[60px] md:min-h-[72px]');
  });

  it('PatientStatePanel fields pair up at md + touch sizing', async () => {
    const src = await import('../../components/icu/PatientStatePanel?raw');
    expect((src as { default: string }).default).toContain(
      'grid grid-cols-1 gap-1.5 md:grid-cols-2',
    );
    expect((src as { default: string }).default).toContain('pointer-coarse:min-h-11');
    expect((src as { default: string }).default).toContain('min-h-[2.5rem] md:min-h-[72px]');
  });

  it('MedicalNotesPanel stacks below lg and pairs editor/list at lg+', async () => {
    const src = await import('../../components/icu/MedicalNotesPanel?raw');
    expect((src as { default: string }).default).toContain(
      'grid grid-cols-1 gap-2 lg:grid-cols-2',
    );
    expect((src as { default: string }).default).toContain('flex flex-col');
  });

  it('DialogContent consolidates the tablet max-width policy', async () => {
    const src = await import('../../components/ui/dialog?raw');
    expect((src as { default: string }).default).toContain('sm:max-w-sm md:max-w-md');
    expect((src as { default: string }).default).toContain('data-fullscreen={mobileFullscreen ? "mobile" : undefined}');
  });

  it('right-side patient sidebar sheet stays capped at 768 (sm:max-w-sm)', async () => {
    const src = await import('../../components/monitoring/IntensiveCareCard?raw');
    expect((src as { default: string }).default).toContain('w-full p-0 sm:max-w-sm');
  });

  it('narrow dose popover consumers keep their intentional max-w-xs cap', async () => {
    const dose = await import('../../components/prescription/ExecuteDosePopover?raw');
    expect((dose as { default: string }).default).toContain('max-w-xs');
    const exec = await import('../../components/prescription/PrescriptionExecutionPanel?raw');
    expect((exec as { default: string }).default).toContain('max-w-xs');
  });
});
