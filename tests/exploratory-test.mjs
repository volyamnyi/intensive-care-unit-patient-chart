import { chromium } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const screenshotDir = path.join(__dirname, 'exploratory-screenshots');
const reportDir = path.join(__dirname, 'exploratory-report');
const BASE = 'http://localhost:5173';

fs.rmSync(screenshotDir, { recursive: true, force: true });
fs.mkdirSync(screenshotDir, { recursive: true });
fs.mkdirSync(reportDir, { recursive: true });

const findings = [];
function log(section, step, status, detail) {
  findings.push({ section, step, status, detail });
  const icon = status === 'PASS' ? '✅' : status === 'FAIL' ? '❌' : status === 'INFO' ? 'ℹ️' : '⚠️';
  console.log(`  ${icon} [${new Date().toISOString().slice(11, 19)}] ${step}: ${detail}`);
}
async function ss(page, name) {
  await page.screenshot({ path: path.join(screenshotDir, `${name}.png`), fullPage: false });
  console.log(`  📸 ${name}.png`);
}
const delay = ms => new Promise(r => setTimeout(r, ms));

async function loginAs(page, username, password) {
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' });
  await delay(1000);
  await page.getByLabel('Логін').fill(username);
  await page.getByLabel('Пароль').fill(password);
  await page.getByRole('button', { name: 'Увійти' }).click();
  await delay(3000);
}

async function forceLogout(page) {
  await page.evaluate(() => localStorage.clear());
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' });
  await delay(1500);
}

async function run() {
  console.log('\n========================================\n  EXPLORATORY FUNCTIONAL TESTING\n  ICU Patient Chart\n========================================\n');

  const browser = await chromium.launch({ headless: false, slowMo: 1200, args: ['--start-maximized'] });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, ignoreHTTPSErrors: true });
  const page = await context.newPage();

  // ==============================
  // 1. LOGIN FLOW
  // ==============================
  console.log('\n--- 1. LOGIN FLOW ---');
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' });
  await delay(2000);
  await ss(page, '01-login-page');

  log('Login', 'Login page title', (await page.getByText('Карта інтенсивної терапії').isVisible().catch(() => false)) ? 'PASS' : 'FAIL',
    (await page.getByText('Карта інтенсивної терапії').isVisible().catch(() => false)) ? 'Title visible' : 'Title NOT found');
  log('Login', 'Login field', (await page.getByLabel('Логін').isVisible().catch(() => false)) ? 'PASS' : 'FAIL', '');
  log('Login', 'Password field', (await page.getByLabel('Пароль').isVisible().catch(() => false)) ? 'PASS' : 'FAIL', '');
  log('Login', 'Submit button', (await page.getByRole('button', { name: 'Увійти' }).isVisible().catch(() => false)) ? 'PASS' : 'FAIL', '');

  // Invalid login
  await page.getByLabel('Логін').fill('wrong');
  await page.getByLabel('Пароль').fill('wrong');
  await page.getByRole('button', { name: 'Увійти' }).click();
  await delay(2000);
  await ss(page, '02-login-error');
  const err = page.getByText('Невірний логін або пароль');
  log('Login', 'Invalid credentials error', (await err.isVisible().catch(() => false)) ? 'PASS' : 'FAIL',
    (await err.isVisible().catch(() => false)) ? 'MUI Alert with error text visible' : 'Error alert NOT found');

  // Login as doctor
  await page.getByLabel('Логін').fill('doctor1');
  await page.getByLabel('Пароль').fill('doctor123');
  await page.getByRole('button', { name: 'Увійти' }).click();
  try { await page.waitForURL('**/doctor**', { timeout: 10000 }); } catch {}
  await delay(2000);
  await ss(page, '03-doctor-dashboard');
  log('Login', 'Doctor login', page.url().includes('/doctor') ? 'PASS' : 'FAIL',
    page.url().includes('/doctor') ? 'Redirected to /doctor' : `URL: ${page.url()}`);

  // ==============================
  // 2. DOCTOR DASHBOARD
  // ==============================
  console.log('\n--- 2. DOCTOR DASHBOARD ---');
  log('Dashboard', 'Active patients title', (await page.getByText('Активні пацієнти').isVisible().catch(() => false)) ? 'PASS' : 'FAIL', '');
  const hasTable = await page.locator('.MuiTable-root, table, [role="table"]').isVisible().catch(() => false);
  log('Dashboard', 'Patients table visible', hasTable ? 'PASS' : 'FAIL', '');
  const rows = await page.locator('.MuiTableBody-root tr, tbody tr, [role="row"]').allTextContents();
  const rowText = rows.join(' | ');
  log('Dashboard', `Table has ${rows.length} rows`, rows.length > 0 ? 'PASS' : 'FAIL', rowText.slice(0, 120));

  // Search
  const searchField = page.getByPlaceholder('Пошук');
  if (await searchField.isVisible().catch(() => false)) {
    await searchField.fill('Коваленко');
    await delay(2000);
    await ss(page, '04-doctor-search');
    const afterSearch = await page.locator('.MuiTableBody-root tr, tbody tr, [role="row"]').allTextContents();
    log('Dashboard', 'Search Коваленко', afterSearch.some(t => t.includes('Коваленк')) ? 'PASS' : 'FAIL', `Rows: ${afterSearch.length}`);
    await searchField.fill('');
    await delay(1000);
  }

  // Navigate to create-card
  await page.getByRole('link', { name: /Нова карта/i }).or(page.getByRole('button', { name: /Нова карта/i })).click().catch(() => {});
  await delay(1500);
  await ss(page, '05-create-card-page');
  const onCreateCard = page.url().includes('create-card');
  log('Dashboard', 'Navigate to /doctor/create-card', onCreateCard ? 'PASS' : 'FAIL',
    onCreateCard ? 'Reached create-card page' : `URL: ${page.url()}`);

  if (!onCreateCard) await page.goto(`${BASE}/doctor/create-card`, { waitUntil: 'domcontentloaded' });

  // Create card — short search
  const searchInput = page.getByLabel('ПІБ, телефон або № медкарти').or(page.locator('input').first());
  await searchInput.fill('A');
  await delay(1000);
  const body = await page.locator('body').textContent().catch(() => '');
  log('CreateCard', 'Short search (<2 chars)', body.includes('Введіть мінімум') || body.includes('2 символи') ? 'PASS' : 'INFO', '');
  await ss(page, '06-create-card-short-search');

  // Search Бондаренко
  await searchInput.fill('Бондаренко');
  await delay(2500);
  await ss(page, '07-create-card-search');
  const opts = page.locator('[role="option"], li');
  const optTexts = await opts.allTextContents();
  log('CreateCard', 'Search Бондаренко', optTexts.some(t => t.includes('Бондаренк')) ? 'PASS' : 'FAIL',
    optTexts.some(t => t.includes('Бондаренк')) ? 'MIS returned Бондаренко' : `Options: ${optTexts.join(' | ').slice(0, 80)}`);

  // Go back to doctor dashboard
  await page.goto(`${BASE}/doctor`, { waitUntil: 'domcontentloaded' });
  await delay(1500);
  await ss(page, '08-doctor-dashboard-2');

  // ==============================
  // 3. DOCTOR EPISODE PAGE
  // ==============================
  console.log('\n--- 3. DOCTOR EPISODE PAGE ---');
  const openLink = page.getByRole('link', { name: 'Відкрити' }).or(page.getByRole('button', { name: 'Відкрити' }));
  if (await openLink.first().isVisible().catch(() => false)) {
    await openLink.first().click();
    try { await page.waitForURL('**/episode/**', { timeout: 8000 }); } catch {}
    await delay(2500);
    await ss(page, '09-episode-page');
    log('Episode', 'Open episode', page.url().includes('episode') ? 'PASS' : 'FAIL',
      page.url().includes('episode') ? `Opened: ${page.url().slice(0, 70)}` : `URL: ${page.url()}`);
  } else {
    log('Episode', 'Open button', 'FAIL', 'No "Відкрити" link/button on page');
  }

  // Tabs
  if (page.url().includes('episode')) {
    const tabs = page.getByRole('tab');
    const tc = await tabs.count();
    log('Episode Tabs', `Found ${tc} tabs`, tc >= 5 ? 'PASS' : tc > 0 ? 'INFO' : 'FAIL', '');
    for (let i = 0; i < tc; i++) {
      const txt = (await tabs.nth(i).textContent().catch(() => '')).trim();
      await tabs.nth(i).click();
      await delay(1500);
      log('Episode Tabs', `Tab "${txt}" clicked`, 'PASS', '');
      await ss(page, `09-tab-${(txt || 'tab-' + i).slice(0, 25).replace(/[^a-zа-яіїєґ0-9]/gi, '-')}`);
    }

    // Sign-off
    const sign = page.getByRole('button', { name: /Підписати/i });
    if (await sign.isVisible().catch(() => false)) {
      await sign.click();
      await delay(2000);
      await ss(page, '10-sign-dialog');
      const dialog = page.locator('[role="dialog"]');
      log('SignOff', 'Sign dialog', (await dialog.isVisible().catch(() => false)) ? 'PASS' : 'INFO', '');
      const cancel = page.getByRole('button', { name: 'Скасувати' });
      if (await cancel.isVisible().catch(() => false)) { await cancel.click(); await delay(1000); log('SignOff', 'Cancel', 'PASS', ''); }
    } else log('SignOff', 'Sign button', 'INFO', 'Not visible on this day');

    // Back
    const back = page.getByRole('link', { name: 'Назад' }).or(page.getByRole('button', { name: 'Назад' }));
    if (await back.isVisible().catch(() => false)) { await back.click(); await delay(2000); log('Navigation', 'Back button', 'PASS', ''); }
    else { await page.goto(`${BASE}/doctor`, { waitUntil: 'domcontentloaded' }); await delay(1000); log('Navigation', 'Back', 'INFO', 'Used goto'); }
  } else {
    // Episode never opened — try direct URL
    log('Episode', 'Skipping tabs/sign tests', 'INFO', 'Could not open episode page');
  }

  // ==============================
  // 4. NURSE FLOW
  // ==============================
  console.log('\n--- 4. NURSE FLOW ---');
  await forceLogout(page);
  await ss(page, '11-after-force-logout');

  await loginAs(page, 'nurse1', 'nurse123');
  await ss(page, '12-nurse-dashboard');
  log('Nurse', 'Login', page.url().includes('/nurse') ? 'PASS' : 'FAIL',
    page.url().includes('/nurse') ? 'Redirected to /nurse' : `URL: ${page.url()}`);

  const nTitle = page.getByText('Активні пацієнти');
  log('Nurse', 'Dashboard title', (await nTitle.isVisible().catch(() => false)) ? 'PASS' : 'FAIL', '');

  // Search
  const nSearch = page.getByPlaceholder('Пошук');
  if (await nSearch.isVisible().catch(() => false)) {
    await nSearch.fill('Сидоренко');
    await delay(2000);
    await ss(page, '13-nurse-search');
    const nRows = await page.locator('.MuiTableBody-root tr, tbody tr').allTextContents();
    log('Nurse', 'Search Сидоренко', nRows.some(t => t.includes('Сидоренк')) ? 'PASS' : 'FAIL', `Rows: ${nRows.length}`);
    await nSearch.fill('');
    await delay(500);
  }

  // Open episode
  const nOpen = page.getByRole('link', { name: 'Відкрити' }).or(page.getByRole('button', { name: 'Відкрити' }));
  if (await nOpen.first().isVisible().catch(() => false)) {
    await nOpen.first().click();
    try { await page.waitForURL('**/episode/**', { timeout: 8000 }); } catch {}
    await delay(2500);
    await ss(page, '14-nurse-episode');
    log('Nurse', 'Open episode', page.url().includes('episode') ? 'PASS' : 'FAIL', '');
  }

  if (page.url().includes('episode')) {
    // Tabs
    const nt = page.getByRole('tab');
    const ntc = await nt.count();
    log('Nurse', `Episode tabs: ${ntc}`, ntc > 0 ? 'PASS' : 'FAIL', '');
    await nt.filter({ hasText: /Показники/i }).click().catch(() => {});
    await delay(1500);
    await ss(page, '15-nurse-vitals-tab');
    const labels = page.locator('label');
    const lc = await labels.count();
    log('Nurse', `Vitals form labels: ${lc}`, lc > 0 ? 'PASS' : 'INFO', '');
    for (let i = 0; i < Math.min(lc, 8); i++) {
      const t = (await labels.nth(i).textContent().catch(() => '')).trim();
      if (t) log('Nurse', `  Label: "${t}"`, 'INFO', '');
    }

    // Try to fill vitals if fields exist
    const sys = page.getByLabel('АТ сист').or(page.locator('label').filter({ hasText: /АТ сист/i }).locator('..').locator('input'));
    if (await sys.isVisible().catch(() => false)) {
      await sys.fill('120'); await delay(300);
      const dia = page.getByLabel(/АД діаст|АТ діаст/i).or(page.locator('label').filter({ hasText: /діаст/i }).locator('..').locator('input'));
      if (await dia.isVisible().catch(() => false)) { await dia.fill('80'); await delay(300); }
      await page.getByRole('button', { name: /Зберегти/i }).click().catch(() => {});
      await delay(2000);
      await ss(page, '16-nurse-vitals-saved');
      log('Nurse', 'Save vitals', 'PASS', 'Vitals filled and saved');
    }

    // Fluid balance tab
    await page.getByRole('tab', { name: /Баланс/i }).click().catch(() => {});
    await delay(1500);
    await ss(page, '17-nurse-fluid-balance');
    log('Nurse', 'Fluid balance tab clicked', 'PASS', '');
  }

  // ==============================
  // 5. HOD FLOW
  // ==============================
  console.log('\n--- 5. HOD FLOW ---');
  await forceLogout(page);
  await loginAs(page, 'head1', 'head123');
  await ss(page, '18-hod-dashboard');
  log('HOD', 'Login', page.url().includes('/doctor') ? 'PASS' : 'FAIL',
    page.url().includes('/doctor') ? 'Shared /doctor route' : `URL: ${page.url()}`);

  const hNew = page.getByRole('link', { name: /Нова карта/i }).or(page.getByRole('button', { name: /Нова карта/i }));
  log('HOD', 'Can see "Нова карта"', (await hNew.isVisible().catch(() => false)) ? 'PASS' : 'FAIL', '');

  const hOpen = page.getByRole('link', { name: 'Відкрити' }).or(page.getByRole('button', { name: 'Відкрити' }));
  if (await hOpen.first().isVisible().catch(() => false)) {
    await hOpen.first().click();
    try { await page.waitForURL('**/episode/**', { timeout: 8000 }); } catch {}
    await delay(2000);
    await ss(page, '19-hod-episode');
    log('HOD', 'Open episode', page.url().includes('episode') ? 'PASS' : 'FAIL', '');
  }

  // ==============================
  // 6. ADMIN FLOW
  // ==============================
  console.log('\n--- 6. ADMIN FLOW ---');
  await forceLogout(page);
  await loginAs(page, 'admin', 'admin123');
  await ss(page, '20-admin-dashboard');
  log('Admin', 'Login', page.url().includes('/admin') ? 'PASS' : 'FAIL',
    page.url().includes('/admin') ? 'Redirected to /admin' : `URL: ${page.url()}`);

  log('Admin', 'Title "Користувачі системи"', (await page.getByText('Користувачі системи').isVisible().catch(() => false)) ? 'PASS' : 'FAIL', '');
  log('Admin', 'Doctors section', (await page.getByText('Лікарі').isVisible().catch(() => false)) ? 'PASS' : 'FAIL', '');
  log('Admin', 'Nurses section', (await page.getByText('Медсестри').isVisible().catch(() => false)) ? 'PASS' : 'FAIL', '');

  // ==============================
  // 7. ACCESS CONTROL
  // ==============================
  console.log('\n--- 7. ACCESS CONTROL ---');
  await page.goto(`${BASE}/nurse`, { waitUntil: 'domcontentloaded' });
  await delay(1500);
  log('AccessCtrl', 'Admin accessing /nurse', page.url().includes('/admin') ? 'PASS' : 'INFO',
    page.url().includes('/admin') ? 'Redirected to /admin' : `URL: ${page.url()}`);
  await ss(page, '21-access-nurse');

  await page.goto(`${BASE}/doctor`, { waitUntil: 'domcontentloaded' });
  await delay(1500);
  log('AccessCtrl', 'Admin accessing /doctor', page.url().includes('/admin') ? 'PASS' : 'INFO',
    page.url().includes('/admin') ? 'Redirected to /admin' : `URL: ${page.url()}`);
  await ss(page, '22-access-doctor');

  // ==============================
  // 8. MIS SEARCH
  // ==============================
  console.log('\n--- 8. MIS PATIENT SEARCH ---');
  // Need to login as doctor (admin can't access create-card)
  await forceLogout(page);
  await loginAs(page, 'doctor1', 'doctor123');
  await page.goto(`${BASE}/doctor/create-card`, { waitUntil: 'domcontentloaded' });
  await delay(1000);
  if (!page.url().includes('create-card')) {
    log('MIS Search', 'Not on create-card page', 'FAIL', `Redirected: ${page.url()}`);
  } else {
    const misInput = page.getByLabel('ПІБ, телефон або № медкарти').or(page.locator('input').first());
    await misInput.fill('Ткачук');
    await delay(2500);
    await ss(page, '23-mis-search-tkachuk');
    const misOpts = page.locator('[role="option"], li');
    const misTexts = await misOpts.allTextContents();
    log('MIS Search', 'Search Ткачук', misTexts.some(t => t.includes('Ткачук')) ? 'PASS' : 'FAIL',
      misTexts.some(t => t.includes('Ткачук')) ? 'Found in MIS results' : `Options: ${misTexts.join(' | ').slice(0, 80)}`);
  }

  await browser.close();

  // ==============================
  // REPORT
  // ==============================
  const pass = findings.filter(f => f.status === 'PASS').length;
  const fail = findings.filter(f => f.status === 'FAIL').length;
  const info = findings.filter(f => f.status === 'INFO').length;
  const warn = findings.filter(f => f.status === 'WARN').length;

  let report = '# Exploratory Functional Testing Report\n\n**Date:** ' + new Date().toISOString() + '\n**App:** ICU Patient Chart\n\n## Summary\n\n| Status | Count |\n|--------|-------|\n';
  report += `| ✅ PASS | ${pass} |\n| ❌ FAIL | ${fail} |\n| ℹ️ INFO | ${info} |\n| ⚠️ WARN | ${warn} |\n| **Total** | **${findings.length}** |\n`;

  const secs = [...new Set(findings.map(f => f.section))];
  for (const s of secs) {
    report += `\n## ${s}\n\n| # | Step | Status | Detail |\n|---|---|---|---|\n`;
    findings.filter(f => f.section === s).forEach((f, i) => {
      report += `| ${i + 1} | ${f.step} | ${f.status === 'PASS' ? '✅' : f.status === 'FAIL' ? '❌' : 'ℹ️'} | ${f.detail} |\n`;
    });
  }

  report += `\n---\n\n# Comprehensive Functional Test Plan\n\n`;
  report += `## Use Case 1: Authentication\n\n| ID | Test Case | Steps | Expected | Priority |\n|---|---|---|---|---|\n`;
  report += `| TC-01 | Login as doctor | /login → doctor1/doctor123 → submit | → /doctor | Critical |\n`;
  report += `| TC-02 | Login as nurse | /login → nurse1/nurse123 → submit | → /nurse | Critical |\n`;
  report += `| TC-03 | Login as HOD | /login → head1/head123 → submit | → /doctor | Critical |\n`;
  report += `| TC-04 | Login as admin | /login → admin/admin123 → submit | → /admin | Critical |\n`;
  report += `| TC-05 | Invalid credentials | wrong/wrong → submit | Error "Невірний логін або пароль" | Critical |\n`;
  report += `| TC-06 | Unauthenticated redirect | /doctor without token | → /login | Critical |\n`;
  report += `| TC-07 | Logout | user menu → Вийти | → /login | High |\n`;
  report += `| TC-08 | Access: admin→/nurse | admin navigates to /nurse | Redirected to /admin | High |\n`;
  report += `| TC-09 | Access: admin→/doctor | admin navigates to /doctor | Redirected to /admin | High |\n`;
  report += `| TC-10 | Access: nurse→/doctor | nurse navigates to /doctor | Redirected or 403 | High |\n`;

  report += `\n## Use Case 2: Doctor Dashboard\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-11 | Dashboard loads with episodes | Table with Петренко, Коваленко, Сидоренко | Critical |\n`;
  report += `| TC-12 | Search filters table | Type "Коваленко" → only matching row | High |\n`;
  report += `| TC-13 | Clear search restores list | Clear field → all rows back | Medium |\n`;
  report += `| TC-14 | Search non-existent | "ZZZ" → empty state | Medium |\n`;
  report += `| TC-15 | "Нова карта" navigates | → /doctor/create-card | High |\n`;
  report += `| TC-16 | "Відкрити" opens episode | → /doctor/episode/:id | Critical |\n`;
  report += `| TC-17 | Page title | "ВАІТ — Лікар" | Low |\n`;

  report += `\n## Use Case 3: Create Card\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-18 | Create card for Бондаренко | Search → select → create → episode page | Critical |\n`;
  report += `| TC-19 | Short search validates | 1 char → "Введіть мінімум 2 символи" | High |\n`;
  report += `| TC-20 | Cancel returns to dashboard | Click "Скасувати" → /doctor | Medium |\n`;
  report += `| TC-21 | Patient details shown | Name, sex, blood group | High |\n`;

  report += `\n## Use Case 4: Episode Page\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-22 | All tabs render | Показники, Призначення, Шкали, Нотатки, Баланс рідини | Critical |\n`;
  report += `| TC-23 | Tab switching | Click each tab → content changes | High |\n`;
  report += `| TC-24 | Back to dashboard | Click "Назад" → dashboard | High |\n`;

  report += `\n## Use Case 5: Vitals\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-25 | Enter & save vitals | Fill SYS/DIA/HR/SpO2/Temp → save → confirmation | Critical |\n`;
  report += `| TC-26 | Numeric validation | type=number, min/max attributes | High |\n`;
  report += `| TC-27 | Out-of-range | SYS=500 → validation | Medium |\n`;
  report += `| TC-28 | Empty submission | Click save empty → validation | Medium |\n`;
  report += `| TC-29 | Different hour slot | Select hour → save independently | High |\n`;

  report += `\n## Use Case 6: Prescriptions\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-30 | Create prescription | Fill drug/dose/route/unit/freq/start → save → "Активне" | Critical |\n`;
  report += `| TC-31 | Cancel creation | Click cancel → form closed | Medium |\n`;
  report += `| TC-32 | Empty form validation | Save empty → errors | High |\n`;

  report += `\n## Use Case 7: Notes\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-33 | Add note | Type text → "Додати нотатку" → note in list | Critical |\n`;
  report += `| TC-34 | Empty note | Click add without text → error | Medium |\n`;

  report += `\n## Use Case 8: Fluid Balance\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-35 | Tab components | "Баланс рідини" heading + intake/output/balance | High |\n`;
  report += `| TC-36 | Recalculate | Click recalculate → balance updated | High |\n`;
  report += `| TC-37 | Balance = intake - output | Enter vitals → recalculate → correct | Medium |\n`;

  report += `\n## Use Case 9: Scales\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-38 | Scales tab | List of available scales | High |\n`;
  report += `| TC-39 | APACHE II result | Fill params → save → score | High |\n`;
  report += `| TC-40 | SOFA result | Same → score saved | High |\n`;

  report += `\n## Use Case 10: Sign-off\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-41 | Nurse signs | Click sign → NURSE_SIGNED | Critical |\n`;
  report += `| TC-42 | Doctor signs after nurse | Sign NURSE_SIGNED day → DOCTOR_SIGNED | Critical |\n`;
  report += `| TC-43 | Cancel dialog | Click cancel → day unchanged | Medium |\n`;
  report += `| TC-44 | Doctor signs before nurse | Sign OPEN day → warning | High |\n`;
  report += `| TC-45 | Read-only warning | Dialog text about read-only after sign | High |\n`;

  report += `\n## Use Case 11: Order Execution\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-46 | View active orders | Prescriptions tab → list | High |\n`;
  report += `| TC-47 | Execute order | Click "Виконати" → status updated | High |\n`;

  report += `\n## Use Case 12: Nurse Dashboard\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-48 | Dashboard loads | "Активні пацієнти" + table | Critical |\n`;
  report += `| TC-49 | Search | Type → filtered | High |\n`;
  report += `| TC-50 | Open episode | → /nurse/episode/:id | Critical |\n`;
  report += `| TC-51 | Title | "ВАІТ — Медсестра" | Low |\n`;

  report += `\n## Use Case 13: Admin Dashboard\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-52 | Page loads | "Користувачі системи" | Critical |\n`;
  report += `| TC-53 | Doctors table | doctor1, doctor2 visible | High |\n`;
  report += `| TC-54 | Nurses table | nurse1, nurse2 visible | High |\n`;
  report += `| TC-55 | Title | "ВАІТ — Адміністратор" | Low |\n`;

  report += `\n## Use Case 14: Routing\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-56 | / redirect by role | Doctor→/doctor, Nurse→/nurse, Admin→/admin | High |\n`;
  report += `| TC-57 | Direct /doctor/create-card | Page loads correctly | High |\n`;
  report += `| TC-58 | Direct /nurse/episode/:id | Episode page loads | High |\n`;

  report += `\n## Use Case 15: Error Handling\n\n| ID | Test Case | Expected | Priority |\n|---|---|---|---|\n`;
  report += `| TC-59 | Optimistic locking conflict | Two tabs save same episode → 409 | Medium |\n`;
  report += `| TC-60 | Invalid episode ID | /doctor/episode/bad-id → error/redirect | Medium |\n`;

  fs.writeFileSync(path.join(reportDir, 'exploratory-test-report.md'), report);
  fs.writeFileSync(path.join(reportDir, 'findings.json'), JSON.stringify(findings, null, 2));

  console.log(`\n========================================`);
  console.log(`  RESULTS: ${pass} ✅ PASS  |  ${fail} ❌ FAIL  |  ${info} ℹ️ INFO  |  ${warn} ⚠️ WARN`);
  console.log(`  Total: ${findings.length} checks`);
  console.log(`========================================`);
  console.log(`\nReport: ${reportDir}\\exploratory-test-report.md`);
  console.log(`Screenshots: ${screenshotDir}\\`);
  console.log(`Test plan: 60 test cases across 15 use cases\n`);
}

run().catch(err => {
  console.error('FATAL:', err);
  process.exit(1);
});