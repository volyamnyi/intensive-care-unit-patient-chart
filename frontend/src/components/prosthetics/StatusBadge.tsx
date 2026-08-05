import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type { FlowInstanceStatus } from '@/prosthetics/types';

const STATUS_MAP: Record<FlowInstanceStatus, { label: string; className: string }> = {
  NEW: { label: 'Новий', className: 'bg-mint text-mint-foreground' },
  IN_PROGRESS: { label: 'В процесі', className: 'bg-primary text-primary-foreground' },
  PAUSED: { label: 'Призупинено', className: 'bg-warning text-warning-foreground' },
  BLOCKED_PATIENT: { label: 'Заблоковано (пацієнт)', className: 'bg-destructive text-destructive-foreground' },
  BLOCKED_MATERIAL: { label: 'Заблоковано (матеріали)', className: 'bg-destructive text-destructive-foreground' },
  WAITING_REVIEW: { label: 'Очікує перевірки', className: 'bg-secondary text-secondary-foreground' },
  CORRECTION: { label: 'Корекція', className: 'bg-orange-500 text-white' },
  FAILED_QC: { label: 'Не пройшов QA', className: 'bg-destructive text-destructive-foreground' },
  COMPLETED: { label: 'Завершено', className: 'bg-success text-success-foreground' },
  FAILED: { label: 'Провалено', className: 'bg-destructive text-destructive-foreground' },
};

export function StatusBadge({ status, className }: { status: FlowInstanceStatus; className?: string }) {
  const mapped = STATUS_MAP[status] ?? { label: status, className: 'bg-muted text-muted-foreground' };
  return <Badge className={cn('border-transparent', mapped.className, className)}>{mapped.label}</Badge>;
}