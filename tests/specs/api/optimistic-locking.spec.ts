import { test, expect } from '@playwright/test';

const API = 'http://localhost:8085/api';

async function getToken(request: any) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'doctor1', password: 'doctor123' },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('Optimistic Locking - 409 Conflict', () => {
  test('returns 409 when updating episode with stale version', async ({ request }) => {
    const token = await getToken(request);
    await request.post(`${API}/mis/error-mode?mode=none`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    // Get current version of seeded episode (Петренко)
    const getRes = await request.get(`${API}/episodes/a1111111-1111-1111-1111-111111111111`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!getRes.ok()) {
      return;
    }
    const episode = await getRes.json();
    const oldVersion = episode.version;

    // First update with current version succeeds (version n -> n+1)
    // Use a unique dischargeDate to guarantee a column change and version bump
    const update1 = await request.patch(`${API}/episodes/a1111111-1111-1111-1111-111111111111`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { version: oldVersion, dischargeDate: '2026-07-19T10:00:00' },
    });
    expect(update1.status()).toBe(204);

    // Second update with stale version -> 409 Conflict
    const update2 = await request.patch(`${API}/episodes/a1111111-1111-1111-1111-111111111111`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { version: oldVersion },
    });
    expect(update2.status()).toBe(409);

    const errorBody = await update2.json();
    expect(errorBody).toHaveProperty('code', 'VERSION_CONFLICT');
    expect(errorBody).toHaveProperty('message');
    expect(errorBody).toHaveProperty('correlationId');
  });

  test('update succeeds with current version', async ({ request }) => {
    const token = await getToken(request);
    await request.post(`${API}/mis/error-mode?mode=none`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    // Get current version of a different seeded episode (Коваленко, version=0)
    const getRes = await request.get(`${API}/episodes/a2222222-2222-2222-2222-222222222222`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(getRes.ok()).toBeTruthy();
    const episode = await getRes.json();
    const currentVersion = episode.version;

    // Update with current version -> 204 No Content
    const updateRes = await request.patch(`${API}/episodes/a2222222-2222-2222-2222-222222222222`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { version: currentVersion },
    });
    expect(updateRes.status()).toBe(204);
  });
});
