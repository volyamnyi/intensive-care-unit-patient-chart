import { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2, X, ExternalLink, FileText, Plus } from 'lucide-react';
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { patientApi } from '../../api/platform';
import { prescriptionApi } from '../../api/medication';
import { useAuth } from '../../services/AuthContext';
import { useThemeMode } from '../../styles/ThemeContext';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PatientDto } from '../../types/core';
import type { PrescriptionList } from '../../types/medication';

type Department = 'surgery' | 'rehab';

interface PatientRow {
  patient: PatientDto;
  lists: PrescriptionList[];
  loading: boolean;
}

type SortKey = 'id' | 'name' | 'room' | 'bed' | 'doctor' | 'status';

export default function PrescriptionPage() {
  useThemeMode();
  useEffect(() => { document.title = 'Призначення — Лікар'; }, []);
  const navigate = useNavigate();
  useAuth();

  const [dept, setDept] = useState<Department>(
    () => (localStorage.getItem('prescDept') as Department) || 'surgery',
  );
  const [rows, setRows] = useState<PatientRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerPatient, setDrawerPatient] = useState<PatientDto | null>(null);
  const [drawerLists, setDrawerLists] = useState<PrescriptionList[]>([]);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<PrescriptionList | null>(null);
  const [closingId, setClosingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [creatingPatientId, setCreatingPatientId] = useState<string | null>(null);

  const loadPatients = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await patientApi.search('');
      const deptPatients = res.data.filter(p => {
        if (dept === 'surgery') return p.departmentId === 2;
        return p.departmentId === 1;
      });

      setRows(deptPatients.map(p => ({ patient: p, lists: [], loading: false })));

      for (const p of deptPatients) {
        try {
          const lr = await prescriptionApi.getByPatient(p.id);
          setRows(prev =>
            prev.map(r => r.patient.id === p.id ? { ...r, lists: lr.data } : r)
          );
        } catch {
          // prescription not found, leave []
        }
      }
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити пацієнтів'));
    } finally {
      setLoading(false);
    }
  }, [dept]);

  useEffect(() => { loadPatients(); }, [loadPatients]);

  const handleOpenDrawer = (patient: PatientDto, lists: PrescriptionList[]) => {
    setDrawerPatient(patient);
    setDrawerLists(lists);
    setDrawerOpen(true);
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setDrawerPatient(null);
    setDrawerLists([]);
  };

  const handleNavigate = (listId: string) => {
    handleCloseDrawer();
    navigate(`/prescriptions/doctor/${listId}`);
  };

  const handleDeptChange = (val: Department) => {
    setDept(val);
    localStorage.setItem('prescDept', val);
  };

  const handleCreate = async (patient: PatientDto) => {
    setCreatingPatientId(String(patient.id));
    setError(null);
    try {
      const res = await prescriptionApi.create({ patientId: String(patient.id) });
      const created = res.data;
      setRows(prev => prev.map(r => r.patient.id === patient.id ? { ...r, lists: [created, ...r.lists] } : r));
      navigate(`/prescriptions/doctor/${created.id}`);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося створити листок призначень'));
    } finally {
      setCreatingPatientId(null);
    }
  };

  const handleCloseList = async (list: PrescriptionList) => {
    setClosingId(list.id);
    try {
      await prescriptionApi.close(list.id);
      setDrawerLists(prev => prev.map(l => l.id === list.id ? { ...l, status: 'Finished' } : l));
      setRows(prev => prev.map(r => ({
        ...r,
        lists: r.lists.map(l => l.id === list.id ? { ...l, status: 'Finished' } : l),
      })));
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося закрити листок'));
    } finally {
      setClosingId(null);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    setDeletingId(deleteTarget.id);
    try {
      await prescriptionApi.delete(deleteTarget.id);
      setDrawerLists(prev => prev.filter(l => l.id !== deleteTarget.id));
      setRows(prev => prev.map(r => ({
        ...r,
        lists: r.lists.filter(l => l.id !== deleteTarget.id),
      })));
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося видалити листок'));
    } finally {
      setDeletingId(null);
      setDeleteDialogOpen(false);
      setDeleteTarget(null);
    }
  };

  const toggleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  const getStatusText = (lists: PrescriptionList[]) => {
    if (lists.length === 0) return 'Заплановано';
    if (lists.some(l => l.status !== 'Finished')) return 'В ході';
    return 'Завершено';
  };

  const getRowClasses = (lists: PrescriptionList[]) => {
    if (lists.length === 0) return 'bg-yellow-100 dark:bg-yellow-900/30';
    if (lists.every(l => l.status === 'Finished')) return 'bg-muted/50';
    return '';
  };

  const formatDate = (dateStr: string | null | undefined): string => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? '' : d.toLocaleDateString('uk-UA');
  };

  const filteredRows = useMemo(() => {
    let list = rows;
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(r =>
        r.patient.fullName.toLowerCase().includes(q) ||
        String(r.patient.id).includes(q) ||
        (r.patient.externalId1 ?? '').toLowerCase().includes(q) ||
        (r.patient.room ?? '').toLowerCase().includes(q) ||
        (r.patient.doctorName ?? '').toLowerCase().includes(q),
      );
    }
    list = [...list].sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'id':
          cmp = a.patient.id - b.patient.id;
          break;
        case 'name':
          cmp = a.patient.fullName.localeCompare(b.patient.fullName, 'uk');
          break;
        case 'room':
          cmp = (a.patient.room ?? '').localeCompare(b.patient.room ?? '', 'uk');
          break;
        case 'bed':
          cmp = (a.patient.bed ?? '').localeCompare(b.patient.bed ?? '', 'uk');
          break;
        case 'doctor':
          cmp = (a.patient.doctorName ?? '').localeCompare(b.patient.doctorName ?? '', 'uk');
          break;
        case 'status':
          cmp = getStatusText(a.lists).localeCompare(getStatusText(b.lists), 'uk');
          break;
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return list;
  }, [rows, search, sortKey, sortDir]);

  return (
    <div>
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <h1 className="font-rubik text-xl font-extrabold text-foreground">
          Листок лікарських призначень
        </h1>
        <div className="flex items-center gap-1.5 rounded-lg bg-muted p-0.5">
          <Button
            variant={dept === 'surgery' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => handleDeptChange('surgery')}
          >
            Хірургія
          </Button>
          <Button
            variant={dept === 'rehab' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => handleDeptChange('rehab')}
          >
            Реабілітація
          </Button>
        </div>
      </div>

      {error && (
        <Alert variant="destructive" className="mb-2">
          <AlertDescription>{error}</AlertDescription>
          <Button variant="ghost" size="icon-sm" className="absolute right-2 top-2" onClick={() => setError(null)}>
            <X className="size-4" />
          </Button>
        </Alert>
      )}

      <Input
        placeholder="Пошук пацієнта за ПІБ, ID, № картки, палатою або лікарем"
        value={search}
        onChange={e => setSearch(e.target.value)}
        className="mb-2 max-w-[500px]"
      />

      {loading ? (
        <Loader2 className="mx-auto mt-4 block size-6 animate-spin text-primary" />
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[70px]">
                <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('id')}>
                  Номер {sortKey === 'id' && (sortDir === 'asc' ? '↑' : '↓')}
                </Button>
              </TableHead>
              <TableHead>
                <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('name')}>
                  Пацієнт {sortKey === 'name' && (sortDir === 'asc' ? '↑' : '↓')}
                </Button>
              </TableHead>
              <TableHead className="w-[120px]">
                <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('room')}>
                  Палата {sortKey === 'room' && (sortDir === 'asc' ? '↑' : '↓')}
                </Button>
              </TableHead>
              <TableHead className="w-[90px]">
                <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('bed')}>
                  Ліжко {sortKey === 'bed' && (sortDir === 'asc' ? '↑' : '↓')}
                </Button>
              </TableHead>
              <TableHead className="w-[160px]">
                <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('doctor')}>
                  Лікар {sortKey === 'doctor' && (sortDir === 'asc' ? '↑' : '↓')}
                </Button>
              </TableHead>
              <TableHead className="w-[110px]">
                <Button variant="ghost" className="whitespace-nowrap p-0 font-semibold" onClick={() => toggleSort('status')}>
                  Статус {sortKey === 'status' && (sortDir === 'asc' ? '↑' : '↓')}
                </Button>
              </TableHead>
              <TableHead className="w-[160px]">Дії</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredRows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center">
                  <span className="py-2 text-muted-foreground">
                    {search ? 'Пацієнтів не знайдено' : 'Немає пацієнтів у відділенні'}
                  </span>
                </TableCell>
              </TableRow>
            ) : (
              filteredRows.map(row => (
                <TableRow key={row.patient.id} className={getRowClasses(row.lists)}>
                  <TableCell>{row.patient.id}</TableCell>
                  <TableCell>
                    <span className="font-semibold">{row.patient.fullName}</span>
                  </TableCell>
                  <TableCell>{row.patient.room || '—'}</TableCell>
                  <TableCell>{row.patient.bed || '—'}</TableCell>
                  <TableCell>{row.patient.doctorName || '—'}</TableCell>
                  <TableCell>{getStatusText(row.lists)}</TableCell>
                  <TableCell>
                    <div className="flex gap-0.5">
                      <Button
                        size="sm"
                        className="rounded-full px-1.5 text-[0.8125rem] font-semibold normal-case"
                        variant="outline"
                        disabled={creatingPatientId === String(row.patient.id)}
                        onClick={() => handleCreate(row.patient)}
                      >
                        <Plus className="mr-1 size-4" />
                        {creatingPatientId === String(row.patient.id) ? '...' : 'Створити'}
                      </Button>
                      {row.lists.length > 0 && (
                        <Button
                          size="sm"
                          className="rounded-full px-1.5 text-[0.8125rem] font-semibold normal-case"
                          variant="outline"
                          onClick={() => handleOpenDrawer(row.patient, row.lists)}
                        >
                          <FileText className="mr-1 size-4" />
                          Відкрити
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      )}

      {/* Drawer */}
      {drawerOpen && (
        <>
          <div className="fixed inset-0 z-40 bg-black/15" onClick={handleCloseDrawer} />
          <div className="fixed right-0 top-0 z-50 flex h-full w-[90vw] flex-col border-l bg-card shadow-lg sm:w-[400px]">
            <div className="flex items-center justify-between border-b p-2.5">
              <div>
                <div className="font-rubik text-base font-bold leading-tight">{drawerPatient?.fullName}</div>
                <div className="text-xs text-muted-foreground">
                  ID: {drawerPatient?.id} · {drawerPatient?.room || '—'} · {drawerPatient?.bed || '—'}
                </div>
              </div>
              <Button variant="ghost" size="icon" onClick={handleCloseDrawer}>
                <X className="size-4" />
              </Button>
            </div>

            <div className="flex-1 overflow-auto p-2">
              <div className="mb-1.5 text-[11px] font-medium uppercase tracking-[0.5px] text-muted-foreground">
                Листки призначень ({drawerLists.length})
              </div>

              {drawerLists.length === 0 ? (
                <div className="py-4 text-center text-muted-foreground">Немає листків призначень</div>
              ) : (
                <div className="flex flex-col gap-1">
                  {drawerLists.map(pl => (
                    <div key={pl.id} className="rounded-xl border p-0">
                      <button
                        className="flex w-full flex-col gap-1 rounded-xl px-3 py-2 text-left hover:bg-muted/50"
                        onClick={() => handleNavigate(pl.id)}
                      >
                        <div className="flex items-center gap-1">
                          <FileText className="size-4 text-primary" />
                          <span className="text-sm font-semibold">{pl.documentName}</span>
                        </div>
                        <div className="flex items-center gap-1">
                          <Badge
                            variant={pl.status === 'Finished' ? 'outline' : 'default'}
                            className="h-5 px-1.5 text-[10px]"
                          >
                            {pl.status === 'Finished' ? 'Завершено' : 'В ході'}
                          </Badge>
                          <span className="text-xs text-muted-foreground">
                            {formatDate(pl.updatedAt || pl.createdAt)}
                          </span>
                        </div>
                      </button>
                      <div className="flex justify-end gap-0.5 px-3 pb-1">
                        <Button size="sm" variant="ghost" onClick={() => handleNavigate(pl.id)}>
                          <ExternalLink className="mr-1 size-3" />
                          Відкрити
                        </Button>
                        {pl.status !== 'Finished' && (
                          <Button
                            size="sm"
                            variant="ghost"
                            className="text-amber-600 hover:text-amber-700"
                            disabled={closingId === pl.id}
                            onClick={() => handleCloseList(pl)}
                          >
                            {closingId === pl.id ? '...' : 'Закрити'}
                          </Button>
                        )}
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-destructive hover:text-destructive"
                          disabled={deletingId === pl.id}
                          onClick={() => { setDeleteTarget(pl); setDeleteDialogOpen(true); }}
                        >
                          {deletingId === pl.id ? '...' : 'Видалити'}
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </>
      )}

      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Видалити листок?</DialogTitle>
            <DialogDescription>
              Ви впевнені, що хочете видалити листок &laquo;{deleteTarget?.documentName}&raquo;? Цю дію не можна скасувати.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialogOpen(false)}>Скасувати</Button>
            <Button variant="destructive" onClick={handleDeleteConfirm} disabled={deletingId === deleteTarget?.id}>
              {deletingId === deleteTarget?.id ? 'Видалення...' : 'Видалити'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
