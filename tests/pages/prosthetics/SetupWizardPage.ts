import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for the Setup Wizard (Screens 3-5)
 * Spec ref: Section 2.3 — New Process Creation
 *
 * NOTE: Actual implementation differs from spec in some details:
 * - Patient search: NO search button — automatic with 300ms debounce
 * - Patient table: only renders when results exist (empty state shows text)
 * - Setup is 3 steps (patient → MIS order → review): the technology route is
 *   picked inside the review screen, there is no separate template screen
 *   (decision #449/#453).
 */
export class SetupWizardPage {
  readonly page: Page;
  
  // Screen 3: Patient Selection
  readonly patientSearchInput: Locator;
  readonly patientResultsTable: Locator;
  readonly patientNoResultsMessage: Locator;
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

  // Technology route selection (inside Order Review)
  readonly routeGroup: Locator;
  readonly routeFallbackNotice: Locator;
  readonly routeEmptyState: Locator;

  // Common
  readonly nextButton: Locator;
  readonly backButton: Locator;
  readonly cancelButton: Locator;
  readonly stepIndicator: Locator;
  readonly heading: Locator;

  constructor(page: Page) {
    this.page = page;
    
    // Screen 3: Patient Selection
    this.patientSearchInput = page.getByPlaceholder(/Пошук пацієнта за ПІБ або номером координати/);
    this.patientResultsTable = page.getByRole('table');
    this.patientNoResultsMessage = page.getByText(/Пацієнтів не знайдено/);
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
    
    // Screen 6: technology route selection (inside Order Review)
    this.routeGroup = page.getByRole('radiogroup', { name: 'Технологічні маршрути' });
    this.routeFallbackNotice = page.getByText(/Точного збігу за рівнем ампутації немає/);
    this.routeEmptyState = page.getByText(/Для цього замовлення немає активного шаблону/);

    // Common
    this.nextButton = page.getByRole('button', { name: /Далі/ });
    this.backButton = page.getByRole('button', { name: /Назад/ });
    this.cancelButton = page.getByRole('button', { name: /Скасувати|До головного меню/ });
    this.stepIndicator = page.getByText(/Крок \d з 3/);
    this.heading = page.getByRole('heading', { level: 1 });
  }

  // ==================== SCREEN 3: PATIENT SELECTION ====================
  
  /**
   * Search for patient — NO search button, search triggers automatically
   * after 300ms debounce when query >= 2 characters
   */
  async searchPatient(query: string) {
    // Search fires automatically after the 300ms debounce — wait for the
    // patients GET response (registered before the fill so it cannot be missed).
    const searchResponse = this.page.waitForResponse(
      (r) => r.request().method() === 'GET' && r.url().includes('/prosthesis-manufacturing/patients'),
      { timeout: 10000 },
    );
    await this.patientSearchInput.fill(query);
    await searchResponse;
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
    // The page loads the full patient list on mount AND applies a debounced server
    // search, so the row set shifts while iterating. Target the matching row
    // directly (auto-waits for it to be present/visible and stable) instead of
    // stepping every row with textContent(), which races the re-render.
    const match = rows.filter({ hasText: nameOrId }).first();
    try {
      await expect(match).toBeVisible({ timeout: 15000 });
    } catch {
      // Fallback: select the first patient.
      await rows.first().getByRole('button', { name: /Обрати|Обрано/ }).click();
      return;
    }
    await match.getByRole('button', { name: /Обрати|Обрано/ }).click();
  }

  async selectFirstPatient() {
    await this.patientResultsTable.waitFor({ state: 'visible', timeout: 10000 });
    const rows = this.patientResultsTable.locator('tbody tr');
    await rows.first().getByRole('button', { name: /Обрати|Обрано/ }).click();
  }

  async verifyNoResultsMessage() {
    await expect(this.patientNoResultsMessage).toBeVisible({ timeout: 5000 });
  }

  async verifyPatientTableVisible() {
    // The patient list loads from MIS on mount — the results table is the
    // readiness signal (no more "enter a query" empty state).
    await expect(this.patientResultsTable).toBeVisible({ timeout: 10000 });
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

      // The order list effect re-runs on draft updates (flipping the page back to
      // loading skeletons) — wait for the refetch response instead of a fixed sleep.
      await this.page.waitForResponse(
        (r) => r.request().method() === 'GET' && r.url().includes('/prosthesis-manufacturing/orders'),
        { timeout: 15000 },
      );
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
    // «Старт» stays disabled until the order document finishes loading — waiting
    // for it to become enabled is the deterministic readiness signal (the button
    // being already enabled is fine too: the caller asserts the viewer itself).
    await expect(this.startButton).toBeEnabled({ timeout: 15000 }).catch(() => {});
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

  // ==================== SCREEN 6: TECHNOLOGY ROUTE (REVIEW) ====================

  /**
   * Picks a technology route from the review screen. The radio input is
   * visually hidden (`sr-only`), so the click lands on the label card — the
   * native label activation toggles the radio exactly once.
   */
  async selectRoute(templateName: string) {
    await expect(this.routeGroup).toBeVisible({ timeout: 15000 });
    const card = this.routeGroup.locator('label').filter({ hasText: templateName }).first();
    await expect(card).toBeVisible({ timeout: 10000 });
    await card.click();
    await expect(card.getByRole('radio')).toBeChecked();
  }

  /** Waits for the route list (or its empty state) and returns the candidate count. */
  async getRouteCount(): Promise<number> {
    await expect(this.routeGroup.or(this.routeEmptyState).first()).toBeVisible({ timeout: 15000 });
    return this.routeGroup.getByRole('radio').count();
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
