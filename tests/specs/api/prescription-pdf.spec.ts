import { test, expect } from '@playwright/test';
import { inflateRawSync } from 'zlib';
import { testUser } from '../../helpers/test-users';
import {
  ensureMedicationItem,
  ensureMedicationList,
  firstMedicationPatient,
} from '../../helpers/medication';

const API = 'http://localhost:8085/api';

// Minimal dependency-free ZIP reader: the backend writes STORED entries with
// upfront sizes (no data descriptors), so local headers are self-contained.
// DEFLATED entries are inflated for robustness.
function parseZipEntries(buf: Buffer): { name: string; data: Buffer }[] {
  const entries: { name: string; data: Buffer }[] = [];
  let off = 0;
  for (let guard = 0; guard < 100; guard++) {
    if (off + 30 > buf.length) break;
    if (buf.readUInt32LE(off) !== 0x04034b50) break;
    const method = buf.readUInt16LE(off + 8);
    const compSize = buf.readUInt32LE(off + 18);
    const nameLen = buf.readUInt16LE(off + 26);
    const extraLen = buf.readUInt16LE(off + 28);
    const name = buf.toString('utf8', off + 30, off + 30 + nameLen);
    const dataStart = off + 30 + nameLen + extraLen;
    const raw = buf.subarray(dataStart, dataStart + compSize);
    entries.push({ name, data: method === 8 ? inflateRawSync(raw) : Buffer.from(raw) });
    off = dataStart + compSize;
  }
  return entries;
}

async function doctorToken(request: any): Promise<string> {
  const { login, password } = testUser(1);
  const res = await request.post(`${API}/auth/login`, { data: { login, password } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

async function createList(request: any, token: string, patientId: number): Promise<string> {
  const res = await request.post(`${API}/prescriptions`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { patientId: String(patientId) },
  });
  expect(res.ok(), `list creation failed: ${res.status()} ${await res.text()}`).toBeTruthy();
  return (await res.json()).id as string;
}

async function planFirstFreeDose(request: any, token: string, listId: string): Promise<void> {
  const itemsRes = await request.get(`${API}/prescriptions/${listId}/items`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(itemsRes.ok()).toBeTruthy();
  const items = await itemsRes.json();
  for (const item of items) {
    for (const part of item?.dayParts ?? []) {
      if (!part?.isPlanned && !part?.isCompleted && part?.id) {
        const planRes = await request.put(`${API}/prescriptions/day-parts/${part.id}/plan`, {
          headers: { Authorization: `Bearer ${token}` },
          data: { dose: '500 мг' },
        });
        expect(planRes.ok(), `plan failed: ${planRes.status()} ${await planRes.text()}`).toBeTruthy();
        return;
      }
    }
  }
}

test.describe.serial('Prescription PDF batch (Form 003-4/о)', () => {
  test('info reports pages, file endpoint returns every sheet as ZIP', async ({ request }) => {
    const token = await doctorToken(request);
    const patient = await firstMedicationPatient(request, token);
    const listId = await ensureMedicationList(request, token, patient.id);
    await ensureMedicationItem(request, token, listId, 'Paracetamol 003-4o');
    await planFirstFreeDose(request, token, listId);

    const infoRes = await request.get(`${API}/prescriptions/${listId}/pdf/info`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(infoRes.ok()).toBeTruthy();
    const info = await infoRes.json();
    expect(info.pages).toBeGreaterThanOrEqual(1);
    expect(info.fileName).toBe(`prescription-${listId}.zip`);

    const fileRes = await request.post(`${API}/prescriptions/${listId}/pdf/file`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(fileRes.ok()).toBeTruthy();
    expect(fileRes.headers()['x-total-pages']).toBe(String(info.pages));
    const disp = fileRes.headers()['content-disposition'] ?? '';
    expect(disp).toContain(`prescription-${listId}.zip`);

    const body = Buffer.from(await fileRes.body());
    expect(body.subarray(0, 2).toString('ascii')).toBe('PK');
    const entries = parseZipEntries(body);
    // The batch is never truncated: one PDF file per form sheet.
    expect(entries.length).toBe(info.pages);
    for (let i = 0; i < entries.length; i++) {
      const expected = `prescription-${listId}-p${String(i + 1).padStart(2, '0')}.pdf`;
      expect(entries[i].name).toBe(expected);
      expect(entries[i].data.subarray(0, 5).toString('ascii')).toBe('%PDF-');
      // Filenames carry no PII: only the list UUID and the page index.
      expect(entries[i].name).not.toContain(String(patient.id));
    }
    expect((await request.get(`${API}/prescriptions/${listId}/pdf/file`, {
      headers: { Authorization: `Bearer ${token}` },
    })).status()).toBe(405);
  });

  test('document endpoint returns one PDF for the whole list and 204 when empty', async ({ request }) => {
    const token = await doctorToken(request);
    const patient = await firstMedicationPatient(request, token);
    const listId = await ensureMedicationList(request, token, patient.id);
    await ensureMedicationItem(request, token, listId, 'Paracetamol 003-4o document');
    await planFirstFreeDose(request, token, listId);
    const headers = { Authorization: `Bearer ${token}` };

    const info = await (await request.get(`${API}/prescriptions/${listId}/pdf/info`, { headers })).json();
    expect(info.pages).toBeGreaterThanOrEqual(1);

    const documentRes = await request.post(`${API}/prescriptions/${listId}/pdf/document`, { headers });
    expect(documentRes.status()).toBe(200);
    expect(documentRes.headers()['content-type']).toContain('application/pdf');
    expect(documentRes.headers()['x-total-pages']).toBe(String(info.pages));
    expect(documentRes.headers()['content-disposition'] ?? '').toContain(`prescription-${listId}.pdf`);
    expect(documentRes.headers()['cache-control'] ?? '').toContain('no-store');
    const documentBody = Buffer.from(await documentRes.body());
    expect(documentBody.subarray(0, 5).toString('ascii')).toBe('%PDF-');
    expect(documentBody.toString('latin1').trimEnd().endsWith('%%EOF')).toBe(true);
    expect(documentBody.subarray(0, 2).toString('ascii')).not.toBe('PK');
    expect((await request.get(`${API}/prescriptions/${listId}/pdf/document`, { headers })).status()).toBe(405);

    // An inclusive single-day window yields exactly one physical week sheet.
    const items = await (await request.get(`${API}/prescriptions/${listId}/items`, { headers })).json();
    const planned = (Array.isArray(items) ? items : [])
      .flatMap((item: any) => item?.dayParts ?? [])
      .find((part: any) => part?.isPlanned && part?.dayDate);
    expect(planned?.dayDate).toBeTruthy();
    const oneWeek = await request.post(
      `${API}/prescriptions/${listId}/pdf/document?from=${planned.dayDate}&to=${planned.dayDate}`,
      { headers },
    );
    expect(oneWeek.status()).toBe(200);
    expect(oneWeek.headers()['x-total-pages']).toBe('1');
    expect(Buffer.from(await oneWeek.body()).subarray(0, 5).toString('ascii')).toBe('%PDF-');

    // The window is a mandatory pair in yyyy-MM-dd calendar order.
    expect((await request.post(
      `${API}/prescriptions/${listId}/pdf/document?from=${planned.dayDate}`, { headers },
    )).status()).toBe(400);
    expect((await request.post(
      `${API}/prescriptions/${listId}/pdf/document?from=${planned.dayDate}&to=2000-01-01`, { headers },
    )).status()).toBe(400);
    expect((await request.post(
      `${API}/prescriptions/${listId}/pdf/document?from=16.10.2026&to=2026-10-22`, { headers },
    )).status()).toBe(400);

    const nurseCreds = testUser(3);
    const nurseLogin = await request.post(`${API}/auth/login`, {
      data: { login: nurseCreds.login, password: nurseCreds.password },
    });
    expect(nurseLogin.ok()).toBeTruthy();
    const nurseToken = (await nurseLogin.json()).token as string;
    const nurseDocument = await request.post(`${API}/prescriptions/${listId}/pdf/document`, {
      headers: { Authorization: `Bearer ${nurseToken}` },
    });
    expect(nurseDocument.status()).toBe(200);
    expect(Buffer.from(await nurseDocument.body()).subarray(0, 5).toString('ascii')).toBe('%PDF-');

    // A list without printable orders is an explicit empty document, not a blank sheet.
    const emptyId = await createList(request, token, patient.id);
    try {
      const empty = await request.post(`${API}/prescriptions/${emptyId}/pdf/document`, { headers });
      expect(empty.status()).toBe(204);
      expect(empty.headers()['x-total-pages']).toBe('0');
      expect((await empty.body()).length).toBe(0);
      const emptyInfo = await request.get(`${API}/prescriptions/${emptyId}/pdf/info`, { headers });
      expect(emptyInfo.ok()).toBeTruthy();
      expect((await emptyInfo.json()).pages).toBe(0);
    } finally {
      await request.post(`${API}/prescriptions/${emptyId}/close`, { headers });
      await request.delete(`${API}/prescriptions/${emptyId}`, { headers });
    }
  });

  test('single sheet endpoint returns one printable PDF', async ({ request }) => {
    const token = await doctorToken(request);
    const patient = await firstMedicationPatient(request, token);
    const listId = await ensureMedicationList(request, token, patient.id);
    await ensureMedicationItem(request, token, listId, 'Paracetamol 003-4o');

    const pageRes = await request.post(`${API}/prescriptions/${listId}/pdf/pages/0`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(pageRes.ok()).toBeTruthy();
    expect(pageRes.headers()['content-type']).toContain('application/pdf');
    expect(Buffer.from(await pageRes.body()).subarray(0, 5).toString('ascii')).toBe('%PDF-');

    const outOfRange = await request.post(`${API}/prescriptions/${listId}/pdf/pages/999`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(outOfRange.status()).toBe(404);
    expect((await request.get(`${API}/prescriptions/${listId}/pdf/pages/0`, {
      headers: { Authorization: `Bearer ${token}` },
    })).status()).toBe(405);
  });

  test('unknown list is 404 and nurse read access holds', async ({ request }) => {
    const token = await doctorToken(request);
    const missing = '00000000-0000-0000-0000-000000000000';
    expect((await request.get(`${API}/prescriptions/${missing}/pdf/info`, {
      headers: { Authorization: `Bearer ${token}` },
    })).status()).toBe(404);

    const nurseCreds = testUser(3);
    const nurseLogin = await request.post(`${API}/auth/login`, {
      data: { login: nurseCreds.login, password: nurseCreds.password },
    });
    expect(nurseLogin.ok()).toBeTruthy();
    const nurseToken = (await nurseLogin.json()).token as string;
    const patient = await firstMedicationPatient(request, token);
    const listId = await ensureMedicationList(request, token, patient.id);
    const nurseInfo = await request.get(`${API}/prescriptions/${listId}/pdf/info`, {
      headers: { Authorization: `Bearer ${nurseToken}` },
    });
    expect(nurseInfo.ok()).toBeTruthy();
  });
});
