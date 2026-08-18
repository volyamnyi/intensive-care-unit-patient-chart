import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import VentilationPanel from './VentilationPanel';
import type { VentilationSettings, VentilationCreateRequest } from '../../types/icu';

const mockVent: VentilationSettings[] = [
  {
    id: 'v-1', clinicalDayId: 'day-1', recordHour: 8, mode: 'CMV', fio2: 50, peep: 8,
    tidalVolume: 450, minuteVolume: null, pinsp: null, psupport: null,
    triggerType: '', ieRatio: '', respiratoryRate: 16, plateauPressure: 22,
    meanAirwayPressure: null, version: 1,
  },
];

function renderPanel(props: Partial<React.ComponentProps<typeof VentilationPanel>> = {}) {
  const utils = render(
    <VentilationPanel
      clinicalDayId="day-1"
      ventilation={props.ventilation ?? []}
      isLocked={props.isLocked ?? false}
      onCreate={props.onCreate ?? vi.fn()}
    />
  );
  return { ...utils, selects: () => Array.from(utils.container.querySelectorAll('select')) as HTMLSelectElement[] };
}

describe('VentilationPanel', () => {
  beforeEach(() => vi.clearAllMocks());

  it('shows empty message when no ventilation entries', () => {
    renderPanel({ ventilation: [] });
    expect(screen.getByText('Немає налаштувань вентиляції')).toBeInTheDocument();
  });

  it('renders existing ventilation entries', () => {
    renderPanel({ ventilation: mockVent });
    expect(screen.getByText('CMV')).toBeInTheDocument();
    expect(screen.getByText(/FiO₂ 50%/)).toBeInTheDocument();
  });

  it('hides create UI when locked', () => {
    renderPanel({ ventilation: [], isLocked: true });
    expect(screen.queryByLabelText('Режим')).not.toBeInTheDocument();
  });

  it('creates a ventilation entry from the form', async () => {
    const onCreate = vi.fn();
    renderPanel({ ventilation: [], onCreate });
    await userEvent.click(screen.getByLabelText('Режим'));
    await userEvent.click(await screen.findByRole('option', { name: 'SIMV' }));
    const numberInputs = Array.from(document.querySelectorAll('input[type="number"]')) as HTMLInputElement[];
    fireEvent.change(numberInputs[0], { target: { value: '12' } });
    fireEvent.change(numberInputs[1], { target: { value: '40' } });
    await userEvent.click(screen.getByText('Додати'));
    await waitFor(() => {
      const call = onCreate.mock.calls[0][0] as VentilationCreateRequest;
      expect(call.mode).toBe('SIMV');
      expect(call.recordHour).toBe(12);
      expect(call.fio2).toBe(40);
    });
  });
});
