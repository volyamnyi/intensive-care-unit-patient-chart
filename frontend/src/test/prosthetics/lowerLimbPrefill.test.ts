import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
  LOWER_LIMB_MOBILITY_OPTIONS,
  applyLowerLimbPrefill,
  buildLowerLimbPrefill,
  getLocalTodayDate,
  isEmptyWizardValue,
  normalizeGender,
  normalizeMobilityLevel,
  parseMisDocumentIdFromOrderNumber,
  readDraftMisDocumentId,
  selectMisDocumentForPrefill,
  type LowerLimbHeaderIds,
} from '@/prosthetics/lowerLimbPrefill';
import { LOWER_LIMB_ELEMENT_IDS } from '@/pages/prosthetics/process/LowerLimbMeasurementForm';
import type { MisOrderDocument } from '@/prosthetics/types';

const IDS: LowerLimbHeaderIds = {
  blankNumber: LOWER_LIMB_ELEMENT_IDS.blankNumber,
  date: LOWER_LIMB_ELEMENT_IDS.date,
  pib: LOWER_LIMB_ELEMENT_IDS.pib,
  address: LOWER_LIMB_ELEMENT_IDS.address,
  productCode: LOWER_LIMB_ELEMENT_IDS.productCode,
  productName: LOWER_LIMB_ELEMENT_IDS.productName,
  mobilityLevel: LOWER_LIMB_ELEMENT_IDS.mobilityLevel,
  gender: LOWER_LIMB_ELEMENT_IDS.gender,
  age: LOWER_LIMB_ELEMENT_IDS.age,
  height: LOWER_LIMB_ELEMENT_IDS.height,
  weight: LOWER_LIMB_ELEMENT_IDS.weight,
  notes: LOWER_LIMB_ELEMENT_IDS.notes,
};

const FULL_DOC: MisOrderDocument = {
  documentId: 681078,
  orderNumber: 'BZ-2026-0042',
  patientFullName: 'Сніжко Іван Петрович',
  patientAddress: 'м. Київ, вул. Хрещатик, 1',
  productCode: 'PR-26-0413',
  productName: 'Протез гомілки',
  mobilityLevel: '2 рівень',
  patientGender: 'Чоловіча',
  age: 45,
  height: 180,
  weight: 82,
  note: 'Первинний замір',
};

describe('lowerLimbPrefill', () => {
  describe('mobility options contract', () => {
    it('exposes exactly the 5 agreed options', () => {
      expect([...LOWER_LIMB_MOBILITY_OPTIONS]).toEqual([
        '1 рівень',
        '2 рівень',
        '3 рівень',
        '4 рівень',
        'Інший / Не вказано',
      ]);
    });
  });

  describe('getLocalTodayDate', () => {
    it('formats a local date as YYYY-MM-DD without UTC shift', () => {
      // Constructed in local time — must round-trip verbatim regardless of TZ.
      const local = new Date(2026, 8, 11, 23, 30, 0);
      expect(getLocalTodayDate(local)).toBe('2026-09-11');
      expect(getLocalTodayDate(new Date(2026, 0, 5, 0, 5, 0))).toBe('2026-01-05');
    });
  });

  describe('isEmptyWizardValue', () => {
    it.each([undefined, null, '', '   '])( 'treats %p as empty', (v) => {
      expect(isEmptyWizardValue(v)).toBe(true);
    });
    it.each(['x', '  x  ', 0, 42, false, true])( 'treats %p as filled', (v) => {
      expect(isEmptyWizardValue(v)).toBe(false);
    });
  });

  describe('normalizeMobilityLevel / normalizeGender', () => {
    it('accepts allowlisted values verbatim', () => {
      for (const option of LOWER_LIMB_MOBILITY_OPTIONS) {
        expect(normalizeMobilityLevel(option)).toBe(option);
      }
      expect(normalizeGender('Чоловіча')).toBe('Чоловіча');
      expect(normalizeGender('Жіноча')).toBe('Жіноча');
    });
    it('rejects null/empty/unknown without inventing a value', () => {
      expect(normalizeMobilityLevel(null)).toBeNull();
      expect(normalizeMobilityLevel('')).toBeNull();
      expect(normalizeMobilityLevel('  ')).toBeNull();
      expect(normalizeMobilityLevel('5 рівень')).toBeNull();
      expect(normalizeMobilityLevel(2)).toBeNull();
      expect(normalizeGender(null)).toBeNull();
      expect(normalizeGender('')).toBeNull();
      expect(normalizeGender('Невідомо')).toBeNull();
      expect(normalizeGender('MAL')).toBeNull();
    });
  });

  describe('buildLowerLimbPrefill', () => {
    it('maps all 12 fields including orderNumber → blankNumber', () => {
      const prefill = buildLowerLimbPrefill(FULL_DOC, IDS, '2026-09-11');
      expect(prefill).toEqual({
        [IDS.blankNumber]: 'BZ-2026-0042',
        [IDS.date]: '2026-09-11',
        [IDS.pib]: 'Сніжко Іван Петрович',
        [IDS.address]: 'м. Київ, вул. Хрещатик, 1',
        [IDS.productCode]: 'PR-26-0413',
        [IDS.productName]: 'Протез гомілки',
        [IDS.mobilityLevel]: '2 рівень',
        [IDS.gender]: 'Чоловіча',
        [IDS.age]: '45',
        [IDS.height]: '180',
        [IDS.weight]: '82',
        [IDS.notes]: 'Первинний замір',
      });
    });
    it('omits null/empty/unknown MIS values instead of synthesising', () => {
      const prefill = buildLowerLimbPrefill(
        {
          documentId: 1,
          mobilityLevel: 'unknown-level',
          patientGender: 'MAL',
          // Runtime null (JSON) is tolerated even though the type is optional.
          age: null,
        } as unknown as MisOrderDocument,
        IDS,
        '2026-09-11',
      );
      // date always comes from the local clock
      expect(prefill).toEqual({ [IDS.date]: '2026-09-11' });
      expect(prefill[IDS.blankNumber]).toBeUndefined();
      expect(prefill[IDS.mobilityLevel]).toBeUndefined();
      expect(prefill[IDS.gender]).toBeUndefined();
      expect(prefill[IDS.age]).toBeUndefined();
    });
  });

  describe('applyLowerLimbPrefill', () => {
    it('fills empty values and reports applied ids in header order', () => {
      const { next, applied } = applyLowerLimbPrefill({}, FULL_DOC, IDS, '2026-09-11');
      expect(Object.keys(next)).toHaveLength(12);
      expect(applied).toEqual([
        IDS.blankNumber,
        IDS.date,
        IDS.pib,
        IDS.address,
        IDS.productCode,
        IDS.productName,
        IDS.mobilityLevel,
        IDS.gender,
        IDS.age,
        IDS.height,
        IDS.weight,
        IDS.notes,
      ]);
    });
    it('never overwrites existing or user-entered values', () => {
      const existing = {
        [IDS.pib]: 'Введено вручну',
        [IDS.age]: '50',
        [IDS.date]: '2026-01-01',
      };
      const { next, applied } = applyLowerLimbPrefill(existing, FULL_DOC, IDS, '2026-09-11');
      expect(next[IDS.pib]).toBe('Введено вручну');
      expect(next[IDS.age]).toBe('50');
      expect(next[IDS.date]).toBe('2026-01-01');
      expect(next[IDS.blankNumber]).toBe('BZ-2026-0042');
      expect(applied).not.toContain(IDS.pib);
      expect(applied).not.toContain(IDS.age);
      expect(applied).not.toContain(IDS.date);
    });
    it('is idempotent: a second apply changes nothing and returns the same ref', () => {
      const first = applyLowerLimbPrefill({}, FULL_DOC, IDS, '2026-09-11');
      const second = applyLowerLimbPrefill(first.next, FULL_DOC, IDS, '2026-09-11');
      expect(second.applied).toEqual([]);
      expect(second.next).toBe(first.next);
    });
    it('does not touch diagram/bottom fields', () => {
      const { next } = applyLowerLimbPrefill(
        { [LOWER_LIMB_ELEMENT_IDS.a_r]: '24' },
        FULL_DOC,
        IDS,
        '2026-09-11',
      );
      expect(next[LOWER_LIMB_ELEMENT_IDS.a_r]).toBe('24');
      expect(next[LOWER_LIMB_ELEMENT_IDS.heelHeight]).toBeUndefined();
    });
  });

  describe('parseMisDocumentIdFromOrderNumber', () => {
    it('parses MIS-{patient}-{document} strictly', () => {
      expect(parseMisDocumentIdFromOrderNumber('MIS-900001-681078')).toBe('681078');
      expect(parseMisDocumentIdFromOrderNumber('ПВ-26-0413')).toBeNull();
      expect(parseMisDocumentIdFromOrderNumber('MIS-900001')).toBeNull();
      expect(parseMisDocumentIdFromOrderNumber('MIS-900001-abc')).toBeNull();
      expect(parseMisDocumentIdFromOrderNumber(null)).toBeNull();
      expect(parseMisDocumentIdFromOrderNumber(123)).toBeNull();
    });
  });

  describe('selectMisDocumentForPrefill', () => {
    const docs: MisOrderDocument[] = [
      { documentId: 111, patientFullName: 'Перший' },
      { documentId: 222, patientFullName: 'Другий' },
    ];
    it('prefers the draft misDocumentId (setup step 2 source of truth)', () => {
      expect(selectMisDocumentForPrefill(docs, '222', 'MIS-900001-111')?.documentId).toBe(222);
    });
    it('falls back to the order-number match', () => {
      expect(selectMisDocumentForPrefill(docs, null, 'MIS-900001-222')?.documentId).toBe(222);
    });
    it('falls back to the first document', () => {
      expect(selectMisDocumentForPrefill(docs, null, null)?.documentId).toBe(111);
    });
    it('returns null when there is nothing to prefill from', () => {
      expect(selectMisDocumentForPrefill([], '222', 'MIS-900001-222')).toBeNull();
    });
  });

  describe('readDraftMisDocumentId', () => {
    beforeEach(() => {
      sessionStorage.clear();
      localStorage.clear();
    });
    afterEach(() => {
      sessionStorage.clear();
      localStorage.clear();
      vi.unstubAllGlobals();
    });
    it('reads the picked document id with session-first precedence', () => {
      sessionStorage.setItem('prosthetics:draft', JSON.stringify({ misDocumentId: '222' }));
      localStorage.setItem('prosthetics:draft', JSON.stringify({ misDocumentId: '111' }));
      expect(readDraftMisDocumentId()).toBe('222');
    });
    it('falls back to localStorage', () => {
      localStorage.setItem('prosthetics:draft', JSON.stringify({ misDocumentId: '111' }));
      expect(readDraftMisDocumentId()).toBe('111');
    });
    it('returns null on missing/corrupt payloads', () => {
      expect(readDraftMisDocumentId()).toBeNull();
      localStorage.setItem('prosthetics:draft', 'not-json{{{');
      expect(readDraftMisDocumentId()).toBeNull();
      localStorage.setItem('prosthetics:draft', JSON.stringify({ misDocumentId: '' }));
      expect(readDraftMisDocumentId()).toBeNull();
    });
  });
});
