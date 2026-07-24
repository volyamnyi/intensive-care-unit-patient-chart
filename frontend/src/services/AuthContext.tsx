/* eslint-disable react/only-export-components */
import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import type { User, LoginRequest } from '../types';
import { authApi, userApi } from '../api/endpoints';

const SESSION_FLAG = 'auth:session';

interface AuthContextType {
  user: User | null;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  loading: boolean;
  hasRole: (...roles: string[]) => boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
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
      .then((res) => setUser(res.data))
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
    setUser({ id: userId, login: userName, fullName, role: role as User['role'], email, specialityCode: '', specialityName: '', phone: '' });
  };

  const logout = () => {
    authApi.logout().catch(() => {});
    localStorage.removeItem(SESSION_FLAG);
    setUser(null);
  };

  const hasRole = (...roles: string[]) => {
    return user ? roles.includes(user.role) : false;
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user, loading, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
