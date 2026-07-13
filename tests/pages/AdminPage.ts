import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class AdminPage extends BasePage {
  readonly title: Locator;
  readonly userMenuButton: Locator;
  readonly logoutMenuItem: Locator;

  constructor(page: Page) {
    super(page);
    this.title = page.getByText('Користувачі системи');
    this.userMenuButton = page.getByRole('button', { name: /меню користувача/i });
    this.logoutMenuItem = page.getByRole('menuitem', { name: 'Вийти' });
  }

  async goto(): Promise<void> {
    await this.page.goto('/admin');
    await this.page.waitForLoadState('networkidle');
  }

  getDoctorsTable(): Locator {
    return this.page.getByText('Лікарі').locator('..').getByRole('table');
  }

  getNursesTable(): Locator {
    return this.page.getByText('Медсестри').locator('..').getByRole('table');
  }

  async expectUserInTable(table: Locator, login: string): Promise<void> {
    await expect(table.getByText(login)).toBeVisible({ timeout: 10000 });
  }
}
