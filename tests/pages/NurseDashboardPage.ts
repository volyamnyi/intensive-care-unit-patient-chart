import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class NurseDashboardPage extends BasePage {
  readonly title: Locator;
  readonly searchField: Locator;

  constructor(page: Page) {
    super(page);
    this.title = page.getByText('Активні пацієнти');
    this.searchField = page.getByPlaceholder('Пошук пацієнта за ПІБ...');
  }

  async goto(): Promise<void> {
    await this.page.goto('/icu/nurse');
    await this.page.waitForLoadState('networkidle');
  }

  getRowByPatient(name: string): Locator {
    return this.page.getByRole('row').filter({ hasText: name });
  }

  async openPatient(name: string): Promise<void> {
    await this.getRowByPatient(name).getByRole('button', { name: 'Відкрити' }).click();
  }
}
