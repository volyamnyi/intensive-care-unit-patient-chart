import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import type { User, LoginRequest } from '../types';
import { authApi, userApi } from '../api/endpoints';

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  hasRole: (...roles: string[]) => boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(
    () => localStorage.getItem('token')
  );

  useEffect(() => {
    if (token) {
      userApi.getMe()
        .then((res) => setUser(res.data))
        .catch(() => { localStorage.removeItem('token'); setToken(null); });
    }
  }, [token]);

  const login = async (data: LoginRequest) => {
    const res = await authApi.login(data);
    const { token: newToken, login: userName, fullName, role, email } = res.data;
    localStorage.setItem('token', newToken);
    setToken(newToken);
    setUser({ id: 0, login: userName, fullName, role: role as User['role'], email, specialityCode: '', specialityName: '', phone: '' });
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  const hasRole = (...roles: string[]) => {
    return user ? roles.includes(user.role) : false;
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
