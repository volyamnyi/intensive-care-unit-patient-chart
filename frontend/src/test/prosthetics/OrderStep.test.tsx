import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import OrderStep from '@/pages/prosthetics/setup/OrderStep';

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
    draft: { patientId: null, orderId: null, templateId: null },
    setDraftField: vi.fn(),
    resetDraft: vi.fn(),
    patient: null,
  }),
}));

describe('OrderStep', () => {
  it('redirects when no patientId selected', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/order']}>
        <Routes>
          <Route path="/prosthetics/new/order" element={<OrderStep />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/Спочатку оберіть пацієнта/i)).toBeInTheDocument();
  });
});
