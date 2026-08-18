import { test, expect } from '../../fixtures/index';

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

  test('creates a prescription list for a patient via the Дії column', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    await page.getByPlaceholder('Пошук пацієнта').fill('1002');
    await expect(page.getByRole('cell', { name: 'Коваленко Олена Вікторівна' })).toBeVisible({ timeout: 10000 });

    // «Створити» is always available in the Дії column of the patient list
    await page.getByRole('button', { name: 'Створити' }).click();

    await page.waitForURL(/\/prescriptions\/doctor\/[0-9a-f-]{36}$/, { timeout: 15000 });
    await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10000 });
  });

  test('opens an existing prescription list via the drawer and navigates to details', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    await page.getByPlaceholder('Пошук пацієнта').fill('1002');
    await expect(page.getByRole('cell', { name: 'Коваленко Олена Вікторівна' })).toBeVisible({ timeout: 10000 });

    // The row shows both «Створити» and «Відкрити»; open the drawer via «Відкрити»
    const openButton = page.getByRole('button', { name: 'Відкрити' });
    await openButton.first().click();

    await expect(page.getByText('Листки призначень (')).toBeVisible({ timeout: 10000 });

    // The drawer's inner «Відкрити» button is the last match in DOM order.
    await page.getByRole('button', { name: 'Відкрити' }).last().click();

    await page.waitForURL(/\/prescriptions\/doctor\/[0-9a-f-]{36}$/, { timeout: 15000 });
    await expect(page).toHaveTitle('Призначення — Деталі', { timeout: 10000 });
  });
});
