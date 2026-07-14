import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MedicalOrdersPanel from '../../components/common/MedicalOrdersPanel';
import type { MedicalOrder } from '../../types';

const mockOrder: MedicalOrder = {
  id: 'ord-1',
  clinicalDayId: 'day-1',
  category: 'MEDICATION',
  drugName: 'Дофамін',
  dose: '5',
  unit: 'мкг/кг/хв',
  route: 'в/в',
  frequency: 'постійно',
  startTime: '2025-06-01T10:00:00Z',
  endTime: null,
  status: 'ACTIVE',
  createdBy: 'doc-1',
  createdAt: '2025-06-01T10:00:00Z',
  updatedBy: 'doc-1',
  updatedAt: '2025-06-01T10:00:00Z',
  version: 1,
};

const mockOrders: MedicalOrder[] = [mockOrder];

function renderPanel(props: Partial<React.ComponentProps<typeof MedicalOrdersPanel>> = {}) {
  return render(
    <MedicalOrdersPanel
      orders={props.orders ?? []}
      onCreateOrder={props.onCreateOrder}
      onExecuteOrder={props.onExecuteOrder}
      onCancelOrder={props.onCancelOrder}
      canCreate={props.canCreate}
      canExecute={props.canExecute}
    />
  );
}

describe('MedicalOrdersPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows "Нове призначення" button when canCreate is true', () => {
    renderPanel({ canCreate: true });
    expect(screen.getByText('+ Нове призначення')).toBeInTheDocument();
  });

  it('does not render form elements when canCreate is false', () => {
    renderPanel({ canCreate: false });
    expect(screen.queryByText('+ Нове призначення')).not.toBeInTheDocument();
    expect(screen.queryByText('Нове призначення')).not.toBeInTheDocument();
  });

  it('shows form when create button is clicked', async () => {
    renderPanel({ canCreate: true });
    await userEvent.click(screen.getByText('+ Нове призначення'));
    await waitFor(() => {
      expect(screen.getByText('Нове призначення')).toBeInTheDocument();
      expect(screen.getByText('Створити')).toBeInTheDocument();
    });
  });

  it('fills form and submits a new order', async () => {
    const onCreateOrder = vi.fn();
    renderPanel({ canCreate: true, onCreateOrder });
    await userEvent.click(screen.getByText('+ Нове призначення'));
    await waitFor(() => {
      expect(screen.getByText('Нове призначення')).toBeInTheDocument();
    });
    const drugInput = screen.getByLabelText('Препарат');
    const doseInput = screen.getByLabelText('Доза');
    await userEvent.type(drugInput, 'Норадреналін');
    await userEvent.type(doseInput, '10');
    await userEvent.click(screen.getByText('Створити'));
    await waitFor(() => {
      expect(onCreateOrder).toHaveBeenCalledWith(
        expect.objectContaining({ drugName: 'Норадреналін', dose: '10' })
      );
    });
  });

  it('shows existing orders in a table', () => {
    renderPanel({ orders: mockOrders });
    expect(screen.getByText('Дофамін')).toBeInTheDocument();
    expect(screen.getByText('5 мкг/кг/хв')).toBeInTheDocument();
    expect(screen.getByText('в/в')).toBeInTheDocument();
    expect(screen.getByText('Активне')).toBeInTheDocument();
  });

  it('shows "Немає призначень" empty state', () => {
    renderPanel({ orders: [] });
    expect(screen.getByText('Немає призначень')).toBeInTheDocument();
  });

  it('shows execute button when canExecute is true', () => {
    const onExecuteOrder = vi.fn();
    renderPanel({ orders: mockOrders, canExecute: true, onExecuteOrder });
    const executeBtn = screen.getByRole('button', { name: '' });
    expect(executeBtn).toBeInTheDocument();
  });
});
