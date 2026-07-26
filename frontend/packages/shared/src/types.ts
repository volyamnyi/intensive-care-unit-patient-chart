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
}

export interface PageResponse<T> {
  content: T[];
  pageable?: { pageNumber: number; pageSize: number };
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

export interface ErrorResponse {
  code: string;
  message: string;
  correlationId: string;
}
