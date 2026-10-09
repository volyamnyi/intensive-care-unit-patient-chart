import { useEffect, useRef, useState } from 'react';
import type { InteractionAlertViewModel } from './interactionAlertModel';
import { INTERACTION_SEVERITY } from './interactionSeverity';

export type InteractionSignal = 'initial' | 'escalated' | 'updated-polite' | 'resolved' | 'stable';

export interface SignalSnapshot {
  signature: string;
  maxPriority: 1 | 2 | 3 | null;
  keys: string[];
  sevByKey: Record<string, string>;
}

export interface AlertSignalState {
  animKey: string | null;
  live: boolean;
  announce: string | null;
}

export function snapshotOf(vm: InteractionAlertViewModel): SignalSnapshot {
  const sevByKey: Record<string, string> = {};
  for (const p of vm.pairs) sevByKey[p.pairKey] = p.wireSeverity;
  return {
    signature: vm.signature,
    maxPriority: vm.maxPriority,
    keys: vm.pairs.map((p) => p.pairKey),
    sevByKey,
  };
}

function severityRank(severity: string): number {
  return INTERACTION_SEVERITY[severity as keyof typeof INTERACTION_SEVERITY]?.priority ?? 2;
}

/**
 * Pure classification of the prev → next transition (#354).
 * Assertive (animate + announce) only on initial detect and escalation;
 * everything else updates silently. Never throws.
 */
export function classifyInteractionSignal(
  prev: SignalSnapshot | null,
  next: InteractionAlertViewModel,
): InteractionSignal {
  if (!next.visible) return 'resolved';
  if (prev === null) return 'initial';
  if (prev.signature === next.signature) return 'stable';
  if (next.maxPriority !== null && (prev.maxPriority === null || next.maxPriority > prev.maxPriority)) {
    return 'escalated';
  }
  const prevKeys = new Set(prev.keys);
  for (const p of next.pairs) {
    if (!prevKeys.has(p.pairKey)) {
      // A new pair at or above the current max re-announces; below max stays polite.
      if (next.maxPriority !== null && p.priority >= next.maxPriority) return 'escalated';
      return 'updated-polite';
    }
    const prevSev = prev.sevByKey[p.pairKey];
    if (prevSev !== undefined && severityRank(p.wireSeverity) > severityRank(prevSev)) return 'escalated';
  }
  return 'updated-polite';
}

const GENITIVE: Record<string, string> = { critical: 'критичного', high: 'високого', medium: 'помірного', low: 'низького' };
const NOMINATIVE: Record<string, string> = { critical: 'критичний', high: 'високий', medium: 'помірний', low: 'низький' };

function formatShortDate(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`;
}

/** Exact Ukrainian announcement strings for assertive transitions (#355). */
export function buildAnnounce(vm: InteractionAlertViewModel, signal: InteractionSignal): string | null {
  if (vm.pairs.length === 0 || (signal !== 'initial' && signal !== 'escalated')) return null;
  const top = vm.pairs[0];
  const gen = GENITIVE[top.wireSeverity] ?? top.wireSeverity;
  if (signal === 'escalated') {
    return `Увага. Рівень взаємодії підвищено до ${gen}: ${top.nameA} та ${top.nameB}. ${top.description}`;
  }
  if (vm.pairs.length === 1) {
    return `Увага. Виявлено взаємодію ${gen} рівня: ${top.nameA} та ${top.nameB}. `
      + `${top.description} Період перетину: ${formatShortDate(top.overlapStart)} — ${formatShortDate(top.overlapEnd)}.`;
  }
  const nom = NOMINATIVE[top.wireSeverity] ?? top.wireSeverity;
  return `Увага. Виявлено взаємодій: ${vm.totalPairs}. Найвищий рівень — ${nom}: ${top.nameA} + ${top.nameB}.`;
}

/**
 * Alert signal state: animation key (set → remount + single slide-in, only on
 * initial/escalated), assertive liveness (decays after the entrance settles),
 * and the exact announcement string. StrictMode-safe: the double-invoked
 * effect classifies identically twice and converges instead of toggling.
 */
export function useInteractionAlertSignal(vm: InteractionAlertViewModel): AlertSignalState {
  const prev = useRef<SignalSnapshot | null>(null);
  const [animKey, setAnimKey] = useState<string | null>(null);
  const [live, setLive] = useState(false);
  const [announce, setAnnounce] = useState<string | null>(null);
  useEffect(() => {
    if (!vm.visible) {
      prev.current = null;
      setAnimKey(null);
      setLive(false);
      setAnnounce(null);
      return;
    }
    const signal = classifyInteractionSignal(prev.current, vm);
    prev.current = snapshotOf(vm);
    if (signal === 'initial' || signal === 'escalated') {
      setAnimKey(vm.signature);
      setLive(true);
      setAnnounce(buildAnnounce(vm, signal));
      const t = window.setTimeout(() => {
        setLive(false);
        setAnnounce(null);
      }, 600);
      return () => window.clearTimeout(t);
    }
    return undefined;
  }, [vm]);
  return { animKey, live, announce };
}
