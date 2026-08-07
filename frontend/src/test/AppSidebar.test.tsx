import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AppSidebar from '../components/navigation/AppSidebar';

// Module-navigation permissions come from the dynamic RBAC matrix (PermissionCatalog).
let mockPermissions: string[] = [];
let mockRole: string = 'DOCTOR';

vi.mock('../services/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 1, login: 'doctor1', fullName: 'Доктор Іван', role: mockRole },
    hasRole: () => false,
    hasPermission: (permission: string) => mockPermissions.includes(permission),
  }),
}));

describe('AppSidebar', () => {
  beforeEach(() => {
    mockPermissions = [];
    mockRole = 'DOCTOR';
  });

  it('shows only the module chooser when no module permissions are granted', () => {
    render(
      <MemoryRouter>
        <AppSidebar />
      </MemoryRouter>,
    );
    expect(screen.queryByText('Карта інтенсивної терапії')).not.toBeInTheDocument();
    expect(screen.queryByText('Листок лікарських призначень')).not.toBeInTheDocument();
    expect(screen.queryByText('Виробництво протезів')).not.toBeInTheDocument();
    expect(screen.getByText('Модулі')).toBeInTheDocument();
  });

  it('shows the prosthetics module when MODULE_PROSTHETICS_ACCESS is granted', () => {
    mockPermissions = ['MODULE_PROSTHETICS_ACCESS'];
    render(
      <MemoryRouter>
        <AppSidebar />
      </MemoryRouter>,
    );
    expect(screen.getByText('Виробництво протезів')).toBeInTheDocument();
    expect(screen.queryByText('Карта інтенсивної терапії')).not.toBeInTheDocument();
  });

  it('shows the clinical modules when the corresponding permissions are granted', () => {
    mockPermissions = ['MODULE_ICU_ACCESS', 'MODULE_MEDICATION_ACCESS'];
    render(
      <MemoryRouter>
        <AppSidebar />
      </MemoryRouter>,
    );
    expect(screen.getByText('Карта інтенсивної терапії')).toBeInTheDocument();
    expect(screen.getByText('Листок лікарських призначень')).toBeInTheDocument();
    expect(screen.queryByText('Виробництво протезів')).not.toBeInTheDocument();
  });

  it('routes a NURSE to the nurse prefix when the ICU module is granted', () => {
    mockRole = 'NURSE';
    mockPermissions = ['MODULE_ICU_ACCESS'];
    render(
      <MemoryRouter>
        <AppSidebar />
      </MemoryRouter>,
    );
    const icuLink = screen.getByRole('link', { name: 'Карта інтенсивної терапії' });
    expect(icuLink).toHaveAttribute('href', '/icu/nurse');
  });
});
