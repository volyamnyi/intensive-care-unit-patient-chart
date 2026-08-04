import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

test('nurse edits a loss row inside the modal grid and the value persists after reopening', async ({ page }) => {
  await page.goto(`/icu/nurse/episode/${EPISODE_ID}`);
  await expect(page.getByText('Показник / година')).toBeVisible();

  const trigger = page.getByRole('button', { name: 'Розгорнути на весь екран' });
  await trigger.click();

  const dialog = page.getByRole('dialog', { name: /Погодинна карта/ });
  await expect(dialog).toBeVisible();

  const cell = dialog.getByLabel('Сеча 4:00');
  await cell.click();
  await cell.fill('250');
  await cell.press('Enter');
  await expect(cell).toHaveValue('250');

  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
  await expect(trigger).toBeFocused();

  await trigger.click();
  const reopened = page.getByRole('dialog', { name: /Погодинна карта/ });
  await expect(reopened).toBeVisible();
  await expect(reopened.getByLabel('Сеча 4:00')).toHaveValue('250');
});

test('modal a11y (name, live region, focus trap, reduced motion) and sticky panels', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto(`/icu/nurse/episode/${EPISODE_ID}`);
  await expect(page.getByText('Показник / година')).toBeVisible();

  await page.getByRole('button', { name: 'Розгорнути на весь екран' }).click();

  const dialog = page.getByRole('dialog', { name: /Погодинна карта/ });
  await expect(dialog).toBeVisible();
  await expect(dialog).toHaveAttribute('aria-labelledby');
  await expect(dialog.getByRole('status')).toHaveCount(1);

  for (let i = 0; i < 10; i += 1) {
    await page.keyboard.press('Tab');
    const inside = await page.evaluate(() => {
      const popup = document.querySelector('[data-fullscreen="true"]');
      return popup?.contains(document.activeElement) ?? false;
    });
    expect(inside).toBe(true);
  }

  const scrollBox = dialog.locator('div.overflow-auto');
  await expect(scrollBox).toBeVisible();
  const corner = dialog.getByText('Показник / година');
  const labelCol = dialog.getByRole('cell', { name: 'ЧСС' });
  const cornerBefore = await corner.boundingBox();
  const labelBefore = await labelCol.boundingBox();
  await scrollBox.evaluate((el) => {
    el.scrollTop = 400;
    el.scrollLeft = 250;
  });
  await expect.poll(() => scrollBox.evaluate((el) => el.scrollTop)).toBeGreaterThan(0);
  const cornerAfter = await corner.boundingBox();
  const labelAfter = await labelCol.boundingBox();
  const cornerDy = Math.abs((cornerAfter?.y ?? -1) - (cornerBefore?.y ?? -1));
  const labelDx = Math.abs((labelAfter?.x ?? -1) - (labelBefore?.x ?? -1));
  expect(cornerDy).toBeLessThanOrEqual(4);
  expect(labelDx).toBeLessThanOrEqual(4);
});
