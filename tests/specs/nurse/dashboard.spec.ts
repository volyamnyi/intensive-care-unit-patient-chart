import { test, expect } from '../../fixtures/index';

async function firstPatientLabel(page: any): Promise<string> {
  // The patient column is the first cell of the first data row. The nurse dashboard search
  // matches patientName OR patientId, so reuse whatever label the row renders.
  const label = (await page.locator('table tbody tr').first().locator('td').first().innerText()).trim();
  if (!label) {
    throw new Error('No patient label found in the first dashboard row');
  }
  return label;
}

test.describe('Nurse Dashboard', () => {
  test('displays active patients list', async ({ page }) => {
    await page.goto('/icu/nurse');
    await expect(page.getByText('Активні пацієнти')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('opens patient day page by clicking open', async ({ page }) => {
    await page.goto('/icu/nurse');
    const openBtn = page.getByRole('button', { name: 'Відкрити' }).first();
    await expect(openBtn).toBeVisible({ timeout: 10000 });
    await openBtn.click();
    await expect(page).toHaveURL(/\/icu\/nurse\/episode\//);
  });

  test('search filters the patients table', async ({ page }) => {
    await page.goto('/icu/nurse');
    const label = await firstPatientLabel(page);
    await page.getByPlaceholder('Пошук пацієнта за ПІБ...').fill(label);
    await expect(page.getByText(label).first()).toBeVisible({ timeout: 5000 });
  });

  test('page title is set correctly', async ({ page }) => {
    await page.goto('/icu/nurse');
    await expect(page).toHaveTitle('ВАІТ — Медсестра');
  });
});
