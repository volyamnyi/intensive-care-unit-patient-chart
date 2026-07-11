// Shared environment constants and low-level API helpers.
export const FRONTEND_BASE = process.env.FRONTEND_BASE || 'http://localhost:5173';
export const BACKEND_BASE = process.env.BACKEND_BASE || 'http://localhost:8085';
export const API = `${BACKEND_BASE}/api`;
export const TOKEN_KEY = 'token';

// Seed users (see backend/src/main/resources/data.sql)
export const USERS = {
  doctor1: { login: 'doctor1', password: 'doctor123', role: 'DOCTOR', fullName: 'Олександр Мельник' },
  doctor2: { login: 'doctor2', password: 'doctor123', role: 'DOCTOR', fullName: 'Наталія Бойко' },
  nurse1: { login: 'nurse1', password: 'nurse123', role: 'NURSE', fullName: 'Олена Ткаченко' },
  nurse2: { login: 'nurse2', password: 'nurse123', role: 'NURSE', fullName: 'Марія Кравчук' },
  head1: { login: 'head1', password: 'head123', role: 'HEAD_OF_DEPARTMENT', fullName: 'Василь Гончарук' },
  admin: { login: 'admin', password: 'admin123', role: 'ADMINISTRATOR', fullName: 'Адмін Системи' },
};

// Mock MIS patients (see backend mock MIS service)
export const MIS_PATIENTS = {
  petrenko: { id: 1001, name: 'Петренко Іван Сергійович', ext: 'МК-001234' },
  kovalenko: { id: 1002, name: 'Коваленко Олена Вікторівна', ext: 'МК-005678' },
  sidorenko: { id: 1003, name: 'Сидоренко Василь Петрович', ext: 'МК-009012' },
};

export async function apiLogin(request, login, password) {
  const resp = await request.post(`${API}/auth/login`, {
    data: { login, password },
  });
  return resp;
}

export async function getToken(request, login, password) {
  const resp = await apiLogin(request, login, password);
  if (resp.status() !== 200) {
    throw new Error(`Login failed for ${login}: ${resp.status()} ${await resp.text()}`);
  }
  const body = await resp.json();
  return { token: body.token, body };
}

// Raw API call helper that attaches a bearer token.
export async function authed(request, method, path, { token, data, params, expectedStatus } = {}) {
  const resp = await request.fetch(`${API}${path}`, {
    method,
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    data,
    params,
  });
  if (expectedStatus !== undefined) {
    if (resp.status() !== expectedStatus) {
      throw new Error(`Expected ${expectedStatus} for ${method} ${path} but got ${resp.status()}: ${await resp.text()}`);
    }
  }
  return resp;
}
