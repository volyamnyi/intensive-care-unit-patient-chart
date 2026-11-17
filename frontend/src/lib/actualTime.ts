import dayjs from "dayjs";

/**
 * Pure helpers for the actual administration/dispensing time (#443/#444).
 *
 * Mirrors the server-side rules from ActualTimeValidator (V1-V4) for inline UX
 * feedback; the server stays the source of truth. No I/O, no React, no clock
 * reads - callers pass `nowIso` explicitly so the logic stays testable.
 */

export type ActualTimeValidationCode =
  | "OK"
  | "MALFORMED"
  | "FUTURE"
  | "BEFORE_MIN"
  | "AFTER_MAX"
  | "DISPENSE_AFTER_ADMIN";

const ISO_WITH_OPTIONAL_SECONDS = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/;
const TIME_HH_MM = /^\d{2}:\d{2}$/;
const TIME_HH_MM_SS = /^\d{2}:\d{2}:\d{2}$/;

/** Pad a browser-emitted `HH:mm` to `HH:mm:00`. Returns '' for garbage. */
export function normalizeTime(t: string | null | undefined): string {
  if (!t) return "";
  const trimmed = t.trim();
  if (TIME_HH_MM_SS.test(trimmed)) return trimmed;
  if (TIME_HH_MM.test(trimmed)) return `${trimmed}:00`;
  return "";
}

/** Compose an ISO local string only when both parts are present. */
export function composeActualTime(
  datePart: string | null | undefined,
  timePart: string | null | undefined,
): string | null {
  if (!datePart || !timePart) return null;
  const normalized = normalizeTime(timePart);
  if (!normalized) return null;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(datePart)) return null;
  return `${datePart}T${normalized}`;
}

/** Format ISO local `yyyy-MM-dd'T'HH:mm:ss` as `dd.MM.yyyy HH:mm:ss`. */
export function formatActualTime(iso: string | null | undefined): string {
  if (!iso || !ISO_WITH_OPTIONAL_SECONDS.test(iso)) return "";
  // Normalize to seconds before formatting so `HH:mm` inputs render consistently.
  const padded = iso.length === 16 ? `${iso}:00` : iso;
  try {
    return dayjs(padded).format("DD.MM.YYYY HH:mm:ss");
  } catch {
    return "";
  }
}

function parseIso(iso: string): Date | null {
  if (!ISO_WITH_OPTIONAL_SECONDS.test(iso)) return null;
  const padded = iso.length === 16 ? `${iso}:00` : iso;
  const d = new Date(padded);
  return Number.isNaN(d.getTime()) ? null : d;
}

function addDays(isoDate: string, days: number): string {
  const d = new Date(`${isoDate}T00:00:00`);
  d.setDate(d.getDate() + days);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

/**
 * Mirror of V1-V4 (docs/Actual-Time-Design.md §5). Returns a single code
 * describing the first violation, or 'OK'.
 */
export function validateActualTime(
  iso: string | null | undefined,
  opts: {
    minDate?: string | null;
    maxDate?: string | null;
    nowIso?: string | null;
    toleranceMin?: number;
    dispensedIso?: string | null;
  } = {},
): ActualTimeValidationCode {
  const { minDate, maxDate, nowIso, toleranceMin = 5, dispensedIso } = opts;

  // V1: parse administered.
  if (!iso || !parseIso(iso)) return "MALFORMED";
  const adminDate = parseIso(iso)!;

  // V1: parse dispensed (if provided).
  let dispDate: Date | null = null;
  if (dispensedIso) {
    if (!parseIso(dispensedIso)) return "MALFORMED";
    dispDate = parseIso(dispensedIso)!;
  }

  // V2: not in the future (now + tolerance, inclusive).
  if (nowIso) {
    const now = parseIso(nowIso);
    if (now) {
      const tolMs = toleranceMin * 60 * 1000;
      if (adminDate.getTime() > now.getTime() + tolMs) return "FUTURE";
      if (dispDate && dispDate.getTime() > now.getTime() + tolMs) return "FUTURE";
    }
  }

  const adminDay = iso.slice(0, 10);

  // V3: window [minDate 00:00; maxDate + 1 day] (inclusive on both ends as dates).
  if (minDate) {
    if (adminDay < minDate) return "BEFORE_MIN";
  }
  if (maxDate) {
    const upper = addDays(maxDate, 1);
    if (adminDay > upper) return "AFTER_MAX";
  }

  if (dispDate && dispensedIso) {
    const dispDay = dispensedIso.slice(0, 10);
    if (minDate && dispDay < minDate) return "BEFORE_MIN";
    if (maxDate) {
      const upper = addDays(maxDate, 1);
      if (dispDay > upper) return "AFTER_MAX";
    }
  }

  // V4: dispensed must not be after administered.
  if (dispDate && dispDate.getTime() > adminDate.getTime()) {
    return "DISPENSE_AFTER_ADMIN";
  }

  return "OK";
}
