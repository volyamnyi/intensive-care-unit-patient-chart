import { useCallback, useEffect, useState } from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { auditEventsApi } from '../../api/platform';
import { getErrorMessage } from '../../utils/errorMessage';
import { useAuth } from '../../services/AuthContext';
import type { AuditObjectHistory } from '../../types/core';
import { AuditObjectHistoryView } from './AuditEventConsole';

/**
 * Object-history entry point (Audit v2 F7): chronological canonical events
 * touching one entity, oldest first. The query API is gated server-side by
 * AUDIT_ACCESS/AUDITOR, so the section never fires the request without that
 * grant — a denied fetch would log a 403 network error to the console on
 * every page load. Renders nothing-but-a-hint when the caller lacks console
 * access; it never fabricates history from non-audit sources.
 */
export default function ObjectHistorySection({ entityType, entityId, title }: {
  entityType: string;
  entityId: string;
  title?: string;
}) {
  const { hasPermission, user } = useAuth();
  const canAudit = hasPermission('AUDIT_ACCESS') || user?.role === 'AUDITOR';
  const [history, setHistory] = useState<AuditObjectHistory | null>(null);
  const [denied, setDenied] = useState(false);
  const [failed, setFailed] = useState<string | null>(null);

  const load = useCallback(async (signal: AbortSignal) => {
    try {
      const res = await auditEventsApi.objectHistory(entityType, entityId);
      if (!signal.aborted) {
        setHistory(res.data);
        setDenied(false);
        setFailed(null);
      }
    } catch (err) {
      if (signal.aborted) return;
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 403) {
        setDenied(true);
      } else {
        setFailed(getErrorMessage(err, 'Не вдалося завантажити історію змін'));
      }
    }
  }, [entityType, entityId]);

  useEffect(() => {
    if (!canAudit) {
      setDenied(true);
      return;
    }
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [canAudit, load]);

  if (denied) {
    return (
      <p className="text-xs text-muted-foreground" data-testid="object-history-denied">
        Історія змін доступна ролям із доступом до консолі аудиту.
      </p>
    );
  }

  if (failed) {
    return (
      <Alert variant="destructive">
        <AlertDescription>{failed}</AlertDescription>
      </Alert>
    );
  }

  if (!history) {
    return <p className="text-xs text-muted-foreground">Завантаження історії змін…</p>;
  }

  const noop = () => {};
  return (
    <div data-testid="object-history-section">
      {title && <h3 className="font-rubik text-sm font-medium">{title}</h3>}
      <AuditObjectHistoryView history={history} onSelect={noop} />
    </div>
  );
}
