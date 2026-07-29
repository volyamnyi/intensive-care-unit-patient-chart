import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import { MemoryRouter } from 'react-router-dom';
import GlobalLayout from '../../layouts/GlobalLayout';

const mockNavigate = vi.fn();
const mockLogout = vi.fn();
const mockToggleTheme = vi.fn();

let mockUser = { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
let mockHasRole = (...roles: string[]) => roles.includes('DOCTOR');

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../services/AuthContext', () => ({
  useAuth: () => ({
    user: mockUser,
    token: 'mock-token',
    isAuthenticated: true,
    logout: mockLogout,
    hasRole: mockHasRole,
  }),
}));

vi.mock('../../styles/ThemeContext', () => ({
  useThemeMode: () => ({
    mode: 'dark' as const,
    toggleTheme: mockToggleTheme,
  }),
  ThemeModeProvider: ({ children }: { children: React.ReactNode }) => children,
}));

function renderLayout(route = '/prescriptions/icu/doctor') {
  return render(
    <ThemeModeProvider>
      <MemoryRouter initialEntries={[route]}>
        <GlobalLayout />
      </MemoryRouter>
    </ThemeModeProvider>
  );
}

describe('GlobalLayout - header', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
    mockHasRole = (...roles: string[]) => roles.includes('DOCTOR');
  });

  it('renders app title based on route', () => {
    renderLayout('/prescriptions/icu/doctor');
    expect(screen.getByText('ВАІТ')).toBeInTheDocument();
    expect(screen.getAllByText('Карта інтенсивної терапії').length).toBeGreaterThanOrEqual(1);
  });

  it('renders prescriptions title when on prescriptions route', () => {
    renderLayout('/prescriptions/doctor');
    expect(screen.getAllByText('Листок лікарських призначень').length).toBeGreaterThanOrEqual(1);
  });

  it('renders admin title when on admin route', () => {
    renderLayout('/admin');
    const headings = screen.getAllByText('Адмін');
    expect(headings.length).toBeGreaterThanOrEqual(1);
  });

  it('renders logo image', () => {
    renderLayout();
    const img = screen.getByAltText('Superhumans');
    expect(img).toBeInTheDocument();
  });

  it('renders Модулі link in header', () => {
    renderLayout();
    expect(screen.getByText('Модулі')).toBeInTheDocument();
  });

  it('does not show Відділення for regular doctor', () => {
    renderLayout();
    expect(screen.queryByText('Відділення')).not.toBeInTheDocument();
  });

  it('shows user menu with name and role', async () => {
    renderLayout();
    await userEvent.click(screen.getByLabelText('Меню користувача'));
    await waitFor(() => {
      const menuItems = screen.getAllByText('Доктор Іван');
      expect(menuItems.length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Лікар')).toBeInTheDocument();
    });
  });

  it('calls logout and navigates on logout click', async () => {
    renderLayout();
    await userEvent.click(screen.getByLabelText('Меню користувача'));
    await waitFor(() => expect(screen.getByText('Вийти')).toBeInTheDocument());
    await userEvent.click(screen.getByText('Вийти'));
    expect(mockLogout).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('toggles theme on icon click', async () => {
    renderLayout();
    await userEvent.click(screen.getByLabelText('Переключити тему'));
    expect(mockToggleTheme).toHaveBeenCalled();
  });

  it('nurse sees nurse routes in sidebar', async () => {
    mockUser = { id: 3, login: 'nurse1', fullName: 'Медсестра Олена', role: 'NURSE' };
    mockHasRole = (...roles: string[]) => roles.includes('NURSE');
    renderLayout('/prescriptions/icu/nurse');
    expect(screen.getByText('ВАІТ')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('Меню користувача'));
    await waitFor(() => {
      expect(screen.getByText('Медсестра')).toBeInTheDocument();
    });
  });
});

describe('GlobalLayout - HOD role', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { id: 2, login: 'head1', fullName: 'Завідувач Петро', role: 'HEAD_OF_DEPARTMENT' };
    mockHasRole = (...roles: string[]) => roles.includes('HEAD_OF_DEPARTMENT');
  });

  it('shows HOD role label in user menu', async () => {
    renderLayout();
    await userEvent.click(screen.getByLabelText('Меню користувача'));
    await waitFor(() => {
      expect(screen.getByText('Завідувач відділення')).toBeInTheDocument();
    });
  });
});

describe('GlobalLayout - sidebar and breadcrumbs', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
    mockHasRole = (...roles: string[]) => roles.includes('DOCTOR');
  });

  it('renders sidebar navigation', () => {
    renderLayout('/prescriptions/icu/doctor');
    expect(screen.getByLabelText('Головна навігація')).toBeInTheDocument();
  });

  it('renders breadcrumbs on doctor episode page', () => {
    renderLayout('/prescriptions/icu/doctor/episode/test-123');
    expect(screen.getByLabelText('Breadcrumb')).toBeInTheDocument();
    expect(screen.getAllByText('Пацієнти').length).toBeGreaterThanOrEqual(1);
  });

  it('renders breadcrumbs on prescriptions page', () => {
    renderLayout('/prescriptions/doctor');
    expect(screen.getByLabelText('Breadcrumb')).toBeInTheDocument();
    expect(screen.getAllByText('Призначення').length).toBeGreaterThanOrEqual(1);
  });
});
