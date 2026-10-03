import { useMemo } from 'react';
import { auditActionConfig, createAuditActionId } from '../lib/auditAction';

/**
 * UI-intent correlation hook (Audit v2 F7). Returns one stable action ID per
 * hook instance plus an axios config carrying it, so every request of a
 * single explicit user intent (one button press, one save round-trip) shares
 * the same `X-User-Action-Id`.
 *
 * The ID is correlation only — never an idempotency key and never proof of
 * a business fact. No form bodies, texts, tokens or PHI are attached here;
 * the server-side event stays authoritative and the client value is only
 * ever recorded as intent.
 */
export function useAuditAction() {
  const auditActionId = useMemo(() => createAuditActionId(), []);
  const config = useMemo(() => auditActionConfig(auditActionId), [auditActionId]);
  return { auditActionId, config };
}
