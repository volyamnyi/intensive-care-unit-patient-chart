import { Page, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8085';

export class BasePage {
  protected page: Page;
  protected apiCalls: { method: string; url: string; status: number; duration: number }[] = [];

  constructor(page: Page) {
    this.page = page;
  }

  async startApiLogging(): Promise<void> {
    this.apiCalls = [];
    await this.page.route(`${API_BASE}/**`, async (route) => {
      const start = performance.now();
      try {
        const response = await route.fetch();
        const duration = Math.round(performance.now() - start);
        this.apiCalls.push({
          method: route.request().method(),
          url: route.request().url(),
          status: response.status(),
          duration,
        });
        await route.fulfill({ response });
      } catch {
        this.apiCalls.push({
          method: route.request().method(),
          url: route.request().url(),
          status: 0,
          duration: Math.round(performance.now() - start),
        });
        await route.continue();
      }
    });
  }

  getApiLog(): { method: string; url: string; status: number; duration: number }[] {
    return [...this.apiCalls];
  }

  clearApiLog(): void {
    this.apiCalls = [];
  }

  async measure<T>(label: string, fn: () => Promise<T>): Promise<{ result: T; durationMs: number }> {
    const start = performance.now();
    const result = await fn();
    const durationMs = Math.round(performance.now() - start);
    return { result, durationMs };
  }

  async takeScreenshot(name: string): Promise<void> {
    await this.page.screenshot({ path: `screenshots/${name}.png`, fullPage: true });
  }

  async expectUrl(urlPattern: RegExp | string): Promise<void> {
    await expect(this.page).toHaveURL(urlPattern);
  }
}
