import { chromium } from 'playwright';
import { mkdirSync, readdirSync } from 'fs';
import { join } from 'path';

const BASE = 'http://localhost:5173';
const REPORT_DIR = 'exploratory-report';
const SLOW = 500;

mkdirSync(REPORT_DIR, { recursive: true });

const ss = (page, name) => page.screenshot({ path: join(REPORT_DIR, `${name}.png`), fullPage: true }).catch(() => {});

const sec = (msg) => console.log(`\n${'='.repeat(60)}\n${msg}\n${'='.repeat(60)}`);
const st = (msg) => console.log(`  ${msg}`);

const ctxOpts = (storageFile) => ({
  storageState: storageFile || undefined,
  noViewport: true,
});

(async () => {
  st('Launching chromium — non-headless, fullscreen, slowMo=500ms');
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
    }
  };

  try {
    // =====================================================================
    // SESSION 1: All-Role Authentication
    // =====================================================================
    sec('SESSION 1: All-Role Authentication (UC-01)');

    const loginAs = async (login, password) => {
      const ctx = await browser.newContext(ctxOpts(null));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(300);
      await p.getByLabel('Логін').fill(login);
      await p.getByLabel('Пароль').fill(password);
      await p.getByRole('button', { name: 'Увійти' }).click();
      try { await p.waitForURL(/doctor|nurse|admin/, { timeout: 15000 }); } catch {}
      await p.waitForTimeout(1500);
      await ss(p, `login-${login}`);
      return { ctx, page: p };
    };

    await run('TC-01.01: Login as DOCTOR', async () => {
      const s = await loginAs('doctor1', 'doctor123');
      await s.page.getByText('Активні пацієнти ВАІТ').waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
      await ss(s.page, '01.01-doctor-dashboard');
      await s.ctx.close();
    });

    await run('TC-01.02: Login as NURSE', async () => {
      const s = await loginAs('nurse1', 'nurse123');
      await s.page.getByText('Активні пацієнти ВАІТ').waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
      await ss(s.page, '01.02-nurse-dashboard');
      await s.ctx.close();
    });

    await run('TC-01.03: Login as HOD', async () => {
      const s = await loginAs('head1', 'head123');
      await s.page.getByText('Активні пацієнти ВАІТ').waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
      await ss(s.page, '01.03-hod-dashboard');
      await s.ctx.close();
    });

    await run('TC-01.04: Login as ADMIN', async () => {
      const s = await loginAs('admin', 'admin123');
      await s.page.getByText('Користувачі системи').waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
      await ss(s.page, '01.04-admin-page');
      await s.ctx.close();
    });

    await run('TC-01.05: Invalid credentials', async () => {
      const ctx = await browser.newContext(ctxOpts(null));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(300);
      await p.getByLabel('Логін').fill('invalid_user');
      await p.getByLabel('Пароль').fill('wrong_password');
      await p.getByRole('button', { name: 'Увійти' }).click();
      await p.waitForTimeout(2000);
      await ss(p, '01.05-invalid-login');
      const stillOnLogin = p.url().includes('/login');
      st(`  Stays on /login: ${stillOnLogin}`);
      await ctx.close();
    });

    await run('TC-01.06: Empty login fields', async () => {
      const ctx = await browser.newContext(ctxOpts(null));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(300);
      await p.getByRole('button', { name: 'Увійти' }).click();
      await p.waitForTimeout(1000);
      await ss(p, '01.06-empty-fields');
      await ctx.close();
    });

    await run('TC-01.14: Login as doctor2', async () => {
      const s = await loginAs('doctor2', 'doctor123');
      await s.ctx.close();
    });

    await run('TC-01.15: Login as nurse2', async () => {
      const s = await loginAs('nurse2', 'nurse123');
      await s.ctx.close();
    });

    await run('TC-01.07/01.09: Route restrictions', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/nurse.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(1500);
      st(`  NURSE→/doctor → ${p.url()}`);
      await ss(p, '01.07-nurse-to-doctor');
      await ctx.close();

      const ctx2 = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p2 = await ctx2.newPage();
      await p2.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await p2.waitForTimeout(1500);
      st(`  DOCTOR→/nurse → ${p2.url()}`);
      await ctx2.close();
    });

    await run('TC-01.11: Logout from DOCTOR', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      const menuBtn = p.getByRole('button', { name: /Меню користувача/i });
      if (await menuBtn.isVisible().catch(() => false)) {
        await menuBtn.click();
        await p.waitForTimeout(800);
        await ss(p, '01.11-doctor-menu');
        const logout = p.getByText('Вийти');
        if (await logout.isVisible().catch(() => false)) {
          await logout.click();
          await p.waitForTimeout(2000);
          st(`  After logout URL: ${p.url()}`);
          await ss(p, '01.11-after-logout');
        }
      }
      await ctx.close();
    });

    // =====================================================================
    // SESSION 2: Doctor Dashboard + Create Card
    // =====================================================================
    sec('SESSION 2: Doctor Dashboard + Create Card (UC-02, UC-03)');

    await run('TC-02.01: Dashboard loads episodes', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(1000);
      await ss(p, '02.01-dashboard');
      const rows = await p.locator('table tbody tr').count();
      st(`  Table rows: ${rows}`);
      await ctx.close();
    });

    await run('TC-02.02/02.06: Search filter + empty search', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      const searchIn = p.getByPlaceholder(/Пошук/i);
      if (await searchIn.isVisible().catch(() => false)) {
        await searchIn.fill('Петренко');
        await p.waitForTimeout(1500);
        await ss(p, '02.02-search-petrenko');

        await searchIn.fill('ZZZ_NONEXISTENT');
        await p.waitForTimeout(1500);
        await ss(p, '02.06-empty-search');

        await searchIn.fill('');
        await p.waitForTimeout(500);
      }
      await ctx.close();
    });

    await run('TC-02.03: "Нова карта" button → create-card', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      const btn = p.getByRole('button', { name: 'Нова карта' });
      st(`  Button visible: ${await btn.isVisible().catch(() => false)}`);
      if (await btn.isVisible().catch(() => false)) {
        await btn.click();
        await p.waitForTimeout(1500);
        st(`  URL: ${p.url()}`);
        await ss(p, '02.03-create-card');
      }
      await ctx.close();
    });

    await run('TC-03.01/03.06: MIS search + patient data', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor/create-card`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await ss(p, '03.01-create-card-page');

      const misInput = p.getByLabel('ПІБ, телефон або № медкарти');
      if (await misInput.isVisible().catch(() => false)) {
        await misInput.fill('Коваленко');
        await p.waitForTimeout(2000);
        await ss(p, '03.01-mis-search');
        const opt = p.getByRole('option', { name: /Коваленко/ }).first();
        if (await opt.isVisible({ timeout: 5000 }).catch(() => false)) {
          await opt.click();
          await p.waitForTimeout(1500);
          await ss(p, '03.06-patient-data');
          const fields = ['ПІП', 'Дата народження', 'Стать', 'Зріст', 'Маса', 'Група крові', 'Rezus'];
          for (const f of fields) {
            const vis = await p.getByText(f).isVisible().catch(() => false);
            st(`  Field "${f}": ${vis ? 'OK' : 'MISSING'}`);
          }
        }
      }
      await ctx.close();
    });

    await run('TC-03.04: Short search query hint', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor/create-card`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByLabel('ПІБ, телефон або № медкарти').fill('A');
      await p.waitForTimeout(1500);
      await ss(p, '03.04-short-query');
      await ctx.close();
    });

    await run('TC-03.03: Cancel → dashboard', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor/create-card`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByLabel('ПІБ, телефон або № медкарти').fill('Коваленко');
      await p.waitForTimeout(1000);
      const opt = p.getByRole('option', { name: /Коваленко/ }).first();
      if (await opt.isVisible({ timeout: 5000 }).catch(() => false)) {
        await opt.click();
        await p.waitForTimeout(1000);
        await p.getByRole('button', { name: 'Скасувати' }).click();
        await p.waitForTimeout(2000);
        st(`  URL after cancel: ${p.url()}`);
        await ss(p, '03.03-cancel');
      }
      await ctx.close();
    });

    await run('TC-02.04/02.07: Open episode', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(2000);
      st(`  URL: ${p.url()}`);
      await ss(p, '02.04-episode-page');
      await ctx.close();
    });

    // =====================================================================
    // SESSION 3: Episode Navigation + Vitals
    // =====================================================================
    sec('SESSION 3: Episode Navigation + Vitals (UC-04, UC-05)');

    await run('TC-04.01/04.02/04.03: Tab navigation + re-render', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(2000);
      await ss(p, '04.01-episode-page');

      const tabs = ['Вітальні показники', 'Призначення', 'Шкали', 'Нотатки', 'Баланс рідини'];
      for (const tab of tabs) {
        const el = p.getByRole('tab', { name: tab });
        const vis = await el.isVisible().catch(() => false);
        st(`  Tab "${tab}": ${vis ? 'visible' : 'MISSING'}`);
        if (vis) {
          await el.click();
          await p.waitForTimeout(600);
        }
      }
      await ss(p, '04.02-all-tabs-switched');

      // Re-render: switch back to first tab
      await p.getByRole('tab', { name: 'Вітальні показники' }).click();
      await p.waitForTimeout(600);
      st('  Re-render after switch-back: OK');
      await ctx.close();
    });

    await run('TC-04.04/TC-04.05: ClinicalDayTimeline + HourSelector', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/nurse.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(2000);
      await ss(p, '04.04-episode-page');

      // Check timeline chips
      const timeline = p.locator('[role="tablist"]').first();
      const timelineVisible = await timeline.isVisible().catch(() => false);
      st(`  Timeline row visible: ${timelineVisible}`);

      // Vitals tab
      await p.getByRole('tab', { name: 'Вітальні показники' }).click();
      await p.waitForTimeout(1000);
      await ss(p, '04.05-vitals-tab');

      // Check what renders — look for hour elements
      const hourBtns = await p.locator('button').filter({ has: p.locator('text=:') }).count();
      st(`  Hour buttons (containing ":"): ${hourBtns}`);

      const allBtns = await p.locator('button').count();
      st(`  Total buttons visible: ${allBtns}`);

      await ctx.close();
    });

    await run('TC-05.01/05.03: Vitals entry + read-only view', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/nurse.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(1500);
      await p.getByRole('tab', { name: 'Вітальні показники' }).click();
      await p.waitForTimeout(500);

      // Try clicking an hour button
      const hourButton = p.locator('button').filter({ hasText: /:00/ }).first();
      if (await hourButton.isVisible({ timeout: 3000 }).catch(() => false)) {
        await hourButton.click();
        await p.waitForTimeout(1000);
        await ss(p, '05.01-vitals-form');
        st('  Hour button clicked');
      } else {
        st('  No :00 hour button found');
        await ss(p, '05.01-no-hour-btn');
      }
      await ctx.close();

      // Doctor can view vitals
      const ctx2 = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p2 = await ctx2.newPage();
      await p2.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p2.waitForTimeout(500);
      await p2.getByRole('button', { name: 'Відкрити' }).first().click();
      await p2.waitForTimeout(1500);
      await p2.getByRole('tab', { name: 'Вітальні показники' }).click();
      await p2.waitForTimeout(1000);
      await ss(p2, '05.03-doctor-vitals-view');
      await ctx2.close();
    });

    // =====================================================================
    // SESSION 4: Orders + Notes
    // =====================================================================
    sec('SESSION 4: Orders & Notes (UC-06, UC-07)');

    await run('TC-06.01: Create prescription', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(1500);
      await p.getByRole('tab', { name: 'Призначення' }).click();
      await p.waitForTimeout(500);

      const newBtn = p.getByRole('button', { name: '+ Нове призначення' });
      if (await newBtn.isVisible().catch(() => false)) {
        await newBtn.click();
        await p.waitForTimeout(500);
        await ss(p, '06.01-prescription-form');
        await p.getByLabel('Препарат').fill('Norepinephrine');
        await p.getByLabel('Доза').fill('4');
        await p.getByLabel('Од.').fill('mcg');
        await p.getByLabel('Шлях').fill('IV');
        await p.getByLabel('Частота').fill('stat');
        await p.getByLabel('Початок').fill('2026-07-14T10:00');
        await p.getByRole('button', { name: 'Створити' }).click();
        await p.waitForTimeout(2000);
        await ss(p, '06.01-prescription-created');
        st('  Prescription created');
      }
      await ctx.close();
    });

    await run('TC-06.04: Empty prescriptions state', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(1500);
      await p.getByRole('tab', { name: 'Призначення' }).click();
      await p.waitForTimeout(1000);
      await ss(p, '06.04-prescriptions-tab');
      await ctx.close();
    });

    await run('TC-06.06: Role UI comparison — nurse sees different', async () => {
      const ctx1 = await browser.newContext(ctxOpts('.auth/nurse.json'));
      const p1 = await ctx1.newPage();
      await p1.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await p1.waitForTimeout(500);
      await p1.getByRole('button', { name: 'Відкрити' }).first().click();
      await p1.waitForTimeout(1500);
      await p1.getByRole('tab', { name: 'Призначення' }).click();
      await p1.waitForTimeout(500);
      // Nurse should see orders tab
      await ss(p1, '06.06-nurse-orders');
      await ctx1.close();
    });

    await run('TC-07.01/07.04: Add note + ordering', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(1500);
      await p.getByRole('tab', { name: 'Нотатки' }).click();
      await p.waitForTimeout(500);

      const noteInput = p.getByLabel('Нова нотатка');
      if (await noteInput.isVisible().catch(() => false)) {
        await noteInput.fill('Перша тестова нотатка');
        await p.getByRole('button', { name: 'Додати нотатку' }).click();
        await p.waitForTimeout(1500);
        await ss(p, '07.01-first-note');
        st('  First note added');

        await noteInput.fill('Друга тестова нотатка');
        await p.getByRole('button', { name: 'Додати нотатку' }).click();
        await p.waitForTimeout(1500);
        await ss(p, '07.04-two-notes');
        st('  Second note added — check ordering');
      }
      await ctx.close();
    });

    await run('TC-07.05: Notes persist after tab switch', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/nurse.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(1500);
      await p.getByRole('tab', { name: 'Нотатки' }).click();
      await p.waitForTimeout(500);
      // Switch away and back
      await p.getByRole('tab', { name: 'Шкали' }).click();
      await p.waitForTimeout(500);
      await p.getByRole('tab', { name: 'Нотатки' }).click();
      await p.waitForTimeout(500);
      await ss(p, '07.05-notes-after-switch');
      await ctx.close();
    });

    // =====================================================================
    // SESSION 5: Scales + Fluid Balance + Sign-off + Timeline
    // =====================================================================
    sec('SESSION 5: Scales, Fluid Balance, Sign-off (UC-08, UC-09, UC-10, UC-14)');

    await run('TC-08.01/08.02: Scales tab + empty state', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/nurse.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(1500);
      await p.getByRole('tab', { name: 'Шкали' }).click();
      await p.waitForTimeout(1000);
      await ss(p, '08.01-scales');
      const empty = await p.getByText(/Немає даних шкал/i).isVisible().catch(() => false);
      st(`  Empty state visible: ${empty}`);
      await ctx.close();
    });

    await run('TC-09.01/09.03: Fluid balance + recalculate', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/nurse.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(1500);
      await p.getByRole('tab', { name: 'Баланс рідини' }).click();
      await p.waitForTimeout(1000);
      await ss(p, '09.01-fluid-balance');
      const recalc = p.getByRole('button', { name: /Перерахувати/i });
      st(`  Recalculate button: ${await recalc.isVisible().catch(() => false)}`);
      await ctx.close();
    });

    await run('TC-10.01/10.03/10.05: Sign dialog + cancel', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/nurse.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(1500);
      const signBtn = p.getByRole('button', { name: 'Підписати' });
      const signVis = await signBtn.isVisible().catch(() => false);
      st(`  Sign button visible: ${signVis}`);
      if (signVis) {
        await signBtn.click();
        await p.waitForTimeout(1500);
        await ss(p, '10.01-sign-dialog');
        // Try to find dialog cancel button
        const cancelDlg = p.getByRole('button', { name: 'Скасувати' }).first();
        const cancelVis = await cancelDlg.isVisible().catch(() => false);
        st(`  Dialog cancel visible: ${cancelVis}`);
        if (cancelVis) await cancelDlg.click();
      }
      await ctx.close();
    });

    await run('TC-14.01/14.02: ClinicalDayTimeline', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(2000);
      await ss(p, '14.01-timeline');
      // Look for clinical day chips/badges
      const chips = p.locator('[class*="Chip"], [class*="badge"], [class*="day"]').filter({ hasText: /OPEN|NURSE|DOCTOR|CLOSED|ВІДКРИТИ|ЗАКРИТИ/i });
      const chipCount = await chips.count().catch(() => 0);
      st(`  Clinical day chips: ${chipCount}`);
      if (chipCount > 0) {
        await chips.first().click();
        await p.waitForTimeout(1000);
        st('  Clicked first day chip');
      }
      await ctx.close();
    });

    // =====================================================================
    // SESSION 6: Admin + Edge Cases + User Menu
    // =====================================================================
    sec('SESSION 6: Admin, Edge Cases, User Menu (UC-12, UC-13, UC-16)');

    await run('TC-12.01/12.02/12.03: Admin tables', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/admin.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/admin`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(1000);
      await ss(p, '12.01-admin');
      const tables = await p.locator('table').count();
      st(`  Tables: ${tables}`);
      const headings = await p.locator('h6, h5, h4').filter({ hasText: /Лікарі|Медсестри/i }).count();
      st(`  Section headings (Лікарі/Медсестри): ${headings}`);
      await ctx.close();
    });

    await run('TC-12.04/12.05/12.06: Admin page + logout', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/admin.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/admin`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      // Check heading
      const heading = p.getByText('Користувачі системи');
      st(`  Page heading visible: ${await heading.isVisible().catch(() => false)}`);
      // Logout menu
      const menuBtn = p.getByRole('button', { name: /Меню користувача/i });
      if (await menuBtn.isVisible().catch(() => false)) {
        await menuBtn.click();
        await p.waitForTimeout(800);
        await ss(p, '12.04-admin-menu');
        const logout = p.getByText('Вийти');
        if (await logout.isVisible().catch(() => false)) {
          await logout.click();
          await p.waitForTimeout(2000);
          st(`  After logout: ${p.url()}`);
          await ss(p, '12.05-after-logout');
        }
      }
      await ctx.close();
    });

    await run('TC-16.01/16.02/16.05/16.06: User menu differences', async () => {
      // Doctor user menu
      const ctx1 = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p1 = await ctx1.newPage();
      await p1.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p1.waitForTimeout(500);
      await p1.getByRole('button', { name: /Меню користувача/i }).click();
      await p1.waitForTimeout(800);
      await ss(p1, '16.01-doctor-menu');
      const docRole = await p1.getByText('Лікар').isVisible().catch(() => false);
      st(`  Doctor role label: ${docRole}`);
      await p1.keyboard.press('Escape');
      await p1.waitForTimeout(300);
      // Check "Пацієнти" nav link
      const patientsLink = p1.getByRole('button', { name: 'Пацієнти' });
      st(`  Doctor "Пацієнти" nav: ${await patientsLink.isVisible().catch(() => false)}`);
      await ctx1.close();

      // Nurse user menu
      const ctx2 = await browser.newContext(ctxOpts('.auth/nurse.json'));
      const p2 = await ctx2.newPage();
      await p2.goto(`${BASE}/nurse`, { waitUntil: 'networkidle' });
      await p2.waitForTimeout(500);
      await p2.getByRole('button', { name: /Меню користувача/i }).click();
      await p2.waitForTimeout(800);
      await ss(p2, '16.02-nurse-menu');
      const nurseRole = await p2.getByText('Медсестра').isVisible().catch(() => false);
      st(`  Nurse role label: ${nurseRole}`);
      await p2.keyboard.press('Escape');
      await p2.waitForTimeout(300);
      const nurseNav = p2.getByRole('button', { name: 'Пацієнти' });
      st(`  Nurse "Пацієнти" nav: ${await nurseNav.isVisible().catch(() => false)}`);
      await ctx2.close();
    });

    await run('TC-13.01: 404 unknown route', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/nonexistent-route`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(1000);
      await ss(p, '13.01-404');
      st(`  URL: ${p.url()}`);
      await ctx.close();
    });

    await run('TC-13.03/13.04: Direct URL + refresh', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor/create-card`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(1000);
      st(`  Direct URL: ${p.url().includes('/doctor/create-card')}`);
      await ss(p, '13.03-direct-url');

      // Go to episode, then refresh
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(1500);
      await ss(p, '13.04-before-refresh');
      await p.reload({ waitUntil: 'networkidle' });
      await p.waitForTimeout(1500);
      await ss(p, '13.04-after-refresh');
      st('  Refresh done');
      await ctx.close();
    });

    await run('TC-13.05/13.06: Browser back/forward', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      await p.getByRole('button', { name: 'Відкрити' }).first().click();
      await p.waitForTimeout(1500);
      await p.goBack();
      await p.waitForTimeout(1500);
      st(`  After back: ${p.url()}`);
      await ss(p, '13.05-back');
      await p.goForward();
      await p.waitForTimeout(1500);
      st(`  After forward: ${p.url()}`);
      await ss(p, '13.06-forward');
      await ctx.close();
    });

    await run('TC-13.07: AppBar title link navigates', async () => {
      const ctx = await browser.newContext(ctxOpts('.auth/doctor.json'));
      const p = await ctx.newPage();
      await p.goto(`${BASE}/doctor/create-card`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(500);
      // Click AppBar title link
      const title = p.getByRole('link', { name: /Карта інтенсивної терапії/i });
      if (await title.isVisible().catch(() => false)) {
        await title.click();
        await p.waitForTimeout(1500);
        st(`  After title click: ${p.url()}`);
        await ss(p, '13.07-title-link');
      } else {
        st('  Title link not visible');
      }
      await ctx.close();
    });

    // =====================================================================
    sec('EXPLORATORY SESSION V2 COMPLETE');
    st(`Screenshots saved to: ${REPORT_DIR}/ (${readdirSync(REPORT_DIR).length} files)`);

  } catch (err) {
    st(`FATAL: ${err.message}`);
  } finally {
    await browser.close();
    st('Browser closed.');
  }
})();
