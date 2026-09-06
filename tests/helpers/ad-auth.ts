import type { APIRequestContext } from '@playwright/test';

/**
 * Active Directory test-identity helpers (issue #250).
 *
 * Credentials come exclusively from shell environment variables *by name*
 * (`APP_TEST_USERNAME1..9`, `APP_TEST_PASSWORD1..9`); values are never
 * logged, never committed, and never appear in screenshots, traces, reports,
 * or CI artifacts. All flows authenticate against the real application
 * login endpoint — there are no auth mocks anywhere in this suite.
 *
 * Trace safety: AD setup and the AD spec run with `trace: 'off'`, so API
 * request payloads and UI fills never land in trace files. Screenshots only
 * ever show masked password dots.
 */

export const AD_API = 'http://localhost:8085/api';

/**
 * Local role slots for AD identities 1..9. Roles are assigned to the LOCAL
 * rows by the setup (decision D4: the directory never authorizes); identity 9
 * is additionally used as the deprovisioning probe (demoted back to GUEST).
 */
export const AD_ROLE_MATRIX: { role: string; state: string }[] = [
  { role: 'DOCTOR', state: 'ad-doctor1.json' },
  { role: 'DOCTOR', state: 'ad-doctor2.json' },
  { role: 'NURSE', state: 'ad-nurse1.json' },
  { role: 'NURSE', state: 'ad-nurse2.json' },
  { role: 'HEAD_OF_DEPARTMENT', state: 'ad-hod.json' },
  { role: 'ADMINISTRATOR', state: 'ad-admin.json' },
  { role: 'PROSTHETIST', state: 'ad-prosthetist1.json' },
  { role: 'PROSTHETIST', state: 'ad-prosthetist2.json' },
  { role: 'PROSTHETICS_ADMINISTRATOR', state: 'ad-ptadmin.json' },
];

export interface AdCredentials {
  login: string;
  password: string;
}

/** Reads a variable by name; blank counts as absent. The value is never logged. */
export function adEnv(name: string): string | undefined {
  const value = process.env[name];
  return value && value.trim() !== '' ? value : undefined;
}

/** Credentials for identity i (1-based), or null when the pair is not configured. */
export function adCredentials(i: number): AdCredentials | null {
  const login = adEnv(`APP_TEST_USERNAME${i}`);
  const password = adEnv(`APP_TEST_PASSWORD${i}`);
  if (!login || !password) return null;
  return { login, password };
}

/** True when at least the first AD identity is configured (spec gate). */
export function hasAdEnv(): boolean {
  return adCredentials(1) !== null;
}

/** Raw login call; the caller asserts the status. Password stays in memory only. */
export async function apiLogin(request: APIRequestContext, login: string, password: string) {
  return request.post(`${AD_API}/auth/login`, {
    data: { login, password },
    failOnStatusCode: false,
  });
}

/** Extracts the `jwt` cookie value from a login response, or null. */
export function jwtFromLogin(response: { headers: () => Record<string, string> }): string | null {
  const setCookie = response.headers()['set-cookie'] ?? '';
  const match = /(?:^|;\s*)jwt=([^;]+)/.exec(setCookie);
  return match ? match[1] : null;
}

/** Seed-admin (LOCAL) bearer headers for admin API calls. */
export async function seedAdminHeaders(
  request: APIRequestContext,
): Promise<Record<string, string>> {
  const res = await apiLogin(request, 'admin', 'admin123');
  if (!res.ok()) throw new Error(`seed admin login failed with status ${res.status()}`);
  const token = ((await res.json()) as { token?: string }).token;
  if (!token) throw new Error('seed admin login returned no token');
  return { Authorization: `Bearer ${token}` };
}

interface AdminUser {
  id: number;
  login: string;
  role: string;
}

/** Finds a local user id by login via the admin directory (read-only). */
export async function findUserId(
  request: APIRequestContext,
  adminHeaders: Record<string, string>,
  login: string,
): Promise<number | null> {
  const res = await request.get(`${AD_API}/admin/users`, { headers: adminHeaders });
  if (!res.ok()) throw new Error(`admin user lookup failed with status ${res.status()}`);
  const users = (await res.json()) as AdminUser[];
  return users.find((u) => u.login === login)?.id ?? null;
}

/** Sets a local user role (LOCAL row update only — never touches AD). */
export async function setUserRole(
  request: APIRequestContext,
  adminHeaders: Record<string, string>,
  login: string,
  role: string,
): Promise<void> {
  const id = await findUserId(request, adminHeaders, login);
  if (id === null) throw new Error(`cannot set role ${role}: no local row for ${login}`);
  const res = await request.put(`${AD_API}/admin/users/${id}/role`, {
    headers: { ...adminHeaders, 'Content-Type': 'application/json' },
    data: { role },
  });
  if (!res.ok()) throw new Error(`role update to ${role} failed with status ${res.status()}`);
}

/**
 * Logs in via AD and converges the LOCAL row to the expected role:
 * first-ever login provisions GUEST, then the seed admin promotes.
 * Returns the `jwt` cookie value. Idempotent across runs.
 */
export async function apiLoginEnsureRole(
  request: APIRequestContext,
  login: string,
  password: string,
  role: string,
): Promise<string> {
  const first = await apiLogin(request, login, password);
  if (first.status() !== 200) {
    throw new Error(`AD login for ${login} failed with status ${first.status()}`);
  }
  const body = (await first.json()) as { role?: string };
  if (body.role === role) {
    const cookie = jwtFromLogin(first);
    if (!cookie) throw new Error(`AD login for ${login} issued no jwt cookie`);
    return cookie;
  }
  const adminHeaders = await seedAdminHeaders(request);
  await setUserRole(request, adminHeaders, login, role);
  const second = await apiLogin(request, login, password);
  if (second.status() !== 200) {
    throw new Error(`AD re-login for ${login} failed with status ${second.status()}`);
  }
  const cookie = jwtFromLogin(second);
  if (!cookie) throw new Error(`AD re-login for ${login} issued no jwt cookie`);
  return cookie;
}
