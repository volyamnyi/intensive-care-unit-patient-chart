import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import { MemoryRouter } from 'react-router-dom';
import DepartmentPatientCard from './DepartmentPatientCard';
import type { DepartmentPatient } from '../../types/icu';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const basePatient: DepartmentPatient = {
  id: 'ep-1',
  patientId: 1001,
  patientName: 'Петренко Іван',
  hospitalizationId: null,
  departmentId: null,
  admissionDate: '2025-06-01T10:00:00Z',
  dischargeDate: null,
  status: 'ACTIVE',
  attendingDoctorId: 1,
  attendingDoctorName: 'Доктор Іван',
  ward: 'Відділення 3',
  bedNumber: '12',
  admissionDiagnosis: 'Пневмонія',
  latestDayStatus: 'OPEN',
  latestDayNumber: 1,
  daysSinceAdmission: 2,
};

function renderCard(patient: DepartmentPatient = basePatient) {
  return render(
    <ThemeModeProvider>
      <MemoryRouter>
        <DepartmentPatientCard patient={patient} />
      </MemoryRouter>
    </ThemeModeProvider>
  );
}

describe('DepartmentPatientCard', () => {
  it('renders patient name', () => {
    renderCard();
    expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
  });

  it('renders day number chip', () => {
    renderCard();
    expect(screen.getByText('День 1')).toBeInTheDocument();
  });

  it('renders status chip for OPEN status', () => {
    renderCard();
    expect(screen.getByText('Відкрито')).toBeInTheDocument();
  });

  it('renders status chip for NURSE_SIGNED status', () => {
    renderCard({ ...basePatient, latestDayStatus: 'NURSE_SIGNED' });
    expect(screen.getByText('Підписано медсестрою')).toBeInTheDocument();
  });

  it('renders status chip for DOCTOR_SIGNED status', () => {
    renderCard({ ...basePatient, latestDayStatus: 'DOCTOR_SIGNED' });
    expect(screen.getByText('Підписано лікарем')).toBeInTheDocument();
  });

  it('renders status chip for CLOSED status', () => {
    renderCard({ ...basePatient, latestDayStatus: 'CLOSED' });
    expect(screen.getByText('Закрито')).toBeInTheDocument();
  });

  it('renders status chip for REOPENED status', () => {
    renderCard({ ...basePatient, latestDayStatus: 'REOPENED' });
    expect(screen.getByText('Відкрито повторно')).toBeInTheDocument();
  });

  it('renders ward / bed chip', () => {
    renderCard();
    expect(screen.getByText('Відділення 3 / 12')).toBeInTheDocument();
  });

  it('renders days since admission label', () => {
    renderCard();
    expect(screen.getByText('2 дні')).toBeInTheDocument();
  });

  it('renders daysSinceAdmission 0 as "Сьогодні"', () => {
    renderCard({ ...basePatient, daysSinceAdmission: 0 });
    expect(screen.getByText('Сьогодні')).toBeInTheDocument();
  });

  it('renders daysSinceAdmission 1 as "1 день"', () => {
    renderCard({ ...basePatient, daysSinceAdmission: 1 });
    expect(screen.getByText('1 день')).toBeInTheDocument();
  });

  it('renders daysSinceAdmission 4 as "4 дні"', () => {
    renderCard({ ...basePatient, daysSinceAdmission: 4 });
    expect(screen.getByText('4 дні')).toBeInTheDocument();
  });

  it('renders daysSinceAdmission 5 as "5 днів"', () => {
    renderCard({ ...basePatient, daysSinceAdmission: 5 });
    expect(screen.getByText('5 днів')).toBeInTheDocument();
  });

  it('renders diagnosis', () => {
    renderCard();
    expect(screen.getByText('Пневмонія')).toBeInTheDocument();
  });

  it('renders attending doctor name', () => {
    renderCard();
    expect(screen.getByText('Лікар: Доктор Іван')).toBeInTheDocument();
  });

  it('navigates to episode on click', async () => {
    renderCard();
    await userEvent.click(screen.getByText('Петренко Іван'));
    expect(mockNavigate).toHaveBeenCalledWith('/icu/doctor/episode/ep-1');
  });

  it('renders unknown patient when name is null', () => {
    renderCard({ ...basePatient, patientName: null });
    expect(screen.getByText('Невідомий пацієнт')).toBeInTheDocument();
  });

  it('does not render day chip when latestDayNumber is null', () => {
    renderCard({ ...basePatient, latestDayNumber: null });
    expect(screen.queryByText(/День/)).not.toBeInTheDocument();
  });

  it('does not render ward chip when ward is null', () => {
    renderCard({ ...basePatient, ward: null, bedNumber: null });
    expect(screen.queryByText(/Відділення/)).not.toBeInTheDocument();
  });

  it('does not render diagnosis when not provided', () => {
    renderCard({ ...basePatient, admissionDiagnosis: null });
    expect(screen.queryByText('Пневмонія')).not.toBeInTheDocument();
  });

  it('does not render doctor name when not provided', () => {
    renderCard({ ...basePatient, attendingDoctorName: null });
    expect(screen.queryByText(/Лікар:/)).not.toBeInTheDocument();
  });
});
