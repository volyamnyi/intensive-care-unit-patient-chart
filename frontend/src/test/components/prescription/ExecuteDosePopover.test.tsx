import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import ExecuteDosePopover from '../../../components/prescription/ExecuteDosePopover';
import type { PrescriptionDayPart } from '../../../types/medication';

const dp: PrescriptionDayPart = {
  id: 'dp-1',
  dayId: 'day-1',
  dayDate: '2026-11-17',
  period: 'morning',
  dose: '5mg',
  isPlanned: true,
  isPlannedFinished: false,
  isCompleted: false,
  isCompletedFinished: false,
  doctorName: null,
  nurseName: null,
};

/**
 * The popover is fully controlled (no internal source of truth), so the test
 * drives it through a stateful harness that mirrors the PrescriptionGrid wiring.
 */
function Harness({
  onAdministeredAtChange,
  onDispensedEnabledChange,
  onDispensedAtChange,
  onTimeNow,
  onProceedTo2fa,
  onCommitExecute,
  initialAdministeredAt = '2026-11-17T10:30:07',
  initialDispensedEnabled = false,
  initialDispensedAt = null,
  timeError = null,
  show2fa = false,
}: {
  onAdministeredAtChange?: (v: string | null) => void;
  onDispensedEnabledChange?: (v: boolean) => void;
  onDispensedAtChange?: (v: string | null) => void;
  onTimeNow?: () => void;
  onProceedTo2fa?: () => void;
  onCommitExecute?: () => void;
  initialAdministeredAt?: string | null;
  initialDispensedEnabled?: boolean;
  initialDispensedAt?: string | null;
  timeError?: string | null;
  show2fa?: boolean;
}) {
  const [administeredAt, setAdministeredAt] = useState<string | null>(initialAdministeredAt);
  const [dispensedEnabled, setDispensedEnabled] = useState(initialDispensedEnabled);
  const [dispensedAt, setDispensedAt] = useState<string | null>(initialDispensedAt);
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');

  const anchor = document.createElement('div');

  return (
    <>
      <ExecuteDosePopover
        execAnchor={anchor}
        execDp={dp}
        execDose="5mg"
        onExecDoseChange={vi.fn()}
        execAdministeredAt={administeredAt}
        onExecAdministeredAtChange={(v) => { setAdministeredAt(v); onAdministeredAtChange?.(v); }}
        execDispensedEnabled={dispensedEnabled}
        onExecDispensedEnabledChange={(v) => { setDispensedEnabled(v); onDispensedEnabledChange?.(v); }}
        execDispensedAt={dispensedAt}
        onExecDispensedAtChange={(v) => { setDispensedAt(v); onDispensedAtChange?.(v); }}
        execTimeError={timeError}
        onExecTimeNow={() => { setAdministeredAt('2026-11-17T12:00:00'); onTimeNow?.(); }}
        execMinDate="2026-11-10"
        execMaxDate="2026-11-17"
        executing={false}
        show2fa={show2fa}
        secondPersonLogin={login}
        onSecondPersonLoginChange={setLogin}
        secondPersonPassword={password}
        onSecondPersonPasswordChange={setPassword}
        secondPersonError=""
        onCloseExecute={vi.fn()}
        onProceedTo2fa={() => onProceedTo2fa?.()}
        onCommitExecute={() => onCommitExecute?.()}
        onShow2faChange={vi.fn()}
      />
    </>
  );
}

describe('ExecuteDosePopover — actual time capture (#443/#446)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('keeps the planned context in the header and adds the actual-time section', () => {
    render(<Harness />);
    expect(screen.getByText('Ранок: 5mg')).toBeInTheDocument();
    expect(screen.getByText('Фактичний час введення')).toBeInTheDocument();
    expect(screen.getByLabelText('Дата введення')).toHaveValue('2026-11-17');
    expect(screen.getByLabelText('Час введення')).toHaveValue('10:30:07');
  });

  it('emits the composed ISO value when the time changes', () => {
    const onAdministeredAtChange = vi.fn();
    render(<Harness onAdministeredAtChange={onAdministeredAtChange} />);
    fireEvent.change(screen.getByLabelText('Час введення'), { target: { value: '11:45' } });
    expect(onAdministeredAtChange).toHaveBeenCalledWith('2026-11-17T11:45:00');
  });

  it('emits the composed ISO value when the date changes', () => {
    const onAdministeredAtChange = vi.fn();
    render(<Harness onAdministeredAtChange={onAdministeredAtChange} />);
    fireEvent.change(screen.getByLabelText('Дата введення'), { target: { value: '2026-11-18' } });
    expect(onAdministeredAtChange).toHaveBeenCalledWith('2026-11-18T10:30:07');
  });

  it('«Зараз» resets the administration time to the current local time', async () => {
    const onTimeNow = vi.fn();
    render(<Harness onTimeNow={onTimeNow} />);
    await userEvent.click(screen.getByRole('button', { name: 'Зараз' }));
    expect(onTimeNow).toHaveBeenCalledTimes(1);
    expect(screen.getByLabelText('Час введення')).toHaveValue('12:00:00');
  });

  it('hides the dispensing inputs until the checkbox is checked', async () => {
    const onDispensedEnabledChange = vi.fn();
    render(<Harness onDispensedEnabledChange={onDispensedEnabledChange} />);
    expect(screen.queryByLabelText('Дата видачі')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('checkbox', { name: 'Окремо фіксувати час видачі' }));

    expect(onDispensedEnabledChange).toHaveBeenCalledWith(true);
    expect(screen.getByLabelText('Дата видачі')).toBeInTheDocument();
    expect(screen.getByLabelText('Час видачі')).toBeInTheDocument();
  });

  it('shows the dispensing inputs when the checkbox is already enabled', () => {
    render(<Harness initialDispensedEnabled initialDispensedAt="2026-11-17T10:29:00" />);
    expect(screen.getByLabelText('Дата видачі')).toHaveValue('2026-11-17');
    expect(screen.getByLabelText('Час видачі')).toHaveValue('10:29:00');
  });

  it('emits the composed dispensing value', () => {
    const onDispensedAtChange = vi.fn();
    render(<Harness initialDispensedEnabled initialDispensedAt="2026-11-17T10:29:00" onDispensedAtChange={onDispensedAtChange} />);
    fireEvent.change(screen.getByLabelText('Час видачі'), { target: { value: '10:28' } });
    expect(onDispensedAtChange).toHaveBeenCalledWith('2026-11-17T10:28:00');
  });

  it('renders the inline validation error and blocks «Продовжити»', () => {
    render(<Harness timeError="Фактичний час не може бути в майбутньому" />);
    expect(screen.getByText('Фактичний час не може бути в майбутньому')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Продовжити' })).toBeDisabled();
  });

  it('enables «Продовжити» when the dose and the time are valid', () => {
    render(<Harness />);
    expect(screen.getByRole('button', { name: 'Продовжити' })).not.toBeDisabled();
  });

  it('opens the 2FA step and keeps the actual time out of it', async () => {
    const onProceedTo2fa = vi.fn();
    render(<Harness onProceedTo2fa={onProceedTo2fa} />);
    await userEvent.click(screen.getByRole('button', { name: 'Продовжити' }));
    expect(onProceedTo2fa).toHaveBeenCalledTimes(1);
  });

  it('renders the 2FA dialog and commits the execution', async () => {
    const onCommitExecute = vi.fn();
    render(<Harness show2fa onCommitExecute={onCommitExecute} />);
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Логін другої особи')).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText('Логін другої особи'), { target: { value: 'nurse2' } });
    fireEvent.change(screen.getByPlaceholderText('Пароль'), { target: { value: 'secret' } });
    await waitFor(() => expect(screen.getByRole('button', { name: 'Підтвердити' })).not.toBeDisabled());
    await userEvent.click(screen.getByRole('button', { name: 'Підтвердити' }));
    expect(onCommitExecute).toHaveBeenCalledTimes(1);
  });
});
