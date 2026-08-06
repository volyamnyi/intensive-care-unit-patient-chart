import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import DashboardPage from '@/pages/prosthetics/DashboardPage';
import type { FlowInstance } from '@/prosthetics/types';

const flowInstanceApiMock = vi.hoisted(() => ({
  list: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  flowInstanceApi: flowInstanceApiMock,
}));

const useProsthetics = vi.hoisted(() => vi.fn());

vi.mock('@/prosthetics/ProstheticsContext', () => ({
  useProsthetics,
}));

function mockUseProsthetics(draft = { patientId: null, orderId: null, templateId: null, instanceId: null }) {
  useProsthetics.mockReturnValue({
    draft,
    setDraftField: vi.fn(),
    resetDraft: vi.fn(),
  });
}

const baseInstance = (overrides: Partial<FlowInstance> = {}): FlowInstance => ({
  id: 'i1',
  templateId: 't1',
  patientId: 'p1',
  orderId: 'o1',
  assignedUserId: null,
  status: 'IN_PROGRESS',
  currentStageId: 'stage-1',
  currentStepId: 'step-1',
  currentExecutionId: 'exec-1',
  templateName: 'TP-UL-01',
  patientPib: 'Сніжко Оксана Володимирівна',
  orderNumber: 'ПВ-26-0413',
  currentStageName: 'Виготовлення гільзи',
  currentStepName: 'Гіпсовий негатив',
  startTime: null,
  endTime: null,
  totalActiveSeconds: null,
  totalIdleSeconds: null,
  reworkCount: null,
  failReason: null,
  pausedAt: null,
  resumedAt: null,
  pauseCategory: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  ...overrides,
});

function renderPage() {
  mockUseProsthetics();
  return render(
    <MemoryRouter initialEntries={['/prosthetics']}>
      <Routes>
        <Route path="/prosthetics" element={<DashboardPage />} />
        <Route path="/prosthetics/process/:id/wizard" element={<div>Wizard Page</div>} />
        <Route path="/prosthetics/process/:id/done" element={<div>Done Page</div>} />
        <Route path="/prosthetics/process/:id/failed" element={<div>Failed Page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    flowInstanceApiMock.list.mockResolvedValue({ data: [] });
  });

  it('renders the page title and new-process button', () => {
    renderPage();
    expect(screen.getByText('Виробництво протезів')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Новий процес/ })).toBeInTheDocument();
  });

  it('fetches all instances on mount', async () => {
    renderPage();
    await waitFor(() => {
      expect(flowInstanceApiMock.list).toHaveBeenCalledWith({});
    });
  });

  it('renders stat cards with counts', async () => {
    flowInstanceApiMock.list.mockResolvedValue({
      data: [
        baseInstance({ id: 'i1', status: 'IN_PROGRESS' }),
        baseInstance({ id: 'i2', status: 'PAUSED' }),
        baseInstance({ id: 'i3', status: 'COMPLETED' }),
        baseInstance({ id: 'i4', status: 'FAILED' }),
        baseInstance({ id: 'i5', status: 'NEW' }),
      ],
    });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Активні')).toBeInTheDocument();
    });
    expect(screen.getByText('Призупинені')).toBeInTheDocument();
    expect(screen.getByText('Завершені')).toBeInTheDocument();
    expect(screen.getByText('Провалені')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('renders instances with display columns', async () => {
    flowInstanceApiMock.list.mockResolvedValue({ data: [baseInstance()] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('ПВ-26-0413')).toBeInTheDocument();
    });
    expect(screen.getByText('Сніжко Оксана Володимирівна')).toBeInTheDocument();
    expect(screen.getByText('TP-UL-01')).toBeInTheDocument();
    expect(screen.getByText('Виготовлення гільзи')).toBeInTheDocument();
    expect(screen.getByText('Гіпсовий негатив')).toBeInTheDocument();
  });

  it('filters instances client-side when a tab is selected', async () => {
    flowInstanceApiMock.list.mockResolvedValue({
      data: [
        baseInstance({ id: 'i1', status: 'IN_PROGRESS' }),
        baseInstance({ id: 'i2', status: 'PAUSED' }),
      ],
    });
    renderPage();
    await waitFor(() => expect(screen.getByText('ПВ-26-0413')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('tab', { name: 'Призупинені' }));
    await waitFor(() => {
      expect(screen.queryByText('Гіпсовий негатив')).not.toBeInTheDocument();
    });
  });

  it('filters instances client-side by search query', async () => {
    flowInstanceApiMock.list.mockResolvedValue({
      data: [
        baseInstance({ id: 'i1', orderNumber: 'ПВ-26-0413' }),
        baseInstance({ id: 'i2', orderNumber: 'ПВ-26-0414' }),
      ],
    });
    renderPage();
    await waitFor(() => expect(screen.getByText('ПВ-26-0413')).toBeInTheDocument());
    fireEvent.change(screen.getByPlaceholderText(/пошук/i), { target: { value: '0414' } });
    await waitFor(() => {
      expect(screen.queryByText('ПВ-26-0413')).not.toBeInTheDocument();
    });
    expect(screen.getByText('ПВ-26-0414')).toBeInTheDocument();
  });

  it('navigates to the wizard on row click for an in-progress instance', async () => {
    flowInstanceApiMock.list.mockResolvedValue({ data: [baseInstance()] });
    renderPage();
    await waitFor(() => expect(screen.getByText('ПВ-26-0413')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('row', { name: /Сніжко/ }));
    await waitFor(() => {
      expect(screen.getByText('Wizard Page')).toBeInTheDocument();
    });
  });

  it('navigates to the done screen for a completed instance', async () => {
    flowInstanceApiMock.list.mockResolvedValue({ data: [baseInstance({ status: 'COMPLETED' })] });
    renderPage();
    await waitFor(() => expect(screen.getByText('ПВ-26-0413')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('row', { name: /Сніжко/ }));
    await waitFor(() => {
      expect(screen.getByText('Done Page')).toBeInTheDocument();
    });
  });

  it('navigates to the failed screen for a failed instance', async () => {
    flowInstanceApiMock.list.mockResolvedValue({ data: [baseInstance({ status: 'FAILED' })] });
    renderPage();
    await waitFor(() => expect(screen.getByText('ПВ-26-0413')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('row', { name: /Сніжко/ }));
    await waitFor(() => {
      expect(screen.getByText('Failed Page')).toBeInTheDocument();
    });
  });

  it('renders empty state when no instances match', async () => {
    flowInstanceApiMock.list.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Немає процесів за поточним фільтром/)).toBeInTheDocument();
    });
  });

  it('renders an error alert on fetch failure', async () => {
    flowInstanceApiMock.list.mockRejectedValue(new Error('network'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Не вдалося завантажити процеси/)).toBeInTheDocument();
    });
  });

  it('navigates to new process on button click', async () => {
    renderPage();
    await waitFor(() => expect(flowInstanceApiMock.list).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /Новий процес/ }));
  });
});
