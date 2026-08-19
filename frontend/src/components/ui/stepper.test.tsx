import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Stepper, Step, StepDescription, StepTitle } from './stepper';

function stepEls(container: HTMLElement) {
  return container.querySelectorAll('[data-slot="stepper-step"]');
}

describe('Stepper', () => {
  it('renders step numbers for all steps', () => {
    const { container } = render(
      <Stepper>
        <Step title="Крок 1" />
        <Step title="Крок 2" />
        <Step title="Крок 3" />
      </Stepper>,
    );
    expect(screen.getByText('Крок 1')).toBeInTheDocument();
    expect(screen.getByText('Крок 2')).toBeInTheDocument();
    expect(screen.getByText('Крок 3')).toBeInTheDocument();
    expect(stepEls(container)).toHaveLength(3);
    expect(container.querySelectorAll('[data-slot="stepper-indicator"]')).toHaveLength(3);
  });

  it('marks previous steps completed, the current step active, and later steps inactive', () => {
    const { container } = render(
      <Stepper step={2}>
        <Step title="A" />
        <Step title="B" />
        <Step title="C" />
      </Stepper>,
    );
    const steps = stepEls(container);
    expect(steps).toHaveLength(3);
    expect(steps[0]).toHaveAttribute('data-state', 'completed');
    expect(steps[1]).toHaveAttribute('data-state', 'active');
    expect(steps[2]).toHaveAttribute('data-state', 'inactive');
  });

  it('sets aria-current on the active step indicator only', () => {
    const { container } = render(
      <Stepper step={1}>
        <Step title="A" />
        <Step title="B" />
      </Stepper>,
    );
    const active = container.querySelectorAll('[data-slot="stepper-indicator"][aria-current="step"]');
    expect(active).toHaveLength(1);
  });

  it('renders a description inside a step', () => {
    render(
      <Stepper>
        <Step>
          <StepTitle>Крок 1</StepTitle>
          <StepDescription>Опис кроку</StepDescription>
        </Step>
      </Stepper>,
    );
    expect(screen.getByText('Опис кроку')).toBeInTheDocument();
  });

  it('renders custom children instead of the default layout', () => {
    render(
      <Stepper>
        <Step title="Ігнор">Кастомний вміст</Step>
        <Step title="Другий" />
      </Stepper>,
    );
    expect(screen.getByText('Кастомний вміст')).toBeInTheDocument();
    expect(screen.queryByText('Ігнор')).not.toBeInTheDocument();
  });

  it('renders vertical separators in vertical orientation', () => {
    const { container } = render(
      <Stepper orientation="vertical">
        <Step title="A" />
        <Step title="B" />
      </Stepper>,
    );
    const separators = container.querySelectorAll('[data-slot="stepper-separator"]');
    expect(separators.length).toBeGreaterThan(0);
    expect(separators[0]).toHaveClass('w-px');
  });

  it('renders horizontal separators in horizontal orientation', () => {
    const { container } = render(
      <Stepper>
        <Step title="A" />
        <Step title="B" />
      </Stepper>,
    );
    const separators = container.querySelectorAll('[data-slot="stepper-separator"]');
    expect(separators.length).toBeGreaterThan(0);
    expect(separators[0]).toHaveClass('h-px');
  });

  it('calls onStepClick with the step index when an indicator is clicked in non-linear mode', async () => {
    const user = userEvent.setup();
    const onStepClick = vi.fn();
    render(
      <Stepper nonLinear onStepClick={onStepClick}>
        <Step title="A" />
        <Step title="B" />
        <Step title="C" />
      </Stepper>,
    );
    await user.click(screen.getByRole('button', { name: '2' }));
    expect(onStepClick).toHaveBeenCalledWith(2);
  });

  it('does not render interactive indicators without onStepClick', () => {
    render(
      <Stepper nonLinear>
        <Step title="A" />
      </Stepper>,
    );
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});