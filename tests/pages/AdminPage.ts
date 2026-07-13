import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class AdminPage extends BasePage {
  readonly appBarTitle: Locator;
  readonly userMenuButton: Locator;
  readonly userNameMenuItem: Locator;
  readonly logoutMenuItem: Locator;
  readonly sectionTitle: Locator;
  readonly doctorsTable: Locator;
  readonly nursesTable: Locator;
  readonly loadingSpinner: Locator;
  readonly emptyDoctors: Locator;
  readonly emptyNurses: Locator;

  constructor(page: Page) {
    super(page);
    this.appBarTitle = page.getByText('Панель адміністратора');
    this.userMenuButton = page.getByRole('button', { name: /меню користувача/i });
    this.userNameMenuItem = page.getByRole('menuitem').first();
    this.logoutMenuItem = page.getByRole('menuitem', { name: 'Вийти' });
    this.sectionTitle = page.getByText('Користувачі системи');
    this.doctorsTable = page.getByText('Лікарі').locator('..').getByRole('table');
    this.nursesTable = page.getByText('Медсестри').locator('..').getByRole('table');
    this.loadingSpinner = page.getByRole('progressbar');
    this.emptyDoctors = page.getByText('Немає даних');
    this.emptyNurses = page.getByText('Немає даних');
  }

  async navigate(): Promise<void> {
    await this.page.goto('/admin');
    await this.page.waitForLoadState('networkidle');
  }

  async openUserMenu(): Promise<void> {
    await this.userMenuButton.click();
  }

  async clickLogout(): Promise<void> {
    await this.openUserMenu();
    await this.logoutMenuItem.click();
  }

  async expectUserInTable(tableLocator: Locator, login: string): Promise<void> {
    await expect(tableLocator.getByText(login)).toBeVisible({ timeout: 10000 });
  }

  getDoctorRow(login: string): Locator {
    return this.doctorsTable.getByRole('row').filter({ hasText: login });
  }

  getNurseRow(login: string): Locator {
    return this.nursesTable.getByRole('row').filter({ hasText: login });
  }
}
