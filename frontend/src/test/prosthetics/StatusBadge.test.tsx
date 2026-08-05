import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusBadge } from '@/components/prosthetics/StatusBadge';
import type { FlowInstanceStatus } from '@/prosthetics/types';

describe('StatusBadge', () => {
  it.each<[FlowInstanceStatus, string]>([
    ['NEW', 'Новий'],
    ['IN_PROGRESS', 'В процесі'],
    ['PAUSED', 'Призупинено'],
    ['BLOCKED_PATIENT', 'Заблоковано (пацієнт)'],
    ['BLOCKED_MATERIAL', 'Заблоковано (матеріали)'],
    ['WAITING_REVIEW', 'Очікує перевірки'],
    ['CORRECTION', 'Корекція'],
    ['FAILED_QC', 'Не пройшов QA'],
    ['COMPLETED', 'Завершено'],
    ['FAILED', 'Провалено'],
  ])('renders %s status label', (status, label) => {
    render(<StatusBadge status={status} />);
    expect(screen.getByText(label)).toBeInTheDocument();
  });

  it('falls back to the raw status for unknown values', () => {
    render(<StatusBadge status={'UNKNOWN' as FlowInstanceStatus} />);
    expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
  });

  it('appends custom className to the badge', () => {
    const { container } = render(<StatusBadge status="COMPLETED" className="custom-class" />);
    expect(container.firstChild).toHaveClass('custom-class');
    expect(container.firstChild).toHaveClass('border-transparent');
  });
});
