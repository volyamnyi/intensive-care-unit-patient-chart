import { test, expect } from '../../fixtures/index';
import { getToken, firstMedicationPatient, ensureMedicationList } from '../../helpers/medication';
import {
  ensureActualTimeItem,
  planDose,
  executeDose,
  findTodayMorningPart,
  grantRolePermission,
  ACTUAL_TIME_EDIT_PERMISSION,
  localDate,
  localTime,
  displayActualTime,
} from '../../helpers/actual-time';

/**
 * Doctor — correction of an already recorded actual time (#444, phase #446).
 * A mandatory reason and an optimistic lock travel with the request; the
 * manual marker («уточнено вручну») shows up after a successful correction.
 */
const DOSE = '5mg';

test.describe('Doctor — actual time correction (#446)', () => {
  test('corrects the actual time with a reason and marks it as manually adjusted', async ({ page, request }) => {
    const MEDICINE = 'IT446-Correction';

    // The correction right is a matrix grant: a fresh database holds it for
    // DOCTOR by default, a long-lived one may not. Grant it through the admin
    // API (the sanctioned path for an existing database).
    await grantRolePermission(request, 'DOCTOR', ACTUAL_TIME_EDIT_PERMISSION);

    {
      // --- API setup: doctor plans, nurse executes ------------------------
      const doctorToken = await getToken(request, 1);
      const patient = await firstMedicationPatient(request, doctorToken);
      const listId = await ensureMedicationList(request, doctorToken, patient.id);
      const { itemId, dayPartId } = await ensureActualTimeItem(request, doctorToken, listId, MEDICINE);
      await planDose(request, doctorToken, dayPartId, DOSE);

      const recorded = new Date(Date.now() - 10 * 60_000);
      const nurseToken = await getToken(request, 3);
      await executeDose(request, nurseToken, dayPartId, {
        actualDose: DOSE,
        administeredAt: `${localDate(recorded)}T${localTime(recorded)}`,
      });

      await page.goto(`/prescriptions/doctor/${listId}`);
      const row = page.locator('tbody tr', { has: page.getByText(MEDICINE) });
      await expect(row).toBeVisible({ timeout: 15_000 });
      const done = row.getByText('✓', { exact: true });
      await expect(done).toBeVisible({ timeout: 15_000 });

      // The recorded (automatic) fact is visible in the tooltip.
      await done.hover();
      await expect(page.getByText('Введено: ' + displayActualTime(recorded))).toBeVisible();

      // --- correction dialog ----------------------------------------------
      await done.click();
      await expect(page.getByText('Коригування фактичного часу')).toBeVisible();
      await expect(page.getByText('Введено: ' + displayActualTime(recorded))).toBeVisible();

      // The reason is mandatory - «Зберегти» stays disabled without it.
      const save = page.getByRole('button', { name: 'Зберегти' });
      await expect(save).toBeDisabled();

      const corrected = new Date(Date.now() - 5 * 60_000);
      await page.getByLabel('Новий час введення').fill(localTime(corrected));
      await page.getByLabel('Причина коригування').fill('Дозу введено на 5 хвилин пізніше');
      await expect(save).not.toBeDisabled();
      await save.click();

      await expect(page.getByText('Фактичний час оновлено')).toBeVisible({ timeout: 10_000 });

      // --- the manual marker shows up -------------------------------------
      const manualMark = row.getByRole('img', { name: 'Фактичний час уточнено вручну' });
      await expect(manualMark).toBeVisible({ timeout: 10_000 });

      await manualMark.hover();
      await expect(page.getByText('Введено: ' + displayActualTime(corrected))).toBeVisible();
      await expect(page.getByText('(уточнено вручну)').first()).toBeVisible();

      // --- the fact survives a reload --------------------------------------
      await page.reload();
      const rowAfter = page.locator('tbody tr', { has: page.getByText(MEDICINE) });
      await expect(rowAfter).toBeVisible({ timeout: 15_000 });
      const markAfter = rowAfter.getByRole('img', { name: 'Фактичний час уточнено вручну' });
      await expect(markAfter).toBeVisible({ timeout: 10_000 });
      // The pointer keeps its coordinates across a reload, so the browser
      // fires no new hover event - move it away before hovering the cell.
      await page.mouse.move(0, 0);
      await markAfter.hover();
      await expect(page.getByText('Введено: ' + displayActualTime(corrected))).toBeVisible();

      // The planned context is untouched by the correction.
      const part = await findTodayMorningPart(request, doctorToken, listId, itemId);
      expect(part.dose).toBe(DOSE);
      expect(part.period).toBe('morning');
      expect(part.dayDate).toBe(localDate(new Date()));
    }
  });

  test('the correction endpoint is forbidden without PRESCRIPTION_ACTUAL_TIME_EDIT', async ({ request }) => {
    const MEDICINE = 'IT446-Rbac';

    const doctorToken = await getToken(request, 1);
    const patient = await firstMedicationPatient(request, doctorToken);
    const listId = await ensureMedicationList(request, doctorToken, patient.id);
    const { dayPartId } = await ensureActualTimeItem(request, doctorToken, listId, MEDICINE);
    await planDose(request, doctorToken, dayPartId, DOSE);

    const nurseToken = await getToken(request, 3);
    await executeDose(request, nurseToken, dayPartId, { actualDose: DOSE });

    const partsRes = await request.get(`http://localhost:8085/api/prescriptions/${listId}/items`, {
      headers: { Authorization: `Bearer ${doctorToken}` },
    });
    const items = await partsRes.json();
    const part = (Array.isArray(items) ? items : [])
      .flatMap((i: any) => i?.dayParts ?? [])
      .find((p: any) => p?.executionId);

    const res = await request.put(`http://localhost:8085/api/prescriptions/executions/${part.executionId}/actual-time`, {
      headers: { Authorization: `Bearer ${nurseToken}` },
      data: {
        administeredAt: `${localDate(new Date())}T${localTime(new Date())}`,
        reason: 'Спроба коригування без права',
        version: part.executionVersion,
      },
    });
    expect(res.status()).toBe(403);
  });

  test('a time outside the list period is rejected inline and nothing is sent', async ({ page, request }) => {
    const MEDICINE = 'IT447-Outside';

    await grantRolePermission(request, 'DOCTOR', ACTUAL_TIME_EDIT_PERMISSION);

    const doctorToken = await getToken(request, 1);
    const patient = await firstMedicationPatient(request, doctorToken);
    const listId = await ensureMedicationList(request, doctorToken, patient.id);
    const { dayPartId } = await ensureActualTimeItem(request, doctorToken, listId, MEDICINE);
    await planDose(request, doctorToken, dayPartId, DOSE);

    const nurseToken = await getToken(request, 3);
    await executeDose(request, nurseToken, dayPartId, { actualDose: DOSE });

    await page.goto(`/prescriptions/doctor/${listId}`);
    const row = page.locator('tbody tr', { has: page.getByText(MEDICINE) });
    await expect(row).toBeVisible({ timeout: 15_000 });
    const done = row.getByText('✓', { exact: true });
    await expect(done).toBeVisible({ timeout: 15_000 });

    await done.click();
    await expect(page.getByText('Коригування фактичного часу')).toBeVisible();

    // The list starts today, so yesterday is outside the list period (V3).
    const yesterday = new Date(Date.now() - 24 * 60 * 60_000);
    await page.getByLabel('Нова дата введення').fill(localDate(yesterday));
    await page.getByLabel('Новий час введення').fill(localTime(yesterday));

    await expect(page.getByText('Фактичний час поза межами періоду листка')).toBeVisible();
    // The blocked submit proves that no request leaves the browser.
    await expect(page.getByRole('button', { name: 'Зберегти' })).toBeDisabled();
  });

  test('a concurrent correction surfaces the conflict banner', async ({ page, request }) => {
    const MEDICINE = 'IT447-Conflict';

    await grantRolePermission(request, 'DOCTOR', ACTUAL_TIME_EDIT_PERMISSION);

    const doctorToken = await getToken(request, 1);
    const patient = await firstMedicationPatient(request, doctorToken);
    const listId = await ensureMedicationList(request, doctorToken, patient.id);
    const { itemId, dayPartId } = await ensureActualTimeItem(request, doctorToken, listId, MEDICINE);
    await planDose(request, doctorToken, dayPartId, DOSE);

    const nurseToken = await getToken(request, 3);
    await executeDose(request, nurseToken, dayPartId, { actualDose: DOSE });

    await page.goto(`/prescriptions/doctor/${listId}`);
    const row = page.locator('tbody tr', { has: page.getByText(MEDICINE) });
    await expect(row).toBeVisible({ timeout: 15_000 });
    const done = row.getByText('✓', { exact: true });
    await expect(done).toBeVisible({ timeout: 15_000 });

    await done.click();
    await expect(page.getByText('Коригування фактичного часу')).toBeVisible();

    // --- someone else corrects the same execution in the meantime --------
    const part = await findTodayMorningPart(request, doctorToken, listId, itemId);
    const concurrent = new Date(Date.now() - 6 * 60_000);
    const concurrentRes = await request.put(
      `http://localhost:8085/api/prescriptions/executions/${part.executionId}/actual-time`,
      {
        headers: { Authorization: `Bearer ${doctorToken}` },
        data: {
          administeredAt: `${localDate(concurrent)}T${localTime(concurrent)}`,
          reason: 'Паралельне коригування',
          version: part.executionVersion,
        },
      },
    );
    expect(concurrentRes.status()).toBe(200);

    // The dialog still holds the stale version -> 409 -> conflict banner.
    await page.getByLabel('Причина коригування').fill('Повторне коригування');
    await page.getByRole('button', { name: 'Зберегти' }).click();

    await expect(page.getByText('Дані змінено іншим користувачем')).toBeVisible({ timeout: 10_000 });
    // The dialog stays open - nothing was overwritten.
    await expect(page.getByText('Коригування фактичного часу')).toBeVisible();
  });
});
