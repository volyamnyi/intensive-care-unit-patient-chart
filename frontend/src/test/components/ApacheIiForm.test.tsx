import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ApacheIiForm from '../../components/common/scales/ApacheIiForm';

describe('ApacheIiForm', () => {
  it('renders title and all input fields', () => {
    render(<ApacheIiForm onCalculate={vi.fn()} />);
    expect(screen.getByText('APACHE II — параметри (найгірші за 24 год)')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Температура (°C)')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('ЧСС (уд/хв)')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('GCS (3–15)')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Вік (роки)')).toBeInTheDocument();
  });

  it('button is disabled when no fields filled', () => {
    render(<ApacheIiForm onCalculate={vi.fn()} />);
    expect(screen.getByText('Розрахувати APACHE II')).toBeDisabled();
  });

  it('button is enabled when at least one field is filled', () => {
    render(<ApacheIiForm onCalculate={vi.fn()} />);
    fireEvent.change(screen.getByPlaceholderText('Температура (°C)'), { target: { value: '38.5' } });
    expect(screen.getByText('Розрахувати APACHE II')).not.toBeDisabled();
  });

  it('calls onCalculate with parsed values when submitted', () => {
    const onCalculate = vi.fn();
    render(<ApacheIiForm onCalculate={onCalculate} />);

    fireEvent.change(screen.getByPlaceholderText('Температура (°C)'), { target: { value: '38.5' } });
    fireEvent.change(screen.getByPlaceholderText('ЧСС (уд/хв)'), { target: { value: '110' } });
    fireEvent.change(screen.getByPlaceholderText('Вік (роки)'), { target: { value: '65' } });

    fireEvent.click(screen.getByText('Розрахувати APACHE II'));

    expect(onCalculate).toHaveBeenCalledWith(
      expect.objectContaining({
        temperatureC: 38.5,
        heartRate: 110,
        age: 65,
      })
    );
  });

  it('includes boolean fields in the payload', () => {
    const onCalculate = vi.fn();
    render(<ApacheIiForm onCalculate={onCalculate} />);

    fireEvent.change(screen.getByPlaceholderText('Температура (°C)'), { target: { value: '37.0' } });
    fireEvent.click(screen.getByText('Розрахувати APACHE II'));

    expect(onCalculate).toHaveBeenCalledWith(
      expect.objectContaining({
        acuteRenalFailure: false,
        chronicHealthType: 'NONE',
        emergencySurgical: false,
      })
    );
  });

  it('disables all inputs when disabled prop is true', () => {
    render(<ApacheIiForm onCalculate={vi.fn()} disabled={true} />);
    expect(screen.getByPlaceholderText('Температура (°C)')).toBeDisabled();
    expect(screen.getByText('Розрахувати APACHE II')).toBeDisabled();
  });
});
