import { Stepper, Step } from '@/components/ui/stepper';
import { cn } from '@/lib/utils';

const steps = ['Пацієнт', 'Замовлення', 'Перегляд', 'Маршрут'];

interface SetupStepsProps {
  current: number;
  className?: string;
}

export function SetupSteps({ current, className }: SetupStepsProps) {
  return (
    <Stepper step={current} size="md" className={cn('gap-1.5', className)}>
      {steps.map((label, i) => (
        <Step
          key={label}
          title={<span className={i === current - 1 ? undefined : 'hidden md:inline'}>{label}</span>}
        />
      ))}
    </Stepper>
  );
}
