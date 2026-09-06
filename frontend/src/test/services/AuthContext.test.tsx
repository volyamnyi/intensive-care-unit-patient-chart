import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from '../../services/AuthContext';

const { mockLoginFn, mockGetMe, mockLogoutFn, mockGetMyPermissions } = vi.hoisted(() => ({
  mockLoginFn: vi.fn(),
  mockGetMe: vi.fn(),
  mockLogoutFn: vi.fn(),
  mockGetMyPermissions: vi.fn().mockResolvedValue({ data: [] }),
}));

vi.mock('../../api/platform', () => ({
  authApi: { login: mockLoginFn, logout: mockLogoutFn },
  userApi: { getMe: mockGetMe, getMyPermissions: mockGetMyPermissions },
}));

function TestComponent() {
  const { user, isAuthenticated, hasRole, hasPermission, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="auth">{isAuthenticated ? 'yes' : 'no'}</span>
      <span data-testid="role">{user?.role ?? 'null'}</span>
      <span data-testid="hasRoleDoc">{hasRole('DOCTOR') ? 'true' : 'false'}</span>
      <span data-testid="hasRoleNurse">{hasRole('NURSE') ? 'true' : 'false'}</span>
      <span data-testid="hasPermIcu">{hasPermission('MODULE_ICU_ACCESS') ? 'true' : 'false'}</span>
      <button data-testid="login-btn" onClick={() => login({ login: 'doctor1', password: 'pass' })}>Login</button>
      <button data-testid="logout-btn" onClick={logout}>Logout</button>
    </div>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('AuthContext', () => {
  it('shows unauthenticated state when getMe fails', async () => {
    mockGetMe.mockRejectedValue(new Error('no session'));
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('no'));
    expect(screen.getByTestId('role')).toHaveTextContent('null');
  });

  it('restores session from storage on mount', async () => {
    localStorage.setItem('auth:session', '1');
    mockGetMe.mockResolvedValue({ data: { id: '1', login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: '' } });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('yes'));
    expect(screen.getByTestId('role')).toHaveTextContent('DOCTOR');
  });

  it('login sets user from response', async () => {
    mockGetMe.mockRejectedValue(new Error('no session'));
    mockLoginFn.mockResolvedValue({
      data: { userId: '1', login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: 'doc@test.com' },
    });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('no'));
    await userEvent.click(screen.getByTestId('login-btn'));
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('yes'));
    expect(screen.getByTestId('role')).toHaveTextContent('DOCTOR');
  });

  it('login loads permissions solely from getMyPermissions (matrix)', async () => {
    mockGetMe.mockRejectedValue(new Error('no session'));
    mockGetMyPermissions.mockResolvedValue({ data: ['MODULE_ICU_ACCESS'] });
    mockLoginFn.mockResolvedValue({
      data: { userId: '1', login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: 'doc@test.com' },
    });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('no'));
    await userEvent.click(screen.getByTestId('login-btn'));
    await waitFor(() => expect(screen.getByTestId('hasPermIcu')).toHaveTextContent('true'));
    expect(mockGetMyPermissions).toHaveBeenCalled();
    expect(screen.getByTestId('hasPermIcu')).toHaveTextContent('true');
  });

  it('logout calls api and clears user', async () => {
    localStorage.setItem('auth:session', '1');
    mockLogoutFn.mockResolvedValue(undefined);
    mockGetMe.mockResolvedValue({ data: { id: '1', login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: '' } });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('role')).toHaveTextContent('DOCTOR'));
    await userEvent.click(screen.getByTestId('logout-btn'));
    expect(mockLogoutFn).toHaveBeenCalled();
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('no'));
    expect(screen.getByTestId('role')).toHaveTextContent('null');
  });

  it('hasRole checks user role', async () => {
    mockGetMe.mockRejectedValue(new Error('no session'));
    mockLoginFn.mockResolvedValue({
      data: { userId: '2', login: 'nurse1', fullName: 'Nurse', role: 'NURSE', email: 'n@t.com' },
    });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('no'));
    await userEvent.click(screen.getByTestId('login-btn'));
    await waitFor(() => expect(screen.getByTestId('hasRoleDoc')).toHaveTextContent('false'));
    expect(screen.getByTestId('hasRoleNurse')).toHaveTextContent('true');
  });

  it('guest login authenticates with zero permissions', async () => {
    mockGetMe.mockRejectedValue(new Error('no session'));
    mockGetMyPermissions.mockResolvedValue({ data: [] });
    mockLoginFn.mockResolvedValue({
      data: { userId: '9', login: 'ad.newbie', fullName: 'Ad Newbie', role: 'GUEST', email: '' },
    });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('yes'));
    expect(screen.getByTestId('role')).toHaveTextContent('GUEST');
    expect(screen.getByTestId('hasRoleDoc')).toHaveTextContent('false');
    expect(screen.getByTestId('hasPermIcu')).toHaveTextContent('false');
  });

  it('permissions load failure degrades to empty set without logout', async () => {
    mockGetMe.mockRejectedValue(new Error('no session'));
    mockGetMyPermissions.mockRejectedValue(new Error('forbidden'));
    mockLoginFn.mockResolvedValue({
      data: { userId: '1', login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: 'doc@test.com' },
    });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('yes'));
    expect(screen.getByTestId('hasPermIcu')).toHaveTextContent('false');
  });

  it('stores only the session flag, never token or password', async () => {
    localStorage.clear();
    mockGetMe.mockRejectedValue(new Error('no session'));
    mockGetMyPermissions.mockResolvedValue({ data: [] });
    mockLoginFn.mockResolvedValue({
      data: { userId: '1', login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: 'doc@test.com' },
    });
    render(<AuthProvider><TestComponent /></AuthProvider>);
    await userEvent.click(screen.getByTestId('login-btn'));
    await waitFor(() => expect(screen.getByTestId('auth')).toHaveTextContent('yes'));
    expect(localStorage.getItem('auth:session')).toBe('1');
    expect(localStorage.length).toBe(1);
  });

  it('useAuth throws outside provider', () => {
    expect(() => render(<TestComponent />)).toThrow('useAuth must be used within AuthProvider');
  });
});
