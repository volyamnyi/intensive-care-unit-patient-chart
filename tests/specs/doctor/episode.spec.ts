import { test, expect } from '../../fixtures/index';

test.describe('Episode Page', () => {
  test('shows all 5 tabs on the episode page', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    await expect(page.getByRole('tab', { name: 'Вітальні' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Призначення' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Шкали' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Нотатки' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Баланс' })).toBeVisible();
  });

  test('switching tabs shows different content', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    await page.getByRole('tab', { name: 'Призначення' }).click();
    await expect(page.getByRole('button', { name: '+ Нове призначення' })).toBeVisible();

    await page.getByRole('tab', { name: 'Нотатки' }).click();
    await expect(page.getByLabel('Нова нотатка')).toBeVisible();

    await page.getByRole('tab', { name: 'Баланс' }).click();
    await expect(page.getByRole('heading', { name: 'Баланс рідини' })).toBeVisible();
  });

  test('back button returns to doctor dashboard', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    await page.getByRole('button', { name: 'Назад' }).click();
    await expect(page).toHaveURL('/doctor');
  });
});
