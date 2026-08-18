import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import NurseDashboard from '../../components/monitoring/NurseDashboard';
import type { DashboardProps } from '../../components/monitoring/dashboardTypes';
import type { Episode, ClinicalDay } from '../../types/icu';


const baseEpisode: Episode = {
  id: 'ep-2', patientId: 1002, patientName: 'Коваленко Олена',
  hospitalizationId: null, departmentId: null,
  admissionDate: '2025-06-01T10:00:00Z', dischargeDate: null,
  status: 'ACTIVE', heightCm: 165, ward: 'Відділення 3', bedNumber: '8',
  admissionDiagnosis: 'Сепсис', attendingDoctorId: null,
  createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1,
};

const baseDay: ClinicalDay = {
  id: 'day-2', episodeId: 'ep-2', dayNumber: 2,
  startDateTime: '2025-06-02T08:00:00Z', endDateTime: '2025-06-03T08:00:00Z',
  status: 'OPEN', doctorSigned: false, nurseSigned: false, closedAt: null,
  weightKg: 65, bmi: null,
  createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1,
};

const defaultProps: DashboardProps = {
  episode: baseEpisode,
  clinicalDays: [baseDay],
  selectedDay: baseDay,
  onSelectDay: vi.fn(),
  records: [],
  orders: [],
  balanceItems: [],
  isLocked: false,
  isNurse: true,
  user: { id: 2 },
  onRefresh: vi.fn(),
};

function renderWithTheme(ui: React.ReactElement) {
  return render(<ThemeModeProvider>{ui}</ThemeModeProvider>);
}

describe('NurseDashboard', () => {
  it('renders patient name', () => {
    renderWithTheme(<NurseDashboard {...defaultProps} />);
    const nameElements = screen.getAllByText('Коваленко Олена');
    expect(nameElements.length).toBeGreaterThan(0);
    expect(nameElements[0]).toBeInTheDocument();
  });

  it('renders weight chip when weight is provided', () => {
    renderWithTheme(<NurseDashboard {...defaultProps} />);
    expect(screen.getByText('65 kg')).toBeInTheDocument();
  });

  it('renders ward / bed chip', () => {
    renderWithTheme(<NurseDashboard {...defaultProps} />);
    expect(screen.getByText('Відділення 3 / 8')).toBeInTheDocument();
  });

  it('renders day number chip', () => {
    renderWithTheme(<NurseDashboard {...defaultProps} />);
    const dayElements = screen.getAllByText('Доба 2');
    expect(dayElements.length).toBeGreaterThanOrEqual(1);
  });

  it('shows OPEN status chip', () => {
    renderWithTheme(<NurseDashboard {...defaultProps} />);
    expect(screen.getByText('Відкритий')).toBeInTheDocument();
  });

  it('shows NURSE_SIGNED status correctly', () => {
    const props = { ...defaultProps, selectedDay: { ...baseDay, status: 'NURSE_SIGNED' as const } };
    renderWithTheme(<NurseDashboard {...props} />);
    expect(screen.getByText('Підписано медсестрою')).toBeInTheDocument();
  });

  it('shows DOCTOR_SIGNED status correctly', () => {
    const props = { ...defaultProps, selectedDay: { ...baseDay, status: 'DOCTOR_SIGNED' as const } };
    renderWithTheme(<NurseDashboard {...props} />);
    expect(screen.getByText('Підписано лікарем')).toBeInTheDocument();
  });

  it('renders episode ID truncated', () => {
    renderWithTheme(<NurseDashboard {...defaultProps} />);
    expect(screen.getByText(/Епізод #ep-2/)).toBeInTheDocument();
  });

  it('does not render weight chip when weight is null', () => {
    const props = { ...defaultProps, selectedDay: { ...baseDay, weightKg: null } };
    renderWithTheme(<NurseDashboard {...props} />);
    expect(screen.queryByText(/kg/)).not.toBeInTheDocument();
  });

  it('renders without crashing with null selectedDay', () => {
    const props = { ...defaultProps, selectedDay: null };
    renderWithTheme(<NurseDashboard {...props} />);
    const nameElements = screen.getAllByText('Коваленко Олена');
    expect(nameElements.length).toBeGreaterThanOrEqual(1);
  });

  it('does not crash with no episode patient name', () => {
    const props = { ...defaultProps, episode: { ...baseEpisode, patientName: null } };
    renderWithTheme(<NurseDashboard {...props} />);
    expect(screen.getByText('Patient')).toBeInTheDocument();
  });
});
