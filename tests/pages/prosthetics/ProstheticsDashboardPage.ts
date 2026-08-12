import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for the Prosthetics Dashboard (Screen 2)
 * Spec ref: Section 2.2 — Dashboard
 */
export class ProstheticsDashboardPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly newProcessButton: Locator;
  readonly myProcessesButton: Locator;
  readonly searchInput: Locator;
  readonly tabs: { all: Locator; active: Locator; paused: Locator; completed: Locator; failed: Locator };
  readonly processTable: Locator;
  readonly emptyStateMessage: Locator;
  readonly statCards: { active: Locator; paused: Locator; completed: Locator; failed: Locator };
  readonly logoutButton: Locator;
  readonly headerLogo: Locator;
  readonly userName: Locator;
  readonly notificationsIndicator: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /Виробництво протезів/ });
    this.newProcessButton = page.getByRole('button', { name: /Новий процес/ });
    this.myProcessesButton = page.getByRole('button', { name: /Мої процеси/ });
    this.searchInput = page.getByPlaceholder(/Пошук за номером замовлення або пацієнтом/);
    this.tabs = {
      all: page.getByRole('tab', { name: 'Всі' }),
      active: page.getByRole('tab', { name: 'Активні' }),
      paused: page.getByRole('tab', { name: 'Призупинені' }),
      completed: page.getByRole('tab', { name: 'Завершені' }),
      failed: page.getByRole('tab', { name: 'Провалені' }),
    };
    this.processTable = page.getByRole('table');
    this.emptyStateMessage = page.getByText(/Немає процесів за поточним фільтром/);
    this.statCards = {
      active: page.locator('[data-stat="active"]'),
      paused: page.locator('[data-stat="paused"]'),
      completed: page.locator('[data-stat="completed"]'),
      failed: page.locator('[data-stat="failed"]'),
    };
    this.logoutButton = page.getByRole('button', { name: /Вийти/ });
    this.headerLogo = page.locator('header img, [class*="logo"]').first();
    this.userName = page.locator('[class*="user-name"], [class*="userName"]').first();
    this.notificationsIndicator = page.locator('[class*="notification"], [class*="bell"]').first();
  }

  async goto() {
    await this.page.goto('/prosthetics');
  }

  async clickNewProcess() {
    await this.newProcessButton.click();
  }

  async clickMyProcesses() {
    await this.myProcessesButton.click();
  }

  async filterBy(tab: 'all' | 'active' | 'paused' | 'completed' | 'failed') {
    await this.tabs[tab].click();
  }

  async search(query: string) {
    await this.searchInput.fill(query);
  }

  async getProcessCount(): Promise<number> {
    const rows = this.processTable.locator('tbody tr');
    return rows.count();
  }

  async openProcess(index: number) {
    const rows = this.processTable.locator('tbody tr');
    await rows.nth(index).getByRole('button', { name: /Відкрити/ }).click();
  }

  async hasProcessTable(): Promise<boolean> {
    return await this.processTable.isVisible({ timeout: 3000 }).catch(() => false);
  }

  async hasEmptyState(): Promise<boolean> {
    return await this.emptyStateMessage.isVisible({ timeout: 3000 }).catch(() => false);
  }
}
