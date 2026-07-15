import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

test.describe('Doctor Notes', () => {
  test('adds a clinical note to a patient day', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByRole('tab', { name: 'Нотатки' }).click();

    const noteText = 'Тестова нотатка від лікаря — E2E перевірка';
    await page.getByLabel('Нова нотатка').fill(noteText);
    await page.getByRole('button', { name: 'Додати нотатку' }).click();

    await expect(page.getByText(noteText).first()).toBeVisible({ timeout: 10000 });
  });

  test('shows error when adding empty note', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByRole('tab', { name: 'Нотатки' }).click();

    await page.getByRole('button', { name: 'Додати нотатку' }).click();
    await expect(page.getByLabel('Нова нотатка')).toBeVisible();
  });
});
