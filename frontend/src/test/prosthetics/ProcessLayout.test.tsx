import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProcessLayout from '@/pages/prosthetics/process/ProcessLayout';
import ProcessDetail from '@/pages/prosthetics/process/ProcessDetail';

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
    orders: [],
    templates: [],
    loadingOrders: false,
    loadingTemplates: false,
    loadOrders: vi.fn(),
    loadTemplates: vi.fn(),
  }),
}));

describe('ProcessLayout', () => {
  it('renders navigation items', () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/process/proc-123']}>
        <Routes>
          <Route path="/prosthetics/process/:id" element={<ProcessLayout />}>
            <Route index element={<div>Detail Content</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Огляд')).toBeInTheDocument();
    expect(screen.getByText('Історія')).toBeInTheDocument();
    expect(screen.getByText('Документи')).toBeInTheDocument();
    expect(screen.getByText('Статистика')).toBeInTheDocument();
    expect(screen.getByText('Процес #proc-123')).toBeInTheDocument();
    expect(screen.getByText('Detail Content')).toBeInTheDocument();
  });
});

describe('ProcessDetail', () => {
  beforeEach(() => {
    global.fetch = vi.fn(() =>
      Promise.resolve({ ok: false, json: () => Promise.resolve({}) })
    ) as unknown as typeof fetch;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders "Процес не знайдено" when instance is null', async () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/process/proc-123']}>
        <Routes>
          <Route path="/prosthetics/process/:id" element={<ProcessDetail />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Процес не знайдено.')).toBeInTheDocument();
    });
  });
});
