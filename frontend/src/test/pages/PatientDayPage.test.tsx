import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
const theme = createTheme({});
import PatientDayPage from '../../pages/doctor/PatientDayPage';
import type { Episode, ClinicalDay, ClinicalScale } from '../../types';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ episodeId: 'ep-1' }),
    useNavigate: () => mockNavigate,
  };
});

const mockGetById = vi.fn();
const mockGetClinicalDays = vi.fn();
const mockSignDoctor = vi.fn();
const mockGetRecords = vi.fn();
const mockGetOrders = vi.fn();
const mockGetNotes = vi.fn();
const mockGetScaleResults = vi.fn();
const mockGetAvailableScales = vi.fn();
const mockGetBalanceItems = vi.fn();

vi.mock('../../api/endpoints', () => ({
  episodeApi: {
    getById: (...args: unknown[]) => mockGetById(...args),
    getClinicalDays: (...args: unknown[]) => mockGetClinicalDays(...args),
  },
  clinicalDayApi: {
    signNurse: vi.fn(),
    signDoctor: (...args: unknown[]) => mockSignDoctor(...args),
  },
  hourlyRecordApi: {
    getByClinicalDay: (...args: unknown[]) => mockGetRecords(...args),
    create: vi.fn(),
  },
  medicalOrderApi: {
    getByClinicalDay: (...args: unknown[]) => mockGetOrders(...args),
    create: vi.fn(),
    cancel: vi.fn(),
  },
  medicalNoteApi: {
    getByClinicalDay: (...args: unknown[]) => mockGetNotes(...args),
    create: vi.fn(),
  },
  clinicalScaleApi: {
    getAvailable: (...args: unknown[]) => mockGetAvailableScales(...args),
    getResultsByClinicalDay: (...args: unknown[]) => mockGetScaleResults(...args),
    createResult: vi.fn(),
  },
  ventilationApi: {
    getByClinicalDay: vi.fn().mockResolvedValue({ data: [] }),
    create: vi.fn(),
  },
  labResultApi: {
    getByClinicalDay: vi.fn().mockResolvedValue({ data: [] }),
    create: vi.fn(),
  },
  patientStateApi: {
    getByClinicalDay: vi.fn().mockResolvedValue({ data: [] }),
    create: vi.fn(),
  },
  fluidBalanceApi: {
    getByClinicalDay: (...args: unknown[]) => mockGetBalanceItems(...args),
    recalculate: vi.fn(),
  },
}));

const TEST_USER = { id: 1, login: 'doctor1', fullName: 'Доктор', role: 'DOCTOR', email: '' } as const;

vi.mock('../../services/AuthContext', () => ({
  useAuth: () => ({
    user: TEST_USER,
    token: 'mock-token',
    isAuthenticated: true,
    hasRole: (...roles: string[]) => roles.includes('DOCTOR'),
  }),
}));

const mockEpisode: Episode = {
  id: 'ep-1',
  patientId: 1001,
  patientName: 'Петренко Іван',
  hospitalizationId: null,
  departmentId: null,
  admissionDate: '2025-06-01T10:00:00Z',
  dischargeDate: null,
  status: 'ACTIVE',
  heightCm: null,
  ward: null,
  bedNumber: null,
  admissionDiagnosis: null,
  attendingDoctorId: null,
  createdBy: 1,
  createdAt: '2025-06-01T10:00:00Z',
  updatedBy: 1,
  updatedAt: '2025-06-01T10:00:00Z',
  version: 1,
};

const mockDays: ClinicalDay[] = [
  {
    id: 'day-1',
    episodeId: 'ep-1',
    dayNumber: 1,
    startDateTime: '2025-06-01T08:00:00Z',
    endDateTime: '2025-06-02T08:00:00Z',
    status: 'NURSE_SIGNED',
    doctorSigned: false,
    nurseSigned: true,
    closedAt: null,
    weightKg: null,
    bmi: null,
    createdBy: 1,
    createdAt: '2025-06-01T08:00:00Z',
    updatedBy: 1,
    updatedAt: '2025-06-01T08:00:00Z',
    version: 1,
  },
  {
    id: 'day-2',
    episodeId: 'ep-1',
    dayNumber: 2,
    startDateTime: '2025-06-02T08:00:00Z',
    endDateTime: '2025-06-03T08:00:00Z',
    status: 'OPEN',
    doctorSigned: false,
    nurseSigned: false,
    closedAt: null,
    weightKg: null,
    bmi: null,
    createdBy: 1,
    createdAt: '2025-06-02T08:00:00Z',
    updatedBy: 1,
    updatedAt: '2025-06-02T08:00:00Z',
    version: 1,
  },
];

const mockScales: ClinicalScale[] = [
  { id: 'scale-1', name: 'APACHE II', description: null, isAutomatic: false, status: 'ACTIVE', createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1 },
];

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <PatientDayPage />
    </ThemeProvider>
  );
}

describe('PatientDayPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetById.mockResolvedValue({ data: mockEpisode });
    mockGetClinicalDays.mockResolvedValue({ data: mockDays });
    mockGetRecords.mockResolvedValue({ data: [] });
    mockGetOrders.mockResolvedValue({ data: [] });
    mockGetNotes.mockResolvedValue({ data: [] });
    mockGetScaleResults.mockResolvedValue({ data: [] });
    mockGetAvailableScales.mockResolvedValue({ data: mockScales });
    mockGetBalanceItems.mockResolvedValue({ data: [] });
    mockSignDoctor.mockResolvedValue({ data: {} });
  });

  it('renders loading spinner initially', () => {
    mockGetById.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('renders patient name and episode info after load', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getAllByText('Петренко Іван').length).toBeGreaterThan(0);
    });
    await waitFor(() => {
      expect(screen.getAllByText(/День 1/).length).toBeGreaterThan(0);
    });
  });

  it('renders clinical day timeline', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getAllByText(/Доба 1/).length).toBeGreaterThan(0);
      expect(screen.getAllByText(/Доба 2/).length).toBeGreaterThan(0);
    });
  });

  it('renders all sections on single screen', async () => {
    renderPage();
    await waitFor(() => {
      // Single ICU card: vitals/losses grid header present
      expect(screen.getByText(/Показник \/ година/i)).toBeInTheDocument();
      expect(screen.getAllByText(/Втрати/i).length).toBeGreaterThan(0);
      expect(screen.getAllByText(/Призначення/i).length).toBeGreaterThan(0);
      expect(screen.getAllByText(/Баланс/i).length).toBeGreaterThan(0);
    });
  });

  it('shows all section content simultaneously', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Показник \/ година/i)).toBeInTheDocument();
    });
    await waitFor(() => {
      const headings = screen.getAllByText(/Втрати/i);
      expect(headings.length).toBeGreaterThan(0);
    });
    await waitFor(() => {
      // Verify all sections rendered without tabs (no role="tab" elements)
      const tabs = screen.queryAllByRole('tab');
      expect(tabs.length).toBe(0);
    });
  });

  it('shows sign inline confirm when "Підписати добу" is clicked', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Підписати добу')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('Підписати добу'));
    await waitFor(() => {
      expect(screen.getByText('Підписати')).toBeInTheDocument();
    });
  });

  it('calls signDoctor on sign inline confirm', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Підписати добу')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('Підписати добу'));
    await waitFor(() => {
      expect(screen.getByText('Підписати')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('Підписати'));
    await waitFor(() => {
      expect(mockSignDoctor).toHaveBeenCalledWith('day-1', { userId: 1 });
    });
  });

  it('navigates back on back button click', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('← Назад до пацієнтів')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('← Назад до пацієнтів'));
    expect(mockNavigate).toHaveBeenCalledWith('/doctor');
  });

  it('shows error state when API fails', async () => {
    mockGetById.mockResolvedValue({ data: null });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Епізод не знайдено')).toBeInTheDocument();
    });
  });
});
