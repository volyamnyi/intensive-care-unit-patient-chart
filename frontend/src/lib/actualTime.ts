/**
 * Pure helpers for the actual administration/dispensing time (#443/#444).
 *
 * Mirrors the server-side rules from ActualTimeValidator (V1-V4) for inline UX
 * feedback; the server stays the source of truth. No I/O, no React, no clock
 * reads - callers pass `nowIso` explicitly so the logic stays testable.
 *
 * The domain stores NAIVE LOCAL timestamps (no zone, no offset). Every helper
 * therefore works on the wall-clock components alone and never routes a value
 * through `Date`: on a DST spring-forward day a local time such as 03:30 does
 * not exist, and the JS engine would silently shift it to 04:30 - moving a
 * medically significant fact by an hour (#447).
 */

export type ActualTimeValidationCode =
  | "OK"
  | "MALFORMED"
  | "FUTURE"
  | "BEFORE_MIN"
  | "AFTER_MAX"
  | "DISPENSE_AFTER_ADMIN";

/**
 * Ukrainian messages mirroring the server-side contract
 * (`ActualTimeValidator.MSG_*`). The server stays the source of truth - these
 * strings only drive the inline UX before the request is sent.
 */
export const ACTUAL_TIME_ERROR_MESSAGES: Record<ActualTimeValidationCode, string> = {
  OK: "",
  MALFORMED: "Невірний формат часу.",
  FUTURE: "Фактичний час не може бути в майбутньому",
  BEFORE_MIN: "Фактичний час поза межами періоду листка",
  AFTER_MAX: "Фактичний час поза межами періоду листка",
  DISPENSE_AFTER_ADMIN: "Час видачі не може бути пізніше часу введення",
};

/** Human-readable message for a validation code ('' for OK). */
export function actualTimeErrorMessage(code: ActualTimeValidationCode): string {
  return ACTUAL_TIME_ERROR_MESSAGES[code] ?? "";
}

const ISO_WITH_OPTIONAL_SECONDS = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/;
const TIME_HH_MM = /^\d{2}:\d{2}$/;
const TIME_HH_MM_SS = /^\d{2}:\d{2}:\d{2}$/;
/** Wire parts of a naive-local timestamp: yyyy, MM, dd, HH, mm, ss. */
const ISO_PARTS = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})$/;
/** Date-only part of a naive-local timestamp. */
const DATE_ONLY = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Current local wall-clock time in the wire format ("yyyy-MM-dd'T'HH:mm:ss").
 *
 * The domain stores naive local timestamps (design §6), so the browser's local
 * clock is the right source here - not `toISOString()`, which would convert to
 * UTC. Kept separate from {@link validateActualTime}, which never reads a
 * clock on its own (callers pass `nowIso` explicitly).
 */
export function currentLocalIso(now: Date = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  const date = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
  const time = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
  return `${date}T${time}`;
}

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
  if (!DATE_ONLY.test(datePart)) return null;
  return `${datePart}T${normalized}`;
}

/** Format ISO local `yyyy-MM-dd'T'HH:mm:ss` as `dd.MM.yyyy HH:mm:ss`. */
export function formatActualTime(iso: string | null | undefined): string {
  if (!iso || !ISO_WITH_OPTIONAL_SECONDS.test(iso)) return "";
  const parts = isoParts(iso);
  if (!parts) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${pad(parts.d)}.${pad(parts.mo)}.${parts.y} ${pad(parts.h)}:${pad(parts.mi)}:${pad(parts.s)}`;
}

/**
 * Split a naive-local ISO string into its wall-clock components.
 *
 * Returns null for a malformed value (including an impossible calendar date
 * such as 2026-02-30) - the server's `LocalDateTime.parse` rejects those too,
 * so the client mirror stays faithful.
 */
function isoParts(iso: string): { y: number; mo: number; d: number; h: number; mi: number; s: number } | null {
  const m = ISO_PARTS.exec(iso.length === 16 ? `${iso}:00` : iso);
  if (!m) return null;
  const y = Number(m[1]);
  const mo = Number(m[2]);
  const d = Number(m[3]);
  const h = Number(m[4]);
  const mi = Number(m[5]);
  const s = Number(m[6]);
  if (mo < 1 || mo > 12 || h > 23 || mi > 59 || s > 59) return null;
  // Calendar check - Date.UTC is only an arithmetic container here, never a
  // timezone conversion.
  const probe = new Date(Date.UTC(y, mo - 1, d));
  if (probe.getUTCFullYear() !== y || probe.getUTCMonth() !== mo - 1 || probe.getUTCDate() !== d) return null;
  return { y, mo, d, h, mi, s };
}

/**
 * Comparable number of a naive-local wall clock.
 *
 * `Date.UTC` only packs the wall-clock components into a number - no instant
 * is converted, so the result is independent of the browser's timezone and of
 * DST. Two naive-local values can therefore be compared directly.
 */
function naiveComparable(iso: string): number | null {
  const p = isoParts(iso);
  if (!p) return null;
  return Date.UTC(p.y, p.mo - 1, p.d, p.h, p.mi, p.s);
}

/**
 * Add days to a `yyyy-MM-dd` date without touching a timezone.
 *
 * `Date.UTC` is an arithmetic container only - the result is a calendar date,
 * never an instant, so a DST transition in the middle of the list period
 * cannot move the boundary by a day (#447).
 */
function addDaysToDate(isoDate: string, days: number): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate);
  if (!m) return isoDate;
  const probe = new Date(Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3]) + days));
  const y = probe.getUTCFullYear();
  const mo = String(probe.getUTCMonth() + 1).padStart(2, "0");
  const d = String(probe.getUTCDate()).padStart(2, "0");
  return `${y}-${mo}-${d}`;
}

/**
 * Mirror of V1–V4 (docs/Actual-Time-Design.md §5). Returns a single code
 * describing the first violation, or 'OK'.
 *
 * Every comparison runs on the naive-local wall clock, so the result does not
 * depend on the browser's timezone or on a DST transition.
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
  const adminParts = iso ? isoParts(iso) : null;
  if (!adminParts) return "MALFORMED";
  const adminComparable = naiveComparable(iso!)!;

  // V1: parse dispensed (if provided).
  let dispComparable: number | null = null;
  if (dispensedIso) {
    if (!isoParts(dispensedIso)) return "MALFORMED";
    dispComparable = naiveComparable(dispensedIso)!;
  }

  // V2: not in the future (now + tolerance, inclusive).
  if (nowIso) {
    const nowComparable = naiveComparable(nowIso);
    if (nowComparable !== null) {
      const toleranceMs = toleranceMin * 60 * 1000;
      if (adminComparable > nowComparable + toleranceMs) return "FUTURE";
      if (dispComparable !== null && dispComparable > nowComparable + toleranceMs) return "FUTURE";
    }
  }

  const adminDay = iso!.slice(0, 10);

  // V3: window [minDate 00:00; maxDate + 1 day] (both inclusive as dates).
  if (minDate && DATE_ONLY.test(minDate)) {
    if (adminDay < minDate) return "BEFORE_MIN";
  }
  if (maxDate && DATE_ONLY.test(maxDate)) {
    const upper = addDaysToDate(maxDate, 1);
    if (adminDay > upper) return "AFTER_MAX";
  }

  if (dispComparable !== null && dispensedIso) {
    const dispDay = dispensedIso.slice(0, 10);
    if (minDate && DATE_ONLY.test(minDate) && dispDay < minDate) return "BEFORE_MIN";
    if (maxDate && DATE_ONLY.test(maxDate)) {
      const upper = addDaysToDate(maxDate, 1);
      if (dispDay > upper) return "AFTER_MAX";
    }
  }

  // V4: dispensed must not be after administered.
  if (dispComparable !== null && dispComparable > adminComparable) {
    return "DISPENSE_AFTER_ADMIN";
  }

  return "OK";
}
