import type { FlowInstanceStatus } from '@/prosthetics/types';

export const STATUS_LABELS: Record<
  FlowInstanceStatus,
  { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' | 'link' }
> = {
  NEW: { label: 'Новий', variant: 'default' },
  IN_PROGRESS: { label: 'В процесі', variant: 'default' },
  PAUSED: { label: 'Призупинено', variant: 'outline' },
  BLOCKED_PATIENT: { label: 'Заблоковано (пацієнт)', variant: 'destructive' },
  BLOCKED_MATERIAL: { label: 'Заблоковано (матеріали)', variant: 'destructive' },
  COMPLETED: { label: 'Завершено', variant: 'default' },
  FAILED: { label: 'Завершено з помилкою', variant: 'destructive' },
  BRANCHED: { label: 'Розгалужено', variant: 'outline' },
};

export const FLAG_LABELS: Record<string, string> = {
  FAILED: 'Провал',
  OVERDUE: 'Прострочено',
  REPEAT_BRAK: 'Повторний брак',
  REWORK: 'Rework',
  STALE: 'Застій',
  NO_ASSIGNEE: 'Без виконавця',
};

/** Seconds → `H:MM`, with a day prefix past 24h (`3:42`, `5:17`, `1д 2:05`). */
export function formatDurationSeconds(totalSeconds: number | null | undefined): string {
  const total = Math.max(0, Math.floor(totalSeconds ?? 0));
  const days = Math.floor(total / 86400);
  const hours = Math.floor((total % 86400) / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const hmm = `${hours}:${String(minutes).padStart(2, '0')}`;
  return days > 0 ? `${days}д ${hmm}` : hmm;
}
