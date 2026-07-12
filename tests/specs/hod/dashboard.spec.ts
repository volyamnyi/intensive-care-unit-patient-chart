import { test, expect } from '../../fixtures/index';

test.describe('HOD Dashboard', () => {
  test('displays all active patient cards', async ({ page }) => {
    await page.goto('/doctor');
    await expect(page.getByText('Петренко Іван Сергійович')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Коваленко Олена Вікторівна')).toBeVisible();
    await expect(page.getByText('Сидоренко Василь Петрович')).toBeVisible();
  });

  test('can open seed patient day page', async ({ page }) => {
    await page.goto('/doctor');
    const row = page.getByRole('row').filter({ hasText: 'Петренко' });
    await row.getByRole('button', { name: /Відкрити/ }).click();
    await expect(page).toHaveURL(/\/doctor\/card\/\d+\/day\/\d+/);
  });

  test('can view prescriptions tab on seed card', async ({ page }) => {
    await page.goto('/doctor');
    const row = page.getByRole('row').filter({ hasText: 'Петренко' });
    await row.getByRole('button', { name: /Відкрити/ }).click();
    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByText('Нове призначення')).toBeVisible();
  });

  test('can view scales tab on seed card', async ({ page }) => {
    await page.goto('/doctor');
    const row = page.getByRole('row').filter({ hasText: 'Петренко' });
    await row.getByRole('button', { name: /Відкрити/ }).click();
    await page.getByRole('tab', { name: 'Шкали' }).click();
    await expect(page.getByText('APACHE II').first()).toBeVisible();
    await expect(page.getByText('SOFA').first()).toBeVisible();
  });

  test('can create a new ICU card', async ({ page }) => {
    await page.goto('/doctor/create-card');
    await page.getByRole('combobox').click();
    await page.getByRole('combobox').fill('Бондаренко');

    const option = page.getByRole('option', { name: /Бондаренко/ });
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();

    await page.getByLabel('Діагноз').fill('HOD тест');
    await page.getByLabel('APACHE II').fill('10');
    await page.getByLabel('SOFA').fill('3');

    await page.getByRole('button', { name: 'Створити карту' }).click();
    await expect(page).toHaveURL(/\/doctor\/card\/\d+\/day\/\d+/);
  });

  test('can sign off a patient day', async ({ page }) => {
    await page.goto('/doctor');
    const row = page.getByRole('row').filter({ hasText: 'Коваленко' });
    await row.getByRole('button', { name: /Відкрити/ }).click();
    await page.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(page.getByText('Після підписання доба стане read-only')).toBeVisible();
  });
});
