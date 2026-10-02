import { describe, expect, it } from 'vitest';
import { auditActionConfig, createAuditActionId } from './auditAction';

describe('audit action correlation helper', () => {
  it('creates UUID correlation IDs', () => {
    expect(createAuditActionId()).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
    );
  });

  it('attaches the caller-owned UI action ID without turning it into idempotency', () => {
    const id = createAuditActionId();
    expect(auditActionConfig(id)).toEqual({ auditActionId: id });
  });
});
