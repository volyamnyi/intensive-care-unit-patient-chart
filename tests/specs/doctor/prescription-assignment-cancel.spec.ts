import { test, expect } from '../../fixtures/index';
import type { APIRequestContext, Page } from '@playwright/test';

// E2E for «Відмінити це призначення» (issue #223, phase 3): strict single-cell
// scope — only the right-clicked period cell returns to «Не заплановано»
// (white, empty); siblings, other days and rows stay untouched.
// Net-zero: every test creates its own item via API on patient 1003 (Сидоренко)
// and deletes it afterwards — no seed pollution.

const API = 'http://localhost:8085/api';
const PATIENT_ID = 1003;
const TARGET_DOSE = '12.5 мг';
const SIBLING_DOSE = '7.5 мг';

const MENU_LABEL = 'Контекстне меню дня';
const UNASSIGN_ITEM = 'Відмінити це призначення';

const WHITE = 'rgb(255, 255, 255)';
const BLUE = 'rgb(187, 222, 251)';
const GREEN = 'rgb(200, 230, 201)';

async function login(request: APIRequestContext, login: string, password: string): Promise<string> {
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  return body.token as string;
}

type DayPart = {
  id: string;
  dayId: string;
  dayDate: string;
  period: string;
  dose: string | null;
};

async function openListId(request: APIRequestContext, token: string): Promise<string> {
  const res = await request.get(`${API}/prescriptions`, {
    params: { patientId: PATIENT_ID },
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const lists = (await res.json()) as { id: string; status: string }[];
  const open = lists.find((l) => l.status !== 'Finished');
  if (!open) throw new Error('patient 1003 must have an open prescription list');
  return open.id;
}

async function createItem(request: APIRequestContext, token: string, listId: string, name: string): Promise<string> {
  const res = await request.post(`${API}/prescriptions/${listId}/items`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { medicineName: name, medicineMethod: 'IV', regime: 'test' },
  });
  expect(res.status()).toBe(201);
  const body = await res.json();
  return body.id as string;
}

async function firstDayParts(request: APIRequestContext, token: string, listId: string, itemId: string): Promise<DayPart[]> {
  const res = await request.get(`${API}/prescriptions/${listId}/items`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const items = (await res.json()) as { id: string; dayParts: DayPart[] }[];
  const item = items.find((i) => i.id === itemId);
  if (!item) throw new Error(`item not found: ${itemId}`);
  const firstDate = item.dayParts.map((p) => p.dayDate).sort()[0];
  const order = ['morning', 'day', 'evening', 'night'];
  return item.dayParts
    .filter((p) => p.dayDate === firstDate)
    .sort((a, b) => order.indexOf(a.period) - order.indexOf(b.period));
}

async function planPart(request: APIRequestContext, token: string, partId: string, dose: string): Promise<void> {
  const res = await request.put(`${API}/prescriptions/day-parts/${partId}/plan`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { dose },
  });
  expect(res.status()).toBe(200);
}

async function gotoDetail(page: Page, listId: string): Promise<void> {
  await page.goto(`/prescriptions/doctor/${listId}`);
  await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10_000 });
  await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
}

// The items grid opens at the oldest dates: `viewStart` useState initialises
// to 0 while items are still loading, so a newly added item (days from today)
// starts outside the visible 7-day window. Shift right until the cell renders.
async function shiftCellIntoView(page: Page, cell: ReturnType<Page['locator']>): Promise<void> {
  // Items grid renders before the vital grid, so its chevron is first in DOM order.
  const shiftRight = page.locator('button:has(.lucide-chevron-right)').first();
  for (let i = 0; i < 30; i++) {
    if (await cell.isVisible({ timeout: 300 }).catch(() => false)) return;
    if (await shiftRight.isDisabled().catch(() => true)) break;
    await shiftRight.click();
    await page.waitForTimeout(150);
  }
  await expect(cell).toBeVisible({ timeout: 10_000 });
}

function formatUa(iso: string): string {
  return new Date(iso).toLocaleDateString('uk-UA', { day: '2-digit', month: 'short' });
}

// The detail page renders TWO tables (item grid AND vital-sign grid); scope
// every grid locator to the items table, uniquely identified by its
// «Препарат / Метод» header (vital grid uses «Показник»).
function itemsGrid(page: Page) {
  return page.locator('table').filter({ hasText: 'Препарат / Метод' });
}

const PERIOD_OFFSET: Record<string, number> = { morning: 0, day: 1, evening: 2, night: 3 };

// Pinpoints the period cell of a KNOWN date by header-column index — the only
// reliable locator once the action clears the cell text. Shifts the 7-day
// window until the date header renders, then resolves nth(1 + dateColIdx*4 + offset).
async function periodCellOfDate(
  page: Page,
  grid: ReturnType<Page['locator']>,
  row: ReturnType<Page['locator']>,
  isoDate: string,
  period: string,
) {
  const dateLabel = formatUa(isoDate);
  const header = grid.locator('thead tr').first().locator('th[colspan]', { hasText: dateLabel });
  await shiftCellIntoView(page, header);
  const dateColIdx = await grid.locator('thead tr').first().locator('th[colspan]').evaluateAll(
    (ths, target) => ths.findIndex((t) => (t.textContent ?? '').trim() === target),
    dateLabel,
  );
  expect(dateColIdx).toBeGreaterThanOrEqual(0);
  return row.locator('td').nth(1 + dateColIdx * 4 + (PERIOD_OFFSET[period] ?? 0));
}

async function rightClickCell(page: Page, cell: ReturnType<Page['locator']>) {
  // Cells are 68x32; a first-column cell's center can sit under the sticky
  // «Препарат/Метод» column (z-10), so the contextmenu would hit the sticky
  // overlay instead of the td. Click near the cell's right edge instead.
  await cell.click({ button: 'right', position: { x: 50, y: 16 } });
  const menu = page.getByRole('menu', { name: MENU_LABEL });
  await expect(menu).toBeVisible();
  return menu;
}

async function openMenuOnCell(page: Page, row: ReturnType<Page['locator']>, text: string) {
  // The row renders only after items load; items loading also completes
  // `allDates`, which drives the shift chevron's disabled state. Without this
  // wait the chevron reads disabled (0+7 >= 0) and the window never shifts.
  await expect(row).toBeVisible({ timeout: 10_000 });
  const cell = row.locator('td', { hasText: text });
  await shiftCellIntoView(page, cell);
  const menu = await rightClickCell(page, cell);
  return { cell, menu };
}

// Clicks a context-menu item and waits for BOTH the mutation PUT and the
// subsequent items GET (`loadItems` refetch). Waiting for the GET is required:
// locating the next cell before the grid re-renders makes the shift loop
// overshoot the target window with no way back.
async function clickMenuItemAndReload(
  page: Page,
  menu: ReturnType<Page['locator']>,
  name: string,
  putUrlSuffix: string,
  listId: string,
) {
  const [putRes] = await Promise.all([
    page.waitForResponse(
      (r) => r.url().endsWith(putUrlSuffix) && r.request().method() === 'PUT',
    ),
    page.waitForResponse(
      (r) => r.url().includes(`/prescriptions/${listId}/items`) && r.request().method() === 'GET',
    ),
    menu.getByRole('menuitem', { name }).click(),
  ]);
  return putRes;
}

test.describe('Doctor — «Відмінити це призначення» (single-cell scope)', () => {
  let doctorToken = '';
  let nurseToken = '';

  test.beforeAll(async ({ request }) => {
    doctorToken = await login(request, 'doctor1', 'doctor123');
    nurseToken = await login(request, 'nurse1', 'nurse123');
    await request.post(`${API}/mis/error-mode?mode=none`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
    });
  });

  test('planned cell → white empty, sibling/day/row untouched, reload persists', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-Призначення-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const parts = await firstDayParts(request, doctorToken, listId, itemId);
      expect(parts).toHaveLength(4);
      const targetDate = parts[0].dayDate;
      await planPart(request, doctorToken, parts[0].id, TARGET_DOSE);
      await planPart(request, doctorToken, parts[1].id, SIBLING_DOSE);

      await gotoDetail(page, listId);
      const grid = itemsGrid(page);
      const row = grid.locator('tbody tr').filter({ hasText: name });
      await expect(row).toBeVisible({ timeout: 10_000 });

      const target = await periodCellOfDate(page, grid, row, targetDate, 'morning');
      await expect(target).toContainText(TARGET_DOSE);
      const menu = await rightClickCell(page, target);
      await expect(menu.getByRole('menuitem', { name: UNASSIGN_ITEM })).toBeVisible();

      const putRes = await clickMenuItemAndReload(page, menu, UNASSIGN_ITEM, `/day-parts/${parts[0].id}/cancel-assignment`, listId);
      expect(putRes.status()).toBe(200);

      // Target cell: white, empty. Sibling cell of the same day: dose kept, blue.
      const cleared = await periodCellOfDate(page, grid, row, targetDate, 'morning');
      await expect(cleared).toHaveCSS('background-color', WHITE);
      await expect(cleared).toHaveText('');
      const sibling = row.locator('td', { hasText: SIBLING_DOSE });
      await expect(sibling).toHaveCSS('background-color', BLUE);

      // Reload keeps the isolation: target still white/empty, sibling intact.
      await page.reload();
      await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
      const reloadRow = grid.locator('tbody tr').filter({ hasText: name });
      await expect(reloadRow).toBeVisible({ timeout: 10_000 });
      const reloadCleared = await periodCellOfDate(page, grid, reloadRow, targetDate, 'morning');
      await expect(reloadCleared).toHaveCSS('background-color', WHITE);
      await expect(reloadCleared).toHaveText('');
      const reloadSibling = reloadRow.locator('td', { hasText: SIBLING_DOSE });
      await shiftCellIntoView(page, reloadSibling);
      await expect(reloadSibling).toHaveCSS('background-color', BLUE);
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });

  test('cancelled cell → white empty', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-Відмінено-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const parts = await firstDayParts(request, doctorToken, listId, itemId);
      const targetDate = parts[0].dayDate;
      await planPart(request, doctorToken, parts[0].id, TARGET_DOSE);
      const cancelled = await request.put(`${API}/prescriptions/day-parts/${parts[0].id}/cancel`, { headers: auth });
      expect(cancelled.status()).toBe(200);

      await gotoDetail(page, listId);
      const grid = itemsGrid(page);
      const row = grid.locator('tbody tr').filter({ hasText: name });
      await expect(row).toBeVisible({ timeout: 10_000 });
      const cancelledCell = await periodCellOfDate(page, grid, row, targetDate, 'morning');
      await expect(cancelledCell).toContainText('✕');
      const menu = await rightClickCell(page, cancelledCell);
      await expect(menu.getByRole('menuitem', { name: UNASSIGN_ITEM })).toBeVisible();

      const putRes = await clickMenuItemAndReload(page, menu, UNASSIGN_ITEM, `/day-parts/${parts[0].id}/cancel-assignment`, listId);
      expect(putRes.status()).toBe(200);

      const cleared = await periodCellOfDate(page, grid, row, targetDate, 'morning');
      await expect(cleared).toHaveCSS('background-color', WHITE);
      await expect(cleared).toHaveText('');
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });

  test('completed and empty cells offer no such item', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const nurseAuth = { Authorization: `Bearer ${nurseToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-Захист-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const parts = await firstDayParts(request, doctorToken, listId, itemId);
      await planPart(request, doctorToken, parts[0].id, TARGET_DOSE);
      const done = await request.put(`${API}/prescriptions/day-parts/${parts[0].id}/complete`, { headers: nurseAuth });
      expect(done.status()).toBe(200);

      await gotoDetail(page, listId);
      const row = page.locator('tbody tr').filter({ hasText: name });

      const completed = await openMenuOnCell(page, row, '✓');
      await expect(completed.cell).toHaveCSS('background-color', GREEN);
      await expect(completed.menu.getByRole('menuitem', { name: UNASSIGN_ITEM })).toHaveCount(0);
      await page.keyboard.press('Escape');

      // Evening cell of the same day was never planned: menu opens, item hidden.
      const evening = completed.cell.locator('xpath=following-sibling::td[2]');
      await evening.click({ button: 'right', position: { x: 50, y: 16 } });
      const menu = page.getByRole('menu', { name: MENU_LABEL });
      await expect(menu).toBeVisible();
      await expect(menu.getByRole('menuitem', { name: UNASSIGN_ITEM })).toHaveCount(0);
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });

  test('API guards: completed → 422, unknown → 404, double-unassign → 200, nurse → 403', async ({ request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const nurseAuth = { Authorization: `Bearer ${nurseToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-ГардиПризначення-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const parts = await firstDayParts(request, doctorToken, listId, itemId);
      await planPart(request, doctorToken, parts[0].id, TARGET_DOSE);

      const unassign = (id: string, headers: Record<string, string>) =>
        request.put(`${API}/prescriptions/day-parts/${id}/cancel-assignment`, { headers });

      // Planned → 200, flags cleared.
      const first = await unassign(parts[0].id, auth);
      expect(first.status()).toBe(200);
      const firstBody = (await first.json()) as { isPlanned: boolean; isPlannedFinished: boolean; dose: string | null };
      expect(firstBody.isPlanned).toBe(false);
      expect(firstBody.isPlannedFinished).toBe(false);
      expect(firstBody.dose).toBeNull();

      // Double-unassign is idempotent → 200.
      expect((await unassign(parts[0].id, auth)).status()).toBe(200);

      // Completed part → 422 with ErrorResponse shape.
      await planPart(request, doctorToken, parts[1].id, SIBLING_DOSE);
      const done = await request.put(`${API}/prescriptions/day-parts/${parts[1].id}/complete`, { headers: nurseAuth });
      expect(done.status()).toBe(200);
      const blocked = await unassign(parts[1].id, auth);
      expect(blocked.status()).toBe(422);
      const blockedBody = await blocked.json();
      expect(blockedBody.code).toBe('BUSINESS_RULE');
      expect(typeof blockedBody.message).toBe('string');
      expect(typeof blockedBody.correlationId).toBe('string');

      // Unknown part → 404.
      const missing = await unassign('00000000-0000-0000-0000-000000000000', auth);
      expect(missing.status()).toBe(404);

      // Nurse → 403 (valid path).
      expect((await unassign(parts[2].id, nurseAuth)).status()).toBe(403);
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });
});
