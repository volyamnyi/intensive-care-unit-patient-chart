import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../../styles/ThemeContext';
import ClosePrescriptionDialog from '../../../components/prescription/ClosePrescriptionDialog';


function renderDialog(props: Partial<React.ComponentProps<typeof ClosePrescriptionDialog>> = {}) {
  return render(
    <ThemeModeProvider>
      <ClosePrescriptionDialog
        open={props.open ?? true}
        onClose={props.onClose ?? vi.fn()}
        onConfirm={props.onConfirm ?? vi.fn()}
        allCompleted={props.allCompleted ?? false}
        closing={props.closing}
      />
    </ThemeModeProvider>
  );
}

describe('ClosePrescriptionDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders warning when not all completed', () => {
    renderDialog({ allCompleted: false });
    expect(screen.getByText(/Не всі частини доби виконані/)).toBeInTheDocument();
  });

  it('does not render warning when all completed', () => {
    renderDialog({ allCompleted: true });
    expect(screen.queryByText(/Не всі частини доби виконані/)).not.toBeInTheDocument();
  });

  it('calls onConfirm when close button clicked', async () => {
    const onConfirm = vi.fn();
    renderDialog({ onConfirm });
    await userEvent.click(screen.getByText('Закрити'));
    await waitFor(() => {
      expect(onConfirm).toHaveBeenCalled();
    });
  });

  it('calls onClose when cancel button clicked', async () => {
    const onClose = vi.fn();
    renderDialog({ onClose });
    await userEvent.click(screen.getByText('Скасувати'));
    await waitFor(() => {
      expect(onClose).toHaveBeenCalled();
    });
  });
});
