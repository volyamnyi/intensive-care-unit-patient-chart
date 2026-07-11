import { test, expect } from '@playwright/test';
import { API, USERS, getToken, authed, MIS_PATIENTS } from '../fixtures/env.js';

// Helper: obtain a token for a given user key.
async function tokenFor(request, key) {
  const { token } = await getToken(request, USERS[key].login, USERS[key].password);
  return token;
}

// Helper: create a card + active day + prescription owned by doctor1 for testing.
async function seedDoctorObjects(request) {
  const dt = await tokenFor(request, 'doctor1');
  const cardResp = await request.post(`${API}/icu-cards`, {
    headers: { Authorization: `Bearer ${dt}` },
    data: {
      patientId: MIS_PATIENTS.sidorenko.id,
      patientName: MIS_PATIENTS.sidorenko.name,
      medicalCardNumber: MIS_PATIENTS.sidorenko.ext,
      diagnosis: `API seed ${Date.now()}`,
      apacheIi: 10,
      sofa: 4,
    },
  });
  const card = await cardResp.json();
  const day = (card.icuDays || []).find((d) => d.status === 'ACTIVE') || card.icuDays?.[0];
  const prescResp = await request.post(`${API}/prescriptions/by-card/${card.id}`, {
    headers: { Authorization: `Bearer ${dt}` },
    data: { medication: 'Saline', dose: '500 мл', route: 'IV', frequency: 'once', startHour: 8, endHour: 8 },
  });
  const presc = await prescResp.json();
  return { dt, cardId: card.id, dayId: day?.id, prescId: presc.id };
}

test.describe('API — Authentication', () => {
  test('TC-API-01 valid login returns token and role (doctor)', async ({ request }) => {
    const resp = await request.post(`${API}/auth/login`, { data: { login: 'doctor1', password: 'doctor123' } });
    expect(resp.status()).toBe(200);
    const body = await resp.json();
    expect(body.token).toBeTruthy();
    expect(body.role).toBe('DOCTOR');
    expect(body.login).toBe('doctor1');
  });

  for (const [key, u] of Object.entries(USERS)) {
    test(`TC-API-02 valid login for ${u.login} (${u.role})`, async ({ request }) => {
      const resp = await request.post(`${API}/auth/login`, { data: { login: u.login, password: u.password } });
      expect(resp.status()).toBe(200);
      const body = await resp.json();
      expect(body.role).toBe(u.role);
      expect(body.fullName).toBe(u.fullName);
    });
  }

  test('TC-API-03 invalid password returns 401', async ({ request }) => {
    const resp = await request.post(`${API}/auth/login`, { data: { login: 'doctor1', password: 'wrong' } });
    expect(resp.status()).toBe(401);
  });

  test('TC-API-04 unknown user returns 401', async ({ request }) => {
    const resp = await request.post(`${API}/auth/login`, { data: { login: 'ghost', password: 'x' } });
    expect(resp.status()).toBe(401);
  });

  test('TC-API-05 missing credentials returns 401', async ({ request }) => {
    const resp = await request.post(`${API}/auth/login`, { data: {} });
    expect(resp.status()).toBe(401);
  });
});

test.describe('API — Authorization / token handling', () => {
  test('TC-API-06 request without token returns 403', async ({ request }) => {
    const resp = await request.get(`${API}/icu-cards/active`);
    expect(resp.status()).toBe(403);
  });

  test('TC-API-07 request with invalid token returns 403', async ({ request }) => {
    const resp = await request.get(`${API}/icu-cards/active`, {
      headers: { Authorization: 'Bearer not.a.real.token' },
    });
    expect(resp.status()).toBe(403);
  });

  test('TC-API-08 malformed Authorization header returns 403', async ({ request }) => {
    const resp = await request.get(`${API}/icu-cards/active`, {
      headers: { Authorization: 'Bearer' },
    });
    expect(resp.status()).toBe(403);
  });
});

test.describe('API — Patients (MIS)', () => {
  test('TC-API-09 patient search allowed for doctor', async ({ request }) => {
    const t = await tokenFor(request, 'doctor1');
    const resp = await authed(request, 'GET', '/patients/search', { token: t, params: { name: 'Петренко' }, expectedStatus: 200 });
    const body = await resp.json();
    expect(Array.isArray(body)).toBe(true);
  });

  test('TC-API-10 patient search allowed for nurse', async ({ request }) => {
    const t = await tokenFor(request, 'nurse1');
    const resp = await authed(request, 'GET', '/patients/search', { token: t, params: { name: 'Коваленко' }, expectedStatus: 200 });
    expect(Array.isArray(await resp.json())).toBe(true);
  });

  test('TC-API-11 patient search allowed for HOD', async ({ request }) => {
    const t = await tokenFor(request, 'head1');
    const resp = await authed(request, 'GET', '/patients/search', { token: t, params: { name: 'Сидоренко' }, expectedStatus: 200 });
    expect(Array.isArray(await resp.json())).toBe(true);
  });

  test('TC-API-12 patient search allowed for admin', async ({ request }) => {
    const t = await tokenFor(request, 'admin');
    const resp = await authed(request, 'GET', '/patients/search', { token: t, params: { name: 'Петренко' }, expectedStatus: 200 });
    expect(Array.isArray(await resp.json())).toBe(true);
  });

  test('TC-API-13 patient search without token returns 403', async ({ request }) => {
    const resp = await request.get(`${API}/patients/search`);
    expect(resp.status()).toBe(403);
  });

  test('TC-API-14 known patient by id returns 200', async ({ request }) => {
    const t = await tokenFor(request, 'doctor1');
    await authed(request, 'GET', `/patients/${MIS_PATIENTS.petrenko.id}`, { token: t, expectedStatus: 200 });
  });

  test('TC-API-15 unknown patient by id returns 404', async ({ request }) => {
    const t = await tokenFor(request, 'doctor1');
    await authed(request, 'GET', '/patients/999999', { token: t, expectedStatus: 404 });
  });
});

test.describe('API — ICU Cards', () => {
  test('TC-API-16 doctor can create ICU card', async ({ request }) => {
    const t = await tokenFor(request, 'doctor1');
    const resp = await request.post(`${API}/icu-cards`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { patientId: MIS_PATIENTS.petrenko.id, patientName: MIS_PATIENTS.petrenko.name, medicalCardNumber: MIS_PATIENTS.petrenko.ext, diagnosis: 'Test', apacheIi: 8, sofa: 3 },
    });
    expect(resp.status()).toBe(200);
    const card = await resp.json();
    expect(card.id).toBeTruthy();
    expect(card.status).toBe('ACTIVE');
  });

  test('TC-API-17 HOD can create ICU card', async ({ request }) => {
    const t = await tokenFor(request, 'head1');
    const resp = await request.post(`${API}/icu-cards`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { patientId: MIS_PATIENTS.kovalenko.id, patientName: MIS_PATIENTS.kovalenko.name, medicalCardNumber: MIS_PATIENTS.kovalenko.ext, diagnosis: 'HOD', apacheIi: 8, sofa: 3 },
    });
    expect(resp.status()).toBe(200);
  });

  test('TC-API-18 nurse cannot create ICU card (403)', async ({ request }) => {
    const t = await tokenFor(request, 'nurse1');
    const resp = await request.post(`${API}/icu-cards`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { patientId: MIS_PATIENTS.petrenko.id, patientName: MIS_PATIENTS.petrenko.name, medicalCardNumber: MIS_PATIENTS.petrenko.ext, diagnosis: 'N', apacheIi: 1, sofa: 1 },
    });
    expect(resp.status()).toBe(403);
  });

  test('TC-API-19 admin cannot create ICU card (403)', async ({ request }) => {
    const t = await tokenFor(request, 'admin');
    const resp = await request.post(`${API}/icu-cards`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { patientId: MIS_PATIENTS.petrenko.id, patientName: MIS_PATIENTS.petrenko.name, medicalCardNumber: MIS_PATIENTS.petrenko.ext, diagnosis: 'A', apacheIi: 1, sofa: 1 },
    });
    expect(resp.status()).toBe(403);
  });

  test('TC-API-20 doctor can list active cards; response is valid JSON', async ({ request }) => {
    const t = await tokenFor(request, 'doctor1');
    const resp = await authed(request, 'GET', '/icu-cards/active', { token: t, expectedStatus: 200 });
    const body = await resp.json();
    expect(Array.isArray(body)).toBe(true);
    expect(body.length).toBeGreaterThan(0);
  });

  test('TC-API-21 all clinical roles can read active cards', async ({ request }) => {
    for (const key of ['doctor1', 'nurse1', 'head1', 'admin']) {
      const t = await tokenFor(request, key);
      const resp = await authed(request, 'GET', '/icu-cards/active', { token: t, expectedStatus: 200 });
      expect(Array.isArray(await resp.json())).toBe(true);
    }
  });
});

test.describe('API — ICU Days, vitals, scales, balance', () => {
  let seed;
  test.beforeAll(async ({ request }) => {
    seed = await seedDoctorObjects(request);
  });

  test('TC-API-22 days by card readable by all clinical roles', async ({ request }) => {
    for (const key of ['doctor1', 'nurse1', 'head1', 'admin']) {
      const t = await tokenFor(request, key);
      await authed(request, 'GET', `/icu-days/by-card/${seed.cardId}`, { token: t, expectedStatus: 200 });
    }
  });

  test('TC-API-23 save vitals (PUT) returns 200', async ({ request }) => {
    const t = await tokenFor(request, 'doctor1');
    const resp = await request.put(`${API}/icu-days/${seed.dayId}/vitals/10`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { systolicBp: 120, diastolicBp: 80, heartRate: 75, spo2: 98, temperature: 36.6, cvp: 5, respiratoryRate: 16 },
    });
    expect(resp.status()).toBe(200);
  });

  test('TC-API-24 get vitals returns stored value', async ({ request }) => {
    const t = await tokenFor(request, 'doctor1');
    const resp = await authed(request, 'GET', `/icu-days/${seed.dayId}/vitals`, { token: t, expectedStatus: 200 });
    const vitals = await resp.json();
    const v = vitals.find((x) => x.hour === 10);
    expect(v).toBeTruthy();
    expect(v.systolicBp).toBe(120);
  });

  test('TC-API-25 save scale assessment returns 200', async ({ request }) => {
    const t = await tokenFor(request, 'doctor1');
    const resp = await request.post(`${API}/icu-days/${seed.dayId}/scales`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { scaleType: 'APACHE_II', score: 14, hour: 8 },
    });
    expect(resp.status()).toBe(200);
  });

  test('TC-API-26 fluid balance returns 200 with numeric fields', async ({ request }) => {
    const t = await tokenFor(request, 'doctor1');
    const resp = await authed(request, 'GET', `/icu-days/${seed.dayId}/balance`, { token: t, expectedStatus: 200 });
    const body = await resp.json();
    expect(body).toHaveProperty('totalIntake');
    expect(body).toHaveProperty('totalOutput');
  });
});

test.describe('API — Day sign-off authorization', () => {
  test('TC-API-27 doctor can sign off a day', async ({ request }) => {
    const seed = await seedDoctorObjects(request);
    const t = await tokenFor(request, 'doctor1');
    await authed(request, 'POST', `/icu-days/${seed.dayId}/sign-off`, { token: t, expectedStatus: 200 });
  });

  test('TC-API-28 HOD can sign off a day', async ({ request }) => {
    const seed = await seedDoctorObjects(request);
    const t = await tokenFor(request, 'head1');
    await authed(request, 'POST', `/icu-days/${seed.dayId}/sign-off`, { token: t, expectedStatus: 200 });
  });

  test('TC-API-29 nurse cannot sign off a day (403)', async ({ request }) => {
    const seed = await seedDoctorObjects(request);
    const t = await tokenFor(request, 'nurse1');
    const resp = await request.post(`${API}/icu-days/${seed.dayId}/sign-off`, {
      headers: { Authorization: `Bearer ${t}` },
    });
    expect(resp.status()).toBe(403);
  });

  test('TC-API-30 admin cannot sign off a day (403)', async ({ request }) => {
    const seed = await seedDoctorObjects(request);
    const t = await tokenFor(request, 'admin');
    const resp = await request.post(`${API}/icu-days/${seed.dayId}/sign-off`, {
      headers: { Authorization: `Bearer ${t}` },
    });
    expect(resp.status()).toBe(403);
  });

  test('TC-API-31 sign-off without token returns 403', async ({ request }) => {
    const seed = await seedDoctorObjects(request);
    const resp = await request.post(`${API}/icu-days/${seed.dayId}/sign-off`);
    expect(resp.status()).toBe(403);
  });
});

test.describe('API — Prescriptions', () => {
  let seed;
  test.beforeAll(async ({ request }) => {
    seed = await seedDoctorObjects(request);
  });

  test('TC-API-32 HOD can create prescription', async ({ request }) => {
    const t = await tokenFor(request, 'head1');
    const resp = await request.post(`${API}/prescriptions/by-card/${seed.cardId}`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { medication: 'Dopamine', dose: '250 мл', route: 'IV', frequency: 'q1h', startHour: 9, endHour: 12 },
    });
    expect(resp.status()).toBe(200);
  });

  test('TC-API-33 nurse cannot create prescription (403)', async ({ request }) => {
    const t = await tokenFor(request, 'nurse1');
    const resp = await request.post(`${API}/prescriptions/by-card/${seed.cardId}`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { medication: 'X', dose: '1', route: 'IV', frequency: 'q1h', startHour: 9, endHour: 12 },
    });
    expect(resp.status()).toBe(403);
  });

  test('TC-API-34 admin cannot create prescription (403)', async ({ request }) => {
    const t = await tokenFor(request, 'admin');
    const resp = await request.post(`${API}/prescriptions/by-card/${seed.cardId}`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { medication: 'X', dose: '1', route: 'IV', frequency: 'q1h', startHour: 9, endHour: 12 },
    });
    expect(resp.status()).toBe(403);
  });

  test('TC-API-35 all clinical roles can list prescriptions', async ({ request }) => {
    for (const key of ['doctor1', 'nurse1', 'head1', 'admin']) {
      const t = await tokenFor(request, key);
      await authed(request, 'GET', `/prescriptions/by-card/${seed.cardId}`, { token: t, expectedStatus: 200 });
    }
  });

  test('TC-API-36 nurse can execute prescription', async ({ request }) => {
    const t = await tokenFor(request, 'nurse1');
    const resp = await request.post(`${API}/prescriptions/${seed.prescId}/execute`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { dayId: seed.dayId, hour: 8, actualVolume: 500 },
    });
    expect(resp.status()).toBe(200);
  });

  test('TC-API-37 doctor can execute prescription', async ({ request }) => {
    const t = await tokenFor(request, 'doctor1');
    const resp = await request.post(`${API}/prescriptions/${seed.prescId}/execute`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { dayId: seed.dayId, hour: 9, actualVolume: 500 },
    });
    expect(resp.status()).toBe(200);
  });

  test('TC-API-38 HOD can stop prescription', async ({ request }) => {
    const t = await tokenFor(request, 'head1');
    const resp = await request.post(`${API}/prescriptions/${seed.prescId}/stop`, {
      headers: { Authorization: `Bearer ${t}` },
    });
    expect(resp.status()).toBe(200);
  });
});

test.describe('API — Users', () => {
  test('TC-API-39 /users/me returns current user for each role', async ({ request }) => {
    for (const key of Object.keys(USERS)) {
      const t = await tokenFor(request, key);
      const resp = await authed(request, 'GET', '/users/me', { token: t, expectedStatus: 200 });
      const body = await resp.json();
      expect(body.login).toBe(USERS[key].login);
      expect(body.role).toBe(USERS[key].role);
    }
  });

  test('TC-API-40 /users/doctors and /users/nurses accessible to clinical roles', async ({ request }) => {
    for (const key of ['doctor1', 'nurse1', 'head1', 'admin']) {
      const t = await tokenFor(request, key);
      await authed(request, 'GET', '/users/doctors', { token: t, expectedStatus: 200 });
      await authed(request, 'GET', '/users/nurses', { token: t, expectedStatus: 200 });
    }
  });

  test('TC-API-41 user endpoints require auth (403)', async ({ request }) => {
    const resp = await request.get(`${API}/users/me`);
    expect(resp.status()).toBe(403);
  });
});
