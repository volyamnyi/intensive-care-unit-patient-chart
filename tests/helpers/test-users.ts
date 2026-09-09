/**
 * Test-credential helpers (real-MIS cutover, Phase 11).
 *
 * Every E2E credential is read exclusively from the shell environment by
 * name (`APP_TEST_USERNAME1..9`, `APP_TEST_PASSWORD1..9`) — the same
 * variables `UserSeedService` provisions LOCAL rows from. Values are never
 * hardcoded, never logged, and never fall back to a literal: a missing
 * variable fails loudly so a misconfigured runner cannot silently
 * authenticate against the wrong identity.
 *
 * Slot order matches `AD_ROLE_MATRIX` in `ad-auth.ts` and the seed order in
 * `UserSeedService`: 1/2 = DOCTOR, 3/4 = NURSE, 5 = HEAD_OF_DEPARTMENT,
 * 6 = ADMINISTRATOR, 7/8 = PROSTHETIST, 9 = PROSTHETICS_ADMINISTRATOR.
 */

export interface TestUser {
  login: string;
  password: string;
}

/**
 * Credentials for slot i (1-based) straight from the environment.
 * Throws a descriptive error when either variable is absent — no fallback,
 * never returns `undefined`, so a missing value is a hard failure instead of
 * an accidental login as `undefined`.
 */
export function testUser(i: number): TestUser {
  const login = process.env[`APP_TEST_USERNAME${i}`];
  const password = process.env[`APP_TEST_PASSWORD${i}`];
  if (!login || !password) {
    throw new Error(
      `Test credentials for slot ${i} are not configured: ` +
        `APP_TEST_USERNAME${i} and APP_TEST_PASSWORD${i} must be present in the environment.`,
    );
  }
  return { login, password };
}

// ---- Named role accessors (slot-mapped) ----

/** DOCTOR (slot 1). */
export function doctor(): TestUser {
  return testUser(1);
}

/** Second DOCTOR (slot 2). */
export function doctor2(): TestUser {
  return testUser(2);
}

/** NURSE (slot 3). */
export function nurse(): TestUser {
  return testUser(3);
}

/** Second NURSE (slot 4). */
export function nurse2(): TestUser {
  return testUser(4);
}

/** HEAD_OF_DEPARTMENT (slot 5). */
export function hod(): TestUser {
  return testUser(5);
}

/** ADMINISTRATOR (slot 6). */
export function adminUser(): TestUser {
  return testUser(6);
}

/** PROSTHETIST (slot 7). */
export function prosthetist(): TestUser {
  return testUser(7);
}

/** Second PROSTHETIST (slot 8). */
export function prosthetist2(): TestUser {
  return testUser(8);
}

/** PROSTHETICS_ADMINISTRATOR (slot 9). */
export function prostheticsAdmin(): TestUser {
  return testUser(9);
}
