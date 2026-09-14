import { describe, it, expect, vi } from 'vitest';
import { mapWithConcurrency } from './async';

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('mapWithConcurrency', () => {
  it('returns results in input order', async () => {
    const result = await mapWithConcurrency([3, 1, 2], 2, async n => {
      await new Promise(r => setTimeout(r, (4 - n) * 5));
      return n * 10;
    });
    expect(result).toEqual([30, 10, 20]);
  });

  it('bounds concurrent executions', async () => {
    let active = 0;
    let peak = 0;
    const gates = Array.from({ length: 6 }, () => deferred<void>());
    const run = mapWithConcurrency(gates, 2, async gate => {
      active += 1;
      peak = Math.max(peak, active);
      await gate.promise;
      active -= 1;
    });
    await Promise.resolve();
    await Promise.resolve();
    expect(peak).toBe(2);
    for (const gate of gates) {
      gate.resolve();
    }
    await run;
    expect(peak).toBe(2);
  });

  it('handles empty input without calling fn', async () => {
    const fn = vi.fn();
    await expect(mapWithConcurrency([], 5, fn)).resolves.toEqual([]);
    expect(fn).not.toHaveBeenCalled();
  });

  it('propagates worker errors', async () => {
    await expect(
      mapWithConcurrency([1, 2], 2, async n => {
        if (n === 2) {
          throw new Error('boom');
        }
        return n;
      }),
    ).rejects.toThrow('boom');
  });

  it('stops pulling on abort', async () => {
    const controller = new AbortController();
    const started: number[] = [];
    const run = mapWithConcurrency([1, 2, 3, 4], 1, async (n, signal) => {
      started.push(n);
      expect(signal).toBe(controller.signal);
      if (n === 1) {
        controller.abort();
      }
      return n;
    }, controller.signal);
    await expect(run).rejects.toMatchObject({ name: 'AbortError' });
    expect(started).toEqual([1]);
  });
});
