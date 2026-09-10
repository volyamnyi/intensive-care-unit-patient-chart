import { test, expect } from '../../fixtures';

// Team workload + attention queue UI (issue #278, epic #271).
// Read-only structural assertions: no data setup, no mutations.

test.describe('Production team and attention', () => {
  test('admin sees team and attention tabs with tables', async ({ adminPage }) => {
    await adminPage.goto('/prosthetics/production');
    await expect(adminPage.getByRole('heading', { name: 'Моніторинг виробництва' })).toBeVisible();

    await adminPage.getByRole('tab', { name: /Команда/ }).click();
    const teamTable = adminPage.getByRole('table');
    await expect(teamTable).toBeVisible();
    const teamHeaders = await teamTable.getByRole('columnheader').allTextContents();
    expect(teamHeaders[0]).toContain('Протезист');
    expect(teamHeaders).toContain('Прострочені');

    await adminPage.getByRole('tab', { name: /Потребують уваги/ }).click();
    const attentionTable = adminPage.getByRole('table');
    const emptyNote = adminPage.getByText('Проблемних виробів немає — усе в нормі');
    await expect(attentionTable.or(emptyNote)).toBeVisible();
  });

  test('team drill-down filters items by prosthetist', async ({ adminPage }) => {
    await adminPage.goto('/prosthetics/production');
    await adminPage.getByRole('tab', { name: /Команда/ }).click();
    const teamTable = adminPage.getByRole('table');
    await expect(teamTable).toBeVisible();

    const rows = teamTable.getByRole('row');
    if ((await rows.count()) <= 1) {
      // No team data in this run: nothing to drill into.
      await expect(adminPage.getByText('Немає даних про навантаження команди')).toBeVisible();
      return;
    }
    const name = ((await rows.nth(1).getByRole('cell').first().textContent()) ?? '').trim();
    await rows.nth(1).getByRole('button', { name: 'Вироби' }).click();
    await expect(adminPage.getByRole('tab', { name: 'Вироби' })).toBeVisible();
    await expect(adminPage.getByText(`Протезист: ${name}`)).toBeVisible();
  });

  test('prosthetist has no team tab but sees the attention tab', async ({ page }) => {
    await page.goto('/prosthetics/production');
    await expect(page.getByRole('heading', { name: 'Моніторинг виробництва' })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Команда/ })).toHaveCount(0);
    await expect(page.getByRole('tab', { name: /Потребують уваги/ })).toBeVisible();
  });
});
