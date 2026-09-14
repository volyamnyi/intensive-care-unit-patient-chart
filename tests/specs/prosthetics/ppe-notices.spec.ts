import { test, expect, type Page } from '@playwright/test';
import {
  PROSTH,
  login,
  headersFor,
  findTemplateByIdName,
  createFreeLowerInstance,
  completeToStep,
  terminateInstance,
} from '../../helpers/tp-ll-02-flow';
import { testUser } from '../../helpers/test-users';

/**
 * Prosthetics — PPE notices (епік ЗІЗ, #296). Runs under the serial
 * `prosthetics-chromium` project (storageState `.auth/prosthetist.json`).
 *
 * Карта нотисів (конфіг `ppeNotices.ts`, TP-LL-02 `c0000003`):
 *   e0000022 (Етап 2) / e0000026 (Етап 4) / e0000028 (Етап 6) /
 *   e0000032 (Етап 9)                         → nitrile («Захист рук»)
 *   e0000029 (Етап 7)                          → full-kit (комплект ЗІЗ)
 *   e0000024 (Етап 3, КРОК 1)                  → ОБИДВА (full-kit вище)
 *   e0000021 / e0000023 / e0000025 / e0000030…  → без банерів
 *
 * Кожен тест створює свіжий інстанс на вільному lower-замовленні і
 * добиває його в `afterEach`, щоб замовлення лишалося вільним.
 *
 * УВАГА про апострофи: назви кроків у БД — з ASCII-апострофом U+0027
 * (SQL-escaped `''`), тому в getByText-рядках лише U+0027 (екранується
 * як \'), ніколи U+2019 — інакше локатор нічого не знайде.
 */

const FULL_KIT_TITLE = 'Засоби індивідуального захисту';
const FULL_KIT_TEXT =
  'Роботу виконувати із застосуванням засобів індивідуального захисту (термостійкі рукавиці, окуляри, респіратор, навушники)!!!';
const NITRILE_TITLE = 'Захист рук';
const NITRILE_TEXT = 'Роботу виконувати у штучних захисних рукавицях (нітрилові рукавиці)!!!';

const E22 = 'e0000022-0000-0000-0000-000000000022';
const E23 = 'e0000023-0000-0000-0000-000000000023';
const E24 = 'e0000024-0000-0000-0000-000000000024';
const E25 = 'e0000025-0000-0000-0000-000000000025';
const E26 = 'e0000026-0000-0000-0000-000000000026';
const E28 = 'e0000028-0000-0000-0000-000000000028';
const E29 = 'e0000029-0000-0000-0000-000000000029';
const E30 = 'e0000030-0000-0000-0000-000000000030';
const E32 = 'e0000032-0000-0000-0000-000000000032';

test.describe('Prosthetics — PPE notices', () => {
  let prosthetistToken: string;
  let templateId: string;
  let instanceId = '';
  const h = (token?: string) => headersFor(token ?? prosthetistToken);

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, testUser(7).login, testUser(7).password);
    templateId = await findTemplateByIdName(request, h(), 'TP-LL-02');
  });

  test.afterEach(async ({ request }) => {
    if (instanceId) {
      await terminateInstance(request, h(), instanceId);
    }
    instanceId = '';
  });

  /** Fresh instance driven via API to the target step (current, not past it). */
  async function startAtStep(request: any, stepId: string): Promise<string> {
    const inst = await createFreeLowerInstance(request, h(), templateId);
    const started = await request.post(`${PROSTH}/instances/${inst.id}/start`, { headers: h() });
    expect(started.ok(), `start failed: ${started.status()}`).toBeTruthy();
    return (await completeToStep(request, h(), inst.id, stepId)).id;
  }

  function trackErrors(page: Page): string[] {
    const errors: string[] = [];
    page.on('console', (m) => {
      if (m.type() === 'error') errors.push(m.text());
    });
    return errors;
  }

  async function gotoWizard(page: Page, id: string) {
    await page.goto(`/prosthetics/process/${id}/wizard`);
    await expect(page.getByText(/Етап \d+ з \d+:/)).toBeVisible();
  }

  async function expectFullKit(page: Page) {
    const banner = page.getByTestId('ppe-notice-full-kit');
    await expect(banner).toBeVisible();
    await expect(banner.getByText(FULL_KIT_TITLE)).toBeVisible();
    await expect(banner.getByText(FULL_KIT_TEXT)).toBeVisible();
    const imgs = banner.getByRole('img');
    await expect(imgs).toHaveCount(4);
    for (const caption of ['Термостійкі рукавиці', 'Захисні окуляри', 'Респіратор', 'Навушники']) {
      // exact: підпис у figcaption; підрядок також входить до основного тексту банера
      await expect(banner.getByText(caption, { exact: true })).toBeVisible();
    }
    for (let i = 0; i < 4; i++) {
      expect(await imgs.nth(i).getAttribute('alt')).toBeTruthy();
    }
  }

  async function expectNitrile(page: Page) {
    const banner = page.getByTestId('ppe-notice-nitrile');
    await expect(banner).toBeVisible();
    await expect(banner.getByText(NITRILE_TITLE)).toBeVisible();
    await expect(banner.getByText(NITRILE_TEXT)).toBeVisible();
    const imgs = banner.getByRole('img');
    await expect(imgs).toHaveCount(1);
    await expect(imgs.first()).toHaveAttribute('src', '/ppe/nitrile-gloves.png');
    expect(await imgs.first().getAttribute('alt')).toBeTruthy();
    await expect(banner.getByText('Нітрилові рукавиці', { exact: true })).toBeVisible();
  }

  async function expectNoNotices(page: Page) {
    await expect(page.getByTestId('ppe-notice-full-kit')).toHaveCount(0);
    await expect(page.getByTestId('ppe-notice-nitrile')).toHaveCount(0);
  }

  test('ранні кроки: e22 nitrile → e23 чисто → e24 обидва → e25 чисто → e26 nitrile', async ({
    page,
    request,
  }) => {
    test.setTimeout(300000);
    const errors = trackErrors(page);
    instanceId = await startAtStep(request, E22);

    await gotoWizard(page, instanceId);
    await expectNitrile(page);
    await expect(page.getByTestId('ppe-notice-full-kit')).toHaveCount(0);

    await completeToStep(request, h(), instanceId, E23);
    await gotoWizard(page, instanceId);
    await expect(page.getByText('КРОК 2: Перевірка гіпсового позитива')).toBeVisible();
    await expectNoNotices(page);

    await completeToStep(request, h(), instanceId, E24);
    await gotoWizard(page, instanceId);
    await expect(page.getByText('КРОК 1: Виготовлення тренувальної гільзи')).toBeVisible();
    await expectFullKit(page);
    await expectNitrile(page);
    // Порядок стеку: комплект вище, нітрил нижче.
    const kitBox = await page.getByTestId('ppe-notice-full-kit').boundingBox();
    const nitBox = await page.getByTestId('ppe-notice-nitrile').boundingBox();
    expect(kitBox?.y).toBeLessThan(nitBox?.y ?? Number.POSITIVE_INFINITY);
    // Фото комплекту реально завантажились (асети з public/ppe у git).
    const widths = await page
      .getByTestId('ppe-notice-full-kit')
      .getByRole('img')
      .evaluateAll((els) => els.map((el) => (el as HTMLImageElement).naturalWidth));
    expect(widths).toHaveLength(4);
    for (const w of widths) expect(w).toBeGreaterThan(0);

    await completeToStep(request, h(), instanceId, E25);
    await gotoWizard(page, instanceId);
    await expect(page.getByText('КРОК 2: Контроль якості тренувальної гільзи')).toBeVisible();
    await expectNoNotices(page);

    await completeToStep(request, h(), instanceId, E26);
    await gotoWizard(page, instanceId);
    await expect(page.getByText('КРОК 1: Примірка тренувальної гільзи')).toBeVisible();
    await expectNitrile(page);
    await expect(page.getByTestId('ppe-notice-full-kit')).toHaveCount(0);

    expect(errors).toEqual([]);
  });

  test('пізні кроки: e28 nitrile → e29 full-kit → e30 чисто → e32 nitrile', async ({
    page,
    request,
  }) => {
    test.setTimeout(300000);
    const errors = trackErrors(page);
    instanceId = await startAtStep(request, E28);

    await gotoWizard(page, instanceId);
    await expect(page.getByText('КРОК 1: Примірювання та коректування тренувального протеза')).toBeVisible();
    await expectNitrile(page);
    await expect(page.getByTestId('ppe-notice-full-kit')).toHaveCount(0);

    await completeToStep(request, h(), instanceId, E29);
    await gotoWizard(page, instanceId);
    await expect(page.getByText('КРОК 1: Виготовлення пом\'якшуючого вкладиша')).toBeVisible();
    await expectFullKit(page);
    await expect(page.getByTestId('ppe-notice-nitrile')).toHaveCount(0);

    await completeToStep(request, h(), instanceId, E30);
    await gotoWizard(page, instanceId);
    await expect(page.getByText('КРОК 2: Виготовлення постійної гільзи')).toBeVisible();
    await expectNoNotices(page);

    await completeToStep(request, h(), instanceId, E32);
    await gotoWizard(page, instanceId);
    await expect(page.getByText('КРОК 1: Примірювання та коректування постійного протеза')).toBeVisible();
    await expectNitrile(page);
    await expect(page.getByTestId('ppe-notice-full-kit')).toHaveCount(0);

    expect(errors).toEqual([]);
  });

  test('non-blocking: крок з банером завершується кнопкою «Готово» без підтверджень', async ({
    page,
    request,
  }) => {
    test.setTimeout(120000);
    const errors = trackErrors(page);
    instanceId = await startAtStep(request, E22);
    await gotoWizard(page, instanceId);
    await expectNitrile(page);

    // Банер не містить контролів — відмічаємо лише чекбокси самого кроку.
    for (const cb of await page.getByRole('checkbox').all()) {
      if (!(await cb.isChecked())) await cb.check();
    }
    await page.getByRole('button', { name: /Готово/ }).click();
    // Візард перейшов далі (повний релоад через window.location.href).
    await expect(page.getByText('КРОК 2: Перевірка гіпсового позитива')).toBeVisible();

    expect(errors).toEqual([]);
  });

  test('responsive: крок з подвійним банером без горизонтального скролу на 360/768', async ({
    page,
    request,
  }) => {
    test.setTimeout(120000);
    const errors = trackErrors(page);
    instanceId = await startAtStep(request, E24);

    for (const viewport of [
      { width: 360, height: 800 },
      { width: 768, height: 1024 },
    ]) {
      await page.setViewportSize(viewport);
      await gotoWizard(page, instanceId);
      await expect(page.getByTestId('ppe-notice-full-kit')).toBeVisible();
      await expect(page.getByTestId('ppe-notice-nitrile')).toBeVisible();
      // Чекаємо стабільної верстки (шрифти/дані), потім один замір.
      let previous = Number.NaN;
      await expect
        .poll(
          async () => {
            const current = await page.evaluate(() => document.documentElement.scrollWidth);
            const stable = current === previous;
            previous = current;
            return { stable, width: current };
          },
          { timeout: 15000, intervals: [250, 500, 1000] },
        )
        .toMatchObject({ stable: true });
      const { docWidth, viewportWidth } = await page.evaluate(() => ({
        docWidth: document.documentElement.scrollWidth,
        viewportWidth: window.innerWidth,
      }));
      expect(docWidth, `horizontal overflow at ${viewport.width}px`).toBeLessThanOrEqual(
        viewportWidth + 1,
      );
    }

    expect(errors).toEqual([]);
  });
});
