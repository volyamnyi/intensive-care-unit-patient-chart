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

// MisService.getUser looks up the REAL MIS by its numeric user id (spzIBUserDetails).
// There is no "list MIS users" endpoint the app exposes, so discover a valid id dynamically.
async function findAnExistingMisUserId(request: any, token: string, max: number): Promise<number | null> {
  for (let id = 1; id <= max; id++) {
    const res = await request.get(`${API}/users/${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok()) {
      const body = await res.json();
      if (typeof body.fullName === 'string' && body.fullName.trim().length > 0) {
        return id;
      }
    }
  }
  return null;
}

test.describe('MIS Users API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getToken(request);
  });

  test('get user by ID returns a valid MIS user', async ({ request }) => {
    const id = await findAnExistingMisUserId(request, token, 300);
    if (id === null) {
      test.skip();
      return;
    }
    const res = await request.get(`${API}/users/${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.id).toBe(id);
    expect(typeof body.fullName).toBe('string');
    expect(body.fullName.trim().length).toBeGreaterThan(0);
  });

  test('get user with unknown ID returns 404', async ({ request }) => {
    const res = await request.get(`${API}/users/99999999999`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(404);
  });

  test('get user without auth returns 401', async ({ request }) => {
    const res = await request.get(`${API}/users/1`);
    expect(res.status()).toBe(401);
  });

  test('get current user via /me still works', async ({ request }) => {
    const res = await request.get(`${API}/users/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.login).toBe(testUser(1).login);
  });
});
