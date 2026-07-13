import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class PatientDayPage extends BasePage {
  readonly backButton: Locator;
  readonly signOffButton: Locator;
  readonly vitalsTab: Locator;
  readonly prescriptionsTab: Locator;
  readonly scalesTab: Locator;
  readonly notesTab: Locator;
  readonly balanceTab: Locator;

  readonly vitalSystolicField: Locator;
  readonly vitalDiastolicField: Locator;
  readonly vitalHeartRateField: Locator;
  readonly vitalSpo2Field: Locator;
  readonly vitalTempField: Locator;
  readonly vitalCvpField: Locator;
  readonly vitalRespField: Locator;
  readonly saveVitalsButton: Locator;

  readonly prescriptionMedField: Locator;
  readonly prescriptionDoseField: Locator;
  readonly prescriptionRouteField: Locator;
  readonly prescriptionAddButton: Locator;

  readonly noteTextField: Locator;
  readonly addNoteButton: Locator;

  readonly signDialogTitle: Locator;
  readonly signDialogConfirm: Locator;
  readonly signDialogCancel: Locator;

  constructor(page: Page) {
    super(page);
    this.backButton = page.getByRole('button', { name: 'Назад' });
    this.signOffButton = page.getByRole('button', { name: 'Підписати добу' });
    this.vitalsTab = page.getByRole('tab', { name: 'Вітальні показники' });
    this.prescriptionsTab = page.getByRole('tab', { name: 'Призначення' });
    this.scalesTab = page.getByRole('tab', { name: 'Шкали' });
    this.notesTab = page.getByRole('tab', { name: 'Нотатки' });
    this.balanceTab = page.getByRole('tab', { name: 'Баланс рідини' });

    this.vitalSystolicField = page.getByLabel('АТ сист (мм.рт.ст)');
    this.vitalDiastolicField = page.getByLabel('АТ діас (мм.рт.ст)');
    this.vitalHeartRateField = page.getByLabel('ЧСС (в 1 хв)');
    this.vitalSpo2Field = page.getByLabel('SpO2 (%)');
    this.vitalTempField = page.getByLabel('Темп. тіла (°С)');
    this.vitalCvpField = page.getByLabel('ЦВТ (мм.вод.ст)');
    this.vitalRespField = page.getByLabel('ЧД (в 1 хв)');
    this.saveVitalsButton = page.getByRole('button', { name: 'Зберегти показники' });

    this.prescriptionMedField = page.getByLabel('Препарат');
    this.prescriptionDoseField = page.getByLabel('Доза');
    this.prescriptionRouteField = page.getByLabel('Шлях');
    this.prescriptionAddButton = page.getByRole('button', { name: 'Створити' });

    this.noteTextField = page.getByLabel('Нова нотатка');
    this.addNoteButton = page.getByRole('button', { name: 'Додати нотатку' });

    this.signDialogTitle = page.getByRole('heading', { name: /Підписання доби/ });
    this.signDialogConfirm = page.getByRole('button', { name: 'Підписати' });
    this.signDialogCancel = page.getByRole('button', { name: 'Скасувати' }).first();
  }

  async clickSignOff(): Promise<void> {
    await this.signOffButton.click();
  }

  async confirmSignOff(): Promise<void> {
    await this.signDialogConfirm.click();
  }

  async switchTab(tab: Locator): Promise<void> {
    await tab.click();
  }

  createOrderButton(): Locator {
    return this.page.getByRole('button', { name: '+ Нове призначення' });
  }

  async fillVitals(systolic: string, diastolic: string, hr: string, spo2: string, temp: string): Promise<void> {
    await this.vitalSystolicField.fill(systolic);
    await this.vitalDiastolicField.fill(diastolic);
    await this.vitalHeartRateField.fill(hr);
    await this.vitalSpo2Field.fill(spo2);
    await this.vitalTempField.fill(temp);
  }

  async clickSaveVitals(): Promise<void> {
    await this.saveVitalsButton.click();
  }

  async fillPrescription(name: string, dose: string, route: string): Promise<void> {
    await this.prescriptionMedField.fill(name);
    await this.prescriptionDoseField.fill(dose);
    await this.prescriptionRouteField.fill(route);
  }

  async clickAddPrescription(): Promise<void> {
    await this.prescriptionAddButton.click();
  }

  async fillNote(text: string): Promise<void> {
    await this.noteTextField.fill(text);
  }

  async clickAddNote(): Promise<void> {
    await this.addNoteButton.click();
  }
}
