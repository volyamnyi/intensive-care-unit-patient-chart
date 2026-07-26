import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import PrescriptionItemForm from '../../../components/prescription/PrescriptionItemForm';
import type { MedicineCatalogItem } from '../../../types';

const theme = createTheme({});
const mockMedicines: MedicineCatalogItem[] = [
  { id: 1, name: 'Penicillin', categoryRef: 10, ptgCode: 'PTG1', isHighRisk: false },
  { id: 2, name: 'Paracetamol', categoryRef: 20, ptgCode: 'PTG2', isHighRisk: false },
];

function renderForm(props: Partial<React.ComponentProps<typeof PrescriptionItemForm>> = {}) {
  return render(
    <ThemeProvider theme={theme}>
      <PrescriptionItemForm
        onSubmit={props.onSubmit ?? vi.fn()}
        onSearchMedicine={props.onSearchMedicine ?? vi.fn(() => Promise.resolve(mockMedicines))}
        allergies={props.allergies ?? []}
        disabled={props.disabled}
      />
    </ThemeProvider>
  );
}

describe('PrescriptionItemForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders medicine input', () => {
    renderForm();
    expect(screen.getByLabelText('Препарат')).toBeInTheDocument();
    expect(screen.getByLabelText('Спосіб введення')).toBeInTheDocument();
    expect(screen.getByLabelText('Режим')).toBeInTheDocument();
  });

  it('calls onSearchMedicine when user types in medicine field', async () => {
    const onSearchMedicine = vi.fn(() => Promise.resolve(mockMedicines));
    renderForm({ onSearchMedicine });
    const input = screen.getByLabelText('Препарат');
    await userEvent.click(input);
    await userEvent.type(input, 'Pen');
    await waitFor(() => {
      expect(onSearchMedicine).toHaveBeenCalledWith('Pen');
    });
  });

  it('keeps add button disabled when no medicine selected', () => {
    renderForm();
    expect(screen.getByText('Додати')).toBeDisabled();
  });
});
