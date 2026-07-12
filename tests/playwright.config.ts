import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './specs',
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
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'setup',
      testMatch: '**/*.setup.ts',
    },
    {
      name: 'auth-chromium',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/doctor.json',
      },
      testMatch: '**/auth/*.spec.ts',
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
  ],
});
