import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';
const DAY_ID = 'b3333333-3333-3333-3333-333333333333';

// Допоміжна функція: локальний datetime у форматі datetime-local (YYYY-MM-DDTHH:mm)
function pad(n: number): string {
  return String(n).padStart(2, '0');
}

function startAt(offsetHours: number): string {
  const dt = new Date(Date.now() + offsetHours * 3600_000);
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}T${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
}

test.describe('Doctor day flow', () => {
  test('opens the patient day with redirect and sees all sections', async ({ page }) => {
    // Лікар відкриває сторінку клінічного дня епізоду Сидоренка (seed a3333333)
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    // Очікуваний редірект на маршрут під-застосунку призначень
    await expect(page).toHaveURL(/\/icu\/doctor\/episode\//);

    // Єдина ICU-картка: сітка показників/втрат + терапія
    await expect(page.getByText('Показник / година')).toBeVisible();
    await expect(page.getByText('Терапія (призначення)')).toBeVisible();
    await expect(page.getByText('Glucose 5%')).toBeVisible();

    // Бічна панель: усі секції негодинних даних
    await expect(page.getByText('Нотатки')).toBeVisible();
    await expect(page.getByText('Шкали')).toBeVisible();
    await expect(page.getByText('Баланс рідини')).toBeVisible();
  });

  test('shows seeded vital values in the hourly grid', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    await expect(page).toHaveURL(/\/icu\/doctor\/episode\//);

    // Година 0 — seed-запис c3333001 (Темп 37.2, ЧСС 78, АТсист 122, АТдіас 68, SpO2 98.0, сеча 20)
    await expect(page.getByLabel('АТсист 0:00')).toHaveValue('122');
    await expect(page.getByLabel('Темп 0:00')).toHaveValue('37.2');
    await expect(page.getByLabel('SpO₂ 0:00')).toHaveValue('98');
    await expect(page.getByLabel('Сеча 0:00')).toHaveValue('20');

    // Година 2 — seed-запис c3333002 (Темп 36.9, АТсист 118)
    await expect(page.getByLabel('АТсист 2:00')).toHaveValue('118');
    await expect(page.getByLabel('Темп 2:00')).toHaveValue('36.9');
  });

  test('seeded record exposes computed MAP in the API response', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    await expect(page).toHaveURL(/\/icu\/doctor\/episode\//);

    // Перехоплюємо відповідь GET /hourly-records після перезавантаження сторінки
    const respPromise = page.waitForResponse(
      (r) => r.url().includes(`/api/clinical-days/${DAY_ID}/hourly-records`),
    );
    await page.reload();
    const resp = await respPromise;
    expect(resp.status()).toBe(200);

    // MAP для seed-запису c3333001 (АТсист 122 / АТдіас 68) = (2×68 + 122) / 3 = 86
    const records: Array<{
      systolicBP: number;
      diastolicBP: number;
      meanArterialPressure: number | null;
    }> = await resp.json();
    const seeded = records.find((r) => r.systolicBP === 122 && r.diastolicBP === 68);
    expect(seeded).toBeTruthy();
    expect(seeded!.meanArterialPressure).toBe(86);
  });

  test('creates a medication order via the quick form', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    await expect(page).toHaveURL(/\/icu\/doctor\/episode\//);

    // Відкриваємо форму швидкого створення призначення
    await page.getByRole('button', { name: '+ Нове призначення' }).click();
    await page.getByPlaceholder('Категорія').fill('Ліки');
    await page.getByPlaceholder('Препарат').fill('Dobutamine');
    await page.getByPlaceholder('Доза').fill('250');
    await page.getByPlaceholder('Од.').fill('мг');
    await page.getByPlaceholder('Шлях').fill('в/в');
    await page.getByPlaceholder('Частота').fill('1 р/д');
    // Початок/кінець — у майбутньому, щоб пройти валідацію минулої години
    await page.getByPlaceholder('Початок').fill(startAt(2));
    await page.getByPlaceholder('Кінець').fill(startAt(26));

    // Підтверджуємо створення через браузерний запит POST /orders
    const respPromise = page.waitForResponse(
      (r) => r.request().method() === 'POST' && r.url().includes(`/api/clinical-days/${DAY_ID}/orders`),
    );
    await page.getByRole('button', { name: 'Створити' }).click();
    const resp = await respPromise;
    expect(resp.status()).toBe(201);

    // Нове призначення з'являється в списку терапії
    await expect(page.getByText('Dobutamine')).toBeVisible();
  });
});
