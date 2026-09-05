export type FlowInstanceStatus =
  | 'NEW'
  | 'IN_PROGRESS'
  | 'PAUSED'
  | 'BLOCKED_PATIENT'
  | 'BLOCKED_MATERIAL'
  | 'COMPLETED'
  | 'FAILED'
  | 'BRANCHED';

export type TemplateStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export type PauseCategory =
  | 'OPERATIVE_INTERVENTION'
  | 'VLC_PASSING'
  | 'WENT_ABROAD'
  | 'REAMPUTATION';

export interface ProstheticsPatient {
  id: string;
  pib: string;
  birthDate: string;
  gender: string;
}

export interface ProstheticsOrder {
  id: string;
  patientId: string;
  orderNumber: string;
  productType: string;
  amputationLevel: string;
  limbSide: string;
  status: string;
  materials: string | null;
  createdAt: string;
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
  options: string | null;
  mimeTypes: string | null;
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
  type: string;
  canSkip: boolean;
  requiresApproval: boolean;
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
  currentExecutionId: string | null;
  templateName: string | null;
  patientPib: string | null;
  orderNumber: string | null;
  currentStageName: string | null;
  currentStepName: string | null;
  startTime: string | null;
  endTime: string | null;
  totalActiveSeconds: number | null;
  totalIdleSeconds: number | null;
  failReason: string | null;
  pausedAt: string | null;
  resumedAt: string | null;
  pauseCategory: PauseCategory | null;
  parentInstanceId?: string | null;
  branchSequence?: number | null;
  originStageId?: string | null;
  originStepId?: string | null;
  /** Values captured in previously-completed steps, keyed by stepId (JSON string). */
  priorStepValues?: Record<string, string> | null;
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
  note: string | null;
  completedBy: number | null;
}

export interface ResourceUsageResponse {
  id: string;
  stepExecutionId: string | null;
  stepId: string | null;
  stepName: string | null;
  material: string;
  qty: number | null;
  unit: string | null;
  minutes: number | null;
  recordedBy: number | null;
  createdAt: string | null;
}

export interface FailureSnapshot {
  id: string;
  instanceId: string;
  category: string;
  description: string | null;
  snapshot: string | null;
  createdBy: number | null;
  createdAt: string | null;
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
  material: string;
  quantity: number | null;
  unit: string | null;
  minutes: number | null;
}

export interface PauseRequest {
  category: PauseCategory;
}

export interface FailRequest {
  category: string;
  description: string;
  snapshot?: string;
}

export interface EvidenceFile {
  id: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  checksum: string;
  stepExecutionId?: string | null;
  createdAt?: string | null;
}

export interface StepNotePatchRequest {
  note?: string | null;
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
  type: string;
  canSkip: boolean;
  requiresApproval: boolean;
  steps: TemplateStepCreateRequest[];
}

export interface TemplateStepCreateRequest {
  name: string;
  description?: string;
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
  placeholder?: string;
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

export type WizardStep = 'select-patient' | 'select-order' | 'review-order' | 'select-template';

export interface ProstheticsDraft {
  patientId: string | null;
  orderId: string | null;
  templateId: string | null;
  instanceId: string | null;
}

// Phase 5 types
export type ProcessStatus = FlowInstanceStatus;

export interface TemplateElementField {
  id: string;
  elementType: string;
  label: string;
  placeholder?: string;
  required: boolean;
  unit?: string | null;
  minValue?: number | null;
  maxValue?: number | null;
  minCount?: number | null;
  maxCount?: number | null;
  regexPattern?: string | null;
  options?: string[] | null;
  hint?: string;
}

// Immutable template snapshot attached to a flow instance (GET /snapshot).
export interface SnapshotTemplate {
  name: string;
  version: number;
  productType: string;
  amputationLevel: string | null;
  limbSide: string | null;
  estimatedDurationMin: number | null;
  stages: SnapshotStage[];
}

export interface SnapshotStage {
  id: string;
  name: string;
  stageType: string;
  canSkip: boolean;
  requiresApproval: boolean;
  steps: SnapshotStep[];
}

export interface SnapshotStep {
  id: string;
  name: string;
  stepType: string;
  mandatory: boolean;
  allowBackward: boolean;
  autoStartTimer: boolean;
  normDurationMin: number | null;
  elements: SnapshotElement[];
}

export interface SnapshotElement {
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

export interface BrakCreateRequest {
  returnStageId: string;
  softTissueMisalignment: boolean;
  painDiscomfort: boolean;
  note?: string | null;
}

export interface BrakEvent {
  id: string;
  instanceId: string;
  stageId: string;
  stepId: string;
  softTissueMisalignment: boolean;
  painDiscomfort: boolean;
  note: string | null;
  returnStageId: string;
  returnStageName?: string | null;
  newInstanceId: string | null;
  createdBy: number | null;
  createdAt: string | null;
}

export interface BranchResponse {
  brakEventId: string;
  originalInstanceId: string;
  newInstanceId: string;
  returnStageId: string;
  returnStageName: string | null;
  newStatus: FlowInstanceStatus;
}

export const ALLOWED_RETURN_STAGE_IDS = [
  'd0000012-0000-0000-0000-000000000012',
  'd0000013-0000-0000-0000-000000000013',
  'd0000014-0000-0000-0000-000000000014',
] as const;

export const ALLOWED_RETURN_STAGE_LABELS: Record<string, string> = {
  'd0000012-0000-0000-0000-000000000012': 'Виготовлення гіпсового негатива',
  'd0000013-0000-0000-0000-000000000013': 'Виготовлення гіпсової моделі кукси',
  'd0000014-0000-0000-0000-000000000014': 'Виготовлення тренувальної гільзи',
};
