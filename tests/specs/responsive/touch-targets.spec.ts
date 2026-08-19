import { test, expect, type Locator } from '@playwright/test';

// Responsive UI Phase 6 (issue #165): on touch (coarse-pointer) contexts every
// primary CTA must be at least 44x44 px — enforced via pointer-coarse:min-h-11 /
// pointer-coarse:size-11 on ui Button and explicit min-h-[44px] elsewhere.

async function expectTouchTarget(locator: Locator, label: string) {
  const box = await locator.boundingBox();
  expect(box, `${label}: element has no bounding box`).not.toBeNull();
  expect(box!.width, `${label}: width ${box!.width}px < 44`).toBeGreaterThanOrEqual(44);
  expect(box!.height, `${label}: height ${box!.height}px < 44`).toBeGreaterThanOrEqual(44);
}

test.describe('touch targets — doctor', () => {
  test('dashboard and create-card CTAs are at least 44px', async ({ page }) => {
    await page.goto('/icu/doctor');
    await expectTouchTarget(page.getByRole('button', { name: 'Нова карта' }), 'Нова карта');

    // The create-card form (and its submit button) renders after a patient is
    // picked; Ткачук has no seeded episode, so the form stays unobstructed.
    await page.goto('/icu/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або № медкарти').fill('Ткачук');
    const option = page.getByText('Ткачук Андрій Вікторович');
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();
    await expect(
      page.getByRole('button', { name: 'Створити карту' }),
      'create-card form did not render after patient selection',
    ).toBeVisible();
    await expectTouchTarget(
      page.getByRole('button', { name: 'Створити карту' }),
      'Створити карту',
    );
  });

  test('patient-panel toggle on the episode page is at least 44px', async ({ page }) => {
    await page.goto('/icu/doctor/episode/a3333333-3333-3333-3333-333333333333');
    const panelButton = page.getByRole('button', { name: 'Панель пацієнта' });
    await expect(panelButton).toBeVisible({ timeout: 15000 });
    await expectTouchTarget(panelButton, 'Панель пацієнта');
  });
});

test.describe('touch targets — nurse', () => {
  test.use({ storageState: '.auth/nurse.json' });

  test('episode row action is at least 44px', async ({ page }) => {
    await page.goto('/icu/nurse');
    const open = page.getByRole('button', { name: 'Відкрити' }).first();
    await expect(open).toBeVisible();
    await expectTouchTarget(open, 'Відкрити');
  });
});

test.describe('touch targets — prosthetist', () => {
  test.use({ storageState: '.auth/prosthetist.json' });

  test('dashboard and setup CTAs are at least 44px', async ({ page }) => {
    await page.goto('/prosthetics');
    await expectTouchTarget(page.getByRole('button', { name: 'Новий процес' }), 'Новий процес');

    await page.goto('/prosthetics/new/select-patient');
    await expectTouchTarget(page.getByRole('button', { name: 'Далі' }), 'Далі');
  });
});