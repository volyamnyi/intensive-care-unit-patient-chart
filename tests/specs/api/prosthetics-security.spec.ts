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
    const endpoints = [
      `${API}/prosthesis-manufacturing/instances`,
      `${API}/prosthesis-manufacturing/patients`,
      `${API}/prosthesis-manufacturing/orders`,
      `${API}/prosthesis-manufacturing/templates`,
    ];

    for (const endpoint of endpoints) {
      const res = await request.get(endpoint);
      expect([401, 403]).toContain(res.status());
    }
  });

  test('NURSE role cannot access prosthetics endpoints (403)', async ({ request }) => {
    const token = await getToken(request, 'nurse1', 'nurse123');

    const endpoints = [
      { method: 'GET', url: `${API}/prosthesis-manufacturing/instances` },
      { method: 'GET', url: `${API}/prosthesis-manufacturing/patients` },
      { method: 'GET', url: `${API}/prosthesis-manufacturing/orders` },
      { method: 'GET', url: `${API}/prosthesis-manufacturing/templates` },
      { method: 'POST', url: `${API}/prosthesis-manufacturing/instances`, data: {} },
      { method: 'POST', url: `${API}/prosthesis-manufacturing/templates`, data: {} },
    ];

    for (const { method, url, data } of endpoints) {
      const res = await request[method.toLowerCase()](url, {
        headers: { Authorization: `Bearer ${token}` },
        data,
      });
      expect(res.status()).toBe(403);
    }
  });

  test('DOCTOR role cannot access prosthetics endpoints (403)', async ({ request }) => {
    const token = await getToken(request, 'doctor1', 'doctor123');

    const res = await request.get(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(403);
  });

  test('PROSTHETIST role can access prosthetics endpoints (200)', async ({ request }) => {
    const token = await getToken(request, 'prosthetist1', 'prosthetist123');

    const res = await request.get(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();

    const patients = await request.get(`${API}/prosthesis-manufacturing/patients`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(patients.ok()).toBeTruthy();

    const orders = await request.get(`${API}/prosthesis-manufacturing/orders`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(orders.ok()).toBeTruthy();

    const templates = await request.get(`${API}/prosthesis-manufacturing/templates`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(templates.ok()).toBeTruthy();
  });

  test('PROSTHETICS_ADMINISTRATOR role can access prosthetics endpoints (200)', async ({ request }) => {
    const token = await getToken(request, 'prosthetics_admin1', 'prosthetist123');

    const res = await request.get(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.ok()).toBeTruthy();

    const templates = await request.get(`${API}/prosthesis-manufacturing/templates`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(templates.ok()).toBeTruthy();
  });

  test('PROSTHETIST can create flow instance', async ({ request }) => {
    const token = await getToken(request, 'prosthetist1', 'prosthetist123');

    const res = await request.post(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        orderId: 'b0000001-0000-4000-8000-000000000001',
        templateId: 'c0000001-0000-4000-8000-000000000001',
      },
    });
    expect([200, 201]).toContain(res.status());
  });

  test('PROSTHETIST can complete step execution', async ({ request }) => {
    const token = await getToken(request, 'prosthetist1', 'prosthetist123');

    // First create an instance
    const createRes = await request.post(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        orderId: 'b0000001-0000-4000-8000-000000000001',
        templateId: 'c0000001-0000-4000-8000-000000000001',
      },
    });
    expect([200, 201]).toContain(createRes.status());
    const instance = await createRes.json();

    // Get step executions for this instance
    const stepsRes = await request.get(`${API}/prosthesis-manufacturing/instances/${instance.id}/step-executions`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(stepsRes.ok()).toBeTruthy();
    const steps = await stepsRes.json();

    if (steps.length > 0) {
      const stepId = steps[0].id;
      const completeRes = await request.post(`${API}/prosthesis-manufacturing/step-executions/${stepId}/complete`, {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          values: [
            { elementId: 'f0000001-0000-4000-8000-000000000001', value: '20' },
            { elementId: 'f0000002-0000-4000-8000-000000000002', value: 'термопласт' },
          ],
        },
      });
      expect([200, 201]).toContain(completeRes.status());
    }
  });

  test('PROSTHETICS_ADMINISTRATOR can make gate decision', async ({ request }) => {
    const token = await getToken(request, 'prosthetics_admin1', 'prosthetist123');

    // First create an instance as prosthetist
    const prosthetistToken = await getToken(request, 'prosthetist1', 'prosthetist123');
    const createRes = await request.post(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
      data: {
        orderId: 'b0000001-0000-4000-8000-000000000001',
        templateId: 'c0000001-0000-4000-8000-000000000001',
      },
    });
    expect([200, 201]).toContain(createRes.status());
    const instance = await createRes.json();

    // Complete steps to reach quality gate
    const stepsRes = await request.get(`${API}/prosthesis-manufacturing/instances/${instance.id}/step-executions`, {
      headers: { Authorization: `Bearer ${prosthetistToken}` },
    });
    const steps = await stepsRes.json();

    for (const step of steps) {
      await request.post(`${API}/prosthesis-manufacturing/step-executions/${step.id}/complete`, {
        headers: { Authorization: `Bearer ${prosthetistToken}` },
        data: { values: [{ elementId: 'f0000001-0000-4000-8000-000000000001', value: '20' }] },
      });
    }

    // Now admin can make gate decision
    const gatesRes = await request.get(`${API}/prosthesis-manufacturing/instances/${instance.id}/quality-gates`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const gates = await gatesRes.json();

    if (gates.length > 0) {
      const gateId = gates[0].id;
      const decisionRes = await request.post(`${API}/prosthesis-manufacturing/gate-decisions`, {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          gateId,
          decision: 'PASS',
        },
      });
      expect([200, 201]).toContain(decisionRes.status());
    }
  });

  test('ADMIN role cannot access prosthetics endpoints (403)', async ({ request }) => {
    const token = await getToken(request, 'admin', 'admin123');

    const res = await request.get(`${API}/prosthesis-manufacturing/instances`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(403);
  });
});