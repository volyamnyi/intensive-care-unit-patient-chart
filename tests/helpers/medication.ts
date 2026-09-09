import type { APIRequestContext, Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';
import { testUser } from './test-users';

// Shared helpers for medication-sheet specs that work against the real MIS
// roster (dept-19/37 patients) rather than fixed seed IDs. Credentials read
// dynamically from APP_TEST_USERNAME1..9 / APP_TEST_PASSWORD1..9 via
// `test-users`; no hardcoded user names.
//
// Reference pattern: specs/doctor/prescription-workflow.spec.ts.

const API = 'http://localhost:8085/api';

const SEARCH_PHRASE = 'Пошук пацієнта';
const LIST_HEADER = 'Листки призначень (';

export async function getToken(request: APIRequestContext, slot = 1): Promise<string> {
  const { login, password } = testUser(slot);
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

// GET /api/patients?module=medication returns only dept-19/37 patients from real MIS.
export async function firstMedicationPatient(request: APIRequestContext, token: string): Promise<any> {
  const res = await request.get(`${API}/patients?module=medication`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const patients = await res.json();
  const p = Array.isArray(patients)
    ? patients.find((x: any) => typeof x?.fullName === 'string' && x.fullName.trim().length >= 2)
    : undefined;
  if (!p) throw new Error('No medication (dept 19/37) patient with a full name available from real MIS');
  return p;
}

export function deptTabName(departmentId: number | null | undefined): string {
  return departmentId === 37 ? 'Реабілітація' : 'Хірургія';
}

export function rowForPatient(page: Page, patientId: number): Locator {
  return page.locator('tbody tr', {
    has: page.getByRole('cell', { name: String(patientId), exact: true }),
  });
}

export async function expectRowVisible(page: Page, patientId: number): Promise<void> {
  await expect(rowForPatient(page, patientId)).toBeVisible({ timeout: 10_000 });
}

// Guarantee the patient has at least one prescription list; returns the open
// (non-Finished) list if one exists, else the first, else creates one.
export async function ensureMedicationList(
  request: APIRequestContext,
  token: string,
  patientId: number,
): Promise<string> {
  const res = await request.get(`${API}/prescriptions?patientId=${patientId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const lists = await res.json();
  if (Array.isArray(lists) && lists.length > 0) {
    const open = lists.find((l: any) => l.status !== 'Finished') ?? lists[0];
    return open.id;
  }
  const cr = await request.post(`${API}/prescriptions`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { patientId: String(patientId) },
  });
  if (!cr.ok()) throw new Error(`failed to create prescription list for ${patientId}: status ${cr.status()}`);
  return (await cr.json()).id;
}

// Ensure the list holds exactly one item with the given medicine name (deleting
// any pre-existing rows of that name first), returning the new item id.
export async function ensureMedicationItem(
  request: APIRequestContext,
  token: string,
  listId: string,
  medicineName: string,
): Promise<string> {
  const itemsRes = await request.get(`${API}/prescriptions/${listId}/items`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const raw = await itemsRes.json();
  const items = Array.isArray(raw) ? raw : [];
  for (const it of items.filter((i: any) => i?.medicineName === medicineName)) {
    await request.delete(`${API}/prescriptions/items/${it.id}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
  const addRes = await request.post(`${API}/prescriptions/${listId}/items`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { medicineName },
  });
  if (!addRes.ok()) throw new Error(`failed to add medicine item: status ${addRes.status()}`);
  return (await addRes.json()).id;
}

// Fetch a single real-catalog medicine name (resilient to arbitrary MIS data).
export async function firstCatalogMedicine(request: APIRequestContext, token: string): Promise<string> {
  const res = await request.get(`${API}/prescriptions/medicine-catalog`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const catalog = await res.json();
  const med = Array.isArray(catalog)
    ? catalog.find((m: any) => typeof m?.name === 'string' && m.name.trim().length >= 2)
    : undefined;
  if (!med) throw new Error('No medicine with a name available in the MIS catalog');
  return med.name;
}

export interface NavOpts {
  base: string;
  expectPath: RegExp;
}

export interface NavResult {
  listId: string;
  drugName: string;
  // A near-full prefix of the real-catalog name whose search returns the
  // exact entry in the dropdown (for the "select a suggestion" test).
  partial: string;
}

// Authenticate as the doctor test account, pick the first dept-19/37 patient,
// guarantee an open list + a real-catalog item, then navigate to the detail
// page. Returns { listId, drugName, partial } for the spec to use.
export async function navigateToDetail(
  request: APIRequestContext,
  page: Page,
  opts: NavOpts,
): Promise<NavResult> {
  const token = await getToken(request, 1);
  const patient = await firstMedicationPatient(request, token);
  const listId = await ensureMedicationList(request, token, patient.id);
  const full = await firstCatalogMedicine(request, token);
  const partial = full.length > 2 ? full.slice(0, full.length - 1) : full;
  await ensureMedicationItem(request, token, listId, full);

  await page.goto(opts.base, { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: deptTabName(patient.departmentId) }).click();
  const search = page.getByPlaceholder(SEARCH_PHRASE);
  await search.fill(String(patient.id));
  await expectRowVisible(page, patient.id);
  await rowForPatient(page, patient.id).getByRole('button', { name: 'Відкрити' }).click();
  await expect(page.getByText(LIST_HEADER).first()).toBeVisible({ timeout: 10_000 });
  // The drawer cards render after the open request — wait for any card.
  await expect(page.locator('div.rounded-xl.border').first()).toBeVisible({ timeout: 10_000 }).catch(() => {});
  // The drawer card is «В ході» for an open (non-Finished) list, «Завершено»
  // once closed; fall back to the first card if none shows as in-progress.
  const openCard = page.locator('div.rounded-xl.border', { hasText: 'В ході' }).first();
  const target = (await openCard.count()) ? openCard : page.locator('div.rounded-xl.border').first();
  await target.getByRole('button', { name: /Листок/ }).first().click();
  await page.waitForURL(opts.expectPath, { timeout: 15_000 });
  await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10_000 });
  return { listId, drugName: full, partial };
}
