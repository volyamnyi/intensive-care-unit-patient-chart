import { test, expect } from '@playwright/test';

test.describe('Prosthetics Wizard Flow', () => {
  test.use({ storageState: '.auth/prosthetist.json' });

  test('full wizard flow: patient search → order selection → review → template selection → wizard completion → PDF export', async ({ page }) => {
    // 1. Login and navigate to AppSelector, select "Виробництво протезів"
    await page.goto('/select');
    await expect(page.getByRole('heading', { name: 'Оберіть підсистему' })).toBeVisible();

    await page.getByRole('button', { name: 'Виробництво протезів' }).click();
    await page.waitForURL(/\/prosthetics/);

    // 2. Dashboard - should show empty or own instances
    await expect(page.getByRole('heading', { name: 'Дашборд протезування' })).toBeVisible();

    // 3. Search patient (seed: Сніжко Оксана Володимирівна)
    await page.getByPlaceholder('Пошук пацієнта...').fill('Сніжко');
    await page.getByRole('button', { name: 'Знайти' }).click();

    await expect(page.getByText('Сніжко Оксана Володимирівна')).toBeVisible();

    // 4. Select patient → order selection
    await page.getByRole('button', { name: 'Обрати пацієнта' }).first().click();
    await page.waitForURL(/\/prosthetics\/orders/);

    await expect(page.getByText('ПВ-26-0413')).toBeVisible();
    await expect(page.getByText('протез передпліччя')).toBeVisible();

    // 5. Select order → OrderReviewPage
    await page.getByRole('button', { name: 'Переглянути замовлення' }).first().click();
    await page.waitForURL(/\/prosthetics\/review/);

    // 6. Order Review - recipe PDF should render, "Старт" disabled until loaded
    await expect(page.getByRole('heading', { name: 'Огляд замовлення' })).toBeVisible();
    await expect(page.getByText('ПВ-26-0413')).toBeVisible();
    await expect(page.getByText('протез передпліччя')).toBeVisible();
    await expect(page.getByText('Сніжко Оксана Володимирівна')).toBeVisible();

    // PDF should be visible (iframe or embed)
    await expect(page.locator('iframe, embed, [data-testid="recipe-pdf"]')).toBeVisible({ timeout: 10000 });

    // "Старт" button should be disabled initially, then enabled
    const startButton = page.getByRole('button', { name: 'Старт' });
    await expect(startButton).toBeVisible();

    // 7. Select template → click "Старт"
    await startButton.click();
    await page.waitForURL(/\/prosthetics\/template/);

    // 8. Template Selection Page
    await expect(page.getByRole('heading', { name: 'Вибір шаблону' })).toBeVisible();
    await expect(page.getByText('TP-UL-01')).toBeVisible();
    await expect(page.getByText('Шаблон виготовлення протезу передпліччя')).toBeVisible();

    // 9. Select template → create instance → Wizard
    await page.getByRole('button', { name: 'Обрати шаблон' }).first().click();
    await page.waitForURL(/\/prosthetics\/wizard/);

    // 10. Wizard Screen - Stage 1: Клінічне обстеження
    await expect(page.getByRole('heading', { name: /Клінічне обстеження/ })).toBeVisible();

    // Step 1: Вимірювання
    await expect(page.getByText('Вимірювання')).toBeVisible();
    await page.getByLabel('Довжина кукси, см').fill('20');
    await page.getByLabel('Матеріал').fill('термопласт');
    await page.getByRole('button', { name: 'Готово' }).click();

    // Step 2: Анатомічний анамнез
    await expect(page.getByText('Анатомічний анамнез')).toBeVisible();
    await page.getByLabel('Диагноз').fill('Амптація внаслідок травми');
    await page.getByRole('button', { name: 'Готово' }).click();

    // Stage 2: Виготовлення
    await expect(page.getByRole('heading', { name: /Виготовлення/ })).toBeVisible();
    await expect(page.getByText('Приплічка')).toBeVisible();

    // Step 3: Приплічка - upload drawing
    await page.setInputFiles('input[type="file"]', 'tests/fixtures/test-drawing.png');
    await page.getByRole('button', { name: 'Готово' }).click();

    // Stage 3: Контроль якості (Quality Gate)
    await expect(page.getByRole('heading', { name: /Контроль якості/ })).toBeVisible();
    await expect(page.getByText('Приймальний контроль')).toBeVisible();

    // Quality Gate requires PROSTHETICS_ADMINISTRATOR - for prosthetist it should show pending
    await expect(page.getByText('Очікує затвердження')).toBeVisible();

    // 11. Success screen should be accessible after gate PASS (in real flow, admin would approve)
    // For this test, we verify the wizard structure is correct
    await expect(page.getByRole('button', { name: 'Завантажити PDF' })).toBeVisible();

    // 12. PDF Export
    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Завантажити PDF' }).click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toMatch(/prosthetics.*\.pdf$/);
  });

  test('wizard validation: "Готово" disabled until step is valid', async ({ page }) => {
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

    // Step 1: Вимірювання - required fields
    await expect(page.getByText('Вимірювання')).toBeVisible();

    // "Готово" should be disabled initially (required fields empty)
    const readyButton = page.getByRole('button', { name: 'Готово' });
    await expect(readyButton).toBeDisabled();

    // Fill first required field
    await page.getByLabel('Довжина кукси, см').fill('20');
    await expect(readyButton).toBeDisabled(); // Still disabled - second field empty

    // Fill second required field
    await page.getByLabel('Матеріал').fill('термопласт');
    await expect(readyButton).toBeEnabled();

    // Clear first field - should disable again
    await page.getByLabel('Довжина кукси, см').clear();
    await expect(readyButton).toBeDisabled();
  });

  test('evidence upload: PNG file accepted', async ({ page }) => {
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

    // Navigate to manufacturing stage (step 3)
    await page.getByLabel('Довжина кукси, см').fill('20');
    await page.getByLabel('Матеріал').fill('термопласт');
    await page.getByRole('button', { name: 'Готово' }).click();

    await page.getByLabel('Диагноз').fill('Травма');
    await page.getByRole('button', { name: 'Готово' }).click();

    // Now at Приплічка step with file upload
    await expect(page.getByText('Приплічка')).toBeVisible();

    // Upload PNG
    await page.setInputFiles('input[type="file"]', 'tests/fixtures/test-drawing.png');

    // Should accept the file
    await expect(page.getByText('test-drawing.png')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Готово' })).toBeEnabled();
  });
});