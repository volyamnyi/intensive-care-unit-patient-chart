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

test.describe('Validation edge cases', () => {
  test('rejects a medication order started in the past (422 PAST_HOUR_ORDER)', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    await expect(page).toHaveURL(/\/icu\/doctor\/episode\//);

    // Форма швидкого створення з початком на 2 години в минулому
    await page.getByRole('button', { name: '+ Нове призначення' }).click();
    await page.getByPlaceholder('Препарат').fill('Past Hour Drug');
    await page.getByPlaceholder('Доза').fill('10');
    await page.getByPlaceholder('Од.').fill('мг');
    await page.getByPlaceholder('Шлях').fill('в/в');
    await page.getByPlaceholder('Частота').fill('1 р/д');
    await page.locator('input[type="datetime-local"]').fill(startAt(-2));

    // Бекенд відхиляє запит з кодом PAST_HOUR_ORDER (422)
    const respPromise = page.waitForResponse(
      (r) => r.request().method() === 'POST' && r.url().includes(`/api/clinical-days/${DAY_ID}/orders`),
    );
    await page.getByRole('button', { name: 'Створити' }).click();
    const resp = await respPromise;
    expect(resp.status()).toBe(422);
    const body = (await resp.json()) as { code?: string };
    expect(body.code).toBe('PAST_HOUR_ORDER');

    // UI показує зрозуміле повідомлення про помилку
    await expect(page.getByText('Не можна створити призначення на минулу годину')).toBeVisible();
  });

  test('rejects out-of-range heart rate with 400 VALIDATION_ERROR', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    // Година 5:00 не має seed-запису; ЧСС 301 перевищує верхню межу 300
    const input = page.getByLabel('ЧСС 5:00');
    await expect(input).toBeEnabled();
    const respPromise = page.waitForResponse(
      (r) => r.request().method() === 'POST' && r.url().includes(`/api/clinical-days/${DAY_ID}/hourly-records`),
    );
    await input.fill('301');
    await input.press('Enter');

    // Бекенд відхиляє з кодом VALIDATION_ERROR (400), значення не зберігається
    const resp = await respPromise;
    expect(resp.status()).toBe(400);
    const body = (await resp.json()) as { code?: string };
    expect(body.code).toBe('VALIDATION_ERROR');
    await page.reload();
    await expect(page.getByLabel('ЧСС 5:00')).toHaveValue('');
  });

  test('shows a friendly message when the episode does not exist (404)', async ({ page }) => {
    // Перехоплюємо статус GET /api/episodes для неіснуючого UUID
    let status = 0;
    page.on('response', (r) => {
      if (r.url().includes('/api/episodes/00000000-0000-0000-0000-000000000000')
          && !r.url().includes('/clinical-days')) {
        status = r.status();
      }
    });

    await page.goto('/icu/doctor/episode/00000000-0000-0000-0000-000000000000');
    await expect(page.getByText('Епізод не знайдено')).toBeVisible();
    expect(status).toBe(404);
  });

  test('past-hour therapy cell does not open a plan input', async ({ page }) => {
    // У вікні 8:00-8:59 клінічного дня жодна година ще не минула — тест неможливий
    const clockHour = new Date().getHours();
    test.skip(clockHour === 8, 'no past hour exists between 8:00 and 8:59');

    // Обираємо годину, яка гарантовано в минулому для поточної години доби
    const pastHour = clockHour >= 8 ? clockHour - 1 : 8;
    const cellIndex = pastHour - 8 + 1;

    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    await expect(page).toHaveURL(/\/icu\/doctor\/episode\//);
    const row = page.locator('tr', { hasText: 'Glucose 5%' });
    await expect(row).toBeVisible();

    // Клік по минулій комірці не відкриває інпут планування
    await row.getByRole('cell').nth(cellIndex).click();
    await expect(page.getByLabel(`Запланувати Glucose 5% ${pastHour}:00`)).toBeHidden();
  });
});
