import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { authApi, patientApi, icuCardApi, icuDayApi, prescriptionApi, userApi } from '../api/endpoints';

vi.mock('../api/client', () => {
  const mockAxios = {
    post: vi.fn(),
    get: vi.fn(),
    put: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  };
  return { default: mockAxios };
});

import client from '../api/client';
const mockClient = client as unknown as ReturnType<typeof vi.fn>;

describe('authApi', () => {
  it('login posts to /auth/login', () => {
    authApi.login({ login: 'doctor1', password: 'doctor123' });
    expect(client.post).toHaveBeenCalledWith('/auth/login', {
      login: 'doctor1',
      password: 'doctor123',
    });
  });
});

describe('patientApi', () => {
  it('search calls GET /patients/search with params', () => {
    patientApi.search('Петренко', undefined, undefined, undefined);
    expect(client.get).toHaveBeenCalledWith('/patients/search', {
      params: { name: 'Петренко', phone: undefined, externalId: undefined },
      signal: undefined,
    });
  });

  it('getById calls GET /patients/:id', () => {
    patientApi.getById(1);
    expect(client.get).toHaveBeenCalledWith('/patients/1');
  });
});

describe('icuCardApi', () => {
  it('create posts to /icu-cards', () => {
    icuCardApi.create({ diagnosis: 'Sepsis' });
    expect(client.post).toHaveBeenCalledWith('/icu-cards', { diagnosis: 'Sepsis' });
  });

  it('getActive calls GET /icu-cards/active', () => {
    icuCardApi.getActive();
    expect(client.get).toHaveBeenCalledWith('/icu-cards/active');
  });
});

describe('icuDayApi', () => {
  it('saveVitals PUTs to /icu-days/:id/vitals/:hour', () => {
    icuDayApi.saveVitals(1, 8, { heartRate: 72 });
    expect(client.put).toHaveBeenCalledWith('/icu-days/1/vitals/8', { heartRate: 72 });
  });

  it('getBalance GETs /icu-days/:id/balance', () => {
    icuDayApi.getBalance(1);
    expect(client.get).toHaveBeenCalledWith('/icu-days/1/balance');
  });

  it('signOff POSTs /icu-days/:id/sign-off', () => {
    icuDayApi.signOff(1);
    expect(client.post).toHaveBeenCalledWith('/icu-days/1/sign-off');
  });
});

describe('prescriptionApi', () => {
  it('execute POSTs to /prescriptions/:id/execute', () => {
    prescriptionApi.execute(1, 5, 10, 180);
    expect(client.post).toHaveBeenCalledWith('/prescriptions/1/execute', {
      dayId: 5,
      hour: 10,
      actualVolume: 180,
    });
  });
});

describe('userApi', () => {
  it('getMe GETs /users/me', () => {
    userApi.getMe();
    expect(client.get).toHaveBeenCalledWith('/users/me');
  });

  it('getDoctors GETs /users/doctors', () => {
    userApi.getDoctors();
    expect(client.get).toHaveBeenCalledWith('/users/doctors');
  });
});
