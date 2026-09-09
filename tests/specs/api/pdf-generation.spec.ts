import { test, expect } from '@playwright/test';
import { testUser } from '../../helpers/test-users';

const API = 'http://localhost:8085/api';
/** An OPEN clinical day that no other E2E test modifies */
const OPEN_DAY_ID = 'b2222222-2222-2222-2222-222222222222';
const DOCTOR1_ID = 11;
const NURSE1_ID = 13;

async function getToken(request: any, login: string, password: string) {
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

test.describe.serial('PDF Generation', () => {
  test('generates a PDF for a signed clinical day', async ({ request }) => {
    const docToken = await getToken(request, testUser(1).login, testUser(1).password);
    const nrsToken = await getToken(request, testUser(3).login, testUser(3).password);

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
    const token = await getToken(request, testUser(1).login, testUser(1).password);

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
    expect(genRes.status()).toBe(401);
  });

  test('downloads the generated PDF bytes for in-module print/download', async ({ request }) => {
    const token = await getToken(request, testUser(1).login, testUser(1).password);

    const fileRes = await request.get(`${API}/clinical-days/${OPEN_DAY_ID}/pdf/file`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(fileRes.ok()).toBeTruthy();
    expect(fileRes.headers()['content-type']).toContain('application/pdf');
    const bytes = await fileRes.body();
    expect(bytes.slice(0, 5).toString('utf8')).toBe('%PDF-');
  });

  test('denies PDF file download without auth', async ({ request }) => {
    const fileRes = await request.get(`${API}/clinical-days/${OPEN_DAY_ID}/pdf/file`);
    expect(fileRes.status()).toBe(401);
  });

  test('removed transfer-status endpoint stays gone', async ({ request }) => {
    const token = await getToken(request, testUser(1).login, testUser(1).password);

    const statusRes = await request.get(`${API}/clinical-days/${OPEN_DAY_ID}/pdf/status`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(statusRes.status()).toBe(404);
  });
});
