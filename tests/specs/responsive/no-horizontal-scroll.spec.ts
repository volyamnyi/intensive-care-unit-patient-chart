import { test, expect, type Page } from '@playwright/test';

// Responsive UI Phase 6 (issue #165): no audited route may overflow the
// document horizontally at any breakpoint. Inner scroll containers (tables,
// grids, spreadsheets) are allowed — the page itself must never scroll
// sideways. On failure the message lists the first offending elements with
// their class names and bounding rects to speed up triage.

const VIEWPORTS = [
  { name: 'mobile 360x740', width: 360, height: 740 },
  { name: 'tablet 768x1024', width: 768, height: 1024 },
  { name: 'desktop 1280x800', width: 1280, height: 800 },
] as const;

const DOCTOR_ROUTES = [
  '/icu/doctor',
  '/icu/doctor/create-card',
  '/icu/doctor/episode/a3333333-3333-3333-3333-333333333333',
  '/prescriptions/doctor',
];

const PROSTHETIST_ROUTES = ['/prosthetics', '/prosthetics/new/select-patient'];

// Phase 5 (#179): every clinical route is audited — nurse surfaces included.
const NURSE_ROUTES = [
  '/icu/nurse',
  '/icu/nurse/episode/a3333333-3333-3333-3333-333333333333',
  '/prescriptions/nurse',
];

const HOD_ROUTES = ['/icu/doctor/department'];

// OrderReviewPage redirects to select-order when the draft is empty; seed the
// draft so the real review layout (order details + sticky CTA) gets audited.
const SEED_PATIENT_ID = '900002';
const SEED_ORDER_ID = '20000000-0000-4000-8000-000000000002';

async function assertNoHorizontalScroll(page: Page, route: string) {
  await page.goto(route, { waitUntil: 'domcontentloaded' });
  // Layout can shift after async fonts/data settle — poll scrollWidth until two
  // consecutive samples agree (condition-based, no sleep, no network-silence).
  let previousWidth = Number.NaN;
  await expect
    .poll(
      async () => {
        const current = await page.evaluate(() => document.documentElement.scrollWidth);
        const stable = current === previousWidth;
        previousWidth = current;
        return { stable, width: current };
      },
      { timeout: 15000, intervals: [250, 500, 1000] },
    )
    .toMatchObject({ stable: true });
  const report = await page.evaluate(() => {
    const docWidth = document.documentElement.scrollWidth;
    const viewportWidth = window.innerWidth;
    const offenders: string[] = [];
    if (docWidth > viewportWidth + 1) {
      const limit = 6;
      const elements = Array.from(document.querySelectorAll('body *')) as HTMLElement[];
      for (const el of elements) {
        // Skip elements inside horizontal scroll containers (tables, grids) —
        // their content legitimately extends past the viewport.
        let parent = el.parentElement;
        let insideScrollContainer = false;
        while (parent) {
          const overflowX = window.getComputedStyle(parent).overflowX;
          if (overflowX === 'auto' || overflowX === 'scroll' || overflowX === 'hidden') {
            insideScrollContainer = true;
            break;
          }
          parent = parent.parentElement;
        }
        if (insideScrollContainer) continue;

        const rect = el.getBoundingClientRect();
        if (rect.right > viewportWidth + 1 || rect.left < -1) {
          const tag = el.tagName.toLowerCase();
          const cls = String(el.className ?? '').slice(0, 120);
          offenders.push(
            `${tag}.${cls} left=${Math.round(rect.left)} right=${Math.round(rect.right)}`,
          );
          if (offenders.length >= limit) break;
        }
      }
    }
    return { docWidth, viewportWidth, offenders };
  });
  expect(
    report.docWidth,
    `horizontal overflow on ${route} — offenders: ${report.offenders.join(' | ') || 'none listed'}`,
  ).toBeLessThanOrEqual(report.viewportWidth + 1);
}

for (const viewport of VIEWPORTS) {
  test.describe(`no horizontal scroll @ ${viewport.name}`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    for (const route of DOCTOR_ROUTES) {
      test(`doctor: ${route}`, async ({ page }) => {
        await assertNoHorizontalScroll(page, route);
      });
    }

    test.describe('prosthetist', () => {
      test.use({ storageState: '.auth/prosthetist.json' });

      for (const route of PROSTHETIST_ROUTES) {
        test(`prosthetist: ${route}`, async ({ page }) => {
          await assertNoHorizontalScroll(page, route);
        });
      }

      test('prosthetist: /prosthetics/new/review-order (with draft)', async ({ page }) => {
        await page.addInitScript(
          ([patientId, orderId]) => {
            window.localStorage.setItem(
              'prosthetics:draft',
              JSON.stringify({
                patientId,
                orderId,
                templateId: null,
                instanceId: null,
              }),
            );
          },
          [SEED_PATIENT_ID, SEED_ORDER_ID],
        );
        await assertNoHorizontalScroll(page, '/prosthetics/new/review-order');
      });

      test('prosthetist: /prosthetics/new/select-order (with patient draft)', async ({ page }) => {
        await page.addInitScript(
          (patientId) => {
            window.localStorage.setItem(
              'prosthetics:draft',
              JSON.stringify({
                patientId,
                orderId: null,
                templateId: null,
                instanceId: null,
              }),
            );
          },
          SEED_PATIENT_ID,
        );
        await assertNoHorizontalScroll(page, '/prosthetics/new/select-order');
      });
      test('prosthetist: /prosthetics/new/select-template (with order draft)', async ({ page }) => {
        await page.addInitScript(
          ([patientId, orderId]) => {
            window.localStorage.setItem(
              'prosthetics:draft',
              JSON.stringify({
                patientId,
                orderId,
                templateId: null,
                instanceId: null,
              }),
            );
          },
          [SEED_PATIENT_ID, SEED_ORDER_ID],
        );
        await assertNoHorizontalScroll(page, '/prosthetics/new/select-template');
      });
    });

    test.describe('nurse', () => {
      test.use({ storageState: '.auth/nurse.json' });

      for (const route of NURSE_ROUTES) {
        test(`nurse: ${route}`, async ({ page }) => {
          await assertNoHorizontalScroll(page, route);
        });
      }
    });

    test.describe('hod', () => {
      test.use({ storageState: '.auth/hod.json' });

      for (const route of HOD_ROUTES) {
        test(`hod: ${route}`, async ({ page }) => {
          await assertNoHorizontalScroll(page, route);
        });
      }
    });

    test.describe('admin', () => {
      test.use({ storageState: '.auth/admin.json' });

      test('admin: /admin', async ({ page }) => {
        await assertNoHorizontalScroll(page, '/admin');
      });
    });
  });
}