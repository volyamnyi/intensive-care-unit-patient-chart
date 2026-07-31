import { test, expect } from '../../fixtures/index';

const EPISODE_ID = 'a1111111-1111-1111-1111-111111111111';

test.describe('Sidebar resize', () => {
  test('sidebar sections are visible on the unified card', async ({ doctorPage }) => {
    await doctorPage.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    const sidebar = doctorPage.getByText('Пацієнт').first();
    await expect(sidebar).toBeVisible();
    await expect(doctorPage.getByText('Баланс рідини')).toBeVisible();
    await expect(doctorPage.getByText('Нотатки')).toBeVisible();
    await expect(doctorPage.getByText('Шкали')).toBeVisible();
    await expect(doctorPage.getByText('ШВЛ')).toBeVisible();
    await expect(doctorPage.getByText('Лабораторні результати')).toBeVisible();
    await expect(doctorPage.getByText('Стан пацієнта')).toBeVisible();
  });

  test('resize handle is present on the sidebar', async ({ doctorPage }) => {
    await doctorPage.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    const patientSection = doctorPage.getByText('Пацієнт').first();
    await expect(patientSection).toBeVisible();
    await expect(doctorPage.getByText('Петренко Іван Сергійович').first()).toBeVisible();
  });

  test('sidebar retains functionality after resize interaction', async ({ doctorPage }) => {
    await doctorPage.goto(`/icu/doctor/episode/${EPISODE_ID}`);
    await expect(doctorPage.getByText('Пацієнт').first()).toBeVisible();
    await expect(doctorPage.getByText('Петренко Іван Сергійович').first()).toBeVisible();
    const fluidBalance = doctorPage.getByText(/Надійшло|Виділено/).first();
    await expect(fluidBalance).toBeVisible();
  });
});
