import type { LucideIcon } from 'lucide-react';
import { CircleAlert, Info, OctagonAlert, TriangleAlert } from 'lucide-react';

/**
 * Single source of truth for drug-interaction severity presentation (#350).
 * Backend emits 'medium' | 'high' | 'critical'; 'low' is config-only
 * (stored, never warned) so a future emission renders without redesign.
 * No enums/namespaces (erasableSyntaxOnly) — one const object.
 */
export interface InteractionSeverityDef {
  /** Display-sort rank: critical renders before high. Aggregation math uses bucket priority (HIGH=3) instead. */
  readonly priority: number;
  readonly label: string;
  readonly bucket: 'HIGH' | 'MEDIUM' | 'LOW';
  readonly icon: LucideIcon;
  readonly badgeClass: string;
  /** Assertive announce only for critical; everything else is a polite status. */
  readonly role: 'alert' | 'status';
}

export const INTERACTION_SEVERITY = {
  critical: { priority: 4, label: 'критично', bucket: 'HIGH', icon: OctagonAlert, badgeClass: 'bg-red-500', role: 'alert' },
  high: { priority: 3, label: 'високо', bucket: 'HIGH', icon: TriangleAlert, badgeClass: 'bg-orange-500', role: 'status' },
  medium: { priority: 2, label: 'помірно', bucket: 'MEDIUM', icon: CircleAlert, badgeClass: 'bg-amber-500', role: 'status' },
  low: { priority: 1, label: 'низько', bucket: 'LOW', icon: Info, badgeClass: 'bg-gray-400', role: 'status' },
} as const satisfies Record<string, InteractionSeverityDef>;

export type InteractionSeverityKey = keyof typeof INTERACTION_SEVERITY;

/** Severity sort order + labels (replaces the literals formerly in PrescriptionSpreadsheet). */
export const SEVERITY_ORDER: Record<string, number> = { critical: 4, high: 3, medium: 2, low: 1 };
export const SEVERITY_LABELS: Record<string, string> = { critical: 'критично', high: 'високо', medium: 'помірно', low: 'низько' };
