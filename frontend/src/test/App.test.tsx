import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import App from '../App';

let mockUser: any = { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
let mockIsAuthenticated = true;
let mockLoading = false;
let mockHasRole = (...roles: string[]) => roles.includes('DOCTOR') || roles.includes('HEAD_OF_DEPARTMENT');
let mockHasPermission = () => false;

vi.mock('../services/AuthContext', () => ({
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useAuth: () => ({
    user: mockUser,
    token: 'mock-token',
    isAuthenticated: mockIsAuthenticated,
    loading: mockLoading,
    logout: vi.fn(),
    hasRole: mockHasRole,
    hasPermission: mockHasPermission,
    selectApp: () => {},
    clearApp: () => {},
  }),
}));

vi.mock('../styles/ThemeContext', () => ({
  ThemeModeProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useThemeMode: () => ({
    mode: 'dark' as const,
    toggleTheme: vi.fn(),
  }),
}));

// Mock all route components
vi.mock('../pages/LoginPage', () => ({
  default: () => <div>Login Page</div>,
}));

vi.mock('../pages/AppSelectorPage', () => ({
  default: () => <div>App Selector</div>,
}));

import { Outlet } from 'react-router-dom';

vi.mock('../layouts/GlobalLayout', () => ({
  default: () => <div>Global Layout<Outlet /></div>,
}));

vi.mock('../pages/doctor/DashboardPage', () => ({
  default: () => <div>Doctor Dashboard</div>,
}));

vi.mock('../pages/doctor/CreateCardPage', () => ({
  default: () => <div>Create Card Page</div>,
}));

vi.mock('../pages/doctor/PatientDayPage', () => ({
  default: () => <div>Patient Day Page</div>,
}));

vi.mock('../pages/doctor/DepartmentDashboardPage', () => ({
  default: () => <div>Department Dashboard</div>,
}));

vi.mock('../pages/nurse/NurseDashboardPage', () => ({
  default: () => <div>Nurse Dashboard</div>,
}));

vi.mock('../pages/admin/AdminPage', () => ({
  default: () => <div>Admin Page</div>,
}));

vi.mock('../pages/prescription/PrescriptionPage', () => ({
  default: () => <div>Prescription Page</div>,
}));

vi.mock('../pages/prescription/PrescriptionDetailPage', () => ({
  default: () => <div>Prescription Detail Page</div>,
}));

vi.mock('../pages/prescription/NursePrescriptionPage', () => ({
  default: () => <div>Nurse Prescription Page</div>,
}));

vi.mock('../pages/prosthetics/DashboardPage', () => ({
  default: () => <div>Prosthetics Dashboard</div>,
}));

vi.mock('../prosthetics/ProstheticsContext', () => ({
  ProstheticsProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
    mockIsAuthenticated = true;
    mockLoading = false;
    mockHasRole = (...roles: string[]) => roles.includes('DOCTOR') || roles.includes('HEAD_OF_DEPARTMENT');
    mockHasPermission = () => false;
  });

  it('renders without crashing', async () => {
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('App Selector')).toBeInTheDocument();
    });
  });

  it('renders global layout for DOCTOR user at /icu/doctor', async () => {
    window.history.pushState({}, '', '/icu/doctor');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('Global Layout')).toBeInTheDocument();
    });
  });

  it('renders login page at /login when not authenticated', async () => {
    mockIsAuthenticated = false;
    window.history.pushState({}, '', '/login');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('Login Page')).toBeInTheDocument();
    });
  });

  it('renders nurse dashboard for NURSE user at /icu/nurse', async () => {
    mockUser = { id: 2, login: 'nurse1', fullName: 'Медсестра Олена', role: 'NURSE' };
    mockHasRole = (...roles: string[]) => roles.includes('NURSE');
    window.history.pushState({}, '', '/icu/nurse');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('Global Layout')).toBeInTheDocument();
    });
  });

  it('renders admin page for ADMINISTRATOR at /admin', async () => {
    mockUser = { id: 3, login: 'admin', fullName: 'Адмін', role: 'ADMINISTRATOR' };
    mockHasRole = (...roles: string[]) => roles.includes('ADMINISTRATOR');
    window.history.pushState({}, '', '/admin');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('Admin Page')).toBeInTheDocument();
    });
  });

  it('redirects to login when accessing protected route unauthenticated', async () => {
    mockIsAuthenticated = false;
    mockUser = null;
    window.history.pushState({}, '', '/icu/doctor');
    render(<App />);
    await waitFor(() => {
      expect(screen.queryByText('Global Layout')).not.toBeInTheDocument();
    });
  });

  it('renders the prosthetics module for a role granted MODULE_PROSTHETICS_ACCESS', async () => {
    mockUser = { id: 4, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
    mockHasRole = () => false;
    mockHasPermission = (...perms: string[]) => perms.includes('MODULE_PROSTHETICS_ACCESS');
    window.history.pushState({}, '', '/prosthetics');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('Prosthetics Dashboard')).toBeInTheDocument();
    });
  });

  it('blocks a role without MODULE_PROSTHETICS_ACCESS from the prosthetics module', async () => {
    mockUser = { id: 4, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
    mockHasRole = () => false;
    mockHasPermission = () => false;
    window.history.pushState({}, '', '/prosthetics');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('App Selector')).toBeInTheDocument();
    });
    expect(screen.queryByText('Prosthetics Dashboard')).not.toBeInTheDocument();
  });

  it('renders the ICU module for ADMINISTRATOR granted MODULE_ICU_ACCESS', async () => {
    mockUser = { id: 3, login: 'admin', fullName: 'Адмін', role: 'ADMINISTRATOR' };
    mockHasRole = () => false;
    mockHasPermission = (...perms: string[]) => perms.includes('MODULE_ICU_ACCESS');
    window.history.pushState({}, '', '/icu/doctor');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('Global Layout')).toBeInTheDocument();
    });
  });

  it('renders the medication module for ADMINISTRATOR granted MODULE_MEDICATION_ACCESS', async () => {
    mockUser = { id: 3, login: 'admin', fullName: 'Адмін', role: 'ADMINISTRATOR' };
    mockHasRole = () => false;
    mockHasPermission = (...perms: string[]) => perms.includes('MODULE_MEDICATION_ACCESS');
    window.history.pushState({}, '', '/prescriptions/doctor');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('Prescription Page')).toBeInTheDocument();
    });
  });

  it('keeps the nurse sub-route exclusive from DOCTOR even when holding MODULE_ICU_ACCESS', async () => {
    mockUser = { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
    mockHasRole = (...roles: string[]) => roles.includes('DOCTOR') || roles.includes('HEAD_OF_DEPARTMENT');
    mockHasPermission = () => true; // DOCTOR holds MODULE_ICU_ACCESS in the default matrix
    window.history.pushState({}, '', '/icu/nurse');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('App Selector')).toBeInTheDocument();
    });
    expect(screen.queryByText('Nurse Dashboard')).not.toBeInTheDocument();
  });

  it('keeps the doctor sub-route exclusive from NURSE even when holding MODULE_ICU_ACCESS', async () => {
    mockUser = { id: 2, login: 'nurse1', fullName: 'Медсестра Олена', role: 'NURSE' };
    mockHasRole = (...roles: string[]) => roles.includes('NURSE');
    mockHasPermission = () => true; // NURSE holds MODULE_ICU_ACCESS in the default matrix
    window.history.pushState({}, '', '/icu/doctor');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('App Selector')).toBeInTheDocument();
    });
    expect(screen.queryByText('Doctor Dashboard')).not.toBeInTheDocument();
  });
});
