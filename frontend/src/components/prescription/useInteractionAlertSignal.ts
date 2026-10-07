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

/**
 * Returns the animation key: set (→ remount + single slide-in) only on
 * initial/escalated transitions, stable otherwise. StrictMode-safe: the
 * double-invoked effect classifies identically twice and converges to the
 * same key instead of toggling.
 */
export function useInteractionAlertSignal(vm: InteractionAlertViewModel): string | null {
  const prev = useRef<SignalSnapshot | null>(null);
  const [animKey, setAnimKey] = useState<string | null>(null);
  useEffect(() => {
    if (!vm.visible) {
      prev.current = null;
      setAnimKey(null);
      return;
    }
    const signal = classifyInteractionSignal(prev.current, vm);
    prev.current = snapshotOf(vm);
    if (signal === 'initial' || signal === 'escalated') setAnimKey(vm.signature);
  }, [vm]);
  return animKey;
}
