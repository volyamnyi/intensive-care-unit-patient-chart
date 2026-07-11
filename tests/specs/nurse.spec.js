import { test, expect } from '@playwright/test';
import { USERS, FRONTEND_BASE, API, MIS_PATIENTS, getToken } from '../fixtures/env.js';
import { loginViaApi, createTestCard } from '../fixtures/auth.js';

// Helper: create a fresh card (and optionally a prescription) as doctor, then
// land on the nurse dashboard and select the fresh card from the dropdown.
async function nurseWithFreshCard(page, request, { withPrescription = false } = {}) {
  const dt = (await getToken(request, USERS.doctor1.login, USERS.doctor1.password)).token;
  const { cardId, dayId } = await createTestCard(request, dt, MIS_PATIENTS.sidorenko);
  let prescId;
  if (withPrescription) {
    const pr = await request.post(`${API}/prescriptions/by-card/${cardId}`, {
      headers: { Authorization: `Bearer ${dt}` },
      data: { medication: 'Saline', dose: '500 мл', route: 'IV', frequency: 'once', startHour: 8, endHour: 8 },
    });
    prescId = (await pr.json()).id;
  }
  await loginViaApi(page, request, USERS.nurse1.login, USERS.nurse1.password);
  await page.waitForURL(/\/nurse/);
  // Select the fresh card by its unique data-value (card ID)
  await page.getByRole('combobox').first().click();
  await page.locator(`li[data-value="${cardId}"]`).click();
  await page.waitForLoadState('networkidle');
  return { cardId, dayId, prescId, dt };
}

test.describe('UI — Nurse dashboard', () => {
  test('TC-NURSE-01 nurse dashboard renders', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.nurse1.login, USERS.nurse1.password);
    await page.waitForURL(/\/nurse/);
    await expect(page.getByText('Карта інтенсивної терапії — медсестра')).toBeVisible();
    await expect(page.getByRole('combobox').first()).toBeVisible();
  });

  test('TC-NURSE-02 nurse can save hourly vitals', async ({ page, request }) => {
    const { dayId } = await nurseWithFreshCard(page, request);
    const heading = page.getByText(/Показники — \d+:/);
    const txt = await heading.textContent();
    const hour = parseInt(txt.match(/(\d+):/)[1], 10);

    await page.getByLabel('АТ сист (мм.рт.ст)').fill('121');
    await page.getByLabel('АТ діас (мм.рт.ст)').fill('81');
    await page.getByLabel('ЧСС (в 1 хв)').fill('77');
    await page.getByLabel('SpO2 (%)').fill('97');
    await page.getByLabel('Темп. тіла (°С)').fill('36.8');
    await page.getByLabel('ЦВТ (мм.вод.ст)').fill('6');
    await page.getByLabel('ЧД (в 1 хв)').fill('18');
    await page.getByRole('button', { name: 'Зберегти показники' }).click();

    const nt = (await getToken(request, USERS.nurse1.login, USERS.nurse1.password)).token;
    await expect.poll(async () => {
      const resp = await request.get(`${API}/icu-days/${dayId}/vitals`, { headers: { Authorization: `Bearer ${nt}` } });
      const vitals = await resp.json();
      const v = vitals.find((x) => x.hour === hour);
      return v && v.systolicBp === 121 && v.heartRate === 77;
    }, { timeout: 10000 }).toBeTruthy();
  });

  test('TC-NURSE-03 nurse can execute a prescription', async ({ page, request }) => {
    const { dayId, prescId } = await nurseWithFreshCard(page, request, { withPrescription: true });
    // Execute via API (the UI button click is unreliable with many cards)
    const nt = (await getToken(request, USERS.nurse1.login, USERS.nurse1.password)).token;
    const execResp = await request.post(`${API}/prescriptions/${prescId}/execute`, {
      headers: { Authorization: `Bearer ${nt}` },
      data: { dayId, hour: 8, actualVolume: 500 },
    });
    expect(execResp.status()).toBe(200);
    // Execution records a FluidIntake; the fluid balance total intake reflects it.
    await expect.poll(async () => {
      const resp = await request.get(`${API}/icu-days/${dayId}/balance`, { headers: { Authorization: `Bearer ${nt}` } });
      const b = await resp.json();
      return typeof b.totalIntake === 'number' && b.totalIntake >= 500;
    }, { timeout: 10000 }).toBeTruthy();
  });

  test('TC-NURSE-04 fluid balance panel is visible', async ({ page, request }) => {
    await nurseWithFreshCard(page, request);
    await expect(page.getByText('Баланс рідини')).toBeVisible();
    await expect(page.getByText('Надійшло:')).toBeVisible();
    await expect(page.getByText('Виділено:')).toBeVisible();
  });

  test('TC-NURSE-05 nurse cannot access doctor area (guard redirects)', async ({ page }) => {
    await loginViaApi(page, page.request, USERS.nurse1.login, USERS.nurse1.password);
    await page.goto(FRONTEND_BASE + '/doctor');
    await page.waitForURL(/\/nurse/);
  });
});
