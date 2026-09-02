import { expect, type APIRequestContext, type Page } from '@playwright/test';

/**
 * Shared API helpers for the prosthetics E2E specs.
 *
 * The prosthetics specs run as prosthetist1, but the quality gate requires a
 * PROSTHETICS_ADMINISTRATOR decision and the wizard UI only offers the gate to
 * approvers. These helpers drive an instance to COMPLETED through the backend
 * API so that a test leaves no active instance behind (the "new process" review
 * screen blocks orders that already have an active process).
 */

const BASE = 'http://localhost:8085/api/prosthesis-manufacturing';
const AUTH = 'http://localhost:8085/api/auth/login';

async function login(request: APIRequestContext, login: string, password: string): Promise<string> {
  const res = await request.post(AUTH, { data: { login, password } });
  if (!res.ok()) {
    throw new Error(`Login failed for ${login}: HTTP ${res.status()}`);
  }
  return (await res.json()).token;
}

/** Builds a values payload for a snapshot step, satisfying required elements. */
export function buildValues(
  elements: Array<{ id: string; elementType: string; minValue?: number | null; maxValue?: number | null }> | undefined,
  stepType?: string,
): string {
  const values: Record<string, unknown> = {};
  for (const element of elements ?? []) {
    if (element.elementType === 'CHECKBOX') {
      values[element.id] = true;
    } else if (element.elementType === 'NUMERIC_INPUT') {
      // Respect the element's min/max bounds (e.g. circumference 100–400)
      const min = element.minValue ?? 0;
      const max = element.maxValue;
      values[element.id] = max != null ? Math.round(min + (max - min) / 2) : 10;
    } else if (element.elementType === 'SIGNATURE_CAPTURE') {
      values[element.id] = 'Електронний підпис';
    } else {
      values[element.id] = 'test';
    }
  }
  // Phase 3: soft-liner step e0000029 — keep default as variant A (visual && tactile && !notRequired).
  const NOT_REQUIRED_KEY = 'f0000240-0000-0000-0000-000000000240';
  if (NOT_REQUIRED_KEY in values) {
    values[NOT_REQUIRED_KEY] = false;
  }
  if (stepType === 'MEASUREMENT') {
    // The measurement step collects values via the visual measurement forms
    // (not DB elements) and requires ≥3 filled values + the ЗІЗ gloves
    // acknowledgment. The step's only DB element is a STEP_MESSAGE, so inject
    // the required inputs directly.
    values['ppe-measurement-non-sterile-gloves'] = true;
    for (const key of ['chest_circumference', 'axilla_circumference', 'forearm_circumference']) {
      values[key] = '180';
    }
  }
  return JSON.stringify(values);
}

/**
 * Drives an instance to COMPLETED via the backend API:
 * resume when paused → complete pending step executions (as prosthetist1) →
 * pass quality gates (as prosthetics_admin1) → repeat until COMPLETED.
 */
export async function completeInstanceViaApi(request: APIRequestContext, instanceId: string): Promise<void> {
  const prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
  const prosthetistHeaders = { Authorization: `Bearer ${prosthetistToken}` };

  for (let i = 0; i < 30; i++) {
    const instanceRes = await request.get(`${BASE}/instances/${instanceId}`, { headers: prosthetistHeaders });
    const instance = await instanceRes.json();

    if (instance.status === 'COMPLETED') {
      return;
    }

    if (instance.status === 'PAUSED' || instance.status === 'BLOCKED_PATIENT' || instance.status === 'BLOCKED_MATERIAL') {
      const resumeRes = await request.post(`${BASE}/instances/${instanceId}/resume`, { headers: prosthetistHeaders });
      if (!resumeRes.ok()) {
        throw new Error(`Resume failed: HTTP ${resumeRes.status()}`);
      }
      continue;
    }

    if (instance.status === 'WAITING_REVIEW' || instance.status === 'CORRECTION') {
      await passPendingGateViaApi(request, instanceId);
      continue;
    }

    // IN_PROGRESS / NEW — complete the first pending step execution.
    const stepsRes = await request.get(`${BASE}/instances/${instanceId}/step-executions`, { headers: prosthetistHeaders });
    const steps = (await stepsRes.json()) as Array<{ id: string; stepId: string; status: string }>;
    const pending = steps.find((step) => step.status === 'IN_PROGRESS' || step.status === 'NOT_STARTED');
    if (!pending) {
      throw new Error(`No pending step execution (status=${instance.status})`);
    }
    const snapshotRes = await request.get(`${BASE}/instances/${instanceId}/snapshot`, { headers: prosthetistHeaders });
    const snapshot = await snapshotRes.json();
    const step = (snapshot.stages ?? [])
      .flatMap((stage: { steps?: Array<{ id: string; stepType?: string; elements: Array<{ id: string; elementType: string }> }> }) => stage.steps ?? [])
      .find((candidate: { id: string }) => candidate.id === pending.stepId);
    const completeRes = await request.post(`${BASE}/instances/${instanceId}/steps/${pending.id}/complete`, {
      headers: prosthetistHeaders,
      data: { values: buildValues(step?.elements, step?.stepType) },
    });
    if (!completeRes.ok()) {
      throw new Error(`Step complete failed: HTTP ${completeRes.status()}: ${await completeRes.text()}`);
    }
  }

  throw new Error(`Instance ${instanceId} did not reach COMPLETED in time`);
}

/** Completes the instance's current (first pending) step execution via the API. */
export async function completeCurrentStepViaApi(
  request: APIRequestContext,
  instanceId: string,
): Promise<void> {
  const token = await login(request, 'prosthetist1', 'doctor123');
  const headers = { Authorization: `Bearer ${token}` };

  const stepsRes = await request.get(`${BASE}/instances/${instanceId}/step-executions`, {
    headers,
  });
  const steps = (await stepsRes.json()) as Array<{ id: string; stepId: string; status: string }>;
  const pending = steps.find((step) => step.status === 'IN_PROGRESS' || step.status === 'NOT_STARTED');
  if (!pending) {
    throw new Error(`No pending step execution for instance ${instanceId}`);
  }
  const snapshotRes = await request.get(`${BASE}/instances/${instanceId}/snapshot`, { headers });
  const snapshot = await snapshotRes.json();
  const step = (snapshot.stages ?? [])
    .flatMap((stage: { steps?: Array<{ id: string; stepType?: string; elements: Array<{ id: string; elementType: string }> }> }) => stage.steps ?? [])
    .find((candidate: { id: string }) => candidate.id === pending.stepId);
  const completeRes = await request.post(`${BASE}/instances/${instanceId}/steps/${pending.id}/complete`, {
    headers,
    data: { values: buildValues(step?.elements, step?.stepType) },
  });
  if (!completeRes.ok()) {
    throw new Error(`Step complete failed: HTTP ${completeRes.status()}: ${await completeRes.text()}`);
  }
}

export async function passPendingGateViaApi(request: APIRequestContext, instanceId: string): Promise<void> {
  const adminToken = await login(request, 'prosthetics_admin1', 'doctor123');
  const headers = { Authorization: `Bearer ${adminToken}` };
  const snapshotRes = await request.get(`${BASE}/instances/${instanceId}/snapshot`, { headers });
  const snapshot = await snapshotRes.json();
  const gate = (snapshot.stages ?? []).find((stage: { gate?: unknown }) => stage.gate)?.gate as
    | { id: string; criteria?: Array<{ id: string }> }
    | undefined;
  if (!gate) {
    throw new Error(`No quality gate in snapshot for instance ${instanceId}`);
  }
  const criteriaIds = (gate.criteria ?? []).map((criterion) => criterion.id);
  const passRes = await request.post(`${BASE}/instances/${instanceId}/gates/${gate.id}/decision`, {
    headers,
    data: { decision: 'PASS', criteriaConfirmed: criteriaIds, comment: '' },
  });
  if (!passRes.ok()) {
    throw new Error(`Gate PASS failed: HTTP ${passRes.status()}: ${await passRes.text()}`);
  }
}

/** Clicks «Розпочати процес» when the wizard shows the NEW-instance start screen. */
export async function startProcessIfNeeded(page: Page): Promise<void> {
  const startButton = page.getByRole('button', { name: /Розпочати процес/ });
  if (await startButton.isVisible({ timeout: 3000 }).catch(() => false)) {
    await startButton.click();
    // Deterministic: the start screen must be gone once the process starts —
    // wait for the transition instead of a sleep.
    await expect(startButton).toBeHidden({ timeout: 10000 });
  }
}
