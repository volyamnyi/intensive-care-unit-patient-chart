import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authApi, patientApi, icuCardApi, icuDayApi, prescriptionApi, userApi } from '../../api/endpoints';

const { mockClient } = vi.hoisted(() => ({
  mockClient: {
    post: vi.fn(),
    get: vi.fn(),
    put: vi.fn(),
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
  it('search calls /patients/search with params', () => {
    patientApi.search('Іван');
    expect(mockClient.get).toHaveBeenCalledWith('/patients/search', {
      params: { name: 'Іван', phone: undefined, externalId: undefined },
      signal: undefined,
    });
  });

  it('getById calls /patients/:id', () => {
    patientApi.getById(1);
    expect(mockClient.get).toHaveBeenCalledWith('/patients/1');
  });
});

describe('icuCardApi', () => {
  it('create posts to /icu-cards', () => {
    icuCardApi.create({ patientName: 'Test' });
    expect(mockClient.post).toHaveBeenCalledWith('/icu-cards', { patientName: 'Test' });
  });

  it('getById calls /icu-cards/:id', () => {
    icuCardApi.getById(1);
    expect(mockClient.get).toHaveBeenCalledWith('/icu-cards/1');
  });

  it('getActive calls /icu-cards/active', () => {
    icuCardApi.getActive();
    expect(mockClient.get).toHaveBeenCalledWith('/icu-cards/active');
  });
});

describe('icuDayApi', () => {
  it('getByCard calls /icu-days/by-card/:id', () => {
    icuDayApi.getByCard(1);
    expect(mockClient.get).toHaveBeenCalledWith('/icu-days/by-card/1');
  });

  it('saveVitals puts to /icu-days/:id/vitals/:hour', () => {
    icuDayApi.saveVitals(1, 10, { heartRate: 80 });
    expect(mockClient.put).toHaveBeenCalledWith('/icu-days/1/vitals/10', { heartRate: 80 });
  });

  it('getBalance calls /icu-days/:id/balance', () => {
    icuDayApi.getBalance(1);
    expect(mockClient.get).toHaveBeenCalledWith('/icu-days/1/balance');
  });

  it('signOff posts to /icu-days/:id/sign-off', () => {
    icuDayApi.signOff(1);
    expect(mockClient.post).toHaveBeenCalledWith('/icu-days/1/sign-off');
  });
});

describe('prescriptionApi', () => {
  it('create posts to /prescriptions/by-card/:id', () => {
    prescriptionApi.create(1, { medication: 'Saline' });
    expect(mockClient.post).toHaveBeenCalledWith('/prescriptions/by-card/1', { medication: 'Saline' });
  });

  it('execute posts to /prescriptions/:id/execute', () => {
    prescriptionApi.execute(1, 2, 10, 500);
    expect(mockClient.post).toHaveBeenCalledWith('/prescriptions/1/execute', { dayId: 2, hour: 10, actualVolume: 500 });
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
});
