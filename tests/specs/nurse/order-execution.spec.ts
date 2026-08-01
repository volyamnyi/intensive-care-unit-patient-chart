import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';
const ORDER_NAME = 'Glucose 5%';
const PLAN_DOSE = '500';

function cellIndexForHour(hour: number): number {
  return hour >= 8 ? hour - 8 + 1 : hour + 16 + 1;
}

function planHour(retry: number): number {
  const hour = new Date(Date.now() + 60 * 60 * 1000).getHours();
  return (hour + retry) % 24;
}

test('nurse opens the patient day from the dashboard and sees the therapy grid', async ({ page }) => {
  await page.goto('/icu/nurse');
  const row = page.locator('tr', { hasText: 'Сидоренко' });
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: 'Відкрити' }).click();
  await expect(page).toHaveURL(new RegExp(`/icu/nurse/episode/${EPISODE_ID}`));
  await expect(page.getByText('Терапія (призначення)')).toBeVisible();
  await expect(page.getByText(ORDER_NAME)).toBeVisible();
});

test('doctor plans and nurse executes medication in the hourly grid', async ({ page, doctorPage }, testInfo) => {
  let hour = planHour(testInfo.retry);
  let cellIndex = cellIndexForHour(hour);

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

  await page.goto(`/icu/nurse/episode/${EPISODE_ID}`);
  const nurseRow = page.locator('tr', { hasText: ORDER_NAME });
  await expect(nurseRow).toBeVisible();
  const nurseCell = nurseRow.getByRole('cell').nth(cellIndex);
  await expect(nurseCell).toContainText(PLAN_DOSE);
  await nurseCell.click();
  const executeInput = page.getByLabel(`Виконати ${ORDER_NAME} ${hour}:00`);
  await expect(executeInput).toBeVisible();
  await executeInput.fill(PLAN_DOSE);
  await executeInput.press('Enter');
  await expect(nurseCell).toContainText('✓');

  await nurseCell.click();
  await page.getByRole('button', { name: 'Завершити' }).click();
  await nurseCell.hover();
  await expect(page.getByText(/виконано, доза 500, завершено/)).toBeVisible();
});
