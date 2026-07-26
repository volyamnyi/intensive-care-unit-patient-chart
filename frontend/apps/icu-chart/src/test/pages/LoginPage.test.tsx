import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from '../../pages/LoginPage';
import { ThemeModeProvider } from '../../styles/ThemeContext';

const mockLogin = vi.fn();

vi.mock('../../services/AuthContext', () => ({
  useAuth: () => ({ login: mockLogin }),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

function renderPage() {
  return render(
    <ThemeModeProvider>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </ThemeModeProvider>
  );
}

describe('LoginPage', () => {
  it('renders login form with all fields', () => {
    renderPage();
    expect(screen.getByRole('textbox', { name: /Логін/ })).toBeInTheDocument();
    expect(screen.getByLabelText(/Пароль/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Увійти' })).toBeInTheDocument();
  });

  it('shows error alert on failed login', async () => {
    mockLogin.mockRejectedValue(new Error('Invalid'));
    renderPage();
    await userEvent.type(screen.getByRole('textbox', { name: /Логін/ }), 'baduser');
    await userEvent.type(screen.getByLabelText(/Пароль/), 'badpass');
    await userEvent.click(screen.getByRole('button', { name: 'Увійти' }));
    await waitFor(() => {
      expect(screen.getByText('Невірний логін або пароль')).toBeInTheDocument();
    });
  });

  it('calls login with credentials on submit', async () => {
    mockLogin.mockResolvedValue(undefined);
    renderPage();
    await userEvent.type(screen.getByRole('textbox', { name: /Логін/ }), 'doctor1');
    await userEvent.type(screen.getByLabelText(/Пароль/), 'doctor123');
    await userEvent.click(screen.getByRole('button', { name: 'Увійти' }));
    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({ login: 'doctor1', password: 'doctor123' });
    });
  });
});
