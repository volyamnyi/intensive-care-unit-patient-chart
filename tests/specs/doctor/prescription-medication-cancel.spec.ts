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
  const cell = row.locator('td', { hasText: text });
  await cell.click({ button: 'right' });
  const menu = page.getByRole('menu', { name: MENU_LABEL });
  await expect(menu).toBeVisible();
  return { cell, menu };
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
      const afterReload = page.locator('tbody tr').filter({ hasText: name }).locator('td', { hasText: DOSE });
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
