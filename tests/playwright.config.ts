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
      use: {
        trace: 'off',
      },
    },
    {
      name: 'login-chromium',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
      },
      testMatch: ['**/auth/login.spec.ts', '**/auth/logout.spec.ts', '**/auth/access-control.spec.ts', '**/auth/role-redirects.spec.ts', '**/auth/ad-login.spec.ts'],
    },
    {
      name: 'doctor-chromium',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/doctor.json',
      },
      testMatch: '**/doctor/*.spec.ts',
    },
    {
      name: 'nurse-chromium',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/nurse.json',
      },
      testMatch: '**/nurse/*.spec.ts',
    },
    {
      name: 'hod-chromium',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/hod.json',
      },
      testMatch: '**/hod/*.spec.ts',
    },
    {
      name: 'admin-chromium',
      dependencies: ['setup'],
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
      workers: 1,
    },
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
        '**/responsive/medication-markers-overflow.spec.ts',
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
        '**/responsive/medication-markers-overflow.spec.ts',
      ],
      fullyParallel: false,
    },
  ],
});
