import { test, expect, Page } from '@playwright/test';
import { mkdirSync, existsSync, writeFileSync, appendFileSync } from 'fs';

const CONFIG = {
  screenshotDir: 'screenshots/prosthetics-workflow',
  bugReportFile: 'test-results/prosthetics-workflow-bugs.json',
  logFile: 'test-results/prosthetics-workflow-log.txt',
  maxWizardSteps: 50,
  baseUrl: 'http://localhost:5173',
};

interface BugReport {
  id: string;
  step: number;
  phase: string;
  severity: 'Critical' | 'Major' | 'Minor';
  description: string;
  expected: string;
  actual: string;
  screenshot?: string;
}

const bugs: BugReport[] = [];
let stepCounter = 0;
let currentPhase = '';

function log(msg: string) {
  const ts = new Date().toISOString();
  const line = `[${ts}] ${msg}`;
  console.log(line);
  if (!existsSync('test-results')) mkdirSync('test-results', { recursive: true });
  try { appendFileSync(CONFIG.logFile, line + '\n'); } catch { writeFileSync(CONFIG.logFile, line + '\n'); }
}

function logStep(action: string, details?: string) {
  stepCounter++;
  log(`STEP ${stepCounter} [${currentPhase}]: ${action}${details ? ' | ' + details : ''}`);
}

function reportBug(severity: 'Critical' | 'Major' | 'Minor', desc: string, expected: string, actual: string, screenshot?: string) {
  bugs.push({ id: `BUG-${String(bugs.length + 1).padStart(3, '0')}`, step: stepCounter, phase: currentPhase, severity, description: desc, expected, actual, screenshot });
  log(`BUG (${severity}): ${desc}`);
}

async function screenshot(page: Page, name: string): Promise<string> {
  if (!existsSync(CONFIG.screenshotDir)) mkdirSync(CONFIG.screenshotDir, { recursive: true });
  const file = `${CONFIG.screenshotDir}/${String(stepCounter).padStart(3, '0')}-${name}.png`;
  await page.screenshot({ path: file });
  return file;
}

test.describe('Prosthetics Workflow Verification', () => {
  test.beforeAll(async () => {
    if (!existsSync('test-results')) mkdirSync('test-results', { recursive: true });
    writeFileSync(CONFIG.logFile, `=== Prosthetics Workflow Test - ${new Date().toISOString()} ===\n\n`);
  });

  test.afterAll(async () => {
    writeFileSync(CONFIG.bugReportFile, JSON.stringify(bugs, null, 2));
    log(`\n=== COMPLETED: ${stepCounter} steps, ${bugs.length} bugs ===`);
    log(`  Critical: ${bugs.filter(b => b.severity === 'Critical').length}`);
    log(`  Major: ${bugs.filter(b => b.severity === 'Major').length}`);
    log(`  Minor: ${bugs.filter(b => b.severity === 'Minor').length}`);
  });

  test('Full prosthetics workflow: Dashboard → Patient → Order → Template → Wizard → Quality Gate → Done', async ({ page, request }) => {
    test.setTimeout(300000);
    
    // ===== QUICK LOGIN =====
    // Resilient: with the prosthetist storage state the app is already authenticated
    // (a fresh /login visit redirects to /select), otherwise log in via the form.
    logStep('Quick login');
    await page.goto(`${CONFIG.baseUrl}/prosthetics`);
    if (await page.locator('#login').isVisible({ timeout: 5000 }).catch(() => false)) {
      await page.locator('#login').fill('prosthetist1');
      await page.locator('#password').fill('doctor123');
      await page.getByRole('button', { name: /Увійти/i }).first().click();
      await page.waitForURL('**/prosthetics**', { timeout: 15000 });
    }
    await screenshot(page, '01-logged-in');
    log('✓ Logged in');

    // ===== PHASE 1: DASHBOARD (Screen 2) =====
    currentPhase = 'DASHBOARD';
    log(`\n--- ${currentPhase} ---`);

    await test.step('Verify Dashboard (Spec 2.2)', async () => {
      logStep('Verify dashboard elements');
      const newProcessBtn = page.getByRole('button', { name: /Новий процес/i }).or(page.getByText(/Новий процес/i)).first();
      await expect(newProcessBtn).toBeVisible({ timeout: 10000 });
      log('✓ New Process button visible');
      await screenshot(page, '02-dashboard');
    });

    // ===== PHASE 2: PATIENT SELECTION (Screen 3) =====
    currentPhase = 'PATIENT SELECTION';
    log(`\n--- ${currentPhase} ---`);

    await test.step('Navigate to patient selection (Spec 2.3.1)', async () => {
      logStep('Click New Process');
      const btn = page.getByRole('button', { name: /Новий процес/i }).or(page.getByText(/Новий процес/i)).first();
      await btn.click();
      await page.waitForURL('**/select-patient**', { timeout: 10000 });
      await screenshot(page, '03-patient-selection');
      log('✓ On patient selection screen');
    });

    await test.step('Search and select patient (Spec 2.3.1)', async () => {
      logStep('Search for Сніжко');
      const searchInput = page.getByPlaceholder(/Пошук|ПІБ|ID/i).or(page.locator('input[type="text"], input[type="search"]').first());
      // Deterministic: the debounced search fires one GET — await its
      // round-trip, then the first rendered row (count() is not auto-waiting).
      const patientsResp = page.waitForResponse(
        (r) => r.request().method() === 'GET' && r.url().includes('/prosthesis-manufacturing/patients'),
        { timeout: 10000 },
      ).catch(() => {});
      await searchInput.first().fill('Сніжко');
      await patientsResp;
      await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 10000 }).catch(() => {});
      
      const rows = page.locator('tbody tr');
      const count = await rows.count();
      log(`  Found ${count} patients`);
      
      if (count > 0) {
        await rows.first().getByRole('button', { name: /Обрати/i }).click();
        log('✓ Patient selected');
      } else {
        reportBug('Critical', 'No patients found', 'Should find patients in mock data', 'Zero results', await screenshot(page, 'bug-no-patients'));
      }
    });

    // ===== PHASE 3: ORDER SELECTION (Screen 4) =====
    currentPhase = 'ORDER SELECTION';
    log(`\n--- ${currentPhase} ---`);

    await test.step('Select order (Spec 2.3.2)', async () => {
      logStep('Select order');
      await page.waitForURL('**/select-order**', { timeout: 10000 }).catch(() => {});
      // The orders fetch fires on navigation — await its round-trip, then the
      // first rendered row (count() is not auto-waiting).
      const ordersResp = page.waitForResponse(
        (r) => r.request().method() === 'GET' && r.url().includes('/prosthesis-manufacturing/orders'),
        { timeout: 10000 },
      ).catch(() => {});
      await ordersResp;
      await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 10000 }).catch(() => {});
      
      const rows = page.locator('tbody tr');
      const count = await rows.count();
      log(`  Found ${count} orders`);
      
      if (count > 0) {
        await rows.first().getByRole('button', { name: /Обрати/i }).click();
        log('✓ Order selected');
      } else {
        reportBug('Critical', 'No orders found', 'Should find orders for patient', 'Zero results', await screenshot(page, 'bug-no-orders'));
      }
      // Current implementation routes through the order review screen — confirm with «Старт»
      await page.waitForURL('**/review-order**', { timeout: 10000 }).catch(() => {});
      const startBtn = page.getByRole('button', { name: /Старт/i });
      if (await startBtn.isVisible({ timeout: 5000 }).catch(() => false)) {
        await startBtn.click();
        log('✓ Order review confirmed («Старт»)');
      }
    });

    // ===== PHASE 4: TEMPLATE SELECTION (Screen 6) =====
    currentPhase = 'TEMPLATE SELECTION';
    log(`\n--- ${currentPhase} ---`);

    await test.step('Select template (Spec 2.3.4)', async () => {
      logStep('Select template');
      await page.waitForURL('**/select-template**', { timeout: 10000 }).catch(() => {});
      
      // Click template card (the tolerant isVisible checks below are the
      // condition-based waits — no sleep needed after navigation).
      const card = page.locator('[class*="cursor-pointer"]').filter({ hasText: /TP-UL|TP-LL|Шаблон/i }).first();
      if (await card.isVisible({ timeout: 5000 }).catch(() => false)) {
        await card.click();
        log('✓ Template card clicked');
      }
      
      // Click Обрати
      const selectBtn = page.getByRole('button', { name: /Обрати/i }).first();
      if (await selectBtn.isVisible({ timeout: 5000 }).catch(() => false)) {
        await selectBtn.click();
        log('✓ Обрати clicked - process created');
      } else {
        reportBug('Major', 'No Обрати button found', 'Should have Обрати button', 'Button not found');
      }
      // The process wizard opens on «Обрати» — wait for the URL, not time.
      await page.waitForURL('**/process/**', { timeout: 15000 }).catch(() => {});
    });

    // ===== PHASE 5: WIZARD EXECUTION (Screen 8) =====
    currentPhase = 'WIZARD EXECUTION';
    log(`\n--- ${currentPhase} ---`);

    await test.step('Execute wizard steps (Spec 2.4.2)', async () => {
      logStep('Execute wizard');
      
      let completed = 0;
      let qualityGate = false;
      let done = false;
      
      for (let i = 0; i < CONFIG.maxWizardSteps; i++) {
        // Check for quality gate
        const qgBtn = page.getByRole('button', { name: /Прийнято \(Pass\)|Схвалити|Пройдено/i }).first();
        if (await qgBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
          log('Quality Gate detected');
          qualityGate = true;
          break;
        }
        
        // Check for completion
        if (page.url().includes('/done') || await page.getByText(/завершено|Completed/i).isVisible({ timeout: 1000 }).catch(() => false)) {
          log('Process completed');
          done = true;
          break;
        }
        
        // Fill required fields
        await fillFields(page);
        
        // Try to complete step
        const completeBtn = page.getByRole('button', { name: /Готово|Завершити процес|Завершити крок/i }).first();
        if (await completeBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
          const enabled = await completeBtn.isEnabled({ timeout: 1000 }).catch(() => false);
          // Deterministic: the step-complete POST is the signal the wizard
          // advanced — register the waiter before the click (soft: a stuck
          // wizard must not hard-fail this self-reporting spec).
          const stepResp = page.waitForResponse(
            (r) => r.request().method() === 'POST' && r.url().includes('/step-executions/') && r.url().endsWith('/complete'),
            { timeout: 1500 },
          ).catch(() => {});
          if (enabled) {
            await completeBtn.click();
            await stepResp;
            completed++;
            log(`  Step ${i + 1} completed (${completed} total)`);
          } else {
            await completeBtn.click({ force: true });
            await stepResp;
            completed++;
          }
        } else {
          // Step transition in progress — the bounded loop retries via a
          // condition wait instead of a throttle.
          await completeBtn.waitFor({ state: 'visible', timeout: 1000 }).catch(() => {});
        }
      }
      
      log(`✓ Completed ${completed} wizard steps`);
      await screenshot(page, '04-wizard-complete');
    });

    // ===== PHASE 6: QUALITY GATE (Screen 9) =====
    currentPhase = 'QUALITY GATE';
    log(`\n--- ${currentPhase} ---`);

    await test.step('Handle Quality Gate (Spec 2.5.1)', async () => {
      logStep('Pass quality gate');
      
      // Check all criteria
      const checkboxes = page.locator('input[type="checkbox"]');
      const cbCount = await checkboxes.count();
      for (let i = 0; i < cbCount; i++) {
        await checkboxes.nth(i).check({ force: true });
      }
      log(`  Checked ${cbCount} criteria`);
      
      // The gate requires a PROSTHETICS_ADMINISTRATOR decision; the wizard runs as prosthetist1,
      // so pass the gate via API with an admin token, then continue the wizard.
      const instanceId = page.url().match(/\/process\/([0-9a-f-]+)\//)?.[1];
      if (!instanceId) {
        reportBug('Critical', 'Cannot resolve instance id from wizard URL', 'Wizard URL should contain instance id', page.url());
      } else {
        const adminLogin = await request.post('/api/auth/login', {
          data: { login: 'prosthetics_admin1', password: 'doctor123' },
        });
        if (!adminLogin.ok()) {
          reportBug('Critical', 'Admin API login failed', 'Admin login should succeed', `HTTP ${adminLogin.status()}`);
        } else {
          const adminToken = (await adminLogin.json()).token;
          const headers = { Authorization: `Bearer ${adminToken}` };
          const snapRes = await request.get(`/api/prosthesis-manufacturing/instances/${instanceId}/snapshot`, { headers });
          const snapshot = await snapRes.json();
          const gate = (snapshot.stages ?? []).find((s: any) => s.gate)?.gate;
          if (!gate) {
            reportBug('Critical', 'No quality gate in template snapshot', 'Gated stage should exist', 'Gate not found');
          } else {
            const criteriaIds = (gate.criteria ?? []).map((c: any) => c.id);
            const passRes = await request.post(
              `/api/prosthesis-manufacturing/instances/${instanceId}/gates/${gate.id}/decision`,
              { headers, data: { decision: 'PASS', criteriaConfirmed: criteriaIds, comment: '' } },
            );
            if (!passRes.ok()) {
              reportBug('Major', 'Gate PASS via API failed', 'Admin PASS should advance the instance', `HTTP ${passRes.status()}: ${await passRes.text()}`);
            } else {
              log('✓ Quality gate passed by administrator (API)');
              await page.reload();
              // The post-gate wizard (or /done) renders after reload — wait for
              // either signal instead of a sleep.
              await page.waitForURL('**/done**', { timeout: 5000 }).catch(() => {});
              await page.getByRole('button', { name: /Готово|Завершити процес|Завершити крок/i }).first()
                .waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
              for (let i = 0; i < 6; i++) {
                if (page.url().includes('/done')) break;
                await fillFields(page);
                const continueBtn = page.getByRole('button', { name: /Готово|Завершити процес|Завершити крок/i }).first();
                if (await continueBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
                  const stepResp = page.waitForResponse(
                    (r) => r.request().method() === 'POST' && r.url().includes('/step-executions/') && r.url().endsWith('/complete'),
                    { timeout: 1500 },
                  ).catch(() => {});
                  await continueBtn.click({ force: true });
                  await stepResp;
                  log(`  Post-gate step ${i + 1} completed`);
                } else {
                  await continueBtn.waitFor({ state: 'visible', timeout: 1000 }).catch(() => {});
                }
              }
              await screenshot(page, '05-post-gate-continue');
            }
          }
        }
      }
    });

    // ===== PHASE 7: COMPLETION =====
    currentPhase = 'COMPLETION';
    log(`\n--- ${currentPhase} ---`);

    await test.step('Verify completion (Spec 2.6.1)', async () => {
      logStep('Verify completion');
      // The wizard either lands on /done or /failed — wait for the final
      // state instead of a sleep.
      await page.waitForURL(/\/prosthetics\/process\/[0-9a-f-]+\/(done|failed)/, { timeout: 10000 }).catch(() => {});
      await screenshot(page, '05-final-state');
      
      const url = page.url();
      log(`  Final URL: ${url}`);
      
      if (url.includes('/done')) {
        log('✓ Process completed successfully!');
      } else if (url.includes('/failed')) {
        reportBug('Major', 'Process failed', 'Should complete successfully', 'Ended in failed state');
      } else {
        log(`⚠ Final state: ${url}`);
      }
    });

    // ===== SUMMARY =====
    currentPhase = 'SUMMARY';
    log(`\n--- TEST COMPLETE ---`);
    logStep(`Total: ${stepCounter} steps, ${bugs.length} bugs`);
  });
});

async function fillFields(page: Page) {
  // Toggle signature capture (SIGNATURE_CAPTURE element)
  const sig = page.locator('button:has-text("електронного підпису")').first();
  if (await sig.isVisible().catch(() => false)) {
    await sig.click();
  }

  // Fill text inputs
  const texts = page.locator('input[type="text"]:visible, input:not([type]):visible');
  for (let i = 0; i < await texts.count(); i++) {
    const val = await texts.nth(i).inputValue();
    if (!val) await texts.nth(i).fill(`Value ${i}`);
  }
  
  // Fill numbers
  const nums = page.locator('input[type="number"]:visible');
  for (let i = 0; i < await nums.count(); i++) {
    const val = await nums.nth(i).inputValue();
    if (!val) await nums.nth(i).fill('10');
  }
  
  // Check checkboxes (Base UI renders button[data-slot="checkbox"], not inputs)
  const cbs = page.locator('[data-slot="checkbox"][aria-checked="false"], input[type="checkbox"]:not(:checked)');
  for (let i = 0; i < await cbs.count(); i++) {
    await cbs.nth(i).click({ force: true }).catch(() => {});
  }
  
  // Fill textareas
  const tas = page.locator('textarea:visible');
  for (let i = 0; i < await tas.count(); i++) {
    const val = await tas.nth(i).inputValue();
    if (!val) await tas.nth(i).fill(`Note ${i}`);
  }
  
  // Select dropdowns
  const selects = page.locator('select:visible');
  for (let i = 0; i < await selects.count(); i++) {
    const opts = await selects.nth(i).locator('option').count();
    if (opts > 1) await selects.nth(i).selectOption({ index: 1 });
  }
}
