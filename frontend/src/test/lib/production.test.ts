import { describe, it, expect } from 'vitest';
import { FLAG_LABELS, STATUS_LABELS, formatDurationSeconds } from '@/lib/production';

describe('lib/production', () => {
  it('formats durations like the spec examples', () => {
    expect(formatDurationSeconds(0)).toBe('0:00');
    expect(formatDurationSeconds(222)).toBe('0:03');
    expect(formatDurationSeconds(3 * 3600 + 42 * 60)).toBe('3:42');
    expect(formatDurationSeconds(5 * 3600 + 17 * 60)).toBe('5:17');
    expect(formatDurationSeconds(13740)).toBe('3:49');
    expect(formatDurationSeconds(90000)).toBe('1д 1:00');
    expect(formatDurationSeconds(null)).toBe('0:00');
    expect(formatDurationSeconds(-5)).toBe('0:00');
  });

  it('covers every status and attention flag', () => {
    for (const status of [
      'NEW',
      'IN_PROGRESS',
      'PAUSED',
      'BLOCKED_PATIENT',
      'BLOCKED_MATERIAL',
      'COMPLETED',
      'FAILED',
      'BRANCHED',
    ] as const) {
      expect(STATUS_LABELS[status].label).toBeTruthy();
    }
    for (const flag of ['FAILED', 'OVERDUE', 'REPEAT_BRAK', 'REWORK', 'STALE', 'NO_ASSIGNEE']) {
      expect(FLAG_LABELS[flag]).toBeTruthy();
    }
  });
});
