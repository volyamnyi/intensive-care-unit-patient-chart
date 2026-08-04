import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import PatientStep from '@/pages/prosthetics/setup/PatientStep';

const mockSetDraftField = vi.fn();

vi.mock('@/services/AuthContext', () => ({
  useAuth: () => ({
    user: { fullName: 'Test User', role: 'PROSTHETIST', app: 'prosthetics' as const },
    selectApp: vi.fn(),
    hasRole: vi.fn(),
    isAuthenticated: true,
    loading: false,
  }),
}));

vi.mock('@/prosthetics/ProstheticsContext', () => ({
  ProstheticsProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useProsthetics: () => ({
    draft: { patientId: null, orderId: null, templateId: null },
    setDraftField: mockSetDraftField,
    resetDraft: vi.fn(),
    patient: null,
    orders: [],
    templates: [],
    loadingOrders: false,
    loadingTemplates: false,
    loadOrders: vi.fn(),
    loadTemplates: vi.fn(),
  }),
}));

describe('PatientStep', () => {
  it('renders search input', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/patient']}>
        <Routes>
          <Route path="/prosthetics/new/patient" element={<PatientStep />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByPlaceholderText('Пошук пацієнта за ПІБ або ідентифікатором...')).toBeInTheDocument();
  });

  it('renders back button', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/patient']}>
        <Routes>
          <Route path="/prosthetics/new/patient" element={<PatientStep />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByRole('button', { name: 'Назад' })).toBeInTheDocument();
  });

  it('renders "Продовжити" button', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/patient']}>
        <Routes>
          <Route path="/prosthetics/new/patient" element={<PatientStep />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByRole('button', { name: 'Продовжити' })).toBeInTheDocument();
  });
});
