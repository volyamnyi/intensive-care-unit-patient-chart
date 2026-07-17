import type {
  Episode, ClinicalDay, HourlyRecord, HourlyRecordCreateRequest,
  MedicalOrder, MedicalOrderCreateRequest, MedicalNote, ScaleResult,
  ClinicalScale, FluidBalanceItem, LabResult, VentilationSettings, PatientStateAssessment,
} from '../../types';
import type { LabResultCreateData } from '../common/LabResultsPanel';
import type { VentilationFormData } from '../common/VentilationPanel';
import type { PatientStateFormData } from '../common/PatientStatePanel';

export interface DashboardUser {
  id: number;
  role?: string;
}

export interface DashboardProps {
  user?: DashboardUser | null;
  labResults: LabResult[];
  ventilationSettings: VentilationSettings[];
  patientStateAssessments: PatientStateAssessment[];
  episode: Episode;
  clinicalDays: ClinicalDay[];
  selectedDay: ClinicalDay | null;
  onSelectDay: (day: ClinicalDay) => void;
  records: HourlyRecord[];
  orders: MedicalOrder[];
  notes: MedicalNote[];
  scaleResults: ScaleResult[];
  availableScales: ClinicalScale[];
  balanceItems: FluidBalanceItem[];
  currentHour: number;
  onSetCurrentHour: (h: number) => void;
  vitalForm: HourlyRecordCreateRequest;
  onVitalFormChange: (v: HourlyRecordCreateRequest) => void;
  onSaveVitals: () => void;
  autoSaving: boolean;
  onCreateOrder: (order: MedicalOrderCreateRequest) => void;
  onCancelOrder: (orderId: string) => void;
  onExecuteOrder?: (orderId: string) => void;
  onCreateNote: (text: string, noteType: string) => void;
  onCreateScaleResult: (scaleId: string, result: string) => void;
  onRecalculateBalance: () => void;
  onCreateLabResult?: (data: LabResultCreateData) => void;
  onSaveVentilation?: (data: VentilationFormData) => void;
  onSavePatientState?: (data: PatientStateFormData) => void;
  isLocked: boolean;
  isNurse: boolean;
  isDoctor: boolean;
}
