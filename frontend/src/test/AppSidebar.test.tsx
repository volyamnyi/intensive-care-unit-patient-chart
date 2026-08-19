import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AppSidebar, { AppNavList, TABLET_RAIL_QUERY } from '../components/navigation/AppSidebar';

// Module-navigation permissions come from the dynamic RBAC matrix (PermissionCatalog).
let mockPermissions: string[] = [];
let mockRole: string = 'DOCTOR';

const MOBILE_QUERY = '(max-width: 639.98px)';

vi.mock('../services/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: mockRole },
    hasRole: () => false,
    hasPermission: (permission: string) => mockPermissions.includes(permission),
  }),
}));

function renderInRouter(ui: React.ReactNode) {
  return render(<MemoryRouter>{ui}</MemoryRouter>);
}

describe('AppSidebar', () => {
  beforeEach(() => {
    mockPermissions = [];
    mockRole = 'DOCTOR';
    localStorage.clear();
  });

  it('shows only the module chooser when no module permissions are granted', () => {
    renderInRouter(<AppSidebar />);
    expect(screen.queryByText('Карта інтенсивної терапії')).not.toBeInTheDocument();
    expect(screen.queryByText('Листок лікарських призначень')).not.toBeInTheDocument();
    expect(screen.queryByText('Виробництво протезів')).not.toBeInTheDocument();
    expect(screen.getByText('Модулі')).toBeInTheDocument();
  });

  it('shows the prosthetics module when MODULE_PROSTHETICS_ACCESS is granted', () => {
    mockPermissions = ['MODULE_PROSTHETICS_ACCESS'];
    renderInRouter(<AppSidebar />);
    expect(screen.getByText('Виробництво протезів')).toBeInTheDocument();
    expect(screen.queryByText('Карта інтенсивної терапії')).not.toBeInTheDocument();
  });

  it('shows the clinical modules when the corresponding permissions are granted', () => {
    mockPermissions = ['MODULE_ICU_ACCESS', 'MODULE_MEDICATION_ACCESS'];
    renderInRouter(<AppSidebar />);
    expect(screen.getByText('Карта інтенсивної терапії')).toBeInTheDocument();
    expect(screen.getByText('Листок лікарських призначень')).toBeInTheDocument();
    expect(screen.queryByText('Виробництво протезів')).not.toBeInTheDocument();
  });

  it('routes a NURSE to the nurse prefix when the ICU module is granted', () => {
    mockRole = 'NURSE';
    mockPermissions = ['MODULE_ICU_ACCESS'];
    renderInRouter(<AppSidebar />);
    const icuLink = screen.getByRole('link', { name: 'Карта інтенсивної терапії' });
    expect(icuLink).toHaveAttribute('href', '/icu/nurse');
  });

  describe('responsive behavior', () => {
    it('renders nothing on mobile (rail unmounted — Sheet nav is used instead)', () => {
      (globalThis as any).setMatchMediaQuery(MOBILE_QUERY);
      mockPermissions = ['MODULE_ICU_ACCESS', 'MODULE_MEDICATION_ACCESS'];
      const { container } = renderInRouter(<AppSidebar />);
      expect(container).toBeEmptyDOMElement();
      expect(screen.queryByRole('navigation', { name: 'Головна навігація' })).not.toBeInTheDocument();
    });

    it('defaults to the collapsed icon rail on a tablet viewport', () => {
      (globalThis as any).setMatchMediaQuery(TABLET_RAIL_QUERY);
      mockPermissions = ['MODULE_ICU_ACCESS'];
      const { container } = renderInRouter(<AppSidebar />);
      const aside = container.querySelector('aside');
      expect(aside).toBeInTheDocument();
      expect((aside as HTMLElement).className).toContain('w-[60px]');
      expect(screen.queryByText('Карта інтенсивної терапії')).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Розгорнути меню' })).toBeInTheDocument();
    });

    it('renders the full label in expanded mode on a desktop viewport', () => {
      mockPermissions = ['MODULE_ICU_ACCESS'];
      renderInRouter(<AppSidebar />);
      expect(screen.getByText('Карта інтенсивної терапії')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Згорнути меню' })).toBeInTheDocument();
    });
  });

  describe('AppNavList standalone', () => {
    it('renders the nav list with all granted modules', () => {
      mockPermissions = ['MODULE_ICU_ACCESS', 'MODULE_MEDICATION_ACCESS', 'MODULE_PROSTHETICS_ACCESS'];
      renderInRouter(<AppNavList />);
      expect(screen.getByRole('navigation', { name: 'Головна навігація' })).toBeInTheDocument();
      expect(screen.getByText('Карта інтенсивної терапії')).toBeInTheDocument();
      expect(screen.getByText('Листок лікарських призначень')).toBeInTheDocument();
      expect(screen.getByText('Виробництво протезів')).toBeInTheDocument();
      expect(screen.getByText('Модулі')).toBeInTheDocument();
    });

    it('hides labels in collapsed mode but keeps accessible link names', () => {
      mockPermissions = ['MODULE_ICU_ACCESS'];
      renderInRouter(<AppNavList collapsed />);
      const link = screen.getByRole('link', { name: 'Карта інтенсивної терапії' });
      expect(screen.queryByText('Карта інтенсивної терапії')).not.toBeInTheDocument();
      expect(link).toHaveAttribute('title', 'Карта інтенсивної терапії');
    });
  });
});
