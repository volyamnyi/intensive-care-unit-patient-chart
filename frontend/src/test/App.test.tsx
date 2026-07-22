import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import App from '../App';

let mockUser: any = { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
let mockIsAuthenticated = true;
let mockLoading = false;
let mockHasRole = (...roles: string[]) => roles.includes('DOCTOR') || roles.includes('HEAD_OF_DEPARTMENT');

vi.mock('../services/AuthContext', () => ({
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useAuth: () => ({
    user: mockUser,
    token: 'mock-token',
    isAuthenticated: mockIsAuthenticated,
    loading: mockLoading,
    logout: vi.fn(),
    hasRole: mockHasRole,
  }),
}));

const mockTheme = {
  palette: {
    mode: 'dark' as const,
    common: { black: '#000', white: '#fff' },
    primary: { main: '#1976d2', light: '#42a5f5', dark: '#1565c0', contrastText: '#fff' },
    secondary: { main: '#9c27b0', light: '#ba68c8', dark: '#7b1fa2', contrastText: '#fff' },
    error: { main: '#d32f2f', light: '#ef5350', dark: '#c62828', contrastText: '#fff' },
    warning: { main: '#ed6c02', light: '#ff9800', dark: '#e65100', contrastText: '#fff' },
    info: { main: '#0288d1', light: '#03a9f4', dark: '#01579b', contrastText: '#fff' },
    success: { main: '#2e7d32', light: '#4caf50', dark: '#1b5e20', contrastText: '#fff' },
    text: { primary: '#fff', secondary: 'rgba(255,255,255,0.7)', disabled: 'rgba(255,255,255,0.5)' },
    background: { default: '#121212', paper: '#1e1e1e' },
    divider: 'rgba(255,255,255,0.12)',
    action: {
      active: '#fff', hover: 'rgba(255,255,255,0.08)', selected: 'rgba(255,255,255,0.16)',
      disabled: 'rgba(255,255,255,0.3)', disabledBackground: 'rgba(255,255,255,0.12)',
    },
  },
  typography: {
    fontWeightBold: 700,
    fontWeightLight: 300,
    fontWeightMedium: 500,
    fontWeightRegular: 400,
    fontFamily: '"Roboto","Helvetica","Arial",sans-serif',
    htmlFontSize: 16,
    pxToRem: (px: number) => `${px / 16}rem`,
  },
  breakpoints: {
    keys: ['xs', 'sm', 'md', 'lg', 'xl'],
    values: { xs: 0, sm: 600, md: 900, lg: 1200, xl: 1536 },
    up: () => '', down: () => '', between: () => '', only: () => '', not: () => '',
  },
  shape: { borderRadius: 4 },
  spacing: (factor: number) => `${8 * factor}px`,
  direction: 'ltr' as const,
  mixins: { toolbar: { minHeight: 56 } },
  shadows: Array(25).fill('none') as any,
  transitions: { create: () => '', duration: { standard: 300 }, easing: { easeInOut: 'cubic-bezier(0.4,0,0.2,1)' } },
  zIndex: { mobileStepper: 1000, fab: 1050, speedDial: 1050, appBar: 1100, drawer: 1200, modal: 1300, snackbar: 1400, tooltip: 1500 },
};

vi.mock('../styles/ThemeContext', () => ({
  ThemeModeProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useThemeMode: () => ({
    mode: 'dark' as const,
    toggleTheme: vi.fn(),
    theme: mockTheme as any,
  }),
}));

// Mock all route components
vi.mock('../pages/LoginPage', () => ({
  default: () => <div>Login Page</div>,
}));

vi.mock('../layouts/DoctorLayout', () => ({
  default: () => <div>Doctor Layout</div>,
}));

vi.mock('../layouts/NurseLayout', () => ({
  default: () => <div>Nurse Layout</div>,
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

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
    mockIsAuthenticated = true;
    mockLoading = false;
    mockHasRole = (...roles: string[]) => roles.includes('DOCTOR') || roles.includes('HEAD_OF_DEPARTMENT');
  });

  it('renders without crashing', () => {
    const { container } = render(<App />);
    expect(container).toBeDefined();
  });

  it('renders doctor layout for DOCTOR user at /doctor', async () => {
    window.history.pushState({}, '', '/doctor');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('Doctor Layout')).toBeInTheDocument();
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

  it('renders nurse dashboard for NURSE user at /nurse', async () => {
    mockUser = { id: 2, login: 'nurse1', fullName: 'Медсестра Олена', role: 'NURSE' };
    mockHasRole = (...roles: string[]) => roles.includes('NURSE');
    window.history.pushState({}, '', '/nurse');
    render(<App />);
    await waitFor(() => {
      expect(screen.getByText('Nurse Layout')).toBeInTheDocument();
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
    window.history.pushState({}, '', '/doctor');
    render(<App />);
    // Should not render doctor - redirect to login
    await waitFor(() => {
      expect(screen.queryByText('Doctor Layout')).not.toBeInTheDocument();
    });
  });
});
