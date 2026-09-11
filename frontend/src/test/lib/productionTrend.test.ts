import { describe, it, expect } from 'vitest';
import { bucketTrend } from '@/lib/productionTrend';
import type { ProductionWorkItem } from '@/prosthetics/types';

const NOW = new Date(2026, 8, 10, 12, 0, 0);

const base = (overrides: Partial<ProductionWorkItem> = {}): ProductionWorkItem => ({
  instanceId: 'i1',
  orderId: 'o1',
  patientId: '900001',
  patientPib: 'ПІБ',
  prosthetistUserId: 5,
  prosthetistFullName: 'Протезист',
  orderNumber: 'MIS-900001-55',
  productCode: '06 24 09',
  productType: 'LOWER_LIMB',
  prosthesisType: null,
  prescriptionDate: null,
  templateName: null,
  currentStageName: null,
  currentStepName: null,
  status: 'IN_PROGRESS',
  startTime: null,
  endTime: null,
  lastActivityAt: null,
  createdAt: '2026-09-10T08:00:00',
  updatedAt: null,
  elapsedSeconds: 0,
  activeSeconds: 0,
  idleSeconds: 0,
  expectedActiveSeconds: null,
  activeDeviationSeconds: null,
  brakCount: 0,
  reworkCount: 0,
  failed: false,
  attentionFlags: [],
  ...overrides,
});

describe('bucketTrend', () => {
  it('returns empty buckets for no items', () => {
    const points = bucketTrend([], 7, NOW);
    expect(points).toHaveLength(7);
    expect(points.every((p) => p.created + p.completed + p.failed === 0)).toBe(true);
    expect(points.at(-1)?.date).toBe('2026-09-10');
    expect(points[0]?.date).toBe('2026-09-04');
  });

  it('maps created/completed/failed by day', () => {
    const points = bucketTrend(
      [
        base({ instanceId: 'a', startTime: '2026-09-10T08:00:00' }),
        base({
          instanceId: 'b',
          status: 'COMPLETED',
          startTime: '2026-09-08T08:00:00',
          endTime: '2026-09-09T08:00:00',
        }),
        base({
          instanceId: 'c',
          status: 'FAILED',
          startTime: '2026-09-09T08:00:00',
          endTime: '2026-09-09T18:00:00',
        }),
        base({
          instanceId: 'd',
          status: 'BRANCHED',
          startTime: '2026-09-01T08:00:00',
          endTime: '2026-09-09T18:00:00',
        }),
      ],
      7,
      NOW,
    );
    const byDate = Object.fromEntries(points.map((p) => [p.date, p]));
    expect(byDate['2026-09-10'].created).toBe(1);
    expect(byDate['2026-09-08'].created).toBe(1);
    expect(byDate['2026-09-09'].completed).toBe(1);
    expect(byDate['2026-09-09'].failed).toBe(2);
    expect(byDate['2026-09-04'].created).toBe(0);
  });

  it('ignores terminal rows without endTime and out-of-range dates', () => {
    const points = bucketTrend(
      [
        base({ instanceId: 'a', status: 'COMPLETED', endTime: null }),
        base({ instanceId: 'b', status: 'FAILED', endTime: null }),
        base({
          instanceId: 'c',
          status: 'COMPLETED',
          startTime: '2026-01-01T08:00:00',
          endTime: '2026-01-02T08:00:00',
        }),
        base({
          instanceId: 'd',
          status: 'IN_PROGRESS',
          startTime: '2026-09-20T08:00:00',
          createdAt: '2026-09-20T08:00:00',
        }),
      ],
      7,
      NOW,
    );
    // Terminal mapping is ignored (created still counts for in-range rows).
    expect(points.every((p) => p.completed + p.failed === 0)).toBe(true);
  });

  it('falls back to createdAt when startTime is missing', () => {
    const points = bucketTrend(
      [base({ startTime: null, createdAt: '2026-09-10T08:00:00' })],
      7,
      NOW,
    );
    expect(points.at(-1)?.created).toBe(1);
  });

  it('labels buckets in uk-UA', () => {
    const points = bucketTrend([], 1, NOW);
    expect(points).toHaveLength(1);
    expect(points[0]?.label).toMatch(/10/);
  });
});
