import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProductionWorkItemDrawer from '@/components/prosthetics/ProductionWorkItemDrawer';
import type { ProductionDetail } from '@/prosthetics/types';

const detailApiMock = vi.hoisted(() => ({
  detail: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  productionApi: detailApiMock,
}));

let mockPermissions: string[] = [
  'PROSTHETICS_PRODUCTION_VIEW',
  'PROSTHETICS_PRODUCTION_PATIENT_VIEW',
  'PROSTHETICS_PRODUCTION_QUALITY_VIEW',
];

vi.mock('@/services/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 5, login: 'prosthetist1', fullName: 'Іваненко Іван', role: 'PROSTHETIST' },
    hasPermission: (permission: string) => mockPermissions.includes(permission),
  }),
}));

const detail: ProductionDetail = {
  workItem: {
    instanceId: 'i1',
    orderId: 'o1',
    patientId: '900001',
    patientPib: 'Бондаренко Тарас',
    prosthetistUserId: 5,
    prosthetistFullName: 'Іваненко Іван',
    orderNumber: 'MIS-900001-55',
    productCode: '06 24 09',
    productType: 'LOWER_LIMB',
    prosthesisType: 'Модульний',
    prescriptionDate: '2026-09-01',
    templateName: 'TP-UL-01',
    currentStageName: 'Формування',
    currentStepName: 'Моделювання',
    status: 'IN_PROGRESS',
    startTime: '2026-09-05T08:00:00',
    endTime: null,
    lastActivityAt: null,
    createdAt: '2026-09-05T08:00:00',
    updatedAt: null,
    elapsedSeconds: 345600,
    activeSeconds: 7200,
    idleSeconds: 600,
    expectedActiveSeconds: 3600,
    activeDeviationSeconds: 3600,
    brakCount: 1,
    reworkCount: 0,
    failed: false,
    attentionFlags: [],
  },
  timeline: [
    {
      id: 'e1',
      instanceId: 'i1',
      stageId: 's1',
      stepId: 'step-1',
      attemptNumber: 1,
      status: 'COMPLETED',
      startedAt: '2026-09-05T08:00:00',
      completedAt: '2026-09-05T10:00:00',
      activeSeconds: 7200,
      values: null,
      note: 'Мірки знято',
      completedBy: 5,
    },
  ],
  brakEvents: [
    {
      id: 'b1',
      instanceId: 'i1',
      stageId: 's1',
      stepId: 'step-1',
      softTissueMisalignment: true,
      painDiscomfort: false,
      note: 'Тріщина гільзи',
      returnStageId: 's1',
      returnStageName: 'Формування',
      newInstanceId: null,
      createdBy: 5,
      createdAt: '2026-09-06T08:00:00',
    },
  ],
  branches: [],
  order: {
    id: 'o1',
    patientId: '900001',
    orderNumber: 'MIS-900001-55',
    productType: 'LOWER_LIMB',
    amputationLevel: '',
    limbSide: '',
    status: 'NEW',
    materials: null,
    productCode: '06 24 09',
    doctorName: null,
    prescriptionDate: '2026-09-01',
    createdAt: '2026-09-01T00:00:00',
  },
  patient: {
    id: '900001',
    pib: 'Бондаренко Тарас',
    birthDate: '1980-01-01',
    gender: 'Чоловіча',
    heightCm: 182,
    weightKg: 84,
    socialStatus: null,
    cause: null,
    amputationDate: null,
    affectedLimb: null,
    amputationLevel: null,
    amputationSite: null,
    phone: null,
    email: null,
    residence: 'м. Львів',
    healthStatus: null,
    clinicalState: null,
    stump: null,
    departmentId: null,
  },
  patientDetailsVisible: true,
  documents: [
    {
      documentId: 55,
      documentTemplateName: 'Замовлення',
      documentUrl: 'https://mis.local/55',
      productName: 'Гомілка модульна',
      mobilityLevel: 'K3',
      note: 'Терміново',
    },
  ],
  matchedDocument: {
    documentId: 55,
    documentTemplateName: 'Замовлення',
    documentUrl: 'https://mis.local/55',
    productName: 'Гомілка модульна',
    mobilityLevel: 'K3',
    note: 'Терміново',
  },
  documentsUnknown: false,
};

function renderDrawer() {
  return render(
    <MemoryRouter>
      <ProductionWorkItemDrawer instanceId="i1" onClose={vi.fn()} onOpenProcess={vi.fn()} />
    </MemoryRouter>,
  );
}

describe('ProductionWorkItemDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPermissions = [
      'PROSTHETICS_PRODUCTION_VIEW',
      'PROSTHETICS_PRODUCTION_PATIENT_VIEW',
      'PROSTHETICS_PRODUCTION_QUALITY_VIEW',
    ];
    detailApiMock.detail.mockResolvedValue({ data: detail });
  });

  it('renders header, patient, order, timeline, time and quality', async () => {
    renderDrawer();
    await waitFor(() => {
      expect(screen.getByText('Гомілка модульна')).toBeInTheDocument();
    });
    expect(screen.getByText('Бондаренко Тарас')).toBeInTheDocument();
    expect(screen.getByText('м. Львів')).toBeInTheDocument();
    expect(screen.getByText('MIS-900001-55')).toBeInTheDocument();
    expect(screen.getByText('Мірки знято')).toBeInTheDocument();
    expect(screen.getByText('Тріщина гільзи')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Відкрити документ' })).toBeInTheDocument();
    expect(screen.getByText('Якість')).toBeInTheDocument();
  });

  it('masks personal data without PATIENT_VIEW', async () => {
    mockPermissions = ['PROSTHETICS_PRODUCTION_VIEW', 'PROSTHETICS_PRODUCTION_QUALITY_VIEW'];
    detailApiMock.detail.mockResolvedValue({
      data: {
        ...detail,
        patientDetailsVisible: false,
        patient: { ...detail.patient, birthDate: null, residence: null },
        documents: [],
        matchedDocument: null,
      },
    });
    renderDrawer();
    await waitFor(() => {
      expect(screen.getByText('Бондаренко Тарас')).toBeInTheDocument();
    });
    expect(screen.getByText(/Деталі приховано/)).toBeInTheDocument();
    expect(screen.queryByText('м. Львів')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Відкрити документ' })).not.toBeInTheDocument();
  });

  it('hides the quality block without QUALITY_VIEW', async () => {
    mockPermissions = ['PROSTHETICS_PRODUCTION_VIEW', 'PROSTHETICS_PRODUCTION_PATIENT_VIEW'];
    renderDrawer();
    await waitFor(() => {
      expect(screen.getByText('Хронологія кроків')).toBeInTheDocument();
    });
    expect(screen.queryByText('Якість')).not.toBeInTheDocument();
    expect(screen.queryByText('Тріщина гільзи')).not.toBeInTheDocument();
  });

  it('shows an error with retry', async () => {
    detailApiMock.detail.mockRejectedValueOnce(new Error('boom'));
    renderDrawer();
    await waitFor(() => {
      expect(screen.getByText('Помилка')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Спробувати знову' }));
    await waitFor(() => {
      expect(screen.getByText('Гомілка модульна')).toBeInTheDocument();
    });
  });

  it('opens the process on action', async () => {
    const onOpenProcess = vi.fn();
    render(
      <MemoryRouter>
        <ProductionWorkItemDrawer instanceId="i1" onClose={vi.fn()} onOpenProcess={onOpenProcess} />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('Гомілка модульна')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Відкрити процес' }));
    expect(onOpenProcess).toHaveBeenCalledWith('i1', 'IN_PROGRESS');
  });
});
