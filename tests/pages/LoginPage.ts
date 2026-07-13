import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class LoginPage extends BasePage {
  readonly title: Locator;
  readonly loginField: Locator;
  readonly passwordField: Locator;
  readonly submitButton: Locator;
  readonly errorAlert: Locator;

  constructor(page: Page) {
    super(page);
    this.title = page.getByText('Карта інтенсивної терапії');
    this.loginField = page.getByLabel('Логін');
    this.passwordField = page.getByLabel('Пароль');
    this.submitButton = page.getByRole('button', { name: 'Увійти' });
    this.errorAlert = page.getByText('Невірний логін або пароль');
  }

  async goto(): Promise<void> {
    await this.page.goto('/login');
  }

  async loginAs(login: string, password: string): Promise<void> {
    await this.loginField.fill(login);
    await this.passwordField.fill(password);
    await this.submitButton.click();
  }

  async expectVisible(): Promise<void> {
    await expect(this.title).toBeVisible();
    await expect(this.loginField).toBeVisible();
    await expect(this.passwordField).toBeVisible();
    await expect(this.submitButton).toBeVisible();
  }

  async expectError(): Promise<void> {
    await expect(this.errorAlert).toBeVisible({ timeout: 10000 });
  }
}
