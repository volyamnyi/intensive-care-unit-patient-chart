import { FRONTEND_BASE, API, TOKEN_KEY, USERS, getToken } from './env.js';

// Login via the API and inject the JWT into localStorage so the SPA
// authenticates without driving the login form. Returns role + token.
export async function loginViaApi(page, request, login, password) {
  const { token, body } = await getToken(request, login, password);
  await page.goto(FRONTEND_BASE + '/');
  // NOTE: values used inside page.evaluate run in the browser context, so we
  // pass the storage key explicitly rather than closing over the Node constant.
  await page.evaluate((t) => localStorage.setItem('token', t), token);
  await page.goto(FRONTEND_BASE + '/');
  return { token, role: body.role, fullName: body.fullName, user: body };
}

// Navigate to the app as a given seed user and wait for the role-specific landing page.
export async function gotoAsRole(page, request, key) {
  const u = USERS[key];
  const { token, role } = await loginViaApi(page, request, u.login, u.password);
  const expected = role === 'NURSE' ? /\/nurse/ : role === 'ADMINISTRATOR' ? /\/admin/ : /\/doctor/;
  await page.waitForURL(expected, { timeout: 15000 });
  return { token, role, user: u };
}

// Create a fresh ICU card via the API (doctor/HOD only) and return ids.
export async function createTestCard(request, token, patient) {
  const resp = await request.post(`${API}/icu-cards`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      patientId: patient.id,
      patientName: patient.name,
      medicalCardNumber: patient.ext,
      diagnosis: `Тестова карта ${Date.now()}`,
      apacheIi: 12,
      sofa: 6,
    },
  });
  if (resp.status() !== 200) {
    throw new Error(`createTestCard failed: ${resp.status()} ${await resp.text()}`);
  }
  const card = await resp.json();
  const activeDay = (card.icuDays || []).find((d) => d.status === 'ACTIVE') || card.icuDays?.[0];
  return { cardId: card.id, dayId: activeDay?.id, card };
}
