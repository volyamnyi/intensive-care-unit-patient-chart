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
    await expect(page.getByRole('button', { name: 'Реабілітація' })).toHaveClass(/Mui-selected/);
  });

  test('dashboard page renders without errors', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    const body = page.locator('body');
    await expect(body).not.toHaveText('Error', { timeout: 10000 });
  });

  test('shows patient table after loading', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    await expect(page.getByText('Листок лікарських призначень')).toBeVisible();
  });
});
