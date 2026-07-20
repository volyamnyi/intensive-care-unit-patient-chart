import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';

async function getDoctorToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'doctor1', password: 'doctor123' },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('Scales Full', () => {
  test('scales section shows empty state and backend returns scale list', async ({ page, request }) => {
    const token = await getDoctorToken(request);
    const res = await request.get(`${API}/scales`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const scales = await res.json();
    expect(Array.isArray(scales)).toBeTruthy();

    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    await page.getByText('Шкали').first().click();
    await expect(page.getByText('Немає даних шкал').or(page.getByText('Не заповнено'))).toBeVisible();
  });

  test('scales section is accessible from episode page', async ({ page }) => {
    await page.goto('/doctor');
    await page.getByRole('button', { name: 'Відкрити' }).first().click();
    await expect(page).toHaveURL(/\/doctor\/episode\//);

    const scalesSection = page.getByText('Шкали').first();
    await expect(scalesSection).toBeVisible();
    await scalesSection.click();
    await expect(page.getByText('Немає даних шкал').or(page.getByText('Не заповнено'))).toBeVisible();
  });
});
