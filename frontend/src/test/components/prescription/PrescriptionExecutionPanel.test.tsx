import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../../styles/ThemeContext';
import PrescriptionExecutionPanel from '../../../components/prescription/PrescriptionExecutionPanel';
import type { PrescriptionDayPart } from '../../../types';

const mockParts: PrescriptionDayPart[] = [
  { id: 'dp1', dayId: 'd1', period: 'morning', dose: '5mg', isPlanned: true, isPlannedFinished: false, isCompleted: false, isCompletedFinished: false, doctorName: null, nurseName: null },
  { id: 'dp2', dayId: 'd1', period: 'evening', dose: '10mg', isPlanned: true, isPlannedFinished: false, isCompleted: false, isCompletedFinished: false, doctorName: null, nurseName: null },
  { id: 'dp3', dayId: 'd1', period: 'night', dose: '10mg', isPlanned: true, isPlannedFinished: false, isCompleted: true, isCompletedFinished: false, doctorName: null, nurseName: null },
];

function renderPanel(props: Partial<React.ComponentProps<typeof PrescriptionExecutionPanel>> = {}) {
  return render(
    <ThemeModeProvider>
      <PrescriptionExecutionPanel
        dayParts={props.dayParts ?? mockParts}
        onExecute={props.onExecute ?? vi.fn()}
        executing={props.executing}
      />
    </ThemeModeProvider>
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

  it('opens 2FA dialog with login fields on execute click', async () => {
    const onExecute = vi.fn().mockResolvedValue(undefined);
    renderPanel({ onExecute });
    const inputs = screen.getAllByPlaceholderText('Фактична доза');
    await userEvent.type(inputs[0], '5mg');
    await userEvent.click(screen.getAllByText('Виконати')[0]);

    await waitFor(() => {
      expect(screen.getByText('2-факторна авторизація')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Логін другої особи')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Пароль')).toBeInTheDocument();
    });
  });

  it('calls onExecute with login+password when confirmed', async () => {
    const onExecute = vi.fn().mockResolvedValue(undefined);
    renderPanel({ onExecute });
    const inputs = screen.getAllByPlaceholderText('Фактична доза');
    await userEvent.type(inputs[0], '5mg');
    await userEvent.click(screen.getAllByText('Виконати')[0]);

    await waitFor(() => {
      expect(screen.getByText('2-факторна авторизація')).toBeInTheDocument();
    });

    await userEvent.type(screen.getByPlaceholderText('Логін другої особи'), 'nurse2');
    const passwordInputs = screen.getAllByPlaceholderText('Пароль');
    await userEvent.type(passwordInputs[0], 'nurse123');
    await userEvent.click(screen.getByText('Підтвердити'));

    await waitFor(() => {
      expect(onExecute).toHaveBeenCalledWith('dp1', '5mg', 'nurse2', 'nurse123');
    });
  });

  it('shows error when onExecute rejects', async () => {
    const onExecute = vi.fn().mockRejectedValue({
      response: { data: { message: 'Invalid credentials' } },
    });
    renderPanel({ onExecute });
    const inputs = screen.getAllByPlaceholderText('Фактична доза');
    await userEvent.type(inputs[0], '5mg');
    await userEvent.click(screen.getAllByText('Виконати')[0]);

    await waitFor(() => {
      expect(screen.getByText('2-факторна авторизація')).toBeInTheDocument();
    });

    await userEvent.type(screen.getByPlaceholderText('Логін другої особи'), 'nurse2');
    const passwordInputs2 = screen.getAllByPlaceholderText('Пароль');
    await userEvent.type(passwordInputs2[0], 'wrong');
    await userEvent.click(screen.getByText('Підтвердити'));

    await waitFor(() => {
      expect(screen.getByText('Invalid credentials')).toBeInTheDocument();
    });
  });

  it('shows error message when onExecute rejects without response data', async () => {
    const onExecute = vi.fn().mockRejectedValue(new Error('Network failure'));
    renderPanel({ onExecute });
    const inputs = screen.getAllByPlaceholderText('Фактична доза');
    await userEvent.type(inputs[0], '5mg');
    await userEvent.click(screen.getAllByText('Виконати')[0]);

    await waitFor(() => {
      expect(screen.getByText('2-факторна авторизація')).toBeInTheDocument();
    });

    await userEvent.type(screen.getByPlaceholderText('Логін другої особи'), 'nurse2');
    const passwordInputs = screen.getAllByPlaceholderText('Пароль');
    await userEvent.type(passwordInputs[0], 'nurse123');
    await userEvent.click(screen.getByText('Підтвердити'));

    await waitFor(() => {
      expect(screen.getByText('Network failure')).toBeInTheDocument();
    });
  });

  it('shows generic 2FA error when onExecute rejects without response or message', async () => {
    const onExecute = vi.fn().mockRejectedValue({});
    renderPanel({ onExecute });
    const inputs = screen.getAllByPlaceholderText('Фактична доза');
    await userEvent.type(inputs[0], '5mg');
    await userEvent.click(screen.getAllByText('Виконати')[0]);

    await waitFor(() => {
      expect(screen.getByText('2-факторна авторизація')).toBeInTheDocument();
    });

    await userEvent.type(screen.getByPlaceholderText('Логін другої особи'), 'nurse2');
    const passwordInputs = screen.getAllByPlaceholderText('Пароль');
    await userEvent.type(passwordInputs[0], 'nurse123');
    await userEvent.click(screen.getByText('Підтвердити'));

    await waitFor(() => {
      expect(screen.getByText('Помилка 2FA')).toBeInTheDocument();
    });
  });
});
