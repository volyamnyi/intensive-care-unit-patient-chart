import { defineConfig, devices } from '@playwright/test';

const FRONTEND_BASE = process.env.FRONTEND_BASE || 'http://localhost:5173';
const BACKEND_BASE = process.env.BACKEND_BASE || 'http://localhost:8085';

export default defineConfig({
  testDir: './specs',
  timeout: 30000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['github'],
    ['html', { outputFolder: '../test-results/report', open: 'never', attach: 'on-failure' }],
    ['json', { outputFile: '../test-results/results.json' }],
  ],
  use: {
    baseURL: FRONTEND_BASE,
    apiBaseURL: BACKEND_BASE,
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
