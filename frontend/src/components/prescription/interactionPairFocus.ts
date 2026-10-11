/**
 * Grid focus helpers for the medication-interaction alert (#350–#355).
 * Shared by the in-flow banner (InteractionAlert) and the centered
 * announcement overlay (InteractionAlertOverlay): hovering/focusing a pair
 * rings its grid cells via `data-active-pair`, and the pair button scrolls
 * the first cell into view. Pure DOM helpers — no React state.
 */

export function pairCells(itemIdA: string, itemIdB: string): Element[] {
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

export function setActivePair(itemIdA: string, itemIdB: string, on: boolean): void {
  for (const el of pairCells(itemIdA, itemIdB)) {
    if (on) el.setAttribute('data-active-pair', 'true');
    else el.removeAttribute('data-active-pair');
  }
}

export function clearActivePair(): void {
  for (const el of document.querySelectorAll('[data-active-pair="true"]')) {
    el.removeAttribute('data-active-pair');
  }
}

export function showPairInGrid(itemIdA: string, itemIdB: string): void {
  setActivePair(itemIdA, itemIdB, true);
  const first = pairCells(itemIdA, itemIdB)[0];
  if (first) first.scrollIntoView({ block: 'nearest' });
}
