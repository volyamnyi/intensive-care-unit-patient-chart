import { test, expect } from '../../fixtures/index';

test.describe('Doctor Dashboard', () => {
  test('displays active patient cards', async ({ page }) => {
    await page.goto('/doctor');
    await expect(page.getByText('Петренко Іван')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Коваленко Петро')).toBeVisible();
    await expect(page.getByText('Сидоренко Ольга')).toBeVisible();
  });

  test('opens patient day page', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('link', { name: /Відкрити/ }).first().click();
    await expect(page).toHaveURL(/\/doctor\/card\/\d+\/day\/\d+/);
  });

  test('patient day page shows vitals tab', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('link', { name: /Відкрити/ }).first().click();
    await expect(page.getByText('Вітальні показники')).toBeVisible();
  });

  test('prescriptions tab shows prescription list', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('link', { name: /Відкрити/ }).first().click();
    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByText('Призначення')).toBeVisible();
  });

  test('scales tab shows scale cards', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('link', { name: /Відкрити/ }).first().click();
    await page.getByRole('tab', { name: 'Шкали' }).click();
    await expect(page.getByText('APACHE II')).toBeVisible();
    await expect(page.getByText('SOFA')).toBeVisible();
  });

  test('sign off button signs the day', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('link', { name: /Відкрити/ }).first().click();
    await page.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(page.getByText('Підтвердити')).toBeVisible();
  });
});
