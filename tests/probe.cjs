const { request } = require('@playwright/test');
(async () => {
  const ctx = await request.newContext();
  const resp = await ctx.post('http://localhost:8085/api/auth/login', { data: { login: 'doctor1', password: 'doctor123' } });
  const text = await resp.text();
  const buf = await resp.body();
  const asUtf8 = buf.toString('utf-8');
  const m = text.match(/"fullName":"([^"]+)"/);
  const m2 = asUtf8.match(/"fullName":"([^"]+)"/);
  console.log('text() fullName:', m && m[1]);
  console.log('utf8  fullName:', m2 && m2[1]);
  await ctx.dispose();
})().catch((e) => console.log('ERR', e.message));
