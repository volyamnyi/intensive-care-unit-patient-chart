import { test, expect } from '@playwright/test';

const API = 'http://localhost:8085/api';
/** An OPEN clinical day that no other E2E test modifies */
const OPEN_DAY_ID = 'b2222222-2222-2222-2222-222222222222';
const DOCTOR1_ID = '11111111-1111-1111-1111-111111111111';
const NURSE1_ID = '33333333-3333-3333-3333-333333333333';

async function getToken(request: any, login: string, password: string) {
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe('PDF Generation', () => {
  test('generates a PDF for a signed clinical day', async ({ request }) => {
    const docToken = await getToken(request, 'doctor1', 'doctor123');
    const nrsToken = await getToken(request, 'nurse1', 'nurse123');

    const signNrs = await request.post(`${API}/clinical-days/${OPEN_DAY_ID}/sign/nurse`, {
      headers: { Authorization: `Bearer ${nrsToken}` },
      data: { userId: NURSE1_ID },
    });
    expect(signNrs.status()).toBe(204);

    const signDoc = await request.post(`${API}/clinical-days/${OPEN_DAY_ID}/sign/doctor`, {
      headers: { Authorization: `Bearer ${docToken}` },
      data: { userId: DOCTOR1_ID },
    });
    expect(signDoc.status()).toBe(204);

    const genRes = await request.post(`${API}/clinical-days/${OPEN_DAY_ID}/pdf`, {
      headers: { Authorization: `Bearer ${docToken}` },
    });
    expect(genRes.ok()).toBeTruthy();
    const genBody = await genRes.json();
    expect(genBody).toHaveProperty('id');
    expect(genBody).toHaveProperty('fileName');
    expect(genBody.fileName).toContain('.pdf');
    expect(genBody).toHaveProperty('fileVersion');
    expect(genBody).toHaveProperty('generatedAt');
    expect(genBody).toHaveProperty('generatedBy');
    expect(genBody).toHaveProperty('checksum');
  });

  test('retrieves the latest PDF for a clinical day', async ({ request }) => {
    const token = await getToken(request, 'doctor1', 'doctor123');

    const getRes = await request.get(`${API}/clinical-days/${OPEN_DAY_ID}/pdf`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(getRes.ok()).toBeTruthy();
    const body = await getRes.json();
    expect(body).toHaveProperty('id');
    expect(body).toHaveProperty('fileName');
    expect(body.fileName).toContain('.pdf');
  });

  test('denies PDF generation without auth', async ({ request }) => {
    const genRes = await request.post(`${API}/clinical-days/${OPEN_DAY_ID}/pdf`);
    expect(genRes.status()).toBe(403);
  });
});
