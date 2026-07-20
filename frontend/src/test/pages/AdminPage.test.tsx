import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import AdminPage from '../../pages/admin/AdminPage';
import type { User } from '../../types';

const theme = createTheme({});

const mockNavigate = vi.fn();
const mockLogout = vi.fn();
const mockGetDoctors = vi.fn();
const mockGetNurses = vi.fn();
const mockAuditList = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../api/endpoints', () => ({
  userApi: {
    getDoctors: (...args: unknown[]) => mockGetDoctors(...args),
    getNurses: (...args: unknown[]) => mockGetNurses(...args),
  },
  auditApi: {
    list: (...args: unknown[]) => mockAuditList(...args),
  },
}));

vi.mock('../../services/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 0, login: 'admin', fullName: 'Адмін', role: 'ADMINISTRATOR', email: 'admin@test.com' },
    token: 'mock-token',
    isAuthenticated: true,
    logout: mockLogout,
    hasRole: (...roles: string[]) => roles.includes('ADMINISTRATOR'),
  }),
}));

const mockDoctors: User[] = [
  { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR', email: 'doctor1@test.com', specialityCode: '001', specialityName: 'Хірург', phone: '123' },
  { id: 2, login: 'head1', fullName: 'Завідувач Петро', role: 'HEAD_OF_DEPARTMENT', email: 'head1@test.com', specialityCode: '002', specialityName: 'Завідувач', phone: '456' },
];

const mockNurses: User[] = [
  { id: 3, login: 'nurse1', fullName: 'Медсестра Олена', role: 'NURSE', email: 'nurse1@test.com', specialityCode: '010', specialityName: 'Медсестра', phone: '789' },
];

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <AdminPage />
    </ThemeProvider>
  );
}

describe('AdminPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetDoctors.mockResolvedValue({ data: mockDoctors });
    mockGetNurses.mockResolvedValue({ data: mockNurses });
  });

  it('sets document title on mount', () => {
    renderPage();
    expect(document.title).toBe('ВАІТ — Адміністрування');
  });

  it('renders the page title', () => {
    renderPage();
    expect(screen.getByText('Адміністрування')).toBeInTheDocument();
  });

  it('shows loading spinner initially', () => {
    mockGetDoctors.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('renders doctor and nurse tables after load', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Лікарі')).toBeInTheDocument();
      expect(screen.getByText('Медсестри')).toBeInTheDocument();
    });
  });

  it('renders doctor rows with correct role labels', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Доктор Іван')).toBeInTheDocument();
      expect(screen.getByText('Завідувач Петро')).toBeInTheDocument();
      expect(screen.getByText('Лікар')).toBeInTheDocument();
      expect(screen.getByText('Завідувач відділення')).toBeInTheDocument();
    });
  });

  it('renders nurse rows with correct role label', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Медсестра Олена')).toBeInTheDocument();
      expect(screen.getByText('Медсестра')).toBeInTheDocument();
    });
  });

  it('renders user menu icon', () => {
    renderPage();
    expect(screen.getByLabelText('Меню користувача')).toBeInTheDocument();
  });

  it('opens user menu on icon click', async () => {
    renderPage();
    await userEvent.click(screen.getByLabelText('Меню користувача'));
    await waitFor(() => {
      expect(screen.getByText('Адмін')).toBeInTheDocument();
      expect(screen.getByText('Вийти')).toBeInTheDocument();
    });
  });

  it('calls logout and navigates on logout click', async () => {
    renderPage();
    await userEvent.click(screen.getByLabelText('Меню користувача'));
    await waitFor(() => expect(screen.getByText('Вийти')).toBeInTheDocument());
    await userEvent.click(screen.getByText('Вийти'));
    expect(mockLogout).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('shows empty table when no users', async () => {
    mockGetDoctors.mockResolvedValue({ data: [] });
    mockGetNurses.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      const emptyCells = screen.getAllByText('Немає даних');
      expect(emptyCells.length).toBeGreaterThanOrEqual(2);
    });
  });
<<<<<<< HEAD

=======
>>>>>>> 91f3cfc (fix: IntensiveCareCard.test timeout — mock location.reload, increase test timeout)
  it('renders audit log rows from paginated response (content array) without crashing', async () => {
    mockAuditList.mockResolvedValue({
      data: {
        content: [
          { id: '1', timestamp: '2026-07-20T10:00:00', userId: 11, entity: 'AUTH', entityId: null, action: 'LOGIN', oldValue: null, newValue: null, correlationId: null, ipAddress: '0:0:0:0:0:0:0:1', userRole: 'DOCTOR' },
          { id: '2', timestamp: '2026-07-20T09:00:00', userId: 16, entity: 'EPISODE', entityId: 'a1111111-1111-1111-1111-111111111111', action: 'CREATE', oldValue: null, newValue: '{}', correlationId: null, ipAddress: '0:0:0:0:0:0:0:1', userRole: 'ADMINISTRATOR' },
        ],
        totalElements: 2,
        totalPages: 1,
        number: 0,
        size: 10,
      },
    });
    renderPage();
<<<<<<< HEAD
    await userEvent.click(await screen.findByRole('button', { name: 'Переглянути журнал аудиту' }));
=======
    await userEvent.click(await screen.findByRole('button', { name: 'Переглянути' }));
>>>>>>> 91f3cfc (fix: IntensiveCareCard.test timeout — mock location.reload, increase test timeout)
    await waitFor(() => {
      expect(screen.getByText('AUTH')).toBeInTheDocument();
      expect(screen.getByText('EPISODE')).toBeInTheDocument();
    });
    expect(mockAuditList).toHaveBeenCalled();
  });

  it('handles empty audit log content without crashing', async () => {
    mockAuditList.mockResolvedValue({
      data: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 },
    });
    renderPage();
<<<<<<< HEAD
    await userEvent.click(await screen.findByRole('button', { name: /журнал/i }));
=======
    await userEvent.click(await screen.findByRole('button', { name: 'Переглянути' }));
>>>>>>> 91f3cfc (fix: IntensiveCareCard.test timeout — mock location.reload, increase test timeout)
    await waitFor(() => {
      expect(screen.getByText('Немає записів аудиту')).toBeInTheDocument();
    });
  });
});
