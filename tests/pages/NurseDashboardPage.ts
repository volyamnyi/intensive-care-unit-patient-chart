import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class NurseDashboardPage extends BasePage {
  readonly patientSelect: Locator;
  readonly vitalsSectionTitle: (hour: number) => Locator;
  readonly fluidSectionTitle: (hour: number) => Locator;

  readonly vitalSystolicField: Locator;
  readonly vitalDiastolicField: Locator;
  readonly vitalHeartRateField: Locator;
  readonly vitalSpo2Field: Locator;
  readonly vitalTempField: Locator;
  readonly vitalCvpField: Locator;
  readonly vitalRespField: Locator;
  readonly saveVitalsButton: Locator;

  readonly fluidUrineField: Locator;
  readonly fluidTubeField: Locator;
  readonly fluidDrainageField: Locator;
  readonly fluidStoolSelect: Locator;
  readonly saveFluidButton: Locator;

  readonly balancePanel: Locator;
  readonly balanceIntake: Locator;
  readonly balanceOutput: Locator;
  readonly balanceDaily: Locator;
  readonly balanceCumulative: Locator;

  readonly bradenSection: Locator;
  readonly emptyState: Locator;
  readonly loadingSpinner: Locator;
  readonly userMenu: Locator;
  readonly logoutButton: Locator;

  constructor(page: Page) {
    super(page);
    this.patientSelect = page.locator('[role="combobox"]').first();
    this.vitalsSectionTitle = (hour: number) => page.getByText(`Показники — ${hour}:00`);
    this.fluidSectionTitle = (hour: number) => page.getByText(`Втрати рідини — ${hour}:00`);

    this.vitalSystolicField = page.getByLabel('АТ сист (мм.рт.ст)');
    this.vitalDiastolicField = page.getByLabel('АТ діас (мм.рт.ст)');
    this.vitalHeartRateField = page.getByLabel('ЧСС (в 1 хв)');
    this.vitalSpo2Field = page.getByLabel('SpO2 (%)');
    this.vitalTempField = page.getByLabel('Темп. тіла (°С)');
    this.vitalCvpField = page.getByLabel('ЦВТ (мм.вод.ст)');
    this.vitalRespField = page.getByLabel('ЧД (в 1 хв)');
    this.saveVitalsButton = page.getByRole('button', { name: 'Зберегти показники' });

    this.fluidUrineField = page.getByLabel('Сеча (мл)');
    this.fluidTubeField = page.getByLabel('Зонд (мл)');
    this.fluidDrainageField = page.getByLabel('Дренаж (мл)');
    this.fluidStoolSelect = page.locator('[role="combobox"]').nth(1);
    this.saveFluidButton = page.getByRole('button', { name: 'Зберегти втрати' });

    this.balancePanel = page.getByText('Баланс рідини');
    this.balanceIntake = page.getByText(/Надійшло:/);
    this.balanceOutput = page.getByText(/Виділено:/);
    this.balanceDaily = page.getByText(/Добовий баланс:/);
    this.balanceCumulative = page.getByText(/Кумулятивний баланс:/);

    this.bradenSection = page.getByText('Шкала Брейдена');
    this.emptyState = page.getByText('Немає активних пацієнтів');
    this.loadingSpinner = page.getByRole('progressbar');
    this.userMenu = page.getByRole('button', { name: /меню користувача/i });
    this.logoutButton = page.getByRole('menuitem', { name: 'Вийти' });
  }

  async navigate(): Promise<void> {
    await this.page.goto('/nurse');
    await this.page.waitForLoadState('networkidle');
    await this.delay();
  }

  patientOption(text: string): Locator {
    return this.page.getByRole('option', { name: new RegExp(text) });
  }

  hourBox(hour: number): Locator {
    return this.page.getByText(`${hour}:00`).first();
  }

  getHeading(): Locator {
    return this.page.getByRole('heading', { name: 'Карта інтенсивної терапії — медсестра' });
  }

  async selectPatient(text: string): Promise<void> {
    await this.patientSelect.click();
    await this.delay();
    const option = this.patientOption(text);
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();
    await this.delay();
  }

  async selectHour(hour: number): Promise<void> {
    await this.hourBox(hour).click();
    await this.delay();
  }

  async fillAllVitals(systolic: string, diastolic: string, hr: string, spo2: string, temp: string, cvp: string, resp: string): Promise<void> {
    await this.vitalSystolicField.click();
    await this.vitalSystolicField.fill(systolic);
    await this.vitalDiastolicField.fill(diastolic);
    await this.vitalHeartRateField.fill(hr);
    await this.vitalSpo2Field.fill(spo2);
    await this.vitalTempField.fill(temp);
    await this.vitalCvpField.fill(cvp);
    await this.vitalRespField.fill(resp);
    await this.delay();
  }

  async clickSaveVitals(): Promise<void> {
    await this.saveVitalsButton.click();
    await this.delay();
  }

  async fillFluidOutput(urine: string, tube: string, drainage: string): Promise<void> {
    await this.fluidUrineField.click();
    await this.fluidUrineField.fill(urine);
    await this.fluidTubeField.fill(tube);
    await this.fluidDrainageField.fill(drainage);
    await this.delay();
  }

  async selectStool(value: 'Так' | 'Ні'): Promise<void> {
    await this.fluidStoolSelect.click();
    await this.delay();
    await this.page.getByRole('option', { name: value }).click();
    await this.delay();
  }

  async clickSaveFluid(): Promise<void> {
    await this.saveFluidButton.click();
    await this.delay();
  }

  async clickLogout(): Promise<void> {
    await this.userMenu.click();
    await this.delay();
    await this.logoutButton.click();
    await this.delay();
  }

  async expectVitalsValue(label: Locator, value: string): Promise<void> {
    await expect(label).toHaveValue(value, { timeout: 10000 });
  }
}
