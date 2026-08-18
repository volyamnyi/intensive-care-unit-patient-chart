import client from './client';
import type {
  Episode, ClinicalDay, HourlyRecord, MedicalOrder,
  OrderExecution, MedicalNote, ClinicalScale, ScaleResult, FluidBalanceItem,
  PdfResponse, SignRequest, SignResponse, ReopenRequest, DepartmentStats, DepartmentPatient,
  EpisodeCreateRequest, EpisodePatchRequest, EpisodeCloseRequest,
  ClinicalDayCreateRequest, ClinicalDayPatchRequest,
  HourlyRecordCreateRequest,
  MedicalOrderCreateRequest,
  OrderExecutionCreateRequest,
  OrderExecutionPlanRequest,
  OrderExecutionFinishRequest,
  MedicalNoteCreateRequest,
  ScaleResultCreateRequest,
  LabResult, LabResultCreateRequest,
  VentilationSettings, VentilationCreateRequest,
  PatientStateAssessment, PatientStateCreateRequest,
} from '../types/icu';

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
  plan: (orderId: string, data: OrderExecutionPlanRequest) =>
    client.put<OrderExecution>(`/orders/${orderId}/plan`, data),
  planFinish: (orderId: string, data: OrderExecutionFinishRequest) =>
    client.put<OrderExecution>(`/orders/${orderId}/plan/finish`, data),
  cancel: (orderId: string, data: OrderExecutionFinishRequest) =>
    client.put<OrderExecution>(`/orders/${orderId}/cancel`, data),
  execute: (orderId: string, data: OrderExecutionCreateRequest) =>
    client.post<OrderExecution>(`/orders/${orderId}/execute`, data),
  executeFinish: (orderId: string, data: OrderExecutionFinishRequest) =>
    client.post<OrderExecution>(`/orders/${orderId}/execute/finish`, data),
  update: (id: string, data: { actualDose?: string; comment?: string; version: number }) =>
    client.patch<void>(`/executions/${id}`, data),
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
  getResultsByEpisode: (episodeId: string) =>
    client.get<ScaleResult[]>(`/episodes/${episodeId}/scales`),
  createEpisodeResult: (episodeId: string, data: ScaleResultCreateRequest) =>
    client.post<ScaleResult>(`/episodes/${episodeId}/scales`, data),
  calculateAndSave: (episodeId: string, scaleId: string, rawData: Record<string, unknown>, clinicalDayId?: string) =>
    client.post<ScaleResult>(`/episodes/${episodeId}/scales/calculate?scaleId=${scaleId}${clinicalDayId ? `&clinicalDayId=${clinicalDayId}` : ''}`, rawData),
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
