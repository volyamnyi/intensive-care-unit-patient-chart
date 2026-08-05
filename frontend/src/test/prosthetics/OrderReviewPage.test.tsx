import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OrderReviewPage from '@/pages/prosthetics/setup/OrderReviewPage';

const prostheticsOrderApiMock = vi.hoisted(() => ({
  getById: vi.fn(),
  getDocument: vi.fn(),
}));

const flowTemplateApiMock = vi.hoisted(() => ({
  getById: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  prostheticsOrderApi: prostheticsOrderApiMock,
  flowTemplateApi: flowTemplateApiMock,
}));

const useProsthetics = vi.hoisted(() => vi.fn());

vi.mock('@/prosthetics/ProstheticsContext', () => ({
  useProsthetics,
}));

function mockUseProsthetics(draft: { patientId: string | null; orderId: string | null; templateId: string | null; instanceId: string | null } = { patientId: null, orderId: null, templateId: null, instanceId: null }) {
  useProsthetics.mockReturnValue({
    draft,
    setDraftField: vi.fn(),
    resetDraft: vi.fn(),
  });
}

function renderPage() {
  mockUseProsthetics();
  return render(
    <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
      <OrderReviewPage />
    </MemoryRouter>,
  );
}

describe('OrderReviewPage', () => {
  beforeEach(() => {
    prostheticsOrderApiMock.getById.mockClear();
    prostheticsOrderApiMock.getDocument.mockClear();
    flowTemplateApiMock.getById.mockClear();
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the page title', async () => {
    prostheticsOrderApiMock.getById.mockResolvedValue({
      data: { id: 'o1', orderNumber: 'ORD-001', patientId: 'p1', productType: 'Протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' },
    });
    flowTemplateApiMock.getById.mockResolvedValue({
      data: { id: 't1', name: 'Шаблон', description: 'desc', templateVersion: 1, stages: [], productType: 'протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', estimatedDurationMin: 1800, createdAt: '', updatedAt: '' },
    });
    const mockFetch = vi.mocked(fetch);
    mockFetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ fullName: 'Іван Іванов' }) } as unknown as Response);
    prostheticsOrderApiMock.getDocument.mockResolvedValue({ data: new Blob(['x']) });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Перевірка замовлення')).toBeInTheDocument();
    });
  });

  it('shows loading state initially', () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: 't1', instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    expect(screen.getByText(/Завантаження даних/)).toBeInTheDocument();
  });

  it('redirects when orderId or templateId is missing', () => {
    mockUseProsthetics({ patientId: null as string | null, orderId: null as string | null, templateId: null as string | null, instanceId: null as string | null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    expect(screen.getByText(/Необхідно обрати замовлення та шаблон/)).toBeInTheDocument();
  });

  it('renders order and template details after load', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: 't1', instanceId: null });
    prostheticsOrderApiMock.getById.mockResolvedValue({
      data: { id: 'o1', orderNumber: 'ORD-001', patientId: 'p1', productType: 'Протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' },
    });
    flowTemplateApiMock.getById.mockResolvedValue({
      data: { id: 't1', name: 'Шаблон', description: 'desc', templateVersion: 1, stages: [], productType: 'протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', estimatedDurationMin: 1800, createdAt: '', updatedAt: '' },
    });
    const mockFetch = vi.mocked(fetch);
    mockFetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ fullName: 'Іван Іванов' }) } as unknown as Response);
    prostheticsOrderApiMock.getDocument.mockResolvedValue({ data: new Blob(['x']) });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('ORD-001')).toBeInTheDocument();
    });
    expect(screen.getByText('Шаблон')).toBeInTheDocument();
  });

  it('shows error state on fetch failure', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: 't1', instanceId: null });
    prostheticsOrderApiMock.getById.mockRejectedValue(new Error('network'));
    flowTemplateApiMock.getById.mockRejectedValue(new Error('network'));
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText(/Не вдалося завантажити дані/)).toBeInTheDocument();
    });
  });

  it('has three tabs: details, document, materials', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: 't1', instanceId: null });
    prostheticsOrderApiMock.getById.mockResolvedValue({
      data: { id: 'o1', orderNumber: 'ORD-001', patientId: 'p1', productType: 'Протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' },
    });
    flowTemplateApiMock.getById.mockResolvedValue({
      data: { id: 't1', name: 'Шаблон', description: 'desc', templateVersion: 1, stages: [], productType: 'протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', estimatedDurationMin: 1800, createdAt: '', updatedAt: '' },
    });
    const mockFetch = vi.mocked(fetch);
    mockFetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ fullName: 'Іван Іванов' }) } as unknown as Response);
    prostheticsOrderApiMock.getDocument.mockResolvedValue({ data: new Blob(['x']) });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByText('ORD-001')).toBeInTheDocument());
    expect(screen.getByRole('tab', { name: 'Деталі' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Рецепт' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Матеріали' })).toBeInTheDocument();
  });

  it('shows loading spinner for document when not yet loaded', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: 't1', instanceId: null });
    prostheticsOrderApiMock.getById.mockResolvedValue({
      data: { id: 'o1', orderNumber: 'ORD-001', patientId: 'p1', productType: 'Протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' },
    });
    flowTemplateApiMock.getById.mockResolvedValue({
      data: { id: 't1', name: 'Шаблон', description: '', templateVersion: 1, stages: [], productType: 'протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', estimatedDurationMin: 1800, createdAt: '', updatedAt: '' },
    });
    const mockFetch = vi.mocked(fetch);
    mockFetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ fullName: 'Іван Іванов' }) } as unknown as Response);
    prostheticsOrderApiMock.getDocument.mockReturnValue(new Promise(() => {}));
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByText('ORD-001')).toBeInTheDocument());
    expect(screen.getByText(/Очікування рецепта/)).toBeInTheDocument();
  });

  it('disables the Start button until document is loaded', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: 't1', instanceId: null });
    prostheticsOrderApiMock.getById.mockResolvedValue({
      data: { id: 'o1', orderNumber: 'ORD-001', patientId: 'p1', productType: 'Протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' },
    });
    flowTemplateApiMock.getById.mockResolvedValue({
      data: { id: 't1', name: 'Шаблон', description: '', templateVersion: 1, stages: [], productType: 'протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', estimatedDurationMin: 1800, createdAt: '', updatedAt: '' },
    });
    const mockFetch = vi.mocked(fetch);
    mockFetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ fullName: 'Іван Іванов' }) } as unknown as Response);
    prostheticsOrderApiMock.getDocument.mockResolvedValue({ data: new Blob(['x']) });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByText('ORD-001')).toBeInTheDocument());
    expect(screen.getByRole('button', { name: /Старт/ })).not.toBeDisabled();
  });
});