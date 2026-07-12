import { test, expect } from '../../fixtures/index';

test.describe('Doctor Notes', () => {
  test('adds a clinical note to a patient day', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: /Відкрити/ }).first().click();
    await page.getByRole('tab', { name: 'Нотатки' }).click();

    await expect(page.getByText('Немає нотаток')).toBeVisible();

    const noteText = 'Тестова нотатка від лікаря — E2E перевірка';
    await page.getByLabel('Нова нотатка').fill(noteText);
    await page.getByRole('button', { name: 'Додати нотатку' }).click();

    await expect(page.getByText(noteText)).toBeVisible({ timeout: 10000 });
  });
});
