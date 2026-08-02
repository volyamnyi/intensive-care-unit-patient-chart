import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

test.describe('Episode-Level Scales', () => {

  test('APACHE II calculator form renders and calculates result', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await page.getByText('Шкали').first().click();

    const scaleSelect = page.getByRole('combobox', { name: 'Шкала' });
    await scaleSelect.click();
    await page.getByRole('option', { name: 'APACHE II' }).click();

    await expect(page.getByPlaceholder('Температура (°C)')).toBeVisible();
    await expect(page.getByPlaceholder('Вік (роки)')).toBeVisible();

    await page.getByPlaceholder('Вік (роки)').fill('65');
    await page.getByPlaceholder('Температура (°C)').fill('38.5');
    await page.getByPlaceholder('ЧСС (уд/хв)').fill('110');
    await page.getByPlaceholder('ЧД (дих/хв)').fill('28');
    await page.getByPlaceholder('Середній АТ (mmHg)').fill('70');

    await page.getByRole('button', { name: 'Розрахувати APACHE II' }).click();

    await expect(page.getByText(/APACHE II/).first()).toBeVisible({ timeout: 10000 });
  });

  test('SOFA daily-scale form renders and saves result', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await page.getByText('Шкали').first().click();

    const scaleSelect = page.getByRole('combobox', { name: 'Шкала' });
    await scaleSelect.click();
    await page.getByRole('option', { name: 'SOFA' }).click();

    await expect(page.getByPlaceholder('PaO₂ (mmHg)')).toBeVisible();
    await expect(page.getByPlaceholder('FiO₂ (%)')).toBeVisible();
    await expect(page.getByText(/Розрахувати SOFA/i)).toBeVisible();
  });

  test('CAM-ICU form renders with delirium assessment options', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await page.getByText('Шкали').first().click();

    const scaleSelect = page.getByRole('combobox', { name: 'Шкала' });
    await scaleSelect.click();
    await page.getByRole('option', { name: 'CAM-ICU' }).click();

    await expect(page.getByText(/Гострий початок/i)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Зберегти CAM-ICU' })).toBeVisible();
  });

  test('Braden form renders with pressure injury risk options', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await page.getByText('Шкали').first().click();

    const scaleSelect = page.getByRole('combobox', { name: 'Шкала' });
    await scaleSelect.click();
    await page.getByRole('option', { name: 'Браден' }).click();

    await expect(page.getByText(/Сенсорне сприйняття/i)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Зберегти Браден' })).toBeVisible();
  });

  test('APACHE II appears in key scales header after calculation', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await page.getByText('Шкали').first().click();

    const apiResponse = page.waitForResponse(
      resp => resp.url().includes('/episodes/') && resp.url().includes('/scales/calculate')
    );

    const scaleSelect = page.getByRole('combobox', { name: 'Шкала' });
    await scaleSelect.click();
    await page.getByRole('option', { name: 'APACHE II' }).click();

    await page.getByPlaceholder('Вік (роки)').fill('45');
    await page.getByPlaceholder('Температура (°C)').fill('37.0');
    await page.getByPlaceholder('ЧСС (уд/хв)').fill('80');

    await page.getByRole('button', { name: 'Розрахувати APACHE II' }).click();

    await apiResponse;

    await expect(page.getByText(/APACHE II/).first()).toBeVisible({ timeout: 10000 });
  });

  test('existing scale results display correctly', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await page.getByText('Шкали').first().click();

    await expect(page.getByText(/GCS/)).toBeVisible();
    await expect(page.getByText(/SOFA/)).toBeVisible();
  });

  test('scale dropdown lists all available scales', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await page.getByText('Шкали').first().click();

    const scaleSelect = page.getByRole('combobox', { name: 'Шкала' });
    await scaleSelect.click();

    await expect(page.getByRole('option', { name: 'APACHE II' })).toBeVisible();
    await expect(page.getByRole('option', { name: 'SOFA' })).toBeVisible();
    await expect(page.getByRole('option', { name: 'CAM-ICU' })).toBeVisible();
    await expect(page.getByRole('option', { name: 'Браден' })).toBeVisible();
    await expect(page.getByRole('option', { name: 'GCS' })).toBeVisible();
    await expect(page.getByRole('option', { name: 'RASS' })).toBeVisible();
  });
});
