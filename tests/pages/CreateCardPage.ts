import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class CreateCardPage extends BasePage {
  readonly title: Locator;
  readonly errorAlert: Locator;
  readonly patientSearch: Locator;
  readonly minCharsHint: Locator;
  readonly noResultsHint: Locator;
  readonly diagnosisField: Locator;
  readonly apacheField: Locator;
  readonly sofaField: Locator;
  readonly createButton: Locator;
  readonly cancelButton: Locator;

  constructor(page: Page) {
    super(page);
    this.title = page.getByText('Нова карта інтенсивної терапії');
    this.errorAlert = page.getByRole('alert');
    this.patientSearch = page.getByLabel('ПІБ, телефон або № медкарти');
    this.minCharsHint = page.getByText('Введіть мінімум 2 символи');
    this.noResultsHint = page.getByText('Пацієнтів не знайдено');
    this.diagnosisField = page.getByLabel('Діагноз');
    this.apacheField = page.getByLabel('APACHE II');
    this.sofaField = page.getByLabel('SOFA');
    this.createButton = page.getByRole('button', { name: 'Створити карту' });
    this.cancelButton = page.getByRole('button', { name: 'Скасувати' });
  }

  async navigate(): Promise<void> {
    await this.page.goto('/doctor/create-card');
  }

  async searchPatient(query: string): Promise<void> {
    await this.patientSearch.click();
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

  async fillDiagnosis(text: string): Promise<void> {
    await this.diagnosisField.click();
    await this.diagnosisField.fill(text);
  }

  async fillApache(value: string): Promise<void> {
    await this.apacheField.click();
    await this.apacheField.fill(value);
  }

  async fillSofa(value: string): Promise<void> {
    await this.sofaField.click();
    await this.sofaField.fill(value);
  }

  async clickCreate(): Promise<void> {
    await this.createButton.click();
  }

  async clickCancel(): Promise<void> {
    await this.cancelButton.click();
  }

  getReadOnlyField(label: string): Locator {
    return this.page.getByLabel(label);
  }
}
