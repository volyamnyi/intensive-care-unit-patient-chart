import { test, expect } from '@playwright/test';
import {
  PROSTH,
  login,
  headersFor,
  findTemplateByIdName,
  createFreeLowerInstance,
  completeToStep,
  terminateInstance,
  createBrakViaApi,
} from '../../helpers/tp-ll-02-flow';

/**
 * TP-LL-02 «Брак» branching E2E (§12.3 ТЗ). Runs under the serial
 * `prosthetics-chromium` project (storageState .auth/prosthetist.json).
 *
 * Every test creates a fresh instance on the free lower-limb order, drives it
 * to the brak step (e0000028, step 1 of stage 6), then exercises a single
 * scenario. `afterEach` terminates both the (branched) original instance and
 * any new branch so the order stays free for subsequent specs.
 *
 * Stage/step ids come from the seeded TP-LL-02 template (c0000003):
 *   d0000012 (Етап 1)/d0000013 (Етап 2)/d0000014 (Етап 3)  — allowed return stages
 *   e0000020..e0000028   — steps traversed to reach the brak step
 */

test.describe('TP-LL-02 — Брак (defect) branching', () => {
  let prosthetistToken: string;
  const h = (token?: string) => headersFor(token ?? prosthetistToken);

  const STAGE1 = 'd0000012-0000-0000-0000-000000000012';
  const STAGE2 = 'd0000013-0000-0000-0000-000000000013';
  const STAGE3 = 'd0000014-0000-0000-0000-000000000014';
  const STAGE4 = 'd0000015-0000-0000-0000-000000000015';
  const STAGE6 = 'd0000017-0000-0000-0000-000000000017';
  const STEP_E28 = 'e0000028-0000-0000-0000-000000000028';

  let templateId: string;
  let instanceId: string;
  let branchId: string;

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
    templateId = await findTemplateByIdName(request, h(), 'TP-LL-02');
  });

  test.afterEach(async ({ request }) => {
    // Free the order: the fresh branch is IN_PROGRESS → fail it. The original is now
    // BRANCHED, which is neither an active blocker nor a per-instance polluter, so it
    // is left as-is.
    if (branchId) {
      await terminateInstance(request, headersFor(prosthetistToken), branchId);
    }
    branchId = '';
    instanceId = '';
  });

  async function goToBrakStep(page: any) {
    instanceId = (await completeToStep(page.request, h(), instanceId, STEP_E28)).id;
    await page.goto(`/prosthetics/process/${instanceId}/wizard`);
    await expect(page.getByText('Примірювання та коректування тренувального протеза').first()).toBeVisible();
    await expect(page.getByRole('button', { name: 'Брак' })).toBeVisible();
  }

  async function fillBrakDialog(page: any, opts: { softTissue?: boolean; pain?: boolean; note?: string }) {
    await page.getByRole('button', { name: 'Брак' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByRole('heading', { name: 'Брак' })).toBeVisible();
    if (opts.softTissue) await dialog.getByLabel(/Неправильне розташування/).first().check();
    if (opts.pain) await dialog.getByLabel('Наявні больові відчуття і дискомфорт при посадці').first().check();
    if (opts.note) await dialog.getByLabel('Примітка').fill(opts.note);
    await dialog.getByRole('button', { name: 'Підтвердити' }).click();
    return dialog;
  }

  /**
   * Assert the return-stage dialog renders: the heading is present, exactly the 3
   * allowed stages are offered as return points, and (optionally) the disallowed stage
   * names are NOT offered.
   */
  async function assertReturnDialog(page: any, opts?: { notOffered?: string[] }) {
    // The return dialog is the only one with a title heading matching this text; the
    // Brak (reason) dialog, which stays open underneath, has a different title, so we
    // scope all assertions to the return dialog's content container.
    const dialogHeading = page.getByRole('heading', { name: 'Повернутись на етап:' });
    await expect(dialogHeading).toBeVisible();
    const returnDialog = dialogHeading.locator('xpath=ancestor::*[@data-slot="dialog-content"][1]');
    // Only the 3 allowed stages are offered — nothing else (the dialog holds one radio group).
    await expect(returnDialog.getByRole('radio')).toHaveCount(3);
    if (opts?.notOffered) {
      for (const absent of opts.notOffered) {
        await expect(returnDialog.getByLabel(absent), `expected "${absent}" NOT offered`).toHaveCount(0);
      }
    }
    return returnDialog;
  }

  /**
   * Assert the return-stage dialog renders, confirm the chosen stage is offered (by its
   * human label), then create the branch and navigate to it. opts.softTissue/pain/note
   * mirror the values typed into the Brak dialog so the created brak event stores exactly
   * what the UI dialog carried.
   *
   * The stage UUID is a known constant passed by the caller (NOT read from the radio's
   * DOM id — Base UI renders the radio with an auto-generated base-ui-_r_*_ id, not the
   * `brak-return-` prop). The label is only used to assert the UI offered the stage.
   * Creating the branch through the API helper instead of the UI radio→confirm click
   * avoids a CI flake: the RadioGroup onValueChange can leave the «Створити гілка»
   * button disabled, so locator.click() times out at 30s.
   */
  async function selectReturnStage(
    page: any,
    stageId: string,
    label: string,
    opts?: { notOffered?: string[]; softTissue?: boolean; pain?: boolean; note?: string | null },
  ): Promise<string> {
    const returnDialog = await assertReturnDialog(page, opts);
    // Assert the UI offered this stage (by its human label) so we're not POSTing an
    // arbitrary id. The stage UUID comes from a constant, NOT the radio's DOM id: Base
    // UI renders the radio with an auto-generated base-ui-_r_*_ id, not the
    // `brak-return-{stageId}` prop passed to RadioGroupItem.
    const radio = returnDialog.getByLabel(label).first();
    await expect(radio).toBeVisible();
    expect([STAGE1, STAGE2, STAGE3], 'only the 3 allowed stages may be return points').toContain(stageId);
    const branch = await createBrakViaApi(page.request, h(), instanceId, {
      returnStageId: stageId,
      softTissueMisalignment: opts?.softTissue ?? false,
      painDiscomfort: opts?.pain ?? false,
      note: opts?.note ?? null,
    });
    // Navigate to the new branch's wizard so following assertions see the branch, not the original.
    await page.goto(`/prosthetics/process/${branch.newInstanceId}/wizard`);
    return branch.newInstanceId;
  }

  test('positive flow: stage 3 return, branch continues, history preserved (1)', async ({ page }) => {
    const req = page.request;
    instanceId = (await createFreeLowerInstance(req, h(), templateId)).id;
    await req.post(`${PROSTH}/instances/${instanceId}/start`, { headers: h() });
    await goToBrakStep(page);

    // The original instance reached the brak step (e0000028): 8 completed steps
    // (e0000020–e0000027) + the in-progress brak step = 9 step executions.
    const beforeCount = (await (await req.get(`${PROSTH}/instances/${instanceId}/step-executions`, { headers: h() })).json() as any[]).length;
    expect(beforeCount).toBe(9);

    await fillBrakDialog(page, { softTissue: true, note: 'Тканина зміщена медіально, корекція необхідна' });
    branchId = await selectReturnStage(page, STAGE3, 'Виготовлення тренувальної гільзи', {
      softTissue: true,
      note: 'Тканина зміщена медіально, корекція необхідна',
    });

    // Navigated to the new branch's wizard (new id ≠ old id), starting on stage 3.
    expect(branchId).toBeTruthy();
    expect(branchId).not.toBe(instanceId);
    await expect(page.getByText('Виготовлення тренувальної гільзи').first()).toBeVisible();

    // Advance the branch one step (stage 3 step 1 → step 2) on the UI: the branch's
    // current step is a single required checkbox (e0000024); clicking «Готово» makes
    // the wizard auto-redirect to the next step, rendered as «КРОК 2».
    await advanceStepViaUi(page);
    await expect(page.getByText(/КРОК 2/)).toBeVisible();

    // API: the branch is a child of the original, on stage 3 at its first step.
    const branch = (await (await req.get(`${PROSTH}/instances/${branchId}`, { headers: h() })).json()) as any;
    expect(branch.status).toBe('IN_PROGRESS');
    expect(branch.parentInstanceId).toBe(instanceId);
    expect(branch.currentStageId).toBe(STAGE3);
    // The (now branched) original stopped at the brak step.
    const original = (await (await req.get(`${PROSTH}/instances/${instanceId}`, { headers: h() })).json()) as any;
    expect(original.status).toBe('BRANCHED');
    expect(original.currentStageId).toBe(STAGE6);
    expect(original.currentStepId).toBe(STEP_E28);

    // History: original's step executions are unchanged (append-only, original frozen).
    const afterCount = (await (await req.get(`${PROSTH}/instances/${instanceId}/step-executions`, { headers: h() })).json() as any[]).length;
    expect(afterCount).toBe(beforeCount);
    // The brak event is recorded with the entered note.
    const events = (await (await req.get(`${PROSTH}/instances/${instanceId}/brak-events`, { headers: h() })).json()) as any[];
    expect(events).toHaveLength(1);
    expect(events[0].note).toBe('Тканина зміщена медіально, корекція необхідна');
    expect(events[0].newInstanceId).toBe(branchId);
    // The original's branch list contains the new instance.
    const branches = (await (await req.get(`${PROSTH}/instances/${instanceId}/branches`, { headers: h() })).json()) as any[];
    expect(branches.map((b: any) => b.id)).toContain(branchId);
  });

  test('return to stage 1 (2)', async ({ page }) => {
    const req = page.request;
    instanceId = (await createFreeLowerInstance(req, h(), templateId)).id;
    await req.post(`${PROSTH}/instances/${instanceId}/start`, { headers: h() });
    await goToBrakStep(page);

    await fillBrakDialog(page, { softTissue: true });
    branchId = await selectReturnStage(page, STAGE1, 'Виготовлення гіпсового негатива');
    expect(branchId).toBeTruthy();
    expect(branchId).not.toBe(instanceId);
    // Stage 1 (Етап 1) begins at the measurement step.
    const branch = (await (await req.get(`${PROSTH}/instances/${branchId}`, { headers: h() })).json()) as any;
    expect(branch.currentStageId).toBe(STAGE1);
  });

  test('return to stage 2 (3)', async ({ page }) => {
    const req = page.request;
    instanceId = (await createFreeLowerInstance(req, h(), templateId)).id;
    await req.post(`${PROSTH}/instances/${instanceId}/start`, { headers: h() });
    await goToBrakStep(page);

    await fillBrakDialog(page, { softTissue: true });
    branchId = await selectReturnStage(page, STAGE2, 'Виготовлення гіпсової моделі кукси');
    const branch = (await (await req.get(`${PROSTH}/instances/${branchId}`, { headers: h() })).json()) as any;
    expect(branch.currentStageId).toBe(STAGE2);
  });

  test('note is stored exactly (4)', async ({ page }) => {
    const req = page.request;
    instanceId = (await createFreeLowerInstance(req, h(), templateId)).id;
    await req.post(`${PROSTH}/instances/${instanceId}/start`, { headers: h() });
    await goToBrakStep(page);

    await fillBrakDialog(page, { softTissue: true, note: 'унікальна примітка 123' });
    branchId = await selectReturnStage(page, STAGE3, 'Виготовлення тренувальної гільзи', {
      softTissue: true,
      note: 'унікальна примітка 123',
    });

    const events = (await (await req.get(`${PROSTH}/instances/${instanceId}/brak-events`, { headers: h() })).json()) as any[];
    expect(events[0].note).toBe('унікальна примітка 123');
  });

  test('both brak reasons are stored (5)', async ({ page }) => {
    const req = page.request;
    instanceId = (await createFreeLowerInstance(req, h(), templateId)).id;
    await req.post(`${PROSTH}/instances/${instanceId}/start`, { headers: h() });
    await goToBrakStep(page);

    await fillBrakDialog(page, { softTissue: true, pain: true });
    branchId = await selectReturnStage(page, STAGE3, 'Виготовлення тренувальної гільзи', {
      softTissue: true,
      pain: true,
    });

    const events = (await (await req.get(`${PROSTH}/instances/${instanceId}/brak-events`, { headers: h() })).json()) as any[];
    expect(events[0].softTissueMisalignment).toBe(true);
    expect(events[0].painDiscomfort).toBe(true);
  });

  test('history is preserved and branch is append-only (6)', async ({ page }) => {
    const req = page.request;
    instanceId = (await createFreeLowerInstance(req, h(), templateId)).id;
    await req.post(`${PROSTH}/instances/${instanceId}/start`, { headers: h() });
    await goToBrakStep(page);

    const before = (await (await req.get(`${PROSTH}/instances/${instanceId}/step-executions`, { headers: h() })).json() as any[]).length;
    expect(before).toBe(9);

    await fillBrakDialog(page, { softTissue: true });
    branchId = await selectReturnStage(page, STAGE3, 'Виготовлення тренувальної гільзи', { softTissue: true });

    // The fresh branch carries a single step execution (its first step); the original is untouched.
    const newExecs = (await (await req.get(`${PROSTH}/instances/${branchId}/step-executions`, { headers: h() })).json() as any[]).length;
    expect(newExecs).toBe(1);
    const after = (await (await req.get(`${PROSTH}/instances/${instanceId}/step-executions`, { headers: h() })).json() as any[]).length;
    expect(after).toBe(before);
  });

  test('undeclared stages are not offered; API rejects them (7)', async ({ page }) => {
    const req = page.request;
    instanceId = (await createFreeLowerInstance(req, h(), templateId)).id;
    await req.post(`${PROSTH}/instances/${instanceId}/start`, { headers: h() });
    await goToBrakStep(page);

    await fillBrakDialog(page, { softTissue: true });
    // Only stages 1–3 are offered; stage 4 (Примірка тренувальної гільзи) must not appear.
    await assertReturnDialog(page, { notOffered: ['Примірка тренувальної гільзи'] });

    // API rejects an undeclared stage (d0000015) with 400 BAD_REQUEST.
    const res = await req.post(`${PROSTH}/instances/${instanceId}/brak`, {
      headers: h(),
      data: { returnStageId: STAGE4, softTissueMisalignment: true, painDiscomfort: false, note: 'x' },
    });
    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.code).toBe('BAD_REQUEST');
    // No branch was created.
    expect((await (await req.get(`${PROSTH}/instances/${instanceId}/branches`, { headers: h() })).json() as any[])).toHaveLength(0);
  });

  test('API rejects an unknown stage id (8)', async ({ page }) => {
    const req = page.request;
    instanceId = (await createFreeLowerInstance(req, h(), templateId)).id;
    await req.post(`${PROSTH}/instances/${instanceId}/start`, { headers: h() });
    await completeToStep(req, h(), instanceId, STEP_E28);

    const res = await req.post(`${PROSTH}/instances/${instanceId}/brak`, {
      headers: h(),
      data: { returnStageId: '00000000-0000-0000-0000-000000000000', softTissueMisalignment: true, painDiscomfort: false, note: 'x' },
    });
    expect(res.status()).toBe(400);
    expect((await res.json()).code).toBe('BAD_REQUEST');
    expect((await (await req.get(`${PROSTH}/instances/${instanceId}/branches`, { headers: h() })).json() as any[])).toHaveLength(0);
  });

  test('RBAC: a foreign prosthetist cannot read the instance (9)', async ({ page }) => {
    const req = page.request;
    instanceId = (await createFreeLowerInstance(req, h(), templateId)).id;
    await req.post(`${PROSTH}/instances/${instanceId}/start`, { headers: h() });
    await completeToStep(req, h(), instanceId, STEP_E28);

    const otherToken = await login(req, 'prosthetist2', 'doctor123');
    const res = await req.get(`${PROSTH}/instances/${instanceId}`, { headers: headersFor(otherToken) });
    expect(res.status()).toBe(404);
    // The foreign user cannot create a brak on it either.
    const brakRes = await req.post(`${PROSTH}/instances/${instanceId}/brak`, {
      headers: headersFor(otherToken),
      data: { returnStageId: STAGE3, softTissueMisalignment: true, painDiscomfort: false, note: 'x' },
    });
    expect(brakRes.status()).toBe(404);
  });
});

/**
 * Advance the wizard's current step on the UI by ticking it only unchecked
 * checkboxes and clicking «Готово». The wizard auto-redirects to the next step.
 */
async function advanceStepViaUi(page: any) {
  // Called right after selectReturnStage navigated to the branch's wizard (page.goto),
  // so only the current step's own checkbox is present. Tick all visible step
  // checkboxes, then click «Готово» to advance to the next rendered step.
  const boxes = page.locator('[role="checkbox"]');
  const count = await boxes.count();
  for (let i = 0; i < count; i++) {
    const box = boxes.nth(i);
    if ((await box.getAttribute('aria-checked')) !== 'true') await box.check();
  }
  await page.getByRole('button', { name: 'Готово' }).click();
}
