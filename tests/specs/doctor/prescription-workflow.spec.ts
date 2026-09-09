import { test, expect } from '../../fixtures/index';
import { testUser } from '../../helpers/test-users';

const API = 'http://localhost:8085/api';

async function getToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: testUser(1).login, password: testUser(1).password },
  });
  return (await res.json()).token as string;
}

// GET /api/patients?module=medication returns only dept-19/37 patients from real MIS.
// Pick the first and reuse its numeric id to locate its exact roster row.
async function firstMedicationPatient(request: any, token: string): Promise<any> {
  const res = await request.get(`${API}/patients?module=medication`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const patients = await res.json();
  const p = patients.find((x: any) => typeof x?.fullName === 'string' && x.fullName.trim().length >= 2);
  if (!p) {
    throw new Error('No medication (dept 19/37) patient with a full name available from real MIS');
  }
  return p;
}

// The surgery|rehab toggle splits the roster client-side by departmentId (19 / 37).
const deptTabName = (departmentId: number) => (departmentId === 37 ? 'Реабілітація' : 'Хірургія');

// Locate the single roster row for a patient by its exact numeric id cell (unique).
function rowForPatient(page: any, patientId: number) {
  return page.locator('tbody tr', {
    has: page.getByRole('cell', { name: String(patientId), exact: true }),
  });
}

// Guarantee the patient has at least one prescription list (the drawer opens only on existing lists).
async function ensurePrescriptionList(request: any, token: string, patientId: number) {
  const res = await request.get(`${API}/prescriptions?patientId=${patientId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const lists = await res.json();
  if (Array.isArray(lists) && lists.length > 0) return;
  await request.post(`${API}/prescriptions`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { patientId: String(patientId) },
  });
}

test.describe('Prescription Workflow (Doctor)', () => {
  test('navigates to prescription page and sees department toggle', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    await expect(page.getByRole('button', { name: 'Хірургія' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Реабілітація' })).toBeVisible();
    await expect(page.getByPlaceholder('Пошук пацієнта')).toBeVisible();
  });

  test('switches department and shows patients', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    await page.getByRole('button', { name: 'Реабілітація' }).click();
    await expect(page.getByRole('button', { name: 'Хірургія' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Реабілітація' })).toBeVisible();
  });

  test('dashboard page renders without errors', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    const body = page.locator('body');
    await expect(body).not.toHaveText('Error', { timeout: 10000 });
  });

  test('shows patient table after loading', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    await expect(page.getByRole('heading', { name: 'Листок лікарських призначень' })).toBeVisible();
  });

  test('creates a prescription list for a patient from the lists drawer', async ({ page, request }) => {
    const token = await getToken(request);
    const patient = await firstMedicationPatient(request, token);

    await page.goto('/prescriptions/doctor');
    await page.getByRole('button', { name: deptTabName(patient.departmentId) }).click();
    await page.getByPlaceholder('Пошук пацієнта').fill(String(patient.id));
    const row = rowForPatient(page, patient.id);
    await expect(row).toBeVisible({ timeout: 10000 });

    // The «Дії» column offers «Відкрити»; creation lives inside the drawer
    await row.getByRole('button', { name: 'Відкрити' }).click();
    await expect(page.getByText('Листки призначень (')).toBeVisible({ timeout: 10000 });

    await page.getByRole('button', { name: 'Створити листок' }).click();

    await page.waitForURL(/\/prescriptions\/doctor\/[0-9a-f-]{36}$/, { timeout: 15000 });
    await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10000 });
  });

  test('opens an existing prescription list via the drawer and navigates to details', async ({ page, request }) => {
    const token = await getToken(request);
    const patient = await firstMedicationPatient(request, token);
    await ensurePrescriptionList(request, token, patient.id);

    await page.goto('/prescriptions/doctor');
    await page.getByRole('button', { name: deptTabName(patient.departmentId) }).click();
    await page.getByPlaceholder('Пошук пацієнта').fill(String(patient.id));
    const row = rowForPatient(page, patient.id);
    await expect(row).toBeVisible({ timeout: 10000 });

    // The row shows «Відкрити»; open the drawer, then the drawer's inner «Відкрити»
    // (the last match in DOM order) navigates to the list details.
    await row.getByRole('button', { name: 'Відкрити' }).click();
    await expect(page.getByText('Листки призначень (')).toBeVisible({ timeout: 10000 });
    await page.getByRole('button', { name: 'Відкрити' }).last().click();

    await page.waitForURL(/\/prescriptions\/doctor\/[0-9a-f-]{36}$/, { timeout: 15000 });
    await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10000 });
  });
});
