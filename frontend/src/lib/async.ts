/**
 * Cap for per-row follow-up fan-outs (roster list loading, pool rows).
 * Bounds concurrent requests so large rosters cannot exhaust browser
 * sockets (net::ERR_INSUFFICIENT_RESOURCES); pages show skeletons meanwhile.
 */
export const PRESCRIPTION_FANOUT_LIMIT = 15;

/**
 * Bounded-concurrency fan-out. An unbounded `Promise.all` over hundreds of
 * patients exhausts browser sockets (`ERR_INSUFFICIENT_RESOURCES`); this keeps
 * at most `limit` requests in flight, preserves input order, and propagates
 * aborts.
 */
export async function mapWithConcurrency<T, R>(
  items: readonly T[],
  limit: number,
  fn: (item: T, signal: AbortSignal | undefined) => Promise<R>,
  signal?: AbortSignal,
): Promise<R[]> {
  const workers = Math.min(Math.max(limit, 1), items.length);
  const results: R[] = new Array(items.length);
  let next = 0;
  const runWorker = async (): Promise<void> => {
    for (;;) {
      if (signal?.aborted) {
        throw new DOMException('Aborted', 'AbortError');
      }
      const index = next;
      next += 1;
      if (index >= items.length) {
        return;
      }
      results[index] = await fn(items[index], signal);
    }
  };
  await Promise.all(Array.from({ length: workers }, () => runWorker()));
  return results;
}
