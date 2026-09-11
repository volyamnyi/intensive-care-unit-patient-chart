import type { ProductionWorkItem } from '@/prosthetics/types';

export interface TrendPoint {
  /** Local `YYYY-MM-DD` bucket key. */
  date: string;
  /** Short uk-UA label (`5 вер`). */
  label: string;
  created: number;
  completed: number;
  failed: number;
}

function dayKey(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function keyOf(iso: string | null | undefined): string | null {
  if (!iso) return null;
  const parsed = new Date(iso);
  if (Number.isNaN(parsed.getTime())) return null;
  return dayKey(parsed);
}

/**
 * Buckets work items by day over the last `days` days (inclusive of today).
 * Created counts use `startTime ?? createdAt`; terminal counts use `endTime`
 * (COMPLETED → completed, FAILED/BRANCHED → failed; terminal rows without
 * `endTime` are ignored). Out-of-range and future dates are ignored.
 */
export function bucketTrend(
  items: ProductionWorkItem[],
  days: number,
  now: Date = new Date(),
): TrendPoint[] {
  const buckets = new Map<string, TrendPoint>();
  for (let i = days - 1; i >= 0; i--) {
    const day = new Date(now);
    day.setDate(day.getDate() - i);
    const key = dayKey(day);
    buckets.set(key, {
      date: key,
      label: day.toLocaleDateString('uk-UA', { day: 'numeric', month: 'short' }),
      created: 0,
      completed: 0,
      failed: 0,
    });
  }
  for (const item of items) {
    const createdKey = keyOf(item.startTime ?? item.createdAt);
    if (createdKey !== null) {
      const bucket = buckets.get(createdKey);
      if (bucket) bucket.created += 1;
    }
    if (item.endTime) {
      const endKey = keyOf(item.endTime);
      const bucket = endKey !== null ? buckets.get(endKey) : undefined;
      if (bucket) {
        if (item.status === 'COMPLETED') bucket.completed += 1;
        else if (item.status === 'FAILED' || item.status === 'BRANCHED') bucket.failed += 1;
      }
    }
  }
  return [...buckets.values()];
}
