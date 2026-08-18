import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import SofaForm from './SofaForm';

describe('SofaForm', () => {
  it('renders title and all input fields', () => {
    render(<SofaForm onCalculate={vi.fn()} />);
    expect(screen.getByText('SOFA — параметри')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('PaO₂ (mmHg)')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('FiO₂ (%)')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Тромбоцити (×10⁹/л)')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Білірубін (мкмоль/л або мг/дл)')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('MAP (mmHg)')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('GCS (3–15)')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Креатинін (мкмоль/л або мг/дл)')).toBeInTheDocument();
  });

  it('button is disabled when no fields filled', () => {
    render(<SofaForm onCalculate={vi.fn()} />);
    expect(screen.getByText('Розрахувати SOFA')).toBeDisabled();
  });

  it('button is enabled when at least one field is filled', () => {
    render(<SofaForm onCalculate={vi.fn()} />);
    fireEvent.change(screen.getByPlaceholderText('PaO₂ (mmHg)'), { target: { value: '80' } });
    expect(screen.getByText('Розрахувати SOFA')).not.toBeDisabled();
  });

  it('calls onCalculate with parsed values when submitted', () => {
    const onCalculate = vi.fn();
    render(<SofaForm onCalculate={onCalculate} />);

    fireEvent.change(screen.getByPlaceholderText('PaO₂ (mmHg)'), { target: { value: '80' } });
    fireEvent.change(screen.getByPlaceholderText('FiO₂ (%)'), { target: { value: '50' } });
    fireEvent.change(screen.getByPlaceholderText('Тромбоцити (×10⁹/л)'), { target: { value: '50' } });

    fireEvent.click(screen.getByText('Розрахувати SOFA'));

    expect(onCalculate).toHaveBeenCalledWith(
      expect.objectContaining({
        paO2: 80,
        fio2: 50,
        platelets: 50,
      })
    );
  });

  it('includes onVentilator boolean in the payload', () => {
    const onCalculate = vi.fn();
    render(<SofaForm onCalculate={onCalculate} />);

    fireEvent.change(screen.getByPlaceholderText('PaO₂ (mmHg)'), { target: { value: '80' } });
    fireEvent.click(screen.getByText('Розрахувати SOFA'));

    expect(onCalculate).toHaveBeenCalledWith(
      expect.objectContaining({
        onVentilator: false,
      })
    );
  });

  it('disables all inputs when disabled prop is true', () => {
    render(<SofaForm onCalculate={vi.fn()} disabled={true} />);
    expect(screen.getByPlaceholderText('PaO₂ (mmHg)')).toBeDisabled();
    expect(screen.getByText('Розрахувати SOFA')).toBeDisabled();
  });
});
