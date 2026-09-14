import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
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
  backward: vi.fn(),
  listExecutions: vi.fn().mockResolvedValue({ data: [] }),
  pause: vi.fn(),
  resume: vi.fn(),
  fail: vi.fn(),
  brak: vi.fn(),
  uploadEvidence: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  flowInstanceApi: flowInstanceApiMock,
  prostheticsOrderApi: prostheticsOrderApiMock,
  prostheticsPatientApi: prostheticsPatientApiMock,
}));

const prostheticsOrderApiMock = vi.hoisted(() => ({
  getById: vi.fn(),
}));

const prostheticsPatientApiMock = vi.hoisted(() => ({
  getById: vi.fn(),
}));

const STAGE_TRAINING_SOCKET = 'd0000014-0000-0000-0000-000000000014';
const STEP_TRAINING_SOCKET = 'e0000024-0000-0000-0000-000000000024';
const STAGE_NEGATIVE = 'd0000012-0000-0000-0000-000000000012';
const STEP_PLASTER_NEGATIVE = 'e0000021-0000-0000-0000-000000000021';

const checkboxElement = (id: string, label: string) => ({
  id,
  elementType: 'CHECKBOX',
  label,
  required: true,
  unit: null,
  minValue: null,
  maxValue: null,
  minCount: null,
  maxCount: null,
  regexPattern: null,
  options: null,
  mimeTypes: null,
  maxSizeMb: null,
});

function snapshotFor(stageId: string, stageName: string, stepId: string, stepName: string): SnapshotTemplate {
  return {
    name: 'TP-LL-02',
    version: 1,
    productType: 'Протез',
    amputationLevel: 'гомілка',
    limbSide: 'ліва',
    estimatedDurationMin: 540,
    stages: [
      {
        id: stageId,
        name: stageName,
        stageType: 'TECHNICAL',
        canSkip: false,
        requiresApproval: false,
        steps: [
          {
            id: stepId,
            name: stepName,
            stepType: 'INFORMATION',
            mandatory: true,
            allowBackward: true,
            autoStartTimer: false,
            normDurationMin: 25,
            elements: [checkboxElement('el-1', 'Підтвердити виконання')],
          },
        ],
      },
    ],
  };
}

function instanceAt(stageId: string, stepId: string): FlowInstance {
  return {
    id: 'inst-1',
    templateId: 'tpl-1',
    patientId: 'pat-1',
    orderId: 'ord-1',
    assignedUserId: 5,
    status: 'IN_PROGRESS',
    currentStageId: stageId,
    currentStepId: stepId,
    currentExecutionId: 'exec-1',
    templateName: 'TP-LL-02',
    patientPib: 'Гаврилюк Олена Миколаївна',
    orderNumber: 'ПВ-26-0414',
    currentStageName: null,
    currentStepName: null,
    startTime: '2026-01-01T08:00:00Z',
    endTime: null,
    totalActiveSeconds: 120,
    totalIdleSeconds: 0,
    failReason: null,
    pausedAt: null,
    resumedAt: null,
    pauseCategory: null,
    createdAt: '2026-01-01T08:00:00Z',
    updatedAt: '2026-01-01T08:00:00Z',
  };
}

function renderWizard() {
  return render(
    <MemoryRouter initialEntries={['/prosthetics/process/inst-1/wizard']}>
      <Routes>
        <Route path="/prosthetics/process/:id/wizard" element={<WizardScreen />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('WizardScreen PPE notices wiring', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    flowInstanceApiMock.listExecutions.mockResolvedValue({ data: [] });
    prostheticsOrderApiMock.getById.mockResolvedValue({
      data: { id: 'ord-1', orderNumber: 'ПВ-26-0414', patientId: 'pat-1' },
    });
    prostheticsPatientApiMock.getById.mockResolvedValue({
      data: { id: 'pat-1', pib: 'Гаврилюк Олена Миколаївна' },
    });
  });

  it('e0000024 рендерить обидва банери, full-kit вище за nitrile', async () => {
    flowInstanceApiMock.getById.mockResolvedValue({
      data: instanceAt(STAGE_TRAINING_SOCKET, STEP_TRAINING_SOCKET),
    });
    flowInstanceApiMock.getSnapshot.mockResolvedValue({
      data: snapshotFor(
        STAGE_TRAINING_SOCKET,
        'Виготовлення тренувальної гільзи',
        STEP_TRAINING_SOCKET,
        'Виготовлення тренувальної гільзи',
      ),
    });
    renderWizard();

    const kit = await screen.findByTestId('ppe-notice-full-kit');
    const nitrile = await screen.findByTestId('ppe-notice-nitrile');
    expect(kit).toBeInTheDocument();
    expect(nitrile).toBeInTheDocument();
    // Порядок у DOM: комплект першим (масив getPpeNotices).
    expect(kit.compareDocumentPosition(nitrile) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    // Банери стоять перед формою кроку.
    const formControl = screen.getByText('Підтвердити виконання');
    expect(kit.compareDocumentPosition(formControl) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('e0000021 не рендерить жодного банера', async () => {
    flowInstanceApiMock.getById.mockResolvedValue({
      data: instanceAt(STAGE_NEGATIVE, STEP_PLASTER_NEGATIVE),
    });
    flowInstanceApiMock.getSnapshot.mockResolvedValue({
      data: snapshotFor(
        STAGE_NEGATIVE,
        'Виготовлення гіпсового негатива',
        STEP_PLASTER_NEGATIVE,
        'Виготовлення гіпсового негатива',
      ),
    });
    renderWizard();

    await waitFor(() => {
      expect(screen.getByText('КРОК 1: Виготовлення гіпсового негатива')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('ppe-notice-full-kit')).not.toBeInTheDocument();
    expect(screen.queryByTestId('ppe-notice-nitrile')).not.toBeInTheDocument();
  });
});
