import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';
const EPISODE_ID = 'a1111111-1111-1111-1111-111111111111';
const DOCTOR_SIGNED_DAY_ID = 'b1111113-1111-1111-1111-111111111111';

async function getHodToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'head1', password: 'head123' },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('Reopen and Re-sign Full Chain', () => {
  test('HOD reopens a doctor-signed clinical day via API', async ({ request }) => {
    const token = await getHodToken(request);

    const dayRes = await request.get(`${API}/clinical-days/${DOCTOR_SIGNED_DAY_ID}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(dayRes.ok()).toBeTruthy();
    const day = await dayRes.json();
    expect(day.status).toBe('DOCTOR_SIGNED');

    const reopenRes = await request.post(`${API}/clinical-days/${DOCTOR_SIGNED_DAY_ID}/reopen`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { reason: 'E2E test reopen for re-sign', version: day.version },
    });
    expect(reopenRes.ok()).toBeTruthy();
    const reopened = await reopenRes.json();
    expect(reopened.status).toBe('REOPENED');
    expect(reopened.doctorSigned).toBe(false);
    expect(reopened.nurseSigned).toBe(false);
    expect(reopened.closedAt).toBeNull();
  });

  test('nurse can re-sign after reopen', async ({ nursePage }) => {
    await nursePage.goto(`/nurse/episode/${EPISODE_ID}`);
    await expect(nursePage.getByText('Підписати добу')).toBeVisible({ timeout: 10000 });

    await nursePage.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(nursePage.getByText('Після підписання доба стане read-only')).toBeVisible();
    await nursePage.getByRole('button', { name: 'Підписати' }).click();
    await expect(nursePage.getByText('Підписана медсестрою').first()).toBeVisible({ timeout: 10000 });
  });

  test('doctor can re-sign after nurse re-signed', async ({ page }) => {
    await page.goto(`/doctor/episode/${EPISODE_ID}`);
    await expect(page.getByText('Підписана медсестрою').first()).toBeVisible({ timeout: 10000 });

    await page.getByRole('button', { name: 'Підписати добу' }).click();
    await expect(page.getByText('Після підписання доба стане read-only')).toBeVisible();
    await page.getByRole('button', { name: 'Підписати' }).click();
    await expect(page.getByText('Підписана').first()).toBeVisible({ timeout: 10000 });
  });
});
