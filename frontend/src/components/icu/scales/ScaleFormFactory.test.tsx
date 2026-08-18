import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import ScaleFormFactory from './ScaleFormFactory';

describe('ScaleFormFactory', () => {
  it('renders ApacheIiForm for "APACHE II"', () => {
    render(<ScaleFormFactory scaleName="APACHE II" onCalculate={vi.fn()} />);
    expect(screen.getByText('APACHE II — параметри (найгірші за 24 год)')).toBeInTheDocument();
  });

  it('renders ApacheIiForm for "apache ii" (lowercase)', () => {
    render(<ScaleFormFactory scaleName="apache ii" onCalculate={vi.fn()} />);
    expect(screen.getByText('APACHE II — параметри (найгірші за 24 год)')).toBeInTheDocument();
  });

  it('renders SofaForm for "SOFA"', () => {
    render(<ScaleFormFactory scaleName="SOFA" onCalculate={vi.fn()} />);
    expect(screen.getByText('SOFA — параметри')).toBeInTheDocument();
  });

  it('renders SofaForm for "sofa" (lowercase)', () => {
    render(<ScaleFormFactory scaleName="sofa" onCalculate={vi.fn()} />);
    expect(screen.getByText('SOFA — параметри')).toBeInTheDocument();
  });

  it('renders CamIcuForm for "CAM-ICU"', () => {
    render(<ScaleFormFactory scaleName="CAM-ICU" onCalculate={vi.fn()} />);
    expect(screen.getByText('CAM-ICU — оцінка делірію')).toBeInTheDocument();
  });

  it('renders CamIcuForm for "cam" (short name)', () => {
    render(<ScaleFormFactory scaleName="cam" onCalculate={vi.fn()} />);
    expect(screen.getByText('CAM-ICU — оцінка делірію')).toBeInTheDocument();
  });

  it('renders BradenForm for "Браден"', () => {
    render(<ScaleFormFactory scaleName="Браден" onCalculate={vi.fn()} />);
    expect(screen.getByText('Шкала Браден — ризик пролежнів')).toBeInTheDocument();
  });

  it('renders BradenForm for "braden" (latin)', () => {
    render(<ScaleFormFactory scaleName="braden" onCalculate={vi.fn()} />);
    expect(screen.getByText('Шкала Браден — ризик пролежнів')).toBeInTheDocument();
  });

  it('renders RassSelector for "RASS"', () => {
    render(<ScaleFormFactory scaleName="RASS" onCalculate={vi.fn()} />);
    expect(screen.getByText('RASS:')).toBeInTheDocument();
  });

  it('renders RassSelector for "ричмонд"', () => {
    render(<ScaleFormFactory scaleName="ричмонд" onCalculate={vi.fn()} />);
    expect(screen.getByText('RASS:')).toBeInTheDocument();
  });

  it('returns null for unknown scale name', () => {
    const { container } = render(<ScaleFormFactory scaleName="UNKNOWN_SCALE" onCalculate={vi.fn()} />);
    expect(container.innerHTML).toBe('');
  });

  it('passes disabled prop to all forms', () => {
    render(<ScaleFormFactory scaleName="APACHE II" onCalculate={vi.fn()} disabled={true} />);
    expect(screen.getByText('Розрахувати APACHE II')).toBeDisabled();
  });

  it('passes onRassChange and rassValue to RassSelector', () => {
    const onRassChange = vi.fn();
    render(
      <ScaleFormFactory
        scaleName="RASS"
        onCalculate={vi.fn()}
        onRassChange={onRassChange}
        rassValue="-3"
      />
    );
    expect(screen.getByText('RASS:')).toBeInTheDocument();
  });
});
