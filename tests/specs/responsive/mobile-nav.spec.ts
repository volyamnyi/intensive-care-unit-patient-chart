import { test, expect, type Page } from '@playwright/test';

// Responsive UI Phase 6 (issue #165): mobile navigation. The GlobalLayout
// hamburger opens the shared AppNavList inside a Sheet; links navigate and
// auto-close; Escape dismisses the sheet.

async function openNavSheet(page: Page) {
  await page.getByRole('button', { name: 'Відкрити навігацію' }).click();
  const sheet = page.getByRole('dialog', { name: 'Навігація' });
  await expect(sheet).toBeVisible();
  return sheet;
}

test.describe('mobile nav — doctor', () => {
  test('opens the sheet, switches to the medication module', async ({ page }) => {
    await page.goto('/icu/doctor');
    await expect(page.getByText('Активні пацієнти')).toBeVisible();

    const sheet = await openNavSheet(page);
    await sheet.getByRole('link', { name: 'Листок лікарських призначень' }).click();

    await expect(page).toHaveURL('/prescriptions/doctor');
    await expect(page.getByRole('dialog', { name: 'Навігація' })).toBeHidden();
  });

  test('Escape closes the nav sheet', async ({ page }) => {
    await page.goto('/icu/doctor');
    await openNavSheet(page);
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog', { name: 'Навігація' })).toBeHidden();
  });
});

test.describe('mobile nav — nurse', () => {
  test.use({ storageState: '.auth/nurse.json' });

  test('switches to the medication module', async ({ page }) => {
    await page.goto('/icu/nurse');
    await expect(page.getByText('Активні пацієнти')).toBeVisible();

    const sheet = await openNavSheet(page);
    await sheet.getByRole('link', { name: 'Листок лікарських призначень' }).click();

    await expect(page).toHaveURL('/prescriptions/nurse');
    await expect(page.getByRole('dialog', { name: 'Навігація' })).toBeHidden();
  });
});

test.describe('mobile nav — prosthetist', () => {
  test.use({ storageState: '.auth/prosthetist.json' });

  test('opens the sheet and returns to the module selector', async ({ page }) => {
    await page.goto('/prosthetics');
    await expect(page.getByText('Новий процес')).toBeVisible();

    const sheet = await openNavSheet(page);
    await expect(sheet.getByRole('link', { name: 'Виробництво протезів' })).toBeVisible();
    await sheet.getByRole('link', { name: 'Модулі' }).click();

    await expect(page).toHaveURL('/select');
  });
});