import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import type { AuditLog } from '../../types/core';

interface AuditLogTableProps {
  logs: AuditLog[];
  loading?: boolean;
}

const actionVariantMap: Record<string, 'default' | 'secondary' | 'destructive' | 'outline' | 'ghost' | 'link'> = {
  CREATE: 'default',
  UPDATE: 'secondary',
  DELETE: 'destructive',
  ACTION: 'outline',
};

export default function AuditLogTable({ logs, loading }: AuditLogTableProps) {
  if (loading) {
    return <p className="text-muted-foreground font-mulish">Завантаження...</p>;
  }

  if (logs.length === 0) {
    return <p className="text-muted-foreground font-mulish">Немає записів аудиту</p>;
  }

  return (
    <div className="overflow-x-auto touch-pan-x">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Час</TableHead>
            <TableHead>Користувач</TableHead>
            <TableHead className="hidden sm:table-cell">Сутність</TableHead>
            <TableHead>Дія</TableHead>
            <TableHead className="hidden md:table-cell">Зміни</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {logs.map((log) => (
            <TableRow key={log.id}>
              <TableCell className="whitespace-nowrap">
                {new Date(log.timestamp).toLocaleString('uk-UA')}
              </TableCell>
              <TableCell>{log.userId ?? '-'}</TableCell>
              <TableCell className="hidden sm:table-cell">
                {log.entity}
                {log.entityId && <span className="ml-0.5 text-xs text-muted-foreground font-mulish">#{log.entityId.slice(0, 8)}</span>}
              </TableCell>
              <TableCell>
                <Badge variant={actionVariantMap[log.action] || 'default'}>
                  {log.action}
                </Badge>
              </TableCell>
              <TableCell className="hidden md:table-cell">
                <span className="whitespace-pre-wrap text-xs text-muted-foreground font-mulish">
                  {log.oldValue && `- ${log.oldValue}`}
                  {log.newValue && `\n+ ${log.newValue}`}
                </span>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
