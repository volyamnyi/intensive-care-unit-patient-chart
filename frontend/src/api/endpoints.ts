import client from './client';
import type {
  User, PatientDto, Episode, ClinicalDay, HourlyRecord, MedicalOrder,
  OrderExecution, MedicalNote, ClinicalScale, ScaleResult, FluidBalanceItem,
  PdfResponse, AuditLog, LoginRequest, LoginResponse,
  SignRequest, SignResponse, ReopenRequest,
  EpisodeCreateRequest, EpisodePatchRequest, EpisodeCloseRequest,
  ClinicalDayCreateRequest, ClinicalDayPatchRequest,
  HourlyRecordCreateRequest,
  MedicalOrderCreateRequest,
  OrderExecutionCreateRequest,
  MedicalNoteCreateRequest,
  ScaleResultCreateRequest,
} from '../types';

export const authApi = {
  login: (data: LoginRequest) =>
    client.post<LoginResponse>('/auth/login', data),
  logout: () =>
    client.post('/auth/logout'),
};

export const patientApi = {
  search: (query?: string, signal?: AbortSignal) =>
    client.get<PatientDto[]>('/patients', { params: { query }, signal }),
  getById: (id: string) =>
    client.get<PatientDto>(`/patients/${id}`),
};

export const episodeApi = {
  search: (params?: { patientId?: number; status?: string }) =>
    client.get<Episode[]>('/episodes', { params }),
  getById: (id: string) =>
    client.get<Episode>(`/episodes/${id}`),
  create: (data: EpisodeCreateRequest) =>
    client.post<Episode>('/episodes', data),
  update: (id: string, data: EpisodePatchRequest) =>
    client.patch<Episode>(`/episodes/${id}`, data),
  close: (id: string, data: EpisodeCloseRequest) =>
    client.post<Episode>(`/episodes/${id}/close`, data),
  getClinicalDays: (episodeId: string) =>
    client.get<ClinicalDay[]>(`/episodes/${episodeId}/clinical-days`),
};

export const clinicalDayApi = {
  getById: (id: string) =>
    client.get<ClinicalDay>(`/clinical-days/${id}`),
  create: (data: ClinicalDayCreateRequest) =>
    client.post<ClinicalDay>('/clinical-days', data),
  update: (id: string, data: ClinicalDayPatchRequest) =>
    client.patch<ClinicalDay>(`/clinical-days/${id}`, data),
  signNurse: (id: string, data: SignRequest) =>
    client.post<SignResponse>(`/clinical-days/${id}/sign/nurse`, data),
  signDoctor: (id: string, data: SignRequest) =>
    client.post<SignResponse>(`/clinical-days/${id}/sign/doctor`, data),
  reopen: (id: string, data: ReopenRequest) =>
    client.post<ClinicalDay>(`/clinical-days/${id}/reopen`, data),
};

export const hourlyRecordApi = {
  getByClinicalDay: (clinicalDayId: string) =>
    client.get<HourlyRecord[]>(`/clinical-days/${clinicalDayId}/hourly-records`),
  create: (clinicalDayId: string, data: HourlyRecordCreateRequest) =>
    client.post<HourlyRecord>(`/clinical-days/${clinicalDayId}/hourly-records`, data),
  update: (id: string, data: Partial<HourlyRecordCreateRequest> & { version: number }) =>
    client.patch<HourlyRecord>(`/hourly-records/${id}`, data),
};

export const medicalOrderApi = {
  getByClinicalDay: (clinicalDayId: string) =>
    client.get<MedicalOrder[]>(`/clinical-days/${clinicalDayId}/orders`),
  create: (clinicalDayId: string, data: MedicalOrderCreateRequest) =>
    client.post<MedicalOrder>(`/clinical-days/${clinicalDayId}/orders`, data),
  update: (id: string, data: { dose?: string; route?: string; frequency?: string; endTime?: string; version: number }) =>
    client.patch<MedicalOrder>(`/orders/${id}`, data),
  cancel: (id: string, data: { version: number }) =>
    client.post<MedicalOrder>(`/orders/${id}/cancel`, data),
};

export const orderExecutionApi = {
  getByOrder: (orderId: string) =>
    client.get<OrderExecution[]>(`/orders/${orderId}/executions`),
  create: (orderId: string, data: OrderExecutionCreateRequest) =>
    client.post<OrderExecution>(`/orders/${orderId}/execute`, data),
  update: (id: string, data: { actualDose?: string; comment?: string; version: number }) =>
    client.patch<OrderExecution>(`/executions/${id}`, data),
};

export const medicalNoteApi = {
  getByClinicalDay: (clinicalDayId: string) =>
    client.get<MedicalNote[]>(`/clinical-days/${clinicalDayId}/notes`),
  create: (clinicalDayId: string, data: MedicalNoteCreateRequest) =>
    client.post<MedicalNote>(`/clinical-days/${clinicalDayId}/notes`, data),
  update: (id: string, data: { text?: string; version: number }) =>
    client.patch<MedicalNote>(`/notes/${id}`, data),
};

export const clinicalScaleApi = {
  getAvailable: () => client.get<ClinicalScale[]>('/scales'),
  getResultsByClinicalDay: (clinicalDayId: string) =>
    client.get<ScaleResult[]>(`/clinical-days/${clinicalDayId}/scales`),
  createResult: (clinicalDayId: string, data: ScaleResultCreateRequest) =>
    client.post<ScaleResult>(`/clinical-days/${clinicalDayId}/scales`, data),
  updateResult: (id: string, data: { result?: string; version: number }) =>
    client.patch<ScaleResult>(`/scales/${id}`, data),
};

export const fluidBalanceApi = {
  getByClinicalDay: (clinicalDayId: string) =>
    client.get<FluidBalanceItem[]>(`/clinical-days/${clinicalDayId}/fluid-balance`),
  recalculate: (clinicalDayId: string) =>
    client.post<FluidBalanceItem[]>(`/clinical-days/${clinicalDayId}/fluid-balance/recalculate`),
};

export const pdfApi = {
  getByClinicalDay: (clinicalDayId: string) =>
    client.get<PdfResponse>(`/clinical-days/${clinicalDayId}/pdf`),
  generate: (clinicalDayId: string) =>
    client.post<PdfResponse>(`/clinical-days/${clinicalDayId}/pdf`),
};

export const userApi = {
  getMe: () => client.get<User>('/users/me'),
  getDoctors: () => client.get<User[]>('/users/doctors'),
  getNurses: () => client.get<User[]>('/users/nurses'),
};

export const auditApi = {
  list: (params?: { page?: number; size?: number; action?: string; dateFrom?: string; dateTo?: string }) =>
    client.get<AuditLog[]>('/audit', { params }),
  getById: (id: string) =>
    client.get<AuditLog>(`/audit/${id}`),
};


