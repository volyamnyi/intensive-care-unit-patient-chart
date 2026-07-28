import { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Typography, Button, CircularProgress, Alert, useTheme,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TableSortLabel, Paper, ToggleButtonGroup, ToggleButton, TextField, Link,
  Menu, MenuItem,
} from '@mui/material';
import { patientApi, prescriptionApi } from '../../api/endpoints';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PatientDto, PrescriptionList } from '../../types';

type Department = 'surgery' | 'rehab';

interface PatientRow {
  patient: PatientDto;
  lists: PrescriptionList[];
}

type SortKey = 'id' | 'name' | 'room' | 'bed' | 'doctor' | 'status';

export default function NursePrescriptionPage() {
  const theme = useTheme();
  const navigate = useNavigate();
  useEffect(() => { document.title = 'Призначення — Медсестра'; }, []);

  const [dept, setDept] = useState<Department>(
    () => (localStorage.getItem('nursePrescDept') as Department) || 'surgery',
  );
  const [rows, setRows] = useState<PatientRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const [menuPatientId, setMenuPatientId] = useState<number | null>(null);
  const [menuLists, setMenuLists] = useState<PrescriptionList[]>([]);

  const loadPatients = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await patientApi.search('');
      const deptPatients = res.data.filter(p => {
        if (dept === 'surgery') return p.departmentId === 2;
        return p.departmentId === 1;
      });

      setRows(deptPatients.map(p => ({ patient: p, lists: [] })));

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

  const handleOpenMenu = (e: React.MouseEvent<HTMLElement>, lists: PrescriptionList[], patientId: number) => {
    setMenuAnchor(e.currentTarget);
    setMenuPatientId(patientId);
    setMenuLists(lists);
  };

  const handleCloseMenu = () => {
    setMenuAnchor(null);
    setMenuPatientId(null);
    setMenuLists([]);
  };

  const handleNavigate = (listId: string) => {
    handleCloseMenu();
    navigate(`/prescriptions/nurse/${listId}`);
  };

  const handleDeptChange = (_: unknown, val: Department | null) => {
    if (!val) return;
    setDept(val);
    localStorage.setItem('nursePrescDept', val);
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
          Виконання призначень
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <ToggleButtonGroup value={dept} exclusive onChange={handleDeptChange} size="small">
            <ToggleButton value="surgery">Хірургія</ToggleButton>
            <ToggleButton value="rehab">Реабілітація</ToggleButton>
          </ToggleButtonGroup>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

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
                <TableCell sx={{ width: 130 }}>Дії</TableCell>
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
                          onClick={e => handleOpenMenu(e, row.lists, row.patient.id)}
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
                      {row.lists.length > 0 && (
                        <Button size="small" variant="outlined"
                          onClick={e => handleOpenMenu(e, row.lists, row.patient.id)}>
                          Відкрити
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={handleCloseMenu}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      >
        {menuLists.map(pl => (
          <MenuItem key={pl.id} onClick={() => handleNavigate(pl.id)}>
            <Box sx={{ display: 'flex', flexDirection: 'column' }}>
              <Typography variant="body2">{pl.documentName}</Typography>
              <Typography variant="caption" color="text.secondary">
                {pl.status === 'Finished' ? 'Завершено' : 'В ході'}
              </Typography>
            </Box>
          </MenuItem>
        ))}
      </Menu>
    </Box>
  );
}
