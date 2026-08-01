import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import IntensiveCareCard from '../../components/monitoring/IntensiveCareCard';
import type { Episode, ClinicalDay, HourlyRecord, MedicalOrder, FluidBalanceItem, OrderExecution } from '../../types';

const HOURS = Array.from({ length: 24 }, (_, i) => i);

const mockEpisode: Episode = {
  id: 'ep-1', patientId: 1001, patientName: 'Петренко Іван',
  hospitalizationId: null, departmentId: null,
  admissionDate: '2025-06-01T10:00:00Z', dischargeDate: null,
  status: 'ACTIVE', heightCm: 175, ward: 'Відділення 3', bedNumber: '12',
  admissionDiagnosis: 'Пневмонія', attendingDoctorId: null, createdBy: 1, createdAt: '',
  updatedBy: 0, updatedAt: '', version: 1,
};

const mockDay: ClinicalDay = {
  id: 'day-1', episodeId: 'ep-1', dayNumber: 1,
  startDateTime: '2025-06-01T08:00:00Z', endDateTime: '2025-06-02T08:00:00Z',
  status: 'OPEN', doctorSigned: false, nurseSigned: false, closedAt: null,
  weightKg: 72, bmi: null, createdBy: 1, createdAt: '',
  updatedBy: 0, updatedAt: '', version: 1,
};

const mockLockedDay: ClinicalDay = { ...mockDay, status: 'NURSE_SIGNED', nurseSigned: true };

const mockBalanceItems: FluidBalanceItem[] = [
  { id: 'b1', clinicalDayId: 'day-1', hour: 0, intake: 500, output: 300, balance: 200, cumulativeBalance: 200, version: 1 },
  { id: 'b2', clinicalDayId: 'day-1', hour: 1, intake: 250, output: 100, balance: 150, cumulativeBalance: 350, version: 1 },
];

const mockOrders: MedicalOrder[] = [
  { id: 'o1', clinicalDayId: 'day-1', category: 'MEDICATION', drugName: 'Амоксицилін', dose: '1', unit: 'г', route: 'в/в', frequency: 'кожні 8 год', startTime: '2025-06-01T08:00', endTime: null, status: 'ACTIVE', createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1 },
  { id: 'o2', clinicalDayId: 'day-1', category: 'MEDICATION', drugName: 'Гепарин', dose: '5000', unit: 'од', route: 'п/ш', frequency: '2 рази/день', startTime: '2025-06-01T08:00', endTime: null, status: 'ACTIVE', createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1 },
];

function plannedExecution(overrides: Partial<OrderExecution> = {}): OrderExecution {
  return {
    id: 'e1', orderId: 'o1', hour: 0, planned: true, plannedBy: 1, plannedAt: '2025-06-01T08:00:00',
    plannedDose: '1', plannedFinished: false, completedFinished: false,
    executedBy: null, executedAt: null, actualDose: null,
    status: 'PLANNED', comment: null, createdBy: 1, createdAt: '', updatedBy: 1, updatedAt: '', version: 1,
    ...overrides,
  };
}

const mockRecords: HourlyRecord[] = [
  {
    id: 'r1', clinicalDayId: 'day-1', recordTime: '2025-06-01T08:00:00',
    systolicBP: 120, diastolicBP: 80, heartRate: 72, spo2: 98,
    temperature: 36.6, cvp: 8, respiratoryRate: 16,
    consciousness: null, gcs: null, meanArterialPressure: null, etco2: null, fio2: null,
    dopamine: null, dobutamine: null, norepinephrine: null, epinephrine: null,
    urineOutput: null, drainOutput: null, gastricOutput: null, stool: null, vomit: null,
    bedPosition: null, headEnd: null,
    painScore: null, notes: null,
    createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1,
  },
  {
    id: 'r2', clinicalDayId: 'day-1', recordTime: '2025-06-01T10:00:00',
    systolicBP: 130, diastolicBP: 85, heartRate: 76, spo2: 97,
    temperature: 36.8, cvp: 7, respiratoryRate: 18,
    consciousness: 'Ясна', gcs: 14, meanArterialPressure: null, etco2: null, fio2: null,
    dopamine: null, dobutamine: null, norepinephrine: null, epinephrine: null,
    urineOutput: 150, drainOutput: 50, gastricOutput: null, stool: null, vomit: null,
    bedPosition: null, headEnd: null,
    painScore: null, notes: null,
    createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1,
  },
];

interface CardProps {
  episode?: Episode; selectedDay?: ClinicalDay | null; records?: HourlyRecord[];
  orders?: MedicalOrder[]; balanceItems?: FluidBalanceItem[]; isNurse?: boolean;
  isLocked?: boolean; user?: { id: number } | null; onRefresh?: () => void;
}

function renderCard(props: CardProps = {}) {
  return render(
    <ThemeModeProvider>
      <IntensiveCareCard
        episode={mockEpisode}
        selectedDay={mockDay}
        records={mockRecords}
        orders={mockOrders}
        balanceItems={mockBalanceItems}
        isNurse={false}
        isLocked={false}
        user={{ id: 1 }}
        {...props}
      />
    </ThemeModeProvider>
  );
}

let mockHourlyRecordCreate = vi.fn();
let mockHourlyRecordUpdate = vi.fn();
let mockNoteGetByClinicalDay = vi.fn();
let mockScaleGetResultsByClinicalDay = vi.fn();
let mockVentilationGetByClinicalDay = vi.fn();
let mockLabResultGetByClinicalDay = vi.fn();
let mockPatientStateGetByClinicalDay = vi.fn();
let mockOrderCreate = vi.fn();
let mockOrderExecutionGetByOrder = vi.fn();
let mockOrderExecutionPlan = vi.fn();
let mockOrderExecutionCancel = vi.fn();
let mockOrderExecutionExecute = vi.fn();
let mockOrderExecutionExecuteFinish = vi.fn();

vi.mock('../../api/endpoints', () => ({
  hourlyRecordApi: {
    getByClinicalDay: vi.fn(),
    create: (...args: unknown[]) => mockHourlyRecordCreate(...args),
    update: (...args: unknown[]) => mockHourlyRecordUpdate(...args),
  },
  medicalNoteApi: {
    getByClinicalDay: (...args: unknown[]) => mockNoteGetByClinicalDay(...args),
    create: vi.fn(),
  },
  clinicalScaleApi: {
    getResultsByClinicalDay: (...args: unknown[]) => mockScaleGetResultsByClinicalDay(...args),
    getResultsByEpisode: vi.fn().mockResolvedValue({ data: [] }),
    getAvailable: vi.fn().mockResolvedValue({ data: [] }),
    create: vi.fn().mockResolvedValue({ data: {} }),
    createEpisodeResult: vi.fn().mockResolvedValue({ data: {} }),
    calculateAndSave: vi.fn().mockResolvedValue({ data: {} }),
  },
  ventilationApi: {
    getByClinicalDay: (...args: unknown[]) => mockVentilationGetByClinicalDay(...args),
  },
  labResultApi: {
    getByClinicalDay: (...args: unknown[]) => mockLabResultGetByClinicalDay(...args),
    create: vi.fn(),
  },
  patientStateApi: {
    getByClinicalDay: (...args: unknown[]) => mockPatientStateGetByClinicalDay(...args),
    create: vi.fn(),
  },
  orderExecutionApi: {
    getByOrder: (...args: unknown[]) => mockOrderExecutionGetByOrder(...args),
    plan: (...args: unknown[]) => mockOrderExecutionPlan(...args),
    cancel: (...args: unknown[]) => mockOrderExecutionCancel(...args),
    execute: (...args: unknown[]) => mockOrderExecutionExecute(...args),
    executeFinish: (...args: unknown[]) => mockOrderExecutionExecuteFinish(...args),
  },
  medicalOrderApi: {
    create: (...args: unknown[]) => mockOrderCreate(...args),
  },
}));

describe('IntensiveCareCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(window, 'location', {
      value: { ...window.location, reload: vi.fn() },
      writable: true, configurable: true,
    });
    mockHourlyRecordCreate.mockResolvedValue({ data: {} });
    mockHourlyRecordUpdate.mockResolvedValue({ data: {} });
    mockNoteGetByClinicalDay.mockResolvedValue({ data: [] });
    mockScaleGetResultsByClinicalDay.mockResolvedValue({ data: [] });
    mockVentilationGetByClinicalDay.mockResolvedValue({ data: [] });
    mockLabResultGetByClinicalDay.mockResolvedValue({ data: [] });
    mockPatientStateGetByClinicalDay.mockResolvedValue({ data: [] });
    mockOrderCreate.mockResolvedValue({ data: {} });
    mockOrderExecutionGetByOrder.mockResolvedValue({ data: [] });
    mockOrderExecutionPlan.mockResolvedValue({ data: {} });
    mockOrderExecutionCancel.mockResolvedValue({ data: {} });
    mockOrderExecutionExecute.mockResolvedValue({ data: {} });
    mockOrderExecutionExecuteFinish.mockResolvedValue({ data: {} });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Rendering basics', () => {
    it('renders header, hour columns, group headers, sidebar, and all 12 row labels', () => {
      renderCard();
      expect(screen.getByText('Показник / година')).toBeInTheDocument();
      HOURS.forEach(h => expect(screen.getByText(`${h}:00`)).toBeInTheDocument());
      expect(screen.getByText('Показники')).toBeInTheDocument();
      expect(screen.getByText('Втрати (мл)')).toBeInTheDocument();
      expect(screen.getByText('Терапія (призначення)')).toBeInTheDocument();
    const rowLabels = ['АТсист', 'АТдіас', 'ЧСС', 'SpO₂', 'Темп', 'ЦВТ',
      'GCS', 'EtCO₂', 'FiO₂,%', 'Допамін (мкг/кг/хв)', 'Добутамін (мкг/кг/хв)',
      'Норадреналін (мкг/кг/хв)', 'Адреналін (мкг/кг/хв)',
      'Сеча', 'Дренаж', 'Випорожнення', 'Блювота'];
      rowLabels.forEach(label => expect(screen.getByText(label)).toBeInTheDocument());
      expect(screen.getAllByText('ЧД').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Пацієнт')).toBeInTheDocument();
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.getByText(/Пневмонія/)).toBeInTheDocument();
      expect(screen.getByText('Баланс рідини')).toBeInTheDocument();
    });

    it('shows weight and all sidebar sections', () => {
      renderCard({ selectedDay: { ...mockDay, weightKg: 78 } });
      expect(screen.getByText(/Вага: 78/)).toBeInTheDocument();
      ['Нотатки', 'Шкали', 'ШВЛ', 'Лабораторні результати', 'Стан пацієнта']
        .forEach(label => expect(screen.getByText(label)).toBeInTheDocument());
    });

    it('shows "тільки лікар" hint for nurse', () => {
      renderCard({ isNurse: true });
      expect(screen.getByText('(тільки лікар)')).toBeInTheDocument();
    });

    it('handles null selectedDay and null user', () => {
      renderCard({ selectedDay: null, user: null });
      expect(screen.getByText('Показник / година')).toBeInTheDocument();
    });
  });

  describe('Cell value display', () => {
    it('displays values from mockRecords via getByDisplayValue', () => {
      renderCard();
      expect(screen.getByDisplayValue('120')).toBeInTheDocument();
      expect(screen.getByDisplayValue('80')).toBeInTheDocument();
      expect(screen.getByDisplayValue('72')).toBeInTheDocument();
      expect(screen.getByDisplayValue('98')).toBeInTheDocument();
      expect(screen.getAllByDisplayValue('36.6').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByDisplayValue('8').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByDisplayValue('16').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByDisplayValue('130')).toBeInTheDocument();
      expect(screen.getByDisplayValue('76')).toBeInTheDocument();
      expect(screen.getByDisplayValue('14')).toBeInTheDocument();
      expect(screen.getByDisplayValue('150')).toBeInTheDocument();
      expect(screen.getByDisplayValue('50')).toBeInTheDocument();
    });
  });

  describe('Cell editing via API', () => {
    it('creates hourly record via api.create on blur', async () => {
      renderCard({ records: [], orders: [] });
      const input = screen.getByLabelText('ЧСС 10:00');
      await fireEvent.change(input, { target: { value: '88' } });
      await fireEvent.blur(input);
      await waitFor(() => expect(mockHourlyRecordCreate)
        .toHaveBeenCalledWith('day-1', expect.objectContaining({ heartRate: 88 })));
    });

    it('saves on Enter key', async () => {
      renderCard({ records: [], orders: [] });
      const input = screen.getByLabelText('АТсист 5:00');
      await fireEvent.change(input, { target: { value: '140' } });
      fireEvent.keyDown(input, { key: 'Enter' });
      fireEvent.blur(input);
      await waitFor(() => expect(mockHourlyRecordCreate)
        .toHaveBeenCalledWith('day-1', expect.objectContaining({ systolicBP: 140 })));
    });

    it('updates existing record via api.update', async () => {
      renderCard();
      const input = screen.getByLabelText('ЧСС 8:00');
      await fireEvent.change(input, { target: { value: '90' } });
      await fireEvent.blur(input);
      await waitFor(() => expect(mockHourlyRecordUpdate)
        .toHaveBeenCalledWith('r1', expect.objectContaining({ heartRate: 90, version: 1 })));
    });

    it('does not save empty value', async () => {
      renderCard({ records: [], orders: [] });
      const input = screen.getByLabelText('ЧСС 3:00');
      await fireEvent.change(input, { target: { value: '' } });
      await fireEvent.blur(input);
      await waitFor(() => expect(mockHourlyRecordCreate).not.toHaveBeenCalled());
    });
  });

  describe('Permissions and locked state', () => {
    it('nurse: vital signs disabled, loss rows enabled', () => {
      renderCard({ isNurse: true });
      expect(screen.getByDisplayValue('72')).toBeDisabled();
      expect(screen.getByDisplayValue('120')).toBeDisabled();
      expect(screen.getByDisplayValue('98')).toBeDisabled();
      expect(screen.getByDisplayValue('150')).not.toBeDisabled();
      expect(screen.getByDisplayValue('50')).not.toBeDisabled();
    });

    it('doctor can edit all rows', () => {
      renderCard({ isNurse: false });
      expect(screen.getByDisplayValue('72')).not.toBeDisabled();
      expect(screen.getByDisplayValue('150')).not.toBeDisabled();
    });

    it('doctor sees new order button', () => {
      renderCard({ isNurse: false });
      expect(screen.getByText('+ Нове призначення')).toBeInTheDocument();
    });

    it('nurse does not see new order button', () => {
      renderCard({ isNurse: true });
      expect(screen.queryByText('+ Нове призначення')).not.toBeInTheDocument();
    });

    it('disables all cells when locked', () => {
      renderCard({ selectedDay: mockLockedDay, isLocked: true });
      expect(screen.getByDisplayValue('72')).toBeDisabled();
      expect(screen.getByDisplayValue('150')).toBeDisabled();
    });
  });

  describe('Therapy and water balance', () => {
    it('shows active orders', () => {
      renderCard();
      expect(screen.getByText('Амоксицилін 1г')).toBeInTheDocument();
      expect(screen.getByText('Гепарин 5000од')).toBeInTheDocument();
    });

    it('shows empty state when no orders', () => {
      renderCard({ orders: [] });
      expect(screen.getByText('Немає призначень')).toBeInTheDocument();
    });

    it('creates order from inline form', { timeout: 120000 }, async () => {
      renderCard({ isNurse: false });
      fireEvent.click(screen.getByText('+ Нове призначення'));
      await waitFor(() => expect(screen.getByText('Нове призначення')).toBeInTheDocument(), { timeout: 10000 });
      const inputs = screen.getAllByPlaceholderText('Препарат');
      fireEvent.change(inputs[inputs.length - 1], { target: { value: 'Парацетамол' } });
      const doseInputs = screen.getAllByPlaceholderText('Доза');
      fireEvent.change(doseInputs[doseInputs.length - 1], { target: { value: '500' } });
      fireEvent.click(screen.getByText('Створити'));
      await waitFor(() => expect(mockOrderCreate).toHaveBeenCalled(), { timeout: 10000 });
    });

    it('shows water balance totals', () => {
      renderCard();
      expect(screen.getByText(/750/)).toBeInTheDocument();
      expect(screen.getByText(/400/)).toBeInTheDocument();
      expect(screen.getAllByText('+350').length).toBeGreaterThanOrEqual(1);
    });
  });

  describe('Edge cases', () => {
    it('renders with empty records and no orders', () => {
      renderCard({ records: [], orders: [] });
      expect(screen.getByText('Немає призначень')).toBeInTheDocument();
    });

    it('handles missing weight', () => {
      renderCard({ selectedDay: { ...mockDay, weightKg: null } });
      expect(screen.queryByText(/Вага:/)).not.toBeInTheDocument();
    });

    it('all 24 hour columns render', () => {
      renderCard({ records: [] });
      HOURS.forEach(h => expect(screen.getByText(`${h}:00`)).toBeInTheDocument());
    });
  });

  describe('Sidebar resize', () => {
    it('renders sidebar with patient info and all section labels', () => {
      renderCard();
      expect(screen.getByText('Пацієнт')).toBeInTheDocument();
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.getByText('Баланс рідини')).toBeInTheDocument();
      expect(screen.getByText('Нотатки')).toBeInTheDocument();
      expect(screen.getByText('Шкали')).toBeInTheDocument();
      expect(screen.getByText('ШВЛ')).toBeInTheDocument();
      expect(screen.getByText('Лабораторні результати')).toBeInTheDocument();
      expect(screen.getByText('Стан пацієнта')).toBeInTheDocument();
    });

    it('renders resize rail with aria-label', () => {
      renderCard();
      const rail = screen.getByRole('separator', { name: 'Зміна ширини бічної панелі' });
      expect(rail).toBeInTheDocument();
    });

    it('mousedown on rail sets body cursor and mouseup cleans up', () => {
      renderCard();
      const rail = screen.getByRole('separator', { name: 'Зміна ширини бічної панелі' });
      fireEvent.mouseDown(rail, { clientX: 500 });
      expect(document.body.style.cursor).toBe('col-resize');
      fireEvent.mouseUp(window);
      expect(document.body.style.cursor).toBe('');
    });
  });

  describe('Order execution (plan/execute)', () => {
    const HOURS_ORDER = Array.from({ length: 24 }, (_, i) => (i + 8) % 24);

    function cellAtHour(orderName: string, hour: number): HTMLElement {
      const row = screen.getByText(orderName).closest('tr');
      expect(row).not.toBeNull();
      const tds = (row as HTMLElement).querySelectorAll('td');
      return tds[HOURS_ORDER.indexOf(hour) + 1] as HTMLElement;
    }

    it('doctor plans a dose for a future hour', async () => {
      vi.spyOn(Date.prototype, 'getHours').mockReturnValue(0);
      renderCard();
      fireEvent.click(cellAtHour('Амоксицилін 1г', 0));
      const input = screen.getByLabelText('Запланувати Амоксицилін 0:00');
      await fireEvent.change(input, { target: { value: '1' } });
      await fireEvent.blur(input);
      await waitFor(() => expect(mockOrderExecutionPlan).toHaveBeenCalledWith('o1', { hour: 0, dose: '1' }));
    });

    it('doctor cancels a planned execution', async () => {
      vi.spyOn(Date.prototype, 'getHours').mockReturnValue(0);
      mockOrderExecutionGetByOrder.mockResolvedValue({ data: [plannedExecution()] });
      renderCard();
      fireEvent.click(cellAtHour('Амоксицилін 1г', 0));
      const cancelButton = await screen.findByLabelText('Скасувати Амоксицилін 0:00');
      fireEvent.click(cancelButton);
      await waitFor(() => expect(mockOrderExecutionCancel).toHaveBeenCalledWith('o1', { hour: 0 }));
    });

    it('nurse executes a planned dose with actual value', async () => {
      vi.spyOn(Date.prototype, 'getHours').mockReturnValue(0);
      mockOrderExecutionGetByOrder.mockResolvedValue({ data: [plannedExecution()] });
      renderCard({ isNurse: true });
      const cell = cellAtHour('Амоксицилін 1г', 0);
      await waitFor(() => expect(cell.textContent).toContain('1'));
      fireEvent.click(cell);
      const input = screen.getByLabelText('Виконати Амоксицилін 0:00');
      await fireEvent.change(input, { target: { value: '0.8' } });
      await fireEvent.blur(input);
      await waitFor(() => expect(mockOrderExecutionExecute).toHaveBeenCalledWith('o1', { hour: 0, actualDose: '0.8' }));
    });

    it('nurse finishes a completed execution', async () => {
      vi.spyOn(Date.prototype, 'getHours').mockReturnValue(0);
      mockOrderExecutionGetByOrder.mockResolvedValue({
        data: [plannedExecution({
          status: 'COMPLETED', planned: false, plannedDose: null,
          executedBy: 1, executedAt: '2025-06-01T08:30:00', actualDose: '1',
        })],
      });
      renderCard({ isNurse: true });
      const cell = cellAtHour('Амоксицилін 1г', 0);
      await waitFor(() => expect(cell.textContent).toContain('✓'));
      fireEvent.click(cell);
      fireEvent.click(screen.getByRole('button', { name: 'Завершити' }));
      await waitFor(() => expect(mockOrderExecutionExecuteFinish).toHaveBeenCalledWith('o1', { hour: 0 }));
    });
  });
});
