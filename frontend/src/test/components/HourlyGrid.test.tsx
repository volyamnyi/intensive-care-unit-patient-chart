import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
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
    expect(onSaveCell).toHaveBeenCalledTimes(1);
    expect(onSaveCell).toHaveBeenCalledWith(8, 'heartRate', '90');

    const input2 = screen.getByLabelText('ЧД 8:00');
    fireEvent.change(input2, { target: { value: '18' } });
    fireEvent.keyDown(input2, { key: 'Escape' });
    expect(onSaveCell).toHaveBeenCalledTimes(1);
  });
});
