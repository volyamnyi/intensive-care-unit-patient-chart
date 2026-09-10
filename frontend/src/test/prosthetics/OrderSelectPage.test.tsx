import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OrderSelectPage from '@/pages/prosthetics/setup/OrderSelectPage';

const prostheticsOrderApiMock = vi.hoisted(() => ({
  listMisDocuments: vi.fn(),
  provisionFromMis: vi.fn(),
}));

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

const misDoc = {
  documentId: 681078,
  documentCreationDate: '2026-09-07T13:32:12.1',
  documentTemplateId: 121,
  documentTemplateName: 'Замовлення на протези нижніх кінцівок',
  documentUrl: 'https://mis.example/document?request=abc',
  productName: '06.12.09 : Ортези на колінний суглоб',
};

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

  it('fetches MIS limb-order documents for the selected patient', async () => {
    renderPage();
    await waitFor(() => {
      expect(prostheticsOrderApiMock.listMisDocuments).toHaveBeenCalledWith('p1');
    });
  });

  it('does not depend on local orders', async () => {
    renderPage();
    await waitFor(() => {
      expect(prostheticsOrderApiMock.listMisDocuments).toHaveBeenCalled();
    });
    expect('listByPatient' in prostheticsOrderApiMock).toBe(false);
    expect(screen.queryByText(/локальних замовлень/)).not.toBeInTheDocument();
  });

  it('renders MIS order documents in a table', async () => {
    prostheticsOrderApiMock.listMisDocuments.mockResolvedValue({ data: [misDoc] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Замовлення на протези нижніх кінцівок')).toBeInTheDocument();
    });
    expect(screen.getByText('06.12.09 : Ортези на колінний суглоб')).toBeInTheDocument();
  });

  it('shows MIS empty state when no limb-order documents exist', async () => {
    prostheticsOrderApiMock.listMisDocuments.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Замовлень на протези в MIS не знайдено/)).toBeInTheDocument();
    });
  });

  it('shows MIS error on documents fetch failure', async () => {
    prostheticsOrderApiMock.listMisDocuments.mockRejectedValue(new Error('network'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Не вдалося завантажити замовлення MIS/)).toBeInTheDocument();
    });
  });

  it('shows MIS document badges for the selected patient candidate', async () => {
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

  it('provisions the local order and stores it in the draft on MIS select', async () => {
    const setDraftField = vi.fn();
    useProsthetics.mockReturnValue({
      draft: { patientId: 'p1', orderId: null, templateId: null, instanceId: null },
      setDraftField,
      resetDraft: vi.fn(),
    });
    prostheticsOrderApiMock.listMisDocuments.mockResolvedValue({ data: [misDoc] });
    prostheticsOrderApiMock.provisionFromMis.mockResolvedValue({ data: { id: 'ord-1' } });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/select-order']}>
        <OrderSelectPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByText('Замовлення на протези нижніх кінцівок')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Обрати/i }));
    await waitFor(() => {
      expect(prostheticsOrderApiMock.provisionFromMis).toHaveBeenCalledWith({
        patientId: 'p1',
        documentId: 681078,
      });
    });
    await waitFor(() => {
      expect(setDraftField).toHaveBeenCalledWith('orderId', 'ord-1');
    });
    expect(setDraftField).toHaveBeenCalledWith('misDocumentUrl', 'https://mis.example/document?request=abc');
    expect(setDraftField).toHaveBeenCalledWith('misDocumentId', '681078');
    expect(setDraftField).toHaveBeenCalledWith('misDocumentTemplateName', 'Замовлення на протези нижніх кінцівок');
  });

  it('shows an error and stays on step 2 when provisioning fails', async () => {
    const setDraftField = vi.fn();
    useProsthetics.mockReturnValue({
      draft: { patientId: 'p1', orderId: null, templateId: null, instanceId: null },
      setDraftField,
      resetDraft: vi.fn(),
    });
    prostheticsOrderApiMock.listMisDocuments.mockResolvedValue({ data: [misDoc] });
    prostheticsOrderApiMock.provisionFromMis.mockRejectedValue({
      response: { data: { message: 'Документ замовлення недоступний' } },
    });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/select-order']}>
        <OrderSelectPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByText('Замовлення на протези нижніх кінцівок')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Обрати/i }));
    await waitFor(() => {
      expect(screen.getByText('Документ замовлення недоступний')).toBeInTheDocument();
    });
    expect(setDraftField).not.toHaveBeenCalled();
  });
});
