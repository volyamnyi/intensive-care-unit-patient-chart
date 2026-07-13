import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/LoginPage';
import { NurseDashboardPage } from '../../pages/NurseDashboardPage';
import { PatientDayPage } from '../../pages/PatientDayPage';
import { DoctorDashboardPage } from '../../pages/DoctorDashboardPage';

test.describe('Nurse: Exploratory Testing', () => {

  test('NUR-LOGIN-001: Login as nurse1 with valid credentials', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.expectLoginFormVisible();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await expect(nurse.getHeading()).toBeVisible({ timeout: 10000 });
    await expect(nurse.patientSelect).toBeVisible();
  });

  test('NUR-LOGIN-002: Login as nurse2 shows dashboard', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse2', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await expect(nurse.getHeading()).toBeVisible({ timeout: 10000 });
    await expect(nurse.patientSelect).toBeVisible();
  });

  test('NUR-SELECT-001: Select patient shows available hours', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await nurse.selectPatient('Петренко');
    await expect(nurse.hourBox(8)).toBeVisible({ timeout: 10000 });
  });

  test('NUR-SELECT-002: Selecting different patient changes hours', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await nurse.selectPatient('Петренко');
    await expect(nurse.hourBox(8)).toBeVisible({ timeout: 10000 });
    await nurse.selectPatient('Коваленко');
    await expect(nurse.hourBox(8)).toBeVisible({ timeout: 10000 });
  });

  test('NUR-VIT-001: Fill vitals form for selected patient and hour', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await nurse.selectPatient('Петренко');
    await nurse.selectHour(8);
    await expect(nurse.vitalsSectionTitle(8)).toBeVisible({ timeout: 10000 });
    await nurse.fillAllVitals('120', '80', '76', '98', '36.6', '8', '16');
    await expect(nurse.saveVitalsButton).toBeEnabled();
  });

  test('NUR-VIT-002: Save vitals persists and shows in doctor view', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await nurse.selectPatient('Сидоренко');
    await nurse.selectHour(8);
    await expect(nurse.vitalsSectionTitle(8)).toBeVisible({ timeout: 10000 });
    await nurse.fillAllVitals('110', '70', '72', '97', '36.8', '7', '18');
    await nurse.clickSaveVitals();

    await nurse.clickLogout();
    await login.expectLoginFormVisible();

    const doctor = new DoctorDashboardPage(page);
    const day = new PatientDayPage(page);
    await login.loginAs('doctor1', 'doctor123');
    await doctor.expectUrl(/\/doctor/);
    await doctor.clickOpenByName('Сидоренко');
    await expect(day.patientName).toBeVisible({ timeout: 10000 });
    await day.switchTab(day.vitalsTab);
    await expect(day.vitalsTable).toBeVisible({ timeout: 10000 });
  });

  test('NUR-FLU-001: Fill fluid output form and save', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await nurse.selectPatient('Петренко');
    await nurse.selectHour(8);
    await expect(nurse.fluidSectionTitle(8)).toBeVisible({ timeout: 10000 });
    await nurse.fillFluidOutput('300', '200', '50');
    await nurse.selectStool('Ні');
    await expect(nurse.saveFluidButton).toBeEnabled();
  });

  test('NUR-FLU-002: Fluid balance panel updates after saving', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await nurse.selectPatient('Петренко');
    await nurse.selectHour(8);
    await nurse.fillFluidOutput('100', '50', '10');
    await nurse.selectStool('Ні');
    await nurse.clickSaveFluid();
    await expect(nurse.balancePanel).toBeVisible({ timeout: 10000 });
  });

  test('NUR-FLU-003: Save fluid with empty fields still works', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await nurse.selectPatient('Коваленко');
    await nurse.selectHour(8);
    await nurse.clickSaveFluid();
    await expect(nurse.balancePanel).toBeVisible({ timeout: 10000 });
  });

  test('NUR-LOGOUT-001: Logout from nurse dashboard', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await nurse.clickLogout();
    await expect(page).toHaveURL('/login');
    await login.expectLoginFormVisible();
  });

  test('NUR-FLU-004: Save vitals with extreme values', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await nurse.selectPatient('Петренко');
    await nurse.selectHour(9);
    await nurse.fillAllVitals('250', '150', '200', '100', '42.0', '25', '40');
    await nurse.clickSaveVitals();
  });

  test('NUR-NAV-001: Direct /doctor access redirects nurse', async ({ page }) => {
    const login = new LoginPage(page);
    const nurse = new NurseDashboardPage(page);
    await login.navigate();
    await login.loginAs('nurse1', 'nurse123');
    await nurse.expectUrl(/\/nurse/);
    await page.goto('/doctor');
    await nurse.delay();
    await expect(nurse.getHeading()).toBeVisible({ timeout: 10000 });
    await expect(nurse.patientSelect).toBeVisible();
  });
});
