import { test, expect } from '@playwright/test';
import { USERS, FRONTEND_BASE, API, MIS_PATIENTS } from '../fixtures/env.js';
import { loginViaApi, createTestCard } from '../fixtures/auth.js';

test.describe('UI — Doctor dashboard & cards', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaApi(page, page.request, USERS.doctor1.login, USERS.doctor1.password);
    await page.waitForURL(/\/doctor/);
  });

  test('TC-DOC-01 dashboard shows active patients heading and table', async ({ page }) => {
    await expect(page.getByText('Активні пацієнти ВАІТ')).toBeVisible();
    await expect(page.locator('table')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Нова карта' })).toBeVisible();
  });

  test('TC-DOC-02 search box filters the patient table', async ({ page }) => {
    const search = page.getByPlaceholder('Пошук пацієнта за ПІБ...');
    await expect(search).toBeVisible();
    await search.fill('zzzznomatch');
    await expect(page.getByText('Немає активних пацієнтів')).toBeVisible();
    await search.fill('');
    await expect(page.locator('table tbody tr').first()).toBeVisible();
  });

  test('TC-DOC-03 navigate to create-card page', async ({ page }) => {
    await page.getByRole('button', { name: 'Нова карта' }).click();
    await page.waitForURL(/\/doctor\/create-card/);
    await expect(page.getByText('Нова карта інтенсивної терапії')).toBeVisible();
  });

  test('TC-DOC-04 create ICU card via UI flow', async ({ page }) => {
    await page.goto(FRONTEND_BASE + '/doctor/create-card');
    await expect(page.getByText('Нова карта інтенсивної терапії')).toBeVisible();
    const input = page.getByRole('combobox');
    await input.fill(MIS_PATIENTS.petrenko.name);
    await page.getByRole('option', { name: new RegExp(MIS_PATIENTS.petrenko.name) }).first().click();
    await expect(page.getByLabel('Діагноз')).toBeVisible();
    await page.getByLabel('Діагноз').fill('Гострий респіраторний дистрес-синдром');
    await page.getByLabel('APACHE II').fill('15');
    await page.getByLabel('SOFA').fill('9');
    await page.getByRole('button', { name: 'Створити карту' }).click();
    await page.waitForURL(/\/doctor\/card\/\d+\/day\/\d+/, { timeout: 15000 });
    await expect(page.getByText(MIS_PATIENTS.petrenko.name).first()).toBeVisible();
  });

  test('TC-DOC-05 open existing patient day shows details', async ({ page, request }) => {
    const { token } = await loginViaApi(page, request, USERS.doctor1.login, USERS.doctor1.password);
    const active = await request.get(`${API}/icu-cards/active`, { headers: { Authorization: `Bearer ${token}` } });
    const cards = await active.json();
    const card = cards.find((c) => c.icuDays?.some((d) => d.status === 'ACTIVE'));
    if (!card) throw new Error('No card with active day found');
    const day = card.icuDays.find((d) => d.status === 'ACTIVE');
    await page.goto(FRONTEND_BASE + `/doctor/card/${card.id}/day/${day.id}`);
    await page.waitForLoadState('networkidle');
    await expect(page.getByText(card.patientName).first()).toBeVisible();
    await expect(page.getByText('Вітальні показники')).toBeVisible();
  });

  test('TC-DOC-06 doctor can sign off an active day via UI', async ({ page, request }) => {
    const { token } = await loginViaApi(page, request, USERS.doctor1.login, USERS.doctor1.password);
    const { cardId, dayId } = await createTestCard(request, token, MIS_PATIENTS.sidorenko);
    await page.goto(FRONTEND_BASE + `/doctor/card/${cardId}/day/${dayId}`);
    await page.waitForLoadState('networkidle');
    await expect(page.getByText('Вітальні показники')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Підписати добу' })).toBeVisible();
    await page.getByRole('button', { name: 'Підписати добу' }).click();
    await page.getByRole('button', { name: 'Підписати' }).click();
    await page.waitForURL(/\/doctor$/, { timeout: 15000 });
    const dayResp = await request.get(`${API}/icu-days/${dayId}`, { headers: { Authorization: `Bearer ${token}` } });
    const day = await dayResp.json();
    expect(day.status).toBe('SIGNED');
  });

  test('TC-DOC-07 doctor cannot access nurse area (guard redirects)', async ({ page }) => {
    await page.goto(FRONTEND_BASE + '/nurse');
    await page.waitForURL(/\/doctor/);
  });

  test('TC-DOC-08 doctor can create a prescription from patient day', async ({ page, request }) => {
    const { token } = await loginViaApi(page, request, USERS.doctor1.login, USERS.doctor1.password);
    const { cardId, dayId } = await createTestCard(request, token, MIS_PATIENTS.kovalenko);
    await page.goto(FRONTEND_BASE + `/doctor/card/${cardId}/day/${dayId}`);
    await page.waitForLoadState('networkidle');
    await expect(page.getByText('Вітальні показники')).toBeVisible();
    await page.getByRole('tab', { name: 'Призначення' }).click();
    await page.getByLabel('Препарат').fill('Noradrenaline');
    await page.getByLabel('Доза').fill('10 мл');
    await page.getByLabel('Шлях').fill('IV');
    await page.getByRole('button', { name: '+', exact: true }).click();
    await expect(page.getByText('Noradrenaline')).toBeVisible();
  });
});
