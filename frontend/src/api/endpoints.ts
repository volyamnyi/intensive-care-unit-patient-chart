import client from './client';
import type {
  User, PatientDto, Episode, ClinicalDay, HourlyRecord, MedicalOrder,
  OrderExecution, MedicalNote, ClinicalScale, ScaleResult, FluidBalanceItem,
  PdfResponse, AuditLog, LoginRequest, LoginResponse, PageResponse,
  SignRequest, SignResponse, ReopenRequest, DepartmentStats, DepartmentPatient,
  EpisodeCreateRequest, EpisodePatchRequest, EpisodeCloseRequest,
  ClinicalDayCreateRequest, ClinicalDayPatchRequest,
  HourlyRecordCreateRequest,
  MedicalOrderCreateRequest,
  OrderExecutionCreateRequest,
  MedicalNoteCreateRequest,
  ScaleResultCreateRequest,
  LabResult, LabResultCreateRequest,
  VentilationSettings, VentilationCreateRequest,
  PatientStateAssessment, PatientStateCreateRequest,
  PrescriptionList, PrescriptionListCreateRequest,
  PrescriptionItem, PrescriptionItemAddRequest,
  PrescriptionDayPart, PrescriptionExecutionCreateRequest,
  MedicineCatalogItem, AllergyItem,
  VitalSignEntry, VitalSignDay, VitalSignEntryCreateRequest,
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
  getStatus: (clinicalDayId: string) =>
    client.get<PdfResponse>(`/clinical-days/${clinicalDayId}/pdf/status`),
};

export const userApi = {
  getMe: () => client.get<User>('/users/me'),
  getDoctors: () => client.get<User[]>('/users/doctors'),
  getNurses: () => client.get<User[]>('/users/nurses'),
};

export const patientStateApi = {
  getByClinicalDay: (clinicalDayId: string) =>
    client.get<PatientStateAssessment[]>(`/clinical-days/${clinicalDayId}/patient-state`),
  create: (clinicalDayId: string, data: PatientStateCreateRequest) =>
    client.post<PatientStateAssessment>(`/clinical-days/${clinicalDayId}/patient-state`, data),
  update: (id: string, data: Partial<PatientStateCreateRequest> & { version: number }) =>
    client.patch<PatientStateAssessment>(`/patient-state/${id}`, data),
};

export const ventilationApi = {
  getByClinicalDay: (clinicalDayId: string) =>
    client.get<VentilationSettings[]>(`/clinical-days/${clinicalDayId}/ventilation`),
  create: (clinicalDayId: string, data: VentilationCreateRequest) =>
    client.post<VentilationSettings>(`/clinical-days/${clinicalDayId}/ventilation`, data),
  update: (id: string, data: Partial<VentilationCreateRequest> & { version: number }) =>
    client.patch<VentilationSettings>(`/ventilation/${id}`, data),
};

export const labResultApi = {
  getByClinicalDay: (clinicalDayId: string) =>
    client.get<LabResult[]>(`/clinical-days/${clinicalDayId}/lab-results`),
  create: (clinicalDayId: string, data: LabResultCreateRequest) =>
    client.post<LabResult>(`/clinical-days/${clinicalDayId}/lab-results`, data),
  update: (id: string, data: { result?: string; version: number }) =>
    client.patch<LabResult>(`/lab-results/${id}`, data),
};

export const departmentApi = {
  getStats: () =>
    client.get<DepartmentStats>('/department/stats'),
  getPatients: () =>
    client.get<DepartmentPatient[]>('/department/patients'),
};

export const auditApi = {
  list: (params?: { page?: number; size?: number; action?: string; dateFrom?: string; dateTo?: string }) =>
    client.get<PageResponse<AuditLog>>('/audit', { params }),
  getById: (id: string) =>
    client.get<AuditLog>(`/audit/${id}`),
};

export const adminApi = {
  getUsers: () => client.get<User[]>('/admin/users'),
  getUser: (id: number) => client.get<User>(`/admin/users/${id}`),
  updateRole: (id: number, role: string) => client.put<User>(`/admin/users/${id}/role`, { role }),
  updatePermissions: (id: number, action: 'add' | 'remove', permission: string) =>
    client.put<User>(`/admin/users/${id}/permissions`, { action, permission }),
  deleteUser: (id: number) => client.delete(`/admin/users/${id}`),
  getStats: () => client.get<Record<string, number>>('/admin/stats'),
};

export const prescriptionApi = {
  getByPatient: (patientId: number) =>
    client.get<PrescriptionList[]>('/prescriptions', { params: { patientId } }),
  getById: (id: string) =>
    client.get<PrescriptionList>(`/prescriptions/${id}`),
  create: (data: PrescriptionListCreateRequest) =>
    client.post<PrescriptionList>('/prescriptions', data),
  delete: (id: string) =>
    client.delete(`/prescriptions/${id}`),
  close: (id: string) =>
    client.post<PrescriptionList>(`/prescriptions/${id}/close`),
  getItems: (listId: string) =>
    client.get<PrescriptionItem[]>(`/prescriptions/${listId}/items`),
  addItem: (listId: string, data: PrescriptionItemAddRequest) =>
    client.post<PrescriptionItem>(`/prescriptions/${listId}/items`, data),
  removeItem: (itemId: string) =>
    client.delete(`/prescriptions/items/${itemId}`),
  planDose: (dayPartId: string, dose: string) =>
    client.put<PrescriptionDayPart>(`/prescriptions/day-parts/${dayPartId}/plan`, { dose }),
  completeDose: (dayPartId: string) =>
    client.put<PrescriptionDayPart>(`/prescriptions/day-parts/${dayPartId}/complete`),
  cancelDose: (dayPartId: string) =>
    client.put<PrescriptionDayPart>(`/prescriptions/day-parts/${dayPartId}/cancel`),
  executeDose: (dayPartId: string, data: PrescriptionExecutionCreateRequest) =>
    client.post<void>(`/prescriptions/day-parts/${dayPartId}/execute`, data),
  getAllergies: (patientId: number) =>
    client.get<AllergyItem[]>('/prescriptions/allergies', { params: { patientId } }),
  getMedicineCatalog: (keyword?: string, signal?: AbortSignal) =>
    client.get<MedicineCatalogItem[]>('/prescriptions/medicine-catalog', { params: { keyword }, signal }),
};

export const vitalSignApi = {
  getByPrescriptionList: (prescriptionListId: string) =>
    client.get<VitalSignDay[]>(`/vital-signs`, { params: { prescriptionListId } }),
  getEntries: (dayId: string) =>
    client.get<VitalSignEntry[]>(`/vital-signs/days/${dayId}/entries`),
  create: (data: VitalSignEntryCreateRequest) =>
    client.post<VitalSignEntry>('/vital-signs', data),
};

