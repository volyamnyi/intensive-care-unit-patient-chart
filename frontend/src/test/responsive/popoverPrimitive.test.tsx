import { describe, it, expect } from 'vitest';

/**
 * Phase 2 contract tests: verify that ui/popover.tsx exists and exports
 * the expected components, and that consumer migrations route through the
 * ui primitive layer instead of hand-rolled markup or direct Base UI
 * imports (issue #176).
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

  it('DeleteConfirmPopover uses the ui/popover layer', async () => {
    const src = await import('../../components/prescription/DeleteConfirmPopover?raw');
    const code = (src as { default: string }).default;
    expect(code).toContain("from '@/components/ui/popover'");
    // Consumers must not import the Base UI primitive directly (boundary:
    // only components/ui may import @base-ui/react)
    expect(code).not.toContain('@base-ui/react');
    // Hand-rolled fixed positioning must be gone
    expect(code).not.toContain('getBoundingClientRect');
    expect(code).not.toContain('position:fixed');
  });

  it('ExecuteDosePopover uses the ui/popover layer for dose entry', async () => {
    const src = await import('../../components/prescription/ExecuteDosePopover?raw');
    const code = (src as { default: string }).default;
    expect(code).toContain("from '@/components/ui/popover'");
    expect(code).not.toContain('@base-ui/react');
    // Hand-rolled fixed positioning must be gone from the dose-entry part
    expect(code).not.toContain('getBoundingClientRect');
  });
});
