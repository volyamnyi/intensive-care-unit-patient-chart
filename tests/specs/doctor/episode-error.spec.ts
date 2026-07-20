import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a3333333-3333-3333-3333-333333333333';

test.describe('Episode Page — error observability (F3)', () => {
  test('episode page does not white-screen when episode API fails', async ({ page }) => {
    // Force the episode fetch to fail. The SPA shell must stay mounted —
    // no blank screen / React render crash (regression guard for F3).
    await page.route('**/api/episodes/**', (route) => route.abort());

    await page.goto(`/doctor/episode/${EPISODE_ID}`);
    await page.waitForLoadState('domcontentloaded');

    // The app root must still contain rendered content (not a blank crash).
    const rootHtml = await page.locator('#root').innerHTML();
    expect(rootHtml.length).toBeGreaterThan(0);
    // URL is preserved (router did not bail out).
    expect(page.url()).toContain('/doctor/episode/');
  });
});
