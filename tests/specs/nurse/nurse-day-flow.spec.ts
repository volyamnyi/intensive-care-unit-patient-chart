import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';
const DAY_ID = 'b3333333-3333-3333-3333-333333333333';
const ORDER_NAME = 'Glucose 5%';
const PLAN_DOSE = '500';

// Індекс комірки в сітці терапії (години клінічного дня 8:00..7:59)
function cellIndexForHour(hour: number): number {
  return hour >= 8 ? hour - 8 + 1 : hour + 16 + 1;
}

// Година для планування — поточна реальна година: HourlyGrid позначає минулі
// комірки доби 08:00->07:59 через isPastMedDay(h, realClockHour) як '✓' (неклікабельні),
// а поточна година ніколи не вважається минулою. У вікні 07:00-07:59Z клікабельна
// лише поточна година; modal-therapy.spec.ts лишає в ній план (доза 1000) —
// лікар може перепланувати заплановану комірку, тож план-інпут з'явиться.
function planHour(): number {
  return new Date().getHours();
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
    test.setTimeout(60000);

    // Чекаємо завантаження погодинних записів: комірки редаговані одразу після рендеру,
    // але значення заповнюються асинхронно після GET — без очікування читання '' дає POST
    // замість PATCH (409 на повторному запуску, коли запис 4:00 уже існує)
    const recordsLoaded = page.waitForResponse(
      (r) => r.request().method() === 'GET' && r.url().includes(`/api/clinical-days/${DAY_ID}/hourly-records`),
    );
    await page.goto(`/icu/nurse/episode/${EPISODE_ID}`);
    await recordsLoaded;

    // Година 4:00 не має seed-запису — значення унікальне для кожного запуску (стійко до ретраїв)
    const input = page.getByLabel('Сеча 4:00');
    await expect(input).toBeEnabled();

    // Вводимо значення і підтверджуємо збереження через браузерний POST (створення) або PATCH (оновлення)
    const saveResponse = (r: { request: () => { method(): string; url(): string } }) =>
      (r.request().method() === 'POST' && r.url().includes(`/api/clinical-days/${DAY_ID}/hourly-records`))
      || (r.request().method() === 'PATCH' && r.url().includes('/api/hourly-records/'));
    let resp: { status(): number } | null = null;
    let savedValue = '';
    for (let attempt = 0; attempt < 3 && !resp; attempt++) {
      // Значення обчислюємо з актуального стану комірки: якщо сітка довантажилася між
      // спробами, комірка вже містить серверне значення — тоді коректно йде PATCH, а не POST
      const valueNow = await input.inputValue();
      savedValue = valueNow === '' ? '250' : String(Number(valueNow) + 10);
      const respPromise = page.waitForResponse(saveResponse, { timeout: 8000 });
      await input.fill(savedValue);
      await input.press('Enter');
      try {
        const r = await respPromise;
        // 409/422 (зміна стану між читанням і збереженням) — повторюємо замість жорсткої помилки
        if ([201, 204].includes(r.status())) resp = r;
      } catch {
        // Запит не пішов або відповідь випередила реєстрацію очікування — повторюємо fill + Enter
      }
    }
    if (resp) {
      expect([201, 204]).toContain(resp.status());
      await expect(input).toHaveValue(savedValue);
    } else {
      // Відповідь могла випередити реєстрацію очікування — перевіряємо збереження на сервері
      await page.goto(`/icu/nurse/episode/${EPISODE_ID}`);
      await expect(page.getByLabel('Сеча 4:00')).toHaveValue(savedValue);
    }
  });

  test('nurse executes a planned medication and finishes it', async ({ page, doctorPage }) => {
    let hour = planHour();

    // Лікар планує дозу Glucose 5% у комірці обраної години
    await doctorPage.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    const doctorRow = doctorPage.locator('tr', { hasText: ORDER_NAME });
    await expect(doctorRow).toBeVisible();
    // Виконання завантажуються асинхронно після призначень (GET /orders/{id}/executions) —
    // чекаємо стабілізації сітки, інакше перевірка ✓/✕ може не побачити зайняту годину
    await doctorPage.waitForLoadState('networkidle');
    // Шукаємо першу комірку, яку можна (пере)планувати, від поточної години (wrap 24):
    // '✓' (виконано/минула) і '✕' (скасовано) неклікабельні; вільна комірка показує
    // '➚', запланована — дозу (лікар може перепланувати її).
    let cellIndex = 0;
    for (let i = 0; i < 24; i++) {
      const h = (hour + i) % 24;
      const idx = cellIndexForHour(h);
      const txt = await doctorRow.getByRole('cell').nth(idx).textContent();
      if (txt && (txt.includes('✓') || txt.includes('✕'))) continue;
      hour = h;
      cellIndex = idx;
      break;
    }
    const cell = doctorRow.getByRole('cell').nth(cellIndex);
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
