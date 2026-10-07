import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

/**
 * Motion contract for the medication-interaction alert entrance (#354):
 * one-time slide-in with a prefers-reduced-motion gate. Real animation
 * behavior is covered by E2E; jsdom asserts the CSS contract only.
 *
 * NOTE: index.css ?raw comes back empty through the Vitest CSS pipeline —
 * read it from disk (same pattern as focusMotion.test.ts).
 */
function readIndexCss(): string {
  return readFileSync(resolve(import.meta.dirname, '../../index.css'), 'utf8');
}

describe('Interaction alert motion contract', () => {
  it('defines the one-time slide-in entrance', () => {
    const src = readIndexCss();
    expect(src).toContain('@keyframes interactionAlertIn');
    expect(src).toContain('.interaction-alert-enter');
    expect(src).toContain('translateX(-16px)');
    expect(src).toContain('animation: interactionAlertIn 0.25s');
  });

  it('gates the entrance behind prefers-reduced-motion', () => {
    const src = readIndexCss();
    const gates = src.split('@media (prefers-reduced-motion: reduce)');
    const hit = gates.some((block) => block.includes('.interaction-alert-enter') && block.includes('animation: none'));
    expect(hit).toBe(true);
  });
});
