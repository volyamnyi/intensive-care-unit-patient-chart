import { test, expect } from '../../fixtures/index';

test.describe('HOD Dashboard', () => {
  test('displays active patients list', async ({ page }) => {
    await page.goto('/doctor');
    await expect(page.getByText('Активні пацієнти ВАІТ')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('can create a new ICU card', async ({ page }) => {
    await page.goto('/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або № медкарти').fill('Бондаренко');

    const option = page.getByRole('option', { name: /Бондаренко/ });
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();

    await expect(page.getByText('Дані пацієнта (з МІС)')).toBeVisible();
    await page.getByRole('button', { name: 'Створити карту' }).click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);
  });

  test('can view prescriptions tab on episode page', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByRole('button', { name: '+ Нове призначення' })).toBeVisible();
  });

  test('can view scales tab', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await page.getByRole('tab', { name: 'Шкали' }).click();
  });
});
