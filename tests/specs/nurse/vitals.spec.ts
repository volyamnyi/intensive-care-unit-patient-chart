import { test, expect } from '../../fixtures/index';

test.describe('Nurse Vitals Entry', () => {
  test('enters and persists vitals for a patient hour', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван/ }).click();
    await expect(page.getByText('Петренко Іван Сергійович').first()).toBeVisible();
    await expect(page.getByRole('heading', { name: /Показники/ })).toBeVisible({ timeout: 10000 });

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

  test('save vitals shows success snackbar', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван/ }).click();
    await expect(page.getByText('Петренко Іван Сергійович').first()).toBeVisible();
    await expect(page.getByRole('heading', { name: /Показники/ })).toBeVisible({ timeout: 10000 });

    await page.getByText('8:00').first().click();
    await page.getByLabel('АТ сист (мм.рт.ст)').fill('110');
    await page.getByLabel('АТ діас (мм.рт.ст)').fill('70');
    await page.getByLabel('ЧСС (в 1 хв)').fill('65');

    await page.getByRole('button', { name: 'Зберегти показники' }).click();
    await expect(page.getByText('Показники збережено')).toBeVisible({ timeout: 5000 });
  });

  test('vitals fields have HTML5 validation attributes', async ({ page }) => {
    await page.goto('/nurse');
    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван/ }).click();
    await expect(page.getByLabel('АТ сист (мм.рт.ст)')).toBeVisible({ timeout: 10000 });

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
