export type PrescriptionListStatus = 'Active' | 'Saved' | 'Finished';

export interface PrescriptionList {
  id: string;
  patientId: number;
  hospitalizationId: string | null;
  departmentId: string | null;
  documentName: string;
  status: PrescriptionListStatus;
  editingUserId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PrescriptionListCreateRequest {
  patientId: string;
}

export type PrescriptionItemStatus = 'Active';

export interface PrescriptionItem {
  id: string;
  listId: string;
  medicineName: string;
  medicineMethod: string;
  regime: string;
  /** ATC code captured from the MIS catalog at add-time; null for legacy rows. */
  medicineAtcCode: string | null;
  status: PrescriptionItemStatus;
  sortOrder: number;
  dayParts?: PrescriptionDayPart[];
}

export interface PrescriptionItemAddRequest {
  medicineName: string;
  medicineMethod?: string;
  regime?: string;
  /** ATC code of the selected catalog item (issue #304). */
  medicineAtcCode?: string | null;
}

export interface PrescriptionDayPart {
  id: string;
  dayId?: string;
  dayDate?: string;
  period: string;
  dose: string | null;
  isPlanned: boolean;
  isPlannedFinished: boolean;
  isCompleted: boolean;
  isCompletedFinished: boolean;
  doctorName: string | null;
  nurseName: string | null;
}

export interface PrescriptionExecutionCreateRequest {
  actualDose: string;
  secondPersonLogin: string;
  secondPersonPassword: string;
}

/** Batch metadata for the Form №003-4/о PDF download (Phase 17, no PII). */
export interface PrescriptionPdfInfo {
  pages: number;
  fileName: string;
}

export interface MedicineCatalogItem {
  id: number;
  name: string;
  categoryRef: number | null;
  ptgCode: string | null;
  isHighRisk: boolean | null;
  /** From MIS `itemKindAtc`: the ATC code, used for the interaction warnings (#304). */
  itemKindAtc?: string | null;
  /** From MIS `itemKindIsDisabled`: the item exists in the MIS catalog but is
   *  not orderable. Rendered non-selectable (visible, dimmed, no-op) never hidden. */
  itemKindIsDisabled?: boolean | null;
}

/** Server-computed drug-interaction warnings for a prescription list (issue #304). */
export interface PrescriptionInteractionsResponse {
  warnings: ItemInteractionWarning[];
  missingAtc: { present: boolean; names: string[] } | null;
}

export interface ItemInteractionWarning {
  itemId: string;
  nameUk: string;
  interactions: PairInteractionWarning[];
}

export interface PairInteractionWarning {
  otherItemId: string;
  otherNameUk: string;
  severity: 'medium' | 'high' | 'critical';
  interactionText: string;
  overlapStart: string;
  overlapEnd: string;
  interactionIds: string[];
}

/** Report of an administrator drug-interactions dataset import (#304). */
export interface DrugInteractionImportReport {
  drugs: number;
  interactions: number;
  skipped: number;
  skippedDetails: string[];
  durationMs: number;
  sourceHash: string;
}

export interface VitalSignEntry {
  id: string;
  dayId: string;
  period: string;
  temperature: number | null;
  systolicBp: number | null;
  diastolicBp: number | null;
  spo2: number | null;
  pulse: number | null;
  stool: string | null;
  painScore: number | null;
}

export interface VitalSignDay {
  id: string;
  vitalListId: string;
  dayDate: string;
}

export interface VitalSignList {
  id: string;
  prescriptionListId: string;
}

export interface VitalSignEntryCreateRequest {
  prescriptionListId?: string;
  temperature?: number;
  systolicBp?: number;
  diastolicBp?: number;
  spo2?: number;
  pulse?: number;
  stool?: string;
  painScore?: number;
}

export interface VitalGridDay {
  id: string;
  dayDate: string;
  vitalListId: string;
  entries: VitalSignEntry[];
}
