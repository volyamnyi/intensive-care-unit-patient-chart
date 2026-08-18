import client from './client';
import type {
  PrescriptionList, PrescriptionListCreateRequest,
  PrescriptionItem, PrescriptionItemAddRequest,
  PrescriptionDayPart, PrescriptionExecutionCreateRequest,
  MedicineCatalogItem, AllergyItem,
  VitalSignEntry, VitalSignDay, VitalSignEntryCreateRequest, VitalGridDay,
} from '../types/medication';

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
  getGrid: (prescriptionListId: string) =>
    client.get<VitalGridDay[]>(`/vital-signs/grid`, { params: { prescriptionListId } }),
  create: (data: VitalSignEntryCreateRequest) =>
    client.post<VitalSignEntry>('/vital-signs', data),
  updateEntry: (entryId: string, data: { temperature?: number; systolicBp?: number; diastolicBp?: number; spo2?: number; pulse?: number; stool?: string; painScore?: number }) =>
    client.put<VitalSignEntry>(`/vital-signs/entries/${entryId}`, data),
  updateCell: (dayId: string, period: string, data: { temperature?: number; systolicBp?: number; diastolicBp?: number; spo2?: number; pulse?: number; stool?: string; painScore?: number }) =>
    client.put<VitalSignEntry>(`/vital-signs/cells?dayId=${dayId}&period=${period}`, data),
};
