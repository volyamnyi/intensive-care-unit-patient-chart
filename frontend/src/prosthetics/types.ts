export type FlowInstanceStatus =
  | 'NEW'
  | 'IN_PROGRESS'
  | 'PAUSED'
  | 'BLOCKED_PATIENT'
  | 'BLOCKED_MATERIAL'
  | 'WAITING_REVIEW'
  | 'CORRECTION'
  | 'FAILED_QC'
  | 'COMPLETED'
  | 'FAILED';

export type TemplateStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export type GateDecision = 'PASS' | 'REWORK' | 'FAIL';

export type PauseCategory = 'PATIENT' | 'MATERIAL' | 'TECH_IDLE';

export interface ProstheticsPatient {
  id: string;
  fullName: string;
  birthDate: string;
  sexCode: string;
}

export interface ProstheticsOrder {
  id: string;
  patientId: string;
  orderNumber: string;
  productType: string;
  amputationLevel: string;
  limbSide: string;
  status: string;
  createdAt: string;
}

export interface ReworkLoop {
  targetStepId: string;
  reworkType: string;
  maxAttempts: number;
}

export interface QualityGate {
  id: string;
  name: string;
  description: string;
  requiredApproverRole: string;
  checklist: string;
  attachmentsRequired: boolean;
  reworkLoops: ReworkLoop[];
}

export interface TemplateElement {
  id: string;
  elementType: string;
  label: string;
  required: boolean;
  unit: string | null;
  minValue: number | null;
  maxValue: number | null;
  minCount: number | null;
  maxCount: number | null;
  regexPattern: string | null;
  options: string[] | null;
  mimeTypes: string[] | null;
  maxSizeMb: number | null;
}

export interface TemplateStep {
  id: string;
  name: string;
  stepType: string;
  mandatory: boolean;
  allowBackward: boolean;
  autoStartTimer: boolean;
  normDurationMin: number | null;
  elements: TemplateElement[];
}

export interface TemplateStage {
  id: string;
  name: string;
  stageType: string;
  canSkip: boolean;
  requiresApproval: boolean;
  gate: QualityGate | null;
  steps: TemplateStep[];
}

export interface FlowTemplate {
  id: string;
  name: string;
  description: string;
  templateVersion: number;
  productType: string;
  amputationLevel: string;
  limbSide: string;
  status: TemplateStatus;
  estimatedDurationMin: number;
  createdAt: string;
  updatedAt: string;
  stages: TemplateStage[];
}

export interface FlowInstance {
  id: string;
  templateId: string;
  patientId: string;
  orderId: string;
  assignedUserId: number | null;
  status: FlowInstanceStatus;
  currentStageId: string | null;
  currentStepId: string | null;
  startTime: string | null;
  endTime: string | null;
  totalActiveSeconds: number | null;
  totalIdleSeconds: number | null;
  reworkCount: number | null;
  failReason: string | null;
  pausedAt: string | null;
  resumedAt: string | null;
  pauseCategory: PauseCategory | null;
  createdAt: string;
  updatedAt: string;
}

export interface StepExecution {
  id: string;
  instanceId: string;
  stageId: string;
  stepId: string;
  attemptNumber: number;
  status: string;
  startedAt: string | null;
  completedAt: string | null;
  activeSeconds: number | null;
  values: string | null;
  completedBy: number | null;
}

export interface InstanceCreateRequest {
  orderId: string;
  templateId: string;
}

export interface StepCompleteRequest {
  values: string;
  resources?: ResourceUsageRequest[];
}

export interface ResourceUsageRequest {
  resourceId: string;
  quantity: number;
}

export interface GateDecisionRequest {
  decision: GateDecision;
  comment?: string;
}

export interface PauseRequest {
  pauseCategory: PauseCategory;
  note?: string;
}

export interface FailRequest {
  category: string;
  description: string;
  snapshot: string;
}

export interface EvidenceFile {
  id: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  checksum: string;
}

export interface ProstheticsPdf {
  id: string;
  fileName: string;
  fileVersion: number;
  generatedAt: string;
  generatedBy: number;
  checksum: string | null;
  totalActiveSeconds: number | null;
  totalIdleSeconds: number | null;
  status: FlowInstanceStatus;
}

export interface TemplateCreateRequest {
  name: string;
  description?: string;
  productType: string;
  amputationLevel: string;
  limbSide: string;
  stages: TemplateStageCreateRequest[];
}

export interface TemplateStageCreateRequest {
  name: string;
  stageType: string;
  canSkip: boolean;
  requiresApproval: boolean;
  gate: GateCreateRequest | null;
  steps: TemplateStepCreateRequest[];
}

export interface GateCreateRequest {
  name: string;
  description: string;
  requiredApproverRole: string;
  checklist: string;
  attachmentsRequired: boolean;
  reworkLoops: ReworkLoopCreateRequest[];
}

export interface ReworkLoopCreateRequest {
  targetStepId: string;
  reworkType: string;
  maxAttempts: number;
}

export interface TemplateStepCreateRequest {
  name: string;
  stepType: string;
  mandatory: boolean;
  allowBackward: boolean;
  autoStartTimer: boolean;
  normDurationMin: number | null;
  elements: TemplateElementCreateRequest[];
}

export interface TemplateElementCreateRequest {
  elementType: string;
  label: string;
  required: boolean;
  unit?: string;
  minValue?: number;
  maxValue?: number;
  minCount?: number;
  maxCount?: number;
  regexPattern?: string;
  options?: string[];
  mimeTypes?: string[];
  maxSizeMb?: number;
}

export interface TemplatePatchRequest {
  name?: string;
  description?: string;
  productType?: string;
  amputationLevel?: string;
  limbSide?: string;
  status?: TemplateStatus;
  version?: number;
}

export type WizardStep = 'patient' | 'order' | 'template' | 'review';

export interface ProstheticsDraft {
  patientId: string | null;
  orderId: string | null;
  templateId: string | null;
}
