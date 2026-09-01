import client from './client';
import type {
  ProstheticsPatient,
  ProstheticsOrder,
  FlowTemplate,
  FlowInstance,
  InstanceCreateRequest,
  StepCompleteRequest,
  GateDecisionRequest,
  PauseRequest,
  FailRequest,
  EvidenceFile,
  FlowInstanceStatus,
  StepExecution,
  TemplateStatus,
  TemplateCreateRequest,
  TemplatePatchRequest,
  SnapshotTemplate,
  GateDecisionResponse,
  ResourceUsageResponse,
  FailureSnapshot,
} from '../prosthetics/types';

const BASE = '/prosthesis-manufacturing';

export const prostheticsPatientApi = {
  search: (query?: string, signal?: AbortSignal) =>
    client.get<ProstheticsPatient[]>(`${BASE}/patients`, { params: { query }, signal }),
  getById: (id: string) => client.get<ProstheticsPatient>(`${BASE}/patients/${id}`),
};

export const prostheticsOrderApi = {
  list: () => client.get<ProstheticsOrder[]>(`${BASE}/orders`),
  listByPatient: (patientId: string) => client.get<ProstheticsOrder[]>(`${BASE}/orders`, { params: { patientId } }),
  getById: (id: string) => client.get<ProstheticsOrder>(`${BASE}/orders/${id}`),
  getDocument: (id: string) => client.get<Blob>(`${BASE}/orders/${id}/document`, { responseType: 'blob' }),
};

export const flowTemplateApi = {
  list: (params?: { status?: TemplateStatus; productType?: string; amputationLevel?: string; limbSide?: string }) =>
    client.get<FlowTemplate[]>(`${BASE}/templates`, { params }),
  getById: (id: string) => client.get<FlowTemplate>(`${BASE}/templates/${id}`),
  create: (data: TemplateCreateRequest) =>
    client.post<FlowTemplate>(`${BASE}/templates`, data),
  update: (id: string, data: TemplatePatchRequest) =>
    client.patch<FlowTemplate>(`${BASE}/templates/${id}`, data),
  delete: (id: string) => client.delete(`${BASE}/templates/${id}`),
};

export const flowInstanceApi = {
  list: (params?: { assignee?: number; status?: FlowInstanceStatus }) =>
    client.get<FlowInstance[]>(`${BASE}/instances`, { params }),
  getById: (id: string) => client.get<FlowInstance>(`${BASE}/instances/${id}`),
  getSnapshot: (id: string) =>
    client.get<SnapshotTemplate>(`${BASE}/instances/${id}/snapshot`),
  create: (data: InstanceCreateRequest) =>
    client.post<FlowInstance>(`${BASE}/instances`, data),
  start: (id: string) => client.post<FlowInstance>(`${BASE}/instances/${id}/start`),
  completeStep: (id: string, executionId: string, data: StepCompleteRequest) =>
    client.post<FlowInstance>(`${BASE}/instances/${id}/steps/${executionId}/complete`, data),
  backward: (id: string) => client.post<FlowInstance>(`${BASE}/instances/${id}/backward`),
  listExecutions: (id: string) =>
    client.get<StepExecution[]>(`${BASE}/instances/${id}/step-executions`),
  listGateDecisions: (id: string) =>
    client.get<GateDecisionResponse[]>(`${BASE}/instances/${id}/gate-decisions`),
  listResources: (id: string) =>
    client.get<ResourceUsageResponse[]>(`${BASE}/instances/${id}/resources`),
  getFailureSnapshot: (id: string) =>
    client.get<FailureSnapshot>(`${BASE}/instances/${id}/failure-snapshot`),
  pause: (id: string, data: PauseRequest) =>
    client.post<FlowInstance>(`${BASE}/instances/${id}/pause`, data),
  resume: (id: string) => client.post<FlowInstance>(`${BASE}/instances/${id}/resume`),
  fail: (id: string, data: FailRequest) =>
    client.post<FlowInstance>(`${BASE}/instances/${id}/fail`, data),
  replacement: (id: string) =>
    client.post<FlowInstance>(`${BASE}/instances/${id}/replacement`),
  decideGate: (id: string, gateId: string, data: GateDecisionRequest) =>
    client.post<FlowInstance>(`${BASE}/instances/${id}/gates/${gateId}/decision`, data),
  uploadEvidence: (id: string, executionId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return client.post<EvidenceFile>(
      `${BASE}/instances/${id}/evidence?executionId=${executionId}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },
  downloadEvidence: (id: string, fileId: string) =>
    client.get<Blob>(`${BASE}/instances/${id}/evidence/${fileId}`, {
      responseType: 'blob',
    }),
  generateReport: (id: string) =>
    client.get<Blob>(`${BASE}/instances/${id}/pdf`, {
      responseType: 'blob',
    }),
};

export const prostheticsApi = {
  patient: prostheticsPatientApi,
  order: prostheticsOrderApi,
  template: flowTemplateApi,
  instance: flowInstanceApi,
};
