import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import DoneScreen from '@/pages/prosthetics/process/DoneScreen';
import type { FlowInstance, SnapshotTemplate } from '@/prosthetics/types';

vi.mock('@/services/AuthContext', () => ({
  useAuth: () => ({
    user: { fullName: 'Test User', role: 'PROSTHETIST' },
    selectApp: vi.fn(),
    hasRole: vi.fn(),
    isAuthenticated: true,
    loading: false,
  }),
}));

const flowInstanceApiMock = vi.hoisted(() => ({
  getById: vi.fn(),
  getSnapshot: vi.fn(),
  listExecutions: vi.fn(),
  listGateDecisions: vi.fn(),
  listResources: vi.fn(),
  generateReport: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  flowInstanceApi: flowInstanceApiMock,
}));

const completedInstance: FlowInstance = {
  id: 'inst-1',
  templateId: 'tpl-1',
  patientId: 'pat-1',
  orderId: 'ord-1',
  assignedUserId: 5,
  status: 'COMPLETED',
  currentStageId: null,
  currentStepId: null,
  currentExecutionId: null,
  templateName: 'TP-LL-01',
  patientPib: 'Гаврилюк Тарас Олексійович',
  orderNumber: 'ПВ-26-0414',
  currentStageName: null,
  currentStepName: null,
  startTime: '2026-01-01T08:00:00Z',
  endTime: '2026-01-01T11:30:00Z',
  totalActiveSeconds: 12600,
  totalIdleSeconds: 1800,
  reworkCount: 1,
  failReason: null,
  pausedAt: null,
  resumedAt: null,
  pauseCategory: null,
  createdAt: '2026-01-01T08:00:00Z',
  updatedAt: '2026-01-01T11:30:00Z',
};

const snapshot: SnapshotTemplate = {
  name: 'Протез гомілки',
  version: 3,
  productType: 'Протез',
  amputationLevel: 'гомілка',
  limbSide: 'ліва',
  estimatedDurationMin: 300,
  stages: [
    {
      id: 'stage-1',
      name: 'Виготовлення',
      stageType: 'TECHNICAL',
      canSkip: false,
      requiresApproval: false,
      gate: null,
      steps: [{ id: 's1', name: 'Мірки', stepType: 'MEASUREMENT', mandatory: true, allowBackward: false, autoStartTimer: true, normDurationMin: 30, elements: [] }],
    },
  ],
};

describe('DoneScreen', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    flowInstanceApiMock.getById.mockResolvedValue({ data: completedInstance });
    flowInstanceApiMock.getSnapshot.mockResolvedValue({ data: snapshot });
    flowInstanceApiMock.listExecutions.mockResolvedValue({
      data: [
        {
          id: 'ex-1',
          instanceId: 'inst-1',
          stepId: 's1',
          stepName: 'Мірки',
          attemptNumber: 1,
          status: 'COMPLETED',
          startedAt: '2026-01-01T08:00:00Z',
          completedAt: '2026-01-01T08:30:00Z',
          activeSeconds: 1800,
          values: null,
          completedBy: 5,
        },
      ],
    });
    flowInstanceApiMock.listGateDecisions.mockResolvedValue({ data: [] });
    flowInstanceApiMock.listResources.mockResolvedValue({ data: [] });
  });

  it('renders success summary with stats', async () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/process/inst-1/done']}>
        <Routes>
          <Route path="/prosthetics/process/:id/done" element={<DoneScreen />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Процес успішно завершено')).toBeInTheDocument();
    });
    expect(screen.getByText(/Протез гомілки \(v3\)/)).toBeInTheDocument();
    expect(screen.getByText('3 год 30 хв')).toBeInTheDocument();
    expect(screen.getByText('Доопрацювань')).toBeInTheDocument();
    const reworkCard = screen.getByText('Доопрацювань').parentElement;
    expect(reworkCard?.firstElementChild?.textContent).toBe('1');
    expect(screen.getByText(/Етапи виготовлення/)).toBeInTheDocument();
    expect(screen.getByText('Виготовлення')).toBeInTheDocument();
    expect(screen.getByText('1/1 кроків')).toBeInTheDocument();
  });

  it('shows error state when instance cannot be loaded', async () => {
    flowInstanceApiMock.getById.mockRejectedValue(new Error('network'));
    render(
      <MemoryRouter initialEntries={['/prosthetics/process/inst-1/done']}>
        <Routes>
          <Route path="/prosthetics/process/:id/done" element={<DoneScreen />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Підсумок недоступний')).toBeInTheDocument();
    });
  });

  it('exports the PDF report on button click', async () => {
    const createObjectURL = vi.fn(() => 'blob:mock');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { ...URL, createObjectURL, revokeObjectURL });
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    flowInstanceApiMock.generateReport.mockResolvedValue({ data: new Blob() });
    render(
      <MemoryRouter initialEntries={['/prosthetics/process/inst-1/done']}>
        <Routes>
          <Route path="/prosthetics/process/:id/done" element={<DoneScreen />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Процес успішно завершено')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /Експортувати PDF/ }));
    await waitFor(() => {
      expect(flowInstanceApiMock.generateReport).toHaveBeenCalledWith('inst-1');
    });
    expect(createObjectURL).toHaveBeenCalled();
    clickSpy.mockRestore();
  });
});
