import { test, expect } from '../../fixtures/index';
import type { APIRequestContext, Page } from '@playwright/test';

// E2E for added/removed-day visual distinction (issue #226, phase 6):
// added days get header + inactive-cell markers, removed gaps get an edge
// marker on the following header, status colors always win, everything is
// derived (reload-stable). Net-zero: temp items on patient 1003 (Сидоренко).

const API = 'http://localhost:8085/api';
const PATIENT_ID = 1003;
const DOSE = '12.5 мг';

const ADD_DAY = 'Додати день';

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

function formatUa(iso: string): string {
  return new Date(iso).toLocaleDateString('uk-UA', { day: '2-digit', month: 'short' });
}

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

function lastDay(parts: DayPart[]): { dayDate: string; dayId: string } {
  const sorted = [...parts].sort((a, b) => (a.dayDate < b.dayDate ? -1 : 1));
  const last = sorted[sorted.length - 1];
  if (!last) throw new Error('item has no days');
  return { dayDate: last.dayDate, dayId: last.dayId };
}

async function gotoDetail(page: Page, listId: string): Promise<void> {
  await page.goto(`/prescriptions/doctor/${listId}`);
  await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10_000 });
  await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
}

function itemsGrid(page: Page) {
  return page.locator('table').filter({ hasText: 'Препарат / Метод' });
}

async function rowFor(page: Page, name: string) {
  const row = page.locator('tbody tr').filter({ hasText: name });
  await expect(row).toBeVisible({ timeout: 10_000 });
  return row;
}

// The items grid opens at the oldest dates; shift the 7-day window until the
// locator renders (works for cells and date headers alike).
async function shiftIntoView(page: Page, target: ReturnType<Page['locator']>): Promise<void> {
  const shiftRight = page.locator('button:has(.lucide-chevron-right)').first();
  for (let i = 0; i < 30; i++) {
    if (await target.isVisible({ timeout: 300 }).catch(() => false)) return;
    if (await shiftRight.isDisabled().catch(() => true)) break;
    await shiftRight.click();
    await page.waitForTimeout(150);
  }
  await expect(target).toBeVisible({ timeout: 10_000 });
}

async function dateHeader(page: Page, isoDate: string) {
  const header = itemsGrid(page).locator('thead tr').first().locator('th[colspan]', { hasText: formatUa(isoDate) });
  await shiftIntoView(page, header);
  return header;
}

test.describe('Doctor — added/removed day visual distinction', () => {
  let doctorToken = '';

  test.beforeAll(async ({ request }) => {
    doctorToken = await login(request, 'doctor1', 'doctor123');
    await request.post(`${API}/mis/error-mode?mode=none`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
    });
  });

  test('added day: marked header + cells, status wins on planned cell, reload persists, no overflow', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-Маркер-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      await gotoDetail(page, listId);
      const row = await rowFor(page, name);

      const [addRes] = await Promise.all([
        page.waitForResponse(
          (r) => r.url().includes(`/items/${itemId}/days`) && r.request().method() === 'POST',
        ),
        row.getByRole('button', { name: ADD_DAY }).click(),
      ]);
      expect(addRes.status()).toBe(201);
      const addedParts = (await addRes.json()).dayParts as DayPart[];
      const added = lastDay(addedParts);

      // Header of the added day is marked with the owning medicine in the title.
      const header = await dateHeader(page, added.dayDate);
      await expect(header).toHaveAttribute('data-added-day', 'true');
      expect(await header.getAttribute('title')).toContain(name);

      // All four white slots of the added day are marked.
      await expect(row.locator('td[data-added-day="true"]')).toHaveCount(4);

      // No horizontal page overflow with the added day present.
      const widths = await page.evaluate(() => ({
        doc: document.documentElement.scrollWidth,
        win: window.innerWidth,
      }));
      expect(widths.doc).toBeLessThanOrEqual(widths.win + 1);

      // Plan one cell of the added day: status color wins, marker drops there.
      const morning = addedParts.find((p) => p.dayId === added.dayId && p.period === 'morning');
      if (!morning) throw new Error('no morning part on added day');
      const planned = await request.put(`${API}/prescriptions/day-parts/${morning.id}/plan`, {
        headers: auth,
        data: { dose: DOSE },
      });
      expect(planned.status()).toBe(200);

      await page.reload();
      await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
      const reloadRow = await rowFor(page, name);
      const plannedCell = reloadRow.locator('td', { hasText: DOSE });
      await shiftIntoView(page, plannedCell);
      await expect(plannedCell).toHaveCSS('background-color', 'rgb(187, 222, 251)');
      await expect(plannedCell).not.toHaveAttribute('data-added-day', 'true');
      await expect(reloadRow.locator('td[data-added-day="true"]')).toHaveCount(3);
      const reloadHeader = await dateHeader(page, added.dayDate);
      await expect(reloadHeader).toHaveAttribute('data-added-day', 'true');
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });

  test('removed gap: following header carries the edge marker, reload persists', async ({ page, request }) => {
    const auth = { Authorization: `Bearer ${doctorToken}` };
    const listId = await openListId(request, doctorToken);
    const name = `E2E-Пропуск-${Date.now()}`;
    const itemId = await createItem(request, doctorToken, listId, name);
    try {
      const parts = await getParts(request, doctorToken, listId, itemId);
      const dayIds = [...new Set(parts.map((p) => p.dayId))];
      expect(dayIds).toHaveLength(21);
      const removedDayId = dayIds[5];
      const removedDate = parts.find((p) => p.dayId === removedDayId)?.dayDate;
      if (!removedDate) throw new Error('removed day has no date');
      const followingDate = parts
        .map((p) => p.dayDate)
        .filter((d) => d > removedDate)
        .sort()[0];
      if (!followingDate) throw new Error('no date follows the removed day');

      const delRes = await request.delete(`${API}/prescriptions/items/${itemId}/days/${removedDayId}`, { headers: auth });
      expect(delRes.status()).toBe(204);

      await gotoDetail(page, listId);
      await rowFor(page, name);

      const header = await dateHeader(page, followingDate);
      await expect(header).toHaveAttribute('data-removed-gap', 'true');
      expect(await header.getAttribute('title')).toContain(formatUa(followingDate));
      // Nothing was added in this test: no added-day markers anywhere.
      await expect(page.locator('[data-added-day]')).toHaveCount(0);

      await page.reload();
      await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
      await rowFor(page, name);
      const reloadHeader = await dateHeader(page, followingDate);
      await expect(reloadHeader).toHaveAttribute('data-removed-gap', 'true');
    } finally {
      await request.delete(`${API}/prescriptions/items/${itemId}`, { headers: auth });
    }
  });
});
