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
  '/icu/doctor/episode/a3333333',
  '/prescriptions/doctor',
];

const PROSTHETIST_ROUTES = ['/prosthetics', '/prosthetics/new/select-patient'];

// OrderReviewPage redirects to select-order when the draft is empty; seed the
// draft so the real review layout (order details + sticky CTA) gets audited.
const SEED_PATIENT_ID = '900002';
const SEED_ORDER_ID = '20000000-0000-4000-8000-000000000002';

async function assertNoHorizontalScroll(page: Page, route: string) {
  await page.goto(route);
  await page.waitForLoadState('networkidle');
  const report = await page.evaluate(() => {
    const docWidth = document.documentElement.scrollWidth;
    const viewportWidth = window.innerWidth;
    const offenders: string[] = [];
    if (docWidth > viewportWidth + 1) {
      for (const el of Array.from(document.querySelectorAll('body *')) as HTMLElement[]) {
        const rect = el.getBoundingClientRect();
        if (rect.right > viewportWidth + 1 || rect.left < -1) {
          const tag = el.tagName.toLowerCase();
          const cls = String(el.className ?? '').slice(0, 120);
          offenders.push(
            `${tag}.${cls} left=${Math.round(rect.left)} right=${Math.round(rect.right)}`,
          );
          if (offenders.length >= 6) break;
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
    });

    test.describe('admin', () => {
      test.use({ storageState: '.auth/admin.json' });

      test('admin: /admin', async ({ page }) => {
        await assertNoHorizontalScroll(page, '/admin');
      });
    });
  });
}