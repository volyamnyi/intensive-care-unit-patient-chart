import { test, expect } from '../../fixtures/index';
import { getToken, firstMedicationPatient, ensureMedicationList } from '../../helpers/medication';
import {
  ensureActualTimeItem,
  planDose,
  localDate,
  localTime,
  displayActualTime,
} from '../../helpers/actual-time';
import { nurse2 } from '../../helpers/test-users';

/**
 * Nurse — the actual administration time is captured at execution time (#443,
 * phase #446). The planned context (period + dose) stays untouched; only the
 * actual fact is recorded and shown in the grid.
 *
 * Each test uses its own medicine name: the tests of a file run in parallel,
 * so a shared row would be deleted by the sibling test's setup.
 */
const DOSE = '5mg';

test.describe('Nurse — actual time capture at execution (#446)', () => {
  test('records a manual actual time and keeps it after a reload', async ({ page, request }) => {
    const MEDICINE = 'IT446-ExecTime';

    // --- API setup: doctor plans today's morning dose -------------------
    const doctorToken = await getToken(request, 1);
    const patient = await firstMedicationPatient(request, doctorToken);
    const listId = await ensureMedicationList(request, doctorToken, patient.id);
    const { dayPartId } = await ensureActualTimeItem(request, doctorToken, listId, MEDICINE);
    await planDose(request, doctorToken, dayPartId, DOSE);

    // A manual time in the past: V2 (not in the future) and V3 (today is
    // inside the list period) both hold.
    const manual = new Date(Date.now() - 2 * 60_000);

    await page.goto(`/prescriptions/nurse/${listId}`);
    const row = page.locator('tbody tr', { has: page.getByText(MEDICINE) });
    await expect(row).toBeVisible({ timeout: 15_000 });
    const cell = row.getByText(DOSE, { exact: true });
    await expect(cell).toBeVisible({ timeout: 15_000 });

    // --- the execution popover carries the actual-time section -----------
    await cell.click();
    await expect(page.getByText('Фактичний час введення')).toBeVisible();
    // The planned context stays in the popover header.
    await expect(page.getByText('Ранок: 5mg').first()).toBeVisible();

    const dateInput = page.getByLabel('Дата введення');
    const timeInput = page.getByLabel('Час введення');
    await expect(dateInput).toHaveValue(localDate(manual));
    await timeInput.fill(localTime(manual));

    // --- two-person confirmation ----------------------------------------
    await page.getByRole('button', { name: 'Продовжити' }).click();
    const second = nurse2();
    await page.getByPlaceholder('Логін другої особи').fill(second.login);
    await page.getByPlaceholder('Пароль').fill(second.password);
    await page.getByRole('button', { name: 'Підтвердити' }).click();

    // --- the executed cell shows the recorded fact ----------------------
    const done = row.getByText('✓', { exact: true });
    await expect(done).toBeVisible({ timeout: 15_000 });

    await done.hover();
    await expect(page.getByText('Введено: ' + displayActualTime(manual))).toBeVisible();
    // The planned context is unchanged next to the actual fact.
    await expect(page.getByText('Ранок: 5mg').first()).toBeVisible();

    // --- persistence across a reload -------------------------------------
    await page.reload();
    const rowAfter = page.locator('tbody tr', { has: page.getByText(MEDICINE) });
    await expect(rowAfter).toBeVisible({ timeout: 15_000 });
    const doneAfter = rowAfter.getByText('✓', { exact: true });
    await expect(doneAfter).toBeVisible({ timeout: 15_000 });
    // The pointer keeps its coordinates across a reload, so the browser fires
    // no new hover event - move it away before hovering the cell.
    await page.mouse.move(0, 0);
    await doneAfter.hover();
    await expect(page.getByText('Введено: ' + displayActualTime(manual))).toBeVisible();
  });

  test('without PRESCRIPTION_ACTUAL_TIME_EDIT the correction dialog is not offered', async ({ page, request }) => {
    const MEDICINE = 'IT446-NoPerm';

    const doctorToken = await getToken(request, 1);
    const patient = await firstMedicationPatient(request, doctorToken);
    const listId = await ensureMedicationList(request, doctorToken, patient.id);
    const { dayPartId } = await ensureActualTimeItem(request, doctorToken, listId, MEDICINE);
    await planDose(request, doctorToken, dayPartId, DOSE);

    // Execute the dose through the API (nurse + second nurse), then open the
    // list as a nurse - the nurse role has no correction permission.
    const nurseToken = await getToken(request, 3);
    const executed = await request.post(`http://localhost:8085/api/prescriptions/day-parts/${dayPartId}/execute`, {
      headers: { Authorization: `Bearer ${nurseToken}` },
      data: {
        actualDose: DOSE,
        secondPersonLogin: nurse2().login,
        secondPersonPassword: nurse2().password,
      },
    });
    expect(executed.ok(), `failed to execute dose: ${executed.status()}`).toBeTruthy();

    await page.goto(`/prescriptions/nurse/${listId}`);
    const row = page.locator('tbody tr', { has: page.getByText(MEDICINE) });
    await expect(row).toBeVisible({ timeout: 15_000 });
    const done = row.getByText('✓', { exact: true });
    await expect(done).toBeVisible({ timeout: 15_000 });

    await done.click();
    await expect(page.getByText('Коригування фактичного часу')).toHaveCount(0, { timeout: 3_000 });
  });

  test('a future actual time is rejected inline and no request is sent', async ({ page, request }) => {
    const MEDICINE = 'IT446-Future';

    const doctorToken = await getToken(request, 1);
    const patient = await firstMedicationPatient(request, doctorToken);
    const listId = await ensureMedicationList(request, doctorToken, patient.id);
    const { dayPartId } = await ensureActualTimeItem(request, doctorToken, listId, MEDICINE);
    await planDose(request, doctorToken, dayPartId, DOSE);

    await page.goto(`/prescriptions/nurse/${listId}`);
    const row = page.locator('tbody tr', { has: page.getByText(MEDICINE) });
    await expect(row).toBeVisible({ timeout: 15_000 });
    await row.getByText(DOSE, { exact: true }).click();

    // The client allows at most now + 5 min (V2); two hours ahead always
    // fails. Date and time are filled together so the case stays valid even
    // when the wall clock is close to midnight.
    const future = new Date(Date.now() + 2 * 60 * 60_000);
    await page.getByLabel('Дата введення').fill(localDate(future));
    await page.getByLabel('Час введення').fill(localTime(future));

    await expect(page.getByText('Фактичний час не може бути в майбутньому')).toBeVisible();
    // The blocked submit proves that no request leaves the browser.
    await expect(page.getByRole('button', { name: 'Продовжити' })).toBeDisabled();
    await expect(row.getByText('✓', { exact: true })).toHaveCount(0);
  });
});
