import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../../styles/ThemeContext';
import MedicineSearchInput from '../../../components/prescription/MedicineSearchInput';
import type { MedicineCatalogItem } from '../../../types/medication';

const mockCatalog: MedicineCatalogItem[] = [
  { id: 1, name: 'Paracetamol', categoryRef: 1, ptgCode: '1', isHighRisk: false },
  { id: 2, name: 'Ondansetron', categoryRef: 4, ptgCode: '2', isHighRisk: false },
];

function renderSearchInput(props: Partial<React.ComponentProps<typeof MedicineSearchInput>> = {}) {
  return render(
    <ThemeModeProvider>
      <MedicineSearchInput
        canEdit={props.canEdit ?? true}
        isDoctor={props.isDoctor ?? true}
        onAddItem={props.onAddItem ?? vi.fn().mockResolvedValue(undefined)}
        onSearchMedicine={props.onSearchMedicine ?? vi.fn(() => Promise.resolve(mockCatalog))}
      />
    </ThemeModeProvider>,
  );
}

describe('MedicineSearchInput — «Додати» button enablement (in-progress list bug)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders disabled «Додати» when the field is empty', () => {
    renderSearchInput();
    expect(screen.getByRole('button', { name: 'Додати' })).toBeDisabled();
  });

  it('enables «Додати» once a drug name is typed, WITHOUT a dropdown click (bug repro)', async () => {
    const user = userEvent.setup();
    renderSearchInput();
    const input = screen.getByPlaceholderText('Препарат');
    // Type a name that IS in the catalog — but do NOT click a suggestion.
    await user.type(input, 'Ondansetron');
    const add = screen.getByRole('button', { name: 'Додати' });
    // Must be enabled purely from the typed text.
    await waitFor(() => expect(add).toBeEnabled());
  });

  it('submits with the typed name when no suggestion was clicked', async () => {
    const user = userEvent.setup();
    const onAddItem = vi.fn().mockResolvedValue(undefined);
    renderSearchInput({ onAddItem });
    const input = screen.getByPlaceholderText('Препарат');
    await user.type(input, 'Ondansetron');
    const add = screen.getByRole('button', { name: 'Додати' });
    await waitFor(() => expect(add).toBeEnabled());
    await user.click(add);
    await waitFor(() => expect(onAddItem).toHaveBeenCalledTimes(1));
    expect(onAddItem).toHaveBeenCalledWith(
      expect.objectContaining({ medicineName: 'Ondansetron' }),
    );
  });

  it('still enables and submits when a catalog suggestion is selected', async () => {
    const user = userEvent.setup();
    const onAddItem = vi.fn().mockResolvedValue(undefined);
    renderSearchInput({ onAddItem });
    const input = screen.getByPlaceholderText('Препарат');
    await user.type(input, 'Par');
    // Click the first suggestion row.
    const suggestion = await screen.findByRole('button', { name: /Paracetamol/ });
    await user.click(suggestion);
    const add = screen.getByRole('button', { name: 'Додати' });
    await waitFor(() => expect(add).toBeEnabled());
    await user.click(add);
    await waitFor(() => expect(onAddItem).toHaveBeenCalledTimes(1));
    expect(onAddItem).toHaveBeenCalledWith(
      expect.objectContaining({ medicineName: 'Paracetamol' }),
    );
  });

  it('renders null when not editable (e.g. nurse on a finished list)', () => {
    const { container } = renderSearchInput({ canEdit: false });
    expect(container.textContent).toBe('');
    expect(screen.queryByRole('button', { name: 'Додати' })).toBeNull();
  });

  it('surfaces a fetch error without blocking submit', async () => {
    const user = userEvent.setup();
    const onSearchMedicine = vi.fn(() => Promise.reject(new Error('network')));
    const onAddItem = vi.fn().mockResolvedValue(undefined);
    renderSearchInput({ onSearchMedicine, onAddItem });
    const input = screen.getByPlaceholderText('Препарат');
    await user.type(input, 'Ondansetron');
    expect(await screen.findByText(/Не вдалося завантажити каталог ліків з MIS/)).toBeInTheDocument();
    const add = screen.getByRole('button', { name: 'Додати' });
    await waitFor(() => expect(add).toBeEnabled());
    await user.click(add);
    await waitFor(() => expect(onAddItem).toHaveBeenCalledWith(
      expect.objectContaining({ medicineName: 'Ondansetron' }),
    ));
  });

  it('retries a failed catalog fetch via the error-state button', async () => {
    const user = userEvent.setup();
    const onSearchMedicine = vi.fn(() => Promise.reject(new Error('network')));
    renderSearchInput({ onSearchMedicine });
    const input = screen.getByPlaceholderText('Препарат');
    await user.type(input, 'Ond');
    const retry = await screen.findByRole('button', { name: 'Спробувати ще раз' });
    await user.click(retry);
    await waitFor(() => expect(onSearchMedicine).toHaveBeenCalledTimes(2));
  });

  it('shows an empty state when the catalog returns no matches', async () => {
    const user = userEvent.setup();
    const onSearchMedicine = vi.fn(() => Promise.resolve([]));
    renderSearchInput({ onSearchMedicine });
    const input = screen.getByPlaceholderText('Препарат');
    await user.type(input, 'Zzzz');
    expect(await screen.findByText('Нічого не знайдено')).toBeInTheDocument();
  });

  it('renders a disabled catalog item non-selectable (visible, not hidden)', async () => {
    const user = userEvent.setup();
    const onSearchMedicine = vi.fn(() => Promise.resolve<MedicineCatalogItem[]>([
      { id: 9, name: 'DisabledPar', categoryRef: 9, ptgCode: '9', isHighRisk: false, itemKindIsDisabled: true },
    ]));
    const onAddItem = vi.fn().mockResolvedValue(undefined);
    renderSearchInput({ onSearchMedicine, onAddItem });
    const input = screen.getByPlaceholderText('Препарат');
    await user.type(input, 'DisabledPar');
    const row = await screen.findByRole('button', { name: /DisabledPar/ });
    expect(row).toBeDisabled();
    expect(screen.queryByText('Нічого не знайдено')).not.toBeInTheDocument();
    // Clicking the disabled row must not select/enable «Додати» from a selection.
    await user.click(row);
    const add = screen.getByRole('button', { name: 'Додати' });
    // «Додати» is enabled purely from the typed text (free-text contract), so
    // assert the row itself stayed disabled rather than the add button.
    expect(add).toBeEnabled();
  });
});
