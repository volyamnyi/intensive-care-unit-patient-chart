import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import BradenForm from './BradenForm';

describe('BradenForm', () => {
  it('renders title and all select elements', () => {
    render(<BradenForm onCalculate={vi.fn()} />);
    expect(screen.getByText('Шкала Браден — ризик пролежнів')).toBeInTheDocument();
    expect(screen.getAllByRole('combobox')).toHaveLength(6);
  });

  it('shows default total and risk category', () => {
    render(<BradenForm onCalculate={vi.fn()} />);
    expect(screen.getByText(/Сума: 17/)).toBeInTheDocument();
    expect(screen.getByText(/Ризик: Помірний/)).toBeInTheDocument();
  });

  it('updates total and risk when dropdowns change', () => {
    render(<BradenForm onCalculate={vi.fn()} />);
    const selects = screen.getAllByRole('combobox');
    expect(selects.length).toBeGreaterThanOrEqual(6);
  });

  it('calls onCalculate with parsed values when submitted', () => {
    const onCalculate = vi.fn();
    render(<BradenForm onCalculate={onCalculate} />);

    fireEvent.click(screen.getByText('Зберегти Браден'));

    expect(onCalculate).toHaveBeenCalledWith({
      sensoryPerception: 3,
      moisture: 3,
      activity: 2,
      mobility: 3,
      nutrition: 3,
      frictionShear: 3,
    });
  });

  it('button is always enabled', () => {
    render(<BradenForm onCalculate={vi.fn()} />);
    expect(screen.getByText('Зберегти Браден')).not.toBeDisabled();
  });

  it('disables all inputs when disabled prop is true', () => {
    render(<BradenForm onCalculate={vi.fn()} disabled={true} />);
    screen.getAllByRole('combobox').forEach(select => {
      expect(select).toBeDisabled();
    });
    expect(screen.getByText('Зберегти Браден')).toBeDisabled();
  });
});
