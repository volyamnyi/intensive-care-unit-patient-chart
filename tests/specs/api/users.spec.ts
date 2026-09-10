import { test, expect } from '@playwright/test';
import { testUser } from '../../helpers/test-users';

const API = 'http://localhost:8085/api';

async function getToken(request: any, login = testUser(1).login, password = testUser(1).password) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login, password },
  });
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  return body.token as string;
}

test.describe('Users API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getToken(request);
  });

  test('get current user via /me still works', async ({ request }) => {
    const res = await request.get(`${API}/users/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.login).toBe(testUser(1).login);
  });

  test('get current user without auth returns 401', async ({ request }) => {
    const res = await request.get(`${API}/users/me`);
    expect(res.status()).toBe(401);
  });
});
