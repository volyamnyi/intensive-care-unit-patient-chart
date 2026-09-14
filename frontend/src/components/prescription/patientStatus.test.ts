import { describe, it, expect } from 'vitest';
import {
  getPatientStatusText,
  MIS_STATUS_LABELS,
  getPatientRowClasses,
} from './patientStatus';
import type { PrescriptionList } from '../../types/medication';

const open = { status: 'Active' } as PrescriptionList;
const finished = { status: 'Finished' } as PrescriptionList;

describe('getPatientStatusText', () => {
  it('maps known MIS codes with priority over lists', () => {
    expect(MIS_STATUS_LABELS).toMatchObject({
      MOV: 'Переведено',
      CMP: 'Виписано',
      CNC: 'Скасовано',
      REJ: 'Відхилено',
    });
    expect(getPatientStatusText('MOV', [open])).toBe('Переведено');
    expect(getPatientStatusText('CMP', [open])).toBe('Виписано');
    expect(getPatientStatusText('CNC', [])).toBe('Скасовано');
    expect(getPatientStatusText('REJ', [finished])).toBe('Відхилено');
  });

  it('surfaces unknown codes verbatim', () => {
    expect(getPatientStatusText('XYZ', [open])).toBe('XYZ');
    expect(getPatientStatusText('XYZ', [])).toBe('XYZ');
  });

  it('falls back to list-derived badges without a status', () => {
    for (const missing of [null, undefined, ''] as const) {
      expect(getPatientStatusText(missing, [])).toBe('В ході');
      expect(getPatientStatusText(missing, [open])).toBe('В ході');
      expect(getPatientStatusText(missing, [finished])).toBe('Завершено');
    }
  });

  it('computes row highlight classes', () => {
    expect(getPatientRowClasses([])).toContain('bg-yellow-100');
    expect(getPatientRowClasses([finished])).toContain('bg-muted/50');
    expect(getPatientRowClasses([open])).toBe('');
  });
});
