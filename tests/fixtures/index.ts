import { test as base, type Page, type Locator } from '@playwright/test';

type RoleFixtures = {
  doctorPage: Page;
  nursePage: Page;
  adminPage: Page;
  getByRoleName: (role: string, name: string | RegExp) => Locator;
};

export const test = base.extend<RoleFixtures>({
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
  adminPage: async ({ browser }, use) => {
    const ctx = await browser.newContext({ storageState: '.auth/admin.json' });
    const page = await ctx.newPage();
    await use(page);
    await ctx.close();
  },
  getByRoleName: async ({ page }, use) => {
    const fn = (role: string, name: string | RegExp) => page.getByRole(role as any, { name });
    await use(fn);
  },
});

export { expect } from '@playwright/test';
