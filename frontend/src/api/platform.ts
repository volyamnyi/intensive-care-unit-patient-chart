import client from './client';
import type {
  User, PatientDto, LoginRequest, LoginResponse, PageResponse,
  AuditLog, PermissionMatrix,
} from '../types/core';

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

export const userApi = {
  getMe: () => client.get<User>('/users/me'),
  getMyPermissions: () => client.get<string[]>('/users/me/permissions'),
  getDoctors: () => client.get<User[]>('/users/doctors'),
  getNurses: () => client.get<User[]>('/users/nurses'),
};

export const settingsApi = {
  getByKey: (key: string) =>
    client.get<{ key: string; value: string }>(`/settings/${key}`),
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
  deleteUser: (id: number) => client.delete(`/admin/users/${id}`),
  getStats: () => client.get<Record<string, number>>('/admin/stats'),
  getPermissions: () => client.get<PermissionMatrix>('/admin/permissions'),
  updateRolePermission: (role: string, permissionCode: string, granted: boolean) =>
    client.put<PermissionMatrix>('/admin/permissions', { role, permissionCode, granted }),
};
