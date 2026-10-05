import { test, expect } from '../../fixtures/index';
import type { Page } from '@playwright/test';

// E2E for the «Всі пацієнти» pool on /prescriptions/doctor (epic #339, F3 #343).
//
// Stub-data contract (MIS stub, fixed IDs): the pool is the RAW MIS roster
// (any stay status), unlike the main roster (under-treatment only, dept
// 19/37). Fixture anchors (tests/mis-stub/fixtures.json):
//   10601/10602 — PRG (dept 19/37), label «В ході», emerald tint;
//   10607 — DED (dept 19), label «Померлий», zinc tint, pool-ONLY row
//     (terminal codes never reach the main roster);
//   10401 — MOV, label «Переведено»;
//   10602 (dept 37) stays pool-only while the surgery tab is active.
// No assertions depend on live MIS data; totalElements is stub-pinned (19).

test.describe('Doctor — «Всі пацієнти» pool (MIS statuses)', () => {
  // The pool table lives inside the expandable «Всі пацієнти» section; the
  // main roster above it can hold the SAME patient (e.g. 10601, PRG dept 19),
  // so every pool-row locator must be scoped here — never page-wide.
  function poolSection(page: Page) {
    return page.locator('div.mt-4', {
      has: page.getByRole('button', { name: 'Всі пацієнти' }),
    });
  }

  function poolRow(page: Page, patientId: string) {
    return poolSection(page).locator('tbody tr', {
      has: page.getByRole('cell', { name: patientId, exact: true }),
    });
  }

  test('«Усі» loads unfiltered, shows PRG label + tint + totals', async ({ page }) => {
    await page.goto('/prescriptions/doctor', { waitUntil: 'domcontentloaded' });

    const poolReq = page.waitForResponse(
      (r) => r.url().includes('/api/patients/pool') && r.ok(),
      { timeout: 15_000 },
    );
    await page.getByRole('button', { name: 'Всі пацієнти' }).click();
    const res = await poolReq;
    expect(new URL(res.url()).searchParams.get('status')).toBeNull();

    // Pager carries the server-side total (stub roster = 19).
    await expect(page.getByText('Всього 19')).toBeVisible({ timeout: 15_000 });

    // DED row exists ONLY in the pool (terminal → excluded from roster).
    const dedRow = poolRow(page, '10607');
    await expect(dedRow).toBeVisible({ timeout: 15_000 });
    await expect(dedRow.getByText('Померлий')).toBeVisible();
    await expect(dedRow).toHaveClass(/bg-zinc-200/);

    // PRG dept-37 row is pool-only while the surgery tab is active.
    const prgRow = poolRow(page, '10602');
    await expect(prgRow).toBeVisible({ timeout: 15_000 });
    await expect(prgRow.getByText('В ході')).toBeVisible();
    await expect(prgRow).toHaveClass(/bg-emerald-50/);
  });

  test('status chips filter server-side and keep working', async ({ page }) => {
    await page.goto('/prescriptions/doctor', { waitUntil: 'domcontentloaded' });
    await page.getByRole('button', { name: 'Всі пацієнти' }).click();
    const statusGroup = page.getByRole('group', { name: 'Статус' });
    await expect(statusGroup).toBeVisible({ timeout: 15_000 });

    // PRG chip → ?status=PRG, DED row disappears, PRG rows stay.
    const prgReq = page.waitForResponse(
      (r) => r.url().includes('/api/patients/pool') && r.url().includes('status=PRG') && r.ok(),
      { timeout: 15_000 },
    );
    await statusGroup.getByRole('button', { name: 'В ході' }).click();
    await prgReq;
    await expect(poolRow(page, '10601')).toBeVisible({ timeout: 15_000 });
    await expect(poolRow(page, '10607')).toHaveCount(0);

    // MOV chip → ?status=MOV, stub MOV patient 10401 listed as «Переведено».
    const movReq = page.waitForResponse(
      (r) => r.url().includes('/api/patients/pool') && r.url().includes('status=MOV') && r.ok(),
      { timeout: 15_000 },
    );
    await statusGroup.getByRole('button', { name: 'Переведено' }).click();
    await movReq;
    const movRow = poolRow(page, '10401');
    await expect(movRow).toBeVisible({ timeout: 15_000 });
    await expect(movRow.getByText('Переведено')).toBeVisible();
  });
});
