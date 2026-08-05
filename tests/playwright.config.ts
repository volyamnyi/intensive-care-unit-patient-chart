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
      use: {
        ...devices['Desktop Chrome'],
      },
      testMatch: ['**/auth/login.spec.ts', '**/auth/logout.spec.ts'],
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
      name: 'prosthetist-chromium',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/prosthetist.json',
      },
      testMatch: '**/prosthetics/*.spec.ts',
    },
    {
      name: 'prosthetadmin-chromium',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/prosthetadmin.json',
      },
      testMatch: '**/prosthetics/*.spec.ts',
    },
    {
      name: 'api-chromium',
      use: { ...devices['Desktop Chrome'] },
      testMatch: '**/api/*.spec.ts',
    },
  ],
});
