import { test, expect } from '../../fixtures/index';

// Phase 3 tablet forms & dialogs (issue #177).
// Runs in responsive-tablet-chromium (768×1024, hasTouch, doctor storageState).
// Episode a3333333 (Сидоренко) is used because its only clinical day b3333333
// stays OPEN across the whole suite — the sidebar forms are editable without
// any day selection or signing.

test.describe.configure({ mode: 'serial' });

const EPISODE_URL = '/icu/doctor/episode/a3333333-3333-3333-3333-333333333333';

async function docWidth(page: import('@playwright/test').Page): Promise<number> {
  return page.evaluate(() => document.documentElement.scrollWidth);
}

test.describe('Tablet forms & dialogs at 768', () => {
  test('episode page shows inline patient sidebar (no sheet trigger)', async ({ page }) => {
    await page.goto(EPISODE_URL);
    await page.waitForLoadState('networkidle');

    expect(await docWidth(page)).toBeLessThanOrEqual(768);

    // Inline sidebar wrapper (hidden md:block) is visible at 768 and hosts the sidebar
    const sidebar = page.locator('div.hidden.md\\:block', {
      has: page.getByRole('combobox', { name: 'Шкала' }),
    });
    await expect(sidebar).toBeVisible({ timeout: 10000 });

    // The mobile-only sheet trigger (md:hidden, aria-label) stays hidden
    await expect(page.getByRole('button', { name: 'Панель пацієнта' })).toBeHidden();

    // The scale picker lives inside the inline sidebar
    await expect(page.getByRole('combobox', { name: 'Шкала' })).toBeVisible({ timeout: 10000 });
  });

  test('SOFA form opens at 768 with md 3-column tier', async ({ page }) => {
    await page.goto(EPISODE_URL);
    await page.waitForLoadState('networkidle');

    const scaleSelect = page.getByRole('combobox', { name: 'Шкала' });
    await scaleSelect.click({ timeout: 10000 });
    await page.getByRole('option', { name: 'SOFA' }).click();

    await expect(page.getByText('SOFA — параметри')).toBeVisible({ timeout: 10000 });

    // At md:grid-cols-3 items 1–2 share row 1; item 4 wraps to row 2
    const paO2 = page.getByPlaceholder('PaO₂ (mmHg)');
    const fio2 = page.getByPlaceholder('FiO₂ (%)');
    const platelets = page.getByPlaceholder('Тромбоцити (×10⁹/л)');
    await expect(paO2).toBeVisible();
    await expect(fio2).toBeVisible();
    await expect(platelets).toBeVisible();

    const yPaO2 = (await paO2.boundingBox())?.y ?? -1;
    const yFio2 = (await fio2.boundingBox())?.y ?? -1;
    const yPlatelets = (await platelets.boundingBox())?.y ?? -1;
    expect(Math.abs(yPaO2 - yFio2)).toBeLessThanOrEqual(2);
    expect(yPlatelets).toBeGreaterThan(yPaO2 + 2);

    // Touch sizing class contract survives rendering
    await expect(paO2).toHaveClass(/pointer-coarse:min-h-11/);

    // Filling one scoreable field enables the calculate CTA
    await paO2.fill('80');
    const calcButton = page.getByRole('button', { name: 'Розрахувати SOFA' });
    await expect(calcButton).toBeEnabled();

    await calcButton.click();
    const sofaCard = page
      .locator('div.rounded-xl', { has: page.locator('p', { hasText: /^SOFA\b/ }) })
      .first();
    await expect(sofaCard).toContainText('Результат:', { timeout: 10000 });
  });

  test('patient state fields pair into an md 2-column grid', async ({ page }) => {
    await page.goto(EPISODE_URL);
    await page.waitForLoadState('networkidle');

    // b3333333 is OPEN → the entry form renders unlocked
    const hourInput = page.getByPlaceholder('Година');
    await expect(hourInput).toBeVisible({ timeout: 10000 });

    const consciousness = page.getByLabel('Свідомість');
    const skin = page.getByLabel('Шкіра');
    const edema = page.getByLabel('Набряки');
    await expect(consciousness).toBeVisible();
    await expect(skin).toBeVisible();
    await expect(edema).toBeVisible();

    // At md:grid-cols-2 fields 1–2 share row 1; field 3 wraps to row 2
    const yConsciousness = (await consciousness.boundingBox())?.y ?? -1;
    const ySkin = (await skin.boundingBox())?.y ?? -1;
    const yEdema = (await edema.boundingBox())?.y ?? -1;
    expect(Math.abs(yConsciousness - ySkin)).toBeLessThanOrEqual(2);
    expect(yEdema).toBeGreaterThan(ySkin + 2);

    await expect(consciousness).toHaveClass(/pointer-coarse:min-h-11/);
  });

  test('prescription delete dialog caps at md max-width (448px)', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    await page.waitForLoadState('networkidle');

    await page.getByPlaceholder('Пошук пацієнта').fill('1002');
    await expect(page.getByRole('cell', { name: 'Коваленко Олена Вікторівна' })).toBeVisible({
      timeout: 10000,
    });
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page.getByText(/Листки призначен/)).toBeVisible({ timeout: 10000 });

    await page.getByRole('button', { name: 'Видалити' }).first().click();
    const dialog = page.getByRole('dialog', { name: 'Видалити листок?' });
    await expect(dialog).toBeVisible({ timeout: 10000 });

    // Old policy capped at sm:max-w-sm (384px); Phase 3 raises it to md:max-w-md (448px)
    const width = (await dialog.boundingBox())?.width ?? 0;
    expect(width).toBeGreaterThan(384);
    expect(width).toBeLessThanOrEqual(450);

    await dialog.getByRole('button', { name: 'Скасувати' }).click();
    await expect(dialog).toBeHidden({ timeout: 5000 });
  });
});
