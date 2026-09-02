import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
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
  listExecutions: vi.fn(),
  pause: vi.fn(),
  resume: vi.fn(),
  decideGate: vi.fn(),
  uploadEvidence: vi.fn(),
  listEvidence: vi.fn().mockResolvedValue({ data: [] }),
  deleteEvidence: vi.fn().mockResolvedValue({}),
  patchStepNote: vi.fn().mockResolvedValue({ data: {} }),
  list: vi.fn(),
  create: vi.fn(),
  fail: vi.fn(),
  replacement: vi.fn(),
  downloadEvidence: vi.fn(),
  generateReport: vi.fn(),
  brak: vi.fn(),
  getBrakEvents: vi.fn(),
  getBranches: vi.fn(),
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
  patientPib: 'Гаврилюк Олена Миколаївна',
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
    prostheticsOrderApiMock.getById.mockResolvedValue({
      data: { id: 'ord-1', orderNumber: 'ПВ-26-0414', patientId: 'pat-1' },
    });
    prostheticsPatientApiMock.getById.mockResolvedValue({
      data: { id: 'pat-1', pib: 'Гаврилюк Олена Миколаївна' },
    });
  });

  it('renders step header, title, patient info and elements', async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText(/Зняття мірок/)).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText(/Гаврилюк Олена Миколаївна · ПВ-26-0414/)).toBeInTheDocument();
    });
    expect(screen.getByText('Протез гомілки')).toBeInTheDocument();
    expect(screen.getByText(/Обхват, см/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Готово →/ })).toBeInTheDocument();
  });

  it('does not call completeStep while required fields are empty', async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText(/Зняття мірок/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Готово/ }));
    await waitFor(() => {
      expect(flowInstanceApiMock.completeStep).not.toHaveBeenCalled();
    });
  });

  it('calls completeStep with values after valid fill (resources panel removed)', async () => {
    flowInstanceApiMock.completeStep.mockResolvedValue({
      data: { ...inProgressInstance(), currentStepId: 'step-2' },
    });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText(/Зняття мірок/)).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/Обхват, см/), { target: { value: '42' } });

    fireEvent.click(screen.getByRole('button', { name: /Готово/ }));
    await waitFor(() => {
      expect(flowInstanceApiMock.completeStep).toHaveBeenCalledWith(
        'inst-1',
        'exec-1',
        expect.objectContaining({
          values: expect.any(String),
        })
      );
    });
    const callArgs = flowInstanceApiMock.completeStep.mock.calls[0][2] as {
      values: string;
      resources?: unknown;
    };
    expect(callArgs.resources).toBeUndefined();
    expect(JSON.parse(callArgs.values)).toMatchObject({ 'el-1': '42' });
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


  it('disables the backward button on the first step of a stage', async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText(/Зняття мірок/)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /Попередній/ })).toBeDisabled();
  });

  it('keeps the process timer running across step navigation (does not reset to zero)', async () => {
    // totalActiveSeconds is the accumulated active time of the whole process
    // (here: 2 minutes from earlier steps → baseline 00:02:00). Start on step-2
    // (Збірка); listExecutions is empty so the seed effect uses the baseline.
    flowInstanceApiMock.getById.mockResolvedValue({
      data: inProgressInstance({ currentStepId: 'step-2', currentStepName: 'Збірка' }),
    });
    flowInstanceApiMock.listExecutions.mockResolvedValue({ data: [] });
    // Navigating back to step-1 keeps the same execution, so the restore effect
    // must NOT re-seed and the (removed) per-step reset must not zero it either.
    flowInstanceApiMock.backward.mockResolvedValue({
      data: inProgressInstance({ currentStepId: 'step-1', currentStepName: 'Зняття мірок' }),
    });
    renderWizard();

    await waitFor(() => {
      expect(screen.getByText(/Збірка/)).toBeInTheDocument();
    });
    // Baseline (00:02:00) is visible while on step-2.
    await waitFor(() => {
      expect(screen.getByText(/^00:02:0\d/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Попередній/ }));
    await waitFor(() => {
      expect(flowInstanceApiMock.backward).toHaveBeenCalledWith('inst-1');
    });
    await waitFor(() => {
      expect(screen.getByText(/Зняття мірок/)).toBeInTheDocument();
    });

    // After the step change the timer still reflects the accumulated process
    // baseline — never reset back to 00:00:00.
    await waitFor(() => {
      expect(screen.getByText(/^00:02:0\d/)).toBeInTheDocument();
    });
    expect(screen.queryByText(/^00:00:0\d/)).not.toBeInTheDocument();
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
      expect(screen.getByText(/Збірка/)).toBeInTheDocument();
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
      data: { ...inProgressInstance(), status: 'PAUSED', pauseCategory: 'VLC_PASSING' },
    });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText(/Зняття мірок/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Пауза/ }));
    expect(screen.getByText('Призупинення роботи')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('radio', { name: 'Відсутні матеріали' }));
    fireEvent.click(screen.getByRole('button', { name: 'Призупинити' }));

    await waitFor(() => {
      expect(flowInstanceApiMock.pause).toHaveBeenCalledWith('inst-1', { category: 'VLC_PASSING' });
    });
  });

  it('shows fail button on every step and opens the fail dialog', async () => {
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText(/Зняття мірок/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Позначити процес як провалений/ }));
    expect(screen.getByText('Категорія провалу')).toBeInTheDocument();
    expect(screen.getByText('Детальний опис причини')).toBeInTheDocument();
    expect(screen.getByText('Файли (опційно)')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Позначити як провалений/ })).toBeInTheDocument();
  });

  it('does not call fail API while category or description is missing', async () => {
    flowInstanceApiMock.fail.mockResolvedValue({ data: inProgressInstance() });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText(/Зняття мірок/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Позначити процес як провалений/ }));
    fireEvent.click(screen.getByRole('button', { name: /Позначити як провалений/ }));
    await waitFor(() => {
      expect(flowInstanceApiMock.fail).not.toHaveBeenCalled();
    });
  });

  it('calls fail API with category and description then navigates to the failed report', async () => {
    flowInstanceApiMock.fail.mockResolvedValue({
      data: { ...inProgressInstance(), status: 'FAILED' },
    });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText(/Зняття мірок/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Позначити процес як провалений/ }));
    fireEvent.click(screen.getByRole('combobox', { name: /Категорія провалу/ }));
    fireEvent.click(await screen.findByRole('option', { name: 'Виробничий дефект' }));
    fireEvent.change(screen.getByLabelText(/Детальний опис причини/), {
      target: { value: 'Гільза тріснула під час ламінації' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Позначити як провалений/ }));

    await waitFor(() => {
      expect(flowInstanceApiMock.fail).toHaveBeenCalledWith('inst-1', {
        category: 'defect',
        description: 'Гільза тріснула під час ламінації',
      });
    });
    await waitFor(() => {
      expect(screen.getByText('Failed Page')).toBeInTheDocument();
    });
  });

  it('toggles a checkbox from anywhere on its parent row surface', async () => {
    flowInstanceApiMock.getById.mockResolvedValue({
      data: inProgressInstance({ currentStepId: 'step-2', currentStepName: 'Збірка' }),
    });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText(/Збірка/)).toBeInTheDocument();
    });

    // The accessible name of the Base UI checkbox span comes from the label
    // association only in a real browser engine — in jsdom's
    // dom-accessibility-api a span[role=checkbox] has no name, so scope the
    // query to the row (the label wrapper) instead.
    const row = screen.getByText('Підтвердити збірку').closest('label');
    expect(row).not.toBeNull();
    const checkbox = within(row!).getByRole('checkbox');
    expect(checkbox).toHaveAttribute('aria-checked', 'false');

    // Click on the label text — native label activation toggles the control.
    fireEvent.click(screen.getByText('Підтвердити збірку'));
    await waitFor(() => {
      expect(checkbox).toHaveAttribute('aria-checked', 'true');
    });

    // Click on the row surface (the label element, away from text/control).
    fireEvent.click(row!);
    await waitFor(() => {
      expect(checkbox).toHaveAttribute('aria-checked', 'false');
    });

    // Clicking the checkbox control itself stays a single toggle (the label
    // must not re-fire for interactive content).
    fireEvent.click(checkbox);
    await waitFor(() => {
      expect(checkbox).toHaveAttribute('aria-checked', 'true');
    });
  });

  it('Phase 3: soft-liner exclusive checkbox logic — skip CTA removed, third checkbox present and transition gated', async () => {
    const conditionalSnapshot: SnapshotTemplate = {
      ...baseSnapshot(),
      stages: [
        {
          id: 'stage-soft',
          name: 'Виготовлення пом\'якшуючого вкладиша та постійної гільзи',
          stageType: 'TECHNICAL',
          canSkip: false,
          requiresApproval: false,
          gate: null,
          steps: [
            {
              id: 'e0000029-0000-0000-0000-000000000029',
              name: 'Виготовлення пом\'якшуючого вкладиша',
              stepType: 'CHECKLIST',
              mandatory: false,
              allowBackward: true,
              autoStartTimer: false,
              normDurationMin: 20,
              elements: [
                { id: 'f0000214-0000-0000-0000-000000000214', elementType: 'CHECKBOX', label: 'Візуальний контроль чистоти помʼякшуючого вкладиша: відсутній пил, стружка, забруднення та інші залишки від механічної обробки.', required: false, unit: null, minValue: null, maxValue: null, minCount: null, maxCount: null, regexPattern: null, options: null, mimeTypes: null, maxSizeMb: null },
                { id: 'f0000215-0000-0000-0000-000000000215', elementType: 'CHECKBOX', label: 'Тактильний контроль поверхні та якість обробки країв помʼякшуючого вкладиша: помʼякшуючий вкладиш перевірено на відсутність задирок, тріщин та гострих кромок. Краї заокруглені та відполіровані. Поверхня гладка та рівномірна, без жорстких включень та виступів.', required: false, unit: null, minValue: null, maxValue: null, minCount: null, maxCount: null, regexPattern: null, options: null, mimeTypes: null, maxSizeMb: null },
                { id: 'f0000240-0000-0000-0000-000000000240', elementType: 'CHECKBOX', label: 'Помʼякшуючий вкладиш не потрібен', required: false, unit: null, minValue: null, maxValue: null, minCount: null, maxCount: null, regexPattern: null, options: null, mimeTypes: null, maxSizeMb: null },
              ],
            },
            {
              id: 'e0000030-0000-0000-0000-000000000030',
              name: 'Виготовлення постійної гільзи',
              stepType: 'CHECKLIST',
              mandatory: true,
              allowBackward: true,
              autoStartTimer: false,
              normDurationMin: 30,
              elements: [
                { id: 'f-perm-1', elementType: 'CHECKBOX', label: 'Візуальний контроль гільзи', required: true, unit: null, minValue: null, maxValue: null, minCount: null, maxCount: null, regexPattern: null, options: null, mimeTypes: null, maxSizeMb: null },
              ],
            },
          ],
        },
      ],
    };
    flowInstanceApiMock.getById.mockResolvedValue({
      data: inProgressInstance({ currentStageId: 'stage-soft', currentStepId: 'e0000029-0000-0000-0000-000000000029', currentExecutionId: 'exec-soft' }),
    });
    flowInstanceApiMock.getSnapshot.mockResolvedValue({ data: conditionalSnapshot });
    flowInstanceApiMock.listExecutions.mockResolvedValue({ data: [] });
    renderWizard();
    await waitFor(() => {
      expect(screen.getByText('Помʼякшуючий вкладиш не потрібен')).toBeInTheDocument();
    });
    // Skip CTA button must be removed — the option is now a checkbox element, not a separate button
    expect(screen.queryByRole('button', { name: /Помʼякшуючий вкладиш не потрібен/ })).not.toBeInTheDocument();
  });
});
