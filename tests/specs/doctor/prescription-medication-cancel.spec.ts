import { test, expect } from '../../fixtures/index';
import type { APIRequestContext, Page } from '@playwright/test';

// E2E for «Відмінити препарат» + «Повернути у Заплановано» (issue #222, phase 2).
// Net-zero: every test creates its own item via API on patient 1003 (Сидоренко)
// and deletes it afterwards — no seed pollution.

const API = 'http://localhost:8085/api';
const PATIENT_ID = 1003;
const DOSE = '12.5 мг';

const MENU_LABEL = 'Контекстне меню дня';
const CANCEL_ITEM = 'Відмінити препарат';
const RESTORE_ITEM = 'Повернути у Заплановано';

const GREEN = 'rgb(200, 230, 201)';
const BLUE = 'rgb(187, 222, 251)';
const PURPLE = 'rgb(225, 190, 231)';

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

async function planPart(request: APIRequestContext, token: string, partId: string, dose: string): Promise<void> {
  const res = await request.put(`${API}/prescriptions/day-parts/${partId}/plan`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { dose },
  });
  expect(res.status()).toBe(200);
}

async function morningPartOfFirstDay(request: APIRequestContext, token: string, listId: string, itemId: string, skipDates: Set<string> = new Set()): Promise<DayPart> {
  const res = await request.get(`${API}/prescriptions/${listId}/items`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const items = (await res.json()) as { id: string; dayParts: DayPart[] }[];
  const item = items.find((i) => i.id === itemId);
  if (!item) throw new Error(`item not found: ${itemId}`);
  const candidates = item.dayParts
    .filter((p) => p.period === 'morning' && !skipDates.has(p.dayDate))
    .sort((a, b) => (a.dayDate < b.dayDate ? -1 : 1));
  expect(candidates.length).toBeGreaterThan(0);
  return candidates[0];
}

async function gotoDetail(page: Page, listId: string): Promise<void> {
  await page.goto(`/prescriptions/doctor/${listId}`);
  await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10_000 });
  await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
}

async function openMenuOnCell(page: Page, row: ReturnType<Page['locator']>, text: string) {
  // The row renders only after items load; items loading also completes
  // `allDates`, which drives the shift chevron's disabled state. Without this
  // wait the chevron reads disabled (0+7 >= 0) and the window never shifts.
  await expect(row).toBeVisible({ timeout: 10_000 });
  const cell = row.locator('td', { hasText: text });
  await shiftCellIntoView(page, cell);
  // Cells are 68x32; a first-column cell's center can sit under the sticky
  // «Препарат/Метод» column (z-10), so the contextmenu would hit the sticky
  // overlay instead of the td. Click near the cell's right edge instead.
  await cell.click({ button: 'right', position: { x: 50, y: 16 } });
  const menu = page.getByRole('menu', { name: MENU_LABEL });
  await expect(menu).toBeVisible();
  return { cell, menu };
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

test.describe('Doctor — «Відмінити препарат» and «Повернути у Заплановано»', () => {
  let doctorToken = '';
  let nurseToken = '';

  test.beforeAll(async ({ request }) => {
    doctorToken = await login(request, 'doctor1', 'doctor123');
    nurseToken = await login(request, 'nurse1', 'nurse123');
    await request.post(`${API}/mis/error-mode?mode=none`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
    });
  });

  test('cancel → purple ✕ → restore → blue dose → reload persists', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-Відміна-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const part = await morningPartOfFirstDay(request, doctorToken, listId, itemId);
      await planPart(request, doctorToken, part.id, DOSE);

      await gotoDetail(page, listId);
      const row = page.locator('tbody tr').filter({ hasText: name });

      // Planned cell offers cancel, not restore.
      let opened = await openMenuOnCell(page, row, DOSE);
      await expect(opened.menu.getByRole('menuitem', { name: CANCEL_ITEM })).toBeVisible();
      await expect(opened.menu.getByRole('menuitem', { name: RESTORE_ITEM })).toHaveCount(0);

      const [cancelRes] = await Promise.all([
        page.waitForResponse(
          (r) => r.url().endsWith(`/day-parts/${part.id}/cancel`) && r.request().method() === 'PUT',
        ),
        opened.menu.getByRole('menuitem', { name: CANCEL_ITEM }).click(),
      ]);
      expect(cancelRes.status()).toBe(200);

      // Cancelled cell: ✕ on purple, offers restore instead of cancel.
      opened = await openMenuOnCell(page, row, '✕');
      await expect(opened.cell).toHaveCSS('background-color', PURPLE);
      await expect(opened.menu.getByRole('menuitem', { name: CANCEL_ITEM })).toHaveCount(0);
      await expect(opened.menu.getByRole('menuitem', { name: RESTORE_ITEM })).toBeVisible();

      const [replanRes] = await Promise.all([
        page.waitForResponse(
          (r) => r.url().endsWith(`/day-parts/${part.id}/replan`) && r.request().method() === 'PUT',
        ),
        opened.menu.getByRole('menuitem', { name: RESTORE_ITEM }).click(),
      ]);
      expect(replanRes.status()).toBe(200);

      // Restored cell: same dose on blue.
      const restored = row.locator('td', { hasText: DOSE });
      await expect(restored).toHaveCSS('background-color', BLUE);

      // Reload keeps the restored state.
      await page.reload();
      await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
      const reloadRow = page.locator('tbody tr').filter({ hasText: name });
      await expect(reloadRow).toBeVisible({ timeout: 10_000 });
      const afterReload = reloadRow.locator('td', { hasText: DOSE });
      await shiftCellIntoView(page, afterReload);
      await expect(afterReload).toHaveCSS('background-color', BLUE);
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });

  test('completed cell offers neither cancel nor restore', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-Виконано-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const part = await morningPartOfFirstDay(request, doctorToken, listId, itemId);
      await planPart(request, doctorToken, part.id, DOSE);
      const done = await request.put(`${API}/prescriptions/day-parts/${part.id}/complete`, {
        headers: { Authorization: `Bearer ${nurseToken}` },
      });
      expect(done.status()).toBe(200);

      await gotoDetail(page, listId);
      const row = page.locator('tbody tr').filter({ hasText: name });
      const opened = await openMenuOnCell(page, row, '✓');
      await expect(opened.cell).toHaveCSS('background-color', GREEN);
      await expect(opened.menu.getByRole('menuitem', { name: CANCEL_ITEM })).toHaveCount(0);
      await expect(opened.menu.getByRole('menuitem', { name: RESTORE_ITEM })).toHaveCount(0);
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });

  test('API guards: double-cancel, restore-on-planned, restore/cancel-on-completed → 422', async ({ request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const nurseAuth = { Authorization: `Bearer ${nurseToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-Гарди-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const part = await morningPartOfFirstDay(request, doctorToken, listId, itemId);
      await planPart(request, doctorToken, part.id, DOSE);

      const cancel = (id: string, headers: Record<string, string>) =>
        request.put(`${API}/prescriptions/day-parts/${id}/cancel`, { headers });
      const replan = (id: string, headers: Record<string, string>) =>
        request.put(`${API}/prescriptions/day-parts/${id}/replan`, { headers });

      // Restore on a planned (not cancelled) part → 422.
      const restorePlanned = await replan(part.id, auth);
      expect(restorePlanned.status()).toBe(422);
      const restorePlannedBody = await restorePlanned.json();
      expect(restorePlannedBody.code).toBe('BUSINESS_RULE');
      expect(typeof restorePlannedBody.message).toBe('string');
      expect(typeof restorePlannedBody.correlationId).toBe('string');

      // Cancel → 200, double-cancel → 422.
      expect((await cancel(part.id, auth)).status()).toBe(200);
      const doubleCancel = await cancel(part.id, auth);
      expect(doubleCancel.status()).toBe(422);
      expect((await doubleCancel.json()).code).toBe('BUSINESS_RULE');

      // Restore → 200, then complete → cancel/restore on completed → 422.
      expect((await replan(part.id, auth)).status()).toBe(200);
      expect((await request.put(`${API}/prescriptions/day-parts/${part.id}/complete`, { headers: nurseAuth })).status()).toBe(200);
      expect((await cancel(part.id, auth)).status()).toBe(422);
      expect((await replan(part.id, auth)).status()).toBe(422);

      // Nurse is rejected on both mutating endpoints (403, valid path).
      expect((await cancel(part.id, nurseAuth)).status()).toBe(403);
      expect((await replan(part.id, nurseAuth)).status()).toBe(403);
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });
});
