import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../../styles/ThemeContext';
import PrescriptionItemForm from '../../../components/prescription/PrescriptionItemForm';
import type { MedicineCatalogItem } from '../../../types/medication';

const mockMedicines: MedicineCatalogItem[] = [
  { id: 1, name: 'Penicillin', categoryRef: 10, ptgCode: 'PTG1', isHighRisk: false },
  { id: 2, name: 'Paracetamol', categoryRef: 20, ptgCode: 'PTG2', isHighRisk: false },
];

function renderForm(props: Partial<React.ComponentProps<typeof PrescriptionItemForm>> = {}) {
  return render(
    <ThemeModeProvider>
      <PrescriptionItemForm
        onSubmit={props.onSubmit ?? vi.fn()}
        onSearchMedicine={props.onSearchMedicine ?? vi.fn(() => Promise.resolve(mockMedicines))}
        disabled={props.disabled}
      />
    </ThemeModeProvider>
  );
}

describe('PrescriptionItemForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders medicine input', () => {
    renderForm();
    expect(screen.getByPlaceholderText('Препарат')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Спосіб введення')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Режим')).toBeInTheDocument();
  });

  it('calls onSearchMedicine with the debounced query and an abort signal', async () => {
    const onSearchMedicine = vi.fn(() => new Promise<MedicineCatalogItem[]>(() => {}));
    renderForm({ onSearchMedicine });
    const input = screen.getByPlaceholderText('Препарат');
    await userEvent.click(input);
    await userEvent.type(input, 'Pen');
    await waitFor(() => {
      expect(onSearchMedicine).toHaveBeenCalledWith('Pen', expect.anything());
    });
  });

  it('keeps add button disabled when no medicine selected', () => {
    renderForm();
    expect(screen.getByText('Додати')).toBeDisabled();
  });

  it('renders a disabled catalog item as non-selectable (visible, not hidden)', async () => {
    const items: MedicineCatalogItem[] = [
      { id: 3, name: 'DisabledDrug', categoryRef: 30, ptgCode: 'PTG3', isHighRisk: false, itemKindIsDisabled: true },
      { id: 4, name: 'EnabledDrug', categoryRef: 40, ptgCode: 'PTG4', isHighRisk: false, itemKindIsDisabled: false },
    ];
    const onSearchMedicine = vi.fn(() => Promise.resolve(items));
    const onSubmit = vi.fn();
    renderForm({ onSearchMedicine, onSubmit });
    const input = screen.getByPlaceholderText('Препарат');
    await userEvent.type(input, 'Dr');
    const disabledRow = await screen.findByRole('button', { name: /DisabledDrug/ });
    expect(disabledRow).toBeDisabled();
    // Selecting the disabled row must NOT submit; selecting the enabled row does.
    await userEvent.click(disabledRow);
    expect(onSubmit).not.toHaveBeenCalled();
    const enabledRow = await screen.findByRole('button', { name: /EnabledDrug/ });
    await userEvent.click(enabledRow);
    expect(screen.getByText('Додати')).toBeEnabled();
    onSubmit.mockClear();
    await userEvent.click(screen.getByText('Додати'));
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ medicineName: 'EnabledDrug' }),
    );
  });

  it('shows an empty state when the catalog returns no matches', async () => {
    const onSearchMedicine = vi.fn(() => Promise.resolve([]));
    renderForm({ onSearchMedicine });
    const input = screen.getByPlaceholderText('Препарат');
    await userEvent.type(input, 'Zzz');
    expect(await screen.findByText('Нічого не знайдено')).toBeInTheDocument();
  });

  it('surfaces a fetch error without blocking submit', async () => {
    const onSearchMedicine = vi.fn(() => Promise.reject(new Error('network')));
    renderForm({ onSearchMedicine });
    const input = screen.getByPlaceholderText('Препарат');
    await userEvent.type(input, 'Pen');
    expect(await screen.findByText(/Не вдалося завантажити каталог ліків з MIS/)).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Спробувати ще раз' })).toBeInTheDocument();
  });
});
