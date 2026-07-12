import { test, expect } from '../../fixtures/index';

test.describe('Nurse Dashboard', () => {
  test('displays patient selector', async ({ page }) => {
    await page.goto('/nurse');
    await expect(page.getByRole('combobox', { name: /Пацієнт/ })).toBeVisible({ timeout: 10000 });
  });

  test('hour selector shows 24 tiles', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('combobox', { name: /Пацієнт/ }).click();
    await expect(page.getByRole('option', { name: /Петренко/ })).toBeVisible({ timeout: 10000 });
  });

  test('can select patient from dropdown', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('combobox', { name: /Пацієнт/ }).click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    await expect(page.getByText('Петренко Іван Сергійович')).toBeVisible({ timeout: 10000 });
  });

  test('vital signs form is visible when patient selected', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('combobox', { name: /Пацієнт/ }).click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    await expect(page.getByText('АТ сист')).toBeVisible({ timeout: 10000 });
  });

  test('fluid output form is visible', async ({ page }) => {
    await page.goto('/nurse');
    await page.getByRole('combobox', { name: /Пацієнт/ }).click();
    await page.getByRole('option', { name: /Петренко Іван Сергійович/ }).click();
    await expect(page.getByText('Сеча')).toBeVisible({ timeout: 10000 });
  });
});
