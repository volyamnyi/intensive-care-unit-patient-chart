import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';

async function getHodToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'head1', password: 'head123' },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('HOD Clinical Day Reopen', () => {
  test('reopens a nurse-signed clinical day via API', async ({ request }) => {
    // Uses a2222222 day 2 (b4444444) — starts NURSE_SIGNED after seed reset.
    const token = await getHodToken(request);

    const dayRes = await request.get(`${API}/clinical-days/b4444444-4444-4444-4444-444444444444`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(dayRes.ok()).toBeTruthy();
    const day = await dayRes.json();

    if (day.status === 'NURSE_SIGNED') {
      const reopenRes = await request.post(`${API}/clinical-days/b4444444-4444-4444-4444-444444444444/reopen`, {
        headers: { Authorization: `Bearer ${token}` },
        data: { reason: 'E2E test reopen', version: day.version },
      });
      expect(reopenRes.status()).toBe(204);
    } else {
      expect(day.status).toBe('REOPENED');
    }

    const reopened = await (await request.get(`${API}/clinical-days/b4444444-4444-4444-4444-444444444444`, {
      headers: { Authorization: `Bearer ${token}` },
    })).json();
    expect(reopened.status).toBe('REOPENED');
    expect(reopened.doctorSigned).toBe(false);
    expect(reopened.nurseSigned).toBe(false);
  });

  test('HOD can open episode page and see reopen button on a1111111 day 2', async ({ page }) => {
    // a1111111 day 2 (b1111112) is NURSE_SIGNED — stable seed, never reopened here.
    await page.goto('/doctor/episode/a1111111-1111-1111-1111-111111111111');

    // Select the NURSE_SIGNED day (Доба 2) so the reopen action appears.
    await page.getByText('Доба 2').click();
    await expect(page.getByRole('button', { name: 'Перевідкрити' })).toBeVisible({ timeout: 10000 });
  });

  test('HOD reopen form blocks empty reason (regression F2/UC-14)', async ({ hodPage }) => {
    await hodPage.goto('/doctor/episode/a1111111-1111-1111-1111-111111111111');

    await hodPage.getByText('Доба 2').click();
    await expect(hodPage.getByRole('button', { name: 'Перевідкрити' })).toBeVisible({ timeout: 10000 });

    await hodPage.getByRole('button', { name: 'Перевідкрити' }).click();
    const reasonInput = hodPage.getByPlaceholder('Причина');
    await expect(reasonInput).toBeVisible();

    const submit = hodPage.getByRole('button', { name: 'Перевідкрити' });
    await expect(submit).toBeDisabled();

    await reasonInput.fill('E2E reason');
    await expect(submit).toBeEnabled();
  });
});
