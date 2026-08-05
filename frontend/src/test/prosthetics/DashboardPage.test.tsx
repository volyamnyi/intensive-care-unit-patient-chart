import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import DashboardPage from '@/pages/prosthetics/DashboardPage';
import type { FlowInstance } from '@/prosthetics/types';

const flowInstanceApiMock = vi.hoisted(() => ({
  list: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  flowInstanceApi: flowInstanceApiMock,
}));

vi.mock('@/prosthetics/ProstheticsContext', () => ({
  useProsthetics: vi.fn(),
}));

const { useProsthetics } = vi.mocked(require('@/prosthetics/ProstheticsContext'));

function mockUseProsthetics(draft = { patientId: null, orderId: null, templateId: null, instanceId: null }) {
  useProsthetics.mockReturnValue({
    draft,
    setDraftField: vi.fn(),
    resetDraft: vi.fn(),
  });
}

function renderPage() {
  mockUseProsthetics();
  return render(
    <MemoryRouter initialEntries={['/prosthetics']}>
      <DashboardPage />
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

  it('fetches instances on mount', async () => {
    renderPage();
    await waitFor(() => {
      expect(flowInstanceApiMock.list).toHaveBeenCalledWith({});
    });
  });

  it('passes active status filter to the API', async () => {
    renderPage();
    await waitFor(() => expect(flowInstanceApiMock.list).toHaveBeenCalled());
    const tabs = screen.getAllByRole('tab');
    fireEvent.click(tabs[1]);
    await waitFor(() => {
      expect(flowInstanceApiMock.list).toHaveBeenCalledWith({ status: 'IN_PROGRESS' });
    });
  });

  it('passes search query to the API', async () => {
    renderPage();
    await waitFor(() => expect(flowInstanceApiMock.list).toHaveBeenCalled());
    fireEvent.change(screen.getByPlaceholderText(/пошук/i), { target: { value: 'ord-1' } });
    await waitFor(() => {
      expect(flowInstanceApiMock.list).toHaveBeenCalledWith({ query: 'ord-1' });
    });
  });

  it('renders instances in a table', async () => {
    const instances: FlowInstance[] = [
      {
        id: 'i1', templateId: 't1', patientId: 'p1', orderId: 'o1', assignedUserId: null,
        status: 'IN_PROGRESS', currentStageId: null, currentStepId: null, currentExecutionId: null,
        startTime: null, endTime: null, totalActiveSeconds: null, totalIdleSeconds: null,
        reworkCount: null, failReason: null, pausedAt: null, resumedAt: null, pauseCategory: null,
        createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
      },
    ];
    flowInstanceApiMock.list.mockResolvedValue({ data: instances });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('i1')).toBeInTheDocument();
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