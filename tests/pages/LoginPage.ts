import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class LoginPage extends BasePage {
  readonly title: Locator;
  readonly subtitle: Locator;
  readonly loginField: Locator;
  readonly passwordField: Locator;
  readonly submitButton: Locator;
  readonly errorAlert: Locator;

  constructor(page: Page) {
    super(page);
    this.title = page.getByText('Карта інтенсивної терапії');
    this.subtitle = page.getByText('Вхід до системи');
    this.loginField = page.getByLabel('Логін');
    this.passwordField = page.getByLabel('Пароль');
    this.submitButton = page.getByRole('button', { name: 'Увійти' });
    this.errorAlert = page.getByText('Невірний логін або пароль');
  }

  async navigate(): Promise<void> {
    await this.page.goto('/login');
    await this.delay();
  }

  async fillLogin(login: string): Promise<void> {
    await this.loginField.click();
    await this.loginField.fill(login);
    await this.delay();
  }

  async fillPassword(password: string): Promise<void> {
    await this.passwordField.click();
    await this.passwordField.fill(password);
    await this.delay();
  }

  async clickSubmit(): Promise<void> {
    await this.submitButton.click();
    await this.delay();
  }

  async loginAs(login: string, password: string): Promise<void> {
    await this.fillLogin(login);
    await this.fillPassword(password);
    await this.clickSubmit();
  }

  async expectLoginFormVisible(): Promise<void> {
    await expect(this.title).toBeVisible();
    await expect(this.subtitle).toBeVisible();
    await expect(this.loginField).toBeVisible();
    await expect(this.passwordField).toBeVisible();
    await expect(this.submitButton).toBeVisible();
  }

  async expectLoginError(): Promise<void> {
    await expect(this.errorAlert).toBeVisible({ timeout: 10000 });
  }
}
