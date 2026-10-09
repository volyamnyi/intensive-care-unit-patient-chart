import type { PrescriptionInteractionsResponse } from '../../types/medication';
import { INTERACTION_SEVERITY } from './interactionSeverity';

export type InteractionWireSeverity = 'medium' | 'high' | 'critical';
export type InteractionBucket = 'HIGH' | 'MEDIUM' | 'LOW';

export interface InteractionAlertPair {
  pairKey: string;
  itemIdA: string;
  nameA: string;
  itemIdB: string;
  nameB: string;
  wireSeverity: InteractionWireSeverity;
  bucket: InteractionBucket;
  /** Aggregation priority: HIGH=3, MEDIUM=2, LOW=1 (critical folds into HIGH). */
  priority: 1 | 2 | 3;
  title: string;
  description: string;
  overlapStart: string;
  overlapEnd: string;
  interactionIds: string[];
  accessibilityLabel: string;
}

export interface InteractionAlertViewModel {
  pairs: InteractionAlertPair[];
  maxPriority: 1 | 2 | 3 | null;
  maxBucket: InteractionBucket | null;
  totalPairs: number;
  /** Stable hash of the interaction set — the sole re-animation input. */
  signature: string;
  visible: boolean;
}

const BUCKET_PRIORITY: Record<InteractionBucket, 1 | 2 | 3> = { HIGH: 3, MEDIUM: 2, LOW: 1 };

/** Canonical 3-level mapping; unknown future severities fall back to MEDIUM, never throw. */
export function priorityOf(severity: string): { bucket: InteractionBucket; priority: 1 | 2 | 3 } {
  const bucket = (INTERACTION_SEVERITY[severity as keyof typeof INTERACTION_SEVERITY]?.bucket ?? 'MEDIUM') as InteractionBucket;
  return { bucket, priority: BUCKET_PRIORITY[bucket] };
}

/**
 * Pure derivation: wire response → alert view-model (#350).
 * Dedupes the bidirectional wire (A→B and B→A collapse into one pair,
 * mirroring the backend `joinTexts` aggregation); no React, no backend.
 */
export function deriveInteractionAlertModel(
  response: PrescriptionInteractionsResponse | null | undefined,
): InteractionAlertViewModel {
  const empty: InteractionAlertViewModel = {
    pairs: [], maxPriority: null, maxBucket: null, totalPairs: 0, signature: '', visible: false,
  };
  const warnings = response?.warnings ?? [];
  if (warnings.length === 0) return empty;

  const byKey = new Map<string, {
    ids: [string, string]; names: [string, string];
    wireSeverity: string; texts: string[]; overlap: [string, string]; interactionIds: string[];
  }>();
  for (const w of warnings) {
    for (const p of w.interactions ?? []) {
      const [idA, idB] = [w.itemId, p.otherItemId].sort();
      const key = `${idA}‖${idB}|${p.overlapStart}|${p.overlapEnd}`;
      // Display order is first-seen (warning owner first), NOT UUID-sorted:
      // item ids are freshly generated per list, so UUID order would flip
      // titles/announcements/screenshots randomly between runs.
      const names: [string, string] = [w.nameUk, p.otherNameUk];
      const slot = byKey.get(key);
      if (slot) {
        // Keep the first row's severity (same rule as the backend aggregation).
        if (p.interactionText && !slot.texts.includes(p.interactionText)) slot.texts.push(p.interactionText);
        for (const iid of p.interactionIds ?? []) {
          if (!slot.interactionIds.includes(iid)) slot.interactionIds.push(iid);
        }
      } else {
        byKey.set(key, {
          ids: [idA, idB], names,
          wireSeverity: p.severity,
          texts: p.interactionText ? [p.interactionText] : [],
          overlap: [p.overlapStart, p.overlapEnd],
          interactionIds: [...(p.interactionIds ?? [])],
        });
      }
    }
  }

  const pairs: InteractionAlertPair[] = [...byKey.entries()].map(([pairKey, s]) => {
    const { bucket, priority } = priorityOf(s.wireSeverity);
    const title = `${s.names[0]} + ${s.names[1]}`;
    const description = s.texts.join(' / ');
    const label = INTERACTION_SEVERITY[s.wireSeverity as keyof typeof INTERACTION_SEVERITY]?.label ?? s.wireSeverity;
    return {
      pairKey,
      itemIdA: s.ids[0], nameA: s.names[0],
      itemIdB: s.ids[1], nameB: s.names[1],
      wireSeverity: s.wireSeverity as InteractionWireSeverity,
      bucket, priority, title, description,
      overlapStart: s.overlap[0], overlapEnd: s.overlap[1],
      interactionIds: s.interactionIds,
      accessibilityLabel: `Взаємодія: ${s.names[0]} та ${s.names[1]}. Рівень — ${label}. ${description}`,
    };
  });

  // Deterministic order: severity rank desc → nameA (uk) → overlapStart.
  const rankOf = (p: InteractionAlertPair) =>
    INTERACTION_SEVERITY[p.wireSeverity as keyof typeof INTERACTION_SEVERITY]?.priority ?? 2;
  pairs.sort((a, b) =>
    rankOf(b) - rankOf(a)
    || a.nameA.localeCompare(b.nameA, 'uk')
    || (a.overlapStart < b.overlapStart ? -1 : a.overlapStart > b.overlapStart ? 1 : 0));

  const maxPriority = pairs.reduce<1 | 2 | 3 | null>(
    (m, p) => (m === null || p.priority > m ? p.priority : m), null);
  const maxBucket = pairs.find((p) => p.priority === maxPriority)?.bucket ?? null;
  const signature = pairs
    .map((p) => [p.pairKey, p.wireSeverity, p.overlapStart, p.overlapEnd, p.interactionIds.join('+')].join(':'))
    .sort()
    .join('|');

  return {
    pairs,
    maxPriority,
    maxBucket,
    totalPairs: pairs.length,
    signature: `${maxPriority ?? ''}|${signature}`,
    visible: true,
  };
}
