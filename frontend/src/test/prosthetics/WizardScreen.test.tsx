import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import WizardScreen from '@/pages/prosthetics/process/WizardScreen';
import type { FlowInstance, SnapshotTemplate } from '@/prosthetics/types';

vi.mock('@/services/AuthContext', () => ({
  useAuth: () => ({
    user: { fullName: 'Test User', role: 'PROSTHETIST' },
    selectApp: vi.fn(),
    hasRole: vi.fn(() => false),
    isAuthenticated: true,
    loading: false,
  }),
}));

const flowInstanceApiMock = vi.hoisted(() => ({
  getById: vi.fn(),
  getSnapshot: vi.fn(),
  start: vi.fn(),
  completeStep: vi.fn(),
  pause: vi.fn(),
  resume: vi.fn(),
  decideGate: vi.fn(),
  uploadEvidence: vi.fn(),
  list: vi.fn(),
  create: vi.fn(),
  fail: vi.fn(),
  replacement: vi.fn(),
  downloadEvidence: vi.fn(),
  generateReport: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  flowInstanceApi: flowInstanceApiMock,
}));

const inProgressInstance: FlowInstance = {
  id: 'inst-1',
  templateId: 'tpl-1',
  patientId: 'pat-1',
  orderId: 'ord-1',
  assignedUserId: 5,
  status: 'IN_PROGRESS',
  currentStageId: 'stage-1',
  currentStepId: 'step-1',
  currentExecutionId: 'exec-1',
  startTime: '2026-01-01T08:00:00Z',
  endTime: null,
  totalActiveSeconds: 120,
  totalIdleSeconds: 0,
  reworkCount: 0,
  failReason: null,
  pausedAt: null,
  resumedAt: null,
  pauseCategory: null,
  createdAt: '2026-01-01T08:00:00Z',
  updatedAt: '2026-01-01T08:00:00Z',
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
      steps: [
        {
          id: 'step-1',
          name: 'Зняття мірок',
          stepType: 'MEASUREMENT',
          mandatory: true,
          allowBackward: false,
          autoStartTimer: true,
          normDurationMin: 30,
          elements: [
            { id: 'el-1', elementType: 'NUMERIC_INPUT', label: 'Обхват', required: true, unit: 'см', minValue: 10, maxValue: 80, minCount: null, maxCount: null, regexPattern: null, options: null, mimeTypes: null, maxSizeMb: null },
            { id: 'el-2', elementType: 'TEXT_INPUT', label: 'Примітка', required: false, unit: null, minValue: null, maxValue: null, minCount: null, maxCount: null, regexPattern: null, options: null, mimeTypes: null, maxSizeMb: null },
          ],
        },
        {
          id: 'step-2',
          name: 'Збірка',
          stepType: 'CHECKLIST',
          mandatory: true,
          allowBackward: false,
          autoStartTimer: true,
          normDurationMin: 60,
          elements: [
            { id: 'el-3', elementType: 'CHECKBOX', label: 'Підтвердити збірку', required: true, unit: null, minValue: null, maxValue: null, minCount: null, maxCount: null, regexPattern: null, options: null, mimeTypes: null, maxSizeMb: null },
          ],
        },
      ],
    },
  ],
};

function renderWizard(route = '/prosthetics/process/inst-1/wizard') {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <Routes>
        <Route path="/prosthetics/process/:id/wizard" element={<WizardScreen />} />
        <Route path="/prosthetics/process/:id" element={<div>Process Layout</div>} />
        <Route path="/prosthetics/process/:id/done" element={<div>Done Page</div>} />
        <Route path="/prosthetics/process/:id/failed" element={<div>Failed Page</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('WizardScreen', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    flowInstanceApiMock.getById.mockResolvedValue({ data: inProgressInstance });
    flowInstanceApiMock.getSnapshot.mockResolvedValue({ data: snapshot });
    flowInstanceApiMock.start.mockResolvedValue({ data: inProgressInstance });
  });

  it('renders step header, title and elements', async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Зняття мірок')).toBeInTheDocument();
    });
    expect(screen.getByText('Протез гомілки')).toBeInTheDocument();
    expect(screen.getByText(/Обхват, см/)).toBeInTheDocument();
    expect(screen.getByText('Завершити крок')).toBeInTheDocument();
  });

  it('does not call completeStep while required fields are empty', async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Зняття мірок')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Завершити крок/ }));
    await waitFor(() => {
      expect(flowInstanceApiMock.completeStep).not.toHaveBeenCalled();
    });
  });

  it('calls completeStep with values and resources after valid fill', async () => {
    flowInstanceApiMock.completeStep.mockResolvedValue({
      data: { ...inProgressInstance, currentStepId: 'step-2' },
    });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Зняття мірок')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/Обхват, см/), { target: { value: '42' } });
    fireEvent.change(screen.getByPlaceholderText('Матеріал'), { target: { value: 'ПВХ' } });
    fireEvent.change(screen.getByPlaceholderText('Кількість'), { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: /Додати/ }));

    fireEvent.click(screen.getByRole('button', { name: /Завершити крок/ }));
    await waitFor(() => {
      expect(flowInstanceApiMock.completeStep).toHaveBeenCalledWith(
        'inst-1',
        'exec-1',
        expect.objectContaining({
          resources: [{ material: 'ПВХ', quantity: 2, unit: 'шт', minutes: null }],
        })
      );
    });
  });

  it('opens pause dialog and calls pause API', async () => {
    flowInstanceApiMock.pause.mockResolvedValue({
      data: { ...inProgressInstance, status: 'PAUSED', pauseCategory: 'MATERIAL' },
    });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Зняття мірок')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Пауза/ }));
    expect(screen.getByText('Призупинення роботи')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('radio', { name: 'Відсутні матеріали' }));
    fireEvent.click(screen.getByRole('button', { name: 'Призупинити' }));

    await waitFor(() => {
      expect(flowInstanceApiMock.pause).toHaveBeenCalledWith('inst-1', { category: 'MATERIAL' });
    });
  });
});
