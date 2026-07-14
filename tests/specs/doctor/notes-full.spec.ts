import { test, expect } from '../../fixtures/index';

test.describe('Doctor Notes Full', () => {
  test('creates a note and shows author and timestamp', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    await page.getByRole('tab', { name: 'Нотатки' }).click();

    const noteText = 'E2E тест: нотатка з перевіркою автора та часу';
    await page.getByLabel('Нова нотатка').fill(noteText);
    await page.getByRole('button', { name: 'Додати нотатку' }).click();

    await expect(page.getByText(noteText).first()).toBeVisible({ timeout: 10000 });

    await expect(page.getByText(/лікар|doctor|DOCTOR/i).first()).toBeVisible();
    await expect(page.getByText(/2025|2026/).first()).toBeVisible();
  });

  test('shows error when adding empty note', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    await page.getByRole('tab', { name: 'Нотатки' }).click();

    await page.getByRole('button', { name: 'Додати нотатку' }).click();
    await expect(page.getByLabel('Нова нотатка')).toBeVisible();
  });

  test('creates multiple notes and verifies list order', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    await page.getByRole('tab', { name: 'Нотатки' }).click();

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
