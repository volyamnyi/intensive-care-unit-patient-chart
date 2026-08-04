import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import TemplateStep from '@/pages/prosthetics/setup/TemplateStep';

const mockSetDraftField = vi.fn();

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
  useProsthetics: () => mockUseProsthetics(),
}));

let mockDraftValue = { patientId: 'pat-1', orderId: null as string | null, templateId: null };

const mockUseProsthetics = () => ({
  draft: mockDraftValue,
  setDraftField: mockSetDraftField,
  resetDraft: vi.fn(),
});

describe('TemplateStep', () => {
  it('redirects when no orderId selected', () => {
    mockDraftValue = { patientId: 'pat-1', orderId: null, templateId: null };

    render(
      <MemoryRouter initialEntries={['/prosthetics/new/template']}>
        <Routes>
          <Route path="/prosthetics/new/template" element={<TemplateStep />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Спочатку оберіть замовлення.')).toBeInTheDocument();
  });

  it('renders "Продовжити" button', () => {
    mockDraftValue = { patientId: 'pat-1', orderId: 'order-1', templateId: null };

    render(
      <MemoryRouter initialEntries={['/prosthetics/new/template']}>
        <Routes>
          <Route path="/prosthetics/new/template" element={<TemplateStep />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByRole('button', { name: 'Продовжити' })).toBeInTheDocument();
  });
});
