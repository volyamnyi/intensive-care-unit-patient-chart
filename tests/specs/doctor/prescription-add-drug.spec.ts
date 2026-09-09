import { test, expect } from '../../fixtures/index';
import {
  getToken,
  firstMedicationPatient,
  ensureMedicationList,
  navigateToDetail,
} from '../../helpers/medication';

// Regression for «Додати» on an in-progress («В ході») prescription list:
// the button must be active as soon as a valid drug name is typed, even
// without selecting a row from the medicine-catalog dropdown.
// Before the fix, `MedicineSearchInput` enabled it only after an explicit
// dropdown click, so users who typed a correct name saw a dead button.
//
// Dynamic-data contract (real MIS, no seed IDs): beforeEach picks the first
// dept-19/37 patient, guarantees an open list + one real-catalog item, and
// navigates to the list detail. `drugName` is a live catalog name (no
// hardcoded `Ondansetron`/`Ceftriaxone`); `partial` is its near-full prefix
// for the suggestion test.

const API = 'http://localhost:8085/api';

test.describe('Medicine add button — in-progress list', () => {
  let drugName = '';
  let partial = '';

  test.beforeEach(async ({ page, request }) => {
    const nav = await navigateToDetail(request, page, {
      base: '/prescriptions/doctor',
      expectPath: /\/prescriptions\/doctor\/[0-9a-f-]{36}$/,
    });
    drugName = nav.drugName;
    partial = nav.partial;
    await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 10000 });
  });

  test('«Додати» is inactive on an empty field', async ({ page }) => {
    const add = page.getByRole('button', { name: 'Додати', exact: true });
    await expect(add).toBeDisabled();
  });

  test('«Додати» becomes active after typing a valid drug name (no dropdown click)', async ({ page }) => {
    const input = page.getByPlaceholder('Препарат').first();
    await input.fill(drugName);
    // Wait for the debounce (300 ms) + catalog fetch to settle; the button
    // must be enabled purely from `medSearch`.
    const add = page.getByRole('button', { name: 'Додати', exact: true });
    await expect(add).toBeEnabled({ timeout: 10000 });
  });

  test('«Додати» with a typed-only name submits and adds the medicine', async ({ page }) => {
    const input = page.getByPlaceholder('Препарат').first();
    await input.fill(drugName);

    const add = page.getByRole('button', { name: 'Додати', exact: true });
    await expect(add).toBeEnabled({ timeout: 10000 });

    const resp = page.waitForResponse(
      r => r.url().includes('/prescriptions/') && r.url().endsWith('/items') && r.request().method() === 'POST',
      { timeout: 15000 },
    );
    await add.click();
    const post = await resp;
    expect(post.status()).toBe(201);

    await expect(page.getByText(drugName).first()).toBeVisible({ timeout: 10000 });
  });

  test('selecting a catalog suggestion and adding still works', async ({ page }) => {
    const input = page.getByPlaceholder('Препарат').first();
    await input.fill(partial);
    // The catalog suggestion renders after the debounced fetch — the
    // auto-waiting assertion below covers it; no sleep needed. The item
    // name cell is plain text (not a button), so this matches the dropdown
    // option only.
    const opt = page.getByRole('button', { name: drugName }).first();
    await expect(opt).toBeVisible({ timeout: 10000 });
    await opt.click();

    const add = page.getByRole('button', { name: 'Додати', exact: true });
    await expect(add).toBeEnabled();

    const resp = page.waitForResponse(
      r => r.url().includes('/prescriptions/') && r.url().endsWith('/items') && r.request().method() === 'POST',
      { timeout: 15000 },
    );
    await add.click();
    const post = await resp;
    expect(post.status()).toBe(201);

    await expect(page.getByText(drugName).first()).toBeVisible({ timeout: 10000 });
  });

  test('a closed («Завершено») list still hides the add row', async ({ page, request }) => {
    // Deterministic against real data: close the patient's open list via API,
    // then open it directly and assert the finished state hides the add row
    // (the component returns null when the list is finished).
    const token = await getToken(request, 1);
    const patient = await firstMedicationPatient(request, token);
    const listId = await ensureMedicationList(request, token, patient.id);
    const closeRes = await request.post(`${API}/prescriptions/${listId}/close`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(closeRes.ok()).toBeTruthy();

    await page.goto(`/prescriptions/doctor/${listId}`);
    await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10000 });
    await expect(page.getByText('Статус: Закрито')).toBeVisible({ timeout: 10000 });
    // For finished lists the add row is entirely hidden (component returns null).
    await expect(page.getByRole('button', { name: 'Додати', exact: true })).toHaveCount(0);
  });
});
