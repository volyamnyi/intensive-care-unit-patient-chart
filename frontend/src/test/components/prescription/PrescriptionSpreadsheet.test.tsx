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
        onCancelMedication={props.onCancelMedication ?? vi.fn()}
        onRestoreToPlanned={props.onRestoreToPlanned ?? vi.fn()}
        onAddDay={props.onAddDay}
        onRemoveDay={props.onRemoveDay ?? vi.fn()}
        onCancelAssignment={props.onCancelAssignment ?? vi.fn()}
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

  it('renders the «Видалити день» row button for a doctor', () => {
    renderGrid({ onAddDay: vi.fn(), onRemoveDay: vi.fn() });
    expect(screen.getAllByRole('button', { name: 'Видалити день' })).toHaveLength(1);
  });

  it('does not render the «Видалити день» row button for a nurse', () => {
    renderGrid({ isNurse: true, isDoctor: false, onAddDay: vi.fn(), onRemoveDay: vi.fn() });
    expect(screen.queryAllByRole('button', { name: 'Видалити день' })).toHaveLength(0);
  });

  it('clicking «−» calls onRemoveDay(itemId, lastDayId) with the max-date day', async () => {
    const onRemoveDay = vi.fn().mockResolvedValue(undefined);
    const today = new Date().toISOString().slice(0, 10);
    const mk = (id: string, dayId: string, dayDate: string): PrescriptionDayPart => ({
      ...makeDayPart({ id }),
      dayId,
      dayDate,
    });
    renderGrid({
      items: [makeItem([
        mk('dp-early', 'day-early', '2026-01-01'),
        mk('dp-late', 'day-late', '2026-02-01'),
        mk('dp-today', 'day-today', today),
      ])],
      onRemoveDay,
    });
    await userEvent.click(screen.getAllByRole('button', { name: 'Видалити день' })[0]);
    await waitFor(() => expect(onRemoveDay).toHaveBeenCalledTimes(1));
    expect(onRemoveDay).toHaveBeenCalledWith('item-1', 'day-today');
  });

  it('«−» is disabled when the row has a single day', () => {
    renderGrid({ onAddDay: vi.fn(), onRemoveDay: vi.fn() });
    expect(screen.getByRole('button', { name: 'Видалити день' })).toBeDisabled();
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

  it('contextmenu on a planned cell opens the menu with «Відмінити це призначення» and triggers onCancelAssignment', async () => {
    const onCancelAssignment = vi.fn().mockResolvedValue(undefined);
    const planned = makeDayPart({ id: 'dp-planned', isPlanned: true, dose: '5mg' });
    renderGrid({ items: [makeItem([planned])], onCancelAssignment });
    const cell = screen.getAllByText('5mg')[0].closest('td');
    expect(cell).not.toBeNull();
    fireEvent.contextMenu(cell!);

    const menu = await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    expect(menu).toBeInTheDocument();

    await userEvent.click(screen.getByRole('menuitem', { name: /Відмінити це призначення/ }));

    await waitFor(() => expect(onCancelAssignment).toHaveBeenCalledTimes(1));
    expect(onCancelAssignment).toHaveBeenCalledWith('dp-planned');
    expect(screen.queryByRole('menu', { name: 'Контекстне меню дня' })).not.toBeInTheDocument();
  });

  it('«Відмінити це призначення» appears for a cancelled cell and triggers onCancelAssignment', async () => {
    const onCancelAssignment = vi.fn().mockResolvedValue(undefined);
    const cancelled = makeDayPart({ id: 'dp-cancelled', isPlanned: true, isPlannedFinished: true, dose: '5mg' });
    renderGrid({ items: [makeItem([cancelled])], onCancelAssignment });
    const cell = screen.getAllByText('✕')[0].closest('td');
    expect(cell).not.toBeNull();
    fireEvent.contextMenu(cell!);

    await userEvent.click(await screen.findByRole('menuitem', { name: /Відмінити це призначення/ }));

    await waitFor(() => expect(onCancelAssignment).toHaveBeenCalledTimes(1));
    expect(onCancelAssignment).toHaveBeenCalledWith('dp-cancelled');
  });

  it('«Відмінити це призначення» is hidden for unplanned and completed cells', async () => {
    const completed = makeDayPart({ id: 'dp-completed', isPlanned: true, isCompleted: true, dose: '5mg' });
    renderGrid({ items: [makeItem([completed])] });
    const completedCell = screen.getAllByText('✓')[0].closest('td');
    fireEvent.contextMenu(completedCell!);
    await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    expect(screen.queryByRole('menuitem', { name: /Відмінити це призначення/ })).not.toBeInTheDocument();
    fireEvent.keyDown(document.body, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('menu', { name: 'Контекстне меню дня' })).not.toBeInTheDocument());

    fireEvent.contextMenu(await firstDayCell());
    await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    expect(screen.queryByRole('menuitem', { name: /Відмінити це призначення/ })).not.toBeInTheDocument();
  });

  it('«Відмінити препарат» appears for planned not-completed doses and calls onCancelMedication', async () => {
    const onCancelMedication = vi.fn().mockResolvedValue(undefined);
    const planned = makeDayPart({ id: 'dp-planned', isPlanned: true, dose: '7.5mg' });
    renderGrid({ items: [makeItem([planned])], onCancelMedication });

    const cell = screen.getAllByText('7.5mg')[0].closest('td');
    expect(cell).not.toBeNull();
    fireEvent.contextMenu(cell!);

    await userEvent.click(await screen.findByRole('menuitem', { name: /Відмінити препарат/ }));

    await waitFor(() => expect(onCancelMedication).toHaveBeenCalledTimes(1));
    expect(onCancelMedication).toHaveBeenCalledWith(planned.id);
  });

  it('«Відмінити препарат» is hidden for an unplanned cell', async () => {
    renderGrid();
    fireEvent.contextMenu(await firstDayCell());
    await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    expect(screen.queryByRole('menuitem', { name: /Відмінити препарат/ })).not.toBeInTheDocument();
  });

  it('«Відмінити препарат» is hidden for an already-cancelled cell', async () => {
    const cancelled = makeDayPart({ id: 'dp-cancelled', isPlanned: true, isPlannedFinished: true, dose: '7.5mg' });
    renderGrid({ items: [makeItem([cancelled])] });
    const cell = screen.getAllByText('✕')[0].closest('td');
    expect(cell).not.toBeNull();
    fireEvent.contextMenu(cell!);
    await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    expect(screen.queryByRole('menuitem', { name: /Відмінити препарат/ })).not.toBeInTheDocument();
  });

  it('«Повернути у Заплановано» appears only for cancelled cells and calls onRestoreToPlanned', async () => {
    const onRestoreToPlanned = vi.fn().mockResolvedValue(undefined);
    const cancelled = makeDayPart({ id: 'dp-cancelled', isPlanned: true, isPlannedFinished: true, dose: '7.5mg' });
    renderGrid({ items: [makeItem([cancelled])], onRestoreToPlanned });

    const cell = screen.getAllByText('✕')[0].closest('td');
    expect(cell).not.toBeNull();
    fireEvent.contextMenu(cell!);

    await userEvent.click(await screen.findByRole('menuitem', { name: /Повернути у Заплановано/ }));

    await waitFor(() => expect(onRestoreToPlanned).toHaveBeenCalledTimes(1));
    expect(onRestoreToPlanned).toHaveBeenCalledWith(cancelled.id);
  });

  it('«Повернути у Заплановано» is hidden for planned, unplanned and completed cells', async () => {
    const planned = makeDayPart({ id: 'dp-planned', isPlanned: true, dose: '7.5mg' });
    renderGrid({ items: [makeItem([planned])] });
    const plannedCell = screen.getAllByText('7.5mg')[0].closest('td');
    fireEvent.contextMenu(plannedCell!);
    await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    expect(screen.queryByRole('menuitem', { name: /Повернути у Заплановано/ })).not.toBeInTheDocument();
    fireEvent.keyDown(document.body, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('menu', { name: 'Контекстне меню дня' })).not.toBeInTheDocument());

    fireEvent.contextMenu(await firstDayCell());
    await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    expect(screen.queryByRole('menuitem', { name: /Повернути у Заплановано/ })).not.toBeInTheDocument();
  });

  it('neither «Відмінити препарат» nor «Повернути у Заплановано» appears for a completed cell', async () => {
    const completed = makeDayPart({ id: 'dp-completed', isPlanned: true, isCompleted: true, dose: '7.5mg' });
    renderGrid({ items: [makeItem([completed])] });
    const cell = screen.getAllByText('✓')[0].closest('td');
    expect(cell).not.toBeNull();
    fireEvent.contextMenu(cell!);
    await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    expect(screen.queryByRole('menuitem', { name: /Відмінити препарат/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('menuitem', { name: /Повернути у Заплановано/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('menuitem', { name: /Відмінити це призначення/ })).not.toBeInTheDocument();
  });

// Sparse fixture: Jun 15 lies beyond Jan 5 + 20 days, so it counts as added
// yet still renders inside the same 7-day window (the union has few dates).
function fullDay(dayId: string, dayDate: string): PrescriptionDayPart[] {
  return (['morning', 'day', 'evening', 'night'] as const).map((period) => makeDayPart({
    id: `dp-${dayId}-${period}`,
    dayId,
    dayDate,
    period,
    dose: null,
    isPlanned: false,
  }));
}

describe('PrescriptionSpreadsheet — added/removed day markers', () => {
  it('marks added-day headers and white cells, leaves base dates clean', () => {
    renderGrid({ items: [makeItem([...fullDay('day-base', '2026-01-05'), ...fullDay('day-added', '2026-06-15')])] });

    const addedHeader = screen.getByText('15 черв.').closest('th');
    expect(addedHeader).not.toBeNull();
    expect(addedHeader).toHaveAttribute('data-added-day', 'true');
    expect(addedHeader!.getAttribute('title')).toContain('Dopamine');

    const baseHeader = screen.getByText('05 січ.').closest('th');
    expect(baseHeader).not.toBeNull();
    expect(baseHeader!.hasAttribute('data-added-day')).toBe(false);

    // All four white slots of the added day are marked; base cells are not.
    expect(document.querySelectorAll('td[data-added-day="true"]')).toHaveLength(4);
    for (const cell of document.querySelectorAll('td[data-added-day="true"]')) {
      expect(cell.className).toMatch(/bg-muted/);
    }
  });

  it('planned cell on an added day keeps its status color and stays marker-free', () => {
    const addedDay = fullDay('day-added', '2026-06-15').map((p) =>
      p.period === 'morning' ? { ...p, dose: '5mg', isPlanned: true } : p,
    );
    renderGrid({ items: [makeItem([...fullDay('day-base', '2026-01-05'), ...addedDay])] });

    const cell = screen.getByText('5mg').closest('td');
    expect(cell).not.toBeNull();
    expect(cell!.hasAttribute('data-added-day')).toBe(false);
    expect(cell!.style.backgroundColor).toBe('rgb(187, 222, 251)');

    // The date itself is still marked at the header level.
    const addedHeader = screen.getByText('15 черв.').closest('th');
    expect(addedHeader).toHaveAttribute('data-added-day', 'true');
    expect(document.querySelectorAll('td[data-added-day="true"]')).toHaveLength(3);
  });

  it('marks the header following a removed gap', () => {
    const d1 = makeDayPart({ id: 'dp-1', dayId: 'day-1', dayDate: '2026-01-05', period: 'morning', dose: null, isPlanned: false });
    const d2 = makeDayPart({ id: 'dp-2', dayId: 'day-2', dayDate: '2026-01-07', period: 'morning', dose: null, isPlanned: false });
    renderGrid({ items: [makeItem([d1, d2])] });

    const gapHeader = screen.getByText('07 січ.').closest('th');
    expect(gapHeader).not.toBeNull();
    expect(gapHeader).toHaveAttribute('data-removed-gap', 'true');
    expect(gapHeader!.getAttribute('title')).toContain('07 січ.');

    const baseHeader = screen.getByText('05 січ.').closest('th');
    expect(baseHeader).not.toBeNull();
    expect(baseHeader!.hasAttribute('data-removed-gap')).toBe(false);
    expect(baseHeader!.hasAttribute('data-added-day')).toBe(false);
  });

  it('legend explains added and removed days', () => {
    renderGrid();
    expect(screen.getByText('Заплановано')).toBeInTheDocument();
    expect(screen.getByText('Доданий день')).toBeInTheDocument();
    expect(screen.getByText('Пропущений день')).toBeInTheDocument();
  });
});

  it('does not open the context menu for a nurse', async () => {
    renderGrid({ isNurse: true, isDoctor: false });
    fireEvent.contextMenu(await firstDayCell());
    expect(screen.queryByRole('menu', { name: 'Контекстне меню дня' })).not.toBeInTheDocument();
  });

  it('closes the menu on Escape', async () => {
    renderGrid();
    fireEvent.contextMenu(await firstDayCell());
    await screen.findByRole('menu', { name: 'Контекстне меню дня' });
    fireEvent.keyDown(document.body, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('menu', { name: 'Контекстне меню дня' })).not.toBeInTheDocument());
  });
});
