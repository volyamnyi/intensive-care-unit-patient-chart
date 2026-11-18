import { test, expect, type Page } from '@playwright/test';

// Issue #451 (depends on #448): route-specific layout regression for the MIS
// order list on `/prosthetics/new/select-order`. The generic
// `no-horizontal-scroll.spec.ts` intentionally skips elements inside local
// scroll containers, so it cannot prove that a long document/product name
// stays readable or that the select action is reachable without hidden
// horizontal scrolling. This spec intercepts only the read/provision API
// responses to keep the fixture deterministic; it changes no backend, MIS,
// or database contract.

const PATIENT_ID = '900002';
const ORDER_ID = 'order-layout-451';
const PATIENT = {
  id: PATIENT_ID,
  pib: 'Гаврилюк Олена Миколаївна',
  birthDate: '1970-01-01',
  gender: 'Жіноча',
};

const DOCUMENT = {
  documentId: 681078,
  documentCreationDate: '2026-09-07T13:32:12.1',
  documentTemplateId: 121,
  documentTemplateName:
    'Замовлення на протези нижніх кінцівок для пацієнта з дуже довгим суспільним описом виробу та додатковими умовами',
  documentUrl: 'https://mis.example/document?request=layout-451',
  productName:
    '06.12.09 : Ортези на колінний суглоб із регулюванням шарніра, симетричний дизайн із зовнішнім протезуванням нижньої кінцівки',
};

const ORDER = {
  id: ORDER_ID,
  orderNumber: 'ТЕСТ-451',
  patientId: PATIENT_ID,
};

async function installOrderListRoutes(page: Page): Promise<void> {
  // Register the broadest patient route first: Playwright matches the most
  // recently registered route first, so the specific routes below win.
  await page.route('**/api/prosthesis-manufacturing/patients*', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([PATIENT]) }),
  );
  await page.route('**/api/prosthesis-manufacturing/patients/candidates*', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
  );
  await page.route(`**/api/prosthesis-manufacturing/patients/${PATIENT_ID}`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(PATIENT) }),
  );
  await page.route('**/api/prosthesis-manufacturing/orders/documents*', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([DOCUMENT]) }),
  );
  await page.route('**/api/prosthesis-manufacturing/orders/provision', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ORDER) }),
  );
  await page.route(`**/api/prosthesis-manufacturing/orders/${ORDER_ID}`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ORDER) }),
  );
}

async function openOrderSelect(page: Page): Promise<void> {
  await installOrderListRoutes(page);
  await page.goto('/prosthetics/new/select-patient', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: 'Вибір пацієнта' })).toBeVisible();

  const patientRow = page.locator('table tbody tr').filter({ hasText: PATIENT.pib }).first();
  await expect(patientRow).toBeVisible();
  await patientRow.getByRole('button', { name: 'Обрати' }).click();

  await expect(page).toHaveURL(/select-order/);
  await expect(page.getByTestId('mis-order-documents')).toBeVisible();
  await expect(page.getByText(DOCUMENT.documentTemplateName)).toBeVisible();
  await expect(page.getByText(DOCUMENT.productName)).toBeVisible();
}

interface LayoutMetrics {
  viewport: number;
  documentScrollWidth: number;
  tableClientWidth: number;
  tableScrollWidth: number;
  buttonLeft: number;
  buttonRight: number;
  buttonInViewport: boolean;
}

async function readLayoutMetrics(page: Page): Promise<LayoutMetrics> {
  return page.evaluate(() => {
    const table = document.querySelector('[data-testid="mis-order-documents"]');
    const container = table?.parentElement ?? null;
    const button = Array.from(document.querySelectorAll('button')).find(
      (element) => element.textContent?.trim() === 'Обрати',
    );
    const rect = button?.getBoundingClientRect() ?? null;
    return {
      viewport: window.innerWidth,
      documentScrollWidth: document.documentElement.scrollWidth,
      tableClientWidth: container?.clientWidth ?? 0,
      tableScrollWidth: container?.scrollWidth ?? 0,
      buttonLeft: rect ? Math.round(rect.left) : Number.NaN,
      buttonRight: rect ? Math.round(rect.right) : Number.NaN,
      buttonInViewport: rect ? rect.left >= 0 && rect.right <= window.innerWidth : false,
    };
  });
}

const VIEWPORTS = [
  { name: 'mobile 360x740', width: 360, height: 740 },
  { name: 'tablet 768x1024', width: 768, height: 1024 },
  { name: 'desktop 1280x800', width: 1280, height: 800 },
] as const;

for (const viewport of VIEWPORTS) {
  test.describe(`order list layout — ${viewport.name}`, () => {
    test.use({
      viewport: { width: viewport.width, height: viewport.height },
      storageState: '.auth/prosthetist.json',
    });

    test('keeps long MIS document content readable and the select action in view', async ({ page }) => {
      await openOrderSelect(page);

      const metrics = await readLayoutMetrics(page);
      expect(metrics.documentScrollWidth).toBeLessThanOrEqual(metrics.viewport + 1);
      expect(metrics.tableScrollWidth).toBeLessThanOrEqual(metrics.tableClientWidth + 1);
      expect(metrics.buttonInViewport).toBe(true);
      expect(metrics.buttonRight).toBeGreaterThan(metrics.buttonLeft);

      const selectButton = page.getByRole('button', { name: 'Обрати' });
      await expect(selectButton).toBeVisible();
      await expect(selectButton).toHaveAccessibleDescription(/Замовлення на протези нижніх кінцівок/);
      await expect(selectButton).toHaveAccessibleDescription(/Ортези на колінний суглоб/);
    });
  });
}

test.describe('order list selection — mobile 360x740', () => {
  test.use({
    viewport: { width: 360, height: 740 },
    hasTouch: true,
    storageState: '.auth/prosthetist.json',
  });

  test('activates the order action from the keyboard and continues to review', async ({ page }) => {
    await openOrderSelect(page);

    const provisionRequest = page.waitForRequest(
      (request) =>
        request.url().includes('/api/prosthesis-manufacturing/orders/provision') &&
        request.method() === 'POST',
    );
    const selectButton = page.getByRole('button', { name: 'Обрати' });
    await selectButton.focus();
    await page.keyboard.press('Enter');

    const request = await provisionRequest;
    expect(request.postDataJSON()).toMatchObject({
      patientId: PATIENT_ID,
      documentId: DOCUMENT.documentId,
    });
    await expect(page).toHaveURL(/review-order/);
  });

  test('activates the order action by touch and continues to review', async ({ page }) => {
    await openOrderSelect(page);

    const provisionRequest = page.waitForRequest(
      (request) =>
        request.url().includes('/api/prosthesis-manufacturing/orders/provision') &&
        request.method() === 'POST',
    );
    await page.getByRole('button', { name: 'Обрати' }).tap();

    const request = await provisionRequest;
    expect(request.postDataJSON()).toMatchObject({
      patientId: PATIENT_ID,
      documentId: DOCUMENT.documentId,
    });
    await expect(page).toHaveURL(/review-order/);
  });
});
