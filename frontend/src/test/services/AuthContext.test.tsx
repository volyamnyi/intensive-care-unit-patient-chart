import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from '../../services/AuthContext';

const { mockLoginFn, mockGetMe } = vi.hoisted(() => ({
  mockLoginFn: vi.fn(),
  mockGetMe: vi.fn(),
}));

vi.mock('../../api/endpoints', () => ({
  authApi: { login: mockLoginFn },
  userApi: { getMe: mockGetMe },
}));

function TestComponent() {
  const { user, token, isAuthenticated, hasRole, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="auth">{isAuthenticated ? 'yes' : 'no'}</span>
      <span data-testid="token">{token ?? 'null'}</span>
      <span data-testid="role">{user?.role ?? 'null'}</span>
      <span data-testid="hasRoleDoc">{hasRole('DOCTOR') ? 'true' : 'false'}</span>
      <span data-testid="hasRoleNurse">{hasRole('NURSE') ? 'true' : 'false'}</span>
      <button data-testid="login-btn" onClick={() => login({ login: 'doctor1', password: 'pass' })}>Login</button>
      <button data-testid="logout-btn" onClick={logout}>Logout</button>
    </div>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
});

describe('AuthContext', () => {
  it('provides default unauthenticated state', () => {
    render(<AuthProvider><TestComponent /></AuthProvider>);
    expect(screen.getByTestId('auth')).toHaveTextContent('no');
    expect(screen.getByTestId('token')).toHaveTextContent('null');
    expect(screen.getByTestId('role')).toHaveTextContent('null');
  });

  it('restores token from localStorage on mount', async () => {
    localStorage.setItem('token', 'saved-token');
    mockGetMe.mockResolvedValue({ data: { id: 1, login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: '' } });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('token')).toHaveTextContent('saved-token'));
  });

  it('login sets token and user', async () => {
    mockLoginFn.mockResolvedValue({
      data: { token: 'new-token', login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: 'doc@test.com' },
    });
    mockGetMe.mockResolvedValue({ data: { id: 1, login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: '' } });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await userEvent.click(screen.getByTestId('login-btn'));
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('yes'));
    expect(screen.getByTestId('token')).toHaveTextContent('new-token');
    expect(screen.getByTestId('role')).toHaveTextContent('DOCTOR');
    expect(localStorage.getItem('token')).toBe('new-token');
  });

  it('logout clears token and user', async () => {
    localStorage.setItem('token', 'existing-token');
    mockGetMe.mockResolvedValue({ data: { id: 1, login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: '' } });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('role')).toHaveTextContent('DOCTOR'));
    await userEvent.click(screen.getByTestId('logout-btn'));
    expect(screen.getByTestId('auth')).toHaveTextContent('no');
    expect(screen.getByTestId('token')).toHaveTextContent('null');
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('hasRole checks user role', async () => {
    mockLoginFn.mockResolvedValue({
      data: { token: 't', login: 'nurse1', fullName: 'Nurse', role: 'NURSE', email: 'n@t.com' },
    });
    mockGetMe.mockResolvedValue({ data: { id: 2, login: 'nurse1', fullName: 'Nurse', role: 'NURSE', email: '' } });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await userEvent.click(screen.getByTestId('login-btn'));
    await waitFor(() => expect(screen.getByTestId('hasRoleDoc')).toHaveTextContent('false'));
    expect(screen.getByTestId('hasRoleNurse')).toHaveTextContent('true');
  });

  it('useAuth throws outside provider', () => {
    expect(() => render(<TestComponent />)).toThrow('useAuth must be used within AuthProvider');
  });
});
