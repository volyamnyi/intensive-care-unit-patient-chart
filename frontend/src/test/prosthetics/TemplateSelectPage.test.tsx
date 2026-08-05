import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import TemplateSelectPage from '@/pages/prosthetics/setup/TemplateSelectPage';

const flowTemplateApiMock = vi.hoisted(() => ({
  list: vi.fn(),
}));

const flowInstanceApiMock = vi.hoisted(() => ({
  create: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  flowTemplateApi: flowTemplateApiMock,
  flowInstanceApi: flowInstanceApiMock,
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
  mockUseProsthetics({ patientId: 'p1', orderId: 'o1', templateId: null, instanceId: null });
  return render(
    <MemoryRouter initialEntries={['/prosthetics/new/select-template']}>
      <TemplateSelectPage />
    </MemoryRouter>,
  );
}

describe('TemplateSelectPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Вибір технологічного маршруту')).toBeInTheDocument();
    });
  });

  it('fetches templates with the correct filters', async () => {
    flowTemplateApiMock.list.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(flowTemplateApiMock.list).toHaveBeenCalledWith({
        status: 'ACTIVE',
        productType: 'протез',
        amputationLevel: 'both',
        limbSide: 'both',
      });
    });
  });

  it('renders template cards', async () => {
    flowTemplateApiMock.list.mockResolvedValue({
      data: [
        {
          id: 't1', name: 'Протез гомілки', description: 'Стандартний', productType: 'протез',
          amputationLevel: 'above', limbSide: 'both', status: 'ACTIVE', templateVersion: 1,
          estimatedDurationMin: 1800, stages: [{ id: 's1', name: 'Етап 1', stageType: 'TECH', canSkip: false, requiresApproval: false, gate: null, steps: [] }],
          createdAt: '', updatedAt: '',
        },
      ],
    });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Протез гомілки')).toBeInTheDocument();
    });
  });

  it('selects a template and creates an instance', async () => {
    flowTemplateApiMock.list.mockResolvedValue({
      data: [
        {
          id: 't1', name: 'Протез гомілки', description: '', productType: 'протез',
          amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', templateVersion: 1,
          estimatedDurationMin: 1800, stages: [{ id: 's1', name: 'Етап 1', stageType: 'TECH', canSkip: false, requiresApproval: false, gate: null, steps: [] }],
          createdAt: '', updatedAt: '',
        },
      ],
    });
    flowInstanceApiMock.create.mockResolvedValue({ data: { id: 'i1' } });
    renderPage();
    await waitFor(() => expect(screen.getByText('Протез гомілки')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Протез гомілки'));
    fireEvent.click(screen.getByRole('button', { name: /Обрати/i }));
    await waitFor(() => {
      expect(flowInstanceApiMock.create).toHaveBeenCalledWith({ orderId: 'o1', templateId: 't1' });
    });
  });

  it('shows a 409 conflict error when process already exists', async () => {
    flowTemplateApiMock.list.mockResolvedValue({
      data: [
        {
          id: 't1', name: 'Протез гомілки', description: '', productType: 'протез',
          amputationLevel: 'above', limbSide: 'left', status: 'ACTIVE', templateVersion: 1,
          estimatedDurationMin: 1800, stages: [{ id: 's1', name: 'Етап 1', stageType: 'TECH', canSkip: false, requiresApproval: false, gate: null, steps: [] }],
          createdAt: '', updatedAt: '',
        },
      ],
    });
    flowInstanceApiMock.create.mockRejectedValue({ response: { status: 409 } });
    renderPage();
    await waitFor(() => expect(screen.getByText('Протез гомілки')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Протез гомілки'));
    fireEvent.click(screen.getByRole('button', { name: /Обрати/i }));
    await waitFor(() => {
      expect(screen.getByText(/Процес для цього замовлення вже існує/)).toBeInTheDocument();
    });
  });

  it('shows error on other failures', async () => {
    flowTemplateApiMock.list.mockRejectedValue(new Error('network'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Не вдалося завантажити шаблони/)).toBeInTheDocument();
    });
  });

  it('renders the no-order guard when draft.orderId is missing', () => {
    mockUseProsthetics({ patientId: 'p1', orderId: null as string | null, templateId: null as string | null, instanceId: null as string | null });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/select-template']}>
        <TemplateSelectPage />
      </MemoryRouter>,
    );
    expect(screen.getByText(/Необхідно обрати замовлення/)).toBeInTheDocument();
  });
});