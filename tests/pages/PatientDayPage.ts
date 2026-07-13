import { expect, type Page, type Locator } from '@playwright/test';
import { BasePage } from './BasePage';

export class PatientDayPage extends BasePage {
  readonly patientName: Locator;
  readonly dayInfo: Locator;
  readonly backButton: Locator;
  readonly signOffButton: Locator;
  readonly signOffButtonDisabled: Locator;
  readonly diagnosisText: Locator;
  readonly admissionText: Locator;
  readonly durationText: Locator;

  readonly vitalsTab: Locator;
  readonly prescriptionsTab: Locator;
  readonly scalesTab: Locator;
  readonly notesTab: Locator;

  readonly vitalsTable: Locator;

  readonly prescriptionTypeSelect: Locator;
  readonly prescriptionMedField: Locator;
  readonly prescriptionDoseField: Locator;
  readonly prescriptionRouteField: Locator;
  readonly prescriptionStartHourField: Locator;
  readonly prescriptionAddButton: Locator;
  readonly newPrescriptionTitle: Locator;

  readonly noteTextField: Locator;
  readonly addNoteButton: Locator;
  readonly notesList: Locator;
  readonly emptyNotes: Locator;

  readonly signDialogTitle: Locator;
  readonly signDialogPrompt: Locator;
  readonly signDialogEscalationNote: Locator;
  readonly signDialogCancel: Locator;
  readonly signDialogConfirm: Locator;

  readonly apacheScaleCard: Locator;
  readonly sofaScaleCard: Locator;
  readonly rassScaleCard: Locator;
  readonly camScaleCard: Locator;
  readonly bradenScaleCard: Locator;
  readonly notFilledText: Locator;

  constructor(page: Page) {
    super(page);
    this.patientName = page.locator('h5').first();
    this.dayInfo = page.getByText(/Доба №/);
    this.backButton = page.getByRole('button', { name: 'Назад' });
    this.signOffButton = page.getByRole('button', { name: 'Підписати добу' });
    this.signOffButtonDisabled = page.getByRole('button', { name: 'Підписати добу', disabled: true });
    this.diagnosisText = page.getByText(/Діагноз:/);
    this.admissionText = page.getByText(/Надійшов:/);
    this.durationText = page.getByText(/Тривалість:/);

    this.vitalsTab = page.getByRole('tab', { name: 'Вітальні показники' });
    this.prescriptionsTab = page.getByRole('tab', { name: 'Призначення' });
    this.scalesTab = page.getByRole('tab', { name: 'Шкали' });
    this.notesTab = page.getByRole('tab', { name: 'Нотатки' });

    this.vitalsTable = page.getByRole('table').first();

    this.prescriptionTypeSelect = page.getByLabel('Тип');
    this.prescriptionMedField = page.getByLabel('Препарат / дослідження');
    this.prescriptionDoseField = page.getByLabel('Доза');
    this.prescriptionRouteField = page.getByLabel('Шлях');
    this.prescriptionStartHourField = page.getByLabel('Год. від');
    this.prescriptionAddButton = page.getByRole('button', { name: '+' });
    this.newPrescriptionTitle = page.getByText('Нове призначення');

    this.noteTextField = page.getByLabel('Нова нотатка');
    this.addNoteButton = page.getByRole('button', { name: 'Додати нотатку' });
    this.emptyNotes = page.getByText('Немає нотаток');
    this.notesList = page.getByText('Немає нотаток').locator('..');

    this.signDialogTitle = page.getByRole('heading', { name: /Підписання доби/ });
    this.signDialogPrompt = page.getByText('Після підписання доба стане read-only');
    this.signDialogEscalationNote = page.getByText('Буде згенеровано PDF');
    this.signDialogCancel = page.getByRole('button', { name: 'Скасувати' }).first();
    this.signDialogConfirm = page.getByRole('button', { name: 'Підписати' });

    this.apacheScaleCard = page.getByText('APACHE II').first();
    this.sofaScaleCard = page.getByText('SOFA').first();
    this.rassScaleCard = page.getByText('RASS').first();
    this.camScaleCard = page.getByText('CAM-ICU').first();
    this.bradenScaleCard = page.getByText('Шкала Брейдена').first();
    this.notFilledText = page.getByText('Не заповнено').first();
  }

  getStatusChip(): Locator {
    return this.page.getByText(/Статус:/);
  }

  getChip(text: string): Locator {
    return this.page.getByText(text);
  }

  getPrescriptionInTable(medication: string): Locator {
    return this.page.getByText(medication);
  }

  getStatusChipInTable(status: string): Locator {
    return this.page.getByText(status);
  }

  getNoteText(index: number): Locator {
    return this.page.getByRole('heading', { name: /Підписання доби/ }).locator('..').locator('..').getByRole('button', { name: /Підписати/ });
  }

  async switchTab(tabLocator: Locator): Promise<void> {
    await tabLocator.click();
  }

  async clickBack(): Promise<void> {
    await this.backButton.click();
  }

  async clickSignOff(): Promise<void> {
    await this.signOffButton.click();
  }

  async fillPrescriptionMed(text: string): Promise<void> {
    await this.prescriptionMedField.click();
    await this.prescriptionMedField.fill(text);
  }

  async fillPrescriptionDose(text: string): Promise<void> {
    await this.prescriptionDoseField.click();
    await this.prescriptionDoseField.fill(text);
  }

  async fillPrescriptionRoute(text: string): Promise<void> {
    await this.prescriptionRouteField.click();
    await this.prescriptionRouteField.fill(text);
  }

  async fillPrescriptionStartHour(value: string): Promise<void> {
    await this.prescriptionStartHourField.click();
    await this.prescriptionStartHourField.fill(value);
  }

  async selectPrescriptionType(value: string): Promise<void> {
    await this.prescriptionTypeSelect.selectOption(value);
  }

  async clickAddPrescription(): Promise<void> {
    await this.prescriptionAddButton.click();
  }

  async fillNote(text: string): Promise<void> {
    await this.noteTextField.click();
    await this.noteTextField.fill(text);
  }

  async clickAddNote(): Promise<void> {
    await this.addNoteButton.click();
  }

  async cancelSignOff(): Promise<void> {
    await this.signDialogCancel.click();
  }

  async confirmSignOff(): Promise<void> {
    await this.signDialogConfirm.click();
  }

  async expectSignOffDialogVisible(): Promise<void> {
    await expect(this.signDialogTitle).toBeVisible({ timeout: 10000 });
    await expect(this.signDialogPrompt).toBeVisible();
    await expect(this.signDialogEscalationNote).toBeVisible();
  }
}
