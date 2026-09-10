import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OrderSelectPage from '@/pages/prosthetics/setup/OrderSelectPage';

const prostheticsOrderApiMock = vi.hoisted(() => ({
  listByPatient: vi.fn(),
  listMisDocuments: vi.fn(),
}));

const misDoc = {
  documentId: 681078,
  documentCreationDate: '2026-09-07T13:32:12.1',
  documentTemplateId: 121,
  documentTemplateName: 'Замовлення на протези нижніх кінцівок',
  documentUrl: 'https://mis.example/document?request=abc',
  productName: '06.12.09 : Ортези на колінний суглоб',
};

const prostheticsPatientApiMock = vi.hoisted(() => ({
  listCandidates: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  prostheticsOrderApi: prostheticsOrderApiMock,
  prostheticsPatientApi: prostheticsPatientApiMock,
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
    prostheticsPatientApiMock.listCandidates.mockResolvedValue({ data: [] });
    prostheticsOrderApiMock.listMisDocuments.mockResolvedValue({ data: [] });
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
      expect(screen.getByText(/Немає локальних замовлень для цього пацієнта/)).toBeInTheDocument();
    });
  });

  it('fetches MIS limb-order documents for the selected patient', async () => {
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(prostheticsOrderApiMock.listMisDocuments).toHaveBeenCalledWith('p1');
    });
  });

  it('renders MIS order documents in a table', async () => {
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({ data: [] });
    prostheticsOrderApiMock.listMisDocuments.mockResolvedValue({ data: [misDoc] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Замовлення на протези нижніх кінцівок')).toBeInTheDocument();
    });
    expect(screen.getByText('06.12.09 : Ортези на колінний суглоб')).toBeInTheDocument();
  });

  it('shows MIS empty state when no limb-order documents exist', async () => {
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({ data: [] });
    prostheticsOrderApiMock.listMisDocuments.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Замовлень на протези в MIS не знайдено/)).toBeInTheDocument();
    });
  });

  it('shows MIS error on documents fetch failure', async () => {
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({ data: [] });
    prostheticsOrderApiMock.listMisDocuments.mockRejectedValue(new Error('network'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Не вдалося завантажити замовлення MIS/)).toBeInTheDocument();
    });
  });

  it('stores the MIS document in the draft on select', async () => {
    const setDraftField = vi.fn();
    useProsthetics.mockReturnValue({
      draft: { patientId: 'p1', orderId: null, templateId: null, instanceId: null },
      setDraftField,
      resetDraft: vi.fn(),
    });
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({ data: [] });
    prostheticsOrderApiMock.listMisDocuments.mockResolvedValue({ data: [misDoc] });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/select-order']}>
        <OrderSelectPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByText('Замовлення на протези нижніх кінцівок')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Обрати/i }));
    expect(setDraftField).toHaveBeenCalledWith('misDocumentUrl', 'https://mis.example/document?request=abc');
    expect(setDraftField).toHaveBeenCalledWith('misDocumentId', '681078');
    expect(setDraftField).toHaveBeenCalledWith('misDocumentTemplateName', 'Замовлення на протези нижніх кінцівок');
  });

  it('shows error on fetch failure', async () => {
    prostheticsOrderApiMock.listByPatient.mockRejectedValue(new Error('network'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Не вдалося завантажити замовлення/)).toBeInTheDocument();
    });
  });

  it('shows MIS document badges for the selected patient candidate', async () => {
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({ data: [] });
    prostheticsPatientApiMock.listCandidates.mockResolvedValue({
      data: [
        {
          patient: { id: 'p1', pib: 'Іван', birthDate: '1990-01-01', gender: 'Чоловіча' },
          orders: [],
          documents: [
            { documentId: 120, documentTemplateId: 120, documentTemplateName: 'Замовлення на протези' },
            { documentId: 121, documentTemplateId: 121, documentTemplateName: 'Висновок лікаря' },
          ],
          documentsUnknown: false,
        },
      ],
    });
    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId('mis-documents')).toBeInTheDocument();
    });
    expect(screen.getByText('Замовлення на протези')).toBeInTheDocument();
    expect(screen.getByText('Висновок лікаря')).toBeInTheDocument();
  });

  it('shows a hint when candidate documents are unknown', async () => {
    prostheticsOrderApiMock.listByPatient.mockResolvedValue({ data: [] });
    prostheticsPatientApiMock.listCandidates.mockResolvedValue({
      data: [
        {
          patient: { id: 'p1', pib: 'Іван', birthDate: '1990-01-01', gender: 'Чоловіча' },
          orders: [],
          documents: [],
          documentsUnknown: true,
        },
      ],
    });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Документи MIS недоступні/)).toBeInTheDocument();
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