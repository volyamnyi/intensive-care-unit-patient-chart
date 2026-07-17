import { test, expect } from '@playwright/test';

const API = 'http://localhost:8085/api';

test.describe('API Error Handling', () => {
  test('returns 401 for unauthenticated requests', async ({ request }) => {
    const res = await request.get(`${API}/episodes`);
    expect(res.status()).toBe(401);
  });

  test('returns 404 for non-existent episode', async ({ request }) => {
    const res = await request.post(`${API}/auth/login`, {
      data: { login: 'doctor1', password: 'doctor123' },
    });
    expect(res.ok()).toBeTruthy();
    const { token } = await res.json();

    const episodeRes = await request.get(`${API}/episodes/00000000-0000-0000-0000-000000009999`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(episodeRes.status()).toBe(404);
  });

  test('returns 404 for non-existent clinical day', async ({ request }) => {
    const res = await request.post(`${API}/auth/login`, {
      data: { login: 'doctor1', password: 'doctor123' },
    });
    expect(res.ok()).toBeTruthy();
    const { token } = await res.json();

    const dayRes = await request.get(`${API}/clinical-days/00000000-0000-0000-0000-000000009999`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(dayRes.status()).toBe(404);
  });

  test('returns 401 with invalid token', async ({ request }) => {
    const res = await request.get(`${API}/episodes`, {
      headers: { Authorization: 'Bearer invalid-token-here' },
    });
    expect(res.status()).toBe(401);
  });

  test('returns 401 for empty login request', async ({ request }) => {
    const res = await request.post(`${API}/auth/login`, {
      data: {},
    });
    expect(res.status()).toBe(401);
  });
});
