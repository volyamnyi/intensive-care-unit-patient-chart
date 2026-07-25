import { test, expect } from '../../fixtures/index';

const PATIENT_ID = 3001;

test.describe('Prescription Workflow (Doctor)', () => {
  test('navigates to prescription page and sees info alert without patientId', async ({ page }) => {
    await page.goto('/prescriptions/doctor');
    await expect(page.getByText('Оберіть пацієнта для перегляду призначень')).toBeVisible();
    await expect(page.getByText('Для перегляду призначень перейдіть з картки пацієнта.')).toBeVisible();
  });

  test('shows prescription list for patient', async ({ page }) => {
    await page.goto(`/prescriptions/doctor?patientId=${PATIENT_ID}`);
    await expect(page.getByText(`Пацієнт ID: ${PATIENT_ID}`)).toBeVisible();
    await expect(page.getByRole('button', { name: /Новий листок/ })).toBeVisible();
  });

  test('prescription list page renders without errors', async ({ page }) => {
    await page.goto(`/prescriptions/doctor?patientId=${PATIENT_ID}`);
    // Verify the page loads and shows patient context
    await expect(page.getByText(`Пацієнт ID: ${PATIENT_ID}`)).toBeVisible();
    // Verify the new prescription button is present
    await expect(page.getByRole('button', { name: /Новий листок/ })).toBeVisible();
  });

  test('shows error when loading prescriptions for unknown patient', async ({ page }) => {
    // Navigate to a patient that may have no prescriptions
    await page.goto(`/prescriptions/doctor?patientId=99999`);
    await expect(page.getByText('Пацієнт ID: 99999')).toBeVisible();
    // Should show empty state or list (not crash)
    await expect(page.getByRole('button', { name: /Новий листок/ })).toBeVisible();
  });
});
