import type { MisOrderDocument } from './types';

/**
 * Prefill contract for TP-LL-02 Step 1 «Зняття та внесення об'ємних
 * розмірів» (`e0000020`, issue #283).
 *
 * Pure, UI-free helpers: a MIS limb-order document
 * (`spiDocumentProsthesCheck` via `GET .../orders/documents`) is mapped onto
 * the 12 `LowerLimbMeasurementForm` header fields. The wizard orchestration
 * (WizardScreen) owns fetching + state; this module owns the mapping rules:
 *
 * - precedence `existing value > MIS prefill > empty` — never overwrites;
 * - idempotent: re-applying with the same document changes nothing;
 * - absent/null/empty/unknown MIS values leave the field empty — no
 *   synthetic numbers, no invented mobility/gender values (MIS is read-only);
 * - the calendar date is local `YYYY-MM-DD` (no UTC shift).
 */

/** Mobility dropdown options — the single source of truth (#283). */
export const LOWER_LIMB_MOBILITY_OPTIONS = [
  '1 рівень',
  '2 рівень',
  '3 рівень',
  '4 рівень',
  'Інший / Не вказано',
] as const;

export type LowerLimbMobilityOption = (typeof LOWER_LIMB_MOBILITY_OPTIONS)[number];

/** Gender options mirrored from the form — unknown values are never coerced. */
const LOWER_LIMB_GENDER_OPTIONS = ['Чоловіча', 'Жіноча'] as const;

/** Element ids of the 12 header fields (structurally matches `LOWER_LIMB_ELEMENT_IDS`). */
export interface LowerLimbHeaderIds {
  blankNumber: string;
  date: string;
  pib: string;
  address: string;
  productCode: string;
  productName: string;
  mobilityLevel: string;
  gender: string;
  age: string;
  height: string;
  weight: string;
  notes: string;
}

const HEADER_KEYS = [
  'blankNumber',
  'date',
  'pib',
  'address',
  'productCode',
  'productName',
  'mobilityLevel',
  'gender',
  'age',
  'height',
  'weight',
  'notes',
] as const satisfies ReadonlyArray<keyof LowerLimbHeaderIds>;

/** Draft storage key shared with `ProstheticsContext` (single literal copy). */
const PROSTHETICS_DRAFT_STORAGE_KEY = 'prosthetics:draft';

/** Empty = absent or blank — the only state a prefill may fill. */
export function isEmptyWizardValue(value: unknown): boolean {
  if (value === undefined || value === null) return true;
  if (typeof value === 'string') return value.trim() === '';
  return String(value).trim() === '';
}

function nonBlankString(value: unknown): string | null {
  if (value === undefined || value === null) return null;
  const text = String(value).trim();
  return text === '' ? null : text;
}

/** Strict allowlist match — null/empty/unknown stay empty (never invented). */
export function normalizeMobilityLevel(raw: unknown): string | null {
  if (typeof raw !== 'string') return null;
  const text = raw.trim();
  return (LOWER_LIMB_MOBILITY_OPTIONS as readonly string[]).includes(text) ? text : null;
}

/** `Чоловіча`/`Жіноча` only — unknown values are never substituted. */
export function normalizeGender(raw: unknown): string | null {
  if (typeof raw !== 'string') return null;
  const text = raw.trim();
  return (LOWER_LIMB_GENDER_OPTIONS as readonly string[]).includes(text) ? text : null;
}

/**
 * Local calendar date as `YYYY-MM-DD` for `<input type="date">`.
 * Uses local getters (never `toISOString`, which shifts by timezone).
 */
export function getLocalTodayDate(now: Date = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

/**
 * Builds the candidate prefill keyed by element id. Only fields with a real
 * MIS/local source are present; everything else is omitted (stays empty).
 */
export function buildLowerLimbPrefill(
  doc: MisOrderDocument,
  ids: LowerLimbHeaderIds,
  today: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  const put = (id: string, value: string | null) => {
    if (value !== null && value !== '') out[id] = value;
  };
  put(ids.blankNumber, nonBlankString(doc.orderNumber));
  if (today !== '') out[ids.date] = today;
  put(ids.pib, nonBlankString(doc.patientFullName));
  put(ids.address, nonBlankString(doc.patientAddress));
  put(ids.productCode, nonBlankString(doc.productCode));
  put(ids.productName, nonBlankString(doc.productName));
  put(ids.mobilityLevel, normalizeMobilityLevel(doc.mobilityLevel));
  put(ids.gender, normalizeGender(doc.patientGender));
  put(ids.age, doc.age === undefined || doc.age === null ? null : nonBlankString(doc.age));
  put(ids.height, doc.height === undefined || doc.height === null ? null : nonBlankString(doc.height));
  put(ids.weight, doc.weight === undefined || doc.weight === null ? null : nonBlankString(doc.weight));
  put(ids.notes, nonBlankString(doc.note));
  return out;
}

export interface LowerLimbPrefillResult {
  /** Merged values (`values` itself when nothing was applied). */
  next: Record<string, unknown>;
  /** Element ids that received a prefill value, in header order. */
  applied: string[];
}

/**
 * Merges the MIS prefill into wizard `values` without touching existing
 * (user-entered or restored) values. Pure + idempotent: applying twice, or
 * applying after a refetch/re-render, changes nothing the second time.
 */
export function applyLowerLimbPrefill(
  values: Record<string, unknown>,
  doc: MisOrderDocument,
  ids: LowerLimbHeaderIds,
  today: string,
): LowerLimbPrefillResult {
  const prefill = buildLowerLimbPrefill(doc, ids, today);
  const applied: string[] = [];
  let next: Record<string, unknown> | null = null;
  for (const key of HEADER_KEYS) {
    const id = ids[key];
    const candidate = prefill[id];
    if (candidate === undefined) continue;
    if (!isEmptyWizardValue(values[id])) continue;
    if (next === null) next = { ...values };
    next[id] = candidate;
    applied.push(id);
  }
  if (next === null) return { next: values, applied };
  return { next, applied };
}

/**
 * Parses the deterministic local order number `MIS-{patientId}-{documentId}`
 * (mirrors backend `ProductionReadService.parseMisDocumentId`). Anything else
 * yields null — never throws.
 */
export function parseMisDocumentIdFromOrderNumber(orderNumber: unknown): string | null {
  if (typeof orderNumber !== 'string') return null;
  const parts = orderNumber.split('-');
  if (parts.length !== 3 || parts[0] !== 'MIS') return null;
  return /^\d+$/.test(parts[2]) ? parts[2] : null;
}

/**
 * Picks the MIS document backing the prefill: the wizard draft's
 * `misDocumentId` first (source of truth when the prosthetist picked a
 * document on setup step 2), then the order-number match, else the first
 * document. Never throws; null when there is nothing to prefill from.
 */
export function selectMisDocumentForPrefill(
  docs: MisOrderDocument[],
  preferredDocumentId?: string | null,
  orderNumber?: string | null,
): MisOrderDocument | null {
  if (!Array.isArray(docs) || docs.length === 0) return null;
  if (preferredDocumentId !== undefined && preferredDocumentId !== null && preferredDocumentId !== '') {
    const byDraft = docs.find((d) => d.documentId !== undefined && String(d.documentId) === preferredDocumentId);
    if (byDraft) return byDraft;
  }
  const parsed = parseMisDocumentIdFromOrderNumber(orderNumber ?? null);
  if (parsed !== null) {
    const byOrder = docs.find((d) => d.documentId !== undefined && String(d.documentId) === parsed);
    if (byOrder) return byOrder;
  }
  return docs[0] ?? null;
}

/**
 * Reads the setup-flow draft's picked MIS document id from storage (the same
 * key `ProstheticsContext` persists). Returns null outside a browser, on
 * corrupt payloads, or when nothing was picked — never throws.
 */
export function readDraftMisDocumentId(): string | null {
  try {
    if (typeof sessionStorage === 'undefined' && typeof localStorage === 'undefined') return null;
    const raw =
      (typeof sessionStorage !== 'undefined' ? sessionStorage.getItem(PROSTHETICS_DRAFT_STORAGE_KEY) : null) ??
      (typeof localStorage !== 'undefined' ? localStorage.getItem(PROSTHETICS_DRAFT_STORAGE_KEY) : null);
    if (!raw) return null;
    const draft = JSON.parse(raw) as { misDocumentId?: unknown };
    return typeof draft.misDocumentId === 'string' && draft.misDocumentId !== ''
      ? draft.misDocumentId
      : null;
  } catch {
    return null;
  }
}
