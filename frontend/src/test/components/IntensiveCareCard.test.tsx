import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import IntensiveCareCard from '../../components/monitoring/IntensiveCareCard';
import type { Episode, ClinicalDay, HourlyRecord, MedicalOrder, FluidBalanceItem } from '../../types';

const theme = createTheme({});
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

const mockRecords: HourlyRecord[] = [
  {
    id: 'r1', clinicalDayId: 'day-1', recordTime: '2025-06-01T08:00:00',
    systolicBP: 120, diastolicBP: 80, heartRate: 72, spo2: 98,
    temperature: 36.6, cvp: 8, respiratoryRate: 16,
    consciousness: null, meanArterialPressure: null, etco2: null, fio2: null,
    urineOutput: null, drainOutput: null, stool: null, vomit: null,
    painScore: null, notes: null,
    createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1,
  },
  {
    id: 'r2', clinicalDayId: 'day-1', recordTime: '2025-06-01T10:00:00',
    systolicBP: 130, diastolicBP: 85, heartRate: 76, spo2: 97,
    temperature: 36.8, cvp: 7, respiratoryRate: 18,
    consciousness: 'Ясна', meanArterialPressure: null, etco2: null, fio2: null,
    urineOutput: 150, drainOutput: 50, stool: null, vomit: null,
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
    <ThemeProvider theme={theme}>
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
    </ThemeProvider>
  );
}

/** Find an <input> element by matching its aria-label attribute value.
 *  MUI TextField may put aria-label on the FormControl or on the input itself. */
function findInputByAriaLabel(label: string): HTMLInputElement {
  const el = screen.getByLabelText(label);
  // If el is already the input, return it; otherwise look inside
  const input = el.tagName === 'INPUT' ? el : el.querySelector('input');
  if (!input) throw new Error(`No input found for aria-label "${label}"`);
  return input as HTMLInputElement;
}

let mockHourlyRecordCreate = vi.fn();
let mockHourlyRecordUpdate = vi.fn();
let mockNoteGetByClinicalDay = vi.fn();
let mockScaleGetResultsByClinicalDay = vi.fn();
let mockVentilationGetByClinicalDay = vi.fn();
let mockLabResultGetByClinicalDay = vi.fn();
let mockPatientStateGetByClinicalDay = vi.fn();
let mockOrderCreate = vi.fn();

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
    create: vi.fn(),
  },
  medicalOrderApi: {
    create: (...args: unknown[]) => mockOrderCreate(...args),
  },
}));

describe('IntensiveCareCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Prevent window.location.reload() from hanging jsdom
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
  });

  describe('Rendering basics', () => {
    it('renders header, hour columns, group headers, sidebar, and all 12 row labels', () => {
      renderCard();
      expect(screen.getByText('Показник / година')).toBeInTheDocument();
      HOURS.forEach(h => expect(screen.getByText(`${h}:00`)).toBeInTheDocument());
      expect(screen.getByText('Показники')).toBeInTheDocument();
      expect(screen.getByText('Втрати (мл)')).toBeInTheDocument();
      expect(screen.getByText('Терапія (призначення)')).toBeInTheDocument();
      // 'ЧД' may appear in multiple contexts — use getAllByText
      const rowLabels = ['АТсист', 'АТдіас', 'ЧСС', 'SpO2', 'Темп', 'ЦВТ',
        'Сеча', 'Зонд', 'Випорожнення', 'Дренаж'];
      rowLabels.forEach(label => expect(screen.getByText(label)).toBeInTheDocument());
      expect(screen.getAllByText('Свідомість').length).toBeGreaterThanOrEqual(1);
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
      // Hour 8 values
      expect(screen.getByDisplayValue('120')).toBeInTheDocument();
      expect(screen.getByDisplayValue('80')).toBeInTheDocument();
      expect(screen.getByDisplayValue('72')).toBeInTheDocument();
      expect(screen.getByDisplayValue('98')).toBeInTheDocument();
      expect(screen.getByDisplayValue('36.6')).toBeInTheDocument();
      expect(screen.getAllByDisplayValue('8').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByDisplayValue('16')).toBeInTheDocument();
      // Hour 10 values
      expect(screen.getByDisplayValue('130')).toBeInTheDocument();
      expect(screen.getByDisplayValue('76')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Ясна')).toBeInTheDocument();
      expect(screen.getByDisplayValue('150')).toBeInTheDocument();
      expect(screen.getByDisplayValue('50')).toBeInTheDocument();
    });
  });

  describe('Cell editing via API', () => {
    it('creates hourly record via api.create on blur', async () => {
      renderCard({ records: [], orders: [] });
      const input = findInputByAriaLabel('ЧСС 10:00');
      await fireEvent.change(input, { target: { value: '88' } });
      await fireEvent.blur(input);
      await waitFor(() => expect(mockHourlyRecordCreate)
        .toHaveBeenCalledWith('day-1', expect.objectContaining({ heartRate: 88 })));
    });

    it('saves on Enter key', async () => {
      renderCard({ records: [], orders: [] });
      const input = findInputByAriaLabel('АТсист 5:00');
      await fireEvent.change(input, { target: { value: '140' } });
      fireEvent.keyDown(input, { key: 'Enter' });
      fireEvent.blur(input);
      await waitFor(() => expect(mockHourlyRecordCreate)
        .toHaveBeenCalledWith('day-1', expect.objectContaining({ systolicBP: 140 })));
    });

    it('updates existing record via api.update', async () => {
      renderCard();
      const input = findInputByAriaLabel('ЧСС 8:00');
      await fireEvent.change(input, { target: { value: '90' } });
      await fireEvent.blur(input);
      await waitFor(() => expect(mockHourlyRecordUpdate)
        .toHaveBeenCalledWith('r1', expect.objectContaining({ heartRate: 90, version: 1 })));
    });

    it('does not save empty value', async () => {
      renderCard({ records: [], orders: [] });
      const input = findInputByAriaLabel('ЧСС 3:00');
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
      const inputs = screen.getAllByLabelText('Препарат');
      fireEvent.change(inputs[inputs.length - 1], { target: { value: 'Парацетамол' } });
      const doseInputs = screen.getAllByLabelText('Доза');
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
});
