import { test, expect } from '@playwright/test';
import {
  PROSTH,
  login,
  headersFor,
  findTemplateByIdName,
  createFreeLowerInstance,
  completeOneStep,
} from '../../helpers/tp-ll-02-flow';

test.describe('TP-LL-02 — Failure terminal state (issue #238)', () => {
  let prosthetistToken: string;

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
  });

  test('FAILED (materials) → failure PDF → terminal, no replacement', async ({ request }) => {
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

    // FAILED is terminal (issue #238 — no replacement): the failed instance
    // no longer blocks the order (FAILED is not an active status), so no
    // cleanup is needed for other specs.
    const statusRes = await request.get(`${PROSTH}/instances/${instance.id}`, { headers });
    expect((await statusRes.json()).status).toBe('FAILED');
  });
});
