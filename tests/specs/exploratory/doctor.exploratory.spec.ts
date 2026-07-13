import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/LoginPage';
import { DoctorDashboardPage } from '../../pages/DoctorDashboardPage';
import { CreateCardPage } from '../../pages/CreateCardPage';
import { PatientDayPage } from '../../pages/PatientDayPage';

test.describe('Doctor: Exploratory Testing', () => {

  test('DOC-LOGIN-001: Login as doctor1 with valid credentials', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    await login.navigate();
    await login.expectLoginFormVisible();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await expect(doctor.title).toBeVisible({ timeout: 10000 });
    await expect(doctor.searchField).toBeVisible();
  });

  test('DOC-LOGIN-002: Login as doctor2 shows dashboard', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    await login.navigate();
    await login.loginAs('doctor2', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await expect(doctor.title).toBeVisible({ timeout: 10000 });
    await expect(doctor.searchField).toBeVisible();
  });

  test('DOC-DASH-001: Dashboard shows all ICU cards', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.expectPatientCards(3);
    await expect(doctor.patientCards).toHaveCount(3);
  });

  test('DOC-DASH-002: Search field filters by patient name', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.searchPatient('Петренко');
    await doctor.expectPatientCards(1);
    await expect(doctor.patientCards).toHaveCount(1);
  });

  test('DOC-DASH-003: Searching non-existent name shows no cards', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.searchPatient('НемаТакийПацієнт');
    await expect(doctor.patientCards).toHaveCount(0);
    await expect(doctor.emptyState).toBeVisible();
  });

  test('DOC-DASH-004: Clear search restores all cards', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.searchPatient('Петренко');
    await doctor.expectPatientCards(1);
    await doctor.searchPatient('');
    await doctor.expectPatientCards(3);
  });

  test('DOC-OPEN-001: Clicking patient card opens day page', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    const day = new PatientDayPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.clickOpenByName('Петренко');
    await expect(day.patientName).toBeVisible({ timeout: 10000 });
    await expect(day.diagnosisText).toBeVisible();
    await expect(day.vitalsTab).toBeVisible();
  });

  test('DOC-OPEN-002: Back button returns to dashboard', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    const day = new PatientDayPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.clickOpenByName('Коваленко');
    await expect(day.patientName).toBeVisible({ timeout: 10000 });
    await day.clickBack();
    await expect(doctor.title).toBeVisible({ timeout: 10000 });
    await doctor.expectPatientCards(3);
  });

  test('DOC-DAY-001: Day page shows all tabs', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    const day = new PatientDayPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.clickOpenByName('Сидоренко');
    await expect(day.patientName).toBeVisible({ timeout: 10000 });
    await expect(day.vitalsTab).toBeVisible();
    await expect(day.prescriptionsTab).toBeVisible();
    await expect(day.scalesTab).toBeVisible();
    await expect(day.notesTab).toBeVisible();
  });

  test('DOC-DAY-002: Vitals tab shows vitals table', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    const day = new PatientDayPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.clickOpenByName('Петренко');
    await expect(day.patientName).toBeVisible({ timeout: 10000 });
    await day.switchTab(day.vitalsTab);
    await expect(day.vitalsTable).toBeVisible({ timeout: 10000 });
  });

  test('DOC-DAY-003: Prescriptions tab shows form and existing prescriptions', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    const day = new PatientDayPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.clickOpenByName('Петренко');
    await expect(day.patientName).toBeVisible({ timeout: 10000 });
    await day.switchTab(day.prescriptionsTab);
    await expect(day.newPrescriptionTitle).toBeVisible({ timeout: 10000 });
    await expect(day.prescriptionTypeSelect).toBeVisible();
    await expect(day.prescriptionMedField).toBeVisible();
  });

  test('DOC-DAY-004: Scales tab shows all scale cards', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    const day = new PatientDayPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.clickOpenByName('Петренко');
    await expect(day.patientName).toBeVisible({ timeout: 10000 });
    await day.switchTab(day.scalesTab);
    await expect(day.apacheScaleCard).toBeVisible({ timeout: 10000 });
    await expect(day.sofaScaleCard).toBeVisible();
    await expect(day.rassScaleCard).toBeVisible();
    await expect(day.camScaleCard).toBeVisible();
    await expect(day.bradenScaleCard).toBeVisible();
  });

  test('DOC-DAY-005: Notes tab shows notes list', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    const day = new PatientDayPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.clickOpenByName('Петренко');
    await expect(day.patientName).toBeVisible({ timeout: 10000 });
    await day.switchTab(day.notesTab);
    await expect(day.noteTextField).toBeVisible({ timeout: 10000 });
    await expect(day.addNoteButton).toBeVisible();
  });

  test('DOC-LOGOUT-001: Logout from doctor dashboard', async ({ page }) => {
    const login = new LoginPage(page);
    const doctor = new DoctorDashboardPage(page);
    await login.navigate();
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.clickLogout();
    await expect(page).toHaveURL('/login');
    await login.expectLoginFormVisible();
  });
});
