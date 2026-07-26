import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SignDialog from '../../components/common/SignDialog';

function renderDialog(props: Partial<React.ComponentProps<typeof SignDialog>> = {}) {
  return render(
    <SignDialog
      open={props.open ?? false}
      onClose={props.onClose ?? vi.fn()}
      onConfirm={props.onConfirm ?? vi.fn()}
      dayNumber={props.dayNumber ?? 1}
      signing={props.signing}
      role={props.role}
    />
  );
}

describe('SignDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('opens when open=true', () => {
    renderDialog({ open: true });
    expect(screen.getByText('Підписання дня 1')).toBeInTheDocument();
  });

  it('does not render when open=false', () => {
    renderDialog({ open: false });
    expect(screen.queryByText('Підписання дня 1')).not.toBeInTheDocument();
  });

  it('shows day number in title', () => {
    renderDialog({ open: true, dayNumber: 3 });
    expect(screen.getByText('Підписання дня 3')).toBeInTheDocument();
  });

  it('shows role-specific text for doctor', () => {
    renderDialog({ open: true, role: 'DOCTOR' });
    expect(screen.getByText(/як лікар/)).toBeInTheDocument();
  });

  it('shows role-specific text for nurse', () => {
    renderDialog({ open: true, role: 'NURSE' });
    const matches = screen.getAllByText(/як медсестра/);
    expect(matches.length).toBeGreaterThan(0);
  });

  it('calls onClose when cancel button clicked', async () => {
    const onClose = vi.fn();
    renderDialog({ open: true, onClose });
    await userEvent.click(screen.getByText('Скасувати'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('calls onConfirm when confirm button clicked', async () => {
    const onConfirm = vi.fn();
    renderDialog({ open: true, onConfirm });
    await userEvent.click(screen.getByText('Підписати'));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('disables confirm button during signing', () => {
    renderDialog({ open: true, signing: true });
    const confirmBtn = screen.getByText('Підписання...');
    expect(confirmBtn).toBeDisabled();
  });
});
