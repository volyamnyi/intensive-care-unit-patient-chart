import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class CreateCardPage extends BasePage {
  readonly title: Locator;
  readonly patientSearch: Locator;
  readonly createButton: Locator;
  readonly cancelButton: Locator;
  readonly patientDetails: Locator;

  constructor(page: Page) {
    super(page);
    this.title = page.getByText('Нова карта інтенсивної терапії');
    this.patientSearch = page.getByLabel('ПІБ, телефон або № медкарти');
    this.createButton = page.getByRole('button', { name: 'Створити карту' });
    this.cancelButton = page.getByRole('button', { name: 'Скасувати' });
    this.patientDetails = page.getByText('Дані пацієнта (з МІС)');
  }

  async goto(): Promise<void> {
    await this.page.goto('/icu/doctor/create-card');
  }

  async searchPatient(query: string): Promise<void> {
    await this.patientSearch.fill(query);
  }

  searchOption(text: string): Locator {
    return this.page.getByRole('option', { name: new RegExp(text) });
  }

  async selectPatient(text: string): Promise<void> {
    const option = this.searchOption(text);
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();
  }

  async clickCreate(): Promise<void> {
    await this.createButton.click();
  }

  async expectPatientDetailsVisible(): Promise<void> {
    await expect(this.patientDetails).toBeVisible({ timeout: 10000 });
  }
}
