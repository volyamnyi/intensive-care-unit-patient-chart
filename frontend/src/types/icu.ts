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
  gcs?: number;
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
  dopamine?: number;
  dobutamine?: number;
  norepinephrine?: number;
  epinephrine?: number;
  urineOutput?: number;
  drainOutput?: number;
  gastricOutput?: number;
  stool?: string;
  vomit?: string;
  bedPosition?: string;
  headEnd?: string;
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
  hour: number;
  actualDose: string;
  comment?: string;
}

export interface OrderExecutionPlanRequest {
  hour: number;
  dose: string;
}

export interface OrderExecutionFinishRequest {
  hour: number;
}

export interface MedicalNoteCreateRequest {
  noteType: string;
  text: string;
}

export interface ScaleResultCreateRequest {
  scaleId: string;
  result: string;
  episodeId?: string;
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
  gcs: number | null;
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
  dopamine: number | null;
  dobutamine: number | null;
  norepinephrine: number | null;
  epinephrine: number | null;
  urineOutput: number | null;
  drainOutput: number | null;
  gastricOutput: number | null;
  stool: string | null;
  vomit: string | null;
  bedPosition: string | null;
  headEnd: string | null;
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
  hour: number | null;
  planned: boolean;
  plannedBy: number | null;
  plannedAt: string | null;
  plannedDose: string | null;
  plannedFinished: boolean;
  completedFinished: boolean;
  executedBy: number | null;
  executedAt: string | null;
  actualDose: string | null;
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
  clinicalDayId?: string;
  episodeId?: string;
  scaleId: string;
  scaleName: string;
  result: string;
  rawData?: string;
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
