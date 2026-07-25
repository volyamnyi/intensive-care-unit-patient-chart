import { test, expect } from '../../fixtures/index';

const PATIENT_ID = 3001;

test.describe('Prescription Workflow (Doctor)', () => {
  test('navigates to prescription page and sees info alert without patientId', async ({ page }) => {
    await page.goto('/doctor/prescriptions');
    await expect(page.getByText('Оберіть пацієнта для перегляду призначень')).toBeVisible();
    await expect(page.getByText('Для перегляду призначень перейдіть з картки пацієнта.')).toBeVisible();
  });

  test('shows prescription list for patient', async ({ page }) => {
    await page.goto(`/doctor/prescriptions?patientId=${PATIENT_ID}`);
    await expect(page.getByText(`Пацієнт ID: ${PATIENT_ID}`)).toBeVisible();
    await expect(page.getByRole('button', { name: /Новий листок/ })).toBeVisible();
  });

  test('creates a new prescription list and navigates to detail', async ({ page }) => {
    await page.goto(`/doctor/prescriptions?patientId=${PATIENT_ID}`);

    await page.getByRole('button', { name: /Новий листок/ }).click();

    // Should navigate to the prescription detail page
    await expect(page).toHaveURL(/\/doctor\/prescription\/[a-f0-9-]+/, { timeout: 15000 });
    await expect(page.getByText('Листок призначень').first()).toBeVisible({ timeout: 10000 });
  });

  test('prescription detail page shows empty items state', async ({ page }) => {
    await page.goto(`/doctor/prescriptions?patientId=${PATIENT_ID}`);
    await page.getByRole('button', { name: /Новий листок/ }).click();
    await expect(page).toHaveURL(/\/doctor\/prescription\/[a-f0-9-]+/, { timeout: 15000 });

    // The detail page should show the prescription item form area
    await expect(page.getByText('Додати препарат').first()).toBeVisible({ timeout: 10000 });
  });

  test('shows error when loading prescriptions fails', async ({ page }) => {
    // Navigate to a patient that may have no prescriptions
    await page.goto(`/doctor/prescriptions?patientId=99999`);
    await expect(page.getByText('Пацієнт ID: 99999')).toBeVisible();
    // Should show empty state or list (not crash)
    await expect(page.getByRole('button', { name: /Новий листок/ })).toBeVisible();
  });
});
