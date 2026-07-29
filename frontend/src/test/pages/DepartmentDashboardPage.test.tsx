import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import { MemoryRouter } from 'react-router-dom';
import DepartmentDashboardPage from '../../pages/doctor/DepartmentDashboardPage';
import type { DepartmentPatient } from '../../types';

const mockNavigate = vi.fn();
const mockGetPatients = vi.fn();
const mockGetStats = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../api/endpoints', () => ({
  departmentApi: {
    getPatients: (...args: unknown[]) => mockGetPatients(...args),
    getStats: (...args: unknown[]) => mockGetStats(...args),
  },
  episodeApi: { search: vi.fn() },
}));

const mockPatients: DepartmentPatient[] = [
  {
    id: 'ep-1', patientId: 1001, patientName: 'Петренко Іван',
    hospitalizationId: null, departmentId: null,
    admissionDate: '2025-06-01T10:00:00Z', dischargeDate: null,
    status: 'ACTIVE', attendingDoctorId: 1,
    attendingDoctorName: 'Доктор Іван', ward: 'Відділення 3',
    bedNumber: '12', admissionDiagnosis: 'Пневмонія',
    latestDayStatus: 'OPEN', latestDayNumber: 1, daysSinceAdmission: 3,
  },
  {
    id: 'ep-2', patientId: 1002, patientName: 'Коваленко Олена',
    hospitalizationId: null, departmentId: null,
    admissionDate: '2025-06-02T10:00:00Z', dischargeDate: null,
    status: 'ACTIVE', attendingDoctorId: 2,
    attendingDoctorName: 'Доктор Петро', ward: 'Відділення 3',
    bedNumber: '8', admissionDiagnosis: 'Сепсис',
    latestDayStatus: 'NURSE_SIGNED', latestDayNumber: 2, daysSinceAdmission: 2,
  },
];

const mockStats = {
  activePatients: 2,
  openDays: 1,
  nurseSignedDays: 1,
  doctorSignedDays: 0,
  closedDays: 0,
  totalBeds: 12,
  occupiedBeds: 2,
  activeDoctors: 3,
  activeNurses: 5,
};

function renderPage() {
  return render(
    <ThemeModeProvider>
      <MemoryRouter>
        <DepartmentDashboardPage />
      </MemoryRouter>
    </ThemeModeProvider>
  );
}

describe('DepartmentDashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetPatients.mockResolvedValue({ data: mockPatients });
    mockGetStats.mockResolvedValue({ data: mockStats });
  });

  it('sets document title on mount', () => {
    renderPage();
    expect(document.title).toBe('ВАІТ — Завідувач відділення');
  });

  it('shows loading spinner initially', () => {
    mockGetPatients.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('renders department title', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Відділення анестезіології та інтенсивної терапії')).toBeInTheDocument();
    });
  });

  it('renders stat cards after loading', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument(); // activePatients
      const ones = screen.getAllByText('1');
      expect(ones.length).toBeGreaterThanOrEqual(2); // openDays + nurseSignedDays
      expect(screen.getByText('2 / 12')).toBeInTheDocument(); // occupied / total beds
      expect(screen.getByText('3')).toBeInTheDocument(); // activeDoctors
      expect(screen.getByText('5')).toBeInTheDocument(); // activeNurses
    });
  });

  it('renders patient cards by default', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.getByText('Коваленко Олена')).toBeInTheDocument();
    });
  });

  it('filters patients by search', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Петренко Іван')).toBeInTheDocument());
    const search = screen.getByPlaceholderText('Пошук пацієнта за ПІБ...');
    await userEvent.type(search, 'Петренко');
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.queryByText('Коваленко Олена')).not.toBeInTheDocument();
    });
  });

  it('shows no results message when search has no matches', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Петренко Іван')).toBeInTheDocument());
    const search = screen.getByPlaceholderText('Пошук пацієнта за ПІБ...');
    await userEvent.type(search, 'NonExistent');
    await waitFor(() => {
      expect(screen.getByText('Немає пацієнтів за запитом')).toBeInTheDocument();
    });
  });

  it('shows no patients alert when list is empty', async () => {
    mockGetPatients.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Немає активних пацієнтів')).toBeInTheDocument();
    });
  });

  it('switches to table view', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Петренко Іван')).toBeInTheDocument());
    await userEvent.click(screen.getByText('Таблиця'));
    await waitFor(() => {
      // Table view shows episodes
      expect(screen.getByText('Картки')).toBeInTheDocument();
    });
  });

  it('switches back to cards view from table view', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Петренко Іван')).toBeInTheDocument());
    await userEvent.click(screen.getByText('Таблиця'));
    await waitFor(() => expect(screen.getByText('Картки')).toBeInTheDocument());
    await userEvent.click(screen.getByText('Картки'));
  });

  it('handles API error gracefully', async () => {
    mockGetPatients.mockRejectedValue(new Error('API error'));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Активні пацієнти')).toBeInTheDocument();
    });
  });

  it('clearing search restores all patients', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Петренко Іван')).toBeInTheDocument());
    const search = screen.getByPlaceholderText('Пошук пацієнта за ПІБ...');
    await userEvent.type(search, 'NonExistent');
    await waitFor(() => expect(screen.getByText('Немає пацієнтів за запитом')).toBeInTheDocument());
    await userEvent.clear(search);
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.getByText('Коваленко Олена')).toBeInTheDocument();
    });
  });
});
