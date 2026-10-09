import { useEffect, useMemo, useRef, useState } from 'react';
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

function pairCells(itemIdA: string, itemIdB: string): Element[] {
  const cells: Element[] = [];
  for (const id of [itemIdA, itemIdB]) {
    const nameCell = document.querySelector(`[data-item-id="${CSS.escape(id)}"]`);
    if (!nameCell) continue;
    cells.push(nameCell);
    const row = nameCell.closest('tr');
    if (row) cells.push(...row.querySelectorAll('td[data-interaction-warn="true"]'));
  }
  return cells;
}

function setActivePair(itemIdA: string, itemIdB: string, on: boolean): void {
  for (const el of pairCells(itemIdA, itemIdB)) {
    if (on) el.setAttribute('data-active-pair', 'true');
    else el.removeAttribute('data-active-pair');
  }
}

function clearActivePair(): void {
  for (const el of document.querySelectorAll('[data-active-pair="true"]')) {
    el.removeAttribute('data-active-pair');
  }
}

function showPairInGrid(itemIdA: string, itemIdB: string): void {
  setActivePair(itemIdA, itemIdB, true);
  const first = pairCells(itemIdA, itemIdB)[0];
  if (first) first.scrollIntoView({ block: 'nearest' });
}

function readStoredExpanded(storageKey: string | null): boolean | null {
  if (!storageKey) return null;
  try {
    const v = sessionStorage.getItem(storageKey);
    return v === null ? null : v === '1';
  } catch {
    return null;
  }
}

function writeStoredExpanded(storageKey: string | null, value: boolean): void {
  if (!storageKey) return;
  try {
    sessionStorage.setItem(storageKey, value ? '1' : '0');
  } catch {
    /* private mode: memory state still works */
  }
}

const ALERT_STYLE: Record<string, { variant: 'default' | 'destructive' | 'warning'; className: string }> = {
  critical: { variant: 'destructive', className: 'border-l-4 border-destructive/50 bg-destructive/10' },
  high: { variant: 'default', className: 'border-l-4 border-[#EA580C]/50 bg-[#EA580C]/10' },
  medium: { variant: 'warning', className: 'border-l-4 border-warning' },
};

function PairButton({ itemIdA, itemIdB }: { itemIdA: string; itemIdB: string }) {
  return (
    <Button
      variant="ghost"
      size="xs"
      onClick={() => showPairInGrid(itemIdA, itemIdB)}
      onMouseEnter={() => setActivePair(itemIdA, itemIdB, true)}
      onMouseLeave={clearActivePair}
      onFocus={() => setActivePair(itemIdA, itemIdB, true)}
      onBlur={clearActivePair}
      onKeyDown={(e) => { if (e.key === 'Escape') clearActivePair(); }}
    >
      Показати в таблиці
    </Button>
  );
}

/**
 * Persistent contextual interaction alert for the prescription sheet (#354/#355).
 * One alert per list: max-severity header + severity-sorted pair rows.
 * Animates once per new/escalated interaction set (keyed remount);
 * polite updates swap content in place without re-animation.
 * Expand choice persists per list; HIGH never dismisses; unmount is data-driven.
 */
export default function InteractionAlert({
  interactions,
  listId,
}: {
  interactions: PrescriptionInteractionsResponse | null;
  listId?: string;
}) {
  const vm = useMemo(() => deriveInteractionAlertModel(interactions), [interactions]);
  const { animKey, live, announce } = useInteractionAlertSignal(vm);
  const storageKey = listId ? `interaction-alert-expanded:${listId}` : null;

  const [open, setOpen] = useState(false);
  const [resolveNotice, setResolveNotice] = useState<string | null>(null);
  const seenData = useRef(false);
  const prevListId = useRef<string | undefined>(undefined);
  const bannerRef = useRef<HTMLDivElement>(null);
  // Read during render: by effect time the banner is already unmounted and the ref detached.
  const focusInside = !vm.visible && (bannerRef.current?.contains(document.activeElement) ?? false);

  useEffect(() => {
    if (prevListId.current !== listId) {
      prevListId.current = listId;
      seenData.current = false;
    }
    if (!vm.visible) {
      if (seenData.current) {
        // Focus was inside the unmounting banner: return it to the page heading.
        if (focusInside) {
          const h1 = document.querySelector('main h1, h1');
          if (h1 instanceof HTMLElement) h1.focus();
          else if (document.activeElement instanceof HTMLElement) document.activeElement.blur();
        }
        setResolveNotice('Взаємодій препаратів більше не виявлено.');
      }
      seenData.current = false;
      return;
    }
    setResolveNotice(null);
    if (!seenData.current) {
      seenData.current = true;
      setOpen(readStoredExpanded(storageKey) ?? vm.maxPriority === 3);
    }
  }, [vm, listId, storageKey, focusInside]);

  const toggle = () => {
    const next = !open;
    setOpen(next);
    writeStoredExpanded(storageKey, next);
  };

  return (
    <>
      {vm.visible && (
        <div ref={bannerRef}>
          <Alert
            key={animKey ?? 'settled'}
            variant={(ALERT_STYLE[vm.pairs[0].wireSeverity] ?? ALERT_STYLE.medium).variant}
            role={(vm.pairs[0].wireSeverity === 'critical' || live) ? 'alert' : 'status'}
            aria-label={announce ?? undefined}
            data-testid="interaction-alert"
            className={cn(
              (ALERT_STYLE[vm.pairs[0].wireSeverity] ?? ALERT_STYLE.medium).className,
              'mb-2',
              animKey ? 'interaction-alert-enter' : undefined,
            )}
          >
            <AlertHeader vm={vm} />
            <AlertBody
              vm={vm}
              open={open}
              onToggle={toggle}
            />
          </Alert>
        </div>
      )}
      {resolveNotice && (
        <div role="status" className="sr-only">
          {resolveNotice}
        </div>
      )}
    </>
  );
}

function AlertHeader({ vm }: { vm: ReturnType<typeof deriveInteractionAlertModel> }) {
  const top = vm.pairs[0];
  const topDef = INTERACTION_SEVERITY[top.wireSeverity as keyof typeof INTERACTION_SEVERITY] ?? INTERACTION_SEVERITY.medium;
  const Icon = topDef.icon;
  const summaryText = vm.pairs.length > 1
    ? `Виявлено взаємодій: ${vm.totalPairs}. Найвищий рівень — ${topDef.label}`
    : top.title;
  return (
    <>
      <Icon aria-hidden className="size-4" />
      <AlertTitle>
        <span className="flex flex-wrap items-center gap-1.5">
          <Badge className={topDef.badgeClass}>{topDef.label}</Badge>
          <span>{summaryText}</span>
        </span>
      </AlertTitle>
    </>
  );
}

function AlertBody({
  vm, open, onToggle,
}: {
  vm: ReturnType<typeof deriveInteractionAlertModel>;
  open: boolean;
  onToggle: () => void;
}) {
  const top = vm.pairs[0];
  if (vm.pairs.length === 1) {
    return (
      <AlertDescription>
        <p>{top.description}</p>
        <p className="text-xs">
          Період перетину: {formatShortDate(top.overlapStart)}–{formatShortDate(top.overlapEnd)}
        </p>
        <PairButton itemIdA={top.itemIdA} itemIdB={top.itemIdB} />
      </AlertDescription>
    );
  }
  return (
    <AlertDescription>
      <Button
        variant="ghost"
        size="xs"
        aria-expanded={open}
        aria-controls="interaction-pair-list"
        onClick={onToggle}
        onKeyDown={(e) => { if (e.key === 'Escape' && open) onToggle(); }}
      >
        Деталі ({vm.totalPairs})
        <ChevronDown aria-hidden className={`size-4 transition-transform ${open ? 'rotate-180' : ''}`} />
      </Button>
      <div id="interaction-pair-list" hidden={!open}>
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
                  <PairButton itemIdA={p.itemIdA} itemIdB={p.itemIdB} />
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </AlertDescription>
  );
}
