import { describe, it, expect } from 'vitest';
import { CRITICAL_RANGES, isCritical, countCriticalByHour, countCriticalTotal, pluralCritical } from '../../components/monitoring/criticalRanges';
import type { HourlyRecord } from '../../types/icu';

const rec = (overrides: Partial<HourlyRecord> = {}): HourlyRecord => ({
  id: 'r1',
  clinicalDayId: 'day-1',
  recordTime: '2025-06-01T08:00:00Z',
  consciousness: null,
  gcs: null,
  temperature: null,
  heartRate: null,
  respiratoryRate: null,
  systolicBP: null,
  diastolicBP: null,
  meanArterialPressure: null,
  spo2: null,
  etco2: null,
  fio2: null,
  cvp: null,
  dopamine: null,
  dobutamine: null,
  norepinephrine: null,
  epinephrine: null,
  urineOutput: null,
  drainOutput: null,
  gastricOutput: null,
  stool: null,
  vomit: null,
  bedPosition: null,
  headEnd: null,
  painScore: null,
  notes: null,
  createdBy: 1,
  createdAt: '',
  updatedBy: 1,
  updatedAt: '',
  version: 1,
  ...overrides,
});

describe('criticalRanges', () => {
  it('covers the full glance layer per ТЗ (8 vital params)', () => {
    const keys = Object.keys(CRITICAL_RANGES).sort();
    expect(keys).toEqual([
      'cvp', 'diastolicBP', 'gcs', 'heartRate',
      'respiratoryRate', 'spo2', 'systolicBP', 'temperature',
    ].sort());
  });

  it('flags values outside ranges, ignores empty and unparsable input', () => {
    expect(isCritical('heartRate', '131')).toBe(true);
    expect(isCritical('heartRate', '49')).toBe(true);
    expect(isCritical('heartRate', '130')).toBe(false);
    expect(isCritical('heartRate', '50')).toBe(false);
    expect(isCritical('heartRate', '90')).toBe(false);
    expect(isCritical('spo2', '89')).toBe(true);
    expect(isCritical('temperature', '41')).toBe(true);
    expect(isCritical('gcs', '14')).toBe(false);
    expect(isCritical('heartRate', '')).toBe(false);
    expect(isCritical('heartRate', 'abc')).toBe(false);
    expect(isCritical('unknown', '130')).toBe(false);
  });

  it('counts violations per hour and in total across all hours', () => {
    const map = new Map<number, HourlyRecord>([
      [8, rec({ heartRate: 131 })],
      [9, rec({ heartRate: 131, spo2: 89 })],
      [10, rec({ heartRate: 80 })],
    ]);
    const byHour = countCriticalByHour(map);
    expect(byHour.get(8)).toBe(1);
    expect(byHour.get(9)).toBe(2);
    expect(byHour.get(10)).toBeUndefined();
    expect(byHour.size).toBe(2);
    expect(countCriticalTotal(map)).toBe(3);
    expect(countCriticalTotal(new Map())).toBe(0);
  });

  it('pluralizes critical-value labels for 1, 2-4 and 5+', () => {
    expect(pluralCritical(1)).toBe('критичне значення');
    expect(pluralCritical(2)).toBe('критичні значення');
    expect(pluralCritical(4)).toBe('критичні значення');
    expect(pluralCritical(5)).toBe('критичних значень');
    expect(pluralCritical(21)).toBe('критичне значення');
    expect(pluralCritical(12)).toBe('критичних значень');
  });
});
