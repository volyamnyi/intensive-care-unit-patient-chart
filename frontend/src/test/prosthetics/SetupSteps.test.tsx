import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SetupSteps } from '@/components/prosthetics/SetupSteps';

describe('SetupSteps', () => {
  it('renders all four step labels', () => {
    render(<SetupSteps current={1} />);
    expect(screen.getByText('Пацієнт')).toBeInTheDocument();
    expect(screen.getByText('Замовлення')).toBeInTheDocument();
    expect(screen.getByText('Перегляд')).toBeInTheDocument();
    expect(screen.getByText('Маршрут')).toBeInTheDocument();
  });

  it('marks no steps as done on the first step', () => {
    const { container } = render(<SetupSteps current={1} />);
    expect(container.querySelectorAll('.lucide-check').length).toBe(0);
  });

  it('marks completed steps with check icons', () => {
    const { container } = render(<SetupSteps current={3} />);
    expect(container.querySelectorAll('.lucide-check').length).toBe(2);
  });

  it('marks all previous steps as done on the last step', () => {
    const { container } = render(<SetupSteps current={4} />);
    expect(container.querySelectorAll('.lucide-check').length).toBe(3);
  });

  it('applies custom className', () => {
    const { container } = render(<SetupSteps current={1} className="custom-class" />);
    expect(container.querySelector('ol')).toHaveClass('custom-class');
  });
});
