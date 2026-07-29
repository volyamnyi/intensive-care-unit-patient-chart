import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../../styles/ThemeContext';
import VitalSignForm from '../../../components/prescription/VitalSignForm';
import type { VitalSignEntry } from '../../../types';

const latest: VitalSignEntry = {
  id: 'vs1', dayId: 'd1', period: 'morning',
  temperature: 36.6, systolicBp: 120, diastolicBp: 80, spo2: 98, pulse: 72, stool: 'normal', painScore: 2,
};

function renderForm(props: Partial<React.ComponentProps<typeof VitalSignForm>> = {}) {
  return render(
    <ThemeModeProvider>
      <VitalSignForm onSubmit={props.onSubmit ?? vi.fn()} latest={props.latest ?? latest} disabled={props.disabled} saving={props.saving} />
    </ThemeModeProvider>
  );
}

describe('VitalSignForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders latest vital signs', () => {
    renderForm();
    expect(screen.getByText(/Останні:/)).toBeInTheDocument();
    expect(screen.getByText(/36\.6/)).toBeInTheDocument();
  });

  it('submits entered values', async () => {
    const onSubmit = vi.fn();
    renderForm({ onSubmit });
    await userEvent.clear(screen.getByLabelText('Пульс'));
    await userEvent.type(screen.getByLabelText('Пульс'), '80');
    await userEvent.click(screen.getByText('Зберегти'));
    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ pulse: 80 }));
    });
  });
});
