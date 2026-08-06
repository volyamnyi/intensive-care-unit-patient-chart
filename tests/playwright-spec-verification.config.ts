import { defineConfig, devices } from '@playwright/test';

/**
 * Standalone config for specification verification tests.
 * Does NOT depend on the setup project — tests handle their own authentication.
 */
export default defineConfig({
  testDir: '.',
  fullyParallel: false,
  forbidOnly: false,
  retries: 0,
  workers: 1,
  reporter: [
    ['html', { outputFolder: '../playwright-report/spec-verification', open: 'never' }],
    ['line'],
  ],
  use: {
    baseURL: 'http://localhost:5173',
    locale: 'uk',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'spec-verification-chromium',
      use: {
        ...devices['Desktop Chrome'],
      },
      testMatch: '**/prosthetics-spec-verification.spec.ts',
    },
  ],
});
