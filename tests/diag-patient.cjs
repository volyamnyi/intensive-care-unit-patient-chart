const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  page.on('console', (msg) => console.log('[console]', msg.type(), msg.text()));
  page.on('request', (req) => {
    if (req.url().includes('prosthesis')) console.log('[REQ]', req.method(), req.url());
  });
  page.on('response', async (res) => {
    if (res.url().includes('prosthesis')) {
      let body = '';
      try { body = await res.text(); } catch {}
      console.log('[RES]', res.status(), res.url().split('/api')[1], '->', body.substring(0, 200));
    }
  });

  // Login
  await page.goto('http://localhost:5173/login', { waitUntil: 'domcontentloaded' });
  await page.fill('#login', 'prosthetist1');
  await page.fill('#password', 'doctor123');
  await page.getByRole('button', { name: 'Увійти' }).click();
  await page.waitForURL('**/select', { timeout: 10000 }).catch(() => {});
  await page.goto('http://localhost:5173/prosthetics', { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1500);

  // Go to new process -> patient selection
  await page.getByRole('button', { name: /Новий процес/i }).first().click().catch(() => {});
  await page.waitForTimeout(1500);
  console.log('URL after new process:', page.url());

  // Fill search
  const input = page.locator('input[placeholder*="Пошук"]').first();
  await input.fill('Сніжко');
  await page.waitForTimeout(2500);

  const bodyText = await page.locator('body').innerText();
  console.log('--- PAGE TEXT (relevant) ---');
  console.log(bodyText.substring(0, 1200));

  await page.screenshot({ path: 'test-results/diag-patient.png' });
  await browser.close();
})();
