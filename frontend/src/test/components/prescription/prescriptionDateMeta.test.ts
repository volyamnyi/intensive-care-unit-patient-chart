import { describe, it, expect } from 'vitest';
import {
  itemDateMeta,
  gridDateMeta,
  shouldMarkAddedCell,
} from '../../../components/prescription/prescriptionDateMeta';
import type { PrescriptionDayPart } from '../../../types/medication';

const DAY_MS = 86_400_000;

function seq(startIso: string, count: number): string[] {
  const out: string[] = [];
  const base = Date.parse(`${startIso}T00:00:00Z`);
  for (let i = 0; i < count; i++) {
    out.push(new Date(base + i * DAY_MS).toISOString().slice(0, 10));
  }
  return out;
}

function part(over: Partial<PrescriptionDayPart> = {}): PrescriptionDayPart {
  return {
    id: 'dp-x',
    dayId: 'day-x',
    dayDate: '2026-09-25',
    period: 'morning',
    dose: null,
    isPlanned: false,
    isPlannedFinished: false,
    isCompleted: false,
    isCompletedFinished: false,
    doctorName: null,
    nurseName: null,
    ...over,
  };
}

describe('itemDateMeta', () => {
  it('returns empty sets for no dates', () => {
    const meta = itemDateMeta([]);
    expect(meta.addedDates.size).toBe(0);
    expect(meta.removedGaps.size).toBe(0);
  });

  it('marks nothing for a base 21-day window', () => {
    const meta = itemDateMeta(seq('2026-09-04', 21));
    expect(meta.addedDates.size).toBe(0);
    expect(meta.removedGaps.size).toBe(0);
  });

  it('marks days beyond min+20 as added', () => {
    const meta = itemDateMeta([...seq('2026-09-04', 21), '2026-09-25', '2026-09-26']);
    expect([...meta.addedDates].sort()).toEqual(['2026-09-25', '2026-09-26']);
    expect(meta.removedGaps.size).toBe(0);
  });

  it('detects a single removed gap inside the range', () => {
    const dates = seq('2026-09-04', 21).filter((_, i) => i !== 5);
    const meta = itemDateMeta(dates);
    expect([...meta.removedGaps]).toEqual(['2026-09-09']);
    expect(meta.addedDates.size).toBe(0);
  });

  it('detects multiple gaps', () => {
    const dates = seq('2026-09-04', 21).filter((_, i) => i !== 2 && i !== 17);
    const meta = itemDateMeta(dates);
    expect([...meta.removedGaps].sort()).toEqual(['2026-09-06', '2026-09-21']);
  });

  it('single-day item has neither added nor gaps', () => {
    const meta = itemDateMeta(['2026-09-04']);
    expect(meta.addedDates.size).toBe(0);
    expect(meta.removedGaps.size).toBe(0);
  });

  it('deduplicates input for added detection', () => {
    const meta = itemDateMeta(['2026-09-25', '2026-09-04', '2026-09-25', '2026-09-04']);
    expect([...meta.addedDates]).toEqual(['2026-09-25']);
  });

  it('added detection follows a shifted min after early removals', () => {
    const meta = itemDateMeta(seq('2026-09-04', 27).slice(5));
    expect([...meta.addedDates]).toEqual(['2026-09-30']);
  });

  it('sparse dates: far date is added, interior hole is a gap', () => {
    const meta = itemDateMeta(['2026-01-05', '2026-01-07', '2026-06-15']);
    expect([...meta.addedDates]).toEqual(['2026-06-15']);
    expect(meta.removedGaps.has('2026-01-06')).toBe(true);
    expect(meta.removedGaps.has('2026-06-14')).toBe(true);
    expect(meta.removedGaps.has('2026-06-15')).toBe(false);
    expect(meta.removedGaps.has('2026-01-05')).toBe(false);
  });
});

describe('gridDateMeta', () => {
  it('marks union headers added with medicine names', () => {
    const meta = gridDateMeta([
      { id: 'a', medicineName: 'Alpha', dayDates: seq('2026-09-04', 21) },
      { id: 'b', medicineName: 'Beta', dayDates: [...seq('2026-09-04', 21), '2026-09-25'] },
    ]);
    expect(meta.headerAdded.get('2026-09-25')).toEqual(['Beta']);
    expect(meta.headerAdded.has('2026-09-04')).toBe(false);
    expect(meta.perItem.get('a')?.addedDates.size).toBe(0);
    expect(meta.perItem.get('b')?.addedDates.has('2026-09-25')).toBe(true);
  });

  it('lists every owning medicine when several rows share an added date', () => {
    const meta = gridDateMeta([
      { id: 'a', medicineName: 'Alpha', dayDates: [...seq('2026-09-04', 21), '2026-09-25'] },
      { id: 'b', medicineName: 'Beta', dayDates: [...seq('2026-09-04', 21), '2026-09-25'] },
    ]);
    expect(meta.headerAdded.get('2026-09-25')).toEqual(['Alpha', 'Beta']);
  });

  it('marks the header following a gap', () => {
    const dates = seq('2026-09-04', 21).filter((_, i) => i !== 5);
    const meta = gridDateMeta([{ id: 'a', medicineName: 'Alpha', dayDates: dates }]);
    expect(meta.headerGapAfter.has('2026-09-10')).toBe(true);
    expect(meta.headerGapAfter.has('2026-09-09')).toBe(false);
    expect(meta.headerGapAfter.size).toBe(1);
  });

  it('empty input yields empty meta', () => {
    const meta = gridDateMeta([]);
    expect(meta.perItem.size).toBe(0);
    expect(meta.headerAdded.size).toBe(0);
    expect(meta.headerGapAfter.size).toBe(0);
  });
});

describe('shouldMarkAddedCell', () => {
  const meta = itemDateMeta([...seq('2026-09-04', 21), '2026-09-25']);
  const white = part();
  const planned = part({ isPlanned: true, dose: '5mg' });
  const cancelled = part({ isPlanned: true, isPlannedFinished: true, dose: '5mg' });
  const completed = part({ isPlanned: true, isCompleted: true, dose: '5mg' });

  it('marks white cells of added dates', () => {
    expect(shouldMarkAddedCell(meta, '2026-09-25', white)).toBe(true);
  });

  it('never marks base dates', () => {
    expect(shouldMarkAddedCell(meta, '2026-09-04', white)).toBe(false);
  });

  it('status colors win over the added marker', () => {
    expect(shouldMarkAddedCell(meta, '2026-09-25', planned)).toBe(false);
    expect(shouldMarkAddedCell(meta, '2026-09-25', cancelled)).toBe(false);
    expect(shouldMarkAddedCell(meta, '2026-09-25', completed)).toBe(false);
  });

  it('is safe for missing meta or part', () => {
    expect(shouldMarkAddedCell(undefined, '2026-09-25', white)).toBe(false);
    expect(shouldMarkAddedCell(meta, '2026-09-25', undefined)).toBe(true);
  });
});
