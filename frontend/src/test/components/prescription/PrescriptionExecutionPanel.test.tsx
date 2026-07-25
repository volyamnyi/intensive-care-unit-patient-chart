import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import PrescriptionExecutionPanel from '../../../components/prescription/PrescriptionExecutionPanel';
import type { PrescriptionDayPart } from '../../../types';

const theme = createTheme({});
const mockParts: PrescriptionDayPart[] = [
  { id: 'dp1', dayId: 'd1', period: 'morning', dose: '5mg', isPlanned: true, isPlannedFinished: false, isCompleted: false, isCompletedFinished: false, doctorName: null, nurseName: null },
  { id: 'dp2', dayId: 'd1', period: 'evening', dose: '10mg', isPlanned: true, isPlannedFinished: false, isCompleted: false, isCompletedFinished: false, doctorName: null, nurseName: null },
  { id: 'dp3', dayId: 'd1', period: 'night', dose: '10mg', isPlanned: true, isPlannedFinished: false, isCompleted: true, isCompletedFinished: false, doctorName: null, nurseName: null },
];

function renderPanel(props: Partial<React.ComponentProps<typeof PrescriptionExecutionPanel>> = {}) {
  return render(
    <ThemeProvider theme={theme}>
      <PrescriptionExecutionPanel
        dayParts={props.dayParts ?? mockParts}
        onExecute={props.onExecute ?? vi.fn()}
        executing={props.executing}
      />
    </ThemeProvider>
  );
}

describe('PrescriptionExecutionPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders only planned not completed parts', () => {
    renderPanel();
    expect(screen.getByText('Ранок')).toBeInTheDocument();
    expect(screen.getByText('Вечір')).toBeInTheDocument();
    expect(screen.queryByText('Ніч')).not.toBeInTheDocument();
  });

  it('shows empty state when no planned parts', () => {
    renderPanel({ dayParts: mockParts.map((p) => ({ ...p, isCompleted: true })) });
    expect(screen.getByText('Немає запланованих доз для виконання')).toBeInTheDocument();
  });

  it('calls onExecute with actual dose', async () => {
    const onExecute = vi.fn();
    renderPanel({ onExecute });
    const inputs = screen.getAllByLabelText('Фактична доза');
    await userEvent.type(inputs[0], '5mg');
    await userEvent.click(screen.getAllByText('Виконати')[0]);
    await waitFor(() => {
      expect(onExecute).toHaveBeenCalledWith('dp1', '5mg', false, undefined);
    });
  });

  it('calls onExecute with 2P auth when enabled', async () => {
    const onExecute = vi.fn();
    renderPanel({ onExecute });
    const inputs = screen.getAllByLabelText('Фактична доза');
    await userEvent.type(inputs[0], '5mg');
    const checkboxes = screen.getAllByLabelText('Потрібна');
    await userEvent.click(checkboxes[0]);
    await userEvent.type(screen.getAllByLabelText('ID другої особи')[0], 'user-2');
    await userEvent.click(screen.getAllByText('Виконати')[0]);
    await waitFor(() => {
      expect(onExecute).toHaveBeenCalledWith('dp1', '5mg', true, 'user-2');
    });
  });
});
