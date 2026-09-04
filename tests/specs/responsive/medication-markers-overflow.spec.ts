import { test, expect } from '../../fixtures/index';
import type { APIRequestContext, Page } from '@playwright/test';

// Responsive overflow check for the medication grid with an added day and
// visual markers present (issue #226, phase 6). Runs under both
// responsive-mobile-chromium (360px) and responsive-tablet-chromium (768px).
// All fixture setup uses local-DB endpoints only (no MIS dependency, no
// error-mode juggling) to avoid cross-project interference.

const API = 'http://localhost:8085/api';
const PATIENT_ID = 1003;
const ROW_PREFIX = 'E2E-Responsive-';

async function login(request: APIRequestContext, login: string, password: string): Promise<string> {
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  return body.token as string;
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

function formatUa(iso: string): string {
  return new Date(iso).toLocaleDateString('uk-UA', { day: '2-digit', month: 'short' });
}

function itemsGrid(page: Page) {
  return page.locator('table').filter({ hasText: 'Препарат / Метод' });
}

type DayPart = {
  id: string;
  dayId: string;
  dayDate: string;
  period: string;
};

async function addedDateOf(request: APIRequestContext, token: string, listId: string, itemId: string): Promise<string> {
  const res = await request.get(`${API}/prescriptions/${listId}/items`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const items = (await res.json()) as { id: string; dayParts: DayPart[] }[];
  const item = items.find((i) => i.id === itemId);
  if (!item) throw new Error(`item not found: ${itemId}`);
  const max = item.dayParts.map((p) => p.dayDate).sort().at(-1);
  if (!max) throw new Error('item has no days');
  return max;
}

// The items grid opens at the oldest dates; shift the 7-day window until the
// target header renders.
async function shiftHeaderIntoView(page: Page, isoDate: string): Promise<void> {
  const header = itemsGrid(page).locator('thead tr').first().locator('th[colspan]', { hasText: formatUa(isoDate) });
  const shiftRight = page.locator('button:has(.lucide-chevron-right)').first();
  for (let i = 0; i < 30; i++) {
    if (await header.isVisible({ timeout: 300 }).catch(() => false)) return;
    if (await shiftRight.isDisabled().catch(() => true)) break;
    await shiftRight.click();
    await page.waitForTimeout(150);
  }
  await expect(header).toBeVisible({ timeout: 10_000 });
}

async function stableDocWidth(page: Page): Promise<{ doc: number; win: number }> {
  // Layout can shift after async fonts/data settle — poll until two reads agree.
  let prev = -1;
  for (let i = 0; i < 20; i++) {
    const cur: number = await page.evaluate(() => document.documentElement.scrollWidth);
    if (cur === prev) break;
    prev = cur;
    await page.waitForTimeout(150);
  }
  const win: number = await page.evaluate(() => window.innerWidth);
  const doc: number = await page.evaluate(() => document.documentElement.scrollWidth);
  return { doc, win };
}

test.describe('Responsive — medication grid with added-day markers', () => {
  let doctorToken = '';
  let listId = '';
  let itemId = '';

  test.beforeAll(async ({ request }) => {
    doctorToken = await login(request, 'doctor1', 'doctor123');
    listId = await openListId(request, doctorToken);
    const res = await request.post(`${API}/prescriptions/${listId}/items`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
      data: { medicineName: `${ROW_PREFIX}${Date.now()}`, medicineMethod: 'IV', regime: 'test' },
    });
    expect(res.status()).toBe(201);
    const body = await res.json();
    itemId = body.id as string;
    // Append one day so added-day markers render on this row.
    const added = await request.post(`${API}/prescriptions/items/${itemId}/days`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
    });
    expect(added.status()).toBe(201);
  });

  test.afterAll(async ({ request }) => {
    await request.delete(`${API}/prescriptions/items/${itemId}`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
    });
  });

  test('detail page has no horizontal overflow with markers present', async ({ page, request }) => {
    await page.goto(`/prescriptions/doctor/${listId}`, { waitUntil: 'domcontentloaded' });
    await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10_000 });
    await expect(page.getByText(/Статус: Відкрито/)).toBeVisible({ timeout: 10_000 });
    // Grid loaded (our row rendered) and the extended legend visible.
    await expect(page.locator('tbody tr').filter({ hasText: ROW_PREFIX })).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText('Доданий день')).toBeVisible();
    await expect(page.getByText('Пропущений день')).toBeVisible();
    // The grid opens at the oldest dates: shift until our appended day renders,
    // then assert its header marker.
    const addedDate = await addedDateOf(request, doctorToken, listId, itemId);
    await shiftHeaderIntoView(page, addedDate);
    await expect(page.locator('th[data-added-day="true"]')).not.toHaveCount(0);

    const { doc, win } = await stableDocWidth(page);
    expect(doc).toBeLessThanOrEqual(win + 1);
  });
});
