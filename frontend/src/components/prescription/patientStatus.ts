import type { PatientDto } from '../../types/core';
import type { PrescriptionList } from '../../types/medication';

/** MIS stay states with dedicated Ukrainian labels (MIS priority, issue #341).
 * Full 12-code dictionary confirmed by the MIS owner; PRG ('In Progress')
 * is the live active code ('В ході'). */
export const MIS_STATUS_LABELS: Record<string, string> = {
  PRG: 'В ході',
  PLN: 'Заплановано',
  RES: 'Зарезервовано',
  RET: 'Повернено',
  REP: 'В ремонті',
  AWY: 'Відсутній',
  MOV: 'Переведено',
  CMP: 'Виписано',
  CLS: 'Закрито',
  CNC: 'Скасовано',
  REJ: 'Відхилено',
  DED: 'Померлий',
};

/**
 * Roster row badge with MIS priority: a reported stay state always wins;
 * unknown codes surface verbatim (never hide a patient behind a code we
 * have not mapped yet); absent status falls back to the list-derived badge
 * (rows are patients under treatment, so an empty row is still «В ході»).
 */
export function getPatientStatusText(
  patientStatus: PatientDto['patientStatus'],
  lists: PrescriptionList[],
): string {
  if (patientStatus !== null && patientStatus !== undefined && patientStatus !== '') {
    return MIS_STATUS_LABELS[patientStatus] ?? patientStatus;
  }
  if (lists.length === 0) {
    return 'В ході';
  }
  if (lists.some(l => l.status !== 'Finished')) {
    return 'В ході';
  }
  return 'Завершено';
}

/**
 * Row tint is a pure function of the MIS stay state (issue #342) — never of
 * the prescription lists. Every known code owns a subtle pastel tint + left
 * accent bar (light and dark variants, Tailwind tokens only, no raw oklch);
 * the status text in the row cell is always the primary carrier (colour is
 * never the sole signal). Unknown/absent codes render untinted (fail-open).
 */
const MIS_STATUS_ROW_CLASSES: Record<string, string> = {
  PRG: 'bg-emerald-50 dark:bg-emerald-950/30 border-l-4 border-l-emerald-500',
  PLN: 'bg-sky-50 dark:bg-sky-950/30 border-l-4 border-l-sky-400',
  RES: 'bg-indigo-50 dark:bg-indigo-950/30 border-l-4 border-l-indigo-400',
  RET: 'bg-amber-50 dark:bg-amber-950/30 border-l-4 border-l-amber-400',
  REP: 'bg-orange-50 dark:bg-orange-950/30 border-l-4 border-l-orange-400',
  AWY: 'bg-yellow-50 dark:bg-yellow-950/30 border-l-4 border-l-yellow-400',
  MOV: 'bg-cyan-50 dark:bg-cyan-950/30 border-l-4 border-l-cyan-500',
  CMP: 'bg-slate-100 dark:bg-slate-800/40',
  CLS: 'bg-muted/50',
  CNC: 'bg-red-50 dark:bg-red-950/20 border-l-4 border-l-destructive',
  REJ: 'bg-rose-50 dark:bg-rose-950/30 border-l-4 border-l-rose-500',
  DED: 'bg-zinc-200 dark:bg-zinc-800',
};

/** Row highlight shared by roster and pool tables. */
export function getPatientRowClasses(
  patientStatus: PatientDto['patientStatus'],
): string {
  if (patientStatus === null || patientStatus === undefined || patientStatus === '') {
    return '';
  }
  return MIS_STATUS_ROW_CLASSES[patientStatus] ?? '';
}
