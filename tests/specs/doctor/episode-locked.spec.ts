import { test, expect } from '../../fixtures/index';

test.describe('Clinical day locking (vitals editability)', () => {
  test('vitals inputs are disabled on a NURSE_SIGNED day for doctor', async ({ page }) => {
    // a1111111 day 2 (b1111112) is NURSE_SIGNED
    await page.goto('/icu/doctor/episode/a1111111-1111-1111-1111-111111111111');

    // Select День 2 (NURSE_SIGNED)
    await page.getByText('Доба 2').click();

    // All vital cells should be disabled on a locked day
    const hrCell = page.getByLabel('ЧСС 1:00');
    await expect(hrCell).toBeDisabled({ timeout: 10000 });
  });

  test('urine output cell is enabled on an OPEN day for nurse', async ({ nursePage }) => {
    // a3333333 day 1 (b3333333) is OPEN
    await nursePage.goto('/icu/nurse/episode/a3333333-3333-3333-3333-333333333333');

    // Loss-row cells are editable by nurse on OPEN day
    const urineCell = nursePage.getByLabel('Сеча 1:00');
    await expect(urineCell).toBeEnabled({ timeout: 10000 });
  });
});
