import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class DoctorDashboardPage extends BasePage {
  readonly title: Locator;
  readonly newCardButton: Locator;
  readonly searchField: Locator;
  readonly loadingSpinner: Locator;

  constructor(page: Page) {
    super(page);
    this.title = page.getByText('Активні пацієнти');
    this.newCardButton = page.getByRole('button', { name: 'Нова карта' });
    this.searchField = page.getByPlaceholder('Пошук пацієнта за ПІБ...');
    this.loadingSpinner = page.getByRole('progressbar');
  }

  async goto(): Promise<void> {
    await this.page.goto('/doctor');
    await this.page.waitForLoadState('networkidle');
  }

  getRowByPatient(name: string): Locator {
    return this.page.getByRole('row').filter({ hasText: name });
  }

  openButtonByPatient(name: string): Locator {
    return this.getRowByPatient(name).getByRole('button', { name: 'Відкрити' });
  }

  async clickNewCard(): Promise<void> {
    await this.newCardButton.click();
  }

  async searchPatient(text: string): Promise<void> {
    await this.searchField.fill(text);
  }

  async openPatient(name: string): Promise<void> {
    await this.openButtonByPatient(name).click();
  }

  async expectPatientVisible(name: string): Promise<void> {
    await expect(this.page.getByText(name)).toBeVisible({ timeout: 10000 });
  }
}
