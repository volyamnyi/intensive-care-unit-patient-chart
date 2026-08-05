import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
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
});
