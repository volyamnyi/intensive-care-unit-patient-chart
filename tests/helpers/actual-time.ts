import type { APIRequestContext } from '@playwright/test';
import { expect } from '@playwright/test';

import { adminUser, nurse2, testUser } from './test-users';

const API = 'http://localhost:8085/api';

/**
 * Shared setup for the actual-time E2E specs (#443/#446).
 *
 * The flow is doctor-planned → nurse-executed, so the helpers mirror the
 * domain rules: planning needs PRESCRIPTION_CREATE (doctor), execution needs
 * PRESCRIPTION_EXECUTE plus a second-person confirmation by ANOTHER nurse.
 * No real MIS is involved - the patient comes from the MIS stub roster.
 */

/** Permission required to correct an already recorded actual time (#444). */
export const ACTUAL_TIME_EDIT_PERMISSION = 'PRESCRIPTION_ACTUAL_TIME_EDIT';

/** Log in and return the bearer token of the given credential slot. */
export async function loginToken(
  request: APIRequestContext,
  slot: number,
): Promise<string> {
  const { login, password } = testUser(slot);
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok(), `failed to log in as slot ${slot}: ${res.status()}`).toBeTruthy();
  return (await res.json()).token as string;
}

/**
 * Make sure a role holds a permission (idempotent grant through the admin API).
 *
 * The default matrix grants PRESCRIPTION_ACTUAL_TIME_EDIT to DOCTOR and
 * HEAD_OF_DEPARTMENT, but `PermissionService.seedIfEmpty()` only seeds a FRESH
 * database: a long-lived dev database whose `role_permissions` table was
 * populated before the permission existed keeps the old matrix. The specs
 * therefore grant the code through the admin API - the sanctioned path for an
 * existing database (no SQL backfill).
 *
 * The grant is deliberately NOT undone afterwards: the actual-time specs run
 * in different Playwright projects (api / doctor) and therefore in parallel,
 * so a spec that revoked the grant while another one was using it would make
 * the suite flaky. Leaving the grant in place converges a stale dev database
 * to the documented default matrix instead.
 */
export async function grantRolePermission(
  request: APIRequestContext,
  role: string,
  permissionCode: string,
): Promise<void> {
  await setRolePermission(request, role, permissionCode, true);
}

/** Grant or revoke a permission for a role through the admin API. */
export async function setRolePermission(
  request: APIRequestContext,
  role: string,
  permissionCode: string,
  granted: boolean,
): Promise<void> {
  const admin = adminUser();
  const loginRes = await request.post(`${API}/auth/login`, {
    data: { login: admin.login, password: admin.password },
  });
  expect(loginRes.ok(), `failed to log in as admin: ${loginRes.status()}`).toBeTruthy();
  const headers = { Authorization: `Bearer ${(await loginRes.json()).token}` };

  const res = await request.put(`${API}/admin/permissions`, {
    headers,
    data: { role, permissionCode, granted },
  });
  expect(res.ok(), `failed to set ${role}/${permissionCode}=${granted}: ${res.status()}`).toBeTruthy();
}

/** Local calendar date (yyyy-MM-dd) - the domain stores naive local dates. */
export function localDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Local wall-clock time (HH:mm:ss). */
export function localTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

/** Display format of the grid tooltip (dd.MM.yyyy HH:mm:ss). */
export function displayActualTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()} ${localTime(d)}`;
}

/**
 * A timestamp in the past that is ALWAYS inside the list period (#447).
 *
 * The specs record "N minutes ago", but in the first minutes after midnight
 * that value would fall on the PREVIOUS day - before the first day of the
 * item - and the server would reject it with the V3 error
 * («Фактичний час поза межами періоду листка»). The helper clamps the value
 * into the first minute of today: always in the past, always inside the
 * period. `floorOffsetSeconds` keeps two different offsets apart when the
 * clamp is active, so a "correction" still changes the recorded fact.
 */
export function safePastDate(
  minutesAgo: number,
  floorOffsetSeconds = 0,
  now: Date = new Date(),
): Date {
  const candidate = new Date(now.getTime() - minutesAgo * 60_000);
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
  if (candidate.getTime() >= startOfToday.getTime()) return candidate;
  const floor = new Date(startOfToday.getTime() + floorOffsetSeconds * 1000);
  return floor.getTime() <= now.getTime() ? floor : startOfToday;
}

/** ISO local wire value of {@link safePastDate} ("yyyy-MM-dd'T'HH:mm:ss"). */
export function safePastIso(
  minutesAgo: number,
  floorOffsetSeconds = 0,
  now: Date = new Date(),
): string {
  const d = safePastDate(minutesAgo, floorOffsetSeconds, now);
  return `${localDate(d)}T${localTime(d)}`;
}

/**
 * Add a uniquely named medicine item and return the day part of TODAY's
 * morning slot - the cell the specs interact with. The name keeps the row
 * isolated from the other medication specs that share the stub patient.
 */
export async function ensureActualTimeItem(
  request: APIRequestContext,
  token: string,
  listId: string,
  medicineName: string,
): Promise<{ itemId: string; dayPartId: string; dayDate: string }> {
  const headers = { Authorization: `Bearer ${token}` };
  const itemsRes = await request.get(`${API}/prescriptions/${listId}/items`, { headers });
  const raw = await itemsRes.json();
  const items = Array.isArray(raw) ? raw : [];
  for (const it of items.filter((i: any) => i?.medicineName === medicineName)) {
    await request.delete(`${API}/prescriptions/items/${it.id}`, { headers });
  }

  const addRes = await request.post(`${API}/prescriptions/${listId}/items`, {
    headers,
    data: { medicineName },
  });
  expect(addRes.ok(), `failed to add item ${medicineName}: ${addRes.status()}`).toBeTruthy();
  const itemId = (await addRes.json()).id as string;

  const refreshed = await request.get(`${API}/prescriptions/${listId}/items`, { headers });
  const refreshedItems = await refreshed.json();
  const item = (Array.isArray(refreshedItems) ? refreshedItems : []).find((i: any) => i?.id === itemId);
  const parts: any[] = item?.dayParts ?? [];
  const today = localDate(new Date());
  // Today's morning slot - but a run that crosses midnight right here would
  // leave the item (created a second ago) on the previous day. The first
  // morning slot is then the right cell (#447).
  const part =
    parts.find((p: any) => p?.dayDate === today && p?.period === 'morning') ??
    parts.find((p: any) => p?.period === 'morning');
  expect(part, `no morning day part for ${today} in item ${itemId}`).toBeTruthy();
  return { itemId, dayPartId: part.id as string, dayDate: part.dayDate as string };
}

/** Plan a dose for a day part (doctor / HOD only). */
export async function planDose(
  request: APIRequestContext,
  token: string,
  dayPartId: string,
  dose: string,
): Promise<void> {
  const res = await request.put(`${API}/prescriptions/day-parts/${dayPartId}/plan`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { dose },
  });
  expect(res.ok(), `failed to plan dose: ${res.status()}`).toBeTruthy();
}

/**
 * Execute a planned dose through the two-person path. `token` must belong to
 * the nurse who executes; the second person is another nurse (slot 4).
 */
export async function executeDose(
  request: APIRequestContext,
  token: string,
  dayPartId: string,
  body: { actualDose: string; administeredAt?: string; dispensedAt?: string },
): Promise<void> {
  const second = nurse2();
  const res = await request.post(`${API}/prescriptions/day-parts/${dayPartId}/execute`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      actualDose: body.actualDose,
      secondPersonLogin: second.login,
      secondPersonPassword: second.password,
      administeredAt: body.administeredAt,
      dispensedAt: body.dispensedAt,
    },
  });
  expect(res.ok(), `failed to execute dose: ${res.status()} ${await res.text()}`).toBeTruthy();
}

/** The day part of today's morning slot for an item (read-only lookup). */
export async function findTodayMorningPart(
  request: APIRequestContext,
  token: string,
  listId: string,
  itemId: string,
): Promise<any> {
  const res = await request.get(`${API}/prescriptions/${listId}/items`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const items = await res.json();
  const item = (Array.isArray(items) ? items : []).find((i: any) => i?.id === itemId);
  const parts: any[] = item?.dayParts ?? [];
  const today = localDate(new Date());
  // Today's morning slot; a run that crossed midnight falls back to the
  // executed cell - the executed dose is the one the specs care about (#447).
  return (
    parts.find((p: any) => p?.dayDate === today && p?.period === 'morning') ??
    parts.find((p: any) => p?.executionId) ??
    parts.find((p: any) => p?.period === 'morning')
  );
}
