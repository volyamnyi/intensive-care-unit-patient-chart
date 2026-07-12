import { test, expect } from '@playwright/test';

test.describe('Patient API', () => {
  const API = 'http://localhost:8085/api';

  test('search returns patients', async ({ request }) => {
    const res = await request.get(`${API}/patients/search`, {
      headers: { Authorization: 'Basic ' + Buffer.from('doctor1:doctor123').toString('base64') },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(Array.isArray(body)).toBeTruthy();
  });

  test('search by name filters results', async ({ request }) => {
    const res = await request.get(`${API}/patients/search?name=Петренко`, {
      headers: { Authorization: 'Basic ' + Buffer.from('doctor1:doctor123').toString('base64') },
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.some((p: any) => p.patientName?.includes('Петренко'))).toBeTruthy();
  });
});
