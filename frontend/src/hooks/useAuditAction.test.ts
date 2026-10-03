import { renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useAuditAction } from './useAuditAction';

describe('useAuditAction', () => {
  it('returns a stable UUID action ID with a matching axios config', () => {
    const { result, rerender } = renderHook(() => useAuditAction());

    expect(result.current.auditActionId).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    );
    expect(result.current.config).toEqual({ auditActionId: result.current.auditActionId });

    const firstId = result.current.auditActionId;
    rerender();
    expect(result.current.auditActionId).toBe(firstId);
  });

  it('creates distinct IDs per hook instance and carries no payload', () => {
    const { result: first } = renderHook(() => useAuditAction());
    const { result: second } = renderHook(() => useAuditAction());

    expect(first.current.auditActionId).not.toBe(second.current.auditActionId);
    expect(Object.keys(first.current.config)).toEqual(['auditActionId']);
  });
});
