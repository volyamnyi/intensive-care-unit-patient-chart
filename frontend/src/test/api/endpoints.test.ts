import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authApi, patientApi, userApi, auditApi } from '../../api/platform';
import { episodeApi, clinicalDayApi, hourlyRecordApi, medicalOrderApi, orderExecutionApi, medicalNoteApi, clinicalScaleApi, fluidBalanceApi, pdfApi } from '../../api/icu';
import { prescriptionApi, vitalSignApi } from '../../api/medication';

const { mockClient } = vi.hoisted(() => ({
  mockClient: {
    post: vi.fn(),
    get: vi.fn(),
    patch: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
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
    episodeApi.create({ patientId: 1001, hospitalizationId: '2001', departmentId: '3001', admissionDate: '2024-01-01T00:00:00Z' });
    expect(mockClient.post).toHaveBeenCalledWith('/episodes', { patientId: 1001, hospitalizationId: '2001', departmentId: '3001', admissionDate: '2024-01-01T00:00:00Z' });
  });
});

describe('clinicalDayApi', () => {
  it('getById calls /clinical-days/:id', () => {
    clinicalDayApi.getById('cd-1');
    expect(mockClient.get).toHaveBeenCalledWith('/clinical-days/cd-1');
  });

  it('signNurse posts to /clinical-days/:id/sign/nurse', () => {
    clinicalDayApi.signNurse('cd-1', { userId: 1 });
    expect(mockClient.post).toHaveBeenCalledWith('/clinical-days/cd-1/sign/nurse', { userId: 1 });
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
  it('getByOrder gets /orders/:id/executions', () => {
    orderExecutionApi.getByOrder('ord-1');
    expect(mockClient.get).toHaveBeenCalledWith('/orders/ord-1/executions');
  });

  it('plan puts to /orders/:id/plan', () => {
    orderExecutionApi.plan('ord-1', { hour: 10, dose: '5мг' });
    expect(mockClient.put).toHaveBeenCalledWith('/orders/ord-1/plan', { hour: 10, dose: '5мг' });
  });

  it('planFinish puts to /orders/:id/plan/finish', () => {
    orderExecutionApi.planFinish('ord-1', { hour: 10 });
    expect(mockClient.put).toHaveBeenCalledWith('/orders/ord-1/plan/finish', { hour: 10 });
  });

  it('cancel puts to /orders/:id/cancel', () => {
    orderExecutionApi.cancel('ord-1', { hour: 10 });
    expect(mockClient.put).toHaveBeenCalledWith('/orders/ord-1/cancel', { hour: 10 });
  });

  it('execute posts to /orders/:id/execute', () => {
    orderExecutionApi.execute('ord-1', { hour: 10, actualDose: '5мг', comment: 'ок' });
    expect(mockClient.post).toHaveBeenCalledWith('/orders/ord-1/execute', { hour: 10, actualDose: '5мг', comment: 'ок' });
  });

  it('executeFinish posts to /orders/:id/execute/finish', () => {
    orderExecutionApi.executeFinish('ord-1', { hour: 10 });
    expect(mockClient.post).toHaveBeenCalledWith('/orders/ord-1/execute/finish', { hour: 10 });
  });

  it('update patches to /executions/:id', () => {
    orderExecutionApi.update('ex-1', { actualDose: '6мг', version: 2 });
    expect(mockClient.patch).toHaveBeenCalledWith('/executions/ex-1', { actualDose: '6мг', version: 2 });
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

describe('prescriptionApi', () => {
  it('getByPatient calls /prescriptions with patientId', () => {
    prescriptionApi.getByPatient(1001);
    expect(mockClient.get).toHaveBeenCalledWith('/prescriptions', { params: { patientId: 1001 } });
  });

  it('create posts to /prescriptions', () => {
    prescriptionApi.create({ patientId: '1001' });
    expect(mockClient.post).toHaveBeenCalledWith('/prescriptions', { patientId: '1001' });
  });

  it('getItems calls /prescriptions/:id/items', () => {
    prescriptionApi.getItems('list-1');
    expect(mockClient.get).toHaveBeenCalledWith('/prescriptions/list-1/items');
  });

  it('addItem posts to /prescriptions/:id/items', () => {
    prescriptionApi.addItem('list-1', { medicineName: 'Penicillin' });
    expect(mockClient.post).toHaveBeenCalledWith('/prescriptions/list-1/items', { medicineName: 'Penicillin' });
  });

  it('removeItem deletes /prescriptions/items/:id', () => {
    prescriptionApi.removeItem('item-1');
    expect(mockClient.delete).toHaveBeenCalledWith('/prescriptions/items/item-1');
  });

  it('close posts to /prescriptions/:id/close', () => {
    prescriptionApi.close('list-1');
    expect(mockClient.post).toHaveBeenCalledWith('/prescriptions/list-1/close');
  });

  it('planDose puts to /prescriptions/day-parts/:id/plan', () => {
    prescriptionApi.planDose('part-1', '10mg');
    expect(mockClient.put).toHaveBeenCalledWith('/prescriptions/day-parts/part-1/plan', { dose: '10mg' });
  });

  it('completeDose puts to /prescriptions/day-parts/:id/complete', () => {
    prescriptionApi.completeDose('part-1');
    expect(mockClient.put).toHaveBeenCalledWith('/prescriptions/day-parts/part-1/complete');
  });

  it('executeDose posts to /prescriptions/day-parts/:id/execute', () => {
    prescriptionApi.executeDose('part-1', { actualDose: '5mg', secondPersonLogin: 'nurse2', secondPersonPassword: 'nurse123' });
    expect(mockClient.post).toHaveBeenCalledWith('/prescriptions/day-parts/part-1/execute', { actualDose: '5mg', secondPersonLogin: 'nurse2', secondPersonPassword: 'nurse123' });
  });

  it('getAllergies calls /prescriptions/allergies', () => {
    prescriptionApi.getAllergies(1001);
    expect(mockClient.get).toHaveBeenCalledWith('/prescriptions/allergies', { params: { patientId: 1001 } });
  });

  it('getMedicineCatalog calls /prescriptions/medicine-catalog', () => {
    prescriptionApi.getMedicineCatalog('Penicillin');
    expect(mockClient.get).toHaveBeenCalledWith('/prescriptions/medicine-catalog', { params: { keyword: 'Penicillin' }, signal: undefined });
  });
});

describe('vitalSignApi', () => {
  it('getByPrescriptionList calls /vital-signs', () => {
    vitalSignApi.getByPrescriptionList('list-1');
    expect(mockClient.get).toHaveBeenCalledWith('/vital-signs', { params: { prescriptionListId: 'list-1' } });
  });

  it('create posts to /vital-signs', () => {
    vitalSignApi.create({ pulse: 80 });
    expect(mockClient.post).toHaveBeenCalledWith('/vital-signs', { pulse: 80 });
  });
});
