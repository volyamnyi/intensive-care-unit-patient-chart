import { useMemo } from 'react';
import { ChevronDown } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { PrescriptionInteractionsResponse } from '../../types/medication';
import { deriveInteractionAlertModel } from './interactionAlertModel';
import { INTERACTION_SEVERITY } from './interactionSeverity';
import { useInteractionAlertSignal } from './useInteractionAlertSignal';

function formatShortDate(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function scrollPairIntoView(itemIdA: string, itemIdB: string): void {
  for (const id of [itemIdA, itemIdB]) {
    const el = document.querySelector(`[data-item-id="${id}"]`);
    if (el) {
      el.scrollIntoView({ block: 'nearest' });
      return;
    }
  }
}

const ALERT_STYLE: Record<string, { variant: 'default' | 'destructive' | 'warning'; className: string }> = {
  critical: { variant: 'destructive', className: 'border-l-4 border-destructive/50 bg-destructive/10' },
  high: { variant: 'default', className: 'border-l-4 border-[#EA580C]/50 bg-[#EA580C]/10' },
  medium: { variant: 'warning', className: 'border-l-4 border-warning' },
};

/**
 * Persistent contextual interaction alert for the prescription sheet (#354).
 * One alert per list: max-severity header + severity-sorted pair rows.
 * Animates once per new/escalated interaction set (keyed remount);
 * polite updates swap content in place without re-animation.
 */
export default function InteractionAlert({
  interactions,
}: {
  interactions: PrescriptionInteractionsResponse | null;
}) {
  const vm = useMemo(() => deriveInteractionAlertModel(interactions), [interactions]);
  const animKey = useInteractionAlertSignal(vm);
  if (!vm.visible) return null;

  const top = vm.pairs[0];
  const topDef = INTERACTION_SEVERITY[top.wireSeverity as keyof typeof INTERACTION_SEVERITY] ?? INTERACTION_SEVERITY.medium;
  const Icon = topDef.icon;
  const isCritical = top.wireSeverity === 'critical';
  const multi = vm.pairs.length > 1;
  const style = ALERT_STYLE[top.wireSeverity] ?? ALERT_STYLE.medium;
  const summaryText = multi
    ? `Виявлено взаємодій: ${vm.totalPairs}. Найвищий рівень — ${topDef.label}`
    : top.title;

  return (
    <Alert
      key={animKey ?? 'settled'}
      variant={style.variant}
      role={isCritical ? 'alert' : 'status'}
      data-testid="interaction-alert"
      className={cn(style.className, 'mb-2', animKey ? 'interaction-alert-enter' : undefined)}
    >
      <Icon aria-hidden className="size-4" />
      <AlertTitle>
        <span className="flex flex-wrap items-center gap-1.5">
          <Badge className={topDef.badgeClass}>{topDef.label}</Badge>
          <span>{summaryText}</span>
        </span>
      </AlertTitle>
      <AlertDescription>
        {multi ? (
          <details {...(vm.maxPriority === 3 ? { open: true } : {})} className="group mt-1">
            <summary className="inline-flex cursor-pointer list-none items-center gap-1 text-sm font-medium [&::-webkit-details-marker]:hidden">
              Деталі ({vm.totalPairs})
              <ChevronDown aria-hidden className="size-4 transition-transform group-open:rotate-180" />
            </summary>
            <div className="mt-1.5 flex flex-col gap-1.5">
              {vm.pairs.map((p) => {
                const def = INTERACTION_SEVERITY[p.wireSeverity as keyof typeof INTERACTION_SEVERITY] ?? INTERACTION_SEVERITY.medium;
                return (
                  <div data-testid="interaction-pair" key={p.pairKey} className="flex flex-col gap-0.5">
                    <span className="flex flex-wrap items-center gap-1.5">
                      <Badge className={def.badgeClass}>{def.label}</Badge>
                      <span className="font-medium">{p.title}</span>
                    </span>
                    <span>{p.description}</span>
                    <span className="text-xs">
                      Період перетину: {formatShortDate(p.overlapStart)}–{formatShortDate(p.overlapEnd)}
                    </span>
                    <span>
                      <Button variant="ghost" size="xs" onClick={() => scrollPairIntoView(p.itemIdA, p.itemIdB)}>
                        Показати в таблиці
                      </Button>
                    </span>
                  </div>
                );
              })}
            </div>
          </details>
        ) : (
          <>
            <p>{top.description}</p>
            <p className="text-xs">
              Період перетину: {formatShortDate(top.overlapStart)}–{formatShortDate(top.overlapEnd)}
            </p>
            <Button variant="ghost" size="xs" onClick={() => scrollPairIntoView(top.itemIdA, top.itemIdB)}>
              Показати в таблиці
            </Button>
          </>
        )}
      </AlertDescription>
    </Alert>
  );
}
