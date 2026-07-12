import { test, expect } from '../../fixtures/index';

test.describe('Nurse Vitals Entry', () => {
  test('enters and persists vitals for a patient hour', async ({ page }) => {
    await page.goto('/nurse');
    await expect(page.getByRole('heading', { name: /Показники/ })).toBeVisible({ timeout: 10000 });

    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван/ }).click();
    await expect(page.getByText('Петренко Іван Сергійович').first()).toBeVisible();

    await page.getByText('8:00').first().click();
    await expect(page.getByText('Показники — 8:00')).toBeVisible();

    await page.getByLabel('АТ сист (мм.рт.ст)').fill('120');
    await page.getByLabel('АТ діас (мм.рт.ст)').fill('80');
    await page.getByLabel('ЧСС (в 1 хв)').fill('72');
    await page.getByLabel('SpO2 (%)').fill('98');
    await page.getByLabel('Темп. тіла (°С)').fill('36.6');
    await page.getByLabel('ЦВТ (мм.вод.ст)').fill('8');
    await page.getByLabel('ЧД (в 1 хв)').fill('16');

    await page.getByRole('button', { name: 'Зберегти показники' }).click();

    await page.getByText('9:00').first().click();
    await page.getByText('8:00').first().click();

    await expect(page.getByLabel('АТ сист (мм.рт.ст)')).toHaveValue('120', { timeout: 10000 });
    await expect(page.getByLabel('ЧСС (в 1 хв)')).toHaveValue('72');
    await expect(page.getByLabel('SpO2 (%)')).toHaveValue('98');
  });
});
