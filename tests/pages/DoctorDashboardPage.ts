import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class DoctorDashboardPage extends BasePage {
  readonly title: Locator;
  readonly newCardButton: Locator;
  readonly searchField: Locator;
  readonly patientTable: Locator;
  readonly loadingSpinner: Locator;
  readonly patientCards: Locator;
  readonly emptyState: Locator;
  readonly appBarTitle: Locator;
  readonly navPatients: Locator;
  readonly userMenu: Locator;
  readonly logoutButton: Locator;

  constructor(page: Page) {
    super(page);
    this.title = page.getByText('Активні пацієнти ВАІТ');
    this.newCardButton = page.getByRole('button', { name: 'Нова карта' });
    this.searchField = page.getByPlaceholder('Пошук пацієнта за ПІБ...');
    this.patientTable = page.getByRole('table');
    this.loadingSpinner = page.getByRole('progressbar');
    this.patientCards = page.locator('tbody tr').filter({ has: page.getByRole('button', { name: 'Відкрити' }) });
    this.emptyState = page.getByText('Немає пацієнтів за запитом');
    this.appBarTitle = page.getByText('Карта інтенсивної терапії').first();
    this.navPatients = page.getByRole('button', { name: 'Пацієнти' });
    this.userMenu = page.getByRole('button', { name: /меню користувача/i });
    this.logoutButton = page.getByRole('menuitem', { name: 'Вийти' });
  }

  async navigate(): Promise<void> {
    await this.page.goto('/doctor');
    await this.page.waitForLoadState('networkidle');
    await this.delay();
  }

  getRows(): Locator {
    return this.page.locator('tbody tr').filter({ has: this.page.getByRole('button', { name: 'Відкрити' }) });
  }

  openButtonByName(patientName: string): Locator {
    return this.page
      .getByRole('row')
      .filter({ hasText: patientName })
      .getByRole('button', { name: /Відкрити/ });
  }

  async clickNewCard(): Promise<void> {
    await this.newCardButton.click();
    await this.delay();
  }

  async searchPatient(text: string): Promise<void> {
    await this.searchField.click();
    await this.searchField.fill(text);
    await this.delay();
  }

  async clickOpenByName(patientName: string): Promise<void> {
    await this.openButtonByName(patientName).click();
    await this.delay();
  }

  async openUserMenu(): Promise<void> {
    await this.userMenu.click();
    await this.delay();
  }

  async clickLogout(): Promise<void> {
    await this.openUserMenu();
    await this.logoutButton.click();
    await this.delay();
  }

  async expectPatientCards(count: number): Promise<void> {
    const rows = this.getRows();
    await expect(rows).toHaveCount(count);
  }

  async expectEmptyStateVisible(): Promise<void> {
    await expect(this.emptyState).toBeVisible();
  }
}
