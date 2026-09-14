import type { PatientDto } from '../../types/core';
import type { PrescriptionList } from '../../types/medication';

/** MIS stay states with dedicated Ukrainian labels (MIS priority). */
export const MIS_STATUS_LABELS: Record<string, string> = {
  MOV: 'Переведено',
  CMP: 'Виписано',
  CNC: 'Скасовано',
  REJ: 'Відхилено',
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

/** Row highlight shared by roster and pool tables. */
export function getPatientRowClasses(lists: PrescriptionList[]): string {
  if (lists.length === 0) {
    return 'bg-yellow-100 dark:bg-yellow-900/30';
  }
  if (lists.every(l => l.status === 'Finished')) {
    return 'bg-muted/50';
  }
  return '';
}
