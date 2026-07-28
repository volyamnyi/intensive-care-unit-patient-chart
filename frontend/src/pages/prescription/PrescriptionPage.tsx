import { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Typography, Button, CircularProgress, Alert, useTheme,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TableSortLabel, Paper, ToggleButtonGroup, ToggleButton, TextField, Link,
  Drawer, IconButton, Chip, List, ListItem, ListItemButton, ListItemText,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions,
} from '@mui/material';
import { Close, Delete, Edit, OpenInNew, ArticleOutlined } from '@mui/icons-material';
import { patientApi, prescriptionApi } from '../../api/endpoints';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PatientDto, PrescriptionList } from '../../types';

type Department = 'surgery' | 'rehab';

interface PatientRow {
  patient: PatientDto;
  lists: PrescriptionList[];
  loading: boolean;
}

type SortKey = 'id' | 'name' | 'room' | 'bed' | 'doctor' | 'status';

export default function PrescriptionPage() {
  const theme = useTheme();
  useEffect(() => { document.title = 'Призначення — Лікар'; }, []);
  const navigate = useNavigate();

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

  const handleDeptChange = (_: unknown, val: Department | null) => {
    if (!val) return;
    setDept(val);
    localStorage.setItem('prescDept', val);
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

  const getRowStyle = (lists: PrescriptionList[]) => {
    if (lists.length === 0) return { backgroundColor: '#FAFAD2' };
    if (lists.every(l => l.status === 'Finished')) return { backgroundColor: 'lightgrey' };
    return {};
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
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3, alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary }}>
          Листок лікарських призначень
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <ToggleButtonGroup value={dept} exclusive onChange={handleDeptChange} size="small">
            <ToggleButton value="surgery">Хірургія</ToggleButton>
            <ToggleButton value="rehab">Реабілітація</ToggleButton>
          </ToggleButtonGroup>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      <TextField
        size="small"
        placeholder="Пошук пацієнта за ПІБ, ID, № картки, палатою або лікарем"
        value={search}
        onChange={e => setSearch(e.target.value)}
        sx={{ mb: 2, maxWidth: 500 }}
      />

      {loading ? (
        <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={{ width: 70 }}>
                  <TableSortLabel active={sortKey === 'id'} direction={sortDir}
                    onClick={() => toggleSort('id')}>
                    Номер
                  </TableSortLabel>
                </TableCell>
                <TableCell>
                  <TableSortLabel active={sortKey === 'name'} direction={sortDir}
                    onClick={() => toggleSort('name')}>
                    Пацієнт
                  </TableSortLabel>
                </TableCell>
                <TableCell sx={{ width: 120 }}>
                  <TableSortLabel active={sortKey === 'room'} direction={sortDir}
                    onClick={() => toggleSort('room')}>
                    Палата
                  </TableSortLabel>
                </TableCell>
                <TableCell sx={{ width: 90 }}>
                  <TableSortLabel active={sortKey === 'bed'} direction={sortDir}
                    onClick={() => toggleSort('bed')}>
                    Ліжко
                  </TableSortLabel>
                </TableCell>
                <TableCell sx={{ width: 160 }}>
                  <TableSortLabel active={sortKey === 'doctor'} direction={sortDir}
                    onClick={() => toggleSort('doctor')}>
                    Лікар
                  </TableSortLabel>
                </TableCell>
                <TableCell sx={{ width: 110 }}>
                  <TableSortLabel active={sortKey === 'status'} direction={sortDir}
                    onClick={() => toggleSort('status')}>
                    Статус
                  </TableSortLabel>
                </TableCell>
                <TableCell sx={{ width: 160 }}>Дії</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredRows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    <Typography color="text.secondary" sx={{ py: 2 }}>
                      {search ? 'Пацієнтів не знайдено' : 'Немає пацієнтів у відділенні'}
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                filteredRows.map(row => (
                  <TableRow key={row.patient.id} hover sx={getRowStyle(row.lists)}>
                    <TableCell>{row.patient.id}</TableCell>
                    <TableCell>
                      {row.lists.length > 0 ? (
                        <Link
                          component="button"
                          variant="body2"
                          underline="hover"
                          sx={{ fontWeight: 600, textAlign: 'left' }}
                          onClick={() => handleOpenDrawer(row.patient, row.lists)}
                        >
                          {row.patient.fullName}
                        </Link>
                      ) : (
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {row.patient.fullName}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>{row.patient.room || '—'}</TableCell>
                    <TableCell>{row.patient.bed || '—'}</TableCell>
                    <TableCell>{row.patient.doctorName || '—'}</TableCell>
                    <TableCell>{getStatusText(row.lists)}</TableCell>
                     <TableCell>
                       <Box sx={{ display: 'flex', gap: 0.5 }}>
                         {row.lists.length > 0 && (
                           <Button size="small" variant="outlined"
                             onClick={() => handleOpenDrawer(row.patient, row.lists)}>
                             Відкрити
                           </Button>
                         )}
                       </Box>
                     </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Drawer anchor="right" open={drawerOpen} onClose={handleCloseDrawer}
        slotProps={{ backdrop: { sx: { backgroundColor: 'rgba(0,0,0,0.15)' } } }}>
        <Box sx={{ width: { xs: '90vw', sm: 400 }, display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: 2.5, borderBottom: 1, borderColor: 'divider' }}>
            <Box>
              <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700, lineHeight: 1.3 }}>
                {drawerPatient?.fullName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                ID: {drawerPatient?.id} · {drawerPatient?.room || '—'} · {drawerPatient?.bed || '—'}
              </Typography>
            </Box>
            <IconButton onClick={handleCloseDrawer} size="small"><Close /></IconButton>
          </Box>

          <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 1.5, color: 'text.secondary', textTransform: 'uppercase', fontSize: 11, letterSpacing: 0.5 }}>
              Листки призначень ({drawerLists.length})
            </Typography>

            {drawerLists.length === 0 ? (
              <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                Немає листків призначень
              </Typography>
            ) : (
              <List disablePadding sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {drawerLists.map(pl => (
                  <Paper key={pl.id} variant="outlined" sx={{ borderRadius: 2 }}>
                    <ListItem disablePadding>
                      <ListItemButton onClick={() => handleNavigate(pl.id)} sx={{ borderRadius: 2 }}>
                        <ListItemText
                          primary={
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <ArticleOutlined sx={{ fontSize: 18, color: 'primary.main' }} />
                              <Typography variant="body2" sx={{ fontWeight: 600 }}>{pl.documentName}</Typography>
                            </Box>
                          }
                          secondary={
                            <Typography component="div" variant="body2">
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
                                <Chip
                                  label={pl.status === 'Finished' ? 'Завершено' : 'В ході'}
                                  size="small"
                                  color={pl.status === 'Finished' ? 'default' : 'success'}
                                  variant="outlined"
                                  sx={{ height: 20, fontSize: 10 }}
                                />
                                <Typography variant="caption" color="text.secondary">
                                  {formatDate(pl.updatedAt || pl.createdAt)}
                                </Typography>
                              </Box>
                            </Typography>
                          }
                        />
                      </ListItemButton>
                    </ListItem>
                    <Box sx={{ display: 'flex', gap: 0.5, px: 1.5, pb: 1, justifyContent: 'flex-end' }}>
                      <Button size="small" variant="text"
                        startIcon={<OpenInNew />}
                        onClick={() => handleNavigate(pl.id)}>
                        Відкрити
                      </Button>
                      {pl.status !== 'Finished' && (
                        <Button size="small" variant="text" color="warning"
                          disabled={closingId === pl.id}
                          onClick={() => handleCloseList(pl)}>
                          {closingId === pl.id ? '...' : 'Закрити'}
                        </Button>
                      )}
                      <Button size="small" variant="text" color="error"
                        disabled={deletingId === pl.id}
                        onClick={() => { setDeleteTarget(pl); setDeleteDialogOpen(true); }}>
                        {deletingId === pl.id ? '...' : 'Видалити'}
                      </Button>
                    </Box>
                  </Paper>
                ))}
              </List>
            )}
           </Box>
         </Box>
       </Drawer>

      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Видалити листок?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Ви впевнені, що хочете видалити листок &laquo;{deleteTarget?.documentName}&raquo;? Цю дію не можна скасувати.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Скасувати</Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained"
            disabled={deletingId === deleteTarget?.id}>
            {deletingId === deleteTarget?.id ? 'Видалення...' : 'Видалити'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
