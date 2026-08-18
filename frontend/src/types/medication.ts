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
  status: PrescriptionItemStatus;
  sortOrder: number;
  dayParts?: PrescriptionDayPart[];
}

export interface PrescriptionItemAddRequest {
  medicineName: string;
  medicineMethod?: string;
  regime?: string;
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

export interface MedicineCatalogItem {
  id: number;
  name: string;
  categoryRef: number | null;
  ptgCode: string | null;
  isHighRisk: boolean | null;
}

export interface AllergyItem {
  id: string;
  patientId: number;
  allergenName: string;
  sourceDocumentId: number | null;
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
