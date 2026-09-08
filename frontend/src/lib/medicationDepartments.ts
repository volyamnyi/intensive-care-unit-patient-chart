/**
 * Medication-sheet department mapping — the single place for the
 * surgery|rehab toggle (Phase 7, #260).
 *
 * The backend roster (`GET /api/patients?module=medication`) already returns
 * only these departments; the toggle splits the roster client-side without
 * extra calls. IDs are plan assumptions (epic blocker (b)) — only this map
 * changes when the MIS owner confirms the real IDs.
 */
export type MedicationDepartment = 'surgery' | 'rehab';

export const MEDICATION_DEPARTMENTS: Record<MedicationDepartment, number> = {
  surgery: 19,
  rehab: 37,
};

export const MEDICATION_MODULE = 'medication';
