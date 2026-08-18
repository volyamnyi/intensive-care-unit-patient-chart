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
