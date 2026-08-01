import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';
const ORDER_NAME = 'Glucose 5%';
const PLAN_DOSE = '500';

// Індекс комірки в сітці терапії (години клінічного дня 8:00..7:59)
function cellIndexForHour(hour: number): number {
  return hour >= 8 ? hour - 8 + 1 : hour + 16 + 1;
}

// Година наступної доби для планування (з урахуванням ретраїв CI)
function planHour(retry: number): number {
  const hour = new Date(Date.now() + 60 * 60 * 1000).getHours();
  return (hour + retry) % 24;
}

test.describe('Nurse day flow', () => {
  test('opens the patient day from the dashboard', async ({ page }) => {
    // Медсестра відкриває день пацієнта Сидоренка зі свого дашборда
    await page.goto('/icu/nurse');
    const row = page.locator('tr', { hasText: 'Сидоренко' });
    await expect(row).toBeVisible();
    await row.getByRole('button', { name: 'Відкрити' }).click();
    await expect(page).toHaveURL(new RegExp(`/icu/nurse/episode/${EPISODE_ID}`));

    // Сітка терапії з seed-призначенням Glucose 5%
    await expect(page.getByText('Терапія (призначення)')).toBeVisible();
    await expect(page.getByText(ORDER_NAME)).toBeVisible();
  });

  test('vitals are read-only for nurse, losses are editable', async ({ page }) => {
    await page.goto(`/icu/nurse/episode/${EPISODE_ID}`);

    // Показники (вітальні) недоступні для редагування медсестрою
    await expect(page.getByLabel('ЧСС 0:00')).toBeDisabled();
    await expect(page.getByLabel('АТсист 0:00')).toBeDisabled();
    await expect(page.getByLabel('Темп 0:00')).toBeDisabled();

    // Втрати рідини (сеча/дренаж) медсестра редагує
    await expect(page.getByLabel('Сеча 0:00')).toBeEnabled();
    await expect(page.getByLabel('Дренаж 0:00')).toBeEnabled();
  });

  test('nurse updates urine output inline (PATCH)', async ({ page }) => {
    await page.goto(`/icu/nurse/episode/${EPISODE_ID}`);

    // Година 4:00 не має seed-запису — значення унікальне для кожного запуску (стійко до ретраїв)
    const input = page.getByLabel('Сеча 4:00');
    await expect(input).toBeEnabled();
    const current = await input.inputValue();
    const next = current === '' ? '250' : String(Number(current) + 10);

    // Вводимо значення і підтверджуємо збереження через браузерний PATCH
    const respPromise = page.waitForResponse(
      (r) => r.request().method() === 'PATCH' && r.url().includes('/api/hourly-records/'),
    );
    await input.fill(next);
    await input.press('Enter');
    const resp = await respPromise;
    expect([201, 204]).toContain(resp.status());

    // Поле відображає збережене значення
    await expect(input).toHaveValue(next);
  });

  test('nurse executes a planned medication and finishes it', async ({ page, doctorPage }, testInfo) => {
    let hour = planHour(testInfo.retry);
    let cellIndex = cellIndexForHour(hour);

    // Лікар планує дозу Glucose 5% у комірці обраної години
    await doctorPage.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    const doctorRow = doctorPage.locator('tr', { hasText: ORDER_NAME });
    await expect(doctorRow).toBeVisible();
    let cell = doctorRow.getByRole('cell').nth(cellIndex);
    const occupied = await cell.textContent();
    if (occupied && (occupied.includes('✓') || occupied.includes('✕'))) {
      hour = (hour + 1) % 24;
      cellIndex = cellIndexForHour(hour);
      cell = doctorRow.getByRole('cell').nth(cellIndex);
    }
    await cell.click();
    const planInput = doctorPage.getByLabel(`Запланувати ${ORDER_NAME} ${hour}:00`);
    await expect(planInput).toBeVisible();
    await planInput.fill(PLAN_DOSE);
    await planInput.press('Enter');
    await expect(doctorRow.getByRole('cell').nth(cellIndex)).toContainText(PLAN_DOSE);

    // Медсестра виконує заплановане призначення
    await page.goto(`/icu/nurse/episode/${EPISODE_ID}`);
    const nurseRow = page.locator('tr', { hasText: ORDER_NAME });
    await expect(nurseRow).toBeVisible();
    const nurseCell = nurseRow.getByRole('cell').nth(cellIndex);
    await expect(nurseCell).toContainText(PLAN_DOSE);

    // Виконання підтверджуємо через браузерний POST /execute
    const execRespPromise = page.waitForResponse(
      (r) => r.request().method() === 'POST' && r.url().includes('/api/orders/') && r.url().includes('/execute'),
    );
    await nurseCell.click();
    const executeInput = page.getByLabel(`Виконати ${ORDER_NAME} ${hour}:00`);
    await expect(executeInput).toBeVisible();
    await executeInput.fill(PLAN_DOSE);
    await executeInput.press('Enter');
    const execResp = await execRespPromise;
    expect(execResp.status()).toBe(201);

    // Комірка показує виконання, медсестра завершує виконання
    await expect(nurseCell).toContainText('✓');
    await nurseCell.click();
    await page.getByRole('button', { name: 'Завершити' }).click();
    await nurseCell.hover();
    await expect(page.getByText(/виконано, доза 500, завершено/)).toBeVisible();
  });
});
