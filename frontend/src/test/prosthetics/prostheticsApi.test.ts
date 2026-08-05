import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { FlowInstanceStatus, TemplateStatus } from '@/prosthetics/types';

const clientMock = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
}));

vi.mock('@/api/client', () => ({ default: clientMock }));

import {
  prostheticsPatientApi,
  prostheticsOrderApi,
  flowTemplateApi,
  flowInstanceApi,
} from '@/api/prosthetics';

function ok(data: unknown) {
  return Promise.resolve({ data });
}

describe('prostheticsPatientApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('searches patients with query and signal', async () => {
    clientMock.get.mockReturnValue(ok([{ id: 'p1' }]));
    const signal = new AbortController().signal;
    await prostheticsPatientApi.search('ivan', signal);
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/patients', {
      params: { query: 'ivan' },
      signal,
    });
  });

  it('searches patients without params when query is undefined', async () => {
    clientMock.get.mockReturnValue(ok([]));
    await prostheticsPatientApi.search();
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/patients', {
      params: { query: undefined },
      signal: undefined,
    });
  });

  it('gets a patient by id', async () => {
    clientMock.get.mockReturnValue(ok({ id: 'p1' }));
    await prostheticsPatientApi.getById('p1');
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/patients/p1');
  });
});

describe('prostheticsOrderApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('lists all orders', async () => {
    clientMock.get.mockReturnValue(ok([]));
    await prostheticsOrderApi.list();
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/orders');
  });

  it('lists orders by patient', async () => {
    clientMock.get.mockReturnValue(ok([]));
    await prostheticsOrderApi.listByPatient('p1');
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/orders', {
      params: { patientId: 'p1' },
    });
  });

  it('gets an order by id', async () => {
    clientMock.get.mockReturnValue(ok({ id: 'o1' }));
    await prostheticsOrderApi.getById('o1');
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/orders/o1');
  });

  it('downloads the order document as a blob', async () => {
    clientMock.get.mockReturnValue(ok(new Blob()));
    await prostheticsOrderApi.getDocument('o1');
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/orders/o1/document', {
      responseType: 'blob',
    });
  });
});

describe('flowTemplateApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('lists templates with optional filters', async () => {
    clientMock.get.mockReturnValue(ok([]));
    const params: { status: TemplateStatus; productType: string; amputationLevel: string; limbSide: string } = {
      status: 'ACTIVE',
      productType: 'протез',
      amputationLevel: 'both',
      limbSide: 'both',
    };
    await flowTemplateApi.list(params);
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/templates', { params });
  });

  it('lists templates without filters', async () => {
    clientMock.get.mockReturnValue(ok([]));
    await flowTemplateApi.list();
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/templates', { params: undefined });
  });

  it('gets a template by id', async () => {
    clientMock.get.mockReturnValue(ok({ id: 't1' }));
    await flowTemplateApi.getById('t1');
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/templates/t1');
  });

  it('creates a template with body', async () => {
    clientMock.post.mockReturnValue(ok({ id: 't1' }));
    const body = { name: 'Протез', productType: 'протез', amputationLevel: 'both', limbSide: 'both', stages: [] };
    await flowTemplateApi.create(body);
    expect(clientMock.post).toHaveBeenCalledWith('/prosthesis-manufacturing/templates', body);
  });

  it('updates a template with patch body', async () => {
    clientMock.patch.mockReturnValue(ok({ id: 't1' }));
    const body = { name: 'Нове ім\'я', version: 2 };
    await flowTemplateApi.update('t1', body);
    expect(clientMock.patch).toHaveBeenCalledWith('/prosthesis-manufacturing/templates/t1', body);
  });

  it('deletes a template', async () => {
    clientMock.delete.mockReturnValue(ok(null));
    await flowTemplateApi.delete('t1');
    expect(clientMock.delete).toHaveBeenCalledWith('/prosthesis-manufacturing/templates/t1');
  });
});

describe('flowInstanceApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('lists instances with assignee and status params', async () => {
    clientMock.get.mockReturnValue(ok([]));
    const params: { assignee: number; status: FlowInstanceStatus } = { assignee: 5, status: 'IN_PROGRESS' };
    await flowInstanceApi.list(params);
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/instances', { params });
  });

  it('lists instances without params', async () => {
    clientMock.get.mockReturnValue(ok([]));
    await flowInstanceApi.list();
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/instances', { params: undefined });
  });

  it('gets an instance by id', async () => {
    clientMock.get.mockReturnValue(ok({ id: 'i1' }));
    await flowInstanceApi.getById('i1');
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/instances/i1');
  });

  it('gets the template snapshot', async () => {
    clientMock.get.mockReturnValue(ok({ name: 'snap' }));
    await flowInstanceApi.getSnapshot('i1');
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/instances/i1/snapshot');
  });

  it('creates an instance from order and template', async () => {
    clientMock.post.mockReturnValue(ok({ id: 'i1' }));
    const body = { orderId: 'o1', templateId: 't1' };
    await flowInstanceApi.create(body);
    expect(clientMock.post).toHaveBeenCalledWith('/prosthesis-manufacturing/instances', body);
  });

  it('starts an instance', async () => {
    clientMock.post.mockReturnValue(ok({ id: 'i1' }));
    await flowInstanceApi.start('i1');
    expect(clientMock.post).toHaveBeenCalledWith('/prosthesis-manufacturing/instances/i1/start');
  });

  it('completes a step with values and resources', async () => {
    clientMock.post.mockReturnValue(ok({ id: 'i1' }));
    const body = { values: '{"a":1}', resources: [{ material: 'ПВХ', quantity: 2, unit: 'шт', minutes: null }] };
    await flowInstanceApi.completeStep('i1', 'exec-1', body);
    expect(clientMock.post).toHaveBeenCalledWith(
      '/prosthesis-manufacturing/instances/i1/steps/exec-1/complete',
      body,
    );
  });

  it('pauses an instance with category', async () => {
    clientMock.post.mockReturnValue(ok({ id: 'i1' }));
    await flowInstanceApi.pause('i1', { category: 'MATERIAL' });
    expect(clientMock.post).toHaveBeenCalledWith('/prosthesis-manufacturing/instances/i1/pause', {
      category: 'MATERIAL',
    });
  });

  it('resumes an instance', async () => {
    clientMock.post.mockReturnValue(ok({ id: 'i1' }));
    await flowInstanceApi.resume('i1');
    expect(clientMock.post).toHaveBeenCalledWith('/prosthesis-manufacturing/instances/i1/resume');
  });

  it('fails an instance with reason', async () => {
    clientMock.post.mockReturnValue(ok({ id: 'i1' }));
    const body = { category: 'MANUFACTURING', description: 'брак', snapshot: '{}' };
    await flowInstanceApi.fail('i1', body);
    expect(clientMock.post).toHaveBeenCalledWith('/prosthesis-manufacturing/instances/i1/fail', body);
  });

  it('creates a replacement instance', async () => {
    clientMock.post.mockReturnValue(ok({ id: 'i2' }));
    await flowInstanceApi.replacement('i1');
    expect(clientMock.post).toHaveBeenCalledWith('/prosthesis-manufacturing/instances/i1/replacement');
  });

  it('decides a gate', async () => {
    clientMock.post.mockReturnValue(ok({ id: 'i1' }));
    const body = { decision: 'PASS' as const, criteriaConfirmed: ['c1'] };
    await flowInstanceApi.decideGate('i1', 'g1', body);
    expect(clientMock.post).toHaveBeenCalledWith(
      '/prosthesis-manufacturing/instances/i1/gates/g1/decision',
      body,
    );
  });

  it('uploads evidence with multipart headers and FormData', async () => {
    clientMock.post.mockReturnValue(ok({ id: 'ev1' }));
    const file = new File(['x'], 'photo.jpg', { type: 'image/jpeg' });
    await flowInstanceApi.uploadEvidence('i1', 'exec-1', file);
    const [url, formData, config] = clientMock.post.mock.calls[0];
    expect(url).toBe('/prosthesis-manufacturing/instances/i1/evidence?executionId=exec-1');
    expect(formData).toBeInstanceOf(FormData);
    expect(formData.get('file')).toBe(file);
    expect(config).toEqual({ headers: { 'Content-Type': 'multipart/form-data' } });
  });

  it('downloads evidence as a blob', async () => {
    clientMock.get.mockReturnValue(ok(new Blob()));
    await flowInstanceApi.downloadEvidence('i1', 'ev1');
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/instances/i1/evidence/ev1', {
      responseType: 'blob',
    });
  });

  it('generates the pdf report as a blob', async () => {
    clientMock.get.mockReturnValue(ok(new Blob()));
    await flowInstanceApi.generateReport('i1');
    expect(clientMock.get).toHaveBeenCalledWith('/prosthesis-manufacturing/instances/i1/pdf', {
      responseType: 'blob',
    });
  });

  it('propagates errors from the client', async () => {
    const err = new Error('network');
    clientMock.get.mockRejectedValue(err);
    await expect(flowInstanceApi.list()).rejects.toBe(err);
  });
});
