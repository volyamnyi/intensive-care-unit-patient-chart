export interface User {
  id: number;
  login: string;
  fullName: string;
  role: 'DOCTOR' | 'NURSE' | 'HEAD_OF_DEPARTMENT' | 'ADMINISTRATOR' | 'AUDITOR';
  email: string;
  specialityCode: string;
  specialityName: string;
  phone: string;
  permissions: string;
  app: 'icu' | 'prescriptions' | null;
  deleted?: boolean;
}

export interface PatientDto {
  id: number;
  fullName: string;
  birthDate: string;
  sexCode: string;
  address: string;
  phone: string;
  email: string;
  externalId1: string;
  externalId2: string;
  height: number | null;
  weight: number | null;
  bloodGroup: string;
  rhFactor: string;
  departmentId?: number;
  room?: string;
  bed?: string;
  doctorName?: string;
}

export interface EpisodeCreateRequest {
  patientId: number;
  hospitalizationId?: string;
  departmentId?: string;
  admissionDate: string;
  heightCm?: number;
  ward?: string;
  bedNumber?: string;
  admissionDiagnosis?: string;
  attendingDoctorId?: number;
}

export interface EpisodePatchRequest {
  hospitalizationId?: string;
  departmentId?: string;
  dischargeDate?: string;
  heightCm?: number;
  ward?: string;
  bedNumber?: string;
  admissionDiagnosis?: string;
  attendingDoctorId?: number;
  version: number;
}

export interface EpisodeCloseRequest {
  dischargeDate: string;
  version: number;
}

export interface ClinicalDayCreateRequest {
  episodeId: string;
  startDateTime: string;
  endDateTime: string;
  weightKg?: number;
}

export interface ClinicalDayPatchRequest {
  endDateTime?: string;
  weightKg?: number;
  version: number;
}

export interface HourlyRecordCreateRequest {
  recordTime: string;
  consciousness?: string;
  temperature?: number;
  heartRate?: number;
  respiratoryRate?: number;
  systolicBP?: number;
  diastolicBP?: number;
  meanArterialPressure?: number;
  spo2?: number;
  etco2?: number;
  fio2?: number;
  cvp?: number;
  urineOutput?: number;
  drainOutput?: number;
  stool?: string;
  vomit?: string;
  painScore?: number;
  notes?: string;
}

export interface MedicalOrderCreateRequest {
  category: string;
  drugName: string;
  dose: string;
  unit: string;
  route: string;
  frequency: string;
  startTime: string;
  endTime?: string;
}

export interface OrderExecutionCreateRequest {
  executedBy: number;
  executedAt: string;
  actualDose: string;
  comment?: string;
}

export interface MedicalNoteCreateRequest {
  noteType: string;
  text: string;
}

export interface ScaleResultCreateRequest {
  scaleId: string;
  result: string;
}

export interface LabResult {
  id: string;
  clinicalDayId: string;
  testCode: string;
  testName: string;
  result: string;
  unit: string;
  referenceMin: number | null;
  referenceMax: number | null;
  isAbnormal: boolean;
  measuredAt: string;
  createdAt: string;
  version: number;
}

export interface LabResultCreateRequest {
  testCode: string;
  testName: string;
  result: string;
  unit: string;
  referenceMin: number | null;
  referenceMax: number | null;
  measuredAt: string;
}

export interface VentilationSettings {
  id: string;
  clinicalDayId: string;
  recordHour: number;
  mode: string;
  fio2: number | null;
  peep: number | null;
  tidalVolume: number | null;
  minuteVolume: number | null;
  pinsp: number | null;
  psupport: number | null;
  triggerType: string;
  ieRatio: string;
  respiratoryRate: number | null;
  plateauPressure: number | null;
  meanAirwayPressure: number | null;
  version: number;
}

export interface PatientStateAssessment {
  id: string;
  clinicalDayId: string;
  recordHour: number;
  consciousness: string;
  skin: string;
  edema: string;
  mucousMembranes: string;
  peripheralCirculation: string;
  bowelSounds: string;
  generalCondition: string;
  additionalNotes: string;
  version: number;
}

export interface PatientStateCreateRequest {
  recordHour: number;
  consciousness?: string;
  skin?: string;
  edema?: string;
  mucousMembranes?: string;
  peripheralCirculation?: string;
  bowelSounds?: string;
  generalCondition?: string;
  additionalNotes?: string;
}

export interface VentilationCreateRequest {
  recordHour: number;
  mode?: string;
  fio2?: number;
  peep?: number;
  tidalVolume?: number;
  minuteVolume?: number;
  pinsp?: number;
  psupport?: number;
  triggerType?: string;
  ieRatio?: string;
  respiratoryRate?: number;
  plateauPressure?: number;
  meanAirwayPressure?: number;
}

export interface SignRequest {
  userId: number;
  hash?: string;
}

export interface SignResponse {
  signatureId: string;
  clinicalDayId: string;
  role: string;
  signedAt: string;
  hash: string | null;
  version: number;
}

export interface ReopenRequest {
  reason: string;
  version: number;
}

export interface Episode {
  id: string;
  patientId: number;
  patientName: string | null;
  hospitalizationId: string | null;
  departmentId: string | null;
  admissionDate: string;
  dischargeDate: string | null;
  status: 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'ARCHIVED';
  heightCm: number | null;
  ward: string | null;
  bedNumber: string | null;
  admissionDiagnosis: string | null;
  attendingDoctorId: number | null;
  createdBy: number;
  createdAt: string;
  updatedBy: number;
  updatedAt: string;
  version: number;
}

export interface ClinicalDay {
  id: string;
  episodeId: string;
  dayNumber: number;
  startDateTime: string;
  endDateTime: string;
  status: 'OPEN' | 'NURSE_SIGNED' | 'DOCTOR_SIGNED' | 'CLOSED' | 'REOPENED';
  doctorSigned: boolean | null;
  nurseSigned: boolean | null;
  closedAt: string | null;
  weightKg: number | null;
  bmi: number | null;
  createdBy: number;
  createdAt: string;
  updatedBy: number;
  updatedAt: string;
  version: number;
}

export interface HourlyRecord {
  id: string;
  clinicalDayId: string;
  recordTime: string;
  consciousness: string | null;
  temperature: number | null;
  heartRate: number | null;
  respiratoryRate: number | null;
  systolicBP: number | null;
  diastolicBP: number | null;
  meanArterialPressure: number | null;
  spo2: number | null;
  etco2: number | null;
  fio2: number | null;
  cvp: number | null;
  urineOutput: number | null;
  drainOutput: number | null;
  stool: string | null;
  vomit: string | null;
  painScore: number | null;
  notes: string | null;
  createdBy: number;
  createdAt: string;
  updatedBy: number;
  updatedAt: string;
  version: number;
}

export interface MedicalOrder {
  id: string;
  clinicalDayId: string;
  category: string;
  drugName: string;
  dose: string;
  unit: string;
  route: string;
  frequency: string;
  startTime: string;
  endTime: string | null;
  status: 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  createdBy: number;
  createdAt: string;
  updatedBy: number;
  updatedAt: string;
  version: number;
}

export interface OrderExecution {
  id: string;
  orderId: string;
  executedBy: number;
  executedAt: string;
  actualDose: string;
  status: 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'PARTIALLY_COMPLETED' | 'CANCELLED';
  comment: string | null;
  createdBy: number;
  createdAt: string;
  updatedBy: number;
  updatedAt: string;
  version: number;
}

export interface MedicalNote {
  id: string;
  clinicalDayId: string;
  authorId: number;
  role: string;
  noteType: string;
  text: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface ClinicalScale {
  id: string;
  name: string;
  description: string | null;
  isAutomatic: boolean | null;
  status: string;
  createdBy: number;
  createdAt: string;
  updatedBy: number;
  updatedAt: string;
  version: number;
}

export interface ScaleResult {
  id: string;
  clinicalDayId: string;
  scaleId: string;
  scaleName: string;
  result: string;
  calculatedAt: string;
  calculatedBy: number;
  createdAt: string;
  version: number;
}

export interface FluidBalanceItem {
  id: string;
  clinicalDayId: string;
  hour: number;
  intake: number | null;
  output: number | null;
  balance: number | null;
  cumulativeBalance: number | null;
  version: number;
  intakeByCategory?: Record<string, number>;
  outputByCategory?: Record<string, number>;
}

export interface Signature {
  id: string;
  clinicalDayId: string;
  userId: number;
  role: string;
  signedAt: string;
  hash: string | null;
  status: string;
  createdBy: number;
  createdAt: string;
  updatedBy: number;
  updatedAt: string;
  version: number;
}

export type TransferStatus = 'PENDING' | 'SENT' | 'FAILED';

export interface PdfResponse {
  id: string;
  clinicalDayId: string;
  fileName: string;
  fileVersion: number;
  generatedAt: string;
  generatedBy: number;
  checksum: string | null;
  transferStatus: TransferStatus | null;
  transferredAt: string | null;
  transferError: string | null;
}

export interface AuditLog {
  id: string;
  timestamp: string;
  userId: number | null;
  entity: string;
  entityId: string | null;
  action: string;
  oldValue: string | null;
  newValue: string | null;
  correlationId: string | null;
  ipAddress: string | null;
  userRole: string | null;
}

export interface LoginRequest {
  login: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  login: string;
  fullName: string;
  role: string;
  email: string;
  permissions: string;
}

export interface DepartmentStats {
  activePatients: number;
  openDays: number;
  nurseSignedDays: number;
  doctorSignedDays: number;
  closedDays: number;
  totalBeds: number;
  occupiedBeds: number;
  activeDoctors: number;
  activeNurses: number;
}

export interface DepartmentPatient {
  id: string;
  patientId: number;
  patientName: string | null;
  hospitalizationId: string | null;
  departmentId: string | null;
  admissionDate: string;
  dischargeDate: string | null;
  status: string;
  attendingDoctorId: number | null;
  attendingDoctorName: string | null;
  ward: string | null;
  bedNumber: string | null;
  admissionDiagnosis: string | null;
  latestDayStatus: string | null;
  latestDayNumber: number | null;
  daysSinceAdmission: number;
}

export interface PageResponse<T> {
  content: T[];
  pageable?: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

export type PrescriptionListStatus = 'Active' | 'Saved' | 'Finished';

export interface PrescriptionList {
  id: string;
  patientId: number;
  hospitalizationId: string | null;
  departmentId: string | null;
  documentName: string;
  status: PrescriptionListStatus;
  editingUserId: string | null;
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

