import { useState, useEffect, useCallback, useMemo, useRef, Fragment } from 'react';
import { History, RefreshCw, ShieldCheck, Save, Loader2, Upload } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Checkbox } from '@/components/ui/checkbox';
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
import { auditApi, adminApi, settingsApi } from '../../api/platform';
import { drugInteractionAdminApi } from '../../api/medication';
import type { DrugInteractionCatalog, DrugInteractionImportReport } from '../../types/medication';
import AuditLogTable from '../../components/common/AuditLogTable';
import { getErrorMessage } from '../../utils/errorMessage';
import { useAuth } from '../../services/AuthContext';
import type { User, AuditLog, PermissionMatrix, PermissionDef } from '../../types/core';

const ROLE_LABELS: Record<string, string> = {
  DOCTOR: 'Лікар',
  NURSE: 'Медсестра',
  HEAD_OF_DEPARTMENT: 'Завідувач відділення',
  ADMINISTRATOR: 'Адміністратор',
  AUDITOR: 'Аудитор',
  PROSTHETIST: 'Протезист',
  PROSTHETICS_ADMINISTRATOR: 'Адміністратор протезування',
  ADJACENT_SPECIALIST: 'Суміжний спеціаліст',
};

/** Interaction severity labels/colors for the «Зміст бази» section (#305). Extends SEVERITY_LABELS with `low`. */
const DI_SEV_LABEL: Record<string, string> = { critical: 'критично', high: 'високо', medium: 'помірно', low: 'низько' };
const DI_SEV_CLASS: Record<string, string> = { critical: 'bg-red-500', high: 'bg-orange-500', medium: 'bg-amber-500', low: 'bg-gray-400' };

export default function AdminPage() {
  useEffect(() => { document.title = 'Адмін — Superhumans Lviv'; }, []);
  const { hasPermission, user } = useAuth();
  const isDiAdmin = user?.role === 'ADMINISTRATOR';
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

  const [diReport, setDiReport] = useState<DrugInteractionImportReport | null>(null);
  const [diBusy, setDiBusy] = useState(false);
  const [diError, setDiError] = useState<string | null>(null);
  const [diNotice, setDiNotice] = useState<string | null>(null);
  const [diFile, setDiFile] = useState<File | null>(null);
  const [diLastImport, setDiLastImport] = useState<string | null>(null);
  const diFileRef = useRef<HTMLInputElement | null>(null);

  const loadDiLastImport = useCallback(async () => {
    try {
      const res = await settingsApi.getByKey('drug_interactions.last_import_at');
      setDiLastImport(res.data?.value ?? null);
    } catch { /* never imported yet */ }
  }, []);

  useEffect(() => { loadDiLastImport(); }, [loadDiLastImport]);

  const [diCatalog, setDiCatalog] = useState<DrugInteractionCatalog | null>(null);
  const [diPage, setDiPage] = useState(0);
  const [diSeverity, setDiSeverity] = useState('');
  const [diQuery, setDiQuery] = useState('');
  const [diReadBusy, setDiReadBusy] = useState(false);
  const [diReadError, setDiReadError] = useState<string | null>(null);
  const [diCatalogTouched, setDiCatalogTouched] = useState(false);

  const loadDiCatalog = useCallback(async (page = 0, severity = diSeverity, query = diQuery) => {
    setDiReadBusy(true);
    setDiReadError(null);
    try {
      const res = await drugInteractionAdminApi.getCatalog({
        severity: severity || undefined,
        query: query || undefined,
        page,
        size: 50,
      });
      setDiCatalog(res.data);
      setDiPage(page);
    } catch (err) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      setDiReadError(status === 403 ? 'Доступно лише Адміністратору' : getErrorMessage(err, 'Не вдалося завантажити базу взаємодій'));
    } finally {
      setDiReadBusy(false);
    }
  }, [diSeverity, diQuery]);

  const diName = useMemo(
    () => new Map((diCatalog?.drugs ?? []).map((d) => [d.atcCode, d.ukrainianRaw] as const)),
    [diCatalog],
  );

  useEffect(() => {
    if (tabValue === 'drug-interactions' && isDiAdmin && !diCatalogTouched) {
      setDiCatalogTouched(true);
      loadDiCatalog(0, '', '');
    }
  }, [tabValue, isDiAdmin, diCatalogTouched, loadDiCatalog]);

  const handleDiImport = async () => {
    if (!diFile) return;
    setDiBusy(true);
    setDiError(null);
    setDiNotice(null);
    try {
      const res = await drugInteractionAdminApi.importDataset(diFile);
      setDiReport(res.data);
      setDiNotice('Імпорт завершено');
      setDiFile(null);
      if (diFileRef.current) diFileRef.current.value = '';
      await loadDiLastImport();
      if (isDiAdmin) await loadDiCatalog(0);
    } catch (err) {
      setDiError(getErrorMessage(err, 'Імпорт відхилено'));
    } finally {
      setDiBusy(false);
    }
  };

  const [matrix, setMatrix] = useState<PermissionMatrix | null>(null);
  const [matrixDraft, setMatrixDraft] = useState<Record<string, Record<string, boolean>>>({});
  const [matrixDirty, setMatrixDirty] = useState(false);
  const [matrixSaving, setMatrixSaving] = useState(false);
  const [matrixError, setMatrixError] = useState<string | null>(null);
  const [matrixNotice, setMatrixNotice] = useState<string | null>(null);

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

  const loadMatrix = useCallback(async () => {
    try {
      const res = await adminApi.getPermissions();
      setMatrix(res.data);
      const draft: Record<string, Record<string, boolean>> = {};
      for (const role of res.data.roles) {
        draft[role] = {};
        for (const p of res.data.permissions) {
          draft[role][p.code] = (res.data.grants[role] ?? []).includes(p.code);
        }
      }
      setMatrixDraft(draft);
      setMatrixDirty(false);
      setMatrixError(null);
    } catch (err) {
      setMatrixError(getErrorMessage(err, 'Не вдалося завантажити матрицю доступів'));
    }
  }, []);

  useEffect(() => { loadMatrix(); }, [loadMatrix]);

  const toggleGrant = (role: string, code: string, checked: boolean) => {
    setMatrixDraft((prev) => ({ ...prev, [role]: { ...prev[role], [code]: checked } }));
    setMatrixDirty(true);
  };

  const saveMatrix = async () => {
    if (!matrix) return;
    setMatrixSaving(true);
    setMatrixError(null);
    setMatrixNotice(null);
    const changes: { role: string; code: string; granted: boolean }[] = [];
    for (const role of matrix.roles) {
      for (const p of matrix.permissions) {
        const current = (matrix.grants[role] ?? []).includes(p.code);
        const next = matrixDraft[role]?.[p.code] ?? false;
        if (current !== next) changes.push({ role, code: p.code, granted: next });
      }
    }
    try {
      for (const c of changes) {
        await adminApi.updateRolePermission(c.role, c.code, c.granted);
      }
      await loadMatrix();
      setMatrixNotice(changes.length > 0 ? `Збережено змін: ${changes.length}` : 'Змін немає');
    } catch (err) {
      setMatrixError(getErrorMessage(err, 'Не вдалося зберегти зміни'));
    } finally {
      setMatrixSaving(false);
    }
  };

  const matrixCategories = useMemo(() => {
    if (!matrix) return [];
    const map = new Map<string, PermissionDef[]>();
    for (const p of matrix.permissions) {
      const arr = map.get(p.category) ?? [];
      arr.push(p);
      map.set(p.category, arr);
    }
    return [...map.entries()];
  }, [matrix]);

  const handleRoleChange = async (userId: number, role: string) => {
    try {
      const res = await adminApi.updateRole(userId, role);
      setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, role: res.data.role } : u)));
      await adminApi.getStats().then((r) => setStats(r.data));
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

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <h1 className="font-rubik text-xl font-bold">Адміністративна панель</h1>
      </div>

      <Tabs value={tabValue} onValueChange={setTabValue} className="mb-3">
        <TabsList>
          <TabsTrigger value="users">Користувачі</TabsTrigger>
          <TabsTrigger value="permissions">Доступи та ролі</TabsTrigger>
          <TabsTrigger value="audit">Журнал аудиту</TabsTrigger>
          <TabsTrigger value="drug-interactions">База взаємодій</TabsTrigger>
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
                <div className="overflow-x-auto touch-pan-x">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>ID</TableHead>
                        <TableHead>ПІБ</TableHead>
                        <TableHead>Логін</TableHead>
                        <TableHead>Роль</TableHead>
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
                                <SelectItem value="PROSTHETIST">Протезист</SelectItem>
                                <SelectItem value="PROSTHETICS_ADMINISTRATOR">Адміністратор протезування</SelectItem>
                              </SelectContent>
                            </Select>
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

            <TabsContent value="permissions">
              <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2.5">
                <div className="mb-1.5 flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <h2 className="font-rubik flex items-center gap-1.5 text-base font-medium">
                      <ShieldCheck className="size-4" />
                      Матриця доступів
                    </h2>
                    <p className="text-sm text-muted-foreground">
                      Налаштуйте, які операції доступні кожній ролі. Зміни застосовуються одразу; для поточних сеансів — після повторного входу.
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {matrixDirty && (
                      <Badge variant="outline">Є незбережені зміни</Badge>
                    )}
                    <Button
                      size="sm"
                      onClick={saveMatrix}
                      disabled={matrixSaving || !matrixDirty}
                    >
                      {matrixSaving ? <Loader2 className="mr-1 size-4 animate-spin" /> : <Save className="mr-1 size-4" />}
                      Зберегти зміни
                    </Button>
                  </div>
                </div>
                {matrixNotice && <p className="mb-1.5 text-sm text-emerald-600">{matrixNotice}</p>}
                {matrixError && <p className="mb-1.5 text-sm text-destructive">{matrixError}</p>}
                {matrix && (
                  <div className="overflow-x-auto touch-pan-x">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead className="sticky left-0 z-10 min-w-[260px] bg-card">Операція</TableHead>
                          {matrix.roles.map((role) => (
                            <TableHead key={role} className="min-w-[72px] text-center">
                              {ROLE_LABELS[role] ?? role}
                            </TableHead>
                          ))}
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {matrixCategories.map(([category, perms]) => (
                          <Fragment key={category}>
                            <TableRow className="bg-muted/40">
                              <TableCell colSpan={matrix.roles.length + 1} className="sticky left-0 z-10 bg-muted/40 font-semibold">
                                {category}
                              </TableCell>
                            </TableRow>
                            {perms.map((p) => (
                              <TableRow key={p.code}>
                                <TableCell className="sticky left-0 z-10 bg-card">
                                  <div className="font-medium">{p.label}</div>
                                  {p.description && (
                                    <div className="text-xs text-muted-foreground">{p.description}</div>
                                  )}
                                </TableCell>
                                {matrix.roles.map((role) => (
                                  <TableCell key={role} className="text-center">
                                    <Checkbox
                                      aria-label={`${p.label} — ${ROLE_LABELS[role] ?? role}`}
                                      checked={matrixDraft[role]?.[p.code] ?? false}
                                      onCheckedChange={(checked) => toggleGrant(role, p.code, checked === true)}
                                    />
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </Fragment>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                )}
                {!matrix && !matrixError && (
                  <Loader2 className="mx-auto mt-4 block size-6 animate-spin text-primary" />
                )}
              </div>
            </TabsContent>

            <TabsContent value="audit">
              <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2.5">
                <div className="mb-1.5 flex items-center justify-between">
                  <h2 className="font-rubik text-base font-medium">Журнал аудиту</h2>
                  {hasPermission('AUDIT_ACCESS') ? (
                    <Button
                      variant={showAudit ? 'outline' : 'default'}
                      onClick={() => setShowAudit(!showAudit)}
                    >
                      <History className="mr-1 size-4" />
                      {showAudit ? 'Сховати' : 'Переглянути'}
                    </Button>
                  ) : (
                    <span className="text-sm text-muted-foreground">Доступ до журналу аудиту відкликано у ролі «Адміністратор»</span>
                  )}
                </div>
                {showAudit && (
                  <>
                    <div className="mb-1.5 flex flex-wrap gap-1">
                      <Input
                        placeholder="Фільтр за сутністю"
                        value={auditFilterEntity}
                        onChange={(e) => setAuditFilterEntity(e.target.value)}
                        className="w-full sm:w-[200px]"
                      />
                      <Button variant="outline" size="sm" onClick={loadAudit}>Пошук</Button>
                    </div>
                    <AuditLogTable logs={auditLogs} loading={auditLoading} />
                  </>
                )}
              </div>
            </TabsContent>

            <TabsContent value="drug-interactions">
              <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2.5">
                <div className="mb-1.5 flex flex-wrap items-center justify-between gap-2">
                  <h2 className="font-rubik text-base font-medium">База взаємодій ліків (Form 003-4/о)</h2>
                  <span className="text-xs text-muted-foreground">
                    {diLastImport ? `Останній імпорт: ${diLastImport}` : 'Імпорт ще не виконували'}
                  </span>
                </div>
                <p className="mb-2 text-sm text-muted-foreground">
                  Повна заміна бази взаємодій завантаженим JSON-файлом (до 40 МБ).
                  Файл містить список препаратів (ATC) та пари взаємодій із severity
                  (low/medium/high/critical).
                </p>
                <div className="flex flex-wrap items-center gap-2">
                  <input
                    ref={diFileRef}
                    type="file"
                    accept="application/json,.json"
                    className="text-sm"
                    onChange={(e) => setDiFile(e.target.files?.[0] ?? null)}
                  />
                  <Button size="sm" onClick={handleDiImport} disabled={diBusy || !diFile}>
                    <Upload className="mr-1 size-4" />
                    {diBusy ? 'Імпортуємо…' : 'Імпортувати'}
                  </Button>
                </div>
                {diError && <Alert variant="destructive" className="mt-2">{diError}</Alert>}
                {diNotice && !diError && <Alert variant="warning" className="mt-2">{diNotice}</Alert>}
                {diReport && (
                  <div className="mt-2 grid grid-cols-2 gap-2 text-sm sm:grid-cols-3 lg:grid-cols-5">
                    <div className="rounded-lg border p-2">Препаратів: <b>{diReport.drugs}</b></div>
                    <div className="rounded-lg border p-2">Взаємодій: <b>{diReport.interactions}</b></div>
                    <div className="rounded-lg border p-2">Пропущено: <b>{diReport.skipped}</b></div>
                    <div className="rounded-lg border p-2">Час: <b>{diReport.durationMs} мс</b></div>
                    <div className="rounded-lg border p-2 break-all">Hash: <b>{diReport.sourceHash.slice(0, 12)}…</b></div>
                  </div>
                )}
                {!user ? null : !isDiAdmin ? (
                  <Alert variant="warning" className="mt-2">
                    <AlertDescription>Перегляд бази взаємодій доступний лише Адміністратору.</AlertDescription>
                  </Alert>
                ) : (
                  <div className="mt-3 border-t pt-2">
                    <div className="mb-1.5 flex flex-wrap items-center justify-between gap-2">
                      <h3 className="font-rubik text-sm font-medium">Зміст бази</h3>
                      <Button size="sm" variant="outline" onClick={() => loadDiCatalog(0)} disabled={diReadBusy}>
                        <RefreshCw className="mr-1 size-4" />
                        Оновити
                      </Button>
                    </div>
                    {diReadError && (
                      <Alert variant="destructive" className="mt-2">
                        <AlertDescription>{diReadError}</AlertDescription>
                      </Alert>
                    )}
                    {diCatalog && (
                      <>
                        <div className="mt-2 grid grid-cols-2 gap-2 text-sm sm:grid-cols-3 lg:grid-cols-6">
                          <div className="rounded-lg border p-2">Препаратів: <b>{diCatalog.summary.drugs}</b></div>
                          <div className="rounded-lg border p-2">Взаємодій: <b>{diCatalog.summary.interactions}</b></div>
                          <div className="rounded-lg border p-2">Критично: <b>{diCatalog.summary.bySeverity.critical}</b></div>
                          <div className="rounded-lg border p-2">Високо: <b>{diCatalog.summary.bySeverity.high}</b></div>
                          <div className="rounded-lg border p-2">Помірно: <b>{diCatalog.summary.bySeverity.medium}</b></div>
                          <div className="rounded-lg border p-2">Низько: <b>{diCatalog.summary.bySeverity.low}</b></div>
                        </div>
                        <div className="mt-2 flex flex-wrap items-center gap-2">
                          <Select
                            value={diSeverity || 'all'}
                            onValueChange={(val: string | null) => { const v = val === 'all' ? '' : (val ?? ''); setDiSeverity(v); loadDiCatalog(0, v, diQuery); }}
                          >
                            <SelectTrigger className="min-w-[140px]" aria-label="Рівень взаємодії">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="all">Усі рівні</SelectItem>
                              <SelectItem value="critical">критично</SelectItem>
                              <SelectItem value="high">високо</SelectItem>
                              <SelectItem value="medium">помірно</SelectItem>
                              <SelectItem value="low">низько</SelectItem>
                            </SelectContent>
                          </Select>
                          <Input
                            className="max-w-[220px]"
                            placeholder="Пошук: назва або ATC"
                            value={diQuery}
                            onChange={(e) => setDiQuery(e.target.value)}
                            onKeyDown={(e) => { if (e.key === 'Enter') loadDiCatalog(0); }}
                          />
                          <Button size="sm" onClick={() => loadDiCatalog(0)} disabled={diReadBusy}>
                            Пошук
                          </Button>
                        </div>
                        <div className="mt-2 overflow-x-auto touch-pan-x">
                          <Table>
                            <TableHeader>
                              <TableRow>
                                <TableHead>Препарат A</TableHead>
                                <TableHead>Препарат B</TableHead>
                                <TableHead>Рівень</TableHead>
                                <TableHead>Опис</TableHead>
                                <TableHead>ID</TableHead>
                              </TableRow>
                            </TableHeader>
                            <TableBody>
                              {diCatalog.page.content.map((p, i) => (
                                <TableRow key={`${p.drugAAtc}-${p.drugBAtc}-${p.severity}-${i}`}>
                                  <TableCell>
                                    <div className="font-medium">{diName.get(p.drugAAtc) ?? p.drugAAtc}</div>
                                    <div className="text-xs text-muted-foreground">{p.drugAAtc}</div>
                                  </TableCell>
                                  <TableCell>
                                    <div className="font-medium">{diName.get(p.drugBAtc) ?? p.drugBAtc}</div>
                                    <div className="text-xs text-muted-foreground">{p.drugBAtc}</div>
                                  </TableCell>
                                  <TableCell>
                                    <Badge className={DI_SEV_CLASS[p.severity] ?? ''}>{DI_SEV_LABEL[p.severity] ?? p.severity}</Badge>
                                  </TableCell>
                                  <TableCell title={p.interaction}>
                                    {p.interaction.length > 80 ? `${p.interaction.slice(0, 80)}…` : p.interaction}
                                  </TableCell>
                                  <TableCell className="text-xs">{p.interactionId}</TableCell>
                                </TableRow>
                              ))}
                              {diCatalog.page.content.length === 0 && (
                                <TableRow>
                                  <TableCell colSpan={5}>Нічого не знайдено</TableCell>
                                </TableRow>
                              )}
                            </TableBody>
                          </Table>
                        </div>
                        <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                          <Button size="sm" variant="outline" disabled={diPage === 0 || diReadBusy} onClick={() => loadDiCatalog(diPage - 1)}>
                            ‹ Назад
                          </Button>
                          <span>Ст. {diPage + 1} із {Math.max(diCatalog.page.totalPages, 1)} · показано {diCatalog.page.content.length} із {diCatalog.page.totalElements}</span>
                          <Button size="sm" variant="outline" disabled={diPage + 1 >= diCatalog.page.totalPages || diReadBusy} onClick={() => loadDiCatalog(diPage + 1)}>
                            Далі ›
                          </Button>
                        </div>
                      </>
                    )}
                    {diReadBusy && !diCatalog && <Loader2 className="mx-auto mt-4 block size-6 animate-spin text-primary" />}
                  </div>
                )}
              </div>
            </TabsContent>

            <TabsContent value="stats">
              <div className="rounded-xl border bg-card text-card-foreground shadow-sm p-2.5">
                <h2 className="font-rubik mb-2 text-base font-medium">Статистика системи</h2>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-4">
                  {Object.entries(stats).map(([key, val]) => (
                    <div
                      key={key}
                      className="rounded-xl border bg-card p-2 text-center shadow-sm"
                    >
                      <div className="text-2xl font-bold">{val}</div>
                      <div className="text-sm text-muted-foreground">
                        {key === 'totalUsers' ? 'Всього користувачів'
                          : key === 'doctors' ? 'Лікарів'
                          : key === 'nurses' ? 'Медсестер'
                          : key === 'headsOfDepartment' ? 'Завідувачів'
                          : key === 'administrators' ? 'Адміністраторів' : key}
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
