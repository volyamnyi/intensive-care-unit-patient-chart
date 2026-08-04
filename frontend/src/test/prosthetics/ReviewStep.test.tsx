import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ReviewStep from '@/pages/prosthetics/setup/ReviewStep';

vi.mock('@/services/AuthContext', () => ({
  useAuth: () => ({
    user: { fullName: 'Test User', role: 'PROSTHETIST' },
    selectApp: vi.fn(),
    hasRole: vi.fn(),
    isAuthenticated: true,
    loading: false,
  }),
}));

vi.mock('@/prosthetics/ProstheticsContext', () => ({
  ProstheticsProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useProsthetics: () => ({
    draft: { patientId: 'pat-1', orderId: 'order-1', templateId: 'tmpl-1' },
    setDraftField: vi.fn(),
    resetDraft: vi.fn(),
    patient: { id: 'pat-1', fullName: 'John Doe', birthDate: '1990-01-01', sexCode: 'M' },
  }),
}));

describe('ReviewStep', () => {
  it('renders when all draft fields are set', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review']}>
        <Routes>
          <Route path="/prosthetics/new/review" element={<ReviewStep />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Перевірка даних')).toBeInTheDocument();
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('Створити процес')).toBeInTheDocument();
  });

  it('renders back button', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review']}>
        <Routes>
          <Route path="/prosthetics/new/review" element={<ReviewStep />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByRole('button', { name: 'Зберегти черновик' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Створити процес' })).toBeInTheDocument();
  });
});
