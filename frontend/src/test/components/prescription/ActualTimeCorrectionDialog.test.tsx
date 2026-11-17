import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

const mockUpdateActualTime = vi.fn();
const mockToastSuccess = vi.fn();

vi.mock('../../../api/medication', () => ({
  prescriptionApi: {
    updateActualTime: (...a: unknown[]) => mockUpdateActualTime(...a),
  },
}));

vi.mock('sonner', () => ({
  toast: {
    success: (...a: unknown[]) => mockToastSuccess(...a),
    error: vi.fn(),
    info: vi.fn(),
    warning: vi.fn(),
  },
}));

import ActualTimeCorrectionDialog from '../../../components/prescription/ActualTimeCorrectionDialog';
import type { PrescriptionDayPart } from '../../../types/medication';

/** A past, in-window execution - stable regardless of the machine clock. */
const dayPart: PrescriptionDayPart = {
  id: 'dp-1',
  dayId: 'day-1',
  dayDate: '2020-01-02',
  period: 'morning',
  dose: '5mg',
  isPlanned: true,
  isPlannedFinished: false,
  isCompleted: true,
  isCompletedFinished: false,
  doctorName: 'Лікар',
  nurseName: 'Медсестра',
  executionId: 'exec-1',
  administeredAt: '2020-01-02T10:30:00',
  dispensedAt: '2020-01-02T10:29:00',
  administeredAtManual: true,
  executionVersion: 3,
};

type Props = React.ComponentProps<typeof ActualTimeCorrectionDialog>;

function renderDialog(over: Partial<Props> = {}) {
  const props: Props = {
    open: true,
    onOpenChange: vi.fn(),
    dayPart,
    minDate: '2020-01-01',
    maxDate: '2020-01-02',
    onUpdated: vi.fn(),
    ...over,
  };
  render(<ActualTimeCorrectionDialog {...props} />);
  return props;
}

describe('ActualTimeCorrectionDialog (#444/#446)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUpdateActualTime.mockResolvedValue({ data: {} });
  });

  it('shows the current values read-only', async () => {
    renderDialog();
    expect(await screen.findByText('Коригування фактичного часу')).toBeInTheDocument();
    expect(screen.getByText('Введено: 02.01.2020 10:30:00')).toBeInTheDocument();
    expect(screen.getByText('Видано: 02.01.2020 10:29:00')).toBeInTheDocument();
    expect(screen.getByText('(уточнено вручну)')).toBeInTheDocument();
  });

  it('seeds the editable fields with the current values', async () => {
    renderDialog();
    expect(await screen.findByLabelText('Нова дата введення')).toHaveValue('2020-01-02');
    expect(screen.getByLabelText('Новий час введення')).toHaveValue('10:30:00');
    expect(screen.getByLabelText('Нова дата видачі')).toHaveValue('2020-01-02');
    expect(screen.getByLabelText('Новий час видачі')).toHaveValue('10:29:00');
  });

  it('keeps «Зберегти» disabled until a reason is provided', async () => {
    renderDialog();
    const save = await screen.findByRole('button', { name: 'Зберегти' });
    expect(save).toBeDisabled();

    await userEvent.type(screen.getByLabelText('Причина коригування'), 'Помилка медсестри');
    expect(save).not.toBeDisabled();
  });

  it('sends the new time, the reason and the optimistic-lock version', async () => {
    const props = renderDialog();
    await screen.findByLabelText('Новий час введення');

    fireEvent.change(screen.getByLabelText('Новий час введення'), { target: { value: '11:00' } });
    await userEvent.type(screen.getByLabelText('Причина коригування'), 'Дозу введено о 11:00');

    await userEvent.click(screen.getByRole('button', { name: 'Зберегти' }));

    await waitFor(() => expect(mockUpdateActualTime).toHaveBeenCalledTimes(1));
    expect(mockUpdateActualTime).toHaveBeenCalledWith('exec-1', {
      administeredAt: '2020-01-02T11:00:00',
      dispensedAt: '2020-01-02T10:29:00',
      reason: 'Дозу введено о 11:00',
      version: 3,
    });
    expect(props.onOpenChange).toHaveBeenCalledWith(false);
    expect(props.onUpdated).toHaveBeenCalledTimes(1);
    expect(mockToastSuccess).toHaveBeenCalledWith('Фактичний час оновлено');
  });

  it('omits the dispensing time when the field is cleared', async () => {
    renderDialog();
    await screen.findByLabelText('Новий час видачі');

    fireEvent.change(screen.getByLabelText('Новий час видачі'), { target: { value: '' } });
    await userEvent.type(screen.getByLabelText('Причина коригування'), 'Видачу не фіксували');
    await userEvent.click(screen.getByRole('button', { name: 'Зберегти' }));

    await waitFor(() => expect(mockUpdateActualTime).toHaveBeenCalledTimes(1));
    expect(mockUpdateActualTime.mock.calls[0][1].dispensedAt).toBeUndefined();
  });

  it('shows a conflict banner on 409 and keeps the dialog open', async () => {
    mockUpdateActualTime.mockRejectedValue({ response: { status: 409, data: { message: 'VERSION_CONFLICT' } } });
    const props = renderDialog();
    await screen.findByLabelText('Причина коригування');

    await userEvent.type(screen.getByLabelText('Причина коригування'), 'Виправлення');
    await userEvent.click(screen.getByRole('button', { name: 'Зберегти' }));

    expect(await screen.findByText('Дані змінено іншим користувачем')).toBeInTheDocument();
    expect(props.onOpenChange).not.toHaveBeenCalled();
    expect(props.onUpdated).not.toHaveBeenCalled();
  });

  it('shows the server message inline on 422', async () => {
    mockUpdateActualTime.mockRejectedValue({
      response: { status: 422, data: { message: 'Фактичний час не може бути в майбутньому' } },
    });
    renderDialog();
    await screen.findByLabelText('Причина коригування');

    await userEvent.type(screen.getByLabelText('Причина коригування'), 'Виправлення');
    await userEvent.click(screen.getByRole('button', { name: 'Зберегти' }));

    expect(await screen.findByText('Фактичний час не може бути в майбутньому')).toBeInTheDocument();
  });

  it('blocks the submit when the new time is outside the list period', async () => {
    renderDialog({ maxDate: '2020-01-02' });
    await screen.findByLabelText('Новий час введення');

    fireEvent.change(screen.getByLabelText('Нова дата введення'), { target: { value: '2020-01-05' } });
    await userEvent.type(screen.getByLabelText('Причина коригування'), 'Виправлення');

    expect(await screen.findByText('Фактичний час поза межами періоду листка')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Зберегти' })).toBeDisabled();
    expect(mockUpdateActualTime).not.toHaveBeenCalled();
  });

  it('blocks the submit when the dispensing time is after the administration time', async () => {
    renderDialog();
    await screen.findByLabelText('Новий час введення');

    fireEvent.change(screen.getByLabelText('Новий час введення'), { target: { value: '09:00' } });
    await userEvent.type(screen.getByLabelText('Причина коригування'), 'Виправлення');

    expect(await screen.findByText('Час видачі не може бути пізніше часу введення')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Зберегти' })).toBeDisabled();
  });

  it('counts the reason characters', async () => {
    renderDialog();
    expect(await screen.findByText('0/500')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Причина коригування'), '12345');
    expect(screen.getByText('5/500')).toBeInTheDocument();
  });
});
