import { test, expect } from '@playwright/test';
import {
  PROSTH,
  login,
  headersFor,
  findTemplateByIdName,
  createFreeLowerInstance,
  completeOneStep,
  terminateInstance,
} from '../../helpers/tp-ll-02-flow';

/**
 * Issue #237 — replacement removal regression.
 *
 * The failed-process report (`/prosthetics/process/{id}/failed`) must NOT
 * offer "Create Replacement Process": no CTA button, no dialog. The failure
 * report itself (heading, read-only banner, reason, PDF export) keeps working.
 * Runs under the serial `prosthetics-chromium` project
 * (storageState `.auth/prosthetist.json` — already prosthetist1).
 */
test.describe('Replacement absent on failed process (issue #237)', () => {
  let prosthetistToken: string;
  let instanceId: string;

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, 'prosthetist1', 'doctor123');
    const headers = headersFor(prosthetistToken);
    const templateId = await findTemplateByIdName(request, headers, 'TP-LL-02');
    const instance = await createFreeLowerInstance(request, headers, templateId);
    instanceId = instance.id as string;
    expect(instanceId).toBeTruthy();

    // `fail` accepts IN_PROGRESS only — start + complete one step first.
    await request.post(`${PROSTH}/instances/${instanceId}/start`, { headers });
    await completeOneStep(request, headers, instanceId);

    const failRes = await request.post(`${PROSTH}/instances/${instanceId}/fail`, {
      headers,
      data: { category: 'materials', description: 'regression: no replacement CTA' },
    });
    expect(failRes.ok(), `fail failed: ${failRes.status()}: ${await failRes.text()}`).toBeTruthy();
    expect((await failRes.json()).status).toBe('FAILED');
  });

  test.afterAll(async ({ request }) => {
    // FAILED is terminal — terminateInstance is a no-op; defensive only.
    try {
      await terminateInstance(request, headersFor(prosthetistToken), instanceId);
    } catch {
      // Ignore cleanup errors.
    }
  });

  test('failed report renders with no replacement action', async ({ page }) => {
    await page.goto(`/prosthetics/process/${instanceId}/failed`);

    // The report itself still works.
    await expect(page.getByRole('heading', { name: 'Процес зупинено (брак)' })).toBeVisible();
    await expect(page.getByText('Незмінний запис — лише для читання')).toBeVisible();
    await expect(page.getByText('regression: no replacement CTA')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Експортувати PDF' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Технологічна карта' })).toBeVisible();

    // The replacement entry point is gone — button and dialog.
    await expect(page.getByRole('button', { name: 'Створити замінювальний процес' })).toHaveCount(0);
    await expect(page.getByRole('dialog')).toHaveCount(0);
  });
});
