import { test, expect } from '@playwright/test';
import {
  PROSTH,
  login,
  headersFor,
  findTemplateByIdName,
  createFreeLowerInstance,
  completeOneStep,
  completeToCompleted,
  instanceStatus,
} from '../../helpers/tp-ll-02-flow';

test.describe('TP-LL-02 — Failure & Replacement (Фаза 5)', () => {
  let prosthetistToken: string;

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
  });

  test('FAILED (materials) → failure PDF → replacement NEW → COMPLETED', async ({ request }) => {
    const headers = headersFor(prosthetistToken);
    const templateId = await findTemplateByIdName(request, headers, 'TP-LL-02');
    const instance = await createFreeLowerInstance(request, headers, templateId);
    expect(instance.orderId).toBeTruthy();

    // Advance one step (needed so the instance is IN_PROGRESS — `fail` rejects NEW).
    await request.post(`${PROSTH}/instances/${instance.id}/start`, { headers });
    await completeOneStep(request, headers, instance.id);

    // Fail with a material defect (Phase 7: only 6 categories allowed).
    const failRes = await request.post(`${PROSTH}/instances/${instance.id}/fail`, {
      headers,
      data: { category: 'materials', description: 'Гільза тріснула' },
    });
    expect(failRes.ok()).toBeTruthy();
    expect((await failRes.json()).status).toBe('FAILED');

    // The failure snapshot records the category + description verbatim.
    const snapshot = await (await request.get(`${PROSTH}/instances/${instance.id}/failure-snapshot`, { headers })).json();
    expect(snapshot.category).toBe('materials');
    expect(snapshot.description).toBe('Гільза тріснула');

    // The failure report PDF renders for the FAILED instance.
    const pdfRes = await request.get(`${PROSTH}/instances/${instance.id}/pdf`, { headers });
    expect(pdfRes.ok()).toBeTruthy();
    expect(pdfRes.headers()['content-type']).toContain('application/pdf');
    const pdfText = Buffer.from(await pdfRes.body()).toString('latin1');
    expect(pdfText).toContain('%PDF');

    // A replacement instance is created for the same order (201, NEW status).
    const replacementRes = await request.post(`${PROSTH}/instances/${instance.id}/replacement`, { headers });
    expect(replacementRes.status()).toBe(201);
    const replacement = await replacementRes.json();
    expect(replacement.status).toBe('NEW');
    expect(replacement.orderId).toBe(instance.orderId);

    // Drive the replacement to COMPLETED (leaves the order free for other specs).
    await request.post(`${PROSTH}/instances/${replacement.id}/start`, { headers });
    await completeToCompleted(request, headers, replacement.id);
    expect(await instanceStatus(request, headers, replacement.id)).toBe('COMPLETED');
  });
});
