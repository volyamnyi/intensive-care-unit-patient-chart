import { test, expect } from '@playwright/test';
import {
  findOrderIdByProductType,
  findTemplateByIdName,
  createFreeInstanceOnOrder,
  terminateInstance,
  login,
  headersFor,
} from '../../helpers/tp-ll-02-flow';
import { testUser } from '../../helpers/test-users';

// Production monitoring dashboard UI (issue #275, epic #271).
// Serial project: a dedicated instance is created on the upper-limb seed
// order in beforeAll and terminated in afterAll.

test.describe('Production monitoring dashboard', () => {
  let instanceId: string;

  test.beforeAll(async ({ request }) => {
    const token = await login(request, testUser(7).login, testUser(7).password);
    const headers = headersFor(token);
    const orderId = await findOrderIdByProductType(request, headers, 'UPPER_LIMB', '');
    const templateId = await findTemplateByIdName(request, headers, 'TP-UL-01');
    const created = (await createFreeInstanceOnOrder(request, headers, orderId, templateId)) as {
      id: string;
    };
    instanceId = created.id;
  });

  test.afterAll(async ({ request }) => {
    if (!instanceId) return;
    const token = await login(request, testUser(7).login, testUser(7).password);
    await terminateInstance(request, headersFor(token), instanceId).catch(() => undefined);
  });

  test('renders KPI cards and the workload table (prosthetist first)', async ({ page }) => {
    await page.goto('/prosthetics/production');
    await expect(page.getByRole('heading', { name: 'Моніторинг виробництва' })).toBeVisible();
    await expect(page.getByText('В роботі')).toBeVisible();
    await expect(page.getByText('Сер. активний час')).toBeVisible();

    const table = page.getByRole('table');
    await expect(table).toBeVisible();
    const headers = await table.getByRole('columnheader').allTextContents();
    expect(headers[0]).toContain('Протезист');
    expect(headers.indexOf('Виріб')).toBeGreaterThan(0);
    expect(headers.indexOf('Пацієнт')).toBeGreaterThan(headers.indexOf('Виріб'));
  });

  test('prosthetist sees no team scope selector', async ({ page }) => {
    await page.goto('/prosthetics/production');
    await expect(page.getByRole('table')).toBeVisible();
    await expect(page.getByRole('combobox', { name: 'Чиї вироби' })).toHaveCount(0);
  });

  test('quality filter narrows the table', async ({ page }) => {
    await page.goto('/prosthetics/production');
    await expect(page.getByRole('table')).toBeVisible();
    await page.getByRole('combobox', { name: 'Якість' }).click();
    await page.getByRole('option', { name: 'Без браку' }).click();
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('row opens the detail drawer', async ({ page }) => {
    await page.goto('/prosthetics/production');
    const firstRow = page.getByRole('table').getByRole('row').nth(1);
    await expect(firstRow).toBeVisible();
    await firstRow.getByRole('button', { name: 'Відкрити' }).click();
    const drawer = page.getByRole('dialog');
    await expect(drawer).toBeVisible();
    await expect(drawer.getByText('Виріб')).toBeVisible();
  });

  test('drawer shows patient, timeline and masked personal data', async ({ page }) => {
    await page.goto('/prosthetics/production');
    const firstRow = page.getByRole('table').getByRole('row').nth(1);
    await expect(firstRow).toBeVisible();
    await firstRow.getByRole('button', { name: 'Відкрити' }).click();
    const drawer = page.getByRole('dialog');
    await expect(drawer).toBeVisible();
    await expect(drawer.getByText('Пацієнт')).toBeVisible();
    await expect(drawer.getByText('Хронологія кроків')).toBeVisible();
    await expect(drawer.getByText('Час виробництва')).toBeVisible();
    // Prosthetist holds no PATIENT_VIEW: personal details hidden, no document.
    await expect(drawer.getByText(/Деталі приховано/)).toBeVisible();
    await expect(drawer.getByRole('button', { name: 'Відкрити документ' })).toHaveCount(0);
  });

  test('drawer opens the process page', async ({ page }) => {
    await page.goto('/prosthetics/production');
    const firstRow = page.getByRole('table').getByRole('row').nth(1);
    await expect(firstRow).toBeVisible();
    await firstRow.getByRole('button', { name: 'Відкрити' }).click();
    const drawer = page.getByRole('dialog');
    await expect(drawer).toBeVisible();
    await drawer.getByRole('button', { name: 'Відкрити процес' }).click();
    await expect(page).toHaveURL(/\/prosthetics\/process\/[0-9a-f-]+\/(wizard|done|failed)/);
  });
});
