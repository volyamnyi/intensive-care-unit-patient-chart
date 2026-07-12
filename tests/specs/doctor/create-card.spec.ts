import { test, expect } from '../../fixtures/index';

test.describe('Create Patient Card', () => {
  test('creates a new ICU card for a patient from MIS', async ({ page }) => {
    await page.goto('/doctor/create-card');
    await expect(page.getByText('Нова карта інтенсивної терапії')).toBeVisible();

    await page.getByRole('combobox').click();
    await page.getByRole('combobox').fill('Бондаренко');

    const option = page.getByRole('option', { name: /Бондаренко Тетяна/ });
    await expect(option).toBeVisible({ timeout: 10000 });
    await option.click();

    await expect(page.getByText('Дані пацієнта (з МІС)')).toBeVisible();

    await page.getByLabel('Діагноз').fill('Тестова пневмонія, дихальна недостатність');
    await page.getByLabel('APACHE II').fill('12');
    await page.getByLabel('SOFA').fill('4');

    await page.getByRole('button', { name: 'Створити карту' }).click();

    await expect(page).toHaveURL(/\/doctor\/card\/\d+\/day\/\d+/);
    await expect(page.getByText('Бондаренко Тетяна Миколаївна')).toBeVisible();
  });

  test('shows info message for short search query', async ({ page }) => {
    await page.goto('/doctor/create-card');
    await page.getByRole('combobox').click();
    await page.getByRole('combobox').fill('A');
    await expect(page.getByText('Введіть мінімум 2 символи')).toBeVisible();
  });
});
