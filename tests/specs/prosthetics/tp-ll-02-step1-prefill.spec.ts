import { test, expect } from '@playwright/test';
import {
  PROSTH,
  login,
  headersFor,
  findTemplateByIdName,
  createFreeLowerInstance,
  terminateInstance,
} from '../../helpers/tp-ll-02-flow';
import { testUser } from '../../helpers/test-users';

/**
 * E2E for TP-LL-02 Step 1 autofill (issue #283): opening
 * «Зняття та внесення об'ємних розмірів» (e0000020) prefills the 12 header
 * fields of the measurement blank from the picked MIS limb-order document
 * (`spiDocumentProsthesCheck` via `GET .../orders/documents`), fills the
 * date with the local today, renders mobility as a 5-option dropdown, and
 * never overwrites manual edits.
 *
 * Live-MIS dependent: when the instance patient has no MIS order documents
 * the spec logs counts and skips honestly (same contract as the
 * production-access spec) instead of failing on absent live data.
 */

const MOBILITY_OPTIONS = ['1 рівень', '2 рівень', '3 рівень', '4 рівень', 'Інший / Не вказано'];

function localToday(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

test.describe('TP-LL-02 — Step 1 prefill from MIS (#283)', () => {
  let prosthetistToken: string;

  test.beforeAll(async ({ request }) => {
    prosthetistToken = await login(request, testUser(7).login, testUser(7).password);
  });

  test('prefills the blank header from the MIS document and keeps manual edits', async ({
    page,
    request,
  }) => {
    const headers = headersFor(prosthetistToken);
    const templateId = await findTemplateByIdName(request, headers, 'TP-LL-02');
    const instance = await createFreeLowerInstance(request, headers, templateId);
    const instanceId = instance.id as string;
    try {
      const startRes = await request.post(`${PROSTH}/instances/${instanceId}/start`, { headers });
      expect(startRes.ok(), `start failed: ${await startRes.text()}`).toBeTruthy();

      const inst = (await (
        await request.get(`${PROSTH}/instances/${instanceId}`, { headers })
      ).json()) as any;
      const order = (await (
        await request.get(`${PROSTH}/orders/${inst.orderId}`, { headers })
      ).json()) as any;
      const patientId = order.patientId as string;
      const docsRes = await request.get(`${PROSTH}/orders/documents`, {
        headers,
        params: { patientId },
      });
      const docs = (docsRes.ok() ? await docsRes.json() : []) as Array<any>;
      console.log(`[step1-prefill] patientId=${patientId} documents=${docs.length}`);
      test.skip(
        docs.length === 0,
        `need 1 MIS order document for patient ${patientId} (live MIS has none right now)`,
      );
      const doc = docs[0];

      await page.goto(`/prosthetics/process/${instanceId}/wizard`);
      await expect(page.getByText('Бланк замірів №')).toBeVisible({ timeout: 15000 });

      const dateInput = page.getByLabel('Дата');
      const pibInput = page.getByLabel('П.І.Б');
      const addressInput = page.getByLabel('Адреса');
      const codeInput = page.getByLabel('Шифр виробу');
      const nameInput = page.getByLabel('Найменування виробу');
      const mobilityCombo = page.getByRole('combobox', { name: 'Рівень мобільності' });
      const genderSelect = page.getByLabel('Стать');
      const ageInput = page.getByLabel('Вік');
      const heightInput = page.getByLabel('Зріст');
      const weightInput = page.getByLabel('Вага');
      const notesInput = page.getByLabel('Примітки');
      const blankInput = page.getByLabel('Номер бланку замірів');

      // Wait for the async prefill chain (order + MIS documents) to land:
      // the first non-empty textual source is the signal.
      const signal =
        doc.patientFullName || doc.patientAddress || doc.productCode || doc.productName;
      if (signal) {
        await expect
          .poll(
            async () =>
              (await pibInput.inputValue()) ||
              (await addressInput.inputValue()) ||
              (await codeInput.inputValue()) ||
              (await nameInput.inputValue()),
            { timeout: 15000 },
          )
          .not.toBe('');
      } else {
        // Degenerate document: only the local date can prefill.
        await expect(dateInput).toHaveValue(localToday(), { timeout: 15000 });
      }

      // Full mapping assertions (only for sources the document provides).
      if (doc.orderNumber) {
        if (/^\d+(\.\d+)?$/.test(String(doc.orderNumber))) {
          await expect(blankInput).toHaveValue(String(doc.orderNumber));
        } else {
          // The blank field is numeric: browsers sanitise alphanumeric
          // prefill to empty rather than showing a misleading value.
          console.log(`[step1-prefill] non-numeric orderNumber=${doc.orderNumber}`);
          await expect(blankInput).toHaveValue('');
        }
      }
      await expect(dateInput).toHaveValue(localToday());
      if (doc.patientFullName) await expect(pibInput).toHaveValue(doc.patientFullName);
      if (doc.patientAddress) await expect(addressInput).toHaveValue(doc.patientAddress);
      if (doc.productCode) await expect(codeInput).toHaveValue(doc.productCode);
      if (doc.productName) await expect(nameInput).toHaveValue(doc.productName);
      if (doc.mobilityLevel && MOBILITY_OPTIONS.includes(doc.mobilityLevel)) {
        await expect(mobilityCombo).toContainText(doc.mobilityLevel);
      } else {
        await expect(mobilityCombo).toContainText('Рівень');
      }
      if (doc.patientGender === 'Чоловіча' || doc.patientGender === 'Жіноча') {
        await expect(genderSelect).toHaveValue(doc.patientGender);
      }
      if (doc.age !== undefined && doc.age !== null) {
        await expect(ageInput).toHaveValue(String(doc.age));
      }
      if (doc.height !== undefined && doc.height !== null) {
        await expect(heightInput).toHaveValue(String(doc.height));
      }
      if (doc.weight !== undefined && doc.weight !== null) {
        await expect(weightInput).toHaveValue(String(doc.weight));
      }
      if (doc.note) await expect(notesInput).toHaveValue(doc.note);

      // Mobility is a dropdown with exactly the 5 agreed options.
      await mobilityCombo.click();
      const mobilityList = page.getByRole('listbox');
      for (const option of MOBILITY_OPTIONS) {
        await expect(mobilityList.getByRole('option', { name: option })).toBeVisible();
      }
      await expect(mobilityList.getByRole('option')).toHaveCount(5);
      await page.keyboard.press('Escape');

      // Manual edits survive the wizard lifecycle (per-second timer
      // re-renders + any background refetch never overwrite filled fields).
      await pibInput.fill('Ручне ПІБ');
      await ageInput.fill('51');
      await page.waitForTimeout(3000);
      await expect(pibInput).toHaveValue('Ручне ПІБ');
      await expect(ageInput).toHaveValue('51');
      await expect(dateInput).toHaveValue(localToday());
      if (doc.productCode) await expect(codeInput).toHaveValue(doc.productCode);
    } finally {
      await terminateInstance(request, headers, instanceId);
    }
  });
});
