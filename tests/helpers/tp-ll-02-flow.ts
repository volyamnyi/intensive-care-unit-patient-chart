import { expect, type APIRequestContext } from '@playwright/test';

/**
 * Shared helpers for the TP-LL-02 E2E specs (full-lifecycle, failure-replacement,
 * no-gate-regression). API-driven so they run under the serial `prosthetics-chromium`
 * project (storageState `.auth/prosthetist.json`).
 *
 * The seeded data has NO active flow instances and the order registry is read-only
 * (GET only), so every spec frees the order it touches by driving its instance to a
 * terminal state (COMPLETED or FAILED). `fail` only accepts IN_PROGRESS /
 * WAITING_REVIEW, so a NEW instance must be started (PAUSED resumed) first.
 */

export const PROSTH = 'http://localhost:8085/api/prosthesis-manufacturing';
export const API = 'http://localhost:8085/api';
export const ACTIVE_STATUSES = ['NEW', 'IN_PROGRESS', 'PAUSED'];
export const TERMINAL_STATUSES = ['COMPLETED', 'FAILED', 'FAILED_QC'];

export async function login(request: APIRequestContext, loginName: string, password: string): Promise<string> {
  const res = await request.post(`${API}/auth/login`, { data: { login: loginName, password } });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token;
}

export const headersFor = (token: string) => ({ Authorization: `Bearer ${token}` });

/** Lower-limb order id from the read-only order registry (falls back to the seed UUID). */
export async function findLowerOrderId(request: APIRequestContext, headers): Promise<string> {
  return findOrderIdByProductType(request, headers, 'LOWER_LIMB', '20000000-0000-4000-8000-000000000002');
}

export async function findOrderIdByProductType(request: APIRequestContext, headers, productType: string, fallback: string): Promise<string> {
  const orders = (await (await request.get(`${PROSTH}/orders`, { headers })).json()) as Array<any>;
  const order = orders.find((o) => o.productType === productType);
  if (!order) throw new Error(`no ${productType} order in the registry`);
  return order.id ?? fallback;
}

export async function findTemplateByIdName(request: APIRequestContext, headers, name: string): Promise<string> {
  const templates = (await (await request.get(`${PROSTH}/templates`, { headers })).json()) as Array<any>;
  const t = templates.find((x) => x.name === name);
  expect(t, `${name} not found`).toBeTruthy();
  return t.id;
}

/** The instance's status (undefined when the caller cannot read it). */
export async function instanceStatus(request: APIRequestContext, headers, instanceId: string): Promise<string | undefined> {
  const res = await request.get(`${PROSTH}/instances/${instanceId}`, { headers });
  return res.ok() ? (await res.json()).status : undefined;
}

/**
 * Create a NEW instance on a free order. If another spec left an active instance there,
 * fail it (after start/resume) first. `headers` must be the caller who owns any blocker
 * (the prosthetics project runs serially with a single order per instance).
 */
export async function createFreeInstanceOnOrder(request: APIRequestContext, headers, orderId: string, templateId: string) {
  const blocker = ((await (await request.get(`${PROSTH}/instances`, { headers })).json()) as Array<any>)
    .find((i) => i.orderId === orderId && ACTIVE_STATUSES.includes(i.status));
  if (blocker) await terminateInstance(request, headers, blocker.id);
  const createRes = await request.post(`${PROSTH}/instances`, { headers, data: { orderId, templateId } });
  expect(createRes.ok(), `create instance failed: ${createRes.status()}: ${await createRes.text()}`).toBeTruthy();
  return createRes.json();
}

/** Create a NEW instance on the free lower-limb order. */
export async function createFreeLowerInstance(request: APIRequestContext, headers, templateId: string) {
  const orderId = await findLowerOrderId(request, headers);
  return createFreeInstanceOnOrder(request, headers, orderId, templateId);
}

/** Bring the instance to a startable/failable state and fail it, leaving the order free. */
export async function terminateInstance(request: APIRequestContext, headers, instanceId: string) {
  const status = await instanceStatus(request, headers, instanceId);
  expect(status, `instance ${instanceId} not readable`).toBeTruthy();
  if (TERMINAL_STATUSES.includes(status)) return;
  if (status === 'PAUSED') {
    await request.post(`${PROSTH}/instances/${instanceId}/resume`, { headers });
  } else if (status === 'NEW') {
    await request.post(`${PROSTH}/instances/${instanceId}/start`, { headers });
  }
  const failRes = await request.post(`${PROSTH}/instances/${instanceId}/fail`, {
    headers,
    data: { category: 'defect', description: 'E2E test cleanup — free the order' },
  });
  expect(failRes.ok(), `terminateInstance fail failed: ${failRes.status()}: ${await failRes.text()}`).toBeTruthy();
}

/** Build a values payload for a snapshot step, satisfying required elements. */
export function buildValues(elements, stepType): string {
  const values: Record<string, unknown> = {};
  for (const el of elements ?? []) {
    if (el.elementType === 'CHECKBOX') values[el.id] = true;
    else if (el.elementType === 'NUMERIC_INPUT') {
      const min = el.minValue ?? 0;
      const max = el.maxValue;
      values[el.id] = max != null ? Math.round(min + (max - min) / 2) : 10;
    } else values[el.id] = 'test';
  }
  if (stepType === 'MEASUREMENT') {
    // The MEASUREMENT step needs ≥3 filled (non-checkbox) values + the ЗІЗ
    // gloves acknowledgment (hardcoded on «Зняття мірок» step). The fields are
    // wizard form keys, not DB elements (that step's only DB element is a
    // STEP_MESSAGE), so inject them directly with in-bounds values.
    values['ppe-measurement-non-sterile-gloves'] = true;
    for (const key of ['chest_circumference', 'axilla_circumference', 'thigh_circumference', 'knee_circumference']) values[key] = '180';
  }
  return JSON.stringify(values);
}

/** Complete the instance's current (first pending) step execution. */
export async function completeOneStep(request: APIRequestContext, headers, instanceId: string): Promise<any> {
  const steps = (await (await request.get(`${PROSTH}/instances/${instanceId}/step-executions`, { headers })).json()) as Array<any>;
  const pending = steps.find((s) => s.status === 'IN_PROGRESS' || s.status === 'NOT_STARTED');
  expect(pending, `no pending step execution for ${instanceId}`).toBeTruthy();
  const snapshot = await (await request.get(`${PROSTH}/instances/${instanceId}/snapshot`, { headers })).json();
  const step = (snapshot.stages ?? []).flatMap((st: any) => st.steps ?? []).find((c: any) => c.id === pending.stepId);
  const completeRes = await request.post(`${PROSTH}/instances/${instanceId}/steps/${pending.id}/complete`, {
    headers,
    data: { values: buildValues(step?.elements, step?.stepType) },
  });
  expect(completeRes.ok(), `complete step failed: ${completeRes.status()}: ${await completeRes.text()}`).toBeTruthy();
  return pending;
}

/** Complete steps until the instance is COMPLETED (linear, no gate in v2.1). */
export async function completeToCompleted(request: APIRequestContext, headers, instanceId: string, max = 30) {
  for (let i = 0; i < max; i++) {
    const status = await instanceStatus(request, headers, instanceId);
    if (status === 'COMPLETED') return;
    if (status === 'FAILED' || status === 'FAILED_QC') throw new Error(`Instance ${instanceId} ${status} before COMPLETED`);
    if (status !== 'IN_PROGRESS') throw new Error(`Instance ${instanceId} in ${status}, cannot advance`);
    await completeOneStep(request, headers, instanceId);
  }
  throw new Error(`Instance ${instanceId} did not reach COMPLETED in ${max} steps`);
}
