import { test, expect } from '@playwright/test';
import {
  API,
  login,
  headersFor,
  findTemplateByIdName,
  createFreeLowerInstance,
  driveToTriggerAndBrak,
  terminateInstance,
  instanceStatus,
} from '../../helpers/tp-ll-02-flow';
import { testUser } from '../../helpers/test-users';

/**
 * TP-LL-02 threshold escalation E2E (epic #322, issue #327). Runs under the
 * serial `prosthetics-chromium` project.
 *
 * Builds brak chains through the real API (4 braks: 6→6→9→9 on one order)
 * and asserts the chain mechanics plus the delivery attempt of the
 * THRESHOLD rows:
 *   - every brak returns a branch (original → BRANCHED, branch IN_PROGRESS);
 *   - GET brak-events of the chain stay consistent;
 *   - the scheduled sweep picks up the THRESHOLD rows for the 3rd/4th braks,
 *     which becomes visible as BrakNotification audit rows (SENT when an
 *     SMTP sink answers, FAILED otherwise — both prove the enqueue+sweep
 *     path ran; no real mail is asserted).
 *
 * Email delivery itself is covered by BrakThresholdIntegrationTest with a
 * mocked JavaMailSender. After each test the last branch is failed so the
 * shared lower-limb order stays free.
 */

test.describe('TP-LL-02 — threshold escalation chain (epic #322)', () => {
  const STAGE1 = 'd0000012-0000-0000-0000-000000000012';
  const STEP_E28 = 'e0000028-0000-0000-0000-000000000028';
  const STEP_E32 = 'e0000032-0000-0000-0000-000000000032';

  let prosthetistToken: string;
  let adminToken: string;
  let templateId: string;
  let lastBranchId = '';

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, testUser(7).login, testUser(7).password);
    adminToken = await login(request, testUser(6).login, testUser(6).password);
    templateId = await findTemplateByIdName(
      request, headersFor(prosthetistToken), 'TP-LL-02');
  });

  test.afterEach(async ({ request }) => {
    if (lastBranchId) {
      await terminateInstance(request, headersFor(prosthetistToken), lastBranchId);
    }
    lastBranchId = '';
  });

  async function auditActions(request, eventId: string): Promise<string[]> {
    const res = await request.get(
      `${API}/audit?entity=BrakNotification&entityId=${eventId}&pageSize=20`,
      { headers: headersFor(adminToken) });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    const content = Array.isArray(body) ? body : body.content ?? [];
    return content.map((e: any) => e.action);
  }

  /** Poll until the sweep attempts the THRESHOLD delivery (SENT or FAILED). */
  async function waitForDeliveryAttempt(request, eventId: string) {
    await expect(async () => {
      const actions = await auditActions(request, eventId);
      expect(actions.length).toBeGreaterThan(0);
    }).toPass({ timeout: 150000, intervals: [5000] });
  }

  test('chain 6→6→9→9 completes; 3rd/4th braks trigger threshold delivery attempts', async ({
    request,
  }) => {
    test.setTimeout(420000);
    const h = headersFor(prosthetistToken);
    const created = await createFreeLowerInstance(request, h, templateId);
    const startRes = await request.post(
      `${API}/prosthesis-manufacturing/instances/${created.id}/start`, { headers: h });
    expect(startRes.ok(), `start failed: ${startRes.status()}: ${await startRes.text()}`).toBeTruthy();

    const first = await driveToTriggerAndBrak(request, h, created.id, STEP_E28, STAGE1);
    expect(await instanceStatus(request, h, created.id)).toBe('BRANCHED');

    const second = await driveToTriggerAndBrak(request, h, first.newInstanceId, STEP_E28, STAGE1);
    const third = await driveToTriggerAndBrak(request, h, second.newInstanceId, STEP_E32, STAGE1);
    const fourth = await driveToTriggerAndBrak(request, h, third.newInstanceId, STEP_E32, STAGE1);
    lastBranchId = fourth.newInstanceId;
    expect(await instanceStatus(request, h, fourth.newInstanceId)).toBe('IN_PROGRESS');

    // Chain linkage: every brak event is readable on its originating instance.
    for (const originId of [created.id, first.newInstanceId, second.newInstanceId, third.newInstanceId]) {
      const eventsRes = await request.get(`${API}/prosthesis-manufacturing/instances/${originId}/brak-events`, { headers: h });
      expect(eventsRes.ok()).toBeTruthy();
      expect((await eventsRes.json()).length).toBe(1);
    }

    // The sweep (60s cadence) picks up the THRESHOLD rows for the 3rd/4th
    // braks; the delivery attempt surfaces as a BrakNotification audit row.
    // No real mail is asserted here (see header comment).
    await waitForDeliveryAttempt(request, third.brakEventId);
    await waitForDeliveryAttempt(request, fourth.brakEventId);
  });
});
