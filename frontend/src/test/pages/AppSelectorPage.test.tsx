import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AppSelectorPage from '../../pages/AppSelectorPage';
import { ThemeModeProvider } from '../../styles/ThemeContext';

const authState: {
  user: { fullName: string } | null;
  selectApp: (...args: unknown[]) => void;
  hasRole: (role: string) => boolean;
  hasPermission: (permission: string) => boolean;
} = {
  user: { fullName: 'Test User' },
  selectApp: vi.fn(),
  hasRole: () => false,
  hasPermission: () => false,
};

vi.mock('../../services/AuthContext', () => ({
  useAuth: () => authState,
}));

beforeEach(() => {
  vi.clearAllMocks();
  authState.hasRole = () => false;
  authState.hasPermission = () => false;
});

function renderPage() {
  return render(
    <ThemeModeProvider>
      <MemoryRouter>
        <AppSelectorPage />
      </MemoryRouter>
    </ThemeModeProvider>
  );
}

const NO_MODULES_TEXT = /не має доступу до жодного модуля/;

describe('AppSelectorPage guest empty state', () => {
  it('shows guidance alert and no cards when user has zero permissions (GUEST)', () => {
    renderPage();
    expect(screen.getByText(NO_MODULES_TEXT)).toBeInTheDocument();
    expect(screen.queryByText('Карта інтенсивної терапії')).not.toBeInTheDocument();
    expect(screen.queryByText('Листок лікарських призначень')).not.toBeInTheDocument();
    expect(screen.queryByText('Виробництво протезів')).not.toBeInTheDocument();
    expect(screen.queryByText('Адміністративна панель')).not.toBeInTheDocument();
  });

  it('shows permitted cards and no alert for a doctor', () => {
    authState.hasPermission = (p) =>
      p === 'MODULE_ICU_ACCESS' || p === 'MODULE_MEDICATION_ACCESS';
    renderPage();
    expect(screen.getByText('Карта інтенсивної терапії')).toBeInTheDocument();
    expect(screen.getByText('Листок лікарських призначень')).toBeInTheDocument();
    expect(screen.queryByText('Виробництво протезів')).not.toBeInTheDocument();
    expect(screen.queryByText(NO_MODULES_TEXT)).not.toBeInTheDocument();
  });

  it('shows admin card and no alert for an administrator', () => {
    authState.hasPermission = (p) => p === 'MODULE_ADMIN_ACCESS';
    renderPage();
    expect(screen.getByText('Адміністративна панель')).toBeInTheDocument();
    expect(screen.queryByText(NO_MODULES_TEXT)).not.toBeInTheDocument();
  });
});
