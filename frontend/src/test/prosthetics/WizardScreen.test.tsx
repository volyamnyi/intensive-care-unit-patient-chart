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
  saveDraft: vi.fn(),
  backward: vi.fn(),
  listExecutions: vi.fn(),
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

const inProgressInstance = (overrides: Partial<FlowInstance> = {}): FlowInstance => ({
  id: 'inst-1',
  templateId: 'tpl-1',
  patientId: 'pat-1',
  orderId: 'ord-1',
  assignedUserId: 5,
  status: 'IN_PROGRESS',
  currentStageId: 'stage-1',
  currentStepId: 'step-1',
  currentExecutionId: 'exec-1',
  templateName: 'TP-LL-01',
  patientPib: 'Гаврилюк Тарас Олексійович',
  orderNumber: 'ПВ-26-0414',
  currentStageName: 'Виготовлення',
  currentStepName: 'Зняття мірок',
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
  ...overrides,
});

const baseSnapshot = (): SnapshotTemplate => ({
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
          allowBackward: true,
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
});

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
    flowInstanceApiMock.getById.mockResolvedValue({ data: inProgressInstance() });
    flowInstanceApiMock.getSnapshot.mockResolvedValue({ data: baseSnapshot() });
    flowInstanceApiMock.start.mockResolvedValue({ data: inProgressInstance() });
    flowInstanceApiMock.listExecutions.mockResolvedValue({ data: [] });
  });

  it('renders step header, title, patient info and elements', async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Зняття мірок')).toBeInTheDocument();
    });
    expect(screen.getByText('Протез гомілки')).toBeInTheDocument();
    expect(screen.getByText(/Гаврилюк Тарас Олексійович · ПВ-26-0414/)).toBeInTheDocument();
    expect(screen.getByText(/Обхват, см/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Готово →/ })).toBeInTheDocument();
  });

  it('does not call completeStep while required fields are empty', async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Зняття мірок')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Готово/ }));
    await waitFor(() => {
      expect(flowInstanceApiMock.completeStep).not.toHaveBeenCalled();
    });
  });

  it('calls completeStep with values and resources after valid fill', async () => {
    flowInstanceApiMock.completeStep.mockResolvedValue({
      data: { ...inProgressInstance(), currentStepId: 'step-2' },
    });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Зняття мірок')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/Обхват, см/), { target: { value: '42' } });
    fireEvent.change(screen.getByPlaceholderText('Матеріал'), { target: { value: 'ПВХ' } });
    fireEvent.change(screen.getByPlaceholderText('Кількість'), { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: /Додати/ }));

    fireEvent.click(screen.getByRole('button', { name: /Готово/ }));
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

  it('restores the saved draft values of the current execution', async () => {
    flowInstanceApiMock.listExecutions.mockResolvedValue({
      data: [
        { id: 'exec-1', instanceId: 'inst-1', stageId: 'stage-1', stepId: 'step-1', attemptNumber: 1, status: 'IN_PROGRESS', startedAt: null, completedAt: null, activeSeconds: 0, values: '{"el-1":"42"}', completedBy: null },
      ],
    });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByDisplayValue('42')).toBeInTheDocument();
    });
  });

  it('saves a draft without validation when the button is clicked', async () => {
    flowInstanceApiMock.saveDraft.mockResolvedValue({ data: inProgressInstance() });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Зняття мірок')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/Обхват, см/), { target: { value: '42' } });
    fireEvent.click(screen.getByRole('button', { name: /Зберегти чернетку/ }));
    await waitFor(() => {
      expect(flowInstanceApiMock.saveDraft).toHaveBeenCalledWith(
        'inst-1',
        'exec-1',
        expect.objectContaining({ values: expect.stringContaining('42') })
      );
    });
    expect(flowInstanceApiMock.completeStep).not.toHaveBeenCalled();
  });

  it('disables the backward button on the first step of a stage', async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Зняття мірок')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /Попередній/ })).toBeDisabled();
  });

  it('calls backward API when navigating to the previous step', async () => {
    flowInstanceApiMock.backward.mockResolvedValue({ data: inProgressInstance() });
    flowInstanceApiMock.getById.mockResolvedValue({
      data: inProgressInstance({
        currentStepId: 'step-2',
        currentExecutionId: 'exec-2',
        currentStepName: 'Збірка',
      }),
    });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Збірка')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /Попередній/ }));
    await waitFor(() => {
      expect(flowInstanceApiMock.backward).toHaveBeenCalledWith('inst-1');
    });
  });

  it('shows "Завершити процес" CTA on the last step of the last stage', async () => {
    const singleStageSnapshot: SnapshotTemplate = {
      ...baseSnapshot(),
      stages: [
        {
          ...baseSnapshot().stages[0],
          steps: [baseSnapshot().stages[0].steps[0]],
        },
      ],
    };
    flowInstanceApiMock.getSnapshot.mockResolvedValue({ data: singleStageSnapshot });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Завершити процес/ })).toBeInTheDocument();
    });
  });

  it('shows "Контроль якості →" CTA before a gated stage', async () => {
    const gatedSnapshot: SnapshotTemplate = {
      ...baseSnapshot(),
      stages: [
        {
          ...baseSnapshot().stages[0],
          steps: [baseSnapshot().stages[0].steps[0]],
        },
        {
          id: 'stage-2',
          name: 'Контроль якості',
          stageType: 'ADMINISTRATIVE',
          canSkip: false,
          requiresApproval: true,
          gate: {
            id: 'gate-1',
            name: 'Контрольна точка якості',
            requiredApproverRole: 'PROSTHETICS_ADMINISTRATOR',
            checklist: ['Розмір', 'Функціональність'],
            attachmentsRequired: false,
            reworkLoops: [],
          },
          steps: [],
        },
      ],
    };
    flowInstanceApiMock.getSnapshot.mockResolvedValue({ data: gatedSnapshot });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Контроль якості →/ })).toBeInTheDocument();
    });
  });

  it('opens pause dialog and calls pause API', async () => {
    flowInstanceApiMock.pause.mockResolvedValue({
      data: { ...inProgressInstance(), status: 'PAUSED', pauseCategory: 'MATERIAL' },
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
