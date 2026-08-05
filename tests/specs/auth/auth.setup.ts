import { test as setup } from '@playwright/test';
import { mkdirSync, existsSync } from 'fs';

const AUTH_BASE = '.auth';
if (!existsSync(AUTH_BASE)) mkdirSync(AUTH_BASE, { recursive: true });

const USERS = [
  { login: 'doctor1', password: 'doctor123', file: 'doctor.json' },
  { login: 'nurse1', password: 'nurse123', file: 'nurse.json' },
  { login: 'head1', password: 'head123', file: 'hod.json' },
  { login: 'admin', password: 'admin123', file: 'admin.json' },
  { login: 'prosthetist1', password: 'doctor123', file: 'prosthetist.json' },
  { login: 'prosthetics_admin1', password: 'doctor123', file: 'prosthetadmin.json' },
];

for (const user of USERS) {
  setup(`authenticate as ${user.login}`, async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Логін').fill(user.login);
    await page.getByLabel('Пароль').fill(user.password);
    await page.getByRole('button', { name: 'Увійти' }).click();
    await page.waitForURL(/\/(doctor|nurse|admin|select|prescriptions|prosthetics)/, { timeout: 30000 });
    await page.context().storageState({ path: `${AUTH_BASE}/${user.file}` });
  });
}
