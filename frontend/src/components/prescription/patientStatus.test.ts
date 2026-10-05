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
      PRG: 'В ході',
      MOV: 'Переведено',
      CMP: 'Виписано',
      CNC: 'Скасовано',
      REJ: 'Відхилено',
    });
    expect(getPatientStatusText('PRG', [])).toBe('В ході');
    expect(getPatientStatusText('PRG', [finished])).toBe('В ході');
    expect(getPatientStatusText('TREAT', [open])).toBe('В ході');
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

  it('tints rows purely by MIS status, never by lists', () => {
    // Active / planned / attention / transfer tints carry a left accent bar.
    expect(getPatientRowClasses('PRG')).toContain('bg-emerald-50');
    expect(getPatientRowClasses('PRG')).toContain('border-l-emerald-500');
    expect(getPatientRowClasses('TREAT')).toContain('bg-emerald-50');
    expect(getPatientRowClasses('PLN')).toContain('bg-sky-50');
    expect(getPatientRowClasses('AWY')).toContain('bg-yellow-50');
    expect(getPatientRowClasses('MOV')).toContain('bg-cyan-50');
    // Terminal / refusal tints.
    expect(getPatientRowClasses('CMP')).toContain('bg-slate-100');
    expect(getPatientRowClasses('CLS')).toContain('bg-muted/50');
    expect(getPatientRowClasses('CNC')).toContain('bg-red-50');
    expect(getPatientRowClasses('REJ')).toContain('bg-rose-50');
    expect(getPatientRowClasses('DED')).toContain('bg-zinc-200');
    // Every tinted status ships a dark-mode variant.
    for (const code of ['PRG', 'PLN', 'RES', 'RET', 'REP', 'AWY', 'MOV', 'CMP', 'CNC', 'REJ', 'DED']) {
      expect(getPatientRowClasses(code)).toMatch(/dark:/);
    }
    expect(getPatientRowClasses('CLS')).toContain('bg-muted/50');
    // Unknown and absent codes render untinted (fail-open).
    expect(getPatientRowClasses('XYZ')).toBe('');
    for (const missing of [null, undefined, ''] as const) {
      expect(getPatientRowClasses(missing)).toBe('');
    }
  });
});
