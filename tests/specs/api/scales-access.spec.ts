import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';
const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';
const APACHE_SCALE_ID = 'c1111111-1111-1111-1111-111111111104';
const SOFA_SCALE_ID = 'c1111111-1111-1111-1111-111111111103';
const CLINICAL_DAY_ID = 'b3333333-3333-3333-3333-333333333333';

async function getToken(request: any, login: string, password: string) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login, password },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('Scales API Access Control', () => {
  test('doctor can calculate APACHE II via episode endpoint', async ({ request }) => {
    const token = await getToken(request, 'doctor1', 'doctor123');

    const res = await request.post(`${API}/episodes/${EPISODE_ID}/scales/calculate`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        scaleId: APACHE_SCALE_ID,
        clinicalDayId: CLINICAL_DAY_ID,
        rawData: {
          age: 65, temperatureC: 38.5, heartRate: 110,
          respiratoryRate: 28, meanArterialPressure: 70,
        },
      },
    });

    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body).toHaveProperty('result');
    expect(body).toHaveProperty('episodeId', EPISODE_ID);
  });

  test('nurse is blocked from APACHE II calculation', async ({ request }) => {
    const token = await getToken(request, 'nurse1', 'nurse123');

    const res = await request.post(`${API}/episodes/${EPISODE_ID}/scales/calculate`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        scaleId: APACHE_SCALE_ID,
        clinicalDayId: CLINICAL_DAY_ID,
        rawData: { age: 50, temperatureC: 37.0 },
      },
    });

    expect(res.status()).toBe(403);
  });

  test('nurse is blocked from creating episode-level SOFA', async ({ request }) => {
    const token = await getToken(request, 'nurse1', 'nurse123');

    const res = await request.post(`${API}/episodes/${EPISODE_ID}/scales`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { scaleId: SOFA_SCALE_ID, result: '5' },
    });

    expect(res.status()).toBe(403);
  });

  test('nurse can create daily-scale results', async ({ request }) => {
    const token = await getToken(request, 'nurse1', 'nurse123');

    const res = await request.post(`${API}/clinical-days/${CLINICAL_DAY_ID}/scales`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { scaleId: SOFA_SCALE_ID, result: '3' },
    });

    expect(res.ok()).toBeTruthy();
  });

  test('doctor can fetch episode-level scale results', async ({ request }) => {
    const token = await getToken(request, 'doctor1', 'doctor123');

    const res = await request.get(`${API}/episodes/${EPISODE_ID}/scales`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(Array.isArray(body)).toBeTruthy();
  });

  test('nurse can fetch episode-level scale results (read-only)', async ({ request }) => {
    const token = await getToken(request, 'nurse1', 'nurse123');

    const res = await request.get(`${API}/episodes/${EPISODE_ID}/scales`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(res.ok()).toBeTruthy();
  });
});
