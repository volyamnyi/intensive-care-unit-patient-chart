import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '../services/AuthContext';
import type { ReactNode } from 'react';

vi.mock('../api/endpoints', () => ({
  authApi: {
    login: vi.fn(),
  },
  userApi: {
    getMe: vi.fn().mockRejectedValue(new Error('No token')),
  },
}));

beforeEach(() => {
  localStorage.clear();
  vi.clearAllMocks();
});

function renderHookWithProvider() {
  return renderHook(() => useAuth(), {
    wrapper: ({ children }: { children: ReactNode }) => (
      <AuthProvider>{children}</AuthProvider>
    ),
  });
}

describe('AuthContext', () => {
  it('starts unauthenticated when no token', () => {
    const { result } = renderHookWithProvider();
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
    expect(result.current.token).toBeNull();
  });

  it('starts with token from localStorage', async () => {
    const { userApi } = await import('../api/endpoints');
    vi.mocked(userApi.getMe).mockResolvedValue({
      data: { id: 1, login: 'doctor1', fullName: 'Doc', role: 'DOCTOR' as const, email: 'd@h.ua', specialityCode: '', specialityName: '', phone: '' },
    } as any);

    localStorage.setItem('token', 'existing-token');
    const { result } = renderHookWithProvider();

    await waitFor(() => {
      expect(result.current.token).toBe('existing-token');
    });
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('login sets token and user', async () => {
    const { authApi, userApi } = await import('../api/endpoints');
    vi.mocked(authApi.login).mockResolvedValue({
      data: { token: 'new-jwt', login: 'doctor1', fullName: 'Олександр Мельник', role: 'DOCTOR', email: 'melnyk@hospital.ua' },
    } as any);
    vi.mocked(userApi.getMe).mockResolvedValue({
      data: { id: 0, login: 'doctor1', fullName: 'Олександр Мельник', role: 'DOCTOR' as const, email: 'melnyk@hospital.ua', specialityCode: '', specialityName: '', phone: '' },
    } as any);

    const { result } = renderHookWithProvider();

    await act(async () => {
      await result.current.login({ login: 'doctor1', password: 'doctor123' });
    });

    expect(result.current.token).toBe('new-jwt');
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user?.login).toBe('doctor1');
    expect(result.current.user?.fullName).toBe('Олександр Мельник');
  });

  it('logout clears token and user', () => {
    localStorage.setItem('token', 'test-jwt');
    const { result } = renderHookWithProvider();
    expect(result.current.isAuthenticated).toBe(true);

    act(() => {
      result.current.logout();
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
    expect(result.current.token).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('hasRole returns true when user has the role', async () => {
    const { authApi, userApi } = await import('../api/endpoints');
    vi.mocked(authApi.login).mockResolvedValue({
      data: { token: 'jwt', login: 'doctor1', fullName: 'Doc', role: 'DOCTOR', email: 'd@h.ua' },
    } as any);
    vi.mocked(userApi.getMe).mockResolvedValue({
      data: { id: 0, login: 'doctor1', fullName: 'Doc', role: 'DOCTOR' as const, email: 'd@h.ua', specialityCode: '', specialityName: '', phone: '' },
    } as any);

    const { result } = renderHookWithProvider();

    await act(async () => {
      await result.current.login({ login: 'doctor1', password: 'doctor123' });
    });

    expect(result.current.hasRole('DOCTOR')).toBe(true);
    expect(result.current.hasRole('NURSE')).toBe(false);
    expect(result.current.hasRole('DOCTOR', 'HEAD_OF_DEPARTMENT')).toBe(true);
  });

  it('hasRole is false when not authenticated', () => {
    const { result } = renderHookWithProvider();
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.hasRole('DOCTOR')).toBe(false);
    expect(result.current.hasRole('NURSE', 'ADMINISTRATOR')).toBe(false);
  });
});
