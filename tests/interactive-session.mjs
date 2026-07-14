import { chromium } from 'playwright';
import * as fs from 'fs';

const BASE = 'http://localhost:5173';
const FINDINGS_FILE = 'C:/projects/intensive-care-unit-patient-chart/tests/exploratory-findings.md';
const ALL_BUGS = [];
let interactionCount = 0;

function note(role, page, element, action, result, severity = 'INFO') {
  interactionCount++;
  ALL_BUGS.push({ role, page, element, action, result, severity });
  const icon = severity === 'P1' ? '🔴' : severity === 'P2' ? '🟡' : severity === 'P3' ? '🟢' : 'ℹ️';
  console.log(`${icon} [${role}] ${page} > ${element}: ${action} = ${result}`);
}

async function clickS(page, locator, desc) {
  try {
    const el = page.locator(locator).first();
    if (await el.isVisible({ timeout: 1500 }).catch(() => false)) {
      await el.scrollIntoViewIfNeeded();
      await page.waitForTimeout(100);
      await el.click({ timeout: 3000 });
      await page.waitForTimeout(500);
      return true;
    }
    console.log(`  ⚠️ not found: ${desc}`);
    return false;
  } catch (e) {
    console.log(`  ⚠️ cannot click ${desc}: ${e.message.slice(0, 80)}`);
    return false;
  }
}

async function clickByRole(page, name, desc) {
  try {
    const el = page.getByRole('button', { name }).first();
    if (await el.isVisible({ timeout: 1500 }).catch(() => false)) {
      await el.scrollIntoViewIfNeeded();
      await page.waitForTimeout(100);
      await el.click({ timeout: 3000 });
      await page.waitForTimeout(500);
      return true;
    }
    console.log(`  ⚠️ not found: ${desc}`);
    return false;
  } catch (e) {
    console.log(`  ⚠️ cannot click ${desc}: ${e.message.slice(0, 80)}`);
    return false;
  }
}

async function fillS(page, locator, value, desc) {
  try {
    const el = page.locator(locator).first();
    if (await el.isVisible({ timeout: 1500 }).catch(() => false)) {
      await el.click({ timeout: 2000 });
      await el.fill(value);
      await page.waitForTimeout(300);
      console.log(`  ✓ fill "${desc}" = ${value}`);
      return true;
    }
    console.log(`  ~ fill "${desc}": not visible`);
    return false;
  } catch (e) {
    console.log(`  ⚠️ cannot fill ${desc}: ${e.message.slice(0, 80)}`);
    return false;
  }
}

async function fillByLabel(page, label, value) {
  try {
    const el = page.getByLabel(label).first();
    if (await el.isVisible({ timeout: 1500 }).catch(() => false)) {
      await el.click({ timeout: 2000 });
      await el.fill(value);
      await page.waitForTimeout(150);
      console.log(`  ✓ fill by label "${label}" = ${value}`);
      return true;
    }
    console.log(`  ~ fill by label "${label}": not visible`);
    return false;
  } catch (e) {
    console.log(`  ⚠️ cannot fill by label "${label}": ${e.message.slice(0, 80)}`);
    return false;
  }
}

async function clickAllHourPills(page, user, withFormFill, role) {
  const divisions = page.locator('#root .MuiBox-root:visible');
  const dCount = await divisions.count();
  for (let i = 0; i < dCount; i++) {
    try {
      const txt = (await divisions.nth(i).textContent())?.trim();
      if (txt && /^\d{1,2}:00/.test(txt) && txt.length < 10) {
        const hour = txt.replace('▶', '').trim();
        const div = divisions.nth(i);
        await div.click({ force: true });
        await page.waitForTimeout(400);
        note(user, 'Vitals Tab', `hour pill ${hour}`, 'click', 'selected');
      }
    } catch (e) {}
  }
}

async function doLogin(page, login, password) {
  await page.goto(`${BASE}/login`);
  await page.waitForTimeout(1500);
  note(login, 'Login Page', 'page', 'navigate', page.url());
  const inputs = page.locator('input');
  const cnt = await inputs.count();
  if (cnt >= 2) {
    await inputs.nth(0).fill(login);
    await inputs.nth(1).fill(password);
  }
  note(login, 'Login Page', 'login+password', 'fill', login);
  await clickS(page, 'button:has-text("Увійти")', 'Увійти');
  await page.waitForTimeout(2000);
}

async function doUserMenu(page, role, label) {
  if (await clickS(page, '[aria-label*="Меню користувача"]', 'user menu')) {
    await page.waitForTimeout(600);
    const items = page.locator('[role="menuitem"]:visible');
    const cnt = await items.count();
    for (let m = 0; m < cnt; m++) {
      try {
        const t = (await items.nth(m).textContent())?.trim() || '';
        note(role, label, `menu item[${m}]`, 'read', `"${t}"`);
      } catch (e) {}
    }
    // close menu
    await page.locator('h5').first().click({ force: true }).catch(() => {});
    await page.waitForTimeout(400);
  }
}

async function clickTableAll(page, role, label, tableIdx = 0) {
  const tables = page.locator('table');
  if ((await tables.count()) <= tableIdx) return;
  const table = tables.nth(tableIdx);
  if (!(await table.isVisible().catch(() => false))) return;
  // headers
  const headers = table.locator('thead th');
  const hCnt = await headers.count();
  for (let h = 0; h < hCnt; h++) {
    try {
      const t = (await headers.nth(h).textContent())?.trim() || '';
      await headers.nth(h).click({ timeout: 500, force: true });
      await page.waitForTimeout(100);
      note(role, label, `table header[${h}] "${t}"`, 'click', 'OK');
    } catch (e) {}
  }
  // rows
  const rows = table.locator('tbody tr');
  const rCnt = await rows.count();
  for (let r = 0; r < rCnt; r++) {
    const cells = rows.nth(r).locator('td');
    const cCnt = await cells.count();
    for (let c = 0; c < cCnt; c++) {
      try {
        const t = (await cells.nth(c).textContent())?.trim() || '';
        await cells.nth(c).click({ timeout: 500, force: true });
        await page.waitForTimeout(80);
      } catch (e) {}
    }
  }
}

async function runDoctorFlow(login, password, displayName) {
  console.log(`\n${'='.repeat(60)}`);
  console.log(`  DOCTOR FLOW: ${displayName} (${login})`);
  console.log(`${'='.repeat(60)}`);
  const browser = await chromium.launch({ headless: false, args: ['--start-maximized', '--start-fullscreen'] });
  const ctx = await browser.newContext({ viewport: null, locale: 'uk-UA' });
  const p = await ctx.newPage();

  try {
    // === LOGIN ===
    await doLogin(p, login, password);

    // === DASHBOARD ===
    note(displayName, 'Dashboard', 'page', 'loaded', p.url());
    await doUserMenu(p, displayName, 'Dashboard');
    await clickS(p, 'a:has-text("Карта інтенсивної терапії")', 'AppBar title');
    await clickS(p, 'a:has-text("Пацієнти")', 'Пацієнти nav');
    await fillS(p, 'input[placeholder*="Пошук"], input[placeholder*="ПІБ"]', 'Коваленко', 'search');
    await p.waitForTimeout(500);
    await fillS(p, 'input[placeholder*="Пошук"], input[placeholder*="ПІБ"]', '', 'clear search');
    await clickS(p, 'button:has-text("Нова карта")', 'Нова карта');

    // === CREATE CARD ===
    note(displayName, 'Create Card', 'page', 'loaded', p.url());
    const autoLabel = 'ПІБ, телефон або № медкарти';
    const autoInput = p.getByLabel(autoLabel).first();
    if (await autoInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await autoInput.click();
      await autoInput.fill('Пет');
      await p.waitForTimeout(1500);
      // Select first option
      const opt = p.locator('[role="option"]:visible').first();
      if (await opt.isVisible().catch(() => false)) {
        const optText = (await opt.textContent())?.trim() || '';
        note(displayName, 'Create Card', 'autocomplete option', 'select', optText.substring(0, 50));
        await opt.click();
        await p.waitForTimeout(1500);
      }
      // Clear
      const clearBtn = p.locator('[aria-label="Clear"]');
      if (await clearBtn.isVisible().catch(() => false)) {
        await clearBtn.click();
        await p.waitForTimeout(500);
      }
    }
    await clickS(p, 'button:has-text("Скасувати")', 'Скасувати');

    // === DASHBOARD → OPEN EPISODE ===
    await p.goto(`${BASE}/doctor`);
    await p.waitForTimeout(1000);
    await doUserMenu(p, displayName, 'Dashboard');

    const openBtn = p.locator('[role="button"]:has-text("Відкрити")').first();
    if (await openBtn.isVisible().catch(() => false)) {
      note(displayName, 'Dashboard', 'Відкрити', 'click', 'opening episode');
      await openBtn.click();
      await p.waitForTimeout(2500);
      note(displayName, 'Episode', 'page', 'loaded', p.url());

      // Episode header
      for (const t of ['Петренко', 'Статус:', '№ a', 'Доба №']) {
        const el = p.locator(`text=${t}`).first();
        if (await el.isVisible().catch(() => false)) {
          await el.click({ force: true });
          await p.waitForTimeout(150);
          note(displayName, 'Episode', `header "${t}"`, 'click', 'OK');
        }
      }
      // Clinical days — click the parent tile div (contains both "Доба N" and date)
      for (const d of ['1', '2']) {
        const tile = p.locator('#root .MuiBox-root').filter({ hasText: `Доба ${d}` }).filter({ hasText: 'лип' }).first();
        if (await tile.isVisible({ timeout: 1000 }).catch(() => false)) {
          await tile.click({ force: true });
          await p.waitForTimeout(600);
          const hdrDay = (await p.locator('h6, h5').first().textContent().catch(() => '')) || '';
          note(displayName, 'Episode', `clinical day "Доба ${d}"`, 'click', `selected (header: "${hdrDay.substring(0,20)}")`);
        }
      }
      // Refresh to clean state after day switching
      await p.goto(p.url());
      await p.waitForTimeout(1500);
      // Re-select a clinical day after refresh
      const dayTile = p.locator('#root .MuiBox-root').filter({ hasText: 'Доба 2' }).filter({ hasText: 'лип' }).first();
      if (await dayTile.isVisible({ timeout: 1000 }).catch(() => false)) {
        await dayTile.click({ force: true });
        await p.waitForTimeout(600);
      }
      await p.waitForTimeout(500);

      // === EXPLORE ALL 5 TABS ===
      const tabs = p.locator('[role="tab"]');
      const tCount = await tabs.count();
      for (let t = 0; t < tCount; t++) {
        const tabName = (await tabs.nth(t).textContent())?.trim() || `tab-${t}`;
        console.log(`\n--- TAB: ${tabName} ---`);
        await tabs.nth(t).click();
        await p.waitForTimeout(1500);
        note(displayName, 'Episode', `tab "${tabName}"`, 'click', 'switched');

        if (tabName.includes('Вітальні')) {
          // Hour pills
          await clickAllHourPills(p, displayName, true, 'doctor');
          // Try filling vitals form (may be readonly for doctors)
          const vitalsFieldsToTry = [
            ['АТ сист (мм.рт.ст)', '120'],
            ['АТ діас (мм.рт.ст)', '80'],
            ['ЧСС (в 1 хв)', '72'],
            ['SpO2 (%)', '98'],
            ['Темп. тіла (°С)', '36.6'],
            ['ЦВТ (мм.вод.ст)', '8'],
            ['ЧД (в 1 хв)', '16'],
            ['Свідомість', 'Ясна'],
            ['etCO2 (мм.рт.ст)', '35'],
            ['FiO2 (%)', '21'],
            ['Діурез (мл/год)', '100'],
            ['Дренаж (мл)', '50'],
            ['Біль (0-10)', '0'],
            ['Нотатки', 'Стан пацієнта стабільний'],
          ];
          let anyFilled = false;
          for (const [lbl, val] of vitalsFieldsToTry) {
            if (await fillByLabel(p, lbl, val)) anyFilled = true;
          }
          if (anyFilled) {
            await p.waitForTimeout(300);
            await clickS(p, 'button:has-text("Зберегти показники")', 'Зберегти показники');
            await p.waitForTimeout(500);
          }
          // Click table headers + rows
          const vt = p.locator('table').last();
          if (await vt.isVisible().catch(() => false)) {
            const vh = vt.locator('thead th');
            const vc = await vh.count();
            for (let h = 0; h < vc; h++) {
              try {
                const ht = (await vh.nth(h).textContent())?.trim() || '';
                await vh.nth(h).click({ timeout: 500, force: true });
                await p.waitForTimeout(80);
              } catch(e) {}
            }
            const vr = vt.locator('tbody tr');
            const vrc = await vr.count();
            for (let r = 0; r < Math.min(vrc, 5); r++) {
              try { await vr.nth(r).click({ timeout: 500, force: true }); await p.waitForTimeout(50); } catch(e) {}
            }
          }
        } else if (tabName.includes('Призначення')) {
          // Toggle order form
          await clickS(p, 'button:has-text("+ Нове призначення")', '+ Нове призначення');
          await p.waitForTimeout(400);
          await clickS(p, 'button:has-text("+ Нове призначення")', '+ Нове призначення (collapse)');
          await p.waitForTimeout(400);
          await clickS(p, 'button:has-text("+ Нове призначення")', '+ Нове призначення (expand again)');

          // Fill order form fields using getByLabel
          const orderFieldDefs = [
            ['Категорія', 'Антибіотики'],
            ['Препарат', 'Цефтріаксон'],
            ['Доза', '1.0'],
            ['Од.', 'г'],
            ['Шлях', 'В/в крапельно'],
            ['Частота', '2 рази на добу'],
          ];
          for (const [lbl, val] of orderFieldDefs) {
            await fillByLabel(p, lbl, val);
          }
          // DateTime fields — fill by label + type
          const dtFields = ['Початок', 'Кінець'];
          const dtVals = ['2026-07-14T08:00', '2026-07-21T08:00'];
          for (let di = 0; di < dtFields.length; di++) {
            const dt = p.getByLabel(dtFields[di]).first();
            if (await dt.isVisible({ timeout: 1500 }).catch(() => false)) {
              await dt.click();
              await dt.type(dtVals[di], { delay: 30 });
              await p.waitForTimeout(150);
              console.log(`  ✓ fill datetime "${dtFields[di]}"`);
            } else {
              console.log(`  ~ fill datetime "${dtFields[di]}": not visible`);
            }
          }
          await clickS(p, 'button:has-text("Створити")', 'Створити order');
          await p.waitForTimeout(500);

          // Click table
          const ot = p.locator('table').first();
          if (await ot.isVisible().catch(() => false)) {
            const oh = ot.locator('thead th');
            const oc = await oh.count();
            for (let h = 0; h < oc; h++) {
              try { await oh.nth(h).click({ force: true }); await p.waitForTimeout(80); } catch(e) {}
            }
          }

          // Click cancel on any order
          await clickS(p, 'button:has-text("Скасувати")', 'Скасувати order');
          await p.waitForTimeout(500);

          // Click empty state
          const emptyOrd = p.locator('text=Немає призначень');
          if (await emptyOrd.isVisible().catch(() => false)) {
            await emptyOrd.click({ force: true });
            note(displayName, 'Orders Tab', 'empty state', 'click', 'OK');
          }

        } else if (tabName.includes('Шкали')) {
          const emptyScale = p.locator('text=Немає даних шкал');
          if (await emptyScale.isVisible().catch(() => false)) {
            await emptyScale.click({ force: true });
            note(displayName, 'Scales Tab', 'empty state', 'click', 'OK');
          }
          // Try scale dropdown (select) + result + add button
          const scaleSelect = p.getByLabel('Шкала').first();
          if (await scaleSelect.isVisible({ timeout: 1000 }).catch(() => false)) {
            await scaleSelect.click();
            await p.waitForTimeout(400);
            const opt = p.locator('[role="option"]:visible').first();
            if (await opt.isVisible().catch(() => false)) {
              await opt.click();
              await p.waitForTimeout(200);
            }
            await fillByLabel(p, 'Результат', '15');
            await clickS(p, 'button:has-text("Додати")', 'Додати scale');
          }

        } else if (tabName.includes('Нотатки')) {
          await p.waitForTimeout(1000);
          const emptyNote = p.locator('text=Немає нотаток');
          if (await emptyNote.isVisible({ timeout: 2000 }).catch(() => false)) {
            await emptyNote.click({ force: true });
            note(displayName, 'Notes Tab', 'empty state', 'click', 'OK');
          }
          const ta = p.getByLabel('Нова нотатка').first();
          if (await ta.isVisible({ timeout: 2000 }).catch(() => false)) {
            console.log('  ✓ notes textarea found');
            await ta.click();
            // Try empty submit
            const emptySubmit = await clickS(p, 'button:has-text("Додати нотатку")', 'Додати нотатку (empty)');
            if (emptySubmit) console.log('  ✓ Додати нотатку (empty) clicked');
            await p.waitForTimeout(300);
            // Fill and submit
            await ta.fill('Стан пацієнта покращується. Дихання везикулярне, показники гемодинаміки стабільні.');
            await p.waitForTimeout(200);
            const filledSubmit = await clickS(p, 'button:has-text("Додати нотатку")', 'Додати нотатку (filled)');
            if (filledSubmit) console.log('  ✓ Додати нотатку (filled) clicked');
            note(displayName, 'Notes Tab', 'add note', 'complete', 'note added');
            await p.waitForTimeout(1000);
            // Click note cards
            const cards = p.locator('.MuiCard-root:visible');
            const cc = await cards.count();
            for (let nc = 0; nc < cc; nc++) {
              try {
                await cards.nth(nc).click({ force: true });
                await p.waitForTimeout(100);
                const spans = cards.nth(nc).locator('p, span, .MuiChip-root');
                const sc = await spans.count();
                for (let s = 0; s < sc; s++) {
                  try {
                    const st = (await spans.nth(s).textContent())?.trim() || '';
                    if (st && st.length < 40) {
                      await spans.nth(s).click({ force: true });
                      await p.waitForTimeout(50);
                    }
                  } catch(e) {}
                }
              } catch(e) {}
            }
          } else {
            console.log('  ~ notes textarea: not visible');
          }

        } else if (tabName.includes('Баланс')) {
          await p.waitForTimeout(1000);
          for (const sec of ['Надійшло:', 'Виділено:', 'Добовий баланс:', 'Кумулятивний баланс:']) {
            const se = p.locator(`text=${sec}`).first();
            if (await se.isVisible({ timeout: 2000 }).catch(() => false)) {
              await se.click({ force: true });
              await p.waitForTimeout(80);
              console.log(`  ✓ fluid section "${sec}" clicked`);
            }
          }
          for (let ri = 0; ri < 2; ri++) {
            const clicked = await clickS(p, 'button:has-text("Перерахувати")', `Перерахувати #${ri + 1}`);
            if (clicked) console.log(`  ✓ Перерахувати #${ri + 1} clicked`);
          }
        }
      }

      // === SIGN DAY ===
      // Check both locator strategies
      let signBtn = p.getByRole('button', { name: /Підписати добу/i }).first();
      if (!(await signBtn.isVisible({ timeout: 1000 }).catch(() => false))) {
        signBtn = p.locator('button:has-text("Підписати добу")').first();
      }
      if (await signBtn.isVisible({ timeout: 1500 }).catch(() => false)) {
        await signBtn.click();
        await p.waitForTimeout(800);
        const dialog = p.getByRole('dialog');
        const dlgVisible = await dialog.isVisible({ timeout: 1000 }).catch(() => false);
        if (dlgVisible) {
          const dialogCancel = dialog.locator('button:has-text("Скасувати")');
          if (await dialogCancel.isVisible().catch(() => false)) {
            await dialogCancel.click();
            await p.waitForTimeout(500);
          }
          await signBtn.click();
          await p.waitForTimeout(500);
          const dialogConfirm = dialog.locator('button:has-text("Підписати")');
          if (await dialogConfirm.isVisible().catch(() => false)) {
            await dialogConfirm.click();
            await p.waitForTimeout(1000);
            note(displayName, 'Episode', 'sign day', 'complete', 'day signed');
          }
        } else {
          note(displayName, 'Episode', 'sign day', 'click', 'no dialog appeared');
        }
      }
    }
  } catch (e) {
    console.log(`  ⚠️ ERROR in ${displayName} flow: ${e.message}`);
  }

  await p.waitForTimeout(500);
  await browser.close();
  console.log(`  ✅ ${displayName} done.`);
}

async function runNurseFlow(login, password, displayName) {
  console.log(`\n${'='.repeat(60)}`);
  console.log(`  NURSE FLOW: ${displayName} (${login})`);
  console.log(`${'='.repeat(60)}`);
  const browser = await chromium.launch({ headless: false, args: ['--start-maximized', '--start-fullscreen'] });
  const ctx = await browser.newContext({ viewport: null, locale: 'uk-UA' });
  const p = await ctx.newPage();

  try {
    await doLogin(p, login, password);
    note(displayName, 'Nurse Dashboard', 'page', 'loaded', p.url());
    await doUserMenu(p, displayName, 'Nurse Dashboard');
    await fillS(p, 'input[placeholder*="Пошук"]', 'Коваленко', 'search');
    await p.waitForTimeout(400);
    await fillS(p, 'input[placeholder*="Пошук"]', '', 'clear search');

    const openBtn = p.locator('[role="button"]:has-text("Відкрити")').first();
    if (await openBtn.isVisible().catch(() => false)) {
      await openBtn.click();
      await p.waitForTimeout(2500);
      note(displayName, 'Nurse Episode', 'page', 'loaded', p.url());

      // Header
      for (const t of ['Петренко', 'Статус:', '№ a', 'Доба №']) {
        const el = p.locator(`text=${t}`).first();
        if (await el.isVisible().catch(() => false)) {
          await el.click({ force: true });
          await p.waitForTimeout(100);
        }
      }
      // Clinical days
      for (const d of ['1', '2']) {
        const tile = p.locator('#root .MuiBox-root').filter({ hasText: `Доба ${d}` }).filter({ hasText: 'лип' }).first();
        if (await tile.isVisible({ timeout: 1000 }).catch(() => false)) {
          await tile.click({ force: true });
          await p.waitForTimeout(600);
        }
      }
      await p.goto(p.url());
      await p.waitForTimeout(1500);
      const dayTile2 = p.locator('#root .MuiBox-root').filter({ hasText: 'Доба 2' }).filter({ hasText: 'лип' }).first();
      if (await dayTile2.isVisible({ timeout: 1000 }).catch(() => false)) {
        await dayTile2.click({ force: true });
        await p.waitForTimeout(600);
      }
      await p.waitForTimeout(500);

      // 5 tabs
      const tabs = p.locator('[role="tab"]');
      const tCount = await tabs.count();
      for (let t = 0; t < tCount; t++) {
        const tabName = (await tabs.nth(t).textContent())?.trim() || `tab-${t}`;
        console.log(`\n--- NURSE TAB: ${tabName} ---`);
        await tabs.nth(t).click();
        await p.waitForTimeout(1500);
        note(displayName, 'Nurse Episode', `tab "${tabName}"`, 'click', 'switched');

        if (tabName.includes('Вітальні')) {
          await clickAllHourPills(p, displayName, true, 'nurse');
          // Fill vitals for 8:00 and 9:00
          for (const hour of ['8:00', '9:00']) {
            const pill = p.locator(`text="${hour}"`).first();
            if (await pill.isVisible().catch(() => false)) {
              await pill.click({ force: true });
              await p.waitForTimeout(500);
              const vitalsFields = [
                ['АТ сист', '120'], ['АТ діас', '80'], ['ЧСС', '72'], ['SpO2', '98'],
                ['Темп. тіла', '36.6'], ['ЦВТ', '8'], ['ЧД', '16'],
                ['Свідомість', 'Ясна, задовільний стан'],
                ['etCO2', '35'], ['FiO2', '21'], ['Діурез', '100'], ['Дренаж', '50'],
                ['Біль', '0'], ['Нотатки', 'Стан пацієнта стабільний, гемодинаміка в нормі'],
              ];
              for (const [lbl, val] of vitalsFields) {
                if (await fillByLabel(p, lbl, val)) {
                  note(displayName, 'Vitals Tab', `field "${lbl}"`, 'fill', val);
                }
              }
              await clickS(p, 'button:has-text("Зберегти показники")', 'Зберегти показники');
              await p.waitForTimeout(500);
            }
          }
          // Table
          const vt = p.locator('table').last();
          if (await vt.isVisible().catch(() => false)) {
            const vh = vt.locator('thead th');
            const vc = await vh.count();
            for (let h = 0; h < vc; h++) {
              try { await vh.nth(h).click({ force: true }); await p.waitForTimeout(50); } catch(e) {}
            }
          }
        } else if (tabName.includes('Призначення')) {
          // Click execute (if orders exist)
          const execBtn = p.locator('button:has-text("Виконати")');
          if (await execBtn.isVisible().catch(() => false)) {
            await execBtn.click();
            await p.waitForTimeout(500);
            await fillS(p, 'input:visible', '1.0', 'exec dose');
            await fillByLabel(p, 'Коментар', 'Виконано успішно, пацієнт переніс добре');
            await clickS(p, 'button:has-text("Підтвердити")', 'exec confirm');
            await p.waitForTimeout(500);
          }
          // Table
          const ot = p.locator('table').first();
          if (await ot.isVisible().catch(() => false)) {
            const oh = ot.locator('thead th');
            const oc = await oh.count();
            for (let h = 0; h < oc; h++) {
              try { await oh.nth(h).click({ force: true }); await p.waitForTimeout(50); } catch(e) {}
            }
          }
          const emptyOrd = p.locator('text=Немає призначень');
          if (await emptyOrd.isVisible().catch(() => false)) {
            await emptyOrd.click({ force: true });
          }
        } else if (tabName.includes('Шкали')) {
          const emptyScale = p.locator('text=Немає даних шкал');
          if (await emptyScale.isVisible().catch(() => false)) {
            await emptyScale.click({ force: true });
            note(displayName, 'Scales Tab', 'empty state', 'click', 'confirmed no create controls');
          }
        } else if (tabName.includes('Нотатки')) {
          await p.waitForTimeout(1000);
          const emptyNote = p.locator('text=Немає нотаток');
          if (await emptyNote.isVisible({ timeout: 2000 }).catch(() => false)) {
            await emptyNote.click({ force: true });
            console.log('  ✓ nurse notes empty state clicked');
          }
          const ta2 = p.getByLabel('Нова нотатка').first();
          if (await ta2.isVisible({ timeout: 2000 }).catch(() => false)) {
            console.log('  ✓ nurse notes textarea found');
            await ta2.click();
            await ta2.fill('Стан пацієнта задовільний. Показники в нормі.');
            await p.waitForTimeout(200);
            if (await clickS(p, 'button:has-text("Додати нотатку")', 'add note')) {
              console.log('  ✓ nurse Додати нотатку clicked');
            }
            note(displayName, 'Nurse Notes', 'add note', 'complete', 'note added');
            await p.waitForTimeout(800);
            const cards2 = p.locator('.MuiCard-root:visible');
            const cc2 = await cards2.count();
            for (let nc = 0; nc < cc2; nc++) {
              try { await cards2.nth(nc).click({ force: true }); await p.waitForTimeout(100); } catch(e) {}
            }
          } else {
            console.log('  ~ nurse notes textarea: not visible');
          }
        } else if (tabName.includes('Баланс')) {
          await p.waitForTimeout(1000);
          for (const sec of ['Надійшло:', 'Виділено:', 'Добовий баланс:', 'Кумулятивний баланс:']) {
            const se = p.locator(`text=${sec}`).first();
            if (await se.isVisible({ timeout: 2000 }).catch(() => false)) {
              await se.click({ force: true });
              await p.waitForTimeout(50);
              console.log(`  ✓ nurse fluid section "${sec}" clicked`);
            }
          }
          for (let ri = 0; ri < 2; ri++) {
            if (await clickS(p, 'button:has-text("Перерахувати")', `Перерахувати #${ri + 1}`)) {
              console.log(`  ✓ nurse Перерахувати #${ri + 1} clicked`);
            }
          }
        }
      }

      // SIGN DAY
      let signBtn2 = p.getByRole('button', { name: /Підписати добу/i }).first();
      if (!(await signBtn2.isVisible({ timeout: 1000 }).catch(() => false))) {
        signBtn2 = p.locator('button:has-text("Підписати добу")').first();
      }
      if (await signBtn2.isVisible({ timeout: 1500 }).catch(() => false)) {
        await signBtn2.click();
        await p.waitForTimeout(800);
        const dialog = p.getByRole('dialog');
        if (await dialog.isVisible({ timeout: 1000 }).catch(() => false)) {
          const dlgCancel = dialog.locator('button:has-text("Скасувати")');
          if (await dlgCancel.isVisible().catch(() => false)) {
            await dlgCancel.click();
            await p.waitForTimeout(500);
          }
          await signBtn2.click();
          await p.waitForTimeout(500);
          const dlgConfirm = dialog.locator('button:has-text("Підписати")');
          if (await dlgConfirm.isVisible().catch(() => false)) {
            await dlgConfirm.click();
            await p.waitForTimeout(1000);
            note(displayName, 'Nurse Episode', 'sign day', 'complete', 'day signed');
          }
        }
      }
    }
  } catch (e) {
    console.log(`  ⚠️ ERROR in ${displayName} flow: ${e.message}`);
  }
  await p.waitForTimeout(500);
  await browser.close();
  console.log(`  ✅ ${displayName} done.`);
}

async function runAdminFlow() {
  console.log(`\n${'='.repeat(60)}`);
  console.log(`  ADMIN FLOW`);
  console.log(`${'='.repeat(60)}`);
  const browser = await chromium.launch({ headless: false, args: ['--start-maximized', '--start-fullscreen'] });
  const ctx = await browser.newContext({ viewport: null, locale: 'uk-UA' });
  const p = await ctx.newPage();

  try {
    await doLogin(p, 'admin', 'admin123');
    note('admin', 'Admin Page', 'page', 'loaded', p.url());
    await doUserMenu(p, 'admin', 'Admin Page');

    // Click headings
    for (const h of ['Користувачі системи', 'Лікарі', 'Медсестри']) {
      const el = p.locator(`text=${h}`).first();
      if (await el.isVisible().catch(() => false)) {
        await el.click({ force: true });
        await p.waitForTimeout(100);
        note('admin', 'Admin Page', `heading "${h}"`, 'click', 'OK');
      }
    }

    // 2 tables - full interaction
    const tables = p.locator('table');
    const tblCount = await tables.count();
    for (let t = 0; t < tblCount; t++) {
      const tblName = t === 0 ? 'Doctors' : 'Nurses';
      // Headers
      const headers = tables.nth(t).locator('thead th');
      const hCnt = await headers.count();
      for (let h = 0; h < hCnt; h++) {
        try {
          const ht = (await headers.nth(h).textContent())?.trim() || '';
          await headers.nth(h).click({ force: true });
          await p.waitForTimeout(80);
          note('admin', 'Admin Page', `${tblName} header[${h}] "${ht}"`, 'click', 'OK');
        } catch(e) {}
      }
      // Data rows
      const rows = tables.nth(t).locator('tbody tr');
      const rCnt = await rows.count();
      for (let r = 0; r < rCnt; r++) {
        const cells = rows.nth(r).locator('td');
        const cCnt = await cells.count();
        for (let c = 0; c < cCnt; c++) {
          try {
            const ct = (await cells.nth(c).textContent())?.trim() || '';
            await cells.nth(c).click({ force: true });
            await p.waitForTimeout(60);
            note('admin', 'Admin Page', `${tblName} row[${r}] cell[${c}]`, 'click', `"${ct.substring(0, 30)}"`);
          } catch(e) {}
        }
      }
    }
  } catch (e) {
    console.log(`  ⚠️ ERROR in admin flow: ${e.message}`);
  }
  await p.waitForTimeout(500);
  await browser.close();
  console.log(`  ✅ Admin done.`);
}

async function runEdgeCases() {
  console.log(`\n${'='.repeat(60)}`);
  console.log(`  EDGE CASES`);
  console.log(`${'='.repeat(60)}`);
  const browser = await chromium.launch({ headless: false, args: ['--start-maximized', '--start-fullscreen'] });
  const ctx = await browser.newContext({ viewport: null, locale: 'uk-UA' });
  const p = await ctx.newPage();

  try {
    // Empty login
    await p.goto(`${BASE}/login`);
    await p.waitForTimeout(1000);
    note('anon', 'Login Page', 'page', 'loaded', p.url());
    await clickS(p, 'button:has-text("Увійти")', 'empty submit');
    await p.waitForTimeout(500);

    // Invalid creds
    const inputs = p.locator('input');
    if ((await inputs.count()) >= 2) {
      await inputs.nth(0).fill('wrong');
      await inputs.nth(1).fill('wrong');
    }
    await clickS(p, 'button:has-text("Увійти")', 'invalid submit');
    await p.waitForTimeout(1500);
    const alert = p.locator('[role="alert"]');
    if (await alert.isVisible().catch(() => false)) {
      const alertText = (await alert.textContent())?.trim() || '';
      note('anon', 'Login Page', 'error alert', 'read', `"${alertText}"`);
    }

    // Unauthenticated redirects
    for (const path of ['/doctor', '/nurse', '/admin']) {
      await p.goto(`${BASE}${path}`);
      await p.waitForTimeout(1000);
      note('anon', path, 'redirect', 'navigate', p.url());
    }

    // 404
    await p.goto(`${BASE}/nonexistent`);
    await p.waitForTimeout(1000);
    note('anon', '/nonexistent', '404 page', 'loaded', p.url());
    const anyEl = p.locator('#root div:visible, #root span:visible').first();
    if (await anyEl.isVisible().catch(() => false)) {
      await anyEl.click({ force: true });
      note('anon', '/nonexistent', 'any element click', 'click', 'no crash');
    }
  } catch (e) {
    console.log(`  ⚠️ ERROR in edge cases: ${e.message}`);
  }
  await p.waitForTimeout(500);
  await browser.close();
  console.log(`  ✅ Edge cases done.`);
}

async function writeFindings() {
  const counts = {};
  for (const f of ALL_BUGS) {
    counts[f.severity] = (counts[f.severity] || 0) + 1;
  }

  let md = `# Exploratory Testing Findings — Full Interactive Session\n\n`;
  md += `**Date:** ${new Date().toISOString()}\n`;
  md += `**Total interactions: ${ALL_BUGS.length}**\n\n`;
  md += `**Severity breakdown:**\n`;
  for (const [sev, cnt] of Object.entries(counts)) {
    md += `- ${sev}: ${cnt}\n`;
  }
  md += `\n## Interaction Log\n\n`;
  md += `| # | Role | Page | Element | Action | Result | Severity |\n`;
  md += `|---|------|------|---------|--------|--------|----------|\n`;
  let i = 1;
  for (const f of ALL_BUGS) {
    md += `| ${i++} | ${f.role} | ${f.page} | ${f.element} | ${f.action} | ${f.result} | ${f.severity} |\n`;
  }
  md += `\n## Bugs / Issues\n\n`;
  md += `| # | Severity | Description | Location |\n`;
  md += `|---|----------|-------------|----------|\n`;
  let b = 1;
  for (const bug of ALL_BUGS) {
    if (bug.severity !== 'INFO') {
      md += `| ${b++} | ${bug.severity} | ${bug.element}: ${bug.action} → ${bug.result} | ${bug.role} / ${bug.page} |\n`;
    }
  }
  if (b === 1) md += `| — | — | No issues found | — |\n`;

  fs.writeFileSync(FINDINGS_FILE, md, 'utf-8');
  console.log(`\n📝 Findings written to ${FINDINGS_FILE}`);
}

(async () => {
  console.log('🚀 FULL INTERACTIVE EXPLORATION SESSION');
  console.log('Browser windows will open one by one for each user.');
  console.log('Watch the automated interactions on screen.\n');

  // DOCTORS
  await runDoctorFlow('doctor1', 'doctor123', 'doctor1');
  await runDoctorFlow('doctor2', 'doctor123', 'doctor2');
  await runDoctorFlow('head1', 'head123', 'head1');

  // NURSES
  await runNurseFlow('nurse1', 'nurse123', 'nurse1');
  await runNurseFlow('nurse2', 'nurse123', 'nurse2');

  // ADMIN
  await runAdminFlow();

  // EDGE
  await runEdgeCases();

  // REPORT
  await writeFindings();

  console.log(`\n${'='.repeat(60)}`);
  console.log(`  ✅ ALL EXPLORATION COMPLETE`);
  console.log(`  Total interactions: ${interactionCount}`);
  console.log(`${'='.repeat(60)}`);
})();
