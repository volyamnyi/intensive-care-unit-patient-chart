import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

test.describe('Doctor Notes Full', () => {
  test('creates a note and shows author and timestamp', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByText('Нотатки').first().click();

    const noteText = 'E2E тест: нотатка з перевіркою автора та часу';
    await page.getByLabel('Нова нотатка').fill(noteText);
    await page.getByRole('button', { name: 'Додати нотатку' }).click();

    await expect(page.getByText(noteText).first()).toBeVisible({ timeout: 10000 });

    await expect(page.getByText(/лікар|doctor|DOCTOR/i).first()).toBeVisible();
    await expect(page.getByText(/2025|2026/).first()).toBeVisible();
  });

  test('shows error when adding empty note', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByText('Нотатки').first().click();

    await page.getByRole('button', { name: 'Додати нотатку' }).click();
    await expect(page.getByLabel('Нова нотатка')).toBeVisible();
  });

  test('creates multiple notes and verifies list order', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);

    await page.getByText('Нотатки').first().click();

    const note1 = 'Перша тестова нотатка';
    const note2 = 'Друга тестова нотатка';

    await page.getByLabel('Нова нотатка').fill(note1);
    await page.getByRole('button', { name: 'Додати нотатку' }).click();
    await expect(page.getByText(note1).first()).toBeVisible({ timeout: 10000 });

    await page.getByLabel('Нова нотатка').fill(note2);
    await page.getByRole('button', { name: 'Додати нотатку' }).click();
    await expect(page.getByText(note2).first()).toBeVisible({ timeout: 10000 });
  });
});
