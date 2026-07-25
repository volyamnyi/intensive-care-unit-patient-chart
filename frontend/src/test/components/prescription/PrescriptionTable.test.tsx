import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import PrescriptionTable from '../../../components/prescription/PrescriptionTable';
import type { PrescriptionList } from '../../../types';

const theme = createTheme({});
const mockPrescriptions: PrescriptionList[] = [
  { id: 'p1', patientId: 1001, hospitalizationId: null, departmentId: null, documentName: 'Листок 1', status: 'Saved', editingUserId: null },
  { id: 'p2', patientId: 1002, hospitalizationId: null, departmentId: null, documentName: 'Листок 2', status: 'Finished', editingUserId: null },
];

function renderTable(props: Partial<React.ComponentProps<typeof PrescriptionTable>> = {}) {
  return render(
    <ThemeProvider theme={theme}>
      <PrescriptionTable prescriptions={props.prescriptions ?? mockPrescriptions} onSelect={props.onSelect} loading={props.loading} />
    </ThemeProvider>
  );
}

describe('PrescriptionTable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders prescription rows', () => {
    renderTable();
    expect(screen.getByText('Листок 1')).toBeInTheDocument();
    expect(screen.getByText('Листок 2')).toBeInTheDocument();
    expect(screen.getByText('1001')).toBeInTheDocument();
  });

  it('shows loading state', () => {
    renderTable({ loading: true, prescriptions: [] });
    expect(screen.getByText('Завантаження...')).toBeInTheDocument();
  });

  it('shows empty state', () => {
    renderTable({ prescriptions: [] });
    expect(screen.getByText('Немає призначень')).toBeInTheDocument();
  });

  it('calls onSelect when row is clicked', async () => {
    const onSelect = vi.fn();
    renderTable({ onSelect });
    await userEvent.click(screen.getAllByText('Відкрити')[0]);
    await waitFor(() => {
      expect(onSelect).toHaveBeenCalledWith(mockPrescriptions[0]);
    });
  });
});
