import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';
const EPISODE_ID = 'a1111111-1111-1111-1111-111111111111';
const NURSE_SIGNED_DAY_ID = 'b1111112-1111-1111-1111-111111111111';

async function getHodToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'head1', password: 'head123' },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('HOD Clinical Day Reopen', () => {
  test('reopens a nurse-signed clinical day via API', async ({ request }) => {
    const token = await getHodToken(request);

    const dayRes = await request.get(`${API}/clinical-days/${NURSE_SIGNED_DAY_ID}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(dayRes.ok()).toBeTruthy();
    const day = await dayRes.json();
    expect(day.status).toBe('NURSE_SIGNED');

    const reopenRes = await request.post(`${API}/clinical-days/${NURSE_SIGNED_DAY_ID}/reopen`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { reason: 'E2E test reopen', version: day.version },
    });
    expect(reopenRes.ok()).toBeTruthy();
    const reopened = await reopenRes.json();
    expect(reopened.status).toBe('REOPENED');
    expect(reopened.doctorSigned).toBe(false);
    expect(reopened.nurseSigned).toBe(false);
  });

  test('HOD can open episode page and view clinical day timeline', async ({ page }) => {
    await page.goto('/doctor');
    await expect(page.getByText('Активні пацієнти ВАІТ')).toBeVisible({ timeout: 10000 });

    await page.goto(`/doctor/episode/${EPISODE_ID}`);
    await expect(page.getByText('Статус:').first()).toBeVisible({ timeout: 10000 });
  });
});
