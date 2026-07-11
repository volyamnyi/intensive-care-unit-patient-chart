import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { theme } from '../styles/theme';
import { AuthProvider } from '../services/AuthContext';
import LoginPage from '../pages/LoginPage';

vi.mock('../api/endpoints', () => ({
  authApi: {
    login: vi.fn(),
  },
  userApi: {
    getMe: vi.fn().mockRejectedValue(new Error('No token')),
  },
}));

function renderLoginPage() {
  return render(
    <ThemeProvider theme={theme}>
      <BrowserRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>,
  );
}

beforeEach(() => {
  localStorage.clear();
  vi.clearAllMocks();
});

describe('LoginPage', () => {
  it('renders login form with fields and button', () => {
    renderLoginPage();
    expect(screen.getByRole('textbox', { name: /Логін/ })).toBeInTheDocument();
    expect(screen.getByLabelText(/Пароль/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Увійти' })).toBeInTheDocument();
  });

  it('shows error message on failed login', async () => {
    const { authApi } = await import('../api/endpoints');
    vi.mocked(authApi.login).mockRejectedValue(new Error('Unauthorized'));

    const user = userEvent.setup();
    renderLoginPage();

    await user.type(screen.getByRole('textbox', { name: /Логін/ }), 'wrong');
    await user.type(screen.getByLabelText(/Пароль/), 'wrong');
    await user.click(screen.getByRole('button', { name: 'Увійти' }));

    await waitFor(() => {
      expect(screen.getByText('Невірний логін або пароль')).toBeInTheDocument();
    });
  });

  it('redirects on successful login', async () => {
    const { authApi, userApi } = await import('../api/endpoints');
    vi.mocked(authApi.login).mockResolvedValue({
      data: { token: 'jwt', login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: 'd@h.ua' },
    } as any);
    vi.mocked(userApi.getMe).mockResolvedValue({
      data: { id: 1, login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: 'd@h.ua', specialityCode: '', specialityName: '', phone: '' },
    } as any);

    const originalLocation = window.location;
    Object.defineProperty(window, 'location', {
      value: { ...originalLocation, href: '' },
      writable: true,
    });

    const user = userEvent.setup();
    renderLoginPage();

    await user.type(screen.getByRole('textbox', { name: /Логін/ }), 'doctor1');
    await user.type(screen.getByLabelText(/Пароль/), 'doctor123');
    await user.click(screen.getByRole('button', { name: 'Увійти' }));

    await waitFor(() => {
      expect(window.location.href).toBe('/');
    });
  });
});
