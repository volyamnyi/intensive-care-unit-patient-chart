import { test, expect } from '@playwright/test';

const API = 'http://localhost:8085/api';

async function getToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'doctor1', password: 'doctor123' },
  });
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  return body.token as string;
}

test.describe('Patient API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getToken(request);
    await request.post(`${API}/mis/error-mode?mode=none`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  });

  test('search returns patients', async ({ request }) => {
    const res = await request.get(`${API}/patients`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(Array.isArray(body)).toBeTruthy();
  });

  test('search by name filters results', async ({ request }) => {
    const res = await request.get(`${API}/patients?query=Петренко`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.some((p: any) => p.fullName?.includes('Петренко'))).toBeTruthy();
  });
});
