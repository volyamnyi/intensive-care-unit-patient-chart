import { test, expect } from '../../fixtures/index';

const API = 'http://localhost:8085/api';
const BACKEND = 'http://localhost:8085';
const SEED_DAY_ID = 'b3333333-3333-3333-3333-333333333333';
const FAKE_DAY_PART_ID = 'ffffffff-ffff-ffff-ffff-fffffffffff1';

async function getToken(request: any, login: string, password: string) {
  const res = await request.post(`${API}/auth/login`, {
    data: { login, password },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('API Security Rules', () => {
  test('unauthenticated requests to secured endpoints return 401', async ({ request }) => {
    const res = await request.get(`${API}/episodes`);
    expect(res.status()).toBe(401);
  });

  test('unauthenticated requests to non-API paths return 401', async ({ request }) => {
    const res = await request.get(`${BACKEND}/health`);
    expect(res.status()).toBe(401);
  });

  test('swagger UI and OpenAPI docs are publicly accessible', async ({ request }) => {
    const docs = await request.get(`${BACKEND}/api-docs`);
    expect(docs.status()).toBe(200);

    const swaggerUi = await request.get(`${BACKEND}/swagger-ui/index.html`);
    expect(swaggerUi.status()).toBe(200);
  });

  test('nurse cannot create episodes (prescriber role required)', async ({ request }) => {
    const token = await getToken(request, 'nurse1', 'nurse123');

    // Valid body on purpose: argument validation runs before method security,
    // so an invalid body would yield 400 (validation) instead of 403 (denied).
    const res = await request.post(`${API}/episodes`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { patientId: 1001, admissionDate: '2026-08-07T10:00:00' },
    });

    expect(res.status()).toBe(403);
  });

  test('admin cannot create episodes (prescriber role required)', async ({ request }) => {
    const token = await getToken(request, 'admin', 'admin123');

    const res = await request.post(`${API}/episodes`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { patientId: 1001, admissionDate: '2026-08-07T10:00:00' },
    });

    expect(res.status()).toBe(403);
  });

  test('doctor passes the episode creation rule (validation rejects empty body)', async ({ request }) => {
    const token = await getToken(request, 'doctor1', 'doctor123');

    const res = await request.post(`${API}/episodes`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {},
    });

    expect([400, 422]).toContain(res.status());
  });

  test('nurse cannot create medical orders', async ({ request }) => {
    const token = await getToken(request, 'nurse1', 'nurse123');

    // Valid body: method security must deny before any validation can pass.
    const res = await request.post(`${API}/clinical-days/${SEED_DAY_ID}/orders`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { category: 'Ліки', drugName: 'Тест', dose: '1', unit: 'мл', route: 'в/в', frequency: '1 раз', startTime: '2026-08-07T10:00:00' },
    });

    expect(res.status()).toBe(403);
  });

  test('audit log endpoints require the administrator role', async ({ request }) => {
    const doctor = await getToken(request, 'doctor1', 'doctor123');
    const doctorRes = await request.get(`${API}/audit`, {
      headers: { Authorization: `Bearer ${doctor}` },
    });
    expect(doctorRes.status()).toBe(403);

    const nurse = await getToken(request, 'nurse1', 'nurse123');
    const nurseRes = await request.get(`${API}/audit`, {
      headers: { Authorization: `Bearer ${nurse}` },
    });
    expect(nurseRes.status()).toBe(403);

    const admin = await getToken(request, 'admin', 'admin123');
    const adminRes = await request.get(`${API}/audit`, {
      headers: { Authorization: `Bearer ${admin}` },
    });
    expect(adminRes.ok()).toBeTruthy();
  });

  test('day-part completion requires the executor role (nurse)', async ({ request }) => {
    const doctor = await getToken(request, 'doctor1', 'doctor123');
    const doctorRes = await request.put(
      `${API}/prescriptions/day-parts/${FAKE_DAY_PART_ID}/complete`,
      { headers: { Authorization: `Bearer ${doctor}` } },
    );
    expect(doctorRes.status()).toBe(403);

    const nurse = await getToken(request, 'nurse1', 'nurse123');
    const nurseRes = await request.put(
      `${API}/prescriptions/day-parts/${FAKE_DAY_PART_ID}/complete`,
      { headers: { Authorization: `Bearer ${nurse}` } },
    );
    expect(nurseRes.status()).toBe(404);
  });

  test('episode search is readable by all clinical roles', async ({ request }) => {
    const doctor = await getToken(request, 'doctor1', 'doctor123');
    const doctorRes = await request.get(`${API}/episodes`, {
      headers: { Authorization: `Bearer ${doctor}` },
    });
    expect(doctorRes.ok()).toBeTruthy();

    const nurse = await getToken(request, 'nurse1', 'nurse123');
    const nurseRes = await request.get(`${API}/episodes`, {
      headers: { Authorization: `Bearer ${nurse}` },
    });
    expect(nurseRes.ok()).toBeTruthy();
  });

  test('order plan and cancel endpoints require the prescriber role', async ({ request }) => {
    const orderId = 'd3333001-3333-3333-0000-333333330001';

    const nurse = await getToken(request, 'nurse1', 'nurse123');
    const nursePlan = await request.put(`${API}/orders/${orderId}/plan`, {
      headers: { Authorization: `Bearer ${nurse}` },
      data: { hour: 6, dose: '500' },
    });
    expect(nursePlan.status()).toBe(403);

    const nurseFinish = await request.put(`${API}/orders/${orderId}/plan/finish`, {
      headers: { Authorization: `Bearer ${nurse}` },
      data: { hour: 6 },
    });
    expect(nurseFinish.status()).toBe(403);

    const nurseCancel = await request.put(`${API}/orders/${orderId}/cancel`, {
      headers: { Authorization: `Bearer ${nurse}` },
      data: { hour: 6 },
    });
    expect(nurseCancel.status()).toBe(403);

    const doctor = await getToken(request, 'doctor1', 'doctor123');
    const doctorPlan = await request.put(`${API}/orders/${orderId}/plan`, {
      headers: { Authorization: `Bearer ${doctor}` },
      data: { hour: 6, dose: '500' },
    });
    expect(doctorPlan.ok()).toBeTruthy();
  });

  test('order execution endpoints require the executor role (nurse)', async ({ request }) => {
    const orderId = 'd3333001-3333-3333-0000-333333330001';
    const doctor = await getToken(request, 'doctor1', 'doctor123');

    const doctorExecute = await request.post(`${API}/orders/${orderId}/execute`, {
      headers: { Authorization: `Bearer ${doctor}` },
      data: { hour: 6, actualDose: '500' },
    });
    expect(doctorExecute.status()).toBe(403);

    const doctorFinish = await request.post(`${API}/orders/${orderId}/execute/finish`, {
      headers: { Authorization: `Bearer ${doctor}` },
      data: { hour: 6 },
    });
    expect(doctorFinish.status()).toBe(403);

    const doctorPatch = await request.patch(`${API}/executions/ffffffff-ffff-ffff-ffff-fffffffffff1`, {
      headers: { Authorization: `Bearer ${doctor}` },
      data: { version: 0 },
    });
    expect(doctorPatch.status()).toBe(403);
  });
});
