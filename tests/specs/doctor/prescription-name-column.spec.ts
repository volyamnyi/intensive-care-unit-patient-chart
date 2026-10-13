import { test, expect } from '@playwright/test';
import { getToken, firstMedicationPatient, ensureMedicationList, ensureMedicationItem, firstCatalogMedicine } from '../../helpers/medication';

// Width-epic (screenshot-driven fix): the sticky drug-name column of the
// prescription grid got explicit wrap-guards — MIS catalog names pack
// name+form+strength+pack+producer into one 45+ char string, and at the old
// md:min-w-[140px] they wrapped into a 7-line tower that overran the row.
// Contract: 180px at md, 220px at lg+, overflow-wrap:anywhere + hyphens:auto
// + native title with the full name.
test('prescription detail: name column holds wrap guards at 768 and 1280', async ({ page, request }) => {
  const token = await getToken(request, 1);
  const patient = await firstMedicationPatient(request, token);
  const listId = await ensureMedicationList(request, token, patient.id);
  // The name column only renders when at least one item exists.
  await ensureMedicationItem(request, token, listId, await firstCatalogMedicine(request, token));
  await page.goto(`/prescriptions/doctor/${listId}`, { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Статус: Відкрито')).toBeVisible({ timeout: 15000 });

  for (const [width, expected] of [[768, 180], [1280, 220]] as const) {
    await page.setViewportSize({ width, height: 800 });
    const nameCell = page.locator('td[data-item-id]').first();
    await expect(nameCell).toBeVisible();
    const namePara = nameCell.locator('p[title]').first();
    await expect(namePara).toBeVisible();
    const minWidth = await nameCell.evaluate((el) => parseFloat(window.getComputedStyle(el).minWidth));
    const wrap = await namePara.evaluate((el) => {
      const cs = window.getComputedStyle(el);
      return { overflowWrap: cs.overflowWrap, hyphens: (cs as CSSStyleDeclaration & { hyphens?: string }).hyphens };
    });
    expect(minWidth).toBeGreaterThanOrEqual(expected);
    expect(wrap.overflowWrap).toBe('anywhere');
    expect(wrap.hyphens).toBe('auto');
    const title = await namePara.getAttribute('title');
    expect((title ?? '').length).toBeGreaterThan(5);
  }

  // The table itself may scroll inside its own container — the document must not.
  const doc = await page.evaluate(() => ({
    docWidth: document.documentElement.scrollWidth,
    viewportWidth: window.innerWidth,
  }));
  expect(doc.docWidth).toBeLessThanOrEqual(doc.viewportWidth + 1);
});
