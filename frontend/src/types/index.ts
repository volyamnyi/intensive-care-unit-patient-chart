export interface User {
  id: number;
  login: string;
  fullName: string;
  role: 'DOCTOR' | 'NURSE' | 'HEAD_OF_DEPARTMENT' | 'ADMINISTRATOR';
  email: string;
  specialityCode: string;
  specialityName: string;
  phone: string;
}

export interface Patient {
  patientID: number;
  patientName: string;
  patientBirthDate: string;
  patientSexCode: string;
  patientAddress: string;
  patientPhone: string;
  patientEmail: string;
  patientExternalID1: string;
  patientExternalID2: string;
  patientHeight: number;
  patientWeight: number;
  bloodGroup: string;
  rhFactor: string;
}

export interface IcuCard {
  id: number;
  patientId: number;
  patientName: string;
  medicalCardNumber: string;
  admissionDate: string;
  diagnosis: string;
  apacheIi: number;
  sofa: number;
  status: 'ACTIVE' | 'CLOSED';
  createdBy: string;
  createdAt: string;
  icuDays: IcuDay[];
  prescriptions: Prescription[];
}

export interface IcuDay {
  id: number;
  dayNumber: number;
  date: string;
  status: 'ACTIVE' | 'SIGNED' | 'ARCHIVED';
  doctorId: number;
  signedAt: string | null;
  pdfUrl: string | null;
  escalationSent: boolean;
}

export interface HourlyVital {
  id: number;
  hour: number;
  systolicBp: number | null;
  diastolicBp: number | null;
  heartRate: number | null;
  spo2: number | null;
  temperature: number | null;
  cvp: number | null;
  respiratoryRate: number | null;
  ventilatorMode: string | null;
  tidalVolume: number | null;
  minuteVentilation: number | null;
  peep: number | null;
  fio2: number | null;
  ventFrequency: number | null;
}

export interface Prescription {
  id: number;
  medication: string;
  dose: string;
  route: string;
  frequency: string;
  startHour: number;
  endHour: number;
  startDate: string;
  endDate: string;
  doctorId: number;
  status: 'ACTIVE' | 'STOPPED' | 'EXPIRED';
  createdAt: string;
}

export interface FluidIntake {
  id: number;
  hour: number;
  medicationName: string;
  volumeOrdered: number;
  volumeActual: number;
  prescriptionId: number;
  status: 'PENDING' | 'DONE' | 'SKIPPED';
}

export interface FluidOutput {
  id: number;
  hour: number;
  type: 'URINE' | 'TUBE' | 'DRAINAGE' | 'STOOL';
  volume: number;
  isPresent: boolean;
}

export interface FluidBalance {
  icuDayId: number;
  totalIntake: number;
  totalOutput: number;
  dailyBalance: number;
  cumulativeBalance: number;
}

export interface ScaleAssessment {
  id: number;
  scaleType: 'APACHE_II' | 'SOFA' | 'RASS' | 'CAM_ICU' | 'BRADEN';
  score: number;
  subScoresJson: string;
  assessedAt: string;
  assessedBy: string;
  hour: number;
}

export interface LoginRequest {
  login: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  login: string;
  fullName: string;
  role: string;
  email: string;
}
