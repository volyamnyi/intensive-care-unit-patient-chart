import { test, expect } from '../../fixtures/index';

test.describe('Nurse Vitals Entry', () => {
  test('enters vitals for a patient hour and saves', async ({ page }) => {
    await page.goto('/nurse/episode/a3333333-3333-3333-3333-333333333333');
    await expect(page.getByRole('tab', { name: 'Вітальні' })).toBeVisible();

    await page.getByLabel('АТ сист (мм.рт.ст)').fill('120');
    await page.getByLabel('АТ діас (мм.рт.ст)').fill('80');
    await page.getByLabel('ЧСС (в 1 хв)').fill('72');
    await page.getByLabel('SpO2 (%)').fill('98');
    await page.getByLabel('Темп. тіла (°С)').fill('36.6');

    await page.getByRole('button', { name: 'Зберегти показники' }).click();
  });

  test('vitals fields have HTML5 validation attributes', async ({ page }) => {
    await page.goto('/nurse/episode/a3333333-3333-3333-3333-333333333333');

    const sysField = page.getByLabel('АТ сист (мм.рт.ст)');
    await expect(sysField).toHaveAttribute('type', 'number');
    await expect(sysField).toHaveAttribute('min', '60');
    await expect(sysField).toHaveAttribute('max', '300');

    const hrField = page.getByLabel('ЧСС (в 1 хв)');
    await expect(hrField).toHaveAttribute('min', '20');
    await expect(hrField).toHaveAttribute('max', '300');

    const tempField = page.getByLabel('Темп. тіла (°С)');
    await expect(tempField).toHaveAttribute('min', '30');
    await expect(tempField).toHaveAttribute('max', '45');
  });
});
