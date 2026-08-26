import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: '.',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: '../playwright-report' }],
    ['line'],
    ...(process.env.CI ? [['allure-playwright']] : []),
  ],
  use: {
    baseURL: 'http://localhost:5173',
    locale: 'uk',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'setup',
      testMatch: '**/*.setup.ts',
      timeout: 60000,
    },
    {
      name: 'login-chromium',
      // access-control.spec.ts runs with per-test storageState (admin/doctor/nurse)
      // so the .auth/*.json files must exist before this project starts.
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
      },
      testMatch: ['**/auth/login.spec.ts', '**/auth/logout.spec.ts', '**/auth/access-control.spec.ts', '**/auth/role-redirects.spec.ts'],
    },
    {
      name: 'api-error-mode-chromium',
      fullyParallel: false,
      use: { ...devices['Desktop Chrome'] },
      testMatch: '**/api/mis-error-scenarios.spec.ts',
    },
    {
      name: 'doctor-chromium',
      dependencies: ['setup', 'api-error-mode-chromium'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/doctor.json',
      },
      testMatch: '**/doctor/*.spec.ts',
    },
    {
      name: 'nurse-chromium',
      dependencies: ['setup', 'api-error-mode-chromium'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/nurse.json',
      },
      testMatch: '**/nurse/*.spec.ts',
    },
    {
      name: 'hod-chromium',
      dependencies: ['setup', 'api-error-mode-chromium'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/hod.json',
      },
      testMatch: '**/hod/*.spec.ts',
    },
    {
      name: 'admin-chromium',
      dependencies: ['setup', 'api-error-mode-chromium'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/admin.json',
      },
      testMatch: '**/admin/*.spec.ts',
    },
    {
      name: 'api-chromium',
      use: { ...devices['Desktop Chrome'] },
      testMatch: '**/api/*.spec.ts',
      testIgnore: '**/api/mis-error-scenarios.spec.ts',
      dependencies: ['api-error-mode-chromium'],
    },
    {
      name: 'prosthetics-chromium',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/prosthetist.json',
      },
      testMatch: '**/prosthetics/*.spec.ts',
      fullyParallel: false,
      // The prosthetics specs share a single seed order and leave active flow instances
      // behind — they must never run concurrently against the same database.
      workers: 1,
    },
    // Responsive UI Phase 6 (issue #165): mobile + tablet smoke projects.
    // Ordered AFTER prosthetics-chromium so mobile-wizard-smoke's runtime flow
    // instance (created on a seed order) can never race the desktop prosthetics
    // specs; it is driven to COMPLETED in afterAll so the "new process" review
    // screen stays unblocked for later projects.
    {
      name: 'responsive-mobile-chromium',
      dependencies: ['setup'],
      use: {
        ...devices['iPhone 13'],
        browserName: 'chromium',
        storageState: '.auth/doctor.json',
      },
      testMatch: [
        '**/responsive/mobile-nav.spec.ts',
        '**/responsive/touch-targets.spec.ts',
        '**/responsive/mobile-wizard-smoke.spec.ts',
      ],
      fullyParallel: false,
    },
    {
      name: 'responsive-tablet-chromium',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 768, height: 1024 },
        hasTouch: true,
        storageState: '.auth/doctor.json',
      },
      testMatch: [
        '**/responsive/no-horizontal-scroll.spec.ts',
        '**/responsive/tablet-dashboard.spec.ts',
        '**/responsive/tablet-clinical-grids.spec.ts',
        '**/responsive/tablet-forms.spec.ts',
        '**/responsive/tablet-admin.spec.ts',
        '**/responsive/tablet-prosthetics.spec.ts',
        '**/responsive/tablet-navigation.spec.ts',
      ],
      fullyParallel: false,
    },
  ],
});
