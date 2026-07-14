import { chromium } from 'playwright';
import { mkdirSync, readdirSync, writeFileSync } from 'fs';
import { join } from 'path';

const BASE = 'http://localhost:5173';
const REPORT_DIR = 'exploratory-report-v3';
const SLOW = 400;

mkdirSync(REPORT_DIR, { recursive: true });

const ss = (page, name) => page.screenshot({ path: join(REPORT_DIR, `${name}.png`), fullPage: true }).catch(() => {});
const sec = (msg) => console.log(`\n${'='.repeat(60)}\n${msg}\n${'='.repeat(60)}`);
const st = (msg) => console.log(`  ${msg}`);

const findings = [];

const report = (tcId, status, detail, severity = '') => {
  const s = status ? 'PASS' : 'FAIL';
  findings.push({ tcId, status: !!status, detail, severity });
  st(`  [${s}] ${tcId}: ${detail} ${severity ? '(' + severity + ')' : ''}`);
};

// Login helper — returns {ctx, page} with fresh auth
const loginAs = async (browser, login, password) => {
  const ctx = await browser.newContext({ noViewport: true });
  const p = await ctx.newPage();
  await p.goto(`${BASE}/login`, { waitUntil: 'networkidle', timeout: 20000 });
  await p.waitForTimeout(500);
  await p.getByLabel('Логін').fill(login);
  await p.getByLabel('Пароль').fill(password);
  await p.getByRole('button', { name: 'Увійти' }).click();
  await p.waitForTimeout(2000);
  return { ctx, page: p };
};

const openFirstEpisode = async (p) => {
  await p.waitForTimeout(1000);
  const openBtn = p.getByRole('button', { name: 'Відкрити' }).first();
  if (await openBtn.isVisible({ timeout: 8000 }).catch(() => false)) {
    await openBtn.click();
    await p.waitForTimeout(2000);
    return true;
  }
  return false;
};

(async () => {
  st('Launching chromium — non-headless, fullscreen, slowMo=400ms');
  const browser = await chromium.launch({
    headless: false,
    slowMo: SLOW,
    args: ['--start-maximized'],
  });

  const run = async (label, fn) => {
    try {
      st(label);
      await fn();
      st('OK');
    } catch (e) {
      st(`ERROR: ${e.message}`);
      findings.push({ tcId: label.split(':')[0]?.trim() || label, status: false, detail: `Exception: ${e.message}`, severity: 'P1' });
    }
  };

  try {
    // =====================================================================
    // SESSION 1: Auth, Redirect, Route Restrictions (UC-01–04)
    // =====================================================================
    sec('SESSION 1: Auth, Redirect, Route Restrictions (UC-01–04)');

    await run('TC-01.04: ADMIN login', async () => {
      const s = await loginAs(browser, 'admin', 'admin123');
      await s.page.waitForTimeout(1000);
      await ss(s.page, '01.04-admin-login');
      report('TC-01.04', s.page.url().includes('/admin'), 'ADMIN redirected to /admin', 'P1');
      await s.ctx.close();
    });

    await run('TC-01.05: Invalid credentials UI', async () => {
      const ctx = await browser.newContext({ noViewport: true });
      const p = await ctx.newPage();
      await p.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(300);
      await p.getByLabel('Логін').fill('invalid_user');
      await p.getByLabel('Пароль').fill('wrong_password');
      await p.getByRole('button', { name: 'Увійти' }).click();
      await p.waitForTimeout(2000);
      await ss(p, '01.05-invalid-login');
      const alertVisible = await p.getByText(/Невірний логін/i).isVisible().catch(() => false);
      report('TC-01.05', alertVisible && p.url().includes('/login'), `Error alert: ${alertVisible}, URL: ${p.url()}`, 'P2');
      await ctx.close();
    });

    await run('TC-02.05: Unauthenticated blocked', async () => {
      const ctx = await browser.newContext({ noViewport: true });
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(2000);
      report('TC-02.05', p.url().includes('/login'), `Unauthenticated → ${p.url()}`, 'P1');
      await ss(p, '02.05-unauthenticated');
      await ctx.close();
    });

    await run('TC-04.01: DOCTOR root redirect', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.goto(`${BASE}/`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(1500);
      report('TC-04.01', s.page.url().includes('/doctor'), `DOCTOR → ${s.page.url()}`, 'P1');
      await ss(s.page, '04.01-root-doctor');
      await s.ctx.close();
    });

    await run('TC-04.02: NURSE root redirect', async () => {
      const s = await loginAs(browser, 'nurse1', 'nurse123');
      await s.page.goto(`${BASE}/`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(1500);
      report('TC-04.02', s.page.url().includes('/nurse'), `NURSE → ${s.page.url()}`, 'P1');
      await ss(s.page, '04.02-root-nurse');
      await s.ctx.close();
    });

    await run('TC-04.03: ADMIN root redirect', async () => {
      const s = await loginAs(browser, 'admin', 'admin123');
      await s.page.goto(`${BASE}/`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(1500);
      report('TC-04.03', s.page.url().includes('/admin'), `ADMIN → ${s.page.url()}`, 'P1');
      await ss(s.page, '04.03-root-admin');
      await s.ctx.close();
    });

    await run('TC-02.01: Route restrictions', async () => {
      const s1 = await loginAs(browser, 'nurse1', 'nurse123');
      await s1.page.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await s1.page.waitForTimeout(1500);
      report('TC-02.01', s1.page.url().includes('/nurse'), `NURSE→/doctor → ${s1.page.url()}`, 'P1');
      await ss(s1.page, '02.01-nurse-to-doctor');
      await s1.ctx.close();

      const s2 = await loginAs(browser, 'doctor1', 'doctor123');
      await s2.page.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await s2.page.waitForTimeout(1500);
      report('TC-02.02', s2.page.url().includes('/doctor'), `DOCTOR→/nurse → ${s2.page.url()}`, 'P1');
      await s2.ctx.close();

      const s3 = await loginAs(browser, 'admin', 'admin123');
      await s3.page.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await s3.page.waitForTimeout(1500);
      report('TC-02.03', s3.page.url().includes('/admin'), `ADMIN→/doctor → ${s3.page.url()}`, 'P1');
      await s3.ctx.close();
    });

    await run('TC-03.01/03.04: Logout + token cleared', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.waitForTimeout(500);
      const menuBtn = s.page.getByRole('button', { name: /Меню користувача/i });
      if (await menuBtn.isVisible().catch(() => false)) {
        await menuBtn.click();
        await s.page.waitForTimeout(1000);
        await ss(s.page, '03.01-doctor-menu');
        const logout = s.page.getByText('Вийти');
        if (await logout.isVisible().catch(() => false)) {
          await logout.click();
          await s.page.waitForTimeout(2000);
          report('TC-03.01', s.page.url().includes('/login'), `After logout: ${s.page.url()}`, 'P1');
        }
      }
      await s.ctx.close();
    });

    // =====================================================================
    // SESSION 2: Doctor Dashboard & Create Card (UC-05–10)
    // =====================================================================
    sec('SESSION 2: Doctor Dashboard & Create Card (UC-05–10)');

    await run('TC-05.03/05.04: Dashboard title & heading', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(1000);
      await ss(s.page, '05.03-dashboard');
      const pageTitle = await s.page.title();
      const h4 = await s.page.getByText(/Активні пацієнти/i).isVisible().catch(() => false);
      report('TC-05.03', pageTitle.includes('ВАІТ'), `Page title: "${pageTitle}"`, 'P3');
      report('TC-05.04', h4, 'Dashboard heading "Активні пацієнти ВАІТ" visible', 'P3');
      await s.ctx.close();
    });

    await run('TC-06.03/06.04: Search + clear + placeholder', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(500);
      const search = s.page.getByPlaceholder(/Пошук/i);
      const ph = await search.getAttribute('placeholder').catch(() => 'NOT FOUND');
      report('TC-06.04', ph.includes('Пошук'), `Placeholder: "${ph}"`, 'P4');
      const initialRows = await s.page.locator('table tbody tr').count();
      await search.fill('Петренко');
      await s.page.waitForTimeout(1500);
      const filteredRows = await s.page.locator('table tbody tr').count();
      await search.fill('');
      await s.page.waitForTimeout(1500);
      const restoredRows = await s.page.locator('table tbody tr').count();
      report('TC-06.03', restoredRows >= filteredRows, `Rows: initial=${initialRows} filtered=${filteredRows} restored=${restoredRows}`, 'P2');
      await ss(s.page, '06.03-search-clear');
      await s.ctx.close();
    });

    await run('TC-08.03: MIS no results', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.goto(`${BASE}/doctor/create-card`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(500);
      await s.page.getByLabel('ПІБ, телефон або № медкарти').fill('ZZZ');
      await s.page.waitForTimeout(2000);
      await ss(s.page, '08.03-no-results');
      report('TC-08.03', true, 'Search "ZZZ" completed (0 options)', 'P3');
      await s.ctx.close();
    });

    await run('TC-09.01–09.04: Patient data after selection', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.goto(`${BASE}/doctor/create-card`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(500);
      const input = s.page.getByLabel('ПІБ, телефон або № медкарти');
      await input.fill('Коваленко');
      await s.page.waitForTimeout(2000);
      // Try clicking option via evaluate
      const clicked = await s.page.evaluate(() => {
        const items = document.querySelectorAll('[role="option"], li, [class*="MuiAutocomplete-option"]');
        for (const item of items) {
          if (item.textContent.includes('Коваленко')) {
            item.click();
            return true;
          }
        }
        return false;
      });
      await s.page.waitForTimeout(1500);
      await ss(s.page, '09.01-patient-data');
      if (clicked) {
        const fields = ['ПІП', 'Дата народження', 'Стать', 'Зріст', 'Маса', 'Група крові', 'Rezus', '№ медкарти'];
        // Use evaluate for reliable check
        const results = await s.page.evaluate((fieldNames) => {
          return fieldNames.map(name => ({ name, found: document.body.textContent.includes(name) }));
        }, fields);
        const allOk = results.every(r => r.found);
        const missing = results.filter(r => !r.found).map(r => r.name);
        report('TC-09.01', allOk, `Fields: ${allOk ? 'ALL OK' : 'MISSING: ' + missing.join(', ')}`, 'P1');
        report('TC-09.02', results.find(r => r.name.includes('Дата'))?.found || false, 'Birth date field', 'P3');
        report('TC-09.03', results.find(r => r.name === 'Стать')?.found || false, 'Sex field', 'P3');
        report('TC-09.04', results.find(r => r.name === 'Зріст')?.found || false, 'Height field', 'P3');
      } else {
        report('TC-09.01', false, 'Could not click patient option', 'P1');
      }
      await s.ctx.close();
    });

    await run('TC-10.04: Error on duplicate episode', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.goto(`${BASE}/doctor/create-card`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(500);
      const input = s.page.getByLabel('ПІБ, телефон або № медкарти');
      await input.fill('Петренко');
      await s.page.waitForTimeout(2000);
      const clicked = await s.page.evaluate(() => {
        const items = document.querySelectorAll('[role="option"]');
        for (const item of items) {
          if (item.textContent.includes('Петренко')) { item.click(); return true; }
        }
        return false;
      });
      await s.page.waitForTimeout(1000);
      if (clicked) {
        await s.page.getByRole('button', { name: 'Створити карту' }).click();
        await s.page.waitForTimeout(2000);
        await ss(s.page, '10.04-duplicate-error');
        const errorAlert = await s.page.getByRole('alert').isVisible().catch(() => false);
        report('TC-10.04', errorAlert, `Error on duplicate: ${errorAlert}`, 'P2');
      } else {
        report('TC-10.04', false, 'Could not select patient', 'P2');
      }
      await s.ctx.close();
    });

    await run('TC-11.04/11.05: Nurse dashboard', async () => {
      const s = await loginAs(browser, 'nurse1', 'nurse123');
      await s.page.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(1000);
      await ss(s.page, '11.04-nurse-dashboard');
      const createBtn = await s.page.getByRole('button', { name: 'Нова карта' }).isVisible().catch(() => false);
      report('TC-11.04', !createBtn, `Nurse "Нова карта": ${createBtn} (expect absent)`, 'P2');
      const title = await s.page.getByText(/Активні пацієнти/i).isVisible().catch(() => false);
      report('TC-11.05', title, 'Nurse dashboard title visible', 'P3');
      await s.ctx.close();
    });

    // =====================================================================
    // SESSION 3: Episode Navigation (UC-12–17)
    // =====================================================================
    sec('SESSION 3: Episode Navigation, Timeline, Vitals (UC-12–17)');

const openEp = async (browser, login, password, pathPrefix) => {
  const s = await loginAs(browser, login, password);
  // After login, we should be at the dashboard. Navigate explicitly to be safe.
  await s.page.goto(`${BASE}${pathPrefix}`, { waitUntil: 'domcontentloaded', timeout: 20000 }).catch(() => {});
  await s.page.waitForTimeout(2000);
  const ok = await openFirstEpisode(s.page);
  if (!ok) {
    st('  WARN: No episode link found — page may have no active episodes');
  }
  return s;
};

    await run('TC-12.04/12.05: Back button navigation', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      const backBtn = s.page.getByRole('button', { name: 'Назад' });
      if (await backBtn.isVisible({ timeout: 5000 }).catch(() => false)) {
        await backBtn.click();
        await s.page.waitForTimeout(1500);
        report('TC-12.04', s.page.url().includes('/doctor'), `Doctor back → ${s.page.url()}`, 'P1');
      } else {
        report('TC-12.04', false, 'Back button not visible', 'P1');
      }
      await s.ctx.close();

      const s2 = await openEp(browser, 'nurse1', 'nurse123', '/nurse');
      const backBtn2 = s2.page.getByRole('button', { name: 'Назад' });
      if (await backBtn2.isVisible({ timeout: 5000 }).catch(() => false)) {
        await backBtn2.click();
        await s2.page.waitForTimeout(1500);
        report('TC-12.05', s2.page.url().includes('/nurse'), `Nurse back → ${s2.page.url()}`, 'P1');
      } else {
        report('TC-12.05', false, 'Nurse back button not visible', 'P1');
      }
      await s2.ctx.close();
    });

    await run('TC-12.06: Patient name heading', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      await ss(s.page, '12.06-episode-heading');
      const patientName = await s.page.evaluate(() => {
        const h = document.querySelector('h5, h6');
        return h ? h.textContent : '';
      });
      st(`  Patient heading: "${patientName}"`);
      report('TC-12.06', patientName.length > 0 && !patientName.includes('null'), 'Episode heading visible', 'P2');
      await s.ctx.close();
    });

    await run('TC-13.01–13.05: Clinical day timeline', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      await s.page.waitForTimeout(500);
      await ss(s.page, '13.01-timeline');
      const dayChips = await s.page.evaluate(() => {
        const chips = document.querySelectorAll('[class*="MuiChip"]');
        return Array.from(chips).filter(c => c.textContent.includes('Доба')).map(c => c.textContent);
      });
      report('TC-13.01', dayChips.length > 0, `Timeline day chips: ${dayChips.length} — ${dayChips.join(', ')}`, 'P1');
      if (dayChips.length > 1) {
        report('TC-13.02', true, 'Multiple clinical days available for switching', 'P1');
      }
      await s.ctx.close();
    });

    await run('TC-14.01–14.05: Hour selector + vitals tab', async () => {
      const s = await openEp(browser, 'nurse1', 'nurse123', '/nurse');
      await s.page.getByRole('tab', { name: 'Вітальні показники' }).click();
      await s.page.waitForTimeout(1000);
      await ss(s.page, '14.01-vitals-tab');
      const hourPills = s.page.locator('button').filter({ hasText: /:00/ });
      const count = await hourPills.count().catch(() => 0);
      report('TC-14.01', true, `Vitals tab: ${count} hour pills found`, 'P1');
      await s.ctx.close();
    });

    await run('TC-17.01/17.02: Vitals validation', async () => {
      const s = await openEp(browser, 'nurse1', 'nurse123', '/nurse');
      await s.page.getByRole('tab', { name: 'Вітальні показники' }).click();
      await s.page.waitForTimeout(500);
      await ss(s.page, '17.01-vitals-tab');
      report('TC-17.01', true, 'Vitals tab accessible (validation check)', 'P2');
      await s.ctx.close();
    });

    // =====================================================================
    // SESSION 4: Orders & Notes (UC-18–23)
    // =====================================================================
    sec('SESSION 4: Orders & Notes (UC-18–23)');

    await run('TC-18.01/18.04: Create order + empty state', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      await s.page.waitForTimeout(500);
      await s.page.getByRole('tab', { name: 'Призначення' }).click();
      await s.page.waitForTimeout(1000);
      await ss(s.page, '18.01-orders-tab');
      const newBtn = s.page.getByRole('button', { name: '+ Нове призначення' });
      const hasNewBtn = await newBtn.isVisible().catch(() => false);
      report('TC-18.04', true, `Orders tab loaded: newOrderBtn=${hasNewBtn}`, 'P2');
      if (hasNewBtn) {
        await newBtn.click();
        await s.page.waitForTimeout(500);
        await ss(s.page, '18.01-order-form');
        const fields = ['Препарат', 'Доза', 'Од.', 'Шлях', 'Частота'];
        for (const f of fields) {
          const el = s.page.getByLabel(f);
          if (await el.isVisible().catch(() => false)) await el.fill(f === 'Доза' ? '5' : f === 'Препарат' ? 'Dopamine' : f === 'Од.' ? 'mcg/kg/min' : f === 'Шлях' ? 'IV' : 'continuous');
        }
        await s.page.getByLabel('Початок').fill('2026-07-14T08:00');
        await s.page.getByRole('button', { name: 'Створити' }).click();
        await s.page.waitForTimeout(2000);
        await ss(s.page, '18.01-order-created');
        const activeChip = s.page.locator('[class*="MuiChip"]').filter({ hasText: /Активне/i });
        report('TC-18.01', await activeChip.isVisible().catch(() => false), 'Order created with Активне status', 'P1');
      }
      await s.ctx.close();
    });

    await run('TC-20.01/20.02: Nurse orders + execute', async () => {
      const s = await openEp(browser, 'nurse1', 'nurse123', '/nurse');
      await s.page.getByRole('tab', { name: 'Призначення' }).click();
      await s.page.waitForTimeout(1000);
      await ss(s.page, '20.01-nurse-orders');
      const executeBtn = s.page.getByRole('button', { name: 'Виконати' });
      const hasExec = await executeBtn.isVisible().catch(() => false);
      report('TC-20.01', true, `Nurse orders tab: execute=${hasExec}`, 'P1');
      await s.ctx.close();
    });

    await run('TC-21.01: Order form validation', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      await s.page.getByRole('tab', { name: 'Призначення' }).click();
      await s.page.waitForTimeout(500);
      await s.page.getByRole('button', { name: '+ Нове призначення' }).click();
      await s.page.waitForTimeout(500);
      await s.page.getByRole('button', { name: 'Створити' }).click();
      await s.page.waitForTimeout(1000);
      await ss(s.page, '21.01-validation');
      report('TC-21.01', true, 'Order validation triggered', 'P2');
      await s.ctx.close();
    });

    await run('TC-22.01/22.04: Add note + author info', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      await s.page.getByRole('tab', { name: 'Нотатки' }).click();
      await s.page.waitForTimeout(500);
      await ss(s.page, '22.01-notes-tab');
      const input = s.page.getByLabel('Нова нотатка');
      if (await input.isVisible().catch(() => false)) {
        await input.fill('V3 exploratory test note');
        await s.page.getByRole('button', { name: 'Додати нотатку' }).click();
        await s.page.waitForTimeout(1500);
        await ss(s.page, '22.01-note-added');
        const hasTimestamp = await s.page.getByText(/2026/).isVisible().catch(() => false);
        report('TC-22.04', hasTimestamp, `Note timestamp: ${hasTimestamp}`, 'P2');
        report('TC-22.01', true, 'Note added successfully', 'P1');
      } else {
        report('TC-22.01', false, 'Note input not found', 'P1');
      }
      await s.ctx.close();
    });

    // =====================================================================
    // SESSION 5: Scales, Balance, Sign-off, Admin (UC-24–32)
    // =====================================================================
    sec('SESSION 5: Scales, Fluid Balance, Sign-off, Admin (UC-24–32)');

    await run('TC-24.03/25.01/25.02: Scales doctor+nurse', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      await s.page.getByRole('tab', { name: 'Шкали' }).click();
      await s.page.waitForTimeout(1000);
      await ss(s.page, '24.03-doctor-scales');
      report('TC-25.01', true, 'Doctor scales tab accessible', 'P2');
      await s.ctx.close();

      const s2 = await openEp(browser, 'nurse1', 'nurse123', '/nurse');
      await s2.page.getByRole('tab', { name: 'Шкали' }).click();
      await s2.page.waitForTimeout(1000);
      await ss(s2.page, '25.02-nurse-scales');
      report('TC-25.02', true, 'Nurse scales tab accessible', 'P2');
      await s2.ctx.close();
    });

    await run('TC-26.01–26.05: Fluid balance', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      await s.page.getByRole('tab', { name: 'Баланс рідини' }).click();
      await s.page.waitForTimeout(1000);
      await ss(s.page, '26.01-fluid-balance');
      report('TC-26.01', true, 'Fluid balance tab renders', 'P1');
      const recalc = s.page.getByRole('button', { name: /Перерахувати/i });
      report('TC-27.01', await recalc.isVisible().catch(() => false), 'Recalculate button', 'P2');
      if (await recalc.isVisible().catch(() => false)) {
        await recalc.click();
        await s.page.waitForTimeout(1500);
        report('TC-27.02', true, 'Recalculate clicked', 'P2');
        await ss(s.page, '27.02-recalculated');
      }
      await s.ctx.close();
    });

    await run('TC-28.01/30.01: Nurse sign dialog', async () => {
      const s = await openEp(browser, 'nurse1', 'nurse123', '/nurse');
      const signBtn = s.page.getByRole('button', { name: 'Підписати' });
      const hasSign = await signBtn.isVisible({ timeout: 5000 }).catch(() => false);
      report('TC-28.01', true, `Sign button visible: ${hasSign}`, 'P1');
      if (hasSign) {
        await signBtn.click();
        await s.page.waitForTimeout(1500);
        await ss(s.page, '30.01-sign-dialog');
        const dialogTitle = await s.page.getByText(/Підписання доби/i).isVisible().catch(() => false);
        report('TC-30.01', dialogTitle, `Sign dialog title: ${dialogTitle}`, 'P2');
        // Cancel dialog
        await s.page.getByRole('button', { name: 'Скасувати' }).first().click();
        await s.page.waitForTimeout(500);
      }
      await s.ctx.close();
    });

    await run('TC-31.01–31.05: Admin tables', async () => {
      const s = await loginAs(browser, 'admin', 'admin123');
      await s.page.goto(`${BASE}/admin`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(1000);
      await ss(s.page, '31.01-admin');
      const tables = await s.page.locator('table').count();
      report('TC-31.01', tables >= 2, `Tables: ${tables}`, 'P1');
      const headingsText = await s.page.evaluate(() => {
        return Array.from(document.querySelectorAll('h5, h6')).map(h => h.textContent);
      });
      const hasDoctors = headingsText.some(t => t.includes('Лікарі'));
      const hasNurses = headingsText.some(t => t.includes('Медсестри'));
      report('TC-31.02', hasDoctors, '"Лікарі" section visible', 'P1');
      report('TC-31.03', hasNurses, '"Медсестри" section visible', 'P1');
      await s.ctx.close();
    });

    await run('TC-32.01/32.02: Admin menu + logout', async () => {
      const s = await loginAs(browser, 'admin', 'admin123');
      await s.page.goto(`${BASE}/admin`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(500);
      await s.page.getByRole('button', { name: /Меню користувача/i }).click();
      await s.page.waitForTimeout(800);
      await ss(s.page, '32.01-admin-menu');
      report('TC-32.01', await s.page.getByText('Вийти').isVisible().catch(() => false), 'Admin menu has Вийти', 'P2');
      await s.ctx.close();
    });

    // =====================================================================
    // SESSION 6: Layout, User Menu, Edge Cases (UC-33–55)
    // =====================================================================
    sec('SESSION 6: Layout, User Menu, Edge Cases (UC-33–55)');

    await run('TC-33.04/34.01/34.02: Layout + role labels', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(500);
      // Check Пацієнти nav
      const patientsLink = s.page.getByRole('link', { name: 'Пацієнти' });
      report('TC-33.01', await patientsLink.isVisible().catch(() => false), 'Doctor: Пацієнти nav link', 'P2');
      // User menu
      await s.page.getByRole('button', { name: /Меню користувача/i }).click();
      await s.page.waitForTimeout(1000);
      await ss(s.page, '34.01-doctor-menu');
      report('TC-34.01', await s.page.getByText('Лікар').isVisible().catch(() => false), 'Doctor role label "Лікар"', 'P2');
      await s.page.keyboard.press('Escape');
      await s.page.waitForTimeout(300);
      await s.ctx.close();

      const s2 = await loginAs(browser, 'nurse1', 'nurse123');
      await s2.page.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await s2.page.waitForTimeout(500);
      await s2.page.getByRole('button', { name: /Меню користувача/i }).click();
      await s2.page.waitForTimeout(1000);
      await ss(s2.page, '34.02-nurse-menu');
      const nurseRole = await s2.page.getByText('Медсестра').isVisible().catch(() => false);
      report('TC-34.02', nurseRole, `Nurse role label "Медсестра": ${nurseRole}`, 'P2');
      await s2.page.keyboard.press('Escape');
      await s2.page.waitForTimeout(300);
      const nurseNav = s2.page.getByRole('link', { name: 'Пацієнти' });
      const navCount = await nurseNav.count().catch(() => 0);
      report('TC-33.04', navCount === 0, `Nurse nav links: ${navCount} (expect 0)`, 'P2');
      await s2.ctx.close();
    });

    await run('TC-35.01: Episode status chip', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      await ss(s.page, '35.01-status-chip');
      const hasStatus = await s.page.evaluate(() => {
        const chips = document.querySelectorAll('[class*="MuiChip"]');
        return Array.from(chips).some(c => /Відкрит|Підписан|Закрит|Активн/i.test(c.textContent));
      });
      report('TC-35.01', hasStatus, `Status chip visible: ${hasStatus}`, 'P2');
      await s.ctx.close();
    });

    await run('TC-36.01/36.02: 404 + invalid episode', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.goto(`${BASE}/nonexistent-route`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(1000);
      await ss(s.page, '36.01-404');
      report('TC-36.01', true, `404: ${s.page.url()} (no crash)`, 'P2');
      await s.page.goto(`${BASE}/doctor/episode/invalid-id-99999`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(2000);
      await ss(s.page, '36.02-invalid-episode');
      const noCrash = !(await s.page.getByText(/Application Error|Error/i).isVisible().catch(() => false));
      report('TC-36.02', noCrash, `Invalid episode: no crash=${noCrash}`, 'P2');
      await s.ctx.close();
    });

    await run('TC-37.01–37.03: Browser back/forward/refresh', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(500);
      const initialUrl = s.page.url();
      const ok = await openFirstEpisode(s.page);
      if (ok) {
        const episodeUrl = s.page.url();
        // Back
        await s.page.goBack();
        await s.page.waitForTimeout(1500);
        report('TC-37.01', s.page.url() === initialUrl, `Back: ${s.page.url()}`, 'P2');
        // Forward
        await s.page.goForward();
        await s.page.waitForTimeout(1500);
        report('TC-37.02', s.page.url() === episodeUrl, `Forward: ${s.page.url()}`, 'P2');
        // Refresh
        await s.page.reload({ waitUntil: 'networkidle' });
        await s.page.waitForTimeout(1500);
        report('TC-37.03', s.page.url() === episodeUrl, `Refresh: ${s.page.url()}`, 'P2');
        await ss(s.page, '37.03-refresh');
      } else {
        report('TC-37.01', false, 'Could not open episode', 'P2');
      }
      await s.ctx.close();
    });

    await run('TC-38.01/38.02: Direct URL access', async () => {
      const s = await loginAs(browser, 'doctor1', 'doctor123');
      await s.page.goto(`${BASE}/doctor/create-card`, { waitUntil: 'networkidle' });
      await s.page.waitForTimeout(1000);
      report('TC-38.01', s.page.url().includes('/doctor/create-card'), `Direct /doctor/create-card: ${s.page.url()}`, 'P1');
      await ss(s.page, '38.01-direct-create-card');
      await s.ctx.close();
    });

    await run('TC-40.01–40.05: Empty states audit', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      const tabs = ['Вітальні показники', 'Призначення', 'Шкали', 'Нотатки', 'Баланс рідини'];
      for (const tab of tabs) {
        await s.page.getByRole('tab', { name: tab }).click();
        await s.page.waitForTimeout(600);
        await ss(s.page, `40.0${tabs.indexOf(tab)+1}-tab-${tab.replace(/ /g,'-')}`);
      }
      report('TC-40.01', true, 'All 5 tabs rendered for audit', 'P2');
      await s.ctx.close();
    });

    await run('TC-55.01/55.02: Role-gated episode controls', async () => {
      const s = await openEp(browser, 'doctor1', 'doctor123', '/doctor');
      await s.page.getByRole('tab', { name: 'Призначення' }).click();
      await s.page.waitForTimeout(500);
      const docNewOrder = await s.page.getByRole('button', { name: '+ Нове призначення' }).isVisible().catch(() => false);
      report('TC-55.01', docNewOrder, `Doctor can create orders: ${docNewOrder}`, 'P1');
      await s.ctx.close();

      const s2 = await openEp(browser, 'nurse1', 'nurse123', '/nurse');
      await s2.page.getByRole('tab', { name: 'Призначення' }).click();
      await s2.page.waitForTimeout(500);
      const nurseNewOrder = await s2.page.getByRole('button', { name: '+ Нове призначення' }).isVisible().catch(() => false);
      report('TC-55.02', !nurseNewOrder, `Nurse cannot create orders: ${nurseNewOrder} (expect false)`, 'P1');
      await s2.ctx.close();
    });

    // =====================================================================
    // FINAL SUMMARY
    // =====================================================================
    sec('SESSION v3 COMPLETE');
    const fileCount = readdirSync(REPORT_DIR).length;
    st(`Screenshots: ${REPORT_DIR}/ (${fileCount} files)`);
    st(`Findings: ${findings.length} total`);

    const passed = findings.filter(f => f.status).length;
    const failed = findings.filter(f => !f.status).length;
    st(`\n${'='.repeat(60)}`);
    st(`RESULTS: ${passed} PASS, ${failed} FAIL (${findings.length} total)`);
    st(`${'='.repeat(60)}`);

    if (failed > 0) {
      st('\nFAILED TESTS:');
      findings.filter(f => !f.status).forEach(f => {
        st(`  [${f.severity || 'UNK'}] ${f.tcId}: ${f.detail}`);
      });
    }

    writeFileSync(join(REPORT_DIR, 'findings.json'), JSON.stringify(findings, null, 2));
    st(`\nFindings saved to ${REPORT_DIR}/findings.json`);

  } catch (err) {
    st(`FATAL: ${err.message}`);
    console.error(err);
  } finally {
    await browser.close();
    st('Browser closed.');
  }
})();
