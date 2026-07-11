import { test, expect } from '@playwright/test';
import { USERS, FRONTEND_BASE, API, MIS_PATIENTS } from '../fixtures/env.js';
import { loginViaApi, createTestCard } from '../fixtures/auth.js';

test.describe('UI — Head of Department', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaApi(page, page.request, USERS.head1.login, USERS.head1.password);
    await page.waitForURL(/\/doctor/);
  });

  test('TC-HOD-01 HOD lands on doctor dashboard', async ({ page }) => {
    await expect(page.getByText('Активні пацієнти ВАІТ')).toBeVisible();
  });

  test('TC-HOD-02 HOD can create an ICU card via UI', async ({ page }) => {
    await page.goto(FRONTEND_BASE + '/doctor/create-card');
    await page.getByLabel('ПІБ, телефон або № медкарти').fill(MIS_PATIENTS.kovalenko.name);
    await page.getByRole('option', { name: new RegExp(MIS_PATIENTS.kovalenko.name) }).first().click();
    await page.getByLabel('Діагноз').fill('Сепсис');
    await page.getByLabel('APACHE II').fill('20');
    await page.getByLabel('SOFA').fill('12');
    await page.getByRole('button', { name: 'Створити карту' }).click();
    await page.waitForURL(/\/doctor\/card\/\d+\/day\/\d+/, { timeout: 15000 });
    await expect(page.getByText(MIS_PATIENTS.kovalenko.name)).toBeVisible();
  });

  test('TC-HOD-03 HOD can sign off a day', async ({ page, request }) => {
    const { token } = await loginViaApi(page, request, USERS.head1.login, USERS.head1.password);
    const { cardId, dayId } = await createTestCard(request, token, MIS_PATIENTS.petrenko);
    await page.goto(FRONTEND_BASE + `/doctor/card/${cardId}/day/${dayId}`);
    await page.getByRole('button', { name: 'Підписати добу' }).click();
    await page.getByRole('button', { name: 'Підписати' }).click();
    await page.waitForURL(/\/doctor$/);
    const dayResp = await request.get(`${API}/icu-days/${dayId}`, { headers: { Authorization: `Bearer ${token}` } });
    expect((await dayResp.json()).status).toBe('SIGNED');
  });

  test('TC-HOD-04 HOD can create a prescription', async ({ page, request }) => {
    const { token } = await loginViaApi(page, request, USERS.head1.login, USERS.head1.password);
    const { cardId, dayId } = await createTestCard(request, token, MIS_PATIENTS.sidorenko);
    await page.goto(FRONTEND_BASE + `/doctor/card/${cardId}/day/${dayId}`);
    await page.getByRole('tab', { name: 'Призначення' }).click();
    await page.getByLabel('Препарат').fill('Dobutamine');
    await page.getByLabel('Доза').fill('250 мл');
    await page.getByLabel('Шлях').fill('IV');
    await page.getByRole('button', { name: '+', exact: true }).click();
    await expect(page.getByText('Dobutamine')).toBeVisible();
  });

  test('TC-HOD-05 HOD cannot access nurse area (guard redirects)', async ({ page }) => {
    await page.goto(FRONTEND_BASE + '/nurse');
    await page.waitForURL(/\/doctor/);
  });
});
