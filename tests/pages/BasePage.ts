import { Page, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8085';

export class BasePage {
  protected page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async navigate(url: string): Promise<void> {
    await this.page.goto(url);
    await this.page.waitForLoadState('networkidle');
  }

  async expectUrl(urlPattern: RegExp | string): Promise<void> {
    await expect(this.page).toHaveURL(urlPattern);
  }

  async takeScreenshot(name: string): Promise<void> {
    await this.page.screenshot({ path: `screenshots/${name}.png`, fullPage: true });
  }
}
