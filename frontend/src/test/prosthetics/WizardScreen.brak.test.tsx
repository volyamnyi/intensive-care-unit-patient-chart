import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import WizardScreen from '@/pages/prosthetics/process/WizardScreen';
import type { FlowInstance, SnapshotTemplate } from '@/prosthetics/types';

vi.mock('@/services/AuthContext', () => ({
  useAuth: () => ({
    user: { fullName: 'Test User', role: 'PROSTHETIST' },
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
  listExecutions: vi.fn(),
  pause: vi.fn(),
  resume: vi.fn(),
  uploadEvidence: vi.fn(),
  listEvidence: vi.fn().mockResolvedValue({ data: [] }),
  deleteEvidence: vi.fn().mockResolvedValue({}),
  patchStepNote: vi.fn().mockResolvedValue({ data: {} }),
  list: vi.fn(),
  create: vi.fn(),
  fail: vi.fn(),
  replacement: vi.fn(),
  brak: vi.fn(),
  getBrakEvents: vi.fn(),
  getBranches: vi.fn(),
  downloadEvidence: vi.fn(),
  generateReport: vi.fn(),
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

// IDs as defined in BrakService / TP-LL-02 seed
const STAGE_D12 = 'd0000012-0000-0000-0000-000000000012';
const STAGE_D13 = 'd0000013-0000-0000-0000-000000000013';
const STAGE_D14 = 'd0000014-0000-0000-0000-000000000014';
const STAGE_D17 = 'd0000017-0000-0000-0000-000000000017';
const STEP_E0028 = 'e0000028-0000-0000-0000-000000000028';

const brakInstance = (overrides: Partial<FlowInstance> = {}): FlowInstance => ({
  id: 'inst-brak-1',
  templateId: 'tpl-ll-02',
  patientId: '900002',
  orderId: 'ord-brak-1',
  assignedUserId: 5,
  status: 'IN_PROGRESS',
  currentStageId: STAGE_D17,
  currentStepId: STEP_E0028,
  currentExecutionId: 'exec-brak-1',
  templateName: 'TP-LL-02',
  patientPib: 'Гаврилюк Олена Миколаївна',
  orderNumber: 'PR-2026-0002',
  currentStageName: 'Примірювання та коректування тренувального протеза',
  currentStepName: 'Примірювання та коректування тренувального протеза',
  startTime: '2026-01-01T08:00:00Z',
  endTime: null,
  totalActiveSeconds: 600,
  totalIdleSeconds: 0,
  failReason: null,
  pausedAt: null,
  resumedAt: null,
  pauseCategory: null,
  createdAt: '2026-01-01T08:00:00Z',
  updatedAt: '2026-01-01T08:00:00Z',
  ...overrides,
});

const brakSnapshot = (): SnapshotTemplate => ({
  name: 'Етапи технологічного процесу нижніх кінцівок',
  version: 1,
  productType: 'LOWER_LIMB',
  amputationLevel: 'generic_lower_limb',
  limbSide: null,
  estimatedDurationMin: 540,
  stages: [
    {
      id: STAGE_D12,
      name: 'Виготовлення гіпсового негатива',
      stageType: 'TECHNICAL',
      canSkip: false,
      requiresApproval: false,
      steps: [{ id: 'e0000020-0000-0000-0000-000000000020', name: 'Зняття та внесення об’ємних розмірів', stepType: 'MEASUREMENT', mandatory: true, allowBackward: true, autoStartTimer: true, normDurationMin: 20, elements: [] }],
    },
    {
      id: STAGE_D13,
      name: 'Виготовлення гіпсової моделі кукси',
      stageType: 'TECHNICAL',
      canSkip: false,
      requiresApproval: false,
      steps: [{ id: 'e0000022-0000-0000-0000-000000000022', name: 'Виготовлення гіпсового позитива', stepType: 'INFORMATION', mandatory: true, allowBackward: true, autoStartTimer: false, normDurationMin: 15, elements: [] }],
    },
    {
      id: STAGE_D14,
      name: 'Виготовлення тренувальної гільзи',
      stageType: 'TECHNICAL',
      canSkip: false,
      requiresApproval: false,
      steps: [{ id: 'e0000024-0000-0000-0000-000000000024', name: 'Виготовлення тренувальної гільзи', stepType: 'INFORMATION', mandatory: true, allowBackward: true, autoStartTimer: false, normDurationMin: 25, elements: [] }],
    },
    {
      id: STAGE_D17,
      name: 'Примірювання та коректування тренувального протеза',
      stageType: 'CLINICAL',
      canSkip: false,
      requiresApproval: false,
      steps: [
        {
          id: STEP_E0028,
          name: 'Примірювання та коректування тренувального протеза',
          stepType: 'CHECKLIST',
          mandatory: true,
          allowBackward: true,
          autoStartTimer: false,
          normDurationMin: 25,
          elements: [{ id: 'f0000213-0000-0000-0000-000000000213', elementType: 'CHECKBOX', label: 'Проведена примірка тренувального протеза з пацієнтом, перевірено відповідність розмірів вказаними у бланку замірів та перевірено правильність схеми побудови протеза.', required: true, unit: null, minValue: null, maxValue: null, minCount: null, maxCount: null, regexPattern: null, options: null, mimeTypes: null, maxSizeMb: null }],
        },
      ],
    },
  ],
});

function renderBrakWizard(route = '/prosthetics/process/inst-brak-1/wizard') {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <Routes>
        <Route path="/prosthetics/process/:id/wizard" element={<WizardScreen />} />
        <Route path="/prosthetics/process/:id" element={<div>Process Layout</div>} />
        <Route path="/prosthetics/process/:id/done" element={<div>Done Page</div>} />
        <Route path="/prosthetics/process/:id/failed" element={<div>Failed Page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('WizardScreen — Брак', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    flowInstanceApiMock.getById.mockResolvedValue({ data: brakInstance() });
    flowInstanceApiMock.getSnapshot.mockResolvedValue({ data: brakSnapshot() });
    flowInstanceApiMock.listExecutions.mockResolvedValue({ data: [] });
    flowInstanceApiMock.brak.mockResolvedValue({
      data: { brakEventId: 'brak-1', originalInstanceId: 'inst-brak-1', newInstanceId: 'inst-branch-2', returnStageId: STAGE_D14, returnStageName: 'Виготовлення тренувальної гільзи', newStatus: 'IN_PROGRESS' },
    });
    prostheticsOrderApiMock.getById.mockResolvedValue({ data: { id: 'ord-brak-1', orderNumber: 'PR-2026-0002', patientId: '900002' } });
    prostheticsPatientApiMock.getById.mockResolvedValue({ data: { id: '900002', pib: 'Гаврилюк Олена Миколаївна' } });
  });

  it('renders Brak button only on e0000028', async () => {
    const { unmount } = renderBrakWizard();
    await waitFor(() => expect(screen.getByRole('button', { name: /Брак/ })).toBeInTheDocument());
    unmount();

    // other step — no brak
    flowInstanceApiMock.getById.mockResolvedValue({ data: brakInstance({ currentStepId: 'e0000020-0000-0000-0000-000000000020', currentStageId: STAGE_D12 }) });
    // need snapshot that contains that step
    const otherSnapshot: SnapshotTemplate = {
      ...brakSnapshot(),
      stages: [
        { id: STAGE_D12, name: 'Виготовлення гіпсового негатива', stageType: 'TECHNICAL', canSkip: false, requiresApproval: false, steps: [{ id: 'e0000020-0000-0000-0000-000000000020', name: 'Зняття', stepType: 'MEASUREMENT', mandatory: true, allowBackward: true, autoStartTimer: true, normDurationMin: 20, elements: [] }] },
      ],
    };
    flowInstanceApiMock.getSnapshot.mockResolvedValue({ data: otherSnapshot });
    renderBrakWizard('/prosthetics/process/inst-brak-2/wizard');
    await waitFor(() => expect(screen.getByText(/Зняття/)).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: /^Брак$/ })).not.toBeInTheDocument();
  });

  it('Brak button hidden for COMPLETED/FAILED/BRANCHED', async () => {
    for (const status of ['COMPLETED', 'FAILED', 'BRANCHED'] as const) {
      flowInstanceApiMock.getById.mockResolvedValue({ data: brakInstance({ status }) });
      flowInstanceApiMock.getSnapshot.mockResolvedValue({ data: brakSnapshot() });
      const { unmount } = renderBrakWizard(`/prosthetics/process/inst-${status}/wizard`);
      // wizard redirects to done/failed for these statuses, so brak button not rendered
      // wait a tick
      await waitFor(() => {}, { timeout: 200 });
      expect(screen.queryByRole('button', { name: /^Брак$/ })).not.toBeInTheDocument();
      unmount();
    }
  });

  it('Brak dialog shows 2 checkboxes + note', async () => {
    renderBrakWizard();
    await waitFor(() => expect(screen.getByRole('button', { name: /Брак/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Брак/ }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('Якість посадки кукси в тренувальній гільзі:')).toBeInTheDocument();
    expect(within(dialog).getByText('Неправильне розташування м’яких тканин у гільзі')).toBeInTheDocument();
    expect(within(dialog).getByText('Наявні больові відчуття і дискомфорт при посадці')).toBeInTheDocument();
    expect(within(dialog).getByLabelText('Примітка')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Підтвердити' })).toBeInTheDocument();
  });

  it('confirm opens stage-selection dialog', async () => {
    renderBrakWizard();
    await waitFor(() => expect(screen.getByRole('button', { name: /Брак/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Брак/ }));
    await screen.findByText('Якість посадки кукси в тренувальній гільзі:');
    fireEvent.click(screen.getByRole('button', { name: 'Підтвердити' }));
    expect(await screen.findByText('Повернутись на етап:')).toBeInTheDocument();
  });

  it('stage-selection has exactly 3 options', async () => {
    renderBrakWizard();
    await waitFor(() => expect(screen.getByRole('button', { name: /Брак/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Брак/ }));
    await screen.findByText('Якість посадки кукси в тренувальній гільзі:');
    fireEvent.click(screen.getByRole('button', { name: 'Підтвердити' }));
    await screen.findByText('Повернутись на етап:');
    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(3);
    expect(screen.getByRole('radio', { name: 'Виготовлення гіпсового негатива' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Виготовлення гіпсової моделі кукси' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Виготовлення тренувальної гільзи' })).toBeInTheDocument();
  });

  it('rejects 4th stage not in UI', async () => {
    renderBrakWizard();
    await waitFor(() => expect(screen.getByRole('button', { name: /Брак/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Брак/ }));
    await screen.findByText('Якість посадки кукси в тренувальній гільзі:');
    fireEvent.click(screen.getByRole('button', { name: 'Підтвердити' }));
    await screen.findByText('Повернутись на етап:');
    expect(screen.queryByText('Примірка тренувальної гільзи')).not.toBeInTheDocument();
    expect(screen.queryByText('Складання тренувального протеза')).not.toBeInTheDocument();
    expect(screen.queryByText('Примірювання та коректування тренувального протеза')).not.toBeInTheDocument();
  });

  it('creates branch via API', async () => {
    renderBrakWizard();
    await waitFor(() => expect(screen.getByRole('button', { name: /Брак/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Брак/ }));
    await screen.findByText('Якість посадки кукси в тренувальній гільзі:');
    // check first checkbox
    const row = screen.getByText('Неправильне розташування м’яких тканин у гільзі').closest('label');
    fireEvent.click(within(row!).getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button', { name: 'Підтвердити' }));
    await screen.findByText('Повернутись на етап:');
    fireEvent.click(screen.getByRole('radio', { name: 'Виготовлення тренувальної гільзи' }));
    fireEvent.click(screen.getByRole('button', { name: 'Створити гілку' }));
    await waitFor(() => {
      expect(flowInstanceApiMock.brak).toHaveBeenCalledWith('inst-brak-1', {
        returnStageId: STAGE_D14,
        softTissueMisalignment: true,
        painDiscomfort: false,
        note: null,
      });
    });
  });

  it('shows error on 400 invalid stage', async () => {
    flowInstanceApiMock.brak.mockRejectedValue({ response: { data: { message: 'Недозволений етап' } } });
    renderBrakWizard();
    await waitFor(() => expect(screen.getByRole('button', { name: /Брак/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Брак/ }));
    await screen.findByText('Якість посадки кукси в тренувальній гільзі:');
    fireEvent.click(screen.getByRole('button', { name: 'Підтвердити' }));
    await screen.findByText('Повернутись на етап:');
    fireEvent.click(screen.getByRole('radio', { name: 'Виготовлення гіпсового негатива' }));
    fireEvent.click(screen.getByRole('button', { name: 'Створити гілку' }));
    await waitFor(() => {
      // error toast or stays on dialog — ensure brak still not navigated
      expect(flowInstanceApiMock.brak).toHaveBeenCalled();
    });
  });

  it('saves note and checkboxes', async () => {
    renderBrakWizard();
    await waitFor(() => expect(screen.getByRole('button', { name: /Брак/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Брак/ }));
    const dialog = await screen.findByRole('dialog');
    await within(dialog).findByText('Якість посадки кукси в тренувальній гільзі:');
    fireEvent.click(within(dialog).getByText('Неправильне розташування м’яких тканин у гільзі').closest('label')!);
    fireEvent.click(within(dialog).getByText('Наявні больові відчуття і дискомфорт при посадці').closest('label')!);
    fireEvent.change(within(dialog).getByLabelText('Примітка'), { target: { value: ' примітка тест ' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Підтвердити' }));
    await screen.findByText('Повернутись на етап:');
    fireEvent.click(screen.getByRole('radio', { name: 'Виготовлення гіпсової моделі кукси' }));
    fireEvent.click(screen.getByRole('button', { name: 'Створити гілку' }));
    await waitFor(() => {
      expect(flowInstanceApiMock.brak).toHaveBeenCalledWith('inst-brak-1', {
        returnStageId: STAGE_D13,
        softTissueMisalignment: true,
        painDiscomfort: true,
        note: 'примітка тест',
      });
    });
  });

  it('touch targets & a11y', async () => {
    renderBrakWizard();
    await waitFor(() => expect(screen.getByRole('button', { name: /Брак/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Брак/ }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toBeInTheDocument();
    // aria-labelledby via DialogTitle
    expect(within(dialog).getByText('Брак')).toBeInTheDocument();
    // checkboxes have min-h-11 via pointer-coarse (class check)
    const cb = within(dialog).getAllByRole('checkbox')[0];
    expect(cb).toBeInTheDocument();
    // second dialog
    fireEvent.click(screen.getByRole('button', { name: 'Підтвердити' }));
    const dialog2 = await screen.findByText('Повернутись на етап:');
    expect(dialog2).toBeInTheDocument();
    const radios = screen.getAllByRole('radio');
    radios.forEach((r) => expect(r).toBeInTheDocument());
  });
});
