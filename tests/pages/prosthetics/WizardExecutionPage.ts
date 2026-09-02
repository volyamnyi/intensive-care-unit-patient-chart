import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object for the Wizard Execution (Screen 8)
 * Spec ref: Section 2.4.2 — Wizard Execution
 */
export class WizardExecutionPage {
  readonly page: Page;
  
  // Header (Sticky)
  readonly processName: Locator;
  readonly patientName: Locator;
  readonly orderNumber: Locator;
  readonly statusBadge: Locator;
  readonly liveTimer: Locator;
  
  // Progress
  readonly progressBar: Locator;
  readonly progressText: Locator;
  
  // Main Content
  readonly stepHeading: Locator;
  readonly stepDescription: Locator;
  readonly instructionText: Locator;
  
  // Control Buttons
  readonly completeStepButton: Locator;
  readonly backStepButton: Locator;
  readonly pauseButton: Locator;
  readonly homeButton: Locator;
  
  // Pause Dialog
  readonly pauseDialog: Locator;
  readonly pauseReasonOperative: Locator;
  readonly pauseReasonVlc: Locator;
  readonly pauseReasonAbroad: Locator;
  readonly pauseReasonReamp: Locator;
  readonly confirmPauseButton: Locator;
  readonly cancelPauseButton: Locator;
  
  // Web Elements (dynamic)
  readonly textInputs: Locator;
  readonly measurementInputs: Locator;
  readonly numericInputs: Locator;
  readonly textareas: Locator;
  readonly checkboxes: Locator;
  readonly dropdowns: Locator;
  readonly radioGroups: Locator;
  readonly datePickers: Locator;
  readonly fileUploads: Locator;
  readonly imageUploads: Locator;
  readonly signaturePad: Locator;
  
  // Resources Panel
  readonly resourcesPanel: Locator;
  readonly materialInput: Locator;
  readonly quantityInput: Locator;
  readonly unitSelect: Locator;
  readonly timeSpentInput: Locator;
  readonly addResourceButton: Locator;
  readonly resourcesTable: Locator;

  constructor(page: Page) {
    this.page = page;
    
    // Header
    this.processName = page.locator('[class*="process-name"], h1').first();
    this.patientName = page.locator('[class*="patient-name"]').first();
    this.orderNumber = page.locator('[class*="order-number"]').first();
    this.statusBadge = page.locator('[class*="badge"], [class*="status"]').first();
    this.liveTimer = page.locator('[class*="font-mono"], [class*="timer"]').first();
    
    // Progress
    this.progressBar = page.getByRole('progressbar');
    this.progressText = page.getByText(/\d+\/\d+/).first();
    
    // Main Content
    this.stepHeading = page.getByRole('heading', { level: 1 }).or(page.getByRole('heading', { level: 2 })).first();
    this.stepDescription = page.locator('[class*="description"], [class*="border-l-4"]').first();
    this.instructionText = page.locator('[class*="instruction"]').first();
    
    // Control Buttons
    this.completeStepButton = page.getByRole('button', { name: /Готово|Завершити крок/ });
    this.backStepButton = page.getByRole('button', { name: /Назад/ });
    this.pauseButton = page.getByRole('button', { name: /Пауза/ });
    this.homeButton = page.getByRole('button', { name: /До головного меню/ });
    
    // Pause Dialog
    this.pauseDialog = page.getByRole('dialog');
    this.pauseReasonOperative = page.getByLabel(/Оперативне втручання/).first();
    this.pauseReasonVlc = page.getByLabel(/Проходження ВЛК/).first();
    this.pauseReasonAbroad = page.getByLabel(/Поїхав за кордон/).first();
    this.pauseReasonReamp = page.getByLabel(/Реампутація/).first();
    this.confirmPauseButton = page.getByRole('button', { name: /Призупинити|Підтвердити/ });
    this.cancelPauseButton = page.getByRole('button', { name: /Скасувати/ });
    
    // Web Elements
    // Measurement form fields are type=text + inputmode=decimal (their onChange
    // strips non-digit chars), so they must be handled separately with numeric
    // values and excluded from the generic text-input fill.
    this.textInputs = page.locator('input[type="text"]:not([inputmode]), input:not([type])');
    this.measurementInputs = page.locator('input.measurement-field, input[inputmode="decimal"]');
    this.numericInputs = page.locator('input[type="number"]');
    this.textareas = page.locator('textarea');
    this.checkboxes = page.locator('[data-slot="checkbox"], input[type="checkbox"]');
    this.dropdowns = page.locator('button[role="combobox"], select');
    this.radioGroups = page.locator('input[type="radio"]');
    this.datePickers = page.locator('input[type="date"]');
    this.fileUploads = page.locator('input[type="file"]');
    this.imageUploads = page.locator('input[type="file"][accept*="image"]');
    this.signaturePad = page.locator('[class*="signature"], button').filter({ hasText: /Область для електронного підпису|Підпис/ });
    
    // Resources Panel
    this.resourcesPanel = page.locator('[class*="resources"], [class*="materials-panel"]').first();
    this.materialInput = page.getByPlaceholder(/Матеріал|матеріал/);
    this.quantityInput = page.getByPlaceholder(/Кількість/);
    this.unitSelect = page.getByRole('combobox').filter({ hasText: /шт|кг|л|м²/ });
    this.timeSpentInput = page.getByPlaceholder(/Час|хв|Хвилини/);
    this.addResourceButton = page.getByRole('button', { name: /Додати/ });
    this.resourcesTable = page.locator('[class*="resources-table"], [class*="materials-table"]').first();
  }

  // ==================== HEADER VERIFICATION ====================
  
  async verifyHeaderElements() {
    await expect(this.processName).toBeVisible();
    await expect(this.patientName).toBeVisible();
    await expect(this.orderNumber).toBeVisible();
    await expect(this.statusBadge).toBeVisible();
    await expect(this.liveTimer).toBeVisible();
  }

  async verifyTimerIsRunning() {
    const time1 = await this.liveTimer.textContent();
    // Deterministic: poll until the live timer ticks forward — no fixed sleep.
    await expect
      .poll(async () => this.liveTimer.textContent(), {
        timeout: 5000,
        message: 'live timer never ticked',
      })
      .not.toBe(time1);
  }

  // ==================== PROGRESS ====================
  
  async getProgress(): Promise<number> {
    const value = await this.progressBar.getAttribute('aria-valuenow');
    return value ? parseInt(value) : 0;
  }

  async getProgressText(): Promise<string> {
    return await this.progressText.textContent() ?? '';
  }

  // ==================== STEP COMPLETION ====================
  
  async completeStep() {
    // Deterministic: wait for the step-completion POST (registered before the
    // click so the round-trip cannot be missed). Soft-catch keeps the caller's
    // own state checks authoritative when the button is not yet actionable.
    const completed = this.page.waitForResponse(
      (r) => r.request().method() === 'POST' && r.url().includes('/steps/') && r.url().includes('/complete'),
      { timeout: 10000 },
    );
    await this.completeStepButton.click();
    await completed.catch(() => {});
  }

  async isCompleteButtonEnabled(): Promise<boolean> {
    return await this.completeStepButton.isEnabled();
  }

  async verifyCompleteButtonDisabled() {
    await expect(this.completeStepButton).toBeDisabled();
  }

  async verifyCompleteButtonEnabled() {
    await expect(this.completeStepButton).toBeEnabled();
  }

  // ==================== STEP INTERACTION ====================
  
  async fillAllTextInputs(prefix: string = 'Test') {
    const count = await this.textInputs.count();
    for (let i = 0; i < count; i++) {
      const input = this.textInputs.nth(i);
      if (await input.isVisible() && await input.isEnabled()) {
        await input.fill(`${prefix}-${i}`);
      }
    }
  }

  async fillAllNumericInputs(value: string = '180') {
    const count = await this.numericInputs.count();
    for (let i = 0; i < count; i++) {
      const input = this.numericInputs.nth(i);
      if (await input.isVisible() && await input.isEnabled()) {
        await input.fill(value);
      }
    }
  }

  async fillAllTextareas(text: string = 'Test comment') {
    const count = await this.textareas.count();
    for (let i = 0; i < count; i++) {
      const textarea = this.textareas.nth(i);
      if (await textarea.isVisible() && await textarea.isEnabled()) {
        await textarea.fill(`${text} ${i}`);
      }
    }
  }

  async checkAllCheckboxes() {
    const count = await this.checkboxes.count();
    for (let i = 0; i < count; i++) {
      const checkbox = this.checkboxes.nth(i);
      if (await checkbox.isVisible() && await checkbox.isEnabled()) {
        // JS click: the sticky bottom action bar overlays lower checkboxes, so pointer clicks
        // would hit the bar instead of the checkbox.
        await checkbox.evaluate((el: HTMLElement) => el.click()).catch(() => {});
      }
    }
  }

  async selectAllDropdowns() {
    const count = await this.dropdowns.count();
    for (let i = 0; i < count; i++) {
      const dropdown = this.dropdowns.nth(i);
      if (await dropdown.isVisible() && await dropdown.isEnabled()) {
        await dropdown.click();
        const options = this.page.getByRole('option');
        // Wait for the popup to render its options instead of a fixed sleep.
        await options.first().waitFor({ state: 'visible', timeout: 3000 }).catch(() => {});
        if (await options.count() > 0) {
          await options.first().click();
        }
      }
    }
  }

  async selectAllRadioButtons() {
    const count = await this.radioGroups.count();
    for (let i = 0; i < count; i++) {
      const radio = this.radioGroups.nth(i);
      if (await radio.isVisible() && await radio.isEnabled()) {
        await radio.check();
      }
    }
  }

  async setAllDates(date: string = '2026-08-05') {
    const count = await this.datePickers.count();
    for (let i = 0; i < count; i++) {
      const picker = this.datePickers.nth(i);
      if (await picker.isVisible() && await picker.isEnabled()) {
        await picker.fill(date);
      }
    }
  }

  async interactWithSignature() {
    if (await this.signaturePad.isVisible({ timeout: 3000 }).catch(() => false)) {
      await this.signaturePad.first().click();
      // The signature toggle flips its label to «Підпис отримано» once the
      // capture is stored — wait for that state instead of a fixed sleep.
      await expect(
        this.page.getByRole('button', { name: 'Підпис отримано' }),
      ).toBeVisible({ timeout: 3000 }).catch(() => {});
      // Confirm signature if dialog appears
      const confirmBtn = this.page.getByRole('button', { name: /Підтвердити|Готово/ }).first();
      if (await confirmBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
        await confirmBtn.click();
      }
    }
  }

  async executeCurrentStep(): Promise<boolean> {
    let interacted = false;
    
    // Fill measurement form fields (inputmode=decimal, sanitized to digits) —
    // the «Зняття мірок» step needs ≥3 non-blank values to advance.
    const measCount = await this.measurementInputs.count();
    for (let i = 0; i < measCount; i++) {
      const input = this.measurementInputs.nth(i);
      if (await input.isVisible() && await input.isEnabled()) {
        await input.fill('180');
        interacted = true;
      }
    }
    
    // Fill text inputs
    const textCount = await this.textInputs.count();
    for (let i = 0; i < textCount; i++) {
      const input = this.textInputs.nth(i);
      if (await input.isVisible() && await input.isEnabled()) {
        await input.fill(`Value-${i}`);
        interacted = true;
      }
    }
    
    // Fill numeric inputs
    const numCount = await this.numericInputs.count();
    for (let i = 0; i < numCount; i++) {
      const input = this.numericInputs.nth(i);
      if (await input.isVisible() && await input.isEnabled()) {
        await input.fill('180');
        interacted = true;
      }
    }
    
    // Fill textareas
    const taCount = await this.textareas.count();
    for (let i = 0; i < taCount; i++) {
      const ta = this.textareas.nth(i);
      if (await ta.isVisible() && await ta.isEnabled()) {
        await ta.fill(`Comment ${i}`);
        interacted = true;
      }
    }
    
    // Check checkboxes (Base UI renders button[data-slot="checkbox"])
    const cbCount = await this.checkboxes.count();
    for (let i = 0; i < cbCount; i++) {
      const cb = this.checkboxes.nth(i);
      if (await cb.isVisible() && await cb.isEnabled()) {
        await cb.evaluate((el: HTMLElement) => el.click()).catch(() => {});
        interacted = true;
      }
    }
    
    // Select dropdowns
    const ddCount = await this.dropdowns.count();
    for (let i = 0; i < ddCount; i++) {
      const dd = this.dropdowns.nth(i);
      if (await dd.isVisible() && await dd.isEnabled()) {
        await dd.click();
        const opts = this.page.getByRole('option');
        // Wait for the popup to render its options instead of a fixed sleep.
        await opts.first().waitFor({ state: 'visible', timeout: 3000 }).catch(() => {});
        if (await opts.count() > 0) {
          await opts.first().click();
          interacted = true;
        }
      }
    }
    
    // Select radios
    const rdCount = await this.radioGroups.count();
    for (let i = 0; i < rdCount; i++) {
      const rd = this.radioGroups.nth(i);
      if (await rd.isVisible() && await rd.isEnabled()) {
        await rd.check();
        interacted = true;
      }
    }
    
    // Set dates
    const dtCount = await this.datePickers.count();
    for (let i = 0; i < dtCount; i++) {
      const dt = this.datePickers.nth(i);
      if (await dt.isVisible() && await dt.isEnabled()) {
        await dt.fill('2026-08-05');
        interacted = true;
      }
    }
    
    // Signature
    await this.interactWithSignature();
    
    // Try to complete — a bounded timeout so a gate/locked state returns quickly
    if (await this.completeStepButton.isEnabled({ timeout: 3000 }).catch(() => false)) {
      await this.completeStep();
      return true;
    }
    
    return interacted;
  }

  // ==================== PAUSE WORKFLOW ====================
  
  async pauseProcess(reason: 'OPERATIVE_INTERVENTION' | 'VLC_PASSING' | 'WENT_ABROAD' | 'REAMPUTATION') {
    await this.pauseButton.click();
    await this.pauseDialog.waitFor({ state: 'visible', timeout: 5000 });

    switch (reason) {
      case 'OPERATIVE_INTERVENTION':
        await this.pauseReasonOperative.click();
        break;
      case 'VLC_PASSING':
        await this.pauseReasonVlc.click();
        break;
      case 'WENT_ABROAD':
        await this.pauseReasonAbroad.click();
        break;
      case 'REAMPUTATION':
        await this.pauseReasonReamp.click();
        break;
    }
    
    await this.confirmPauseButton.click();
    // The pause dialog closes once the instance is paused — wait for that.
    await expect(this.pauseDialog).toBeHidden({ timeout: 10000 });
  }

  async verifyPauseDialogVisible() {
    await expect(this.pauseDialog).toBeVisible({ timeout: 5000 });
  }

  // ==================== RESOURCES PANEL ====================
  
  async addResource(material: string, quantity: string, minutes: string = '') {
    await this.materialInput.fill(material);
    await this.quantityInput.fill(quantity);
    if (minutes) {
      await this.timeSpentInput.fill(minutes);
    }
    await this.addResourceButton.click();
    // The resource row appears in the table once saved — wait for it (soft:
    // some steps render resources outside the panel locator).
    await expect(this.resourcesTable.getByText(material)).toBeVisible({ timeout: 5000 }).catch(() => {});
  }

  async verifyResourcesPanelVisible() {
    await expect(this.resourcesPanel).toBeVisible({ timeout: 5000 });
  }

  // ==================== NAVIGATION ====================
  
  async goBack() {
    const before = await this.progressText.textContent().catch(() => '');
    await this.backStepButton.click();
    // The progress indicator ("N/M") changes when the previous step loads —
    // poll for the change instead of a fixed sleep.
    await expect
      .poll(async () => this.progressText.textContent(), {
        timeout: 10000,
        message: 'step indicator never changed after back',
      })
      .not.toBe(before);
  }

  async goHome() {
    await this.homeButton.click();
  }

  // ==================== STEP STATE CHECKS ====================
  
  async getStepType(): Promise<string> {
    // Determine what type of step this is based on visible elements
    if (await this.numericInputs.count() > 0) return 'measurement';
    if (await this.checkboxes.count() > 0) return 'checklist';
    if (await this.fileUploads.count() > 0 || await this.imageUploads.count() > 0) return 'media';
    if (await this.dropdowns.count() > 0 || await this.radioGroups.count() > 0) return 'selection';
    if (await this.textareas.count() > 0) return 'composite';
    return 'information';
  }
}
