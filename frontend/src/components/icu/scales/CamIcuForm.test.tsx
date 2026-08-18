import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import CamIcuForm from './CamIcuForm';

describe('CamIcuForm', () => {
  it('renders title and all checkboxes', () => {
    render(<CamIcuForm onCalculate={vi.fn()} />);
    expect(screen.getByText('CAM-ICU — оцінка делірію')).toBeInTheDocument();
    expect(screen.getByText('Гострий початок або флуктуюючий перебіг')).toBeInTheDocument();
    expect(screen.getByText('Нездатність утримувати увагу')).toBeInTheDocument();
    expect(screen.getByText('Дезорганізоване мислення')).toBeInTheDocument();
    expect(screen.getByText('Змінений рівень свідомості (RASS ≠ 0)')).toBeInTheDocument();
  });

  it('shows negative delirium by default', () => {
    render(<CamIcuForm onCalculate={vi.fn()} />);
    expect(screen.getByText('Делірій: НЕГАТИВНИЙ')).toBeInTheDocument();
  });

  it('shows positive delirium when all conditions met', () => {
    render(<CamIcuForm onCalculate={vi.fn()} />);
    const checkboxes = screen.getAllByRole('checkbox');
    fireEvent.click(checkboxes[0]);
    fireEvent.click(checkboxes[1]);
    fireEvent.click(checkboxes[2]);
    expect(screen.getByText('Делірій: ПОЗИТИВНИЙ')).toBeInTheDocument();
  });

  it('calls onCalculate with checkbox states when submitted', () => {
    const onCalculate = vi.fn();
    render(<CamIcuForm onCalculate={onCalculate} />);

    const checkboxes = screen.getAllByRole('checkbox');
    fireEvent.click(checkboxes[0]);
    fireEvent.click(checkboxes[1]);
    fireEvent.click(checkboxes[3]);

    fireEvent.click(screen.getByText('Зберегти CAM-ICU'));

    expect(onCalculate).toHaveBeenCalledWith({
      acuteOnset: true,
      inattention: true,
      disorganizedThinking: false,
      alteredConsciousness: true,
    });
  });

  it('button is always enabled', () => {
    render(<CamIcuForm onCalculate={vi.fn()} />);
    expect(screen.getByText('Зберегти CAM-ICU')).not.toBeDisabled();
  });

  it('disables all inputs when disabled prop is true', () => {
    render(<CamIcuForm onCalculate={vi.fn()} disabled={true} />);
    screen.getAllByRole('checkbox').forEach(cb => {
      expect(cb).toHaveAttribute('aria-disabled', 'true');
    });
    expect(screen.getByText('Зберегти CAM-ICU')).toBeDisabled();
  });
});
