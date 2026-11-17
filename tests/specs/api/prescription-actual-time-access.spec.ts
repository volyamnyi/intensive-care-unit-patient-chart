import { test, expect, type APIRequestContext } from '@playwright/test';

import {
  ACTUAL_TIME_EDIT_PERMISSION,
  ensureActualTimeItem,
  executeDose,
  findTodayMorningPart,
  grantRolePermission,
  localDate,
  localTime,
  loginToken,
  planDose,
  setRolePermission,
} from '../../helpers/actual-time';
import { ensureMedicationList } from '../../helpers/medication';

/**
 * API contract of the actual administration/dispensing time (#443/#444, phase #447).
 *
 *   POST /api/prescriptions/day-parts/{dayPartId}/execute
 *        -> captures the actual time; PRESCRIPTION_EXECUTE (nurse) is enough
 *   PUT  /api/prescriptions/executions/{executionId}/actual-time
 *        -> corrects an already recorded time; PRESCRIPTION_ACTUAL_TIME_EDIT
 *           (doctor + head of department by default)
 *
 * The spec is serial: it mutates the role-permission matrix, so the grant /
 * revoke cases must not race with the read-only ones.
 */

const API = 'http://localhost:8085/api';
const DOSE = '5mg';
/** A dedicated stub patient whose prescription list is not shared with the other specs. */
const CLOSED_LIST_PATIENT_ID = 10401;

type Ctx = {
  listId: string;
  itemId: string;
  executionId: string;
  executionVersion: number;
  administeredAt: string;
};

function auth(token: string) {
  return { Authorization: `Bearer ${token}` };
}

/** Read the execution facts of the executed cell (doctor sees the clinical data). */
async function readFacts(request: APIRequestContext, token: string, listId: string, itemId: string) {
  const part = await findTodayMorningPart(request, token, listId, itemId);
  expect(part, 'the executed day part is missing').toBeTruthy();
  return part as {
    executionId: string;
    executionVersion: number;
    administeredAt: string;
    dispensedAt: string | null;
    administeredAtManual: boolean;
  };
}

/** Doctor plans, nurse executes - the facts are then ready for a correction. */
async function executedDose(
  request: APIRequestContext,
  medicineName: string,
  patientId: number,
): Promise<Ctx> {
  const doctorToken = await loginToken(request, 1);
  const listId = await ensureMedicationList(request, doctorToken, patientId);
  const { itemId, dayPartId } = await ensureActualTimeItem(request, doctorToken, listId, medicineName);
  await planDose(request, doctorToken, dayPartId, DOSE);

  const administeredAt = new Date(Date.now() - 10 * 60_000);
  const iso = `${localDate(administeredAt)}T${localTime(administeredAt)}`;
  const nurseToken = await loginToken(request, 3);
  await executeDose(request, nurseToken, dayPartId, { actualDose: DOSE, administeredAt: iso });

  const facts = await readFacts(request, doctorToken, listId, itemId);
  expect(facts.executionId, 'the execution id was not exposed on the day part').toBeTruthy();
  return {
    listId,
    itemId,
    executionId: facts.executionId,
    executionVersion: facts.executionVersion,
    administeredAt: facts.administeredAt,
  };
}

test.describe('Prescription actual-time API contract (#447)', () => {
  test.describe.configure({ mode: 'serial' });

  let ctx: Ctx;

  test.beforeEach(async ({ request }) => {
    // The correction right is a matrix grant: a fresh database holds it for
    // DOCTOR and HEAD_OF_DEPARTMENT by default, a long-lived dev database may
    // not. Grant it through the admin API - the sanctioned path for an
    // existing database (no SQL backfill).
    await grantRolePermission(request, 'DOCTOR', ACTUAL_TIME_EDIT_PERMISSION);
    await grantRolePermission(request, 'HEAD_OF_DEPARTMENT', ACTUAL_TIME_EDIT_PERMISSION);
    ctx = await executedDose(request, 'IT447-Rbac', 10102);
  });

  test('nurse captures the actual time at execution and the fact is stored', async ({ request }) => {
    const nurseToken = await loginToken(request, 3);
    const facts = await readFacts(request, nurseToken, ctx.listId, ctx.itemId);

    // The manually captured time is stored with seconds and flagged as manual.
    expect(facts.administeredAt).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);
    expect(facts.administeredAtManual).toBe(true);
    expect(facts.dispensedAt).toBeNull();
    expect(typeof facts.executionVersion).toBe('number');
  });

  test('nurse cannot correct the actual time (403)', async ({ request }) => {
    const nurseToken = await loginToken(request, 3);
    const res = await request.put(`${API}/prescriptions/executions/${ctx.executionId}/actual-time`, {
      headers: auth(nurseToken),
      data: {
        administeredAt: ctx.administeredAt,
        reason: 'Незрозуміла причина',
        version: ctx.executionVersion,
      },
    });
    expect(res.status()).toBe(403);
    const body = await res.json();
    expect(typeof body.code).toBe('string');
    expect(typeof body.message).toBe('string');
  });

  test('doctor corrects the actual time (200) and the fact changes', async ({ request }) => {
    const doctorToken = await loginToken(request, 1);
    const corrected = new Date(Date.now() - 5 * 60_000);
    const iso = `${localDate(corrected)}T${localTime(corrected)}`;

    const res = await request.put(`${API}/prescriptions/executions/${ctx.executionId}/actual-time`, {
      headers: auth(doctorToken),
      data: { administeredAt: iso, reason: 'Дозу введено на 5 хвилин пізніше', version: ctx.executionVersion },
    });
    expect(res.status()).toBe(200);

    const facts = await readFacts(request, doctorToken, ctx.listId, ctx.itemId);
    expect(facts.administeredAt).toBe(iso);
    expect(facts.administeredAtManual).toBe(true);
  });

  test('head of department corrects the actual time (200)', async ({ request }) => {
    const hodToken = await loginToken(request, 5);
    const facts = await readFacts(request, hodToken, ctx.listId, ctx.itemId);
    const corrected = new Date(Date.now() - 7 * 60_000);
    const iso = `${localDate(corrected)}T${localTime(corrected)}`;

    const res = await request.put(`${API}/prescriptions/executions/${ctx.executionId}/actual-time`, {
      headers: auth(hodToken),
      data: { administeredAt: iso, reason: 'Уточнення за розпорядженням завідувача', version: facts.executionVersion },
    });
    expect(res.status()).toBe(200);

    const after = await readFacts(request, hodToken, ctx.listId, ctx.itemId);
    expect(after.administeredAt).toBe(iso);
  });

  test('administrator cannot correct the actual time (403)', async ({ request }) => {
    const adminToken = await loginToken(request, 6);
    const res = await request.put(`${API}/prescriptions/executions/${ctx.executionId}/actual-time`, {
      headers: auth(adminToken),
      data: {
        administeredAt: ctx.administeredAt,
        reason: 'Адміністратор не має права коригувати',
        version: ctx.executionVersion,
      },
    });
    expect(res.status()).toBe(403);
  });

  test('granting and revoking the permission flips the correction right immediately', async ({ request }) => {
    const nurseToken = await loginToken(request, 3);
    const facts = await readFacts(request, nurseToken, ctx.listId, ctx.itemId);
    const payload = {
      administeredAt: ctx.administeredAt,
      reason: 'Перевірка матриці прав',
      version: facts.executionVersion,
    };
    const url = `${API}/prescriptions/executions/${ctx.executionId}/actual-time`;

    // The nurse holds no correction right by default.
    expect((await request.put(url, { headers: auth(nurseToken), data: payload })).status()).toBe(403);

    // Grant it -> the very same request is accepted.
    await setRolePermission(request, 'NURSE', ACTUAL_TIME_EDIT_PERMISSION, true);
    expect((await request.put(url, { headers: auth(nurseToken), data: payload })).status()).toBe(200);

    // Revoke it again -> the right is gone at once.
    await setRolePermission(request, 'NURSE', ACTUAL_TIME_EDIT_PERMISSION, false);
    expect((await request.put(url, { headers: auth(nurseToken), data: payload })).status()).toBe(403);
  });

  test('a future time is rejected (422) and nothing changes', async ({ request }) => {
    const doctorToken = await loginToken(request, 1);
    const facts = await readFacts(request, doctorToken, ctx.listId, ctx.itemId);
    const future = new Date(Date.now() + 2 * 60 * 60_000);
    const iso = `${localDate(future)}T${localTime(future)}`;

    const res = await request.put(`${API}/prescriptions/executions/${ctx.executionId}/actual-time`, {
      headers: auth(doctorToken),
      data: { administeredAt: iso, reason: 'Майбутній час', version: facts.executionVersion },
    });
    expect(res.status()).toBe(422);
    const body = await res.json();
    expect(body.code).toBe('BUSINESS_RULE');
    expect(body.message).toBe('Фактичний час не може бути в майбутньому');

    const after = await readFacts(request, doctorToken, ctx.listId, ctx.itemId);
    expect(after.administeredAt).toBe(facts.administeredAt);
    expect(after.executionVersion).toBe(facts.executionVersion);
  });

  test('a malformed time is rejected (400)', async ({ request }) => {
    const doctorToken = await loginToken(request, 1);
    const facts = await readFacts(request, doctorToken, ctx.listId, ctx.itemId);
    const res = await request.put(`${API}/prescriptions/executions/${ctx.executionId}/actual-time`, {
      headers: auth(doctorToken),
      data: {
        administeredAt: '17/11/2026 10:30',
        reason: 'Некоректний формат',
        version: facts.executionVersion,
      },
    });
    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.code).toBe('VALIDATION_ERROR');
  });

  test('a blank reason is rejected (400)', async ({ request }) => {
    const doctorToken = await loginToken(request, 1);
    const facts = await readFacts(request, doctorToken, ctx.listId, ctx.itemId);
    const res = await request.put(`${API}/prescriptions/executions/${ctx.executionId}/actual-time`, {
      headers: auth(doctorToken),
      data: { administeredAt: ctx.administeredAt, reason: '', version: facts.executionVersion },
    });
    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.code).toBe('VALIDATION_ERROR');
  });

  test('a stale version is rejected (409) and the stored fact survives', async ({ request }) => {
    const doctorToken = await loginToken(request, 1);
    const facts = await readFacts(request, doctorToken, ctx.listId, ctx.itemId);
    const stale = facts.executionVersion + 5;

    const res = await request.put(`${API}/prescriptions/executions/${ctx.executionId}/actual-time`, {
      headers: auth(doctorToken),
      data: {
        administeredAt: ctx.administeredAt,
        reason: 'Застаріла версія',
        version: stale,
      },
    });
    expect(res.status()).toBe(409);
    const body = await res.json();
    expect(body.code).toBe('VERSION_CONFLICT');

    const after = await readFacts(request, doctorToken, ctx.listId, ctx.itemId);
    expect(after.executionVersion).toBe(facts.executionVersion);
    expect(after.administeredAt).toBe(facts.administeredAt);
  });

  test('an unknown execution id is rejected (404)', async ({ request }) => {
    const doctorToken = await loginToken(request, 1);
    const res = await request.put(
      `${API}/prescriptions/executions/00000000-0000-0000-0000-000000000000/actual-time`,
      {
        headers: auth(doctorToken),
        data: {
          administeredAt: ctx.administeredAt,
          reason: 'Невідомий execution',
          version: 0,
        },
      },
    );
    expect(res.status()).toBe(404);
    const body = await res.json();
    expect(body.code).toBe('NOT_FOUND');
  });

  test('a dose that was never executed has no execution to correct (404)', async ({ request }) => {
    // An execution is created together with isCompleted=true, so an unexecuted
    // cell carries no executionId at all - the correction endpoint is simply
    // not reachable for it (the client cannot even address it).
    const doctorToken = await loginToken(request, 1);
    const listId = await ensureMedicationList(request, doctorToken, 10102);
    const { itemId, dayPartId } = await ensureActualTimeItem(request, doctorToken, listId, 'IT447-NotExecuted');
    await planDose(request, doctorToken, dayPartId, DOSE);

    const part = await findTodayMorningPart(request, doctorToken, listId, itemId);
    expect(part.executionId ?? null).toBeNull();

    const res = await request.put(
      `${API}/prescriptions/executions/00000000-0000-0000-0000-000000000000/actual-time`,
      {
        headers: auth(doctorToken),
        data: {
          administeredAt: ctx.administeredAt,
          reason: 'Дозу не виконували',
          version: 0,
        },
      },
    );
    expect(res.status()).toBe(404);
  });

  test('a closed prescription list rejects the correction (422 DOCUMENT_LOCKED)', async ({ request }) => {
    const closedCtx = await executedDose(request, 'IT447-ClosedList', CLOSED_LIST_PATIENT_ID);
    const doctorToken = await loginToken(request, 1);

    const close = await request.post(`${API}/prescriptions/${closedCtx.listId}/close`, {
      headers: auth(doctorToken),
    });
    expect(close.status()).toBe(200);

    const res = await request.put(`${API}/prescriptions/executions/${closedCtx.executionId}/actual-time`, {
      headers: auth(doctorToken),
      data: {
        administeredAt: closedCtx.administeredAt,
        reason: 'Коригування після закриття листка',
        version: closedCtx.executionVersion,
      },
    });
    expect(res.status()).toBe(422);
    const body = await res.json();
    expect(body.code).toBe('DOCUMENT_LOCKED');
  });
});
