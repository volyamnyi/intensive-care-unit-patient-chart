import { test, expect } from '../../fixtures/index';
import type { APIRequestContext, Page } from '@playwright/test';

// E2E for the row-level «−» («Видалити день») button (issue #224, phase 4):
// removes the LAST day of the medicine row; empty and planned days go,
// completed-containing and last-remaining days are refused.
// Net-zero: every test creates its own item via API on patient 1003 (Сидоренко)
// and deletes it afterwards — no seed pollution.

const API = 'http://localhost:8085/api';
const PATIENT_ID = 1003;

const ADD_DAY = 'Додати день';
const REMOVE_DAY = 'Видалити день';
const COMPLETED_MSG = 'День містить виконані призначення, видалення неможливе';
const LAST_DAY_MSG = 'Неможливо видалити останній день призначення';

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

async function getParts(request: APIRequestContext, token: string, listId: string, itemId: string): Promise<DayPart[]> {
  const res = await request.get(`${API}/prescriptions/${listId}/items`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const items = (await res.json()) as { id: string; dayParts: DayPart[] }[];
  const item = items.find((i) => i.id === itemId);
  if (!item) throw new Error(`item not found: ${itemId}`);
  return item.dayParts;
}

function distinctDayIds(parts: DayPart[]): string[] {
  return [...new Set(parts.map((p) => p.dayId))];
}

function lastDay(parts: DayPart[]): { dayDate: string; dayId: string } {
  const sorted = [...parts].sort((a, b) => (a.dayDate < b.dayDate ? -1 : 1));
  const last = sorted[sorted.length - 1];
  if (!last) throw new Error('item has no days');
  return { dayDate: last.dayDate, dayId: last.dayId };
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

async function rowFor(page: Page, name: string) {
  const row = page.locator('tbody tr').filter({ hasText: name });
  await expect(row).toBeVisible({ timeout: 10_000 });
  return row;
}

test.describe('Doctor — «−» day removal (last day of the row)', () => {
  let doctorToken = '';
  let nurseToken = '';

  test.beforeAll(async ({ request }) => {
    doctorToken = await login(request, 'doctor1', 'doctor123');
    nurseToken = await login(request, 'nurse1', 'nurse123');
    await request.post(`${API}/mis/error-mode?mode=none`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
    });
  });

  test('«+» then «−» round-trip removes exactly the added last day', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-Мінус-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      expect(distinctDayIds(await getParts(request, doctorToken, listId, itemId))).toHaveLength(21);

      await gotoDetail(page, listId);
      const row = await rowFor(page, name);

      const [addRes] = await Promise.all([
        page.waitForResponse(
          (r) => r.url().includes(`/items/${itemId}/days`) && r.request().method() === 'POST',
        ),
        row.getByRole('button', { name: ADD_DAY }).click(),
      ]);
      expect(addRes.status()).toBe(201);
      const addedBody = await addRes.json();
      const addedParts = addedBody.dayParts as DayPart[];
      const added = lastDay(addedParts);
      expect(distinctDayIds(await getParts(request, doctorToken, listId, itemId))).toHaveLength(22);

      const [delRes] = await Promise.all([
        page.waitForResponse(
          (r) => /\/items\/[0-9a-f-]+\/days\/[0-9a-f-]+$/.test(r.url()) && r.request().method() === 'DELETE',
        ),
        row.getByRole('button', { name: REMOVE_DAY }).click(),
      ]);
      expect(delRes.status()).toBe(204);
      expect(delRes.url().endsWith(`/days/${added.dayId}`)).toBeTruthy();
      const doneToast = page.locator('[data-sonner-toast][data-type="success"]', { hasText: 'День видалено' });
      await expect(doneToast).toBeVisible({ timeout: 10_000 });

      const after = await getParts(request, doctorToken, listId, itemId);
      expect(distinctDayIds(after)).toHaveLength(21);
      expect(after.some((p) => p.dayId === added.dayId)).toBe(false);
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });

  test('day with a planned cell is removable', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-ПланДень-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const parts = await getParts(request, doctorToken, listId, itemId);
      const last = lastDay(parts);
      const morning = parts.find((p) => p.dayId === last.dayId && p.period === 'morning');
      if (!morning) throw new Error('no morning part on last day');
      await planPart(request, doctorToken, morning.id, '12.5 мг');

      await gotoDetail(page, listId);
      const row = await rowFor(page, name);

      const [delRes] = await Promise.all([
        page.waitForResponse(
          (r) => /\/items\/[0-9a-f-]+\/days\/[0-9a-f-]+$/.test(r.url()) && r.request().method() === 'DELETE',
        ),
        row.getByRole('button', { name: REMOVE_DAY }).click(),
      ]);
      expect(delRes.status()).toBe(204);
      expect(delRes.url().endsWith(`/days/${last.dayId}`)).toBeTruthy();

      const after = await getParts(request, doctorToken, listId, itemId);
      expect(after.some((p) => p.dayId === last.dayId)).toBe(false);
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });

  test('day with a completed cell is refused, state preserved, error shown', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const nurseAuth = { Authorization: `Bearer ${nurseToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-ВиконДень-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const parts = await getParts(request, doctorToken, listId, itemId);
      const last = lastDay(parts);
      const morning = parts.find((p) => p.dayId === last.dayId && p.period === 'morning');
      if (!morning) throw new Error('no morning part on last day');
      await planPart(request, doctorToken, morning.id, '12.5 мг');
      const done = await request.put(`${API}/prescriptions/day-parts/${morning.id}/complete`, { headers: nurseAuth });
      expect(done.status()).toBe(200);

      await gotoDetail(page, listId);
      const row = await rowFor(page, name);

      const [delRes] = await Promise.all([
        page.waitForResponse(
          (r) => /\/items\/[0-9a-f-]+\/days\/[0-9a-f-]+$/.test(r.url()) && r.request().method() === 'DELETE',
        ),
        row.getByRole('button', { name: REMOVE_DAY }).click(),
      ]);
      expect(delRes.status()).toBe(422);
      const errToast = page.locator('[data-sonner-toast][data-type="error"]', { hasText: COMPLETED_MSG });
      await expect(errToast).toBeVisible({ timeout: 10_000 });

      const after = await getParts(request, doctorToken, listId, itemId);
      expect(after.some((p) => p.dayId === last.dayId)).toBe(true);
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });

  test('last remaining day: «−» disabled, API removal → 422', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-Останній-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const parts = await getParts(request, doctorToken, listId, itemId);
      const ids = distinctDayIds(parts);
      expect(ids).toHaveLength(21);
      // Drain via API down to a single day.
      for (const dayId of ids.slice(1)) {
        const res = await request.delete(`${API}/prescriptions/items/${itemId}/days/${dayId}`, { headers: auth });
        expect(res.status()).toBe(204);
      }
      const remaining = distinctDayIds(await getParts(request, doctorToken, listId, itemId));
      expect(remaining).toHaveLength(1);

      await gotoDetail(page, listId);
      const row = await rowFor(page, name);
      await expect(row.getByRole('button', { name: REMOVE_DAY })).toBeDisabled();

      const blocked = await request.delete(
        `${API}/prescriptions/items/${itemId}/days/${remaining[0]}`,
        { headers: auth },
      );
      expect(blocked.status()).toBe(422);
      const blockedBody = await blocked.json();
      expect(blockedBody.code).toBe('BUSINESS_RULE');
      expect(blockedBody.message).toBe(LAST_DAY_MSG);
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });
});
