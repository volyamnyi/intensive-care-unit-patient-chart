import type { HourlyRecord } from '../../types';

export const CRITICAL_RANGES: Partial<Record<string, { min: number; max: number }>> = {
  systolicBP: { min: 90, max: 180 },
  diastolicBP: { min: 60, max: 120 },
  heartRate: { min: 50, max: 130 },
  temperature: { min: 35.5, max: 39.5 },
  spo2: { min: 90, max: 100 },
  respiratoryRate: { min: 10, max: 30 },
  cvp: { min: 2, max: 14 },
  gcs: { min: 8, max: 15 },
};

export function isCritical(key: string, val: string): boolean {
  const range = CRITICAL_RANGES[key];
  if (!range) return false;
  if (val === '') return false;
  const num = Number(val);
  if (Number.isNaN(num)) return false;
  return num < range.min || num > range.max;
}

export function countCriticalByHour(recByHour: Map<number, HourlyRecord>): Map<number, number> {
  const out = new Map<number, number>();
  for (const [hour, rec] of recByHour) {
    let n = 0;
    for (const key of Object.keys(CRITICAL_RANGES)) {
      const v = (rec as unknown as Record<string, unknown>)[key];
      if (v !== null && v !== undefined && isCritical(key, String(v))) n += 1;
    }
    if (n > 0) out.set(hour, n);
  }
  return out;
}

export function countCriticalTotal(recByHour: Map<number, HourlyRecord>): number {
  let total = 0;
  for (const n of countCriticalByHour(recByHour).values()) total += n;
  return total;
}

export function pluralCritical(n: number): string {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return 'критичне значення';
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return 'критичні значення';
  return 'критичних значень';
}
