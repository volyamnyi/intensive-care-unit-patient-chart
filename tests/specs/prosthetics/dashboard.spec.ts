import { test, expect } from '@playwright/test';

test.describe('Prosthetics Dashboard', () => {
  test.describe('Prosthetist isolation - prosthetist1 cannot see prosthetist2 instances', () => {
    test.use({ storageState: '.auth/prosthetist.json' });

    test('prosthetist1 only sees own instances', async ({ page }) => {
      await page.goto('/select');
      await page.getByRole('button', { name: 'Виробництво протезів' }).click();
      await page.waitForURL(/\/prosthetics/);

      await expect(page.getByRole('heading', { name: 'Дашборд протезування' })).toBeVisible();

      // Dashboard should only show instances for prosthetist1 (Сніжко - ПВ-26-0413)
      // Should NOT show prosthetist2's instances (Гаврилюк - ПВ-26-0414)
      await expect(page.getByText('ПВ-26-0413')).toBeVisible();
      await expect(page.getByText('Сніжко Оксана Володимирівна')).toBeVisible();

      //prosthetist2's order should not be visible
      await expect(page.getByText('ПВ-26-0414')).not.toBeVisible();
      await expect(page.getByText('Гаврилюк Тарас Олексійович')).not.toBeVisible();
    });

    test('prosthetist2 only sees own instances', async ({ page }) => {
      // This would require prosthetist2 login - but we use prosthetist.json for prosthetist1
      // In CI, we'd run with different storageState
      test.skip(true, 'Requires prosthetist2 storageState - separate test run');
    });
  });

  test.describe('Status filters', () => {
    test.use({ storageState: '.auth/prosthetist.json' });

    test('filters by Active/Blocked/Completed/Failed', async ({ page }) => {
      await page.goto('/select');
      await page.getByRole('button', { name: 'Виробництво протезів' }).click();
      await page.waitForURL(/\/prosthetics/);

      // Status filter dropdown
      await expect(page.getByRole('combobox', { name: 'Статус' })).toBeVisible();

      // Test Active filter
      await page.getByRole('combobox', { name: 'Статус' }).click();
      await page.getByRole('option', { name: 'Активні' }).click();
      await expect(page.getByText('ПВ-26-0413')).toBeVisible();

      // Test Completed filter
      await page.getByRole('combobox', { name: 'Статус' }).click();
      await page.getByRole('option', { name: 'Завершені' }).click();
      // May be empty depending on test data

      // Test Failed filter
      await page.getByRole('combobox', { name: 'Статус' }).click();
      await page.getByRole('option', { name: 'Провалені' }).click();

      // Test All filter
      await page.getByRole('combobox', { name: 'Статус' }).click();
      await page.getByRole('option', { name: 'Всі' }).click();
      await expect(page.getByText('ПВ-26-0413')).toBeVisible();
    });
  });

  test.describe('Pause/Resume functionality', () => {
    test.use({ storageState: '.auth/prosthetist.json' });

    test('pause stops timer, shows BLOCKED_*/PAUSED status', async ({ page }) => {
      await page.goto('/select');
      await page.getByRole('button', { name: 'Виробництво протезів' }).click();
      await page.waitForURL(/\/prosthetics/);

      await page.getByPlaceholder('Пошук пацієнта...').fill('Сніжко');
      await page.getByRole('button', { name: 'Знайти' }).click();
      await page.getByRole('button', { name: 'Обрати пацієнта' }).first().click();
      await page.getByRole('button', { name: 'Переглянути замовлення' }).first().click();
      await page.getByRole('button', { name: 'Старт' }).click();
      await page.getByRole('button', { name: 'Обрати шаблон' }).first().click();
      await page.waitForURL(/\/prosthetics\/wizard/);

      // Start a step that has timer
      await page.getByLabel('Довжина кукси, см').fill('20');
      await page.getByLabel('Матеріал').fill('термопласт');

      // Timer should be running
      await expect(page.getByText(/таймер|timer/i)).toBeVisible();

      // Click pause
      await page.getByRole('button', { name: 'Пауза' }).click();

      // Select pause reason
      await page.getByRole('combobox', { name: 'Причина паузи' }).click();
      await page.getByRole('option', { name: 'Очікування матеріалів' }).click();
      await page.getByRole('button', { name: 'Підтвердити паузу' }).click();

      // Timer should stop, status should show PAUSED or BLOCKED
      await expect(page.getByText(/PAUSED|BLOCKED|пауза|зупинено/i)).toBeVisible();

      // Resume button should appear
      await expect(page.getByRole('button', { name: 'Продовжити' })).toBeVisible();

      // Resume
      await page.getByRole('button', { name: 'Продовжити' }).click();

      // Timer should restart
      await expect(page.getByText(/таймер|timer/i)).toBeVisible();
    });

    test('different pause categories show correct BLOCKED_* status', async ({ page }) => {
      await page.goto('/select');
      await page.getByRole('button', { name: 'Виробництво протезів' }).click();
      await page.waitForURL(/\/prosthetics/);

      await page.getByPlaceholder('Пошук пацієнта...').fill('Сніжко');
      await page.getByRole('button', { name: 'Знайти' }).click();
      await page.getByRole('button', { name: 'Обрати пацієнта' }).first().click();
      await page.getByRole('button', { name: 'Переглянути замовлення' }).first().click();
      await page.getByRole('button', { name: 'Старт' }).click();
      await page.getByRole('button', { name: 'Обрати шаблон' }).first().click();
      await page.waitForURL(/\/prosthetics\/wizard/);

      await page.getByLabel('Довжина кукси, см').fill('20');
      await page.getByLabel('Матеріал').fill('термопласт');

      // Test each pause category
      const categories = [
        { label: 'Очікування матеріалів', expectedStatus: 'BLOCKED_MATERIALS' },
        { label: 'Очікування схвалення', expectedStatus: 'BLOCKED_APPROVAL' },
        { label: 'Технічна перерва', expectedStatus: 'BLOCKED_TECHNICAL' },
        { label: 'Інше', expectedStatus: 'BLOCKED_OTHER' },
      ];

      for (const cat of categories) {
        await page.getByRole('button', { name: 'Пауза' }).click();
        await page.getByRole('combobox', { name: 'Причина паузи' }).click();
        await page.getByRole('option', { name: cat.label }).click();
        await page.getByRole('button', { name: 'Підтвердити паузу' }).click();

        await expect(page.getByText(cat.expectedStatus)).toBeVisible();

        await page.getByRole('button', { name: 'Продовжити' }).click();
      }
    });
  });
});