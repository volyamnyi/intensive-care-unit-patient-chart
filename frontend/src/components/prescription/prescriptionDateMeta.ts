import type { PrescriptionDayPart } from '../../types/medication';

// Derived visual metadata for added/removed medication days (Phase 6).
// No persistence: everything derives from the item date sequences, so markers
// recompute identically across reloads and need no migration.

export interface ItemDateMeta {
  /** Dates beyond the item's base 21-day window (dayDate > minDate + 20 days). */
  addedDates: Set<string>;
  /** Missing dates strictly inside [minDate, maxDate] (soft-deleted days). */
  removedGaps: Set<string>;
}

export interface GridDateMeta {
  perItem: Map<string, ItemDateMeta>;
  /** date -> medicine names for which the date is added (header marking). */
  headerAdded: Map<string, string[]>;
  /** Dates whose header follows a removed gap (edge marker). */
  headerGapAfter: Set<string>;
}

const DAY_MS = 86_400_000;
const BASE_WINDOW_DAYS = 21;

function toDayNumber(iso: string): number {
  return Math.floor(Date.parse(`${iso}T00:00:00Z`) / DAY_MS);
}

function fromDayNumber(n: number): string {
  return new Date(n * DAY_MS).toISOString().slice(0, 10);
}

export function itemDateMeta(dayDates: string[]): ItemDateMeta {
  const unique = [...new Set(dayDates)].sort();
  const addedDates = new Set<string>();
  const removedGaps = new Set<string>();
  if (unique.length === 0) return { addedDates, removedGaps };
  const min = toDayNumber(unique[0]);
  const max = toDayNumber(unique[unique.length - 1]);
  const present = new Set(unique);
  for (const d of unique) {
    if (toDayNumber(d) > min + (BASE_WINDOW_DAYS - 1)) addedDates.add(d);
  }
  for (let n = min; n <= max; n++) {
    const iso = fromDayNumber(n);
    if (!present.has(iso)) removedGaps.add(iso);
  }
  return { addedDates, removedGaps };
}

export interface DateMetaInput {
  id: string;
  medicineName: string;
  dayDates: string[];
}

export function gridDateMeta(items: DateMetaInput[]): GridDateMeta {
  const perItem = new Map<string, ItemDateMeta>();
  const headerAdded = new Map<string, string[]>();
  const headerGapAfter = new Set<string>();
  for (const item of items) {
    const meta = itemDateMeta(item.dayDates);
    perItem.set(item.id, meta);
    for (const d of meta.addedDates) {
      const names = headerAdded.get(d) ?? [];
      names.push(item.medicineName);
      headerAdded.set(d, names);
    }
    const present = new Set(item.dayDates);
    for (const d of present) {
      const prev = fromDayNumber(toDayNumber(d) - 1);
      if (meta.removedGaps.has(prev)) headerGapAfter.add(d);
    }
  }
  return { perItem, headerAdded, headerGapAfter };
}

// Status colors always win: only inactive (white) cells of added days are
// marked. An undefined part (another row's date) is never marked, because
// addedDates only ever contains the row's own dates.
export function shouldMarkAddedCell(
  meta: ItemDateMeta | undefined,
  dayDate: string,
  part: PrescriptionDayPart | undefined,
): boolean {
  if (!meta || !meta.addedDates.has(dayDate)) return false;
  if (!part) return true;
  return !part.isPlanned && !part.isPlannedFinished && !part.isCompleted && !part.isCompletedFinished;
}
