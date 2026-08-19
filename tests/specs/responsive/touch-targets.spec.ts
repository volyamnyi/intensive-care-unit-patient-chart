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

    await page.goto('/icu/doctor/create-card');
    await expectTouchTarget(
      page.getByRole('button', { name: 'Створити карту' }),
      'Створити карту',
    );
  });

  test('patient-panel toggle on the episode page is at least 44px', async ({ page }) => {
    await page.goto('/icu/doctor/episode/a3333333');
    const panelButton = page.getByRole('button', { name: 'Панель пацієнта' });
    await expect(panelButton).toBeVisible();
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