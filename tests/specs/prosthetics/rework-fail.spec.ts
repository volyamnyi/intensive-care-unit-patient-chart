import { test, expect } from '@playwright/test';

test.describe('Prosthetics Rework/Fail Flow', () => {
  test.describe('Prosthetist + Admin roles', () => {
    test.use({ storageState: '.auth/prosthetist.json' });

    test('prosthetist can access setup wizard for new instance', async ({ page }) => {
      await page.goto('/prosthetics');
      await page.getByRole('button', { name: 'Новий процес' }).click();
      await page.waitForURL(/\/prosthetics\/new\/select-patient/);

      await page.getByPlaceholder("Пошук пацієнта за ПІБ або номером координати...").fill('Сніжко');
      await expect(page.getByText('Сніжко Оксана Володимирівна')).toBeVisible();

      await page.getByRole('button', { name: 'Обрати' }).first().click();
      await page.waitForURL(/\/prosthetics\/new\/select-order/);

      await expect(page.getByText('ПВ-26-0413')).toBeVisible();
      await expect(page.getByText('протез передпліччя')).toBeVisible();
    });
  });

  test.describe('Admin access', () => {
    test.use({ storageState: '.auth/prosthetadmin.json' });

    test('prosthetics_admin can access dashboard', async ({ page }) => {
      await page.goto('/prosthetics');
      await page.waitForURL(/\/prosthetics/);

      await expect(page.getByRole('heading', { name: 'Виробництво протезів' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Новий процес' })).toBeVisible();
    });

    test('prosthetics_admin can access setup wizard', async ({ page }) => {
      await page.goto('/prosthetics');
      await page.getByRole('button', { name: 'Новий процес' }).click();
      await page.waitForURL(/\/prosthetics\/new\/select-patient/);

      await page.getByPlaceholder("Пошук пацієнта за ПІБ або номером координати...").fill('Сніжко');
      await expect(page.getByText('Сніжко Оксана Володимирівна')).toBeVisible();
    });
  });
});