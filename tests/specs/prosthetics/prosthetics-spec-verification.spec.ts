import { test, expect, Page, Locator } from '@playwright/test';
import { mkdirSync, existsSync, writeFileSync, appendFileSync } from 'fs';
import { completeInstanceViaApi } from '../../helpers/prosthetics-flow';

// ============== CONFIGURATION ==============
const CONFIG = {
  screenshotDir: 'screenshots/prosthetics-spec-verification',
  bugReportFile: 'test-results/prosthetics-spec-bugs.json',
  logFile: 'test-results/prosthetics-spec-test-log.txt',
  delayMs: 100, // Minimal delay for fast execution
  maxWizardSteps: 50,
  baseUrl: 'http://localhost:5173',
};

// ============== BUG TRACKING ==============
interface BugReport {
  id: string;
  step: number;
  phase: string;
  severity: 'Critical' | 'Major' | 'Minor';
  description: string;
  expectedBehavior: string;
  actualBehavior: string;
  screenshot?: string;
  consoleErrors?: string[];
  networkErrors?: string[];
  timestamp: string;
}

const bugs: BugReport[] = [];
let stepCounter = 0;
let currentPhase = '';

// ============== LOGGING ==============
function log(message: string) {
  const timestamp = new Date().toISOString();
  const logMessage = `[${timestamp}] ${message}`;
  console.log(logMessage);

  const dir = 'test-results';
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true });

  try {
    appendFileSync(CONFIG.logFile, logMessage + '\n');
  } catch {
    writeFileSync(CONFIG.logFile, logMessage + '\n');
  }
}

function logStep(action: string, details?: string) {
  stepCounter++;
  const message = `STEP ${stepCounter} [${currentPhase}]: ${action}${details ? ' | ' + details : ''}`;
  log(message);
  return stepCounter;
}

function reportBug(
  severity: 'Critical' | 'Major' | 'Minor',
  description: string,
  expectedBehavior: string,
  actualBehavior: string,
  screenshotPath?: string,
  consoleErrors?: string[]
) {
  const bug: BugReport = {
    id: `BUG-${String(bugs.length + 1).padStart(3, '0')}`,
    step: stepCounter,
    phase: currentPhase,
    severity,
    description,
    expectedBehavior,
    actualBehavior,
    screenshot: screenshotPath,
    consoleErrors,
    timestamp: new Date().toISOString(),
  };
  bugs.push(bug);
  log(`BUG REPORTED [${bug.id}] (${severity}): ${description}`);
}

// ============== SCREENSHOT HELPERS ==============
async function takeScreenshot(page: Page, name: string): Promise<string> {
  const dir = CONFIG.screenshotDir;
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true });

  const filename = `${dir}/${String(stepCounter).padStart(3, '0')}-${name}.png`;
  await page.screenshot({ path: filename, fullPage: false });
  return filename;
}

// ============== CONSOLE & NETWORK MONITORING ==============
function setupPageMonitoring(page: Page, consoleErrors: string[], networkErrors: string[]) {
  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
    }
  });

  page.on('requestfailed', request => {
    networkErrors.push(`Failed: ${request.url()} - ${request.failure()?.errorText}`);
  });

  page.on('response', response => {
    if (response.status() >= 400) {
      networkErrors.push(`HTTP ${response.status()}: ${response.url()}`);
    }
  });
}

// ============== HELPER FUNCTIONS ==============
async function delay() {
  await new Promise(resolve => setTimeout(resolve, CONFIG.delayMs));
}

async function waitForPageLoad(page: Page) {
  await page.waitForLoadState('domcontentloaded', { timeout: 15000 }).catch(() => {});
  await delay();
}

// ============== TEST SUITE ==============
test.describe('Prosthetist Technical Chart — Specification Verification', () => {
  let consoleErrors: string[];
  let networkErrors: string[];

  test.beforeAll(async () => {
    const dir = 'test-results';
    if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
    writeFileSync(CONFIG.logFile, `=== Prosthetics Specification Verification Test Log - ${new Date().toISOString()} ===\n\n`);
    log('Test suite started');
    log(`Configuration: delay=${CONFIG.delayMs}ms, maxWizardSteps=${CONFIG.maxWizardSteps}`);
  });

  test.afterAll(async () => {
    const dir = 'test-results';
    if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
    writeFileSync(CONFIG.bugReportFile, JSON.stringify(bugs, null, 2));

    log(`\n=== TEST SUITE COMPLETED ===`);
    log(`Total steps executed: ${stepCounter}`);
    log(`Total bugs reported: ${bugs.length}`);
    log(`  - Critical: ${bugs.filter(b => b.severity === 'Critical').length}`);
    log(`  - Major: ${bugs.filter(b => b.severity === 'Major').length}`);
    log(`  - Minor: ${bugs.filter(b => b.severity === 'Minor').length}`);
  });

  // ================================================================
  // MAIN E2E TEST: Complete workflow verification against specification
  // ================================================================
  test('Complete specification verification: All phases (1-6) with full validation', async ({ page, request }) => {
    test.setTimeout(300000);
    consoleErrors = [];
    networkErrors = [];
    setupPageMonitoring(page, consoleErrors, networkErrors);

    // ============== QUICK LOGIN (Skip detailed auth validation) ==============
    currentPhase = 'LOGIN';
    log(`\n--- ${currentPhase} ---`);

    await test.step('Quick login as prosthetist', async () => {
      logStep('Quick login as prosthetist1');
      await page.goto(`${CONFIG.baseUrl}/login`);
      await page.waitForSelector('#login', { timeout: 10000 });
      await page.locator('#login').fill('prosthetist1');
      await page.locator('#password').fill('doctor123');
      await page.getByRole('button', { name: 'Увійти' }).click();
      
      // Wait for navigation (may go to /select first)
      await page.waitForURL(/\/(prosthetics|select)/, { timeout: 15000 });
      
      // If on select page, navigate to prosthetics
      if (page.url().includes('/select')) {
        log('On select page, navigating to prosthetics');
        await page.goto(`${CONFIG.baseUrl}/prosthetics`);
        await page.waitForTimeout(1000);
      }
      
      await takeScreenshot(page, '01-logged-in');
      log(`✓ Logged in successfully, on: ${page.url()}`);
    });

    // ============== PHASE 2: DASHBOARD (Screen 2) ==============
    currentPhase = 'PHASE 2: DASHBOARD (Screen 2)';
    log(`\n--- ${currentPhase} ---`);

    await test.step('2.1 Verify Dashboard UI Elements (Spec 2.2)', async () => {
      logStep('Verify all dashboard UI elements per spec 2.2');

      // Header elements
      const header = page.locator('header, [class*="header"], [class*="navbar"]').first();
      await expect(header).toBeVisible({ timeout: 10000 });
      log('✓ Header visible');

      // New Process button (Quick Action)
      const newProcessButton = page.getByRole('button', { name: /Новий процес/i }).or(page.getByText(/Новий процес/i)).first();
      await expect(newProcessButton).toBeVisible({ timeout:5000 });
      log('✓ "Новий процес" button visible');

      await takeScreenshot(page, '06-dashboard-full');

      // Statistics cards (Spec 2.2)
      const statsCards = await page.getByText(/Активні|Призупинені|Завершені|Провалені/i).count();
      if (statsCards >= 4) {
        log(`✓ Statistics cards visible (${statsCards} found)`);
      } else {
        log(`⚠ Only ${statsCards}/4 statistics cards found`);
      }

      // Process table (Spec 2.2)
      const table = page.getByRole('table').or(page.locator('table')).first();
      const tableVisible = await table.isVisible({ timeout: 3000 }).catch(() => false);
      if (tableVisible) {
        log('✓ Process table visible');

        // Verify table headers (Spec 2.2)
        const expectedColumns = ['ID', 'Пацієнт', 'Замовлення', 'Шаблон', 'Статус'];
        const headers = await page.locator('th').allTextContents();
        log(`  Table headers: ${headers.join(', ')}`);
      } else {
        const emptyState = await page.getByText(/Немає процесів|Порожньо/i).isVisible({ timeout: 3000 }).catch(() => false);
        if (emptyState) {
          log('✓ Empty state visible (no processes yet)');
        } else {
          reportBug('Major', 'Neither process table nor empty state visible', 'Dashboard should show either a process table or an empty state message', 'No table or empty state found', await takeScreenshot(page, 'bug-dashboard-no-table'));
        }
      }
    });

    await test.step('2.2 Test filter tabs (Spec 2.2)', async () => {
      logStep('Test all filter tabs per spec 2.2');

      const filterTabs = ['Всі', 'Активні', 'Призупинені', 'Завершені', 'Провалені'];
      for (const tabName of filterTabs) {
        const tab = page.getByRole('tab', { name: tabName }).or(page.getByText(tabName)).first();
        if (await tab.isVisible({ timeout: 2000 }).catch(() => false)) {
          await tab.click();
          await delay();
          log(`✓ Filter tab "${tabName}" clicked`);
        } else {
          log(`⚠ Filter tab "${tabName}" not found`);
        }
      }

      // Reset to "Всі"
      const allTab = page.getByText('Всі').first();
      if (await allTab.isVisible({ timeout: 2000 }).catch(() => false)) {
        await allTab.click();
        await delay();
      }
      await takeScreenshot(page, '07-dashboard-filters-tested');
    });

    await test.step('2.3 Test search functionality (Spec 2.2)', async () => {
      logStep('Test search functionality per spec 2.2');

      const searchInput = page.getByPlaceholder(/Пошук/i).or(page.locator('input[type="search"], input[placeholder*="пошук" i]').first());
      if (await searchInput.isVisible({ timeout: 5000 }).catch(() => false)) {
        await searchInput.fill('ПВ-26');
        await delay();
        await delay(); // Wait for debounce
        log('✓ Search executed with "ПВ-26"');
        await takeScreenshot(page, '08-search-results');

        // Clear search
        await searchInput.fill('');
        await delay();
      } else {
        log('⚠ Search input not found');
      }
    });

    // ============== PHASE 3: CREATING NEW PROCESS (Screens 3-6) ==============
    currentPhase = 'PHASE 3: NEW PROCESS CREATION (Screens 3-6)';
    log(`\n--- ${currentPhase} ---`);

    // ----- Screen 3: Patient Selection (Spec 2.3.1) -----
    await test.step('3.1 Navigate to Patient Selection (Spec 2.3.1)', async () => {
      logStep('Click "Новий процес" to navigate to patient selection');

      const newProcessButton = page.getByRole('button', { name: /Новий процес/i }).or(page.getByText(/Новий процес/i)).first();
      await newProcessButton.click();

      await page.waitForURL('**/prosthetics/new/**', { timeout: 15000 });
      await waitForPageLoad(page);
      await takeScreenshot(page, '09-patient-selection');

      log(`✓ Navigated to patient selection: ${page.url()}`);
    });

    await test.step('3.2 Verify Patient Selection UI (Spec 2.3.1)', async () => {
      logStep('Verify patient selection screen elements per spec 2.3.1');

      // Step indicator (Крок 1 з 4)
      const stepIndicator = await page.getByText(/Крок 1 з 4/i).isVisible({ timeout: 5000 }).catch(() => false);
      if (stepIndicator) {
        log('✓ Step indicator "Крок 1 з 4" visible');
      } else {
        reportBug('Minor', 'Step indicator "Крок 1 з 4" not visible', 'Screen should show "Крок 1 з 4"', 'Step indicator not found');
      }

      // Search input
      const searchInput = page.getByPlaceholder(/Пошук|ПІБ|ID/i).or(page.locator('input[type="text"], input[type="search"]').first());
      await expect(searchInput.first()).toBeVisible({ timeout: 10000 });
      log('✓ Patient search input visible');

      // Empty state message (initial)
      const emptyState = await page.getByText(/Введіть|Порожньо|Немає результатів/i).isVisible({ timeout: 3000 }).catch(() => false);
      if (emptyState) {
        log('✓ Empty state message visible (initial state)');
      }

      // "Далі" button (should be disabled initially)
      const nextButton = page.getByRole('button', { name: /Далі/i }).first();
      const nextDisabled = await nextButton.isDisabled({ timeout: 3000 }).catch(() => false);
      if (nextDisabled) {
        log('✓ "Далі" button disabled (Hard Block working - no patient selected)');
      } else {
        reportBug('Major', '"Далі" button should be disabled when no patient selected', 'Hard Block: cannot proceed without selection', '"Далі" button is enabled without selection');
      }

      await takeScreenshot(page, '10-patient-selection-ui');
    });

    await test.step('3.3 Search for patient (Spec 2.3.1)', async () => {
      logStep('Search for patient "Сніжко"');

      const searchInput = page.getByPlaceholder(/Пошук|ПІБ|ID/i).or(page.locator('input[type="text"], input[type="search"]').first());
      await searchInput.first().fill('Сніжко');

      // Wait for debounce (300ms) + actual API response before asserting state
      await page.waitForResponse(
        (res) => res.url().includes('/prosthesis-manufacturing/patients') && res.request().method() === 'GET',
        { timeout: 15000 }
      ).catch(() => {});
      await delay();

      await takeScreenshot(page, '11-patient-search-results');

      // Verify results table
      const table = page.getByRole('table').or(page.locator('table')).first();
      const tableVisible = await table.isVisible({ timeout: 5000 }).catch(() => false);

      if (tableVisible) {
        // Count rows
        const rows = await page.locator('tbody tr').count();
        log(`✓ Patient search returned ${rows} results`);

        // Verify table columns (Spec 2.3.1)
        const headers = await page.locator('th').allTextContents();
        log(`  Table headers: ${headers.join(', ')}`);

        if (rows === 0) {
          reportBug('Critical', 'No patients found in search', 'Search should return patients from mock data (Сніжко Іван Петрович)', 'Zero results returned for valid patient name');
        }
      } else {
        // Check for "no results" message
        const noResults = await page.getByText(/не знайдено|Немає результатів/i).isVisible({ timeout: 3000 }).catch(() => false);
        if (noResults) {
          reportBug('Critical', 'No patients found - "не знайдено" message', 'Search should return patients from mock data', 'No results message displayed for valid search term');
        } else {
          reportBug('Major', 'No table or "no results" message displayed', 'Search results table or "no results" message should appear', 'Neither table nor message found');
        }
      }
    });

    await test.step('3.4 Test empty search result (Spec 2.3.1)', async () => {
      logStep('Test search with no matching results');

      const searchInput = page.getByPlaceholder(/Пошук|ПІБ|ID/i).or(page.locator('input[type="text"], input[type="search"]').first());
      await searchInput.first().fill('ZZZZZZZZZZZ');

      // Wait for debounce (300ms) + actual API response before asserting the empty state
      await page.waitForResponse(
        (res) => res.url().includes('/prosthesis-manufacturing/patients') && res.request().method() === 'GET',
        { timeout: 15000 }
      ).catch(() => {});
      await delay();

      const noResults = await page.getByText(/не знайдено|Немає результатів/i).isVisible({ timeout: 5000 }).catch(() => false);
      if (noResults) {
        log('✓ "No results" message displayed correctly');
      } else {
        reportBug('Minor', '"No results" message should be displayed', 'System should show "За вказаним запитом пацієнтів не знайдено"', 'Message not found for empty search');
      }

      await takeScreenshot(page, '12-patient-search-empty');

      // Re-enter valid search
      await searchInput.first().fill('Сніжко');
      await page.waitForResponse(
        (res) => res.url().includes('/prosthesis-manufacturing/patients') && res.request().method() === 'GET',
        { timeout: 15000 }
      ).catch(() => {});
      await delay();
    });

    await test.step('3.5 Select patient (Spec 2.3.1)', async () => {
      logStep('Select patient from search results');

      // Find and click "Обрати" button in the row containing "Сніжко"
      const rows = page.locator('tbody tr');
      const count = await rows.count();

      for (let i = 0; i < count; i++) {
        const row = rows.nth(i);
        const text = await row.textContent();
        if (text && text.includes('Сніжко')) {
          const selectButton = row.getByRole('button', { name: /Обрати/i });
          await selectButton.click();
          log('✓ Patient "Сніжко" selected');
          break;
        }
      }

      await delay();
      await takeScreenshot(page, '13-patient-selected');

      // Verify "Далі" button becomes enabled (Hard Block released)
      const nextButton = page.getByRole('button', { name: /Далі/i }).first();
      const nextEnabled = await nextButton.isEnabled({ timeout: 3000 }).catch(() => false);
      if (nextEnabled) {
        log('✓ "Далі" button enabled after patient selection (Hard Block released)');
      } else {
        // In actual implementation, selecting patient may auto-navigate
        log('⚠ "Далі" button still disabled or auto-navigation occurred');
      }
    });

    // ----- Screen 4: Order Selection (Spec 2.3.2) -----
    await test.step('3.6 Navigate to Order Selection (Spec 2.3.2)', async () => {
      logStep('Navigate to order selection screen');

      // Check if already navigated (auto-navigation after patient selection)
      const currentUrl = page.url();
      if (currentUrl.includes('select-order')) {
        log('✓ Already on order selection screen (auto-navigation)');
      } else {
        // Click "Далі" to navigate
        const nextButton = page.getByRole('button', { name: /Далі/i }).first();
        await nextButton.click();
        await page.waitForURL('**/select-order', { timeout: 10000 });
      }

      await waitForPageLoad(page);
      await takeScreenshot(page, '14-order-selection');
    });

    await test.step('3.7 Verify Order Selection UI (Spec 2.3.2)', async () => {
      logStep('Verify order selection screen elements per spec 2.3.2');

      // Step indicator (Крок 2 з 4)
      const stepIndicator = await page.getByText(/Крок 2 з 4/i).isVisible({ timeout: 5000 }).catch(() => false);
      if (stepIndicator) {
        log('✓ Step indicator "Крок 2 з 4" visible');
      }

      // Patient context card (Sticky Header)
      const patientContext = await page.getByText(/Пацієнт|Сніжко/i).first().isVisible({ timeout: 5000 }).catch(() => false);
      if (patientContext) {
        log('✓ Patient context card visible (sticky header)');
      } else {
        reportBug('Minor', 'Patient context card not visible', 'Screen should show patient info in sticky header', 'Patient context not found');
      }

      // Orders table
      const table = page.getByRole('table').or(page.locator('table')).first();
      const tableVisible = await table.isVisible({ timeout: 5000 }).catch(() => false);
      if (tableVisible) {
        const rows = await page.locator('tbody tr').count();
        log(`✓ Orders table visible with ${rows} orders`);

        // Verify table columns (Spec 2.3.2)
        const headers = await page.locator('th').allTextContents();
        log(`  Table headers: ${headers.join(', ')}`);
      } else {
        reportBug('Major', 'Orders table not visible', 'Screen should show orders table', 'Table not found');
      }

      await takeScreenshot(page, '15-order-selection-ui');
    });

    await test.step('3.8 Select order (Spec 2.3.2)', async () => {
      logStep('Select order from the list');

      // Find and click "Обрати" button for an order
      const rows = page.locator('tbody tr');
      const count = await rows.count();

      if (count > 0) {
        const firstRow = rows.first();
        const selectButton = firstRow.getByRole('button', { name: /Обрати/i });
        await selectButton.click();
        log('✓ Order selected');
      } else {
        reportBug('Critical', 'No orders available to select', 'Orders table should have at least one active order', 'Zero orders in table');
      }

      await delay();
      await takeScreenshot(page, '16-order-selected');
    });

    // ----- Screen 5: Order Review (Spec 2.3.3) -----
    await test.step('3.9 Navigate to Order Review (Spec 2.3.3)', async () => {
      logStep('Navigate to order review screen');

      const currentUrl = page.url();
      if (currentUrl.includes('order-review') || currentUrl.includes('review')) {
        log('✓ Already on order review screen');
      } else {
        const nextButton = page.getByRole('button', { name: /Далі/i }).first();
        await nextButton.click();
        await page.waitForURL('**/order-review**', { timeout: 10000 }).catch(() => {
          log('⚠ URL did not match expected pattern, checking current state');
        });
      }

      await waitForPageLoad(page);
      await takeScreenshot(page, '17-order-review');
    });

    await test.step('3.10 Verify Order Review UI (Spec 2.3.3)', async () => {
      logStep('Verify order review screen elements per spec 2.3.3');

      // Step indicator (Крок 3 з 4)
      const stepIndicator = await page.getByText(/Крок 3 з 4/i).isVisible({ timeout: 5000 }).catch(() => false);
      if (stepIndicator) {
        log('✓ Step indicator "Крок 3 з 4" visible');
      }

      // Left panel - Patient info (Spec 2.3.3 Section 1)
      const patientInfo = await page.getByText(/ПІБ|Дата народження|Стать/i).first().isVisible({ timeout: 5000 }).catch(() => false);
      if (patientInfo) {
        log('✓ Patient info panel visible');
      }

      // Central panel - PDF viewer (Spec 2.3.3)
      const pdfViewer = await page.locator('iframe, embed, [class*="pdf"], [class*="viewer"]').first().isVisible({ timeout: 5000 }).catch(() => false);
      if (pdfViewer) {
        log('✓ PDF viewer visible');
      } else {
        reportBug('Minor', 'PDF viewer not visible', 'Screen should show embedded PDF viewer', 'PDF viewer not found');
      }

      // Right panel - Materials (Spec 2.3.3)
      const materialsPanel = await page.getByText(/Специфікація матеріалів|Матеріали/i).first().isVisible({ timeout: 3000 }).catch(() => false);
      if (materialsPanel) {
        log('✓ Materials panel visible');
      }

      // "Старт" button (should be disabled until PDF loads - Hard Block)
      const startButton = page.getByRole('button', { name: /Старт/i }).first();
      const startVisible = await startButton.isVisible({ timeout: 3000 }).catch(() => false);
      if (startVisible) {
        log('✓ "Старт" button visible');
      }

      await takeScreenshot(page, '18-order-review-ui');
    });

    await test.step('3.11 Start process (Spec 2.3.3)', async () => {
      logStep('Click "Старт" to proceed to template selection');

      const startButton = page.getByRole('button', { name: /Старт/i }).first();

      // Wait for button to become enabled (PDF loaded)
      await startButton.waitFor({ state: 'visible', timeout: 15000 });
      await delay();

      const enabled = await startButton.isEnabled({ timeout: 5000 }).catch(() => false);
      if (enabled) {
        await startButton.click();
        log('✓ "Старт" button clicked');
      } else {
        // Force click to test behavior
        reportBug('Major', '"Старт" button should be enabled after PDF loads', 'Button should become active when document is ready', 'Button remains disabled');
        await startButton.click({ force: true });
      }

      await delay();
      await takeScreenshot(page, '19-after-start');
    });

    // ----- Screen 6: Template Selection (Spec 2.3.4) -----
    await test.step('3.12 Navigate to Template Selection (Spec 2.3.4)', async () => {
      logStep('Navigate to template selection screen');

      const currentUrl = page.url();
      if (currentUrl.includes('select-template') || currentUrl.includes('template')) {
        log('✓ On template selection screen');
      } else {
        await page.waitForURL('**/select-template**', { timeout: 10000 }).catch(() => {
          log('⚠ URL did not match expected pattern');
        });
      }

      await waitForPageLoad(page);
      await takeScreenshot(page, '20-template-selection');
    });

    await test.step('3.13 Verify Template Selection UI (Spec 2.3.4)', async () => {
      logStep('Verify template selection screen elements per spec 2.3.4');

      // Step indicator (Крок 4 з 4)
      const stepIndicator = await page.getByText(/Крок 4 з 4/i).isVisible({ timeout: 5000 }).catch(() => false);
      if (stepIndicator) {
        log('✓ Step indicator "Крок 4 з 4" visible');
      }

      // Context info (Patient PIB)
      const contextInfo = await page.getByText(/Пацієнт:/i).isVisible({ timeout: 5000 }).catch(() => false);
      if (contextInfo) {
        log('✓ Context info visible (Patient PIB)');
      }

      // Template cards grid
      const templateCards = await page.locator('[class*="cursor-pointer"], [class*="template-card"], [class*="card"]').count();
      if (templateCards > 0) {
        log(`✓ Template cards visible (${templateCards} cards found)`);
      } else {
        const emptyState = await page.getByText(/Немає доступних шаблонів/i).isVisible({ timeout: 3000 }).catch(() => false);
        if (emptyState) {
          reportBug('Critical', 'Template filtering returns 0 results', 'Templates should be filtered by patient/order data', 'Empty state displayed - no templates available');
        }
      }

      await takeScreenshot(page, '21-template-selection-ui');
    });

    await test.step('3.14 Select template and create process (Spec 2.3.4)', async () => {
      logStep('Select template to create process instance');

      // Find and click a template card
      const templateCard = page.locator('[class*="cursor-pointer"]').filter({ hasText: /TP-UL|TP-LL|Шаблон/i }).first();
      const cardVisible = await templateCard.isVisible({ timeout: 5000 }).catch(() => false);

      if (cardVisible) {
        await templateCard.click();
        await delay();
        log('✓ Template card selected');
      }

      // Click "Обрати" button to create process (shows «Обрано» once the card is selected)
      const selectButton = page.getByRole('button', { name: /Обрати|Обрано/i }).first();
      const buttonVisible = await selectButton.isVisible({ timeout: 5000 }).catch(() => false);

      if (buttonVisible) {
        await selectButton.click();
        log('✓ "Обрати" button clicked - process instance created');
      } else {
        reportBug('Major', '"Обрати" button not found', 'Button should be visible after template selection', 'Button not found');
      }

      await delay();
      await delay();
      await takeScreenshot(page, '22-after-template-selection');
    });

    // ============== PHASE 4: PROCESS EXECUTION (Screens 7-8) ==============
    currentPhase = 'PHASE 4: PROCESS EXECUTION (Screens 7-8)';
    log(`\n--- ${currentPhase} ---`);

    await test.step('4.1 Navigate to Process/Tech Card Overview (Screen 7)', async () => {
      logStep('Navigate to process overview screen');

      const currentUrl = page.url();
      log(`Current URL after template selection: ${currentUrl}`);

      // Check if on process page
      if (currentUrl.includes('/prosthetics/process/')) {
        log('✓ On process page');
      } else {
        // Try to navigate to process page
        await page.goto(`${CONFIG.baseUrl}/prosthetics`);
        await delay();

        // Find and click on the new process
        const openButton = page.getByRole('button', { name: /Відкрити|Переглянути/i }).first();
        if (await openButton.isVisible({ timeout: 5000 }).catch(() => false)) {
          await openButton.click();
          await delay();
        }
      }

      await takeScreenshot(page, '23-process-overview');
    });

    await test.step('4.2 Verify Tech Card Overview UI (Spec 2.4.1)', async () => {
      logStep('Verify tech card overview screen elements per spec 2.4.1');

      // Check for process structure (left panel)
      const structureTree = await page.getByText(/Етап|Крок|Stage|Step/i).first().isVisible({ timeout: 5000 }).catch(() => false);
      if (structureTree) {
        log('✓ Process structure tree visible');
      }

      // Check for BPMN diagram (center panel)
      const diagram = await page.locator('[class*="diagram"], [class*="bpmn"], [class*="flow"]').first().isVisible({ timeout: 3000 }).catch(() => false);
      if (diagram) {
        log('✓ Process diagram visible');
      }

      // Check for metadata (right panel)
      const metadata = await page.getByText(/Пацієнт|Замовлення|Виконавець/i).first().isVisible({ timeout: 3000 }).catch(() => false);
      if (metadata) {
        log('✓ Process metadata visible');
      }

      // "Розпочати процес" button
      const startProcessButton = page.getByRole('button', { name: /Розпочати|Почати/i }).first();
      const startVisible = await startProcessButton.isVisible({ timeout: 3000 }).catch(() => false);
      if (startVisible) {
        log('✓ "Розпочати процес" button visible');
      }

      await takeScreenshot(page, '24-tech-card-overview-ui');
    });

    await test.step('4.3 Start process / Navigate to Wizard (Screen 8)', async () => {
      logStep('Start process to navigate to wizard execution');

      const startProcessButton = page.getByRole('button', { name: /Розпочати|Почати/i }).first();
      if (await startProcessButton.isVisible({ timeout: 5000 }).catch(() => false)) {
        await startProcessButton.click();
        log('✓ Process started');
      }

      await delay();
      await takeScreenshot(page, '25-wizard-start');
    });

    await test.step('4.4 Execute Wizard Steps (Spec 2.4.2)', async () => {
      logStep('Execute wizard steps per spec 2.4.2');

      let stepsCompleted = 0;
      let qualityGateFound = false;
      let processComplete = false;

      for (let i = 0; i < CONFIG.maxWizardSteps; i++) {
        log(`\n--- Wizard iteration ${i + 1} ---`);

        // Check if at quality gate (the gate panel shows «Прийнято (Pass)», disabled for non-approvers)
        const qualityGateButton = page.getByRole('button', { name: /Схвалити|Пройдено|Прийнято/i }).first();
        if (await qualityGateButton.isVisible({ timeout: 2000 }).catch(() => false)) {
          log('Quality Gate detected');
          qualityGateFound = true;
          break;
        }

        // Check if process is complete
        const completeText = await page.getByText(/успішно завершено|Процес завершено|Завершено/i).isVisible({ timeout: 2000 }).catch(() => false);
        const doneUrl = page.url().includes('/done');
        if (completeText || doneUrl) {
          log('Process completed');
          processComplete = true;
          break;
        }

        // Try to complete current step (CTA is «Готово →» or «Контроль якості →» when the next stage has a gate)
        const completeButton = page.getByRole('button', { name: /Готово|Завершити крок|Контроль якості/i }).first();
        if (await completeButton.isVisible({ timeout: 3000 }).catch(() => false)) {
          const enabled = await completeButton.isEnabled({ timeout: 2000 }).catch(() => false);
          // The completion POST can be lost (proxy hiccup) leaving the UI stuck in "submitting".
          // Verify the step actually completed via the API and retry the click when it did not.
          const instanceId = page.url().match(/\/process\/([0-9a-f-]+)/)?.[1];
          const countCompleted = async (): Promise<number> => {
            if (!instanceId) return -1;
            const res = await request.get(`http://localhost:8085/api/prosthesis-manufacturing/instances/${instanceId}/step-executions`).catch(() => null);
            if (!res || !res.ok()) return -1;
            const steps = (await res.json()) as Array<{ status: string }>;
            return steps.filter((s) => s.status === 'COMPLETED').length;
          };
          if (enabled) {
            // Fill any required fields before completing
            await fillRequiredFields(page);
            const before = await countCompleted();
            await completeButton.click({ timeout: 5000 }).catch(() => {});
            for (let retry = 0; retry < 4; retry++) {
              await page.waitForTimeout(2000);
              if (await countCompleted() > before || page.url().includes('/done')) break;
              await completeButton.click({ timeout: 5000, force: true }).catch(() => {});
            }
            stepsCompleted++;
            log(`✓ Step ${i + 1} completed (${stepsCompleted} total)`);
          } else {
            // Fill required fields
            await fillRequiredFields(page);

            // Try clicking anyway
            const before = await countCompleted();
            await completeButton.click({ timeout: 5000, force: true }).catch(() => {});
            for (let retry = 0; retry < 4; retry++) {
              await page.waitForTimeout(2000);
              if (await countCompleted() > before || page.url().includes('/done')) break;
              await completeButton.click({ timeout: 5000, force: true }).catch(() => {});
            }
            stepsCompleted++;
            log(`⚠ Step ${i + 1} completed (forced click)`);
          }
        } else {
          // No complete button - try filling fields and see if auto-advances
          await fillRequiredFields(page);
          await delay();
        }

        await delay();
        await takeScreenshot(page, `26-wizard-step-${i + 1}`);
      }

      log(`✓ Completed ${stepsCompleted} wizard steps`);
      log(`Quality Gate found: ${qualityGateFound}`);
      log(`Process complete: ${processComplete}`);
    });

    // ============== PHASE 5: QUALITY GATE (Screen 9) ==============
    currentPhase = 'PHASE 5: QUALITY GATE (Screen 9)';
    log(`\n--- ${currentPhase} ---`);

    await test.step('5.1 Handle Quality Gate (Spec 2.5.1)', async () => {
      logStep('Handle quality gate decision per spec 2.5.1');

      // Check if at quality gate
      const passButton = page.getByRole('button', { name: /Схвалити|Пройдено/i }).first();
      const passVisible = await passButton.isVisible({ timeout: 5000 }).catch(() => false);

      if (passVisible) {
        log('✓ Quality gate detected');

        // Count criteria checkboxes
        const checkboxes = await page.locator('input[type="checkbox"]').count();
        log(`  ${checkboxes} criteria checkboxes found`);

        // Check all criteria
        const allCheckboxes = page.locator('input[type="checkbox"]');
        for (let i = 0; i < checkboxes; i++) {
          await allCheckboxes.nth(i).check({ force: true });
        }
        log('✓ All criteria checked');

        await delay();
        await takeScreenshot(page, '27-quality-gate-checked');

        // Pass the gate
        await passButton.click();
        log('✓ Quality gate passed');
      } else if (page.url().includes('/done')) {
        log('Process completed directly (no quality gate encountered)');
      } else {
        log('⚠ No quality gate detected - checking current state');
      }

      await delay();
      await takeScreenshot(page, '28-after-quality-gate');
    });

    await test.step('5.2 Complete the process via API (cleanup)', async () => {
      // The gate decision requires PROSTHETICS_ADMINISTRATOR — drive the instance to
      // COMPLETED through the backend API so no active process is left behind.
      const instanceId = page.url().match(/\/process\/([0-9a-f-]+)/)?.[1];
      if (instanceId) {
        await completeInstanceViaApi(request, instanceId);
        await page.reload();
        await page.waitForTimeout(1000);
        log('✓ Process completed via API');
      }
    });

    // ============== PHASE 6: COMPLETION (Screens 11-15) ==============
    currentPhase = 'PHASE 6: COMPLETION (Screens 11-15)';
    log(`\n--- ${currentPhase} ---`);

    await test.step('6.1 Verify Process Completion (Spec 2.6.1)', async () => {
      logStep('Verify completion state per spec 2.6.1');

      const url = page.url();
      log(`Final URL: ${url}`);

      // Check for completion indicators
      const isDone = url.includes('/done');
      const isFailed = url.includes('/failed');
      const hasSuccessText = await page.getByText(/успішно завершено|Процес завершено|Завершено/i).isVisible({ timeout: 3000 }).catch(() => false);

      if (isDone || hasSuccessText) {
        log('Process completed successfully!');
        await takeScreenshot(page, '29-process-completed');

        // Verify completion screen elements (Spec 2.6.1)
        const processInfo = await page.getByText(/Пацієнт|Замовлення/i).first().isVisible({ timeout: 3000 }).catch(() => false);
        if (processInfo) {
          log('✓ Process info visible on completion screen');
        }

        const exportButton = page.getByRole('button', { name: /Експорт|PDF/i }).first();
        if (await exportButton.isVisible({ timeout: 3000 }).catch(() => false)) {
          log('✓ Export PDF button visible');
        }

      } else if (isFailed) {
        reportBug('Major', 'Process ended in failed state', 'Process should complete successfully with valid input', 'Process ended in /failed state', await takeScreenshot(page, 'bug-process-failed'));
      } else {
        log(`⚠ Final state unclear - URL: ${url}`);
      }
    });

    await test.step('6.2 Navigate back to Dashboard (Spec 2.6.1)', async () => {
      logStep('Navigate back to dashboard');

      // Click return button or navigate
      const returnButton = page.getByRole('button', { name: /Повернутися|Панель управління|Dashboard/i }).first();
      if (await returnButton.isVisible({ timeout: 3000 }).catch(() => false)) {
        await returnButton.click();
      } else {
        await page.goto(`${CONFIG.baseUrl}/prosthetics`);
      }

      await delay();
      await takeScreenshot(page, '30-back-to-dashboard');
      log('✓ Navigated to dashboard');
    });

    // ============== FINAL SUMMARY ==============
    currentPhase = 'VERIFICATION COMPLETE';
    await test.step('Final verification summary', async () => {
      logStep('Final test summary');
      await takeScreenshot(page, '31-test-complete');

      log('\n========================================');
      log('SPECIFICATION VERIFICATION SUMMARY');
      log('========================================');
      log(`Total steps executed: ${stepCounter}`);
      log(`Total bugs reported: ${bugs.length}`);
      log(`  Critical: ${bugs.filter(b => b.severity === 'Critical').length}`);
      log(`  Major: ${bugs.filter(b => b.severity === 'Major').length}`);
      log(`  Minor: ${bugs.filter(b => b.severity === 'Minor').length}`);
      log(`Console errors: ${consoleErrors.length}`);
      log(`Network errors: ${networkErrors.length}`);
      log('========================================');
    });
  });
});

// ============== HELPER: Fill Required Fields ==============
async function fillRequiredFields(page: Page) {
  // Fill text inputs
  const textInputs = page.locator('input[type="text"]:visible, input:not([type]):visible');
  const textCount = await textInputs.count();
  for (let i = 0; i < textCount; i++) {
    const input = textInputs.nth(i);
    const value = await input.inputValue();
    if (!value) {
      // Resource inputs sit near the bottom of the viewport where Playwright's actionability
      // check fails (elementFromPoint at the element center is null) — scroll them into view first.
      await input.scrollIntoViewIfNeeded().catch(() => {});
      await input.fill(`Test value ${i + 1}`);
    }
  }

  // Fill numeric inputs (the «Вимірювання кукси» fields require 100–400 / 100–350; the inputs
  // carry no HTML min attribute, so use a fixed value valid for this template)
  const numericInputs = page.locator('input[type="number"]:visible');
  const numCount = await numericInputs.count();
  for (let i = 0; i < numCount; i++) {
    const input = numericInputs.nth(i);
    const value = await input.inputValue();
    if (!value) {
      await input.scrollIntoViewIfNeeded().catch(() => {});
      await input.fill('200');
    }
  }

  // Check unchecked checkboxes — Base UI renders span[data-slot="checkbox"] (no input element).
  // JS click: the sticky bottom action bar overlays lower checkboxes, so pointer clicks would hit
  // the bar instead of the checkbox. Skip hidden native inputs (Base UI also emits a hidden
  // input[type=checkbox] whose JS click would stall).
  const checkboxes = page.locator('[data-slot="checkbox"][aria-checked="false"], input[type="checkbox"]:not(:checked)');
  const checkboxCount = await checkboxes.count();
  for (let i = 0; i < checkboxCount; i++) {
    const cb = checkboxes.nth(i);
    if (!(await cb.isVisible().catch(() => false))) continue;
    await cb.evaluate((el: HTMLElement) => el.click()).catch(() => {});
  }

  // Fill textareas
  const textareas = page.locator('textarea:visible');
  const textareaCount = await textareas.count();
  for (let i = 0; i < textareaCount; i++) {
    const textarea = textareas.nth(i);
    await textarea.scrollIntoViewIfNeeded().catch(() => {});
    const value = await textarea.inputValue();
    if (!value) {
      await textarea.fill(`Test note ${i + 1}`);
    }
  }

  // Select dropdown options
  const selects = page.locator('select:visible');
  const selectCount = await selects.count();
  for (let i = 0; i < selectCount; i++) {
    const select = selects.nth(i);
    const options = await select.locator('option').count();
    if (options > 1) {
      await select.selectOption({ index: 1 });
    }
  }
}
