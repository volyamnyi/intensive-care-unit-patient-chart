import crypto from 'crypto';
import { test, expect } from '../../fixtures/index';

// Wire-level mirror of the backend SEC-B01..B05 red-gate suite (Phase A) over
// raw HTTP (issue #172). Runs in api-chromium against the real filter chain:
// expired/wrong-key/tampered/garbage/unsigned tokens must all be rejected on
// BOTH a cheap identity endpoint and a clinical read, and the login cookie —
// not an Authorization header — must be sufficient for authenticated calls.

const API = 'http://localhost:8085/api';

function b64url(input: Buffer | string): string {
  return Buffer.from(input)
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}

/** HS256 signer used to mint attacker-keyed tokens (never the server secret). */
function signJwt(payload: Record<string, unknown>, secret: string): string {
  const header = b64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = b64url(JSON.stringify(payload));
  const sig = crypto
    .createHmac('sha256', secret)
    .update(`${header}.${body}`)
    .digest('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
  return `${header}.${body}.${sig}`;
}

async function loginToken(request: import('@playwright/test').APIRequestContext): Promise<string> {
  const res = await request.post(`${API}/auth/login`, {
    data: { login: 'doctor1', password: 'doctor123' },
  });
  expect(res.ok()).toBeTruthy();
  return (await res.json()).token as string;
}

const FORGED_TOKENS: Record<string, () => Promise<string>> = {
  'wrong-key signature': async () =>
    signJwt(
      { sub: 'doctor1', role: 'DOCTOR', userId: '11', iat: 1_000_000_000 },
      'attacker-key-not-the-real-secret-value-000',
    ),
  'tampered payload': async () => {
    const real = await loginToken(await getTokenlessRequest());
    const parts = real.split('.');
    const payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));
    payload.role = 'ADMINISTRATOR';
    return `${parts[0]}.${b64url(JSON.stringify(payload))}.${parts[2]}`;
  },
  'unsigned alg-none': async () =>
    `${b64url(JSON.stringify({ alg: 'none' }))}.${b64url(
      JSON.stringify({ sub: 'doctor1', role: 'ADMINISTRATOR', userId: '11' }),
    )}.`,
};

let sharedRequest: import('@playwright/test').APIRequestContext | null = null;
async function getTokenlessRequest() {
  if (!sharedRequest) throw new Error('request fixture not wired');
  return sharedRequest;
}

test.describe('JWT forgery rejected over the wire', () => {
  test.afterEach(() => {
    sharedRequest = null;
  });

  for (const [label, mint] of Object.entries(FORGED_TOKENS)) {
    test(`rejects ${label} on /users/me and /episodes`, async ({ request }) => {
      sharedRequest = request;
      const token = await mint();
      const headers = { Authorization: `Bearer ${token}` };

      const me = await request.get(`${API}/users/me`, { headers });
      const episodes = await request.get(`${API}/episodes`, { headers });

      expect(me.status(), `users/me with ${label}`).toBe(401);
      expect(episodes.status(), `episodes with ${label}`).toBe(401);
    });
  }

  test('rejects garbage and expired-looking tokens', async ({ request }) => {
    const garbageHeaders = { Authorization: 'Bearer not-a-jwt' };
    // Structurally valid token whose exp is far in the past but signed by the
    // attacker key — the server must reject on signature before expiry.
    const stale = await signJwt({ sub: 'x', exp: 1 }, 'another-attacker-key-000000000');
    const staleHeaders = { Authorization: `Bearer ${stale}` };

    for (const headers of [garbageHeaders, staleHeaders]) {
      expect((await request.get(`${API}/users/me`, { headers })).status()).toBe(401);
      expect((await request.get(`${API}/episodes`, { headers })).status()).toBe(401);
    }
  });

  test('login sets an HttpOnly jwt cookie that alone authenticates', async ({ request }) => {
    const loginRes = await request.post(`${API}/auth/login`, {
      data: { login: 'doctor1', password: 'doctor123' },
    });
    expect(loginRes.ok()).toBeTruthy();

    const setCookie = loginRes.headers()['set-cookie'] ?? '';
    expect(setCookie).toContain('jwt=');
    expect(setCookie.toLowerCase()).toContain('httponly');

    const cookieValue = setCookie.split('jwt=')[1].split(';')[0];
    const me = await request.get(`${API}/users/me`, {
      headers: { Cookie: `jwt=${cookieValue}` },
    });
    expect(me.status()).toBe(200);
    expect((await me.json()).login).toBe('doctor1');

    const forged = await request.get(`${API}/users/me`, {
      headers: { Cookie: 'jwt=forged.cookie.value' },
    });
    expect(forged.status()).toBe(401);
  });
});
