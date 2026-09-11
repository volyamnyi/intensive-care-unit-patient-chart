import { test, expect } from '../../fixtures';

// Team workload + attention queue UI (issue #278, epic #271).
// Read-only structural assertions: no data setup, no mutations.

test.describe('Production team and attention', () => {
  test('admin sees team and attention tabs with tables', async ({ prostheticsAdminPage }) => {
    await prostheticsAdminPage.goto('/prosthetics/production');
    await expect(prostheticsAdminPage.getByRole('heading', { name: 'Моніторинг виробництва' })).toBeVisible();

    await prostheticsAdminPage.getByRole('tab', { name: /Команда/ }).click();
    const teamTable = prostheticsAdminPage.getByRole('table');
    await expect(teamTable).toBeVisible();
    const teamHeaders = await teamTable.getByRole('columnheader').allTextContents();
    expect(teamHeaders[0]).toContain('Протезист');
    expect(teamHeaders).toContain('Прострочені');

    await prostheticsAdminPage.getByRole('tab', { name: /Потребують уваги/ }).click();
    const attentionTable = prostheticsAdminPage.getByRole('table');
    const emptyNote = prostheticsAdminPage.getByText('Проблемних виробів немає — усе в нормі');
    await expect(attentionTable.or(emptyNote)).toBeVisible();
  });

  test('team drill-down filters items by prosthetist', async ({ prostheticsAdminPage }) => {
    await prostheticsAdminPage.goto('/prosthetics/production');
    await prostheticsAdminPage.getByRole('tab', { name: /Команда/ }).click();
    const teamTable = prostheticsAdminPage.getByRole('table');
    await expect(teamTable).toBeVisible();

    const rows = teamTable.getByRole('row');
    if ((await rows.count()) <= 1) {
      // No team data in this run: nothing to drill into.
      await expect(prostheticsAdminPage.getByText('Немає даних про навантаження команди')).toBeVisible();
      return;
    }
    const name = ((await rows.nth(1).getByRole('cell').first().textContent()) ?? '').trim();
    await rows.nth(1).getByRole('button', { name: 'Вироби' }).click();
    await expect(prostheticsAdminPage.getByRole('tab', { name: 'Вироби' })).toBeVisible();
    await expect(prostheticsAdminPage.getByText(`Протезист: ${name}`)).toBeVisible();
  });

  test('prosthetist has no team tab but sees the attention tab', async ({ page }) => {
    await page.goto('/prosthetics/production');
    await expect(page.getByRole('heading', { name: 'Моніторинг виробництва' })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Команда/ })).toHaveCount(0);
    await expect(page.getByRole('tab', { name: /Потребують уваги/ })).toBeVisible();
  });

  test('prosthetist without VIEW_ALL sees no analytics section', async ({ page }) => {
    await page.goto('/prosthetics/production');
    await expect(page.getByRole('heading', { name: 'Моніторинг виробництва' })).toBeVisible();
    await expect(page.getByText('Динаміка виробництва')).toHaveCount(0);
  });

  test('admin sees the trend chart and switches ranges', async ({ prostheticsAdminPage }) => {
    await prostheticsAdminPage.goto('/prosthetics/production');
    await expect(prostheticsAdminPage.getByText('Динаміка виробництва')).toBeVisible();
    await expect(prostheticsAdminPage.getByText('Створено')).toBeVisible();
    await prostheticsAdminPage.getByRole('tab', { name: '7д' }).click();
    await expect(prostheticsAdminPage.getByText('Динаміка виробництва')).toBeVisible();
    await prostheticsAdminPage.getByRole('tab', { name: '90д' }).click();
    await expect(prostheticsAdminPage.getByText('Динаміка виробництва')).toBeVisible();
  });
});
