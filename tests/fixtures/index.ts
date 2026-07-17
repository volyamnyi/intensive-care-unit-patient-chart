import { test as base, type Page } from '@playwright/test';

export const test = base.extend<{
  doctorPage: Page;
  nursePage: Page;
  hodPage: Page;
  adminPage: Page;
}>({
  doctorPage: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: '.auth/doctor.json' });
    const page = await ctx.newPage();
    await use(page);
    await ctx.close();
  },
  nursePage: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: '.auth/nurse.json' });
    const page = await ctx.newPage();
    await use(page);
    await ctx.close();
  },
  hodPage: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: '.auth/hod.json' });
    const page = await ctx.newPage();
    await use(page);
    await ctx.close();
  },
  adminPage: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: '.auth/admin.json' });
    const page = await ctx.newPage();
    await use(page);
    await ctx.close();
  },
});

export { expect } from '@playwright/test';
