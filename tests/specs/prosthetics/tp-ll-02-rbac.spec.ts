import { test, expect } from '@playwright/test';
import {
  PROSTH,
  API,
  login,
  headersFor,
  findTemplateByIdName,
  findOrderIdByProductType,
  createFreeLowerInstance,
  terminateInstance,
} from '../../helpers/tp-ll-02-flow';

/**
 * TP-LL-02 access-control matrix (Фаза 5).
 *
 * RBAC facts under test (PermissionCatalog defaults + @PreAuthorize):
 *  - PROSTHETICS_INSTANCE_CREATE  → PROSTHETIST, PROSTHETICS_ADMINISTRATOR only
 *  - PROSTHETICS_DASHBOARD        → PROSTHETIST, PROSTHETICS_ADMINISTRATOR (read surface)
 *  - ADMIN/AUDITOR                → AUDIT_ACCESS only (no prosthetics module perms)
 *  - NURSE                        → neither PROSTHETICS_INSTANCE_CREATE nor AUDIT_ACCESS
 *  - PROSTHETICS_ADMINISTRATOR    → canViewAllInstances() = allowAll (reads ANY instance)
 *  - regular PROSTHETIST          → owner-scoped only (someone else's instance → 404)
 */
test.describe('TP-LL-02 — Role-Based Access Control (Фаза 5)', () => {
  let p1Headers: any;
  let p2Headers: any;
  let prosthetisAdminHeaders: any;
  let adminHeaders: any;
  let nurseHeaders: any;

  test.beforeAll(async ({ request }) => {
    p1Headers = headersFor(await login(request, 'prosthetist1', 'doctor123'));
    p2Headers = headersFor(await login(request, 'prosthetist2', 'doctor123'));
    prosthetisAdminHeaders = headersFor(await login(request, 'prosthetics_admin1', 'doctor123'));
    adminHeaders = headersFor(await login(request, 'admin', 'admin123'));
    nurseHeaders = headersFor(await login(request, 'nurse1', 'nurse123'));
  });

  test('unauthenticated (invalid token) GET /instances returns 401', async ({ request }) => {
    const res = await request.get(`${PROSTH}/instances`, {
      headers: { Authorization: 'Bearer invalid-token-invalid' },
    });
    expect(res.status()).toBe(401);
  });

  test('nurse (no PROSTHETICS_INSTANCE_CREATE) POST /instances (valid body) → 403', async ({ request }) => {
    // Use a p1-readable surface to discover ids; send them under the nurse token.
    const templateId = await findTemplateByIdName(request, p1Headers, 'TP-LL-02');
    const orderId = await findOrderIdByProductType(
      request,
      p1Headers,
      'LOWER_LIMB',
      '20000000-0000-4000-8000-000000000002',
    );
    const res = await request.post(`${PROSTH}/instances`, {
      headers: nurseHeaders,
      data: { orderId, templateId },
    });
    expect(res.status()).toBe(403);
  });

  test('nurse (no AUDIT_ACCESS) GET /audit → 403', async ({ request }) => {
    const res = await request.get(
      `${API}/audit?entity=FlowInstance&entityId=00000000-0000-4000-8000-000000000001`,
      { headers: nurseHeaders },
    );
    expect(res.status()).toBe(403);
  });

  test('regular admin reads a flow instance → 403 but reads its audit trail → 200', async ({ request }) => {
    const templateId = await findTemplateByIdName(request, p1Headers, 'TP-LL-02');
    const instance = await createFreeLowerInstance(request, p1Headers, templateId);

    // Regular admin has AUDIT_ACCESS but no PROSTHETICS_* permission → 403 on the instance read.
    const instRes = await request.get(`${PROSTH}/instances/${instance.id}`, { headers: adminHeaders });
    expect(instRes.status()).toBe(403);

    // The same admin CAN read the audit trail (AUDIT_ACCESS).
    const auditRes = await request.get(`${API}/audit?entity=FlowInstance&entityId=${instance.id}`, {
      headers: adminHeaders,
    });
    expect(auditRes.ok()).toBeTruthy();
    expect(((await auditRes.json()) as any).content.map((e: any) => e.action)).toContain('CREATE');

    await terminateInstance(request, p1Headers, instance.id);
  });

  test('foreign prosthetist → 404, PROSTHETICS_ADMINISTRATOR (allowAll) → 200', async ({ request }) => {
    const templateId = await findTemplateByIdName(request, p2Headers, 'TP-LL-02');

    // Free the lower order first (p1 owns any prior blocker), then give p2 a fresh instance.
    const p1Blocker = await createFreeLowerInstance(request, p1Headers, templateId);
    await terminateInstance(request, p1Headers, p1Blocker.id);
    const p2Instance = await createFreeLowerInstance(request, p2Headers, templateId);

    // p1 is neither the owner nor allowAll → the instance is invisible (404, not 403).
    const p1Res = await request.get(`${PROSTH}/instances/${p2Instance.id}`, { headers: p1Headers });
    expect(p1Res.status()).toBe(404);

    // The PROSTHETICS_ADMINISTRATOR (canViewAllInstances) reads it fine (200).
    const adminRes = await request.get(`${PROSTH}/instances/${p2Instance.id}`, { headers: prosthetisAdminHeaders });
    expect(adminRes.status()).toBe(200);
    expect((await adminRes.json()).id).toBe(p2Instance.id);

    await terminateInstance(request, p2Headers, p2Instance.id);
  });
});
