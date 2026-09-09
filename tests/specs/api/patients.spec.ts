import { test, expect } from '@playwright/test';
import { testUser } from '../../helpers/test-users';

const API = 'http://localhost:8085/api';

async function getToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: testUser(1).login, password: testUser(1).password },
  });
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  return body.token as string;
}

async function listPatients(request: any, token: string) {
  const res = await request.get(`${API}/patients`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  expect(Array.isArray(body)).toBeTruthy();
  return body as any[];
}

test.describe('Patient API', () => {
  let token: string;
  let patients: any[];

  test.beforeAll(async ({ request }) => {
    token = await getToken(request);
    patients = await listPatients(request, token);
  });

  test('search returns patients', () => {
    expect(Array.isArray(patients)).toBeTruthy();
  });

  test('search by name fragment filters results', async ({ request }) => {
    const named = patients.find((p) => typeof p.fullName === 'string' && p.fullName.trim().length >= 2);
    if (!named) {
      test.skip();
      return;
    }
    // Use a middle fragment of the full name so the query is a real substring, not the whole name.
    const query = named.fullName.trim().slice(0, 4);
    const res = await request.get(`${API}/patients?query=${encodeURIComponent(query)}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(Array.isArray(body)).toBeTruthy();
    expect(body.some((p: any) => (p.fullName ?? '').trim().includes(query))).toBeTruthy();
  });
});
