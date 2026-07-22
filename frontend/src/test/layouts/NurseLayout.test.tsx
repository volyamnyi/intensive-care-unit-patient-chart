import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import { MemoryRouter } from 'react-router-dom';
import NurseLayout from '../../layouts/NurseLayout';

const theme = createTheme({});
const mockNavigate = vi.fn();
const mockLogout = vi.fn();
const mockToggleTheme = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../services/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 2, login: 'nurse1', fullName: 'Медсестра Олена', role: 'NURSE' },
    token: 'mock-token',
    isAuthenticated: true,
    logout: mockLogout,
    hasRole: (...roles: string[]) => roles.includes('NURSE'),
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
      <MemoryRouter initialEntries={['/nurse']}>
        <NurseLayout />
      </MemoryRouter>
    </ThemeProvider>
  );
}

describe('NurseLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
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

  it('shows user menu with name and role', async () => {
    renderLayout();
    await userEvent.click(screen.getByLabelText('Меню користувача'));
    await waitFor(() => {
      expect(screen.getByText('Медсестра Олена')).toBeInTheDocument();
      expect(screen.getByText('Медсестра')).toBeInTheDocument();
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

  it('renders theme toggle button', () => {
    renderLayout();
    expect(screen.getByLabelText('Переключити тему')).toBeInTheDocument();
  });
});
