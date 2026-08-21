import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../../styles/ThemeContext';
import PrescriptionGrid from '../../../components/prescription/PrescriptionGrid';
import type { PrescriptionItem, PrescriptionDayPart } from '../../../types/medication';

type GridProps = React.ComponentProps<typeof PrescriptionGrid>;

function makeDayPart(over: Partial<PrescriptionDayPart> = {}): PrescriptionDayPart {
  return {
    id: `dp-${Math.random().toString(36).slice(2)}`,
    dayId: 'day-1',
    dayDate: new Date().toISOString().slice(0, 10),
    period: 'morning',
    dose: '5mg',
    isPlanned: false,
    isPlannedFinished: false,
    isCompleted: false,
    isCompletedFinished: false,
    doctorName: null,
    nurseName: null,
    ...over,
  };
}

function makeItem(dayParts: PrescriptionDayPart[] = []): PrescriptionItem {
  return {
    id: 'item-1',
    listId: 'list-1',
    medicineName: 'Dopamine',
    medicineMethod: 'IV',
    regime: 'stat',
    status: 'Active',
    sortOrder: 0,
    dayParts,
  };
}

const defaultItems: PrescriptionItem[] = [makeItem([makeDayPart()])];

function renderGrid(props: Partial<GridProps> = {}) {
  return render(
    <ThemeModeProvider>
      <PrescriptionGrid
        items={props.items ?? defaultItems}
        canEdit={props.canEdit ?? true}
        isDoctor={props.isDoctor ?? true}
        isNurse={props.isNurse ?? false}
        onPlan={props.onPlan ?? vi.fn()}
        onCancel={props.onCancel ?? vi.fn()}
        onAddDay={props.onAddDay}
        onRemoveDay={props.onRemoveDay}
        onAddItem={props.onAddItem ?? vi.fn()}
        onRemoveItem={props.onRemoveItem ?? vi.fn()}
        onSearchMedicine={props.onSearchMedicine ?? vi.fn().mockResolvedValue([])}
        allergies={props.allergies ?? []}
        loading={props.loading}
      />
    </ThemeModeProvider>
  );
}

// The first dose-bearing cell of the item row (first visible date, morning slot).
async function firstDayCell(): Promise<HTMLElement> {
  const row = (await screen.findByText('Dopamine')).closest('tr');
  expect(row).not.toBeNull();
  const cells = row!.querySelectorAll('td');
  // cells[0] is the sticky anchor column; cells[1] is the morning cell of date 1
  return cells[1] as HTMLElement;
}

describe('PrescriptionSpreadsheet — per-item day actions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the «Додати день» row button for a doctor', () => {
    renderGrid({ onAddDay: vi.fn() });
    expect(screen.getAllByRole('button', { name: 'Додати день' })).toHaveLength(1);
  });

  it('does not render the «Додати день» row button for a nurse', () => {
    renderGrid({ isNurse: true, isDoctor: false, onAddDay: vi.fn() });
    expect(screen.queryAllByRole('button', { name: 'Додати день' })).toHaveLength(0);
  });

  it('clicking «+ День» calls onAddDay(itemId) exactly once', async () => {
    const onAddDay = vi.fn().mockResolvedValue(undefined);
    renderGrid({ onAddDay });
    const btn = screen.getAllByRole('button', { name: 'Додати день' })[0];
    await userEvent.click(btn);
    await waitFor(() => expect(onAddDay).toHaveBeenCalledTimes(1));
    expect(onAddDay).toHaveBeenCalledWith('item-1');
  });

  it('contextmenu on a day cell opens the menu with «Видалити цей день» and triggers onRemoveDay', async () => {
    const onRemoveDay = vi.fn().mockResolvedValue(undefined);
    renderGrid({ onRemoveDay });
    fireEvent.contextMenu(await firstDayCell());

    const menu = await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    expect(menu).toBeInTheDocument();

    await userEvent.click(screen.getByRole('menuitem', { name: /Видалити цей день/ }));

    await waitFor(() => expect(onRemoveDay).toHaveBeenCalledTimes(1));
    expect(onRemoveDay).toHaveBeenCalledWith('item-1', 'day-1');
    expect(screen.queryByRole('menu', { name: 'Контекстне меню дня' })).not.toBeInTheDocument();
  });

  it('«Скасувати дозу» appears for planned not-completed doses and calls onCancel', async () => {
    const onCancel = vi.fn().mockResolvedValue(undefined);
    const planned = makeDayPart({ id: 'dp-planned', isPlanned: true, dose: '7.5mg' });
    renderGrid({ items: [makeItem([planned])], onCancel });

    const cell = screen.getAllByText('7.5mg')[0].closest('td');
    expect(cell).not.toBeNull();
    fireEvent.contextMenu(cell!);

    await userEvent.click(await screen.findByRole('menuitem', { name: /Скасувати дозу/ }));

    await waitFor(() => expect(onCancel).toHaveBeenCalledTimes(1));
    expect(onCancel).toHaveBeenCalledWith(planned.id);
  });

  it('«Скасувати дозу» is hidden for an unplanned cell', async () => {
    renderGrid();
    fireEvent.contextMenu(await firstDayCell());
    await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    expect(screen.queryByRole('menuitem', { name: /Скасувати дозу/ })).not.toBeInTheDocument();
  });

  it('does not open the context menu for a nurse', async () => {
    renderGrid({ isNurse: true, isDoctor: false });
    fireEvent.contextMenu(await firstDayCell());
    expect(screen.queryByRole('menu', { name: 'Контекстне меню дня' })).not.toBeInTheDocument();
  });

  it('closes the menu on Escape', async () => {
    renderGrid({ onRemoveDay: vi.fn() });
    fireEvent.contextMenu(await firstDayCell());
    await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    fireEvent.keyDown(document.body, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('menu', { name: 'Контекстне меню дня' })).not.toBeInTheDocument());
  });
});
