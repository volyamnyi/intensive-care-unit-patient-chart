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
    draft: { patientId: null, orderId: null, templateId: null },
    setDraftField: vi.fn(),
    resetDraft: vi.fn(),
    patient: null,
  }),
}));

describe('ReviewStep', () => {
  it('shows instructions when draft incomplete', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review']}>
        <Routes>
          <Route path="/prosthetics/new/review" element={<ReviewStep />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/Будь ласка, заповніть усі попередні кроки/i)).toBeInTheDocument();
  });
});
