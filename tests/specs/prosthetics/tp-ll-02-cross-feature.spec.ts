import { test, expect } from '@playwright/test';
import {
  PROSTH,
  login,
  headersFor,
  findTemplateByIdName,
  createFreeLowerInstance,
  completeToStep,
  createBrakViaApi,
  terminateInstance,
} from '../../helpers/tp-ll-02-flow';
import { WizardExecutionPage } from '../../pages/prosthetics/WizardExecutionPage';

/**
 * TP-LL-02 cross-feature regression (§12.3 braking + §7.1 soft-liner + notes/files
 * + backward + pause + fail → replacement). Runs under the serial
 * `prosthetics-chromium` project (storageState .auth/prosthetist.json — already
 * prosthetist1). Single order chain: original → BRANCHED → branch (IN_PROGRESS)
 * → notes/files → backward → PAUSED → resumed → FAILED → replacement NEW.
 *
 * Order / template are the seeded lower-limb fixtures, shared across the
 * prosthetics-chromium project which runs with workers:1, fullyParallel:false.
 * The branch stays alive across the 3 tests, so cleanup is deferred to
 * `afterAll` (via terminateInstance which start/resumes before fail).
 *
 * Stage/step ids from the TP-LL-02 snapshot (c0000003):
 *  d0000012 — Виготовлення гіпсового негатива (return stage, stage 1)
 *  e0000028 — Примірювання та коректування тренувального протеза (brak trigger, stage 6 step 1)
 *  e0000029 — Виготовлення помʼякшуючого вкладиша (soft-liner, stage 7 step 1, 3 checkboxes)
 *  e0000030 — Виготовлення постійної гільзи
 *  e0000032 — Примірювання та коректування постійного протеза
 *  e0000033 — Видача протеза (stage 10, allow_backward:false)
 */
test.describe.serial('TP-LL-02 cross-feature', () => {
  const RETURN_STAGE = 'd0000012-0000-0000-0000-000000000012';
  const STEP_E0028 = 'e0000028-0000-0000-0000-000000000028';
  const STEP_E0029 = 'e0000029-0000-0000-0000-000000000029';
  const STEP_E0030 = 'e0000030-0000-0000-0000-000000000030';
  const STEP_E0032 = 'e0000032-0000-0000-0000-000000000032';
  const STEP_E0033 = 'e0000033-0000-0000-0000-000000000033';

  const VISUAL_KEY = 'f0000214-0000-0000-0000-000000000214';
  const TACTILE_KEY = 'f0000215-0000-0000-0000-000000000215';
  const NOT_REQUIRED_KEY = 'f0000240-0000-0000-0000-000000000240';

  let prosthetistToken: string;
  let templateId: string;
  let originalId: string;
  let branchId: string;
  let replacementId: string;
  let orderId: string;

  const h = () => headersFor(prosthetistToken);

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
    templateId = await findTemplateByIdName(request, h(), 'TP-LL-02');
  });

  test.afterAll(async ({ request }) => {
    // Free the shared lower-limb order for subsequent prosthetics specs.
    // originalId is BRANCHED (inactive) — no need to fail it. branchId may be
    // FAILED already (test 3) or still IN_PROGRESS/PAUSED if earlier tests failed.
    // replacementId is NEW → must be failed. terminateInstance handles NEW/PAUSED → start/resume → fail.
    const headers = h();
    for (const id of [branchId, replacementId]) {
      if (!id) continue;
      try {
        await terminateInstance(request, headers, id);
      } catch {
        // Best-effort cleanup — ignore if instance already terminal or missing.
      }
    }
    // Ensure no active blocker remains on the order (defensive, in case branchId was lost).
    try {
      const list = (await (await request.get(`${PROSTH}/instances`, { headers })).json()) as Array<any>;
      const active = list.find((i) => i.orderId === orderId && ['NEW', 'IN_PROGRESS', 'PAUSED'].includes(i.status));
      if (active) await terminateInstance(request, headers, active.id);
    } catch {
      // Ignore cleanup errors.
    }
  });

  test('1 — brak → branch → soft-liner ALLOW v&&t', async ({ page }) => {
    const req = page.request;
    // Create fresh instance on the free lower-limb order and drive to the brak trigger.
    const created = await createFreeLowerInstance(req, h(), templateId);
    originalId = created.id;
    orderId = created.orderId as string;
    expect(orderId).toBeTruthy();

    const startRes = await req.post(`${PROSTH}/instances/${originalId}/start`, { headers: h() });
    expect(startRes.ok(), `start failed: ${startRes.status()}: ${await startRes.text()}`).toBeTruthy();

    await completeToStep(req, h(), originalId, STEP_E0028);
    const preBrak = (await (await req.get(`${PROSTH}/instances/${originalId}`, { headers: h() })).json()) as any;
    expect(preBrak.currentStepId).toBe(STEP_E0028);
    expect(preBrak.status).toBe('IN_PROGRESS');

    // Create brak — return to stage 1 (d0000012). Verify BRANCHED + branch lineage.
    const brak = await createBrakViaApi(req, h(), originalId, {
      returnStageId: RETURN_STAGE,
      softTissueMisalignment: true,
      painDiscomfort: false,
      note: 'cross-feature brak',
    });
    branchId = brak.newInstanceId as string;
    expect(branchId).toBeTruthy();
    expect(branchId).not.toBe(originalId);

    const original = (await (await req.get(`${PROSTH}/instances/${originalId}`, { headers: h() })).json()) as any;
    expect(original.status).toBe('BRANCHED');
    expect(original.currentStageId).toBe('d0000017-0000-0000-0000-000000000017');
    expect(original.currentStepId).toBe(STEP_E0028);

    const branch = (await (await req.get(`${PROSTH}/instances/${branchId}`, { headers: h() })).json()) as any;
    expect(branch.status).toBe('IN_PROGRESS');
    expect(branch.parentInstanceId).toBe(originalId);
    expect(branch.branchSequence).toBeGreaterThan(1);
    expect(branch.currentStageId).toBe(RETURN_STAGE);
    expect(branch.orderId).toBe(orderId);

    // Advance the branch to the soft-liner step e0000029 (stage 7 step 1).
    // completeToStep uses buildValues which defaults to v&&t && !n — the first ALLOW variant.
    await completeToStep(req, h(), branchId, STEP_E0029);
    const atSoft = (await (await req.get(`${PROSTH}/instances/${branchId}`, { headers: h() })).json()) as any;
    expect(atSoft.currentStepId).toBe(STEP_E0029);
    expect(atSoft.status).toBe('IN_PROGRESS');

    // Complete soft-liner with the first ALLOW combo: visual && tactile && !notRequired.
    const execs = (await (await req.get(`${PROSTH}/instances/${branchId}/step-executions`, { headers: h() })).json()) as Array<any>;
    const softExec = execs.find((e) => e.stepId === STEP_E0029 && e.status === 'IN_PROGRESS');
    expect(softExec, 'soft-liner execution not found').toBeTruthy();

    const valuesVt = {
      [VISUAL_KEY]: true,
      [TACTILE_KEY]: true,
      [NOT_REQUIRED_KEY]: false,
    };
    const compVt = await req.post(`${PROSTH}/instances/${branchId}/steps/${softExec.id}/complete`, {
      headers: h(),
      data: { values: JSON.stringify(valuesVt) },
    });
    expect(compVt.ok(), `soft-liner v&&t complete failed: ${compVt.status()}: ${await compVt.text()}`).toBeTruthy();

    const afterVt = (await (await req.get(`${PROSTH}/instances/${branchId}`, { headers: h() })).json()) as any;
    expect(afterVt.currentStepId).toBe(STEP_E0030);
    expect(afterVt.status).toBe('IN_PROGRESS');
  });

  test('2 — note verbatim + file upload + backward + soft-liner ALLOW n alone + stage10 backward + pause/resume', async ({
    page,
  }) => {
    const req = page.request;
    expect(branchId, 'branchId missing — test 1 must have run').toBeTruthy();
    const headers = h();

    // Branch is at e0000030 after test 1; add note to the current IN_PROGRESS execution.
    let execs = (await (await req.get(`${PROSTH}/instances/${branchId}/step-executions`, { headers })).json()) as Array<any>;
    let currentExec = execs.find((e) => e.status === 'IN_PROGRESS');
    expect(currentExec, 'no IN_PROGRESS execution for note').toBeTruthy();
    // The execution should be for e0000030 at this point.
    expect(currentExec.stepId).toBe(STEP_E0030);

    const note = 'Cross E2E note verbatim';
    const patchRes = await req.patch(`${PROSTH}/instances/${branchId}/step-executions/${currentExec.id}`, {
      headers,
      data: { note },
    });
    expect(patchRes.ok(), `PATCH note failed: ${patchRes.status()}: ${await patchRes.text()}`).toBeTruthy();
    expect((await patchRes.json()).note).toBe(note);

    // Verify verbatim via GET step-executions.
    execs = (await (await req.get(`${PROSTH}/instances/${branchId}/step-executions`, { headers })).json()) as Array<any>;
    const patched = execs.find((e) => e.id === currentExec.id);
    expect(patched?.note).toBe(note);

    // Upload evidence file (PNG magic bytes) bound to the same execution.
    // Use globalThis.Buffer to avoid requiring @types/node in the E2E project.
    const pngBuffer = (globalThis as unknown as { Buffer: { from: (a: number[]) => Uint8Array } }).Buffer.from([
      0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
    ]);
    const uploadRes = await req.post(`${PROSTH}/instances/${branchId}/evidence?executionId=${currentExec.id}`, {
      headers,
      multipart: {
        file: {
          name: 'cross-evidence.png',
          mimeType: 'image/png',
          buffer: pngBuffer,
        },
      },
    });
    expect(uploadRes.status(), `evidence upload failed: ${await uploadRes.text()}`).toBe(201);
    const uploaded = (await uploadRes.json()) as any;
    expect(uploaded.fileName).toBe('cross-evidence.png');
    expect(uploaded.stepExecutionId).toBe(currentExec.id);

    // Verify the file appears in the list for this execution.
    const listRes = await req.get(`${PROSTH}/instances/${branchId}/evidence?executionId=${currentExec.id}`, {
      headers,
    });
    expect(listRes.ok()).toBeTruthy();
    const files = (await listRes.json()) as Array<any>;
    expect(files.map((f) => f.id)).toContain(uploaded.id);
    expect(files.find((f) => f.id === uploaded.id)?.mimeType).toBe('image/png');

    // Second ALLOW combo via backward: from e0000030 back to e0000029, then complete with n alone.
    const backToSoft = await req.post(`${PROSTH}/instances/${branchId}/backward`, { headers });
    expect(backToSoft.ok(), `backward to soft-liner failed: ${backToSoft.status()}: ${await backToSoft.text()}`).toBeTruthy();
    const atSoftAgain = (await backToSoft.json()) as any;
    expect(atSoftAgain.currentStepId).toBe(STEP_E0029);
    expect(atSoftAgain.status).toBe('IN_PROGRESS');

    execs = (await (await req.get(`${PROSTH}/instances/${branchId}/step-executions`, { headers })).json()) as Array<any>;
    const softExecAgain = execs.find((e) => e.stepId === STEP_E0029 && e.status === 'IN_PROGRESS');
    expect(softExecAgain, 'soft-liner execution after backward not found').toBeTruthy();

    const valuesN = {
      [VISUAL_KEY]: false,
      [TACTILE_KEY]: false,
      [NOT_REQUIRED_KEY]: true,
    };
    const compN = await req.post(`${PROSTH}/instances/${branchId}/steps/${softExecAgain.id}/complete`, {
      headers,
      data: { values: JSON.stringify(valuesN) },
    });
    expect(compN.ok(), `soft-liner n-alone complete failed: ${compN.status()}: ${await compN.text()}`).toBeTruthy();
    const afterN = (await (await req.get(`${PROSTH}/instances/${branchId}`, { headers })).json()) as any;
    expect(afterN.currentStepId).toBe(STEP_E0030);

    // Advance to issuance-stage e0000033 (Видача протеза, stage 10).
    await completeToStep(req, headers, branchId, STEP_E0033);
    const atIssuance = (await (await req.get(`${PROSTH}/instances/${branchId}`, { headers })).json()) as any;
    expect(atIssuance.currentStepId).toBe(STEP_E0033);
    expect(atIssuance.status).toBe('IN_PROGRESS');

    // Backward at issuance: should revert to e0000032 (Примірювання та коректування постійного протеза).
    // Keep the API path as the source of truth; the UI "Назад"/"Попередній" button follows the same endpoint.
    const backRes = await req.post(`${PROSTH}/instances/${branchId}/backward`, { headers });
    expect(backRes.ok(), `backward from issuance failed: ${backRes.status()}: ${await backRes.text()}`).toBeTruthy();
    const afterBack = (await backRes.json()) as any;
    expect(afterBack.currentStepId).toBe(STEP_E0032);
    expect(afterBack.status).toBe('IN_PROGRESS');

    // Pause via UI (WizardExecutionPage) with the new 4-value category WENT_ABROAD.
    await page.goto(`/prosthetics/process/${branchId}/wizard`);
    const wizard = new WizardExecutionPage(page);
    // The wizard must render before pausing; the heading is the current step title (e0000032).
    await expect(page.getByRole('heading').first()).toBeVisible({ timeout: 10_000 });
    await wizard.pauseProcess('WENT_ABROAD');

    const paused = (await (await req.get(`${PROSTH}/instances/${branchId}`, { headers })).json()) as any;
    expect(paused.status).toBe('PAUSED');
    expect(paused.pauseCategory).toBe('WENT_ABROAD');

    // Resume via API and verify IN_PROGRESS again.
    const resumeRes = await req.post(`${PROSTH}/instances/${branchId}/resume`, { headers });
    expect(resumeRes.ok(), `resume failed: ${resumeRes.status()}: ${await resumeRes.text()}`).toBeTruthy();
    const resumed = (await resumeRes.json()) as any;
    expect(resumed.status).toBe('IN_PROGRESS');
    // After resume the current step stays at e0000032 (the step we backed to).
    expect(resumed.currentStepId).toBe(STEP_E0032);
  });

  test('3 — fail (allowlist) → replacement NEW', async ({ page }) => {
    const req = page.request;
    expect(branchId, 'branchId missing').toBeTruthy();
    expect(orderId, 'orderId missing').toBeTruthy();
    const headers = h();

    // Ensure the instance is IN_PROGRESS before failing (fail rejects PAUSED/NEW).
    const beforeFail = (await (await req.get(`${PROSTH}/instances/${branchId}`, { headers })).json()) as any;
    expect(beforeFail.status).toBe('IN_PROGRESS');

    // Fail with the allowlisted category "other" (Phase 9: materials/component_damage/order_cancelled/patient/other/defect).
    const failRes = await req.post(`${PROSTH}/instances/${branchId}/fail`, {
      headers,
      data: { category: 'other', description: 'Cross E2E fail' },
    });
    expect(failRes.ok(), `fail failed: ${failRes.status()}: ${await failRes.text()}`).toBeTruthy();
    const failed = (await failRes.json()) as any;
    expect(failed.status).toBe('FAILED');
    expect(failed.id).toBe(branchId);

    // Failure snapshot records the category verbatim.
    const snapshot = (await (await req.get(`${PROSTH}/instances/${branchId}/failure-snapshot`, { headers })).json()) as any;
    expect(snapshot.category).toBe('other');
    expect(snapshot.description).toBe('Cross E2E fail');

    // Replacement creates a fresh NEW instance for the same order.
    const replRes = await req.post(`${PROSTH}/instances/${branchId}/replacement`, { headers });
    expect(replRes.status()).toBe(201);
    const repl = (await replRes.json()) as any;
    replacementId = repl.id as string;
    expect(replacementId).toBeTruthy();
    expect(replacementId).not.toBe(branchId);
    expect(repl.status).toBe('NEW');
    expect(repl.orderId).toBe(orderId);
    // Template is preserved.
    expect(repl.templateId).toBeTruthy();
  });
});
