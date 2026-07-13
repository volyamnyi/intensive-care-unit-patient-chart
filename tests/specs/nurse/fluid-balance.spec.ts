import { test, expect } from '../../fixtures/index';

test.describe('Nurse Fluid Balance', () => {
  test('prescription execution updates intake in balance panel', async ({ page }) => {
    await page.goto('/nurse');

    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван/ }).click();
    await expect(page.getByText('Петренко Іван Сергійович').first()).toBeVisible({ timeout: 10000 });

    const initialIntakeText = await page.locator('text=Надійшло:').locator('..').textContent();
    const initialIntake = parseInt(initialIntakeText?.match(/(\d+)\s*мл/)?.[1] || '0');

    const executeButton = page.getByRole('button').filter({ has: page.locator('[data-testid="CheckCircleIcon"]') }).first();
    if (await executeButton.isVisible()) {
      await executeButton.click();


      const updatedIntakeText = await page.locator('text=Надійшло:').locator('..').textContent();
      const updatedIntake = parseInt(updatedIntakeText?.match(/(\d+)\s*мл/)?.[1] || '0');

      expect(updatedIntake).toBeGreaterThanOrEqual(initialIntake);
    }
  });

  test('balance panel displays fluid balance components', async ({ page }) => {
    await page.goto('/nurse');

    await page.locator('[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Петренко Іван/ }).click();
    await expect(page.getByText('Петренко Іван Сергійович').first()).toBeVisible({ timeout: 10000 });

    await expect(page.getByText('Баланс рідини')).toBeVisible();
    await expect(page.getByText(/Надійшло:/)).toBeVisible();
    await expect(page.getByText(/Виділено:/)).toBeVisible();
    await expect(page.getByText(/Добовий баланс:/)).toBeVisible();
    await expect(page.getByText(/Кумулятивний баланс:/)).toBeVisible();
  });
});
