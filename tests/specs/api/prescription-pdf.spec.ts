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

    const fileRes = await request.get(`${API}/prescriptions/${listId}/pdf/file`, {
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
  });

  test('single sheet endpoint returns one printable PDF', async ({ request }) => {
    const token = await doctorToken(request);
    const patient = await firstMedicationPatient(request, token);
    const listId = await ensureMedicationList(request, token, patient.id);
    await ensureMedicationItem(request, token, listId, 'Paracetamol 003-4o');

    const pageRes = await request.get(`${API}/prescriptions/${listId}/pdf/pages/0`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(pageRes.ok()).toBeTruthy();
    expect(pageRes.headers()['content-type']).toContain('application/pdf');
    expect(Buffer.from(await pageRes.body()).subarray(0, 5).toString('ascii')).toBe('%PDF-');

    const outOfRange = await request.get(`${API}/prescriptions/${listId}/pdf/pages/999`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(outOfRange.status()).toBe(404);
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
