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

test.describe('MIS Users API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getToken(request);
  });

  test('get user by ID returns doctor', async ({ request }) => {
    const res = await request.get(`${API}/users/00000000-0000-0000-0000-000000000011`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.fullName).toContain('Мельник');
  });

  test('get user by ID returns nurse', async ({ request }) => {
    const res = await request.get(`${API}/users/00000000-0000-0000-0000-000000000013`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.fullName).toContain('Ткаченко');
  });

  test('get user by ID returns HOD', async ({ request }) => {
    const res = await request.get(`${API}/users/00000000-0000-0000-0000-000000000015`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.fullName).toContain('Гончарук');
  });

  test('get user with unknown ID returns 404', async ({ request }) => {
    const res = await request.get(`${API}/users/00000000-0000-0000-0000-000000009999`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(404);
  });

  test('get user without auth returns 403', async ({ request }) => {
    const res = await request.get(`${API}/users/00000000-0000-0000-0000-000000000011`);
    expect(res.status()).toBe(403);
  });

  test('get current user via /me still works', async ({ request }) => {
    const res = await request.get(`${API}/users/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.login).toBe('doctor1');
  });
});
