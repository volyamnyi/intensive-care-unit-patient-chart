import client from './client';
import type {
  Patient, IcuCard, IcuDay, HourlyVital, Prescription,
  FluidIntake, FluidOutput, FluidBalance, ScaleAssessment,
  LoginRequest, LoginResponse,
} from '../types';

export const authApi = {
  login: (data: LoginRequest) =>
    client.post<LoginResponse>('/auth/login', data),
};

export const patientApi = {
  search: (name?: string, phone?: string, externalId?: string, signal?: AbortSignal) =>
    client.get<Patient[]>('/patients/search', { params: { name, phone, externalId }, signal }),
  getById: (id: number) =>
    client.get<Patient>(`/patients/${id}`),
};

export const icuCardApi = {
  create: (data: Partial<IcuCard>) =>
    client.post<IcuCard>('/icu-cards', data),
  getById: (id: number) =>
    client.get<IcuCard>(`/icu-cards/${id}`),
  getActive: () =>
    client.get<IcuCard[]>('/icu-cards/active'),
  getByPatient: (patientId: number) =>
    client.get<IcuCard[]>(`/icu-cards/by-patient/${patientId}`),
};

export const icuDayApi = {
  getByCard: (cardId: number) =>
    client.get<IcuDay[]>(`/icu-days/by-card/${cardId}`),
  getById: (id: number) =>
    client.get<IcuDay>(`/icu-days/${id}`),
  saveVitals: (dayId: number, hour: number, data: Partial<HourlyVital>) =>
    client.put<HourlyVital>(`/icu-days/${dayId}/vitals/${hour}`, data),
  getVitals: (dayId: number) =>
    client.get<HourlyVital[]>(`/icu-days/${dayId}/vitals`),
  addIntake: (dayId: number, hour: number, data: Partial<FluidIntake>) =>
    client.post(`/icu-days/${dayId}/intake/${hour}`, data),
  addOutput: (dayId: number, hour: number, data: Partial<FluidOutput>) =>
    client.post(`/icu-days/${dayId}/output/${hour}`, data),
  getBalance: (dayId: number) =>
    client.get<FluidBalance>(`/icu-days/${dayId}/balance`),
  saveScale: (dayId: number, data: Partial<ScaleAssessment>) =>
    client.post<ScaleAssessment>(`/icu-days/${dayId}/scales`, data),
  getScales: (dayId: number) =>
    client.get<ScaleAssessment[]>(`/icu-days/${dayId}/scales`),
  signOff: (dayId: number) =>
    client.post<IcuDay>(`/icu-days/${dayId}/sign-off`),
  getPdf: (dayId: number) =>
    client.get(`/icu-days/${dayId}/pdf`, { responseType: 'blob' }),
};

export const prescriptionApi = {
  create: (cardId: number, data: Partial<Prescription>) =>
    client.post<Prescription>(`/prescriptions/by-card/${cardId}`, data),
  getByCard: (cardId: number) =>
    client.get<Prescription[]>(`/prescriptions/by-card/${cardId}`),
  stop: (id: number) =>
    client.post(`/prescriptions/${id}/stop`),
  execute: (id: number, dayId: number, hour: number, actualVolume: number) =>
    client.post<FluidIntake>(`/prescriptions/${id}/execute`, { dayId, hour, actualVolume }),
};

export const userApi = {
  getMe: () => client.get<import('../types').User>('/users/me'),
  getDoctors: () => client.get<import('../types').User[]>('/users/doctors'),
  getNurses: () => client.get<import('../types').User[]>('/users/nurses'),
};
