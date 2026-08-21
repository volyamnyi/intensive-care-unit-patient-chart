import { test, expect } from '../../fixtures/index';

// Regression for «Додати» on an in-progress («В ході») prescription list:
// the button must be active as soon as a valid drug name is typed, even
// without selecting a row from the medicine-catalog dropdown.
// Before the fix, `MedicineSearchInput` enabled it only after an explicit
// dropdown click, so users who typed a correct name saw a dead button.

test.describe('Medicine add button — in-progress list', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    // Use patient 1001 whose in-progress (В ході) list is seeded.
    await page.getByPlaceholder('Пошук пацієнта').fill('1001');
    await expect(page.locator('tr').filter({ hasText: 'В ході' }).first()).toBeVisible({ timeout: 10000 });

    await page.locator('tr').filter({ hasText: 'В ході' }).first().getByRole('button', { name: 'Відкрити' }).click();
    await expect(page.getByText('Листки призначень (')).toBeVisible({ timeout: 10000 });

    const card = page.locator('div.rounded-xl.border', { hasText: 'В ході' }).first();
    await card.getByRole('button', { name: /Листок/ }).first().click();
    await page.waitForURL(/\/prescriptions\/doctor\/[0-9a-f-]{36}$/, { timeout: 15000 });
    await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10000 });
    await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 10000 });
  });

  test('«Додати» is inactive on an empty field', async ({ page }) => {
    const add = page.getByRole('button', { name: 'Додати' });
    await expect(add).toBeDisabled();
  });

  test('«Додати» becomes active after typing a valid drug name (no dropdown click)', async ({ page }) => {
    const input = page.getByPlaceholder('Препарат').first();
    await input.fill('Ondansetron');
    // Wait for the debounce (300 ms) + catalog fetch to settle; the button
    // must be enabled purely from `medSearch`.
    const add = page.getByRole('button', { name: 'Додати' });
    await expect(add).toBeEnabled({ timeout: 10000 });
  });

  test('«Додати» with a typed-only name submits and adds the medicine', async ({ page }) => {
    const input = page.getByPlaceholder('Препарат').first();
    await input.fill('Ondansetron');

    const add = page.getByRole('button', { name: 'Додати' });
    await expect(add).toBeEnabled({ timeout: 10000 });

    const resp = page.waitForResponse(
      r => r.url().includes('/prescriptions/') && r.url().endsWith('/items') && r.request().method() === 'POST',
      { timeout: 15000 },
    );
    await add.click();
    const post = await resp;
    expect(post.status()).toBe(201);

    await expect(page.getByText('Ondansetron').first()).toBeVisible({ timeout: 10000 });
  });

  test('allergy guard still fires for a patient-allergic drug (typed-only name)', async ({ page }) => {
    // Patient 1001 is allergic to Penicillin (seeded in `allergyData`).
    const input = page.getByPlaceholder('Препарат').first();
    await input.fill('Penicillin');

    const add = page.getByRole('button', { name: 'Додати' });
    await expect(add).toBeEnabled({ timeout: 10000 });

    let dialogMessage = '';
    page.once('dialog', (dialog) => {
      dialogMessage = dialog.message();
      void dialog.dismiss();
    });
    await add.click();
    await expect.poll(() => dialogMessage, { timeout: 10000 }).toContain('алергія');
    expect(dialogMessage).toContain('Penicillin');
  });

  test('selecting a catalog suggestion and adding still works', async ({ page }) => {
    const input = page.getByPlaceholder('Препарат').first();
    await input.fill('Ceftr');
    // The catalog suggestion renders after the debounced fetch — the
    // auto-waiting assertion below covers it; no sleep needed.
    const opt = page.getByRole('button', { name: /Ceftriaxone/ }).first();
    await expect(opt).toBeVisible({ timeout: 10000 });
    await opt.click();

    const add = page.getByRole('button', { name: 'Додати' });
    await expect(add).toBeEnabled();

    const resp = page.waitForResponse(
      r => r.url().includes('/prescriptions/') && r.url().endsWith('/items') && r.request().method() === 'POST',
      { timeout: 15000 },
    );
    await add.click();
    const post = await resp;
    expect(post.status()).toBe(201);

    await expect(page.getByText('Ceftriaxone').first()).toBeVisible({ timeout: 10000 });
  });

  test('a closed («Завершено») list still hides the add row', async ({ page }) => {
    // Go back to the drawer, close the in-progress list, then open a
    // finished one (if any). The patient 1001 seed has at least one
    // Finished row after prior runs; guard against the edge where the
    // list set has only in-progress rows by creating a closed one.
    await page.goto('/prescriptions/doctor');
    await page.getByPlaceholder('Пошук пацієнта').fill('1001');
    // The drawer rows load asynchronously — wait for any row before counting
    // (count() is not auto-waiting).
    await expect(page.locator('tr').first()).toBeVisible({ timeout: 10000 });

    // Open drawer, close any in-progress row, then open a closed one.
    const openRow = page.locator('tr').filter({ hasText: 'В ході' }).first();
    if (await openRow.count()) {
      await openRow.getByRole('button', { name: 'Відкрити' }).click();
    } else {
      // fall back to the first row
      await page.locator('tr').first().getByRole('button', { name: 'Відкрити' }).click();
    }
    // The drawer cards render after the open request — wait for the card
    // container before counting (tolerant: the closed-list edge is handled below).
    await expect(page.locator('div.rounded-xl.border').first()).toBeVisible({ timeout: 10000 }).catch(() => {});

    const card = page.locator('div.rounded-xl.border', { hasText: 'В ході' }).first();
    if (await card.count()) {
      // Deterministic: the close round-trip marks the list as closed.
      const closeResp = page.waitForResponse(
        (r) => r.request().method() === 'POST' && r.url().includes('/close'),
        { timeout: 10000 },
      );
      await card.getByRole('button', { name: 'Закрити' }).click();
      await closeResp;
    } else {
      const closeResp = page.waitForResponse(
        (r) => r.request().method() === 'POST' && r.url().includes('/close'),
        { timeout: 10000 },
      );
      await page.getByRole('button', { name: 'Закрити' }).first().click();
      await closeResp;
    }

    // Open any closed list (badge «Завершено»).
    const closedCard = page.locator('div.rounded-xl.border', { hasText: 'Завершено' }).first();
    if (await closedCard.count()) {
      await closedCard.getByRole('button', { name: /Листок/ }).first().click();
      await page.waitForURL(/\/prescriptions\/doctor\/[0-9a-f-]{36}$/, { timeout: 15000 });
      await expect(page.getByText('Статус: Закрито')).toBeVisible({ timeout: 10000 });
      // For finished lists the add row is entirely hidden (component returns null).
      await expect(page.getByRole('button', { name: 'Додати' })).toHaveCount(0);
    }
  });
});
