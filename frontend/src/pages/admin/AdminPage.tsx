import { useState, useEffect, useCallback } from 'react';
import { History, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';

import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '@/components/ui/select';
import {
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
} from '@/components/ui/tabs';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Loader2 } from 'lucide-react';
import { auditApi, adminApi } from '../../api/endpoints';
import AuditLogTable from '../../components/common/AuditLogTable';
import { getErrorMessage } from '../../utils/errorMessage';
import type { User, AuditLog } from '../../types';

export default function AdminPage() {
  useEffect(() => { document.title = 'Адмін — Superhumans Lviv'; }, []);
  const [tabValue, setTabValue] = useState('users');
  const [users, setUsers] = useState<User[]>([]);
  const [stats, setStats] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);
  const [showAudit, setShowAudit] = useState(false);
  const [auditFilterEntity, setAuditFilterEntity] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [usersRes, statsRes] = await Promise.all([
        adminApi.getUsers(),
        adminApi.getStats(),
      ]);
      setUsers(usersRes.data);
      setStats(statsRes.data);
    } catch { /* */ }
    setLoading(false);
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const loadAudit = useCallback(async () => {
    setAuditLoading(true);
    try {
      const params: Record<string, string> = {};
      if (auditFilterEntity) params.entity = auditFilterEntity;
      const res = await auditApi.list(params);
      setAuditLogs(res.data.content ?? res.data);
    } finally {
      setAuditLoading(false);
    }
  }, [auditFilterEntity]);

  useEffect(() => {
    if (showAudit) loadAudit();
  }, [showAudit, loadAudit]);

  const handleRoleChange = async (userId: number, role: string) => {
    try {
      const res = await adminApi.updateRole(userId, role);
      setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, role: res.data.role } : u)));
      await adminApi.getStats().then((r) => setStats(r.data));
    } catch { /* */ }
  };

  const handlePermissionToggle = async (userId: number, permission: string, hasIt: boolean) => {
    try {
      const res = await adminApi.updatePermissions(userId, hasIt ? 'remove' : 'add', permission);
      setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, permissions: res.data.permissions } : u)));
    } catch { /* */ }
  };

  const handleDelete = async (userId: number) => {
    setError(null);
    try {
      await adminApi.deleteUser(userId);
      setUsers((prev) => prev.filter((u) => u.id !== userId));
      setDialogOpen(false);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося видалити користувача'));
    }
  };

  const hasPerm = (u: User, perm: string) =>
    (u.permissions ?? '').split(',').some((p) => p.trim().toUpperCase() === perm.toUpperCase());

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <h1 className="font-rubik text-xl font-bold">Адміністративна панель</h1>
      </div>

      <Tabs value={tabValue} onValueChange={setTabValue} className="mb-3">
        <TabsList>
          <TabsTrigger value="users">Користувачі</TabsTrigger>
          <TabsTrigger value="audit">Журнал аудиту</TabsTrigger>
          <TabsTrigger value="stats">Статистика</TabsTrigger>
        </TabsList>

        {loading && <Loader2 className="mx-auto mt-4 block size-6 animate-spin text-primary" />}

        {!loading && (
          <>
            <TabsContent value="users">
              <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2.5">
                <div className="mb-1.5 flex items-center justify-between">
                  <h2 className="font-rubik text-base font-medium">Користувачі ({users.length})</h2>
                  <Button size="sm" variant="outline" onClick={loadData}>
                    <RefreshCw className="mr-1 size-4" />
                    Оновити
                  </Button>
                </div>
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>ID</TableHead>
                        <TableHead>ПІБ</TableHead>
                        <TableHead>Логін</TableHead>
                        <TableHead>Роль</TableHead>
                        <TableHead>PRESCRIBER</TableHead>
                        <TableHead>Дії</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {users.map((u) => (
                        <TableRow
                          key={u.id}
                          className={u.deleted ? 'bg-destructive/20' : undefined}
                        >
                          <TableCell>{u.id}</TableCell>
                          <TableCell className="font-semibold">{u.fullName}</TableCell>
                          <TableCell>{u.login}</TableCell>
                          <TableCell>
                            <Select
                              value={u.role}
                              onValueChange={(val: string | null) => { if (val !== null) handleRoleChange(u.id, val); }}
                            >
                              <SelectTrigger className="min-w-[140px]">
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="DOCTOR">Лікар</SelectItem>
                                <SelectItem value="NURSE">Медсестра</SelectItem>
                                <SelectItem value="HEAD_OF_DEPARTMENT">Завідувач</SelectItem>
                                <SelectItem value="ADMINISTRATOR">Адміністратор</SelectItem>
                                <SelectItem value="AUDITOR">Аудитор</SelectItem>
                              </SelectContent>
                            </Select>
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant={hasPerm(u, 'PRESCRIBER') ? 'default' : 'outline'}
                              className="cursor-pointer"
                              onClick={() => handlePermissionToggle(u.id, 'PRESCRIBER', hasPerm(u, 'PRESCRIBER'))}
                            >
                              {hasPerm(u, 'PRESCRIBER') ? 'ТАК' : 'НІ'}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Button
                              size="sm"
                              variant="destructive"
                              onClick={() => { setSelectedUser(u); setDialogOpen(true); }}
                            >
                              Видалити
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </div>
            </TabsContent>

            <TabsContent value="audit">
              <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2.5">
                <div className="mb-1.5 flex items-center justify-between">
                  <h2 className="font-rubik text-base font-medium">Журнал аудиту</h2>
                  <Button
                    variant={showAudit ? 'outline' : 'default'}
                    onClick={() => setShowAudit(!showAudit)}
                  >
                    <History className="mr-1 size-4" />
                    {showAudit ? 'Сховати' : 'Переглянути'}
                  </Button>
                </div>
                {showAudit && (
                  <>
                    <div className="mb-1.5 flex flex-wrap gap-1">
                      <Input
                        placeholder="Фільтр за сутністю"
                        value={auditFilterEntity}
                        onChange={(e) => setAuditFilterEntity(e.target.value)}
                        className="w-[200px]"
                      />
                      <Button variant="outline" size="sm" onClick={loadAudit}>Пошук</Button>
                    </div>
                    <AuditLogTable logs={auditLogs} loading={auditLoading} />
                  </>
                )}
              </div>
            </TabsContent>

            <TabsContent value="stats">
              <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2.5">
                <h2 className="font-rubik mb-2 text-base font-medium">Статистика системи</h2>
                <div className="flex flex-wrap gap-2">
                  {Object.entries(stats).map(([key, val]) => (
                    <div
                      key={key}
                      className="min-w-[150px] rounded-xl border bg-card p-2 text-center shadow-sm"
                    >
                      <div className="text-2xl font-bold">{val}</div>
                      <div className="text-sm text-muted-foreground">
                        {key === 'totalUsers' ? 'Всього користувачів'
                          : key === 'doctors' ? 'Лікарів'
                          : key === 'nurses' ? 'Медсестер'
                          : key === 'headsOfDepartment' ? 'Завідувачів'
                          : key === 'administrators' ? 'Адміністраторів'
                          : key === 'prescribers' ? 'PRESCRIBER' : key}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </TabsContent>
          </>
        )}
      </Tabs>

      {error && (
        <div className="fixed top-4 left-1/2 z-50 -translate-x-1/2">
          <Alert variant="destructive" className="w-full">
            <AlertDescription>{error}</AlertDescription>
            <Button variant="ghost" size="icon-sm" className="absolute right-2 top-2" onClick={() => setError(null)}>
              <span className="sr-only">Close</span>
            </Button>
          </Alert>
        </div>
      )}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Підтвердження видалення</DialogTitle>
            <DialogDescription>
              Видалити користувача {selectedUser?.fullName} ({selectedUser?.login})?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>Скасувати</Button>
            <Button variant="destructive" onClick={() => selectedUser && handleDelete(selectedUser.id)}>
              Видалити
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
