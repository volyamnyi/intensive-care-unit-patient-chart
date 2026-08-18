import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import HourlyRecordTable from '../../components/common/HourlyRecordTable';
import type { HourlyRecord } from '../../types/icu';


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
    consciousness: null, gcs: null, meanArterialPressure: null, etco2: null, fio2: null,
    dopamine: null, dobutamine: null, norepinephrine: null, epinephrine: null,
    urineOutput: null, drainOutput: null, gastricOutput: null, stool: null, vomit: null,
    bedPosition: null, headEnd: null,
    painScore: null, notes: null,
    createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1,
  },
];

function renderTable(records: HourlyRecord[] = mockRecords, hours: number[] = [8, 9, 10]) {
  return render(
    <ThemeModeProvider>
      <HourlyRecordTable records={records} hours={hours} />
    </ThemeModeProvider>
  );
}

describe('HourlyRecordTable', () => {
  it('renders table header columns', () => {
    renderTable();
    expect(screen.getByText('Година')).toBeInTheDocument();
    expect(screen.getByText('АТ сист.')).toBeInTheDocument();
    expect(screen.getByText('АТ діас.')).toBeInTheDocument();
    expect(screen.getByText('SpO₂')).toBeInTheDocument();
    expect(screen.getByText('Темп.')).toBeInTheDocument();
    expect(screen.getByText('ЦВТ')).toBeInTheDocument();
    expect(screen.getByText('ЧД')).toBeInTheDocument();
  });

  it('renders hour rows', () => {
    renderTable();
    expect(screen.getByText('8:00')).toBeInTheDocument();
    expect(screen.getByText('9:00')).toBeInTheDocument();
    expect(screen.getByText('10:00')).toBeInTheDocument();
  });

  it('shows record values for hours with data', () => {
    renderTable();
    expect(screen.getByText('120')).toBeInTheDocument();
    expect(screen.getByText('80')).toBeInTheDocument();
    expect(screen.getByText('72')).toBeInTheDocument();
    expect(screen.getByText('98')).toBeInTheDocument();
    expect(screen.getByText('36.6')).toBeInTheDocument();
    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getByText('16')).toBeInTheDocument();
  });

  it('shows dash for hours without data', () => {
    renderTable();
    const dashes = screen.getAllByText('—');
    expect(dashes.length).toBe(7);
  });

  it('shows record values for third hour row', () => {
    renderTable();
    const rows = screen.getAllByRole('row');
    expect(rows[3]).toHaveTextContent('10:00');
    expect(rows[3]).toHaveTextContent('130');
    expect(rows[3]).toHaveTextContent('85');
    expect(rows[3]).toHaveTextContent('76');
    expect(rows[3]).toHaveTextContent('97');
    expect(rows[3]).toHaveTextContent('36.8');
    expect(rows[3]).toHaveTextContent('7');
    expect(rows[3]).toHaveTextContent('18');
  });

  it('renders no rows when hours array is empty', () => {
    const { container } = renderTable([], []);
    const rows = container.querySelectorAll('tbody tr');
    expect(rows.length).toBe(0);
  });
});
