import { test, expect, type APIRequestContext } from '@playwright/test';

// API contract for per-item prescription day endpoints (issue #169):
//   POST   /api/prescriptions/items/{itemId}/days         201  (PRESCRIPTION_CREATE)
//   DELETE /api/prescriptions/items/{itemId}/days/{dayId} 204  (PRESCRIPTION_CREATE)
// Nurses do NOT hold PRESCRIPTION_CREATE → 403 on both endpoints.
// DELETE on a day with completed parts → 422 BUSINESS_RULE.

const API = 'http://localhost:8085/api';

// Patient 1001 seed: морфін item + completed day → 422; open day → 204.
const MORFIN_ITEM_ID = '40f40760-4807-997e-d706-7293273f0769';
const MORFIN_COMPLETED_DAY_ID = '138b0217-910b-6b52-75e5-2af782041333';
const MORFIN_OPEN_DAY_ID = '13e71c36-1def-1eaa-82d0-98c6d441a435';

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
  isPlanned: boolean;
  isPlannedFinished: boolean;
  isCompleted: boolean;
  isCompletedFinished: boolean;
};

test.describe('Prescription-day API access controls', () => {
  test.beforeAll(async ({ request }) => {
    const token = await login(request, 'doctor1', 'doctor123');
    await request.post(`${API}/mis/error-mode?mode=none`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  });

  test('nurse is rejected (403) on POST /items/{id}/days', async ({ request }) => {
    const token = await login(request, 'nurse1', 'nurse123');
    const res = await request.post(`${API}/prescriptions/items/${MORFIN_ITEM_ID}/days`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(403);
    const body = await res.json();
    expect(typeof body.code).toBe('string');
    expect(typeof body.message).toBe('string');
    expect(typeof body.correlationId).toBe('string');
  });

  test('nurse is rejected (403) on DELETE /items/{id}/days/{dayId}', async ({ request }) => {
    const token = await login(request, 'nurse1', 'nurse123');
    const res = await request.delete(
      `${API}/prescriptions/items/${MORFIN_ITEM_ID}/days/${MORFIN_OPEN_DAY_ID}`,
      { headers: { Authorization: `Bearer ${token}` } },
    );
    expect(res.status()).toBe(403);
  });

  test('doctor adds a new day (201) — new day has 4 unplanned parts', async ({ request }) => {
    const token = await login(request, 'doctor1', 'doctor123');
    const res = await request.post(`${API}/prescriptions/items/${MORFIN_ITEM_ID}/days`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body.id).toBe(MORFIN_ITEM_ID);
    expect(Array.isArray(body.dayParts)).toBeTruthy();
    expect((body.dayParts as unknown[]).length).toBeGreaterThan(0);

    // The newly added day is max(dayDate).
    const parts = body.dayParts as DayPart[];
    const addedDate = parts.map(p => p.dayDate).sort().at(-1) as string;
    expect(addedDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);

    const newParts = parts.filter(p => p.dayDate === addedDate);
    expect(newParts).toHaveLength(4);
    expect(new Set(newParts.map(p => p.period))).toEqual(
      new Set(['morning', 'day', 'evening', 'night']),
    );
    const dayIds = new Set(newParts.map(p => p.dayId));
    expect(dayIds.size).toBe(1);
    for (const part of newParts) {
      expect(part.id).toMatch(/^[0-9a-f-]{36}$/);
      expect(part.isPlanned).toBe(false);
      expect(part.isPlannedFinished).toBe(false);
      expect(part.isCompleted).toBe(false);
      expect(part.isCompletedFinished).toBe(false);
    }
  });

  test('doctor is rejected (422) when removing a completed day', async ({ request }) => {
    const token = await login(request, 'doctor1', 'doctor123');
    const res = await request.delete(
      `${API}/prescriptions/items/${MORFIN_ITEM_ID}/days/${MORFIN_COMPLETED_DAY_ID}`,
      { headers: { Authorization: `Bearer ${token}` } },
    );
    expect(res.status()).toBe(422);
    const body = await res.json();
    expect(body.code).toBe('BUSINESS_RULE');
    expect(typeof body.message).toBe('string');
    expect((body.message as string).length).toBeGreaterThan(0);
    expect(typeof body.correlationId).toBe('string');
  });

  test('doctor deletes an open day (204)', async ({ request }) => {
    const token = await login(request, 'doctor1', 'doctor123');
    const res = await request.delete(
      `${API}/prescriptions/items/${MORFIN_ITEM_ID}/days/${MORFIN_OPEN_DAY_ID}`,
      { headers: { Authorization: `Bearer ${token}` } },
    );
    expect(res.status()).toBe(204);
  });
});
