import { useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { PrescriptionInteractionsResponse } from '../../types/medication';
import { deriveInteractionAlertModel } from './interactionAlertModel';
import { showPairInGrid } from './interactionPairFocus';
import { INTERACTION_SEVERITY } from './interactionSeverity';
import {
  classifyInteractionSignal,
  snapshotOf,
  type SignalSnapshot,
} from './useInteractionAlertSignal';

/* Auto-dismiss delay for the centered announcement overlay (exported for
 * tests). Owner review-round decision: 6s — long enough to read the pair text,
 * short enough not to cover the grid. */
export const INTERACTION_OVERLAY_AUTODISMISS_MS = 6_000;

function formatShortDate(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`;
}

/**
 * Centered announcement overlay for detected drug interactions (#350–#355 fix).
 * The in-flow banner stays as persistent context; this floating card slides in
 * from the left at the viewport center on initial detect / escalation so it is
 * visible no matter how far the user scrolled. Non-blocking: the grid stays
 * usable behind it. Deliberately NOT assertive — the banner owns the single
 * `role="alert"` announcement; this card is a visual reinforcement
 * (`role="group"`), shown again only for a fresh signature.
 *
 * Rendered through a portal into document.body: layout wrappers animate with
 * transform-keyframes (e.g. `fade-in-up`), and a transformed ancestor turns
 * `position: fixed` into a containing-block-relative box — the overlay would
 * center against the page height instead of the viewport (the original bug).
 */
export default function InteractionAlertOverlay({
  interactions,
}: {
  interactions: PrescriptionInteractionsResponse | null;
}) {
  const vm = useMemo(() => deriveInteractionAlertModel(interactions), [interactions]);
  const prev = useRef<SignalSnapshot | null>(null);
  const dismissed = useRef<Set<string>>(new Set());
  const [shownSig, setShownSig] = useState<string | null>(null);
  const cardRef = useRef<HTMLDivElement>(null);
  // Read during render: by effect time the card is already unmounted.
  const focusInside = shownSig === null && (cardRef.current?.contains(document.activeElement) ?? false);

  useEffect(() => {
    if (!vm.visible) {
      prev.current = null;
      dismissed.current.clear();
      setShownSig(null);
      return;
    }
    const signal = classifyInteractionSignal(prev.current, vm);
    prev.current = snapshotOf(vm);
    if ((signal === 'initial' || signal === 'escalated') && !dismissed.current.has(vm.signature)) {
      setShownSig(vm.signature);
    }
  }, [vm]);

  useEffect(() => {
    if (shownSig === null) {
      if (focusInside) {
        const h1 = document.querySelector('main h1, h1');
        if (h1 instanceof HTMLElement) h1.focus();
        else if (document.activeElement instanceof HTMLElement) document.activeElement.blur();
      }
      return;
    }
    const t = window.setTimeout(() => {
      dismissed.current.add(shownSig);
      setShownSig(null);
    }, INTERACTION_OVERLAY_AUTODISMISS_MS);
    return () => window.clearTimeout(t);
  }, [shownSig, focusInside]);

  useEffect(() => {
    if (shownSig === null) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        dismissed.current.add(shownSig);
        setShownSig(null);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [shownSig]);

  if (!vm.visible || shownSig === null) return null;

  const top = vm.pairs[0];
  const topDef = INTERACTION_SEVERITY[top.wireSeverity as keyof typeof INTERACTION_SEVERITY] ?? INTERACTION_SEVERITY.medium;
  const Icon = topDef.icon;
  const summaryText = vm.pairs.length > 1
    ? `Виявлено взаємодій: ${vm.totalPairs}. Найвищий рівень — ${topDef.label}`
    : top.title;

  const dismiss = () => {
    dismissed.current.add(shownSig);
    setShownSig(null);
  };

  return createPortal(
    <div ref={cardRef} className="interaction-alert-overlay" data-testid="interaction-alert-overlay">
      <Alert
        key={shownSig}
        variant="default"
        role="group"
        aria-label={summaryText}
        className={cn(
          'interaction-alert-center-enter relative shadow-lg',
          top.wireSeverity === 'critical' && 'border-l-4 border-l-destructive',
          top.wireSeverity === 'high' && 'border-l-4 border-l-[#EA580C]',
          top.wireSeverity === 'medium' && 'border-l-4 border-l-warning',
        )}
      >
        <Button
          variant="ghost"
          size="icon-xs"
          aria-label="Закрити сповіщення"
          onClick={dismiss}
          className="absolute right-1 top-1"
        >
          <X aria-hidden className="size-4" />
        </Button>
        <Icon aria-hidden className="size-4" />
        <AlertTitle>
          <span className="flex flex-wrap items-center gap-1.5 pr-6">
            <Badge className={topDef.badgeClass}>{topDef.label}</Badge>
            <span>{summaryText}</span>
          </span>
        </AlertTitle>
        <AlertDescription>
          <p>{top.description}</p>
          <p className="text-xs">
            Період перетину: {formatShortDate(top.overlapStart)}–{formatShortDate(top.overlapEnd)}
          </p>
          <span className="mt-1 flex flex-wrap gap-1">
            <Button
              variant="ghost"
              size="xs"
              onClick={() => {
                dismiss();
                showPairInGrid(top.itemIdA, top.itemIdB);
              }}
            >
              Показати в таблиці
            </Button>
          </span>
          <p className="text-xs opacity-70">Детальний перелік — у банері зверху сторінки.</p>
        </AlertDescription>
      </Alert>
    </div>
    ,
    document.body,
  );
}
