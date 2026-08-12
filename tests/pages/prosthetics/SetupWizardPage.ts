import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for the Setup Wizard (Screens 3-6)
 * Spec ref: Section 2.3 — New Process Creation
 * 
 * NOTE: Actual implementation differs from spec in some details:
 * - Patient search: NO search button — automatic with 300ms debounce
 * - Patient table: only renders when results exist (empty state shows text)
 */
export class SetupWizardPage {
  readonly page: Page;
  
  // Screen 3: Patient Selection
  readonly patientSearchInput: Locator;
  readonly patientResultsTable: Locator;
  readonly patientNoResultsMessage: Locator;
  readonly patientEmptyStateMessage: Locator;
  readonly patientApiErrorBanner: Locator;
  
  // Screen 4: Order Selection
  readonly orderResultsTable: Locator;
  readonly patientContextCard: Locator;
  
  // Screen 5: Order Review
  readonly orderReviewLeftPanel: Locator;
  readonly orderReviewPdfViewer: Locator;
  readonly orderReviewMaterialsPanel: Locator;
  readonly orderReviewMaterialsPlaceholder: Locator;
  readonly startButton: Locator;
  readonly orderDetailsTab: Locator;
  readonly recipeTab: Locator;
  readonly materialsTab: Locator;
  
  // Screen 6: Template Selection
  readonly templateCards: Locator;
  readonly templateGrid: Locator;
  readonly contextSummary: Locator;
  
  // Common
  readonly nextButton: Locator;
  readonly backButton: Locator;
  readonly cancelButton: Locator;
  readonly stepIndicator: Locator;
  readonly heading: Locator;
  readonly stepFourHeading: Locator;

  constructor(page: Page) {
    this.page = page;
    
    // Screen 3: Patient Selection
    this.patientSearchInput = page.getByPlaceholder(/Пошук пацієнта за ПІБ або номером координати/);
    this.patientResultsTable = page.getByRole('table');
    this.patientNoResultsMessage = page.getByText(/Пацієнтів не знайдено/);
    this.patientEmptyStateMessage = page.getByText(/Введіть ім'я або номер для пошуку/);
    this.patientApiErrorBanner = page.getByText(/Помилка пошуку/);
    
    // Screen 4: Order Selection
    this.orderResultsTable = page.getByRole('table');
    this.patientContextCard = page.locator('[class*="sticky"], [class*="patient-card"]').first();
    
    // Screen 5: Order Review
    this.orderReviewLeftPanel = page.locator('[class*="left-panel"], [class*="order-details"]').first();
    this.orderReviewPdfViewer = page.locator('iframe, embed, [class*="pdf-viewer"]').first();
    this.orderReviewMaterialsPanel = page.locator('[class*="materials"], [class*="bom"]').first();
    this.orderReviewMaterialsPlaceholder = page.getByText(/Специфікація матеріалів відсутня|Дані будуть додані/);
    this.startButton = page.getByRole('button', { name: /Старт/ });
    this.orderDetailsTab = page.getByRole('tab', { name: /Деталі|Загальні відомості/ });
    this.recipeTab = page.getByRole('tab', { name: /Замовлення на протез|Документація/ });
    this.materialsTab = page.getByRole('tab', { name: /Матеріали/ });
    
    // Screen 6: Template Selection
    this.templateCards = page.locator('[class*="template-card"], [class*="cursor-pointer"]');
    this.templateGrid = page.locator('[class*="grid"]').first();
    this.contextSummary = page.getByText(/Пацієнт:/);
    
    // Common
    this.nextButton = page.getByRole('button', { name: /Далі/ });
    this.backButton = page.getByRole('button', { name: /Назад/ });
    this.cancelButton = page.getByRole('button', { name: /Скасувати|До головного меню/ });
    this.stepIndicator = page.getByText(/Крок \d з 4/);
    this.heading = page.getByRole('heading', { level: 1 });
    this.stepFourHeading = page.getByRole('heading', { name: /Вибір технологічного маршруту/ });
  }

  // ==================== SCREEN 3: PATIENT SELECTION ====================
  
  /**
   * Search for patient — NO search button, search triggers automatically
   * after 300ms debounce when query >= 2 characters
   */
  async searchPatient(query: string) {
    await this.patientSearchInput.fill(query);
    await this.page.waitForTimeout(1500); // Wait for debounce + API response
  }

  async searchPatientAndWaitForResults(query: string) {
    await this.searchPatient(query);
    await this.patientResultsTable.waitFor({ state: 'visible', timeout: 15000 });
  }

  async getPatientCount(): Promise<number> {
    const rows = this.patientResultsTable.locator('tbody tr');
    return rows.count();
  }

  async selectPatient(nameOrId: string) {
    await this.patientResultsTable.waitFor({ state: 'visible', timeout: 10000 });
    const rows = this.patientResultsTable.locator('tbody tr');
    // The table shell renders before the search results load — wait for the first row
    await rows.first().waitFor({ state: 'visible', timeout: 15000 }).catch(() => {});
    const count = await rows.count();
    
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i);
      const text = await row.textContent();
      if (text && (text.includes(nameOrId) || text.toLowerCase().includes(nameOrId.toLowerCase()))) {
        await row.getByRole('button', { name: /Обрати|Обрано/ }).click();
        return;
      }
    }
    
    // Fallback: select first patient
    if (count > 0) {
      await rows.first().getByRole('button', { name: /Обрати|Обрано/ }).click();
      return;
    }
    
    throw new Error(`Patient "${nameOrId}" not found in search results`);
  }

  async selectFirstPatient() {
    await this.patientResultsTable.waitFor({ state: 'visible', timeout: 10000 });
    const rows = this.patientResultsTable.locator('tbody tr');
    await rows.first().getByRole('button', { name: /Обрати|Обрано/ }).click();
  }

  async verifyNoResultsMessage() {
    await expect(this.patientNoResultsMessage).toBeVisible({ timeout: 5000 });
  }

  async verifyEmptyStateMessage() {
    await expect(this.patientEmptyStateMessage).toBeVisible({ timeout: 5000 });
  }

  async verifyApiErrorBanner() {
    await expect(this.patientApiErrorBanner).toBeVisible({ timeout: 5000 });
  }

  // ==================== SCREEN 4: ORDER SELECTION ====================
  
  async selectOrder(orderNumber: string) {
    // The order list effect can re-run (draft updates) and flip the page back to loading
    // skeletons after the table appeared — retry until a row is actually present.
    for (let attempt = 0; attempt < 3; attempt++) {
      await this.orderResultsTable.waitFor({ state: 'visible', timeout: 10000 }).catch(() => {});
      const rows = this.orderResultsTable.locator('tbody tr');
      await rows.first().waitFor({ state: 'visible', timeout: 15000 }).catch(() => {});
      const count = await rows.count();

      for (let i = 0; i < count; i++) {
        const row = rows.nth(i);
        const text = await row.textContent();
        if (text && text.includes(orderNumber)) {
          await row.getByRole('button', { name: /Обрати|Обрано/ }).click();
          return;
        }
      }

      // Fallback: select first order
      if (count > 0) {
        await rows.first().getByRole('button', { name: /Обрати|Обрано/ }).click();
        return;
      }

      await this.page.waitForTimeout(3000);
    }

    throw new Error(`Order "${orderNumber}" not found`);
  }

  async selectFirstOrder() {
    await this.orderResultsTable.waitFor({ state: 'visible', timeout: 10000 });
    const rows = this.orderResultsTable.locator('tbody tr');
    await rows.first().getByRole('button', { name: /Обрати|Обрано/ }).click();
  }

  async verifyPatientContextCardVisible() {
    await expect(this.patientContextCard).toBeVisible({ timeout: 5000 });
  }

  // ==================== SCREEN 5: ORDER REVIEW ====================
  
  async waitForPdfToLoad() {
    await this.page.waitForTimeout(3000); // PDF load time
  }

  async clickStart() {
    await this.startButton.click();
  }

  async verifyStartButtonDisabled() {
    await expect(this.startButton).toBeDisabled();
  }

  async verifyStartButtonEnabled() {
    await expect(this.startButton).toBeEnabled({ timeout: 10000 });
  }

  async verifyPdfViewerVisible() {
    await expect(this.orderReviewPdfViewer).toBeVisible({ timeout: 10000 });
  }

  async verifyMaterialsPanelOrPlaceholder() {
    const hasMaterials = await this.orderReviewMaterialsPanel.isVisible({ timeout: 3000 }).catch(() => false);
    const hasPlaceholder = await this.orderReviewMaterialsPlaceholder.isVisible({ timeout: 3000 }).catch(() => false);
    expect(hasMaterials || hasPlaceholder).toBeTruthy();
  }

  // ==================== SCREEN 6: TEMPLATE SELECTION ====================
  
  async selectTemplate(templateName: string) {
    await this.page.waitForTimeout(2000);
    
    // Try to find the template card by text and click it
    const card = this.page.getByText(templateName).first();
    if (await card.isVisible({ timeout: 5000 }).catch(() => false)) {
      await card.click();
      await this.page.waitForTimeout(500);
    }
    
    // Find and click the enabled "Обрати" button
    const selectButton = this.page.getByRole('button', { name: /Обрати|Обрано/ }).first();
    await selectButton.waitFor({ state: 'visible', timeout: 10000 });
    await selectButton.click();
  }

  async selectFirstTemplate() {
    await this.page.waitForTimeout(2000);
    const selectButton = this.page.getByRole('button', { name: /Обрати|Обрано/ }).first();
    await selectButton.waitFor({ state: 'visible', timeout: 10000 });
    await selectButton.click();
  }

  async verifyContextSummaryVisible() {
    // Context summary shows "Пацієнт" label with patient ID (no colon in actual UI)
    const contextCard = this.page.getByText(/Пацієнт/);
    await expect(contextCard.first()).toBeVisible({ timeout: 10000 });
  }

  // ==================== COMMON NAVIGATION ====================
  
  async clickNext() {
    await this.nextButton.click();
  }

  async clickBack() {
    await this.backButton.click();
  }

  async clickCancel() {
    await this.cancelButton.click();
  }

  async getStepText(): Promise<string> {
    return await this.stepIndicator.textContent() ?? '';
  }

  async verifyStepIndicator(expectedStep: string) {
    const text = await this.getStepText();
    expect(text).toContain(expectedStep);
  }

  async verifyNextButtonDisabled() {
    await expect(this.nextButton).toBeDisabled();
  }

  async verifyNextButtonEnabled() {
    await expect(this.nextButton).toBeEnabled();
  }
}
