import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OrderSelectPage from '@/pages/prosthetics/setup/OrderSelectPage';

const prostheticsOrderApiMock = vi.hoisted(() => ({
  listByPatient: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  prostheticsOrderApi: prostheticsOrderApiMock,
}));

const useProsthetics = vi.hoisted(() => vi.fn());

vi.mock('@/prosthetics/ProstheticsContext', () => ({
  useProsthetics,
}));

function mockUseProsthetics(draft = { patientId: 'p1', orderId: null, templateId: null, instanceId: null }) {
  useProsthetics.mockReturnValue({
    draft,
    setDraftField: vi.fn(),
    resetDraft: vi.fn(),
  });
}

function renderPage() {
  mockUseProsthetics();
  return render(
    <MemoryRouter initialEntries={['/prosthetics/new/select-order']}>
      <OrderSelectPage />
    </MemoryRouter>,
  );
}

describe('OrderSelectPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title and patient card', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Вибір замовлення')).toBeInTheDocument();
    });
    expect(screen.getByText('p1')).toBeInTheDocument();
  });

  it('fetches orders for the selected patient', async () => {
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(prostheticsOrderApiMock.listByPatient).toHaveBeenCalledWith('p1');
    });
  });

  it('renders orders in a table', async () => {
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({
      data: [
        {
          id: 'o1', orderNumber: 'ORD-001', productType: 'Протез', amputationLevel: 'above',
          limbSide: 'left', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z',
        },
      ],
    });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('#ORD-001')).toBeInTheDocument();
    });
  });

  it('shows empty state when patient has no orders', async () => {
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Немає замовлень для цього пацієнта/)).toBeInTheDocument();
    });
  });

  it('shows error on fetch failure', async () => {
    prostheticsOrderApiMock.listByPatient.mockRejectedValue(new Error('network'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Не вдалося завантажити замовлення/)).toBeInTheDocument();
    });
  });

  it('calls setDraftField and navigates on order select', async () => {
    const setDraftField = vi.fn();
    useProsthetics.mockReturnValue({
      draft: { patientId: 'p1', orderId: null, templateId: null, instanceId: null },
      setDraftField,
      resetDraft: vi.fn(),
    });
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({
      data: [
        {
          id: 'o1', orderNumber: 'ORD-001', productType: 'Протез', amputationLevel: 'above',
          limbSide: 'left', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z',
        },
      ],
    });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/select-order']}>
        <OrderSelectPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByText('#ORD-001')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Обрати/i }));
    expect(setDraftField).toHaveBeenCalledWith('orderId', 'o1');
  });
});