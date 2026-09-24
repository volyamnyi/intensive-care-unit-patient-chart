import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import AdminPage from '../../pages/admin/AdminPage';

const mockNavigate = vi.fn();
const mockLogout = vi.fn();
const mockGetUsers = vi.fn();
const mockGetStats = vi.fn();
const mockGetPermissions = vi.fn();
const mockGetCatalog = vi.fn();
const mockImportDataset = vi.fn();

const emptyCatalog = {
  summary: { drugs: 0, interactions: 0, bySeverity: { low: 0, medium: 0, high: 0, critical: 0 }, lastImportAt: null },
  drugs: [],
  page: { content: [], totalElements: 0, totalPages: 0 },
};

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../api/platform', () => ({
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

vi.mock('../../api/medication', () => ({
  drugInteractionAdminApi: {
    getCatalog: (...args: unknown[]) => mockGetCatalog(...args),
    importDataset: (...args: unknown[]) => mockImportDataset(...args),
  },
}));

vi.mock('../../services/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 0, login: 'admin', fullName: 'Адмін', role: 'ADMINISTRATOR', email: 'admin@test.com', app: null },
    token: 'mock-token',
    isAuthenticated: true,
    logout: mockLogout,
    hasRole: (...roles: string[]) => roles.includes('ADMINISTRATOR'),
    hasPermission: () => true,
    permissions: ['AUDIT_ACCESS', 'PATIENT_VIEW'],
  }),
}));

const mockUsers = [
  { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR', email: 'doctor1@test.com', specialityCode: '', specialityName: '', phone: '', app: null },
  { id: 2, login: 'nurse1', fullName: 'Медсестра Олена', role: 'NURSE', email: 'nurse1@test.com', specialityCode: '', specialityName: '', phone: '', app: null },
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
    mockGetStats.mockResolvedValue({ data: { totalUsers: 2, doctors: 1, nurses: 1, headsOfDepartment: 0, administrators: 1 } });
    mockGetPermissions.mockResolvedValue({ data: mockMatrix });
    mockGetCatalog.mockResolvedValue({ data: emptyCatalog });
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

  it('renders the interaction catalog summary and pair rows for admin (#305)', async () => {
    mockGetCatalog.mockResolvedValue({ data: {
      summary: { drugs: 2, interactions: 1, bySeverity: { low: 0, medium: 0, high: 1, critical: 0 }, lastImportAt: '2026-09-23T10:00:00' },
      drugs: [
        { atcCode: 'M01AE01', ukrainianRaw: 'Ібупрофен 200 мг', genericEn: 'Ibuprofen' },
        { atcCode: 'N02BE01', ukrainianRaw: 'Парацетамол 500 мг', genericEn: 'Paracetamol' },
      ],
      page: { content: [{ drugAAtc: 'M01AE01', drugBAtc: 'N02BE01', severity: 'high', interaction: 'текст взаємодії', interactionId: 'DI-0001' }], totalElements: 1, totalPages: 1 },
    }});
    renderPage();
    await userEvent.click(screen.getByRole('tab', { name: 'База взаємодій' }));
    await waitFor(() => {
      expect(screen.getByText('Парацетамол 500 мг')).toBeInTheDocument();
    });
    expect(screen.getByText('високо')).toBeInTheDocument();
    expect(screen.getByText(/Ст\. 1 із 1/)).toBeInTheDocument();
    expect(mockGetCatalog).toHaveBeenCalledWith({ severity: undefined, query: undefined, page: 0, size: 50 });
  });

  it('passes the severity filter into the catalog request (#305)', async () => {
    renderPage();
    await userEvent.click(screen.getByRole('tab', { name: 'База взаємодій' }));
    await waitFor(() => expect(mockGetCatalog).toHaveBeenCalled());
    await userEvent.click(screen.getByRole('combobox', { name: 'Рівень взаємодії' }));
    await userEvent.click(await screen.findByRole('option', { name: 'високо' }));
    await waitFor(() => {
      expect(mockGetCatalog).toHaveBeenLastCalledWith({ severity: 'high', query: undefined, page: 0, size: 50 });
    });
  });

  it('renders the permission matrix with role columns and grants', async () => {
    renderPage();
    await userEvent.click(screen.getByRole('tab', { name: 'Доступи та ролі' }));
    await waitFor(() => {
      expect(screen.getByText('Матриця доступів')).toBeInTheDocument();
    });
    expect(screen.getByText('Створення епізоду')).toBeInTheDocument();
    // DOCTOR holds EPISODE_CREATE, NURSE does not
    expect(screen.getByRole('checkbox', { name: 'Створення епізоду — Лікар' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Створення епізоду — Медсестра' })).not.toBeChecked();
  });
});
