import { test, expect } from '@playwright/test';

const API = 'http://localhost:8085/api';

async function getToken(request: any, login = 'doctor1', password = 'doctor123') {
  const res = await request.post(`${API}/auth/login`, {
    data: { login, password },
  });
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  return body.token as string;
}

test.describe('MIS Error Scenarios', () => {
  let token: string;

  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async ({ request }) => {
    token = await getToken(request);
  });

  test.afterEach(async ({ request }) => {
    await request.post(`${API}/mis/error-mode?mode=none`, {
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => {});
  });

  test('mis error mode unavailable - patients returns error', async ({ request }) => {
    const modeRes = await request.post(`${API}/mis/error-mode?mode=unavailable`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(modeRes.ok()).toBeTruthy();

    const patientRes = await request.get(`${API}/patients`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(patientRes.status()).toBe(500);
  });

  test('mis error mode not_found - patients returns error', async ({ request }) => {
    const modeRes = await request.post(`${API}/mis/error-mode?mode=not_found`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(modeRes.ok()).toBeTruthy();

    const patientRes = await request.get(`${API}/patients`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(patientRes.status()).toBe(500);
  });

  test('mis error mode timeout - patients returns error', async ({ request }) => {
    const modeRes = await request.post(`${API}/mis/error-mode?mode=timeout`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(modeRes.ok()).toBeTruthy();

    const patientRes = await request.get(`${API}/patients`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(patientRes.status()).toBe(500);
  });

  test('mis recovery after error mode - none returns success', async ({ request }) => {
    // The error mode endpoints return 500 for all error types, not specific HTTP codes
    // This is by design since MockMisServiceImpl throws RuntimeException
    await request.post(`${API}/mis/error-mode?mode=unavailable`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    await request.get(`${API}/patients`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    const modeRes = await request.post(`${API}/mis/error-mode?mode=none`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(modeRes.ok()).toBeTruthy();

    const patientRes = await request.get(`${API}/patients`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(patientRes.ok()).toBeTruthy();
    const body = await patientRes.json();
    expect(Array.isArray(body)).toBeTruthy();
    expect(body.length).toBeGreaterThanOrEqual(3);
  });

  test('mis error mode without auth returns 401', async ({ request }) => {
    const res = await request.post(`${API}/mis/error-mode?mode=none`);
    expect(res.status()).toBe(401);
  });
});
