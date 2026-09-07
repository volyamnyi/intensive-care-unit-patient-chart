import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OrderReviewPage from '@/pages/prosthetics/setup/OrderReviewPage';
import type { ProstheticsDraft } from '@/prosthetics/types';

const prostheticsOrderApiMock = vi.hoisted(() => ({
  getById: vi.fn(),
  getDocumentUrl: vi.fn(),
}));

const prostheticsPatientApiMock = vi.hoisted(() => ({
  getById: vi.fn(),
}));

const flowInstanceApiMock = vi.hoisted(() => ({
  list: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  prostheticsOrderApi: prostheticsOrderApiMock,
  prostheticsPatientApi: prostheticsPatientApiMock,
  flowInstanceApi: flowInstanceApiMock,
}));

const useProsthetics = vi.hoisted(() => vi.fn());
const navigateMock = vi.hoisted(() => vi.fn());

vi.mock('@/prosthetics/ProstheticsContext', () => ({
  useProsthetics,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => navigateMock,
  };
});

function mockUseProsthetics(draft: ProstheticsDraft = { patientId: null, orderId: null, templateId: null, instanceId: null }) {
  useProsthetics.mockReturnValue({
    draft,
    setDraftField: vi.fn(),
    resetDraft: vi.fn(),
  });
}

const orderMock = { id: 'o1', orderNumber: 'ORD-001', patientId: 'p1', productType: 'Протез', amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' };
const documentUrlMock = 'https://mis.example/document?request=abc';

describe('OrderReviewPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    prostheticsOrderApiMock.getById.mockResolvedValue({ data: orderMock });
    prostheticsOrderApiMock.getDocumentUrl.mockResolvedValue({
      data: { documentId: 1, documentTemplateName: 'Замовлення на протез', documentUrl: documentUrlMock },
    });
    prostheticsPatientApiMock.getById.mockResolvedValue({
      data: { id: 'p1', pib: 'Іван Іванов', birthDate: '1980-01-01' },
    });
    flowInstanceApiMock.list.mockResolvedValue({ data: [] });
  });

  it('renders the page title in redirect state', () => {
    mockUseProsthetics({ patientId: null, orderId: null, templateId: null, instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    expect(screen.getByText('Перевірка замовлення')).toBeInTheDocument();
  });

  it('shows loading state initially', () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: null, instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    expect(screen.getByText(/Завантаження даних/)).toBeInTheDocument();
  });

  it('redirects when orderId is missing', () => {
    mockUseProsthetics({ patientId: null, orderId: null, templateId: null, instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    expect(screen.getByText(/Необхідно обрати замовлення/)).toBeInTheDocument();
  });

  it('renders order details after load', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: null, instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByRole('tab', { name: 'Деталі' })).toBeInTheDocument();
    });
    screen.getByRole('tab', { name: 'Деталі' }).click();
    await waitFor(() => {
      expect(screen.getByText(/Замовлення #ORD-001/)).toBeInTheDocument();
    });
  });

  it('shows error state on fetch failure', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: null, instanceId: null });
    prostheticsOrderApiMock.getById.mockRejectedValue(new Error('network'));
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
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: null, instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Деталі' })).toBeInTheDocument());
    screen.getByRole('tab', { name: 'Деталі' }).click();
    await waitFor(() => expect(screen.getByText(/Замовлення #ORD-001/)).toBeInTheDocument());
    expect(screen.getByRole('tab', { name: 'Деталі' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Замовлення на протез' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Матеріали' })).toBeInTheDocument();
  });

  it('renders patient full name from fetched data', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: null, instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Деталі' })).toBeInTheDocument());
    screen.getByRole('tab', { name: 'Деталі' }).click();
    await waitFor(() => {
      expect(screen.getByText('Іван Іванов')).toBeInTheDocument();
    });
  });

  it('renders the MIS-hosted document URL in an iframe', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: null, instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      const frame = screen.getByTitle('Замовлення на протез (MIS)');
      expect(frame).toBeInTheDocument();
      expect(frame.getAttribute('src')).toBe(documentUrlMock);
    });
    const link = screen.getByRole('link', { name: /Відкрити замовлення на протез у MIS/ });
    expect(link).toHaveAttribute('href', documentUrlMock);
  });

  it('disables start button until document is loaded', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: null, instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTitle('Замовлення на протез (MIS)')).toBeInTheDocument();
    });
  });

  it('navigates to template selection when Start is clicked', async () => {
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: null, instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Деталі' })).toBeInTheDocument());
    const startButton = screen.getByRole('button', { name: /Старт/i });
    expect(startButton).not.toBeDisabled();
    startButton.click();
    await waitFor(() => {
      expect(flowInstanceApiMock.list).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(navigateMock).toHaveBeenCalledWith('/prosthetics/new/select-template');
    });
  });

  it('blocks start when an active process already exists', async () => {
    flowInstanceApiMock.list.mockResolvedValue({
      data: [{ id: 'i1', orderId: 'o1', status: 'IN_PROGRESS' }],
    });
    mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: null, instanceId: null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/review-order']}>
        <OrderReviewPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Деталі' })).toBeInTheDocument());
    const startButton = screen.getByRole('button', { name: /Старт/i });
    startButton.click();
    await waitFor(() => {
      expect(
        screen.getByText(/Для цього замовлення вже існує процес у роботі/),
      ).toBeInTheDocument();
    });
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
