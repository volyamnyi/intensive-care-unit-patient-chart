import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

test.describe('Doctor Notes', () => {
  test('adds a clinical note to a patient day', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await page.getByText('�������').first().click();

    const noteText = '������� ������� �� ����� � E2E ��������';
    await page.getByLabel('���� �������').fill(noteText);
    await page.getByRole('button', { name: '������ �������' }).click();

    await expect(page.getByText(noteText).first()).toBeVisible({ timeout: 10000 });
  });

  test('shows note field when section is open', async ({ page }) => {
    await page.goto(`/icu/doctor/episode/${EPISODE_ID}`);

    await expect(page.getByLabel('���� �������')).toBeVisible();
  });
});
