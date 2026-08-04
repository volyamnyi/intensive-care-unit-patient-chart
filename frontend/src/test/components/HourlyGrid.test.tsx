import { describe, it, expect, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import HourlyGrid, { type HourlyGridProps } from '../../components/monitoring/HourlyGrid';
import type { ClinicalDay, HourlyRecord, MedicalOrder, OrderExecution } from '../../types';

const selectedDay = {
  id: 'day-1', episodeId: 'ep-1', dayNumber: 1,
  startDateTime: '2025-06-01T08:00:00Z', endDateTime: '2025-06-02T08:00:00Z',
  status: 'OPEN', doctorSigned: false, nurseSigned: false, closedAt: null,
  weightKg: 72, bmi: null, createdBy: 1, createdAt: '',
  updatedBy: 0, updatedAt: '', version: 1,
} as ClinicalDay;

function makeProps(overrides: Partial<HourlyGridProps> = {}): HourlyGridProps {
  return {
    isMobile: false,
    isNurse: false,
    isLocked: false,
    user: { id: 1 },
    selectedDay,
    recByHour: new Map<number, HourlyRecord>(),
    orders: [] as MedicalOrder[],
    activeOrders: [] as MedicalOrder[],
    executionsByOrder: {} as Record<string, OrderExecution[]>,
    executing: null,
    orderFormOpen: false,
    realClockHour: 10,
    canEditSidebar: true,
    onSetOrderFormOpen: vi.fn(),
    onSaveCell: vi.fn(),
    onPlanOrder: vi.fn(),
    onCancelOrder: vi.fn(),
    onExecuteOrder: vi.fn(),
    onExecuteFinishOrder: vi.fn(),
    ...overrides,
  };
}

describe('HourlyGrid', () => {
  it('defaults sticky/bare = false: no overflow-auto wrapper, <main> stays <main>', () => {
    const { container } = render(<HourlyGrid {...makeProps()} />);
    expect(container.querySelector('main')).not.toBeNull();
    expect(container.querySelector('div.overflow-auto')).toBeNull();
    expect(container.querySelector('div.overflow-x-auto')).not.toBeNull();
  });

  it('bare=true renders <div> instead of <main>', () => {
    const { container } = render(<HourlyGrid {...makeProps({ bare: true })} />);
    expect(container.querySelector('main')).toBeNull();
    const root = container.firstElementChild;
    expect(root?.tagName).toBe('DIV');
  });

  it('sticky=true adds overflow-auto wrapper, popup keeps overflow-hidden', () => {
    const { container } = render(<HourlyGrid {...makeProps({ sticky: true })} />);
    expect(container.querySelector('div.overflow-auto')).not.toBeNull();
    expect(container.querySelector('div.overflow-hidden')).not.toBeNull();
  });

  it('renders toolbar content', () => {
    render(<HourlyGrid {...makeProps({ toolbar: <div>Панель тест</div> })} />);
    expect(screen.getByText('Панель тест')).toBeInTheDocument();
  });

  it('onHeaderDoubleClick renders tooltip trigger and fires on double click', () => {
    const onHeaderDoubleClick = vi.fn();
    render(<HourlyGrid {...makeProps({ onHeaderDoubleClick })} />);
    const header = screen.getByText('Показник / година');
    expect(header.tagName).toBe('SPAN');
    fireEvent.doubleClick(header);
    expect(onHeaderDoubleClick).toHaveBeenCalledTimes(1);
  });

  it('without onHeaderDoubleClick renders plain header text', () => {
    render(<HourlyGrid {...makeProps()} />);
    const header = screen.getByText('Показник / година');
    expect(header.tagName).toBe('TH');
  });

  it('Cell: Enter blurs and saves, Escape does not save', () => {
    const onSaveCell = vi.fn();
    render(<HourlyGrid {...makeProps({ onSaveCell })} />);

    const input = screen.getByLabelText('ЧСС 8:00');
    fireEvent.change(input, { target: { value: '90' } });
    fireEvent.keyDown(input, { key: 'Enter' });
    fireEvent.blur(input);
    expect(onSaveCell).toHaveBeenCalledTimes(1);
    expect(onSaveCell).toHaveBeenCalledWith(8, 'heartRate', '90');

    const input2 = screen.getByLabelText('ЧД 8:00');
    fireEvent.change(input2, { target: { value: '18' } });
    fireEvent.keyDown(input2, { key: 'Escape' });
    expect(onSaveCell).toHaveBeenCalledTimes(1);
  });

  it('Cell: Ctrl+Z reverts the draft to the last saved value without saving (#140)', () => {
    const onSaveCell = vi.fn();
    render(<HourlyGrid {...makeProps({ onSaveCell })} />);

    const input = screen.getByLabelText('ЧСС 8:00');
    fireEvent.change(input, { target: { value: '90' } });
    fireEvent.keyDown(input, { key: 'z', ctrlKey: true });
    expect(input).toHaveValue(null);
    expect(onSaveCell).not.toHaveBeenCalled();
  });

  it('Cell: first Escape reverts a dirty draft instead of saving so a second press closes the dialog (#140)', () => {
    const onSaveCell = vi.fn();
    render(<HourlyGrid {...makeProps({ onSaveCell })} />);

    const input = screen.getByLabelText('ЧСС 8:00');
    fireEvent.change(input, { target: { value: '90' } });
    fireEvent.keyDown(input, { key: 'Escape' });
    expect(input).toHaveValue(null);
    expect(onSaveCell).not.toHaveBeenCalled();

    fireEvent.keyDown(input, { key: 'Escape' });
    expect(input).toHaveValue(null);
    expect(onSaveCell).not.toHaveBeenCalled();
  });

  it('Cell: Enter during IME composition does not blur or save (isComposing guard)', () => {
    const onSaveCell = vi.fn();
    render(<HourlyGrid {...makeProps({ onSaveCell })} />);

    const input = screen.getByLabelText('ЧСС 8:00');
    input.focus();
    fireEvent.change(input, { target: { value: '90' } });
    const composingEnter = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true });
    Object.defineProperty(composingEnter, 'isComposing', { value: true });
    fireEvent(input, composingEnter);
    expect(document.activeElement).toBe(input);
    expect(onSaveCell).not.toHaveBeenCalled();

    fireEvent.keyDown(input, { key: 'Enter' });
    expect(onSaveCell).toHaveBeenCalledTimes(1);
  });
});

describe('HourlyGrid glance layer (issue #139)', () => {
  const rec = (hour: number, overrides: Partial<HourlyRecord> = {}): HourlyRecord => ({
    id: `r${hour}`,
    clinicalDayId: 'day-1',
    recordTime: `2025-06-01T${String(hour).padStart(2, '0')}:00:00Z`,
    consciousness: null,
    gcs: null,
    temperature: null,
    heartRate: null,
    respiratoryRate: null,
    systolicBP: null,
    diastolicBP: null,
    meanArterialPressure: null,
    spo2: null,
    etco2: null,
    fio2: null,
    cvp: null,
    dopamine: null,
    dobutamine: null,
    norepinephrine: null,
    epinephrine: null,
    urineOutput: null,
    drainOutput: null,
    gastricOutput: null,
    stool: null,
    vomit: null,
    bedPosition: null,
    headEnd: null,
    painScore: null,
    notes: null,
    createdBy: 1,
    createdAt: '',
    updatedBy: 1,
    updatedAt: '',
    version: 1,
    ...overrides,
  });

  it('renders the rail only in sticky mode, with 24 hour buttons', () => {
    const { container } = render(<HourlyGrid {...makeProps()} />);
    expect(container.querySelector('[aria-label="Рейл відхилень"]')).toBeNull();
    const { container: stickyContainer } = render(<HourlyGrid {...makeProps({ sticky: true })} />);
    const rail = stickyContainer.querySelector('[aria-label="Рейл відхилень"]');
    expect(rail).not.toBeNull();
    expect(rail?.querySelectorAll('button[data-hour]')).toHaveLength(24);
  });

  it('colors rail cells by violation count (neutral/1/2+) and hatches incomplete hours', () => {
    const recByHour = new Map<number, HourlyRecord>([
      [9, rec(9, { heartRate: 131, spo2: 89 })],
      [10, rec(10, { heartRate: 131 })],
    ]);
    const { container } = render(<HourlyGrid {...makeProps({ sticky: true, recByHour })} />);
    const rail = container.querySelector('[aria-label="Рейл відхилень"]');
    const cell = (h: number) => rail?.querySelector(`button[data-hour="${h}"]`);
    expect(cell(9)?.getAttribute('data-count')).toBe('2');
    expect(cell(9)?.className).toContain('bg-destructive');
    expect(cell(10)?.getAttribute('data-count')).toBe('1');
    expect(cell(10)?.className).toContain('bg-warning');
    expect(cell(10)?.getAttribute('data-incomplete')).toBeNull();
    expect(cell(11)?.getAttribute('data-incomplete')).toBeNull();
    expect(cell(8)?.getAttribute('data-incomplete')).toBe('true');
    expect(cell(8)?.className).toContain('repeating-linear-gradient');
  });

  it('rail click scrolls the table to the matching hour column', () => {
    const scrollIntoView = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoView;
    const { container } = render(<HourlyGrid {...makeProps({ sticky: true })} />);
    const rail = container.querySelector('[aria-label="Рейл відхилень"]');
    fireEvent.click(rail?.querySelector('button[data-hour="14"]') as Element);
    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'nearest', inline: 'center' });
    const header = container.querySelector('[data-hour-col="14"]');
    expect(header).not.toBeNull();
  });

  it('marks the current-hour column header with the 2px accent', () => {
    const { container } = render(<HourlyGrid {...makeProps({ realClockHour: 10 })} />);
    const header = container.querySelector('[data-hour-col="10"]');
    expect(header?.className).toContain('shadow-[inset_0_-2px_0_0_var(--color-primary)]');
    expect(container.querySelector('[data-hour-col="9"]')?.className).not.toContain('inset_0_-2px');
  });

  it('marks critical cells with data-critical and tabIndex -1, leaving others focusable', () => {
    const recByHour = new Map<number, HourlyRecord>([
      [8, rec(8, { heartRate: 131 })],
      [9, rec(9, { heartRate: 80 })],
    ]);
    render(<HourlyGrid {...makeProps({ recByHour })} />);
    const criticalInput = screen.getByLabelText('ЧСС 8:00');
    const criticalCell = criticalInput.closest('td[data-critical="true"]');
    expect(criticalCell).not.toBeNull();
    expect((criticalCell as HTMLElement).tabIndex).toBe(-1);
    const normalInput = screen.getByLabelText('ЧСС 9:00');
    expect(normalInput.closest('td[data-critical="true"]')).toBeNull();
  });

  it('flashes the cell green for 300ms on a valid save, and not on a critical save', () => {
    vi.useFakeTimers();
    render(<HourlyGrid {...makeProps()} />);
    const input = screen.getByLabelText('ЧСС 8:00');
    const cell = input.closest('td') as HTMLElement;

    fireEvent.change(input, { target: { value: '90' } });
    fireEvent.blur(input);
    expect(cell.className).toContain('bg-success/30');
    act(() => { vi.advanceTimersByTime(300); });
    expect(cell.className).not.toContain('bg-success/30');

    fireEvent.change(input, { target: { value: '131' } });
    fireEvent.blur(input);
    expect(cell.className).not.toContain('bg-success/30');
    vi.useRealTimers();
  });
});
