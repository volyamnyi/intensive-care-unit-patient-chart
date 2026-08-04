import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ProstheticsProvider } from '@/prosthetics/ProstheticsContext';
import ProstheticsDashboard from '@/pages/prosthetics/ProstheticsDashboard';

vi.mock('@/services/AuthContext', () => ({
  useAuth: () => ({
    user: { fullName: 'Test User', role: 'PROSTHETIST', app: 'prosthetics' as const },
    selectApp: vi.fn(),
    hasRole: vi.fn(),
    isAuthenticated: true,
    loading: false,
  }),
}));

describe('ProstheticsDashboard', () => {
  it('renders header with prosthetics title', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics']}>
        <ProstheticsProvider>
          <ProstheticsDashboard />
        </ProstheticsProvider>
      </MemoryRouter>
    );

    expect(screen.getByText('Виробництво протезів')).toBeInTheDocument();
  });

  it('renders "Новий процес" button', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics']}>
        <ProstheticsProvider>
          <ProstheticsDashboard />
        </ProstheticsProvider>
      </MemoryRouter>
    );

    expect(screen.getByRole('button', { name: 'Новий процес' })).toBeInTheDocument();
  });

  it('renders user greeting', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics']}>
        <ProstheticsProvider>
          <ProstheticsDashboard />
        </ProstheticsProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Вітаю, Test User/i)).toBeInTheDocument();
  });

  it('renders overview card', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics']}>
        <ProstheticsProvider>
          <ProstheticsDashboard />
        </ProstheticsProvider>
      </MemoryRouter>
    );

    expect(screen.getByText('Новий процес створення')).toBeInTheDocument();
    expect(screen.getByText('Існуючі процеси')).toBeInTheDocument();
  });
});
