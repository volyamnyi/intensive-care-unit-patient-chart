import { test, expect } from '../../fixtures/index';

const EPISODE_LOCKED = 'a2222222-2222-2222-2222-222222222222';
const EPISODE_OPEN = 'a3333333-3333-3333-3333-333333333333';
const ORDER_NAME_OPEN = 'Glucose 5%';
const DOSE = '1000';

function cellIndexForHour(hour: number): number {
  return hour >= 8 ? hour - 8 + 1 : hour + 16 + 1;
}

function planHour(): number {
  return new Date().getHours();
}

test('locked modal has banner, disabled cells, and closes via "Закрити карту"', async ({ page }) => {
  await page.goto(`/icu/doctor/episode/${EPISODE_LOCKED}`);
  await expect(page.getByText('Показник / година')).toBeVisible();

  const trigger = page.getByRole('button', { name: 'Розгорнути на весь екран' });
  await trigger.click();

  const dialog = page.getByRole('dialog', { name: /Погодинна карта/ });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByText('Доба підписана — перегляд')).toHaveCount(1);

  await expect(dialog.getByRole('button', { name: /Скасувати останню зміну/ })).toBeDisabled();

  const chssCell = dialog.getByLabel('ЧСС 0:00');
  await expect(chssCell).toBeDisabled();

  await dialog.getByRole('button', { name: 'Закрити карту' }).click();
  await expect(dialog).toBeHidden();
});

test('doctor plans therapy in the modal, cancels it via the undo toast, and restores the plan', async ({ page }) => {
  let hour = planHour();
  await page.goto(`/icu/doctor/episode/${EPISODE_OPEN}`);
  await expect(page.getByText('Показник / година')).toBeVisible();

  await page.getByRole('button', { name: 'Розгорнути на весь екран' }).click();
  const dialog = page.getByRole('dialog', { name: /Погодинна карта/ });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByText('Доба підписана — перегляд')).toHaveCount(0);

  const row = dialog.locator('tr', { hasText: ORDER_NAME_OPEN });
  await expect(row).toBeVisible();
  let cellIndex = cellIndexForHour(hour);
  let cell = row.getByRole('cell').nth(cellIndex);
  const occupied = await cell.textContent();
  if (occupied && (occupied.includes('✓') || occupied.includes('✕'))) {
    hour = (hour + 1) % 24;
    cellIndex = cellIndexForHour(hour);
    cell = row.getByRole('cell').nth(cellIndex);
  }

  await cell.click();
  const planInput = dialog.getByLabel(`Запланувати ${ORDER_NAME_OPEN} ${hour}:00`);
  await expect(planInput).toBeVisible();
  await planInput.fill(DOSE);
  await planInput.press('Enter');
  await expect(cell).toContainText(DOSE);

  await cell.click();
  const cancelCell = dialog.getByLabel(`Скасувати ${ORDER_NAME_OPEN} ${hour}:00`);
  await expect(cancelCell).toBeVisible();
  await cancelCell.click();

  const toast = dialog.getByRole('status').filter({ hasText: 'Виконання скасовано' });
  await expect(toast).toBeVisible();
  await expect(cell).toContainText('✕');

  await toast.getByRole('button', { name: 'Скасувати', exact: true }).click();
  await expect(cell).toContainText(DOSE);
});
