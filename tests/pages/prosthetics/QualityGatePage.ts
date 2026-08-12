import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for the Quality Gate (Screen 9)
 * Spec ref: Section 2.5.1 — Quality Gate
 */
export class QualityGatePage {
  readonly page: Page;
  readonly gateHeading: Locator;
  readonly gateDescription: Locator;
  readonly criteriaCheckboxes: Locator;
  readonly attachmentsArea: Locator;
  readonly passButton: Locator;
  readonly failButton: Locator;
  
  // Rework dialog
  readonly reworkDialog: Locator;
  readonly reworkTargetSelect: Locator;
  readonly reworkReasonInput: Locator;
  readonly confirmReworkButton: Locator;
  readonly cancelReworkButton: Locator;
  
  // Fail dialog
  readonly failDialog: Locator;
  readonly failReasonCategorySelect: Locator;
  readonly failDescriptionInput: Locator;
  readonly confirmFailButton: Locator;
  readonly cancelFailButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.gateHeading = page.getByRole('heading', { level: 1 }).or(page.getByRole('heading', { level: 2 })).first();
    this.gateDescription = page.locator('[class*="description"]').first();
    this.criteriaCheckboxes = page.locator('[data-slot="checkbox"], input[type="checkbox"]');
    this.attachmentsArea = page.locator('[class*="attachment"], [class*="upload"]').first();
    this.passButton = page.getByRole('button', { name: /Схвалити|Пройдено/ });
    this.failButton = page.getByRole('button', { name: /Відхилити|Не пройдено/ });
    
    // Rework dialog
    this.reworkDialog = page.getByRole('dialog');
    this.reworkTargetSelect = page.getByRole('combobox').filter({ hasText: /крок|етап/ });
    this.reworkReasonInput = page.getByLabel(/Причина|Коментар/).or(page.getByPlaceholder(/Причина/));
    this.confirmReworkButton = page.getByRole('button', { name: /Повернути на доопрацювання|Підтвердити/ });
    this.cancelReworkButton = page.getByRole('button', { name: /Скасувати/ });
    
    // Fail dialog
    this.failDialog = page.getByRole('dialog');
    this.failReasonCategorySelect = page.getByRole('combobox').filter({ hasText: /Категорія|Виробничий|Матеріали/ });
    this.failDescriptionInput = page.getByLabel(/Опис|Причина/).or(page.getByPlaceholder(/Опис/));
    this.confirmFailButton = page.getByRole('button', { name: /Позначити як провалений|Підтвердити/ });
    this.cancelFailButton = page.getByRole('button', { name: /Скасувати/ });
  }

  async checkAllCriteria() {
    const count = await this.criteriaCheckboxes.count();
    for (let i = 0; i < count; i++) {
      await this.criteriaCheckboxes.nth(i).click({ force: true }).catch(() => {});
    }
  }

  async passGate() {
    await this.passButton.click();
    await this.page.waitForTimeout(1000);
  }

  async clickFail() {
    await this.failButton.click();
    await this.page.waitForTimeout(500);
  }

  async failWithRework(targetStep: string, reason: string) {
    await this.clickFail();
    await this.reworkDialog.waitFor({ state: 'visible', timeout: 5000 });
    await this.selectReworkTarget(targetStep);
    await this.reworkReasonInput.fill(reason);
    await this.confirmReworkButton.click();
    await this.page.waitForTimeout(1000);
  }

  async failPermanently(category: string, description: string) {
    await this.clickFail();
    await this.failDialog.waitFor({ state: 'visible', timeout: 5000 });
    await this.selectFailCategory(category);
    await this.failDescriptionInput.fill(description);
    await this.confirmFailButton.click();
    await this.page.waitForTimeout(1000);
  }

  async selectReworkTarget(targetStep: string) {
    await this.reworkTargetSelect.click();
    await this.page.getByRole('option', { name: targetStep }).click();
  }

  async selectFailCategory(category: string) {
    await this.failReasonCategorySelect.click();
    await this.page.getByRole('option', { name: category }).click();
  }

  async verifyGateVisible() {
    await expect(this.gateHeading).toBeVisible({ timeout: 10000 });
    await expect(this.passButton).toBeVisible();
    await expect(this.failButton).toBeVisible();
  }

  async getCriteriaCount(): Promise<number> {
    return await this.criteriaCheckboxes.count();
  }
}
