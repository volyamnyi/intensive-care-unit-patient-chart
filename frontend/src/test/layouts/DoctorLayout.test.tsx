import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import { MemoryRouter } from 'react-router-dom';
import DoctorLayout from '../../layouts/DoctorLayout';

const theme = createTheme({});
const mockNavigate = vi.fn();
const mockLogout = vi.fn();
const mockToggleTheme = vi.fn();

let mockUser = { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
let mockHasRole = (...roles: string[]) => roles.includes('DOCTOR') || roles.includes('HEAD_OF_DEPARTMENT');

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
    theme: createTheme({ palette: { mode: 'dark' } }),
  }),
}));

function renderLayout() {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={['/doctor']}>
        <DoctorLayout />
      </MemoryRouter>
    </ThemeProvider>
  );
}

describe('DoctorLayout - DOCTOR role', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: 'DOCTOR' };
    mockHasRole = (...roles: string[]) => roles.includes('DOCTOR') || roles.includes('HEAD_OF_DEPARTMENT');
  });

  it('renders VAIT branding', () => {
    renderLayout();
    expect(screen.getByText('ВАІТ')).toBeInTheDocument();
    expect(screen.getByText('Карта інтенсивної терапії')).toBeInTheDocument();
  });

  it('renders logo image with dark mode src', () => {
    renderLayout();
    const img = screen.getByAltText('Superhumans');
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/superhumans-white.svg');
  });

  it('renders navigation link to patients', () => {
    renderLayout();
    expect(screen.getByText('Пацієнти')).toBeInTheDocument();
  });

  it('does not show department link for regular doctor', () => {
    renderLayout();
    expect(screen.queryByText('Відділення')).not.toBeInTheDocument();
  });

  it('shows user menu with name and role', async () => {
    renderLayout();
    await userEvent.click(screen.getByLabelText('Меню користувача'));
    await waitFor(() => {
      expect(screen.getByText('Доктор Іван')).toBeInTheDocument();
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
});

describe('DoctorLayout - HEAD_OF_DEPARTMENT role', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { id: 2, login: 'head1', fullName: 'Завідувач Петро', role: 'HEAD_OF_DEPARTMENT' };
    mockHasRole = (...roles: string[]) => roles.includes('HEAD_OF_DEPARTMENT');
  });

  it('shows department link for HOD role', () => {
    renderLayout();
    expect(screen.getByText('Відділення')).toBeInTheDocument();
  });

  it('shows HOD role label in user menu', async () => {
    renderLayout();
    await userEvent.click(screen.getByLabelText('Меню користувача'));
    await waitFor(() => {
      expect(screen.getByText('Завідувач відділення')).toBeInTheDocument();
    });
  });
});
