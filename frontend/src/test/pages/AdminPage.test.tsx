import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import AdminPage from '../../pages/admin/AdminPage';

const mockNavigate = vi.fn();
const mockLogout = vi.fn();
const mockGetUsers = vi.fn();
const mockGetStats = vi.fn();
const mockGetPermissions = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../api/endpoints', () => ({
  adminApi: {
    getUsers: (...args: unknown[]) => mockGetUsers(...args),
    getStats: (...args: unknown[]) => mockGetStats(...args),
    getPermissions: (...args: unknown[]) => mockGetPermissions(...args),
    updateRolePermission: vi.fn(),
  },
  auditApi: {
    list: vi.fn().mockResolvedValue({ data: { content: [] } }),
  },
}));

vi.mock('../../services/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 0, login: 'admin', fullName: 'Адмін', role: 'ADMINISTRATOR', email: 'admin@test.com', permissions: '', app: null },
    token: 'mock-token',
    isAuthenticated: true,
    logout: mockLogout,
    hasRole: (...roles: string[]) => roles.includes('ADMINISTRATOR'),
    hasPermission: () => true,
    permissions: ['AUDIT_ACCESS', 'PATIENT_VIEW'],
  }),
}));

const mockUsers = [
  { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR', email: 'doctor1@test.com', specialityCode: '', specialityName: '', phone: '', permissions: 'PRESCRIBER', app: null },
  { id: 2, login: 'nurse1', fullName: 'Медсестра Олена', role: 'NURSE', email: 'nurse1@test.com', specialityCode: '', specialityName: '', phone: '', permissions: '', app: null },
];

const mockMatrix = {
  roles: ['DOCTOR', 'NURSE', 'HEAD_OF_DEPARTMENT', 'ADMINISTRATOR', 'PROSTHETIST', 'PROSTHETICS_ADMINISTRATOR'],
  permissions: [
    { code: 'EPISODE_CREATE', label: 'Створення епізоду', description: '', category: 'Клінічні операції' },
    { code: 'AUDIT_ACCESS', label: 'Журнал аудиту', description: '', category: 'Адміністрування' },
  ],
  grants: {
    DOCTOR: ['EPISODE_CREATE'],
    NURSE: [],
    HEAD_OF_DEPARTMENT: ['EPISODE_CREATE'],
    ADMINISTRATOR: ['AUDIT_ACCESS'],
    PROSTHETIST: [],
    PROSTHETICS_ADMINISTRATOR: [],
  },
};

function renderPage() {
  return render(
    <ThemeModeProvider>
        <AdminPage />
    </ThemeModeProvider>
  );
}

describe('AdminPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetUsers.mockResolvedValue({ data: mockUsers });
    mockGetStats.mockResolvedValue({ data: { totalUsers: 2, doctors: 1, nurses: 1, headsOfDepartment: 0, administrators: 1, prescribers: 1 } });
    mockGetPermissions.mockResolvedValue({ data: mockMatrix });
  });

  it('sets document title on mount', () => {
    renderPage();
    expect(document.title).toBe('Адмін — Superhumans Lviv');
  });

  it('renders the page title', () => {
    renderPage();
    expect(screen.getByText('Адміністративна панель')).toBeInTheDocument();
  });

  it('renders user rows after load', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Доктор Іван')).toBeInTheDocument();
      expect(screen.getByText('Медсестра Олена')).toBeInTheDocument();
    });
  });

  it('renders tabs', async () => {
    renderPage();
    expect(screen.getByText('Користувачі')).toBeInTheDocument();
    expect(screen.getByText('Доступи та ролі')).toBeInTheDocument();
    expect(screen.getByText('Журнал аудиту')).toBeInTheDocument();
    expect(screen.getByText('Статистика')).toBeInTheDocument();
  });

  it('renders the permission matrix with role columns and grants', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Матриця доступів')).toBeInTheDocument();
    });
    expect(screen.getByText('Створення епізоду')).toBeInTheDocument();
    // DOCTOR holds EPISODE_CREATE, NURSE does not
    expect(screen.getByRole('checkbox', { name: 'Створення епізоду — Лікар' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Створення епізоду — Медсестра' })).not.toBeChecked();
  });
});
