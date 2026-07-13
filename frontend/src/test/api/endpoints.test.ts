import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  authApi, patientApi, episodeApi, clinicalDayApi, hourlyRecordApi,
  medicalOrderApi, orderExecutionApi, medicalNoteApi, clinicalScaleApi,
  fluidBalanceApi, pdfApi, userApi, auditApi,
} from '../../api/endpoints';

const { mockClient } = vi.hoisted(() => ({
  mockClient: {
    post: vi.fn(),
    get: vi.fn(),
    patch: vi.fn(),
  },
}));

vi.mock('../../api/client', () => ({ default: mockClient }));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('authApi', () => {
  it('login posts to /auth/login', () => {
    authApi.login({ login: 'doctor1', password: 'pass' });
    expect(mockClient.post).toHaveBeenCalledWith('/auth/login', { login: 'doctor1', password: 'pass' });
  });
});

describe('patientApi', () => {
  it('search calls /patients with query param', () => {
    patientApi.search('Іван');
    expect(mockClient.get).toHaveBeenCalledWith('/patients', {
      params: { query: 'Іван' },
      signal: undefined,
    });
  });

  it('getById calls /patients/:id', () => {
    patientApi.getById('1001');
    expect(mockClient.get).toHaveBeenCalledWith('/patients/1001');
  });
});

describe('episodeApi', () => {
  it('search calls /episodes with params', () => {
    episodeApi.search({ status: 'ACTIVE' });
    expect(mockClient.get).toHaveBeenCalledWith('/episodes', { params: { status: 'ACTIVE' } });
  });

  it('getById calls /episodes/:id', () => {
    episodeApi.getById('ep-1');
    expect(mockClient.get).toHaveBeenCalledWith('/episodes/ep-1');
  });

  it('create posts to /episodes', () => {
    episodeApi.create({ patientId: '1001', hospitalizationId: '2001', departmentId: '3001' });
    expect(mockClient.post).toHaveBeenCalledWith('/episodes', { patientId: '1001', hospitalizationId: '2001', departmentId: '3001' });
  });
});

describe('clinicalDayApi', () => {
  it('getById calls /clinical-days/:id', () => {
    clinicalDayApi.getById('cd-1');
    expect(mockClient.get).toHaveBeenCalledWith('/clinical-days/cd-1');
  });

  it('signNurse posts to /clinical-days/:id/sign/nurse', () => {
    clinicalDayApi.signNurse('cd-1', { password: 'pass' });
    expect(mockClient.post).toHaveBeenCalledWith('/clinical-days/cd-1/sign/nurse', { password: 'pass' });
  });
});

describe('hourlyRecordApi', () => {
  it('getByClinicalDay calls /clinical-days/:id/hourly-records', () => {
    hourlyRecordApi.getByClinicalDay('cd-1');
    expect(mockClient.get).toHaveBeenCalledWith('/clinical-days/cd-1/hourly-records');
  });
});

describe('medicalOrderApi', () => {
  it('cancel posts to /orders/:id/cancel', () => {
    medicalOrderApi.cancel('ord-1', { version: 1 });
    expect(mockClient.post).toHaveBeenCalledWith('/orders/ord-1/cancel', { version: 1 });
  });
});

describe('orderExecutionApi', () => {
  it('create posts to /orders/:id/execute', () => {
    orderExecutionApi.create('ord-1', { orderId: 'ord-1' });
    expect(mockClient.post).toHaveBeenCalledWith('/orders/ord-1/execute', { orderId: 'ord-1' });
  });
});

describe('medicalNoteApi', () => {
  it('getByClinicalDay calls /clinical-days/:id/notes', () => {
    medicalNoteApi.getByClinicalDay('cd-1');
    expect(mockClient.get).toHaveBeenCalledWith('/clinical-days/cd-1/notes');
  });
});

describe('clinicalScaleApi', () => {
  it('getAvailable calls /scales', () => {
    clinicalScaleApi.getAvailable();
    expect(mockClient.get).toHaveBeenCalledWith('/scales');
  });
});

describe('fluidBalanceApi', () => {
  it('getByClinicalDay calls /clinical-days/:id/fluid-balance', () => {
    fluidBalanceApi.getByClinicalDay('cd-1');
    expect(mockClient.get).toHaveBeenCalledWith('/clinical-days/cd-1/fluid-balance');
  });
});

describe('pdfApi', () => {
  it('generate posts to /clinical-days/:id/pdf', () => {
    pdfApi.generate('cd-1');
    expect(mockClient.post).toHaveBeenCalledWith('/clinical-days/cd-1/pdf');
  });
});

describe('userApi', () => {
  it('getMe calls /users/me', () => {
    userApi.getMe();
    expect(mockClient.get).toHaveBeenCalledWith('/users/me');
  });

  it('getDoctors calls /users/doctors', () => {
    userApi.getDoctors();
    expect(mockClient.get).toHaveBeenCalledWith('/users/doctors');
  });

  it('getNurses calls /users/nurses', () => {
    userApi.getNurses();
    expect(mockClient.get).toHaveBeenCalledWith('/users/nurses');
  });
});

describe('auditApi', () => {
  it('list calls /audit with params', () => {
    auditApi.list({ page: 0, size: 10 });
    expect(mockClient.get).toHaveBeenCalledWith('/audit', { params: { page: 0, size: 10 } });
  });
});
