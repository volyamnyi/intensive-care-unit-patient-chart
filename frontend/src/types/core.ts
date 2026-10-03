export interface User {
  id: number;
  login: string;
  fullName: string;
  role: 'DOCTOR' | 'NURSE' | 'HEAD_OF_DEPARTMENT' | 'ADMINISTRATOR' | 'AUDITOR' | 'PROSTHETIST' | 'PROSTHETICS_ADMINISTRATOR' | 'ADJACENT_SPECIALIST';
  email: string;
  specialityCode: string;
  specialityName: string;
  phone: string;
  app: 'icu' | 'prescriptions' | 'prosthetics' | null;
  deleted?: boolean;
}

export interface PermissionDef {
  code: string;
  label: string;
  description: string;
  category: string;
}

export interface PermissionMatrix {
  roles: string[];
  permissions: PermissionDef[];
  grants: Record<string, string[]>;
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

/** Audit v2 console search row (no payload, no diff values). */
export interface AuditEventSummary {
  auditId: string;
  occurredAt: string;
  eventClass: string;
  module: string;
  functionalArea: string;
  action: string;
  actionType: string;
  criticality: string;
  actorType: string;
  actorLogin: string | null;
  targetType: string | null;
  targetId: string | null;
  outcome: string;
  correlationId: string | null;
  userActionId: string | null;
  parentAuditId: string | null;
}

export interface AuditChange {
  field: string;
  type: string;
  dataClass: string;
  oldValue: unknown;
  newValue: unknown;
  valuesRedacted: boolean;
}

export interface AuditTargetRef {
  relationType: string;
  entityType: string;
  entityId: string;
  businessKey: string | null;
}

/** Full Audit v2 event card with relations, children and integrity state. */
export interface AuditEventDetail {
  auditId: string;
  occurredAt: string;
  recordedAt: string;
  eventClass: string;
  criticality: string;
  module: string;
  functionalArea: string;
  action: string;
  actionType: string;
  actorType: string;
  actorId: string | null;
  actorLogin: string | null;
  actorDisplayName: string | null;
  actorRoles: string[];
  targetType: string | null;
  targetId: string | null;
  businessKey: string | null;
  outcome: string;
  errorCode: string | null;
  reasonCode: string | null;
  requestId: string | null;
  userActionId: string | null;
  correlationId: string | null;
  parentAuditId: string | null;
  source: string | null;
  httpMethod: string | null;
  routeTemplate: string | null;
  ipAddress: string | null;
  durationMs: number | null;
  affectedRecords: number | null;
  changes: AuditChange[];
  targets: AuditTargetRef[];
  children: AuditEventSummary[];
  externalCalls: Record<string, string>[];
  metadata: Record<string, unknown>;
  integrityHash: string | null;
  integrityVerified: boolean;
  restrictedDetail: boolean;
}

/** Chronological object history, oldest first, root/child via parentAuditId. */
export interface AuditObjectHistory {
  entityType: string;
  entityId: string;
  eventCount: number;
  events: AuditEventSummary[];
}

export interface AuditEventSearchParams {
  actorLogin?: string;
  module?: string;
  functionalArea?: string;
  action?: string;
  targetType?: string;
  targetId?: string;
  outcome?: string;
  actorType?: string;
  criticality?: string;
  correlationId?: string;
  requestId?: string;
  userActionId?: string;
  occurredFrom?: string;
  occurredTo?: string;
  page?: number;
  size?: number;
}

/** MIS spiPatientProsthesCheck — exact 13-field contract (no card numbers, no height/weight). */
export interface PatientDto {
  id: number;
  fullName: string;
  birthDate: string;
  sexCode: string;
  address: string;
  phone: string;
  email: string;
  bloodGroup: string;
  rhFactor: string;
  departmentId?: number;
  room?: string;
  bed?: string;
  doctorName?: string;
  /** MIS stay state (MOV/CMP/CNC/REJ/…, absent when unknown). */
  patientStatus?: string | null;
}
