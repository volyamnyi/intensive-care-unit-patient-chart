import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useEffect } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider, useAuth } from '../../services/AuthContext';
import { ThemeModeProvider } from '../../styles/ThemeContext';

// Phase C frontend security contract (issue #172): the browser-side guards and
// the cookie-based transport are pinned here so regressions fail in
// `frontend-test` before they ever reach an E2E run.

const { mockLoginFn, mockGetMe, mockLogoutFn, mockGetMyPermissions, mockPatientSearch, mockGetByPatient } =
  vi.hoisted(() => ({
    mockLoginFn: vi.fn(),
    mockGetMe: vi.fn(),
    mockLogoutFn: vi.fn(),
    mockGetMyPermissions: vi.fn().mockResolvedValue({ data: [] }),
    mockPatientSearch: vi.fn().mockResolvedValue({ data: [] }),
    mockGetByPatient: vi.fn().mockResolvedValue({ data: [] }),
  }));

vi.mock('../../api/platform', () => ({
  authApi: { login: mockLoginFn, logout: mockLogoutFn },
  userApi: { getMe: mockGetMe, getMyPermissions: mockGetMyPermissions },
  patientApi: { search: mockPatientSearch },
}));

vi.mock('../../api/medication', () => ({
  prescriptionApi: { getByPatient: mockGetByPatient },
}));

function LoginProbe() {
  const { hasPermission, login } = useAuth();
  return (
    <div>
      <span data-testid="lower">{hasPermission('vitals_enter') ? 'yes' : 'no'}</span>
      <span data-testid="upper">{hasPermission('VITALS_ENTER') ? 'yes' : 'no'}</span>
      <span data-testid="mixed">{hasPermission('Vitals_Enter') ? 'yes' : 'no'}</span>
      <button data-testid="login-btn" onClick={() => void login({ login: 'nurse1', password: 'x' })}>
        Login
      </button>
    </div>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
  mockGetMe.mockRejectedValue(new Error('no session'));
  mockGetMyPermissions.mockResolvedValue({ data: [] });
  mockPatientSearch.mockResolvedValue({ data: [] });
  mockGetByPatient.mockResolvedValue({ data: [] });
});

async function renderAndLogin() {
  render(
    <AuthProvider>
      <LoginProbe />
    </AuthProvider>,
  );
  await waitFor(() => expect(screen.getByTestId('lower')).toHaveTextContent('no'));
  await userEvent.click(screen.getByTestId('login-btn'));
}

describe('Axios client cookie-transport contract', () => {
  it('sends credentials (cookie auth) and registers no request interceptor', async () => {
    const mod = await import('../../api/client');
    expect(mod.default.defaults.withCredentials).toBe(true);
    expect(mod.default.interceptors.request.handlers?.filter((h) => h !== null) ?? []).toHaveLength(0);
  });

  it('never injects an Authorization header into defaults', async () => {
    const mod = await import('../../api/client');
    const headers = mod.default.defaults.headers as Record<string, unknown>;
    expect(headers.Authorization).toBeUndefined();
    const common = headers.common as Record<string, unknown> | undefined;
    expect(common?.Authorization ?? headers.Authorization).toBeUndefined();
  });
});

describe('hasPermission is case-insensitive', () => {
  it('matches lower/mixed/upper variants of a matrix code after login', async () => {
    mockGetMyPermissions.mockResolvedValue({ data: ['VITALS_ENTER'] });
    mockLoginFn.mockResolvedValue({
      data: { userId: '13', login: 'nurse1', fullName: 'Nurse', role: 'NURSE', email: '' },
    });

    await renderAndLogin();

    await waitFor(() => expect(screen.getByTestId('lower')).toHaveTextContent('yes'));
    expect(screen.getByTestId('upper')).toHaveTextContent('yes');
    expect(screen.getByTestId('mixed')).toHaveTextContent('yes');
  });
});

describe('localStorage credential contract', () => {
  beforeEach(() => {
    mockLoginFn.mockResolvedValue({
      data: { userId: '13', login: 'nurse1', fullName: 'Nurse', role: 'NURSE', email: '' },
    });
  });

  it('stores ONLY the auth:session flag after login — never a raw token', async () => {
    await renderAndLogin();
    await waitFor(() => expect(localStorage.getItem('auth:session')).toBe('1'));

    expect(localStorage.getItem('jwt')).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
    expect(Object.keys(localStorage)).toEqual(['auth:session']);
  });

  it('clears the session flag when the mount-time getMe fails', async () => {
    localStorage.setItem('auth:session', '1');
    mockGetMe.mockRejectedValue(new Error('expired cookie'));
    render(
      <AuthProvider>
        <LoginProbe />
      </AuthProvider>,
    );
    await waitFor(() => expect(localStorage.getItem('auth:session')).toBeNull());
  });
});

describe('PrescriptionPage list-create gating', () => {
  const fullPatient = {
    id: 1001,
    fullName: 'Петренко Андрій',
    birthDate: '1991-03-14',
    sexCode: 'M',
    address: '',
    phone: '',
    email: '',
    externalId1: '',
    externalId2: '',
    height: 180,
    weight: 80,
    bloodGroup: 'A',
    rhFactor: '+',
    departmentId: 2,
    room: '12',
    bed: '3',
  };

  async function openDrawerWithPerms(perms: string[]) {
    mockGetMyPermissions.mockResolvedValue({ data: perms });
    mockPatientSearch.mockResolvedValue({ data: [fullPatient] });
    mockLoginFn.mockResolvedValue({
      data: { userId: '11', login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: '' },
    });
    const { default: PrescriptionPage } = await import('../../pages/prescription/PrescriptionPage');

    // Silent probe that logs the provider in on mount so the page sees the
    // matrix permissions under test.
    function AutoLogin() {
      const { login } = useAuth();
      useEffect(() => {
        void login({ login: 'doctor1', password: 'x' });
        // eslint-disable-next-line react-hooks/exhaustive-deps
      }, []);
      return null;
    }

    render(
      <MemoryRouter>
        <ThemeModeProvider>
          <AuthProvider>
            <AutoLogin />
            <PrescriptionPage />
          </AuthProvider>
        </ThemeModeProvider>
      </MemoryRouter>,
    );
    await waitFor(() => expect(mockPatientSearch).toHaveBeenCalled(), { timeout: 3000 });
    // Drawer opens via the row's explicit «Відкрити» action button.
    await userEvent.click(await screen.findByRole('button', { name: 'Відкрити' }));
    await screen.findByText(/Листки призначень/);
  }

  it('renders «Створити листок» with PRESCRIPTION_LIST_CREATE', async () => {
    await openDrawerWithPerms(['PRESCRIPTION_LIST_CREATE']);
    expect(await screen.findByRole('button', { name: 'Створити листок' })).toBeInTheDocument();
  });

  it('hides «Створити листок» without the permission', async () => {
    await openDrawerWithPerms([]);
    expect(screen.queryByRole('button', { name: 'Створити листок' })).not.toBeInTheDocument();
    expect(screen.getByText('Немає листків призначень')).toBeInTheDocument();
  });
});
