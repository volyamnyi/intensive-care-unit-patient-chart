import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import FailedScreen from '@/pages/prosthetics/process/FailedScreen';
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

const failedInstance: FlowInstance = {
  id: 'inst-1',
  templateId: 'tpl-1',
  patientId: 'pat-1',
  orderId: 'ord-1',
  assignedUserId: 5,
  status: 'FAILED',
  currentStageId: null,
  currentStepId: null,
  currentExecutionId: null,
  startTime: '2026-01-01T08:00:00Z',
  endTime: '2026-01-01T09:00:00Z',
  totalActiveSeconds: 3600,
  totalIdleSeconds: 0,
  reworkCount: 2,
  failReason: 'Деформація гільзи при полімеризації',
  pausedAt: null,
  resumedAt: null,
  pauseCategory: null,
  createdAt: '2026-01-01T08:00:00Z',
  updatedAt: '2026-01-01T09:00:00Z',
};

const snapshot: SnapshotTemplate = {
  name: 'Протез гомілки',
  version: 3,
  productType: 'Протез',
  amputationLevel: 'гомілка',
  limbSide: 'ліва',
  estimatedDurationMin: 300,
  stages: [],
};

describe('FailedScreen', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    flowInstanceApiMock.getById.mockResolvedValue({ data: failedInstance });
    flowInstanceApiMock.getSnapshot.mockResolvedValue({ data: snapshot });
  });

  it('renders failure report with reason', async () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/process/inst-1/failed']}>
        <Routes>
          <Route path="/prosthetics/process/:id/failed" element={<FailedScreen />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Процес зупинено (брак)')).toBeInTheDocument();
    });
    expect(screen.getByText('Деформація гільзи при полімеризації')).toBeInTheDocument();
    expect(screen.getByText('Причина')).toBeInTheDocument();
  });

  it('shows rework count before failure', async () => {
    render(
      <MemoryRouter initialEntries={['/prosthetics/process/inst-1/failed']}>
        <Routes>
          <Route path="/prosthetics/process/:id/failed" element={<FailedScreen />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Доопрацювань до зупинки')).toBeInTheDocument();
    });
    expect(screen.getByText('2')).toBeInTheDocument();
  });
});
