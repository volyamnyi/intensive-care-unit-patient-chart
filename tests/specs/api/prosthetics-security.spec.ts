import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';
const BACKEND = 'http://localhost:8085';

async function getToken(request: any, login: string, password: string) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login, password },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('Prosthetics API Security Rules', () => {
  test('unauthenticated requests to prosthetics endpoints return 401', async ({ request }) => {
    const res = await request.get(`${API}/prosthesis-manufacturing/instances`);
    expect([401, 403]).toContain(res.status());
  });

  test('NURSE role cannot access prosthetics endpoints (403)', async ({ request }) => {
    const token = await getToken(request, 'nurse1', 'nurse123');

    const res = await request.get(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(403);
  });

  test('DOCTOR role cannot access prosthetics endpoints (403)', async ({ request }) => {
    const token = await getToken(request, 'doctor1', 'doctor123');

    const res = await request.get(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(403);
  });

  test('PROSTHETIST role can access prosthetics endpoints (200)', async ({ request }) => {
    const token = await getToken(request, 'prosthetist1', 'doctor123');

    const res = await request.get(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
  });

  test('PROSTHETICS_ADMINISTRATOR role can access prosthetics endpoints (200)', async ({ request }) => {
    const token = await getToken(request, 'prosthetics_admin1', 'doctor123');

    const res = await request.get(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
  });

  test('ADMIN role cannot access prosthetics endpoints (403)', async ({ request }) => {
    const token = await getToken(request, 'admin', 'admin123');

    const res = await request.get(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(403);
  });
});