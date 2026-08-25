import { describe, it, expect } from 'vitest';

/**
 * Phase 2 contract tests: verify that ui/popover.tsx exists and exports
 * the expected components, and that consumer migrations reference the
 * primitive instead of hand-rolled markup (issue #176).
 */
describe('Popover primitive — Phase 2 contract', () => {
  it('ui/popover.tsx exists and exports all parts', async () => {
    const mod = await import('../../components/ui/popover');
    expect(mod.Popover).toBeDefined();
    expect(mod.PopoverTrigger).toBeDefined();
    expect(mod.PopoverPortal).toBeDefined();
    expect(mod.PopoverPositioner).toBeDefined();
    expect(mod.PopoverContent).toBeDefined();
    expect(mod.PopoverHeader).toBeDefined();
    expect(mod.PopoverTitle).toBeDefined();
    expect(mod.PopoverDescription).toBeDefined();
  });

  it('DeleteConfirmPopover uses Base UI Popover primitive', async () => {
    const src = await import('../../components/prescription/DeleteConfirmPopover?raw');
    expect((src as { default: string }).default).toContain('@base-ui/react/popover');
    expect((src as { default: string }).default).toContain('data-slot="popover-content"');
    // Hand-rolled fixed positioning must be gone
    expect((src as { default: string }).default).not.toContain('getBoundingClientRect');
    expect((src as { default: string }).default).not.toContain('position:fixed');
  });

  it('ExecuteDosePopover uses Base UI Popover primitive for dose entry', async () => {
    const src = await import('../../components/prescription/ExecuteDosePopover?raw');
    expect((src as { default: string }).default).toContain('@base-ui/react/popover');
    expect((src as { default: string }).default).toContain('data-slot="popover-content"');
    // Hand-rolled fixed positioning must be gone from the dose-entry part
    expect((src as { default: string }).default).not.toContain('getBoundingClientRect');
  });
});
