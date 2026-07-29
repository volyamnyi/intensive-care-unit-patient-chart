import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import DoctorDashboard from '../../components/monitoring/DoctorDashboard';
import type { DashboardProps } from '../../components/monitoring/dashboardTypes';
import type { Episode, ClinicalDay } from '../../types';


const baseEpisode: Episode = {
  id: 'ep-1', patientId: 1001, patientName: 'Петренко Іван',
  hospitalizationId: null, departmentId: null,
  admissionDate: '2025-06-01T10:00:00Z', dischargeDate: null,
  status: 'ACTIVE', heightCm: 175, ward: 'Відділення 3', bedNumber: '12',
  admissionDiagnosis: 'Пневмонія', attendingDoctorId: null,
  createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1,
};

const baseDay: ClinicalDay = {
  id: 'day-1', episodeId: 'ep-1', dayNumber: 1,
  startDateTime: '2025-06-01T08:00:00Z', endDateTime: '2025-06-02T08:00:00Z',
  status: 'OPEN', doctorSigned: false, nurseSigned: false, closedAt: null,
  weightKg: 72, bmi: null,
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
  isNurse: false,
  user: { id: 1 },
  onRefresh: vi.fn(),
};

function renderWithTheme(ui: React.ReactElement) {
  return render(<ThemeModeProvider>{ui}</ThemeModeProvider>);
}

describe('DoctorDashboard', () => {
  it('renders patient name', () => {
    renderWithTheme(<DoctorDashboard {...defaultProps} />);
    const nameElements = screen.getAllByText('Петренко Іван');
    expect(nameElements.length).toBeGreaterThan(0);
    expect(nameElements[0]).toBeInTheDocument();
  });

  it('renders weight chip when weight is provided', () => {
    renderWithTheme(<DoctorDashboard {...defaultProps} />);
    expect(screen.getByText('72 kg')).toBeInTheDocument();
  });

  it('renders height chip when height is provided', () => {
    renderWithTheme(<DoctorDashboard {...defaultProps} />);
    expect(screen.getByText('175 cm')).toBeInTheDocument();
  });

  it('renders ward / bed chip', () => {
    renderWithTheme(<DoctorDashboard {...defaultProps} />);
    expect(screen.getByText('Відділення 3 / 12')).toBeInTheDocument();
  });

  it('renders diagnosis chip', () => {
    renderWithTheme(<DoctorDashboard {...defaultProps} />);
    expect(screen.getByText('Пневмонія')).toBeInTheDocument();
  });

  it('renders day number chip', () => {
    renderWithTheme(<DoctorDashboard {...defaultProps} />);
    // The day number appears both as a Chip and in the timeline; verify at least one exists
    const dayElements = screen.getAllByText('День 1');
    expect(dayElements.length).toBeGreaterThanOrEqual(1);
  });

  it('shows OPEN status chip for open days', () => {
    renderWithTheme(<DoctorDashboard {...defaultProps} />);
    expect(screen.getByText('Відкритий')).toBeInTheDocument();
  });

  it('shows NURSE_SIGNED status correctly', () => {
    const props = { ...defaultProps, selectedDay: { ...baseDay, status: 'NURSE_SIGNED' as const } };
    renderWithTheme(<DoctorDashboard {...props} />);
    expect(screen.getByText('Підписано медсестрою')).toBeInTheDocument();
  });

  it('shows DOCTOR_SIGNED status correctly', () => {
    const props = { ...defaultProps, selectedDay: { ...baseDay, status: 'DOCTOR_SIGNED' as const } };
    renderWithTheme(<DoctorDashboard {...props} />);
    expect(screen.getByText('Підписано лікарем')).toBeInTheDocument();
  });

  it('shows REOPENED status correctly', () => {
    const props = { ...defaultProps, selectedDay: { ...baseDay, status: 'REOPENED' as const } };
    renderWithTheme(<DoctorDashboard {...props} />);
    expect(screen.getByText('Відкрито повторно')).toBeInTheDocument();
  });

  it('renders episode ID truncated', () => {
    renderWithTheme(<DoctorDashboard {...defaultProps} />);
    expect(screen.getByText(/Епізод #ep-1/)).toBeInTheDocument();
  });

  it('does not render weight chip when weight is null', () => {
    const props = { ...defaultProps, selectedDay: { ...baseDay, weightKg: null } };
    renderWithTheme(<DoctorDashboard {...props} />);
    expect(screen.queryByText(/kg/)).not.toBeInTheDocument();
  });

  it('does not render height chip when height is null', () => {
    const props = { ...defaultProps, episode: { ...baseEpisode, heightCm: null } };
    renderWithTheme(<DoctorDashboard {...props} />);
    expect(screen.queryByText(/cm/)).not.toBeInTheDocument();
  });

  it('does not render ward chip when ward is null', () => {
    const props = { ...defaultProps, episode: { ...baseEpisode, ward: null, bedNumber: null } };
    renderWithTheme(<DoctorDashboard {...props} />);
    expect(screen.queryByText(/Відділення/)).not.toBeInTheDocument();
  });

  it('renders without crashing with null selectedDay', () => {
    const props = { ...defaultProps, selectedDay: null };
    renderWithTheme(<DoctorDashboard {...props} />);
    const nameElements = screen.getAllByText('Петренко Іван');
    expect(nameElements.length).toBeGreaterThanOrEqual(1);
  });
});
