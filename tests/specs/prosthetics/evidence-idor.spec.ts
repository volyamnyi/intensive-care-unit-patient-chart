import { expect, test, type APIRequestContext } from '@playwright/test';

// Phase C cross-prosthetist IDOR mirror of the Phase A F2 fix (issue #172),
// run in the serial prosthetics-chromium project. Prosthetist1 creates and
// starts a flow instance, uploads evidence to its active step, and
// prosthetist2's credentials must receive 404 for that evidence URL —
// ownership is enforced server-side, not merely hidden in the UI.

const BASE = 'http://localhost:8085/api/prosthesis-manufacturing';
const AUTH = 'http://localhost:8085/api/auth/login';

test.describe.configure({ mode: 'serial' });

async function login(request: APIRequestContext, loginName: string): Promise<string> {
  const res = await request.post(AUTH, {
    data: { login: loginName, password: 'doctor123' },
  });
  if (!res.ok()) {
    throw new Error(`Login failed for ${loginName}: HTTP ${res.status()}`);
  }
  return (await res.json()).token as string;
}

/** Minimal valid PNG so the upload passes magic-byte sniffing (#189). */
const PNG_BYTES = Buffer.from([
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d,
]);

test('prosthetist2 cannot download prosthetist1 evidence (404)', async ({ request }) => {
  const p1 = await login(request, 'prosthetist1');
  const p1Headers = { Authorization: `Bearer ${p1}` };

  // Create + start an instance owned by prosthetist1 (reuses the free-order
  // candidate pattern from wizard-checkbox-surface).
  const orders = (await (
    await request.get(`${BASE}/orders`, { headers: p1Headers })
  ).json()) as Array<{ id: string; status?: string }>;
  const templates = (await (
    await request.get(`${BASE}/templates`, { headers: p1Headers })
  ).json()) as Array<{ id: string; status: string }>;
  const template = templates.find((t) => t.status === 'ACTIVE');
  if (!template) throw new Error('No ACTIVE flow template found');

  let instanceId: string | null = null;
  for (const order of orders) {
    const created = await request.post(`${BASE}/instances`, {
      headers: p1Headers,
      data: { orderId: order.id, templateId: template.id },
    });
    if (created.ok()) {
      instanceId = ((await created.json()) as { id: string }).id;
      break;
    }
  }
  if (!instanceId) throw new Error('Could not create a flow instance for prosthetist1');

  const started = await request.post(`${BASE}/instances/${instanceId}/start`, {
    headers: p1Headers,
  });
  expect(started.ok()).toBeTruthy();

  const executions = (await (
    await request.get(`${BASE}/instances/${instanceId}/step-executions`, { headers: p1Headers })
  ).json()) as Array<{ id: string; status: string }>;
  const activeExecution = executions.find((e) => e.status === 'IN_PROGRESS') ?? executions[0];
  if (!activeExecution) throw new Error('No step execution available for evidence upload');

  const uploaded = await request.post(`${BASE}/instances/${instanceId}/evidence`, {
    headers: p1Headers,
    multipart: {
      executionId: activeExecution.id,
      file: {
        name: 'idor-probe.png',
        mimeType: 'image/png',
        buffer: PNG_BYTES,
      },
    },
  });
  expect(uploaded.status()).toBe(201);
  const fileId = ((await uploaded.json()) as { id: string }).id;

  // Owner reads it back fine…
  const ownerView = await request.get(
    `${BASE}/instances/${instanceId}/evidence/${fileId}`,
    { headers: p1Headers },
  );
  expect(ownerView.status()).toBe(200);

  // …while prosthetist2 gets the ownership-preserving 404.
  const p2 = await login(request, 'prosthetist2');
  const outsiderView = await request.get(
    `${BASE}/instances/${instanceId}/evidence/${fileId}`,
    { headers: { Authorization: `Bearer ${p2}` } },
  );
  expect(outsiderView.status()).toBe(404);
});
