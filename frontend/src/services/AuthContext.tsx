/* eslint-disable react/only-export-components */
import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import type { User, LoginRequest } from '../types/core';
import { authApi, userApi } from '../api/platform';

const SESSION_FLAG = 'auth:session';

interface AuthContextType {
  user: User | null;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  loading: boolean;
  hasRole: (...roles: string[]) => boolean;
  /** Effective permissions of the current role, loaded from the dynamic RBAC matrix. */
  hasPermission: (permission: string) => boolean;
  permissions: string[];
  selectApp: (app: 'icu' | 'prescriptions' | 'prosthetics') => void;
  clearApp: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [permissions, setPermissions] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (window.location.pathname === '/login') {
      setLoading(false);
      return;
    }
    if (!localStorage.getItem(SESSION_FLAG)) {
      setLoading(false);
      return;
    }
    userApi.getMe()
      .then((res) => {
        setUser((prev) => ({ ...res.data, app: (prev?.app ?? null) }));
        return userApi.getMyPermissions().then((permRes) => setPermissions(permRes.data));
      })
      .catch(() => {
        localStorage.removeItem(SESSION_FLAG);
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const login = async (data: LoginRequest) => {
    const res = await authApi.login(data);
    const { userId, login: userName, fullName, role, email } = res.data;
    localStorage.setItem(SESSION_FLAG, '1');
    setUser({
      id: userId, login: userName, fullName, role: role as User['role'], email,
      specialityCode: '', specialityName: '', phone: '', app: null,
    });
    try {
      const permRes = await userApi.getMyPermissions();
      setPermissions(permRes.data);
    } catch {
      setPermissions([]);
    }
  };

  const logout = () => {
    authApi.logout().catch(() => {});
    localStorage.removeItem(SESSION_FLAG);
    setUser(null);
    setPermissions([]);
  };

  const hasRole = (...roles: string[]) => {
    return user ? roles.includes(user.role) : false;
  };

  const hasPermission = (permission: string) => {
    return permissions.some((p) => p.toUpperCase() === permission.toUpperCase());
  };

  const selectApp = (app: 'icu' | 'prescriptions' | 'prosthetics') => {
    setUser((prev) => prev ? { ...prev, app } : null);
  };

  const clearApp = () => {
    setUser((prev) => prev ? { ...prev, app: null } : null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user, loading, hasRole, hasPermission, permissions, selectApp, clearApp }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
