import { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Typography, Button, CircularProgress, Alert, useTheme,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TableSortLabel, Chip, Paper, ToggleButtonGroup, ToggleButton, TextField,
} from '@mui/material';
import { patientApi, prescriptionApi } from '../../api/endpoints';
import { getErrorMessage } from '../../utils/errorMessage';
import type { PatientDto, PrescriptionList } from '../../types';

type Department = 'surgery' | 'rehab';

interface PatientRow {
  patient: PatientDto;
  list: PrescriptionList | null;
}

type SortKey = 'name' | 'doc';

export default function NursePrescriptionPage() {
  const theme = useTheme();
  const navigate = useNavigate();

  const [dept, setDept] = useState<Department>(
    () => (localStorage.getItem('nursePrescDept') as Department) || 'surgery',
  );
  const [rows, setRows] = useState<PatientRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  const loadPatients = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await patientApi.search('');
      const deptPatients = res.data.filter(p => {
        if (dept === 'surgery') return p.departmentId === 2;
        return p.departmentId === 1;
      });

      setRows(deptPatients.map(p => ({ patient: p, list: null })));

      for (const p of deptPatients) {
        try {
          const lr = await prescriptionApi.getByPatient(p.id);
          const active = lr.data.find(l => l.status !== 'Finished');
          setRows(prev =>
            prev.map(r => r.patient.id === p.id ? { ...r, list: active ?? lr.data[0] ?? null } : r)
          );
        } catch {
          // prescription not found, leave null
        }
      }
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося завантажити пацієнтів'));
    } finally {
      setLoading(false);
    }
  }, [dept]);

  useEffect(() => { loadPatients(); }, [loadPatients]);

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

  const filteredRows = useMemo(() => {
    let list = rows;
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(r =>
        r.patient.fullName.toLowerCase().includes(q) ||
        String(r.patient.id).includes(q) ||
        (r.patient.externalId1 ?? '').toLowerCase().includes(q),
      );
    }
    list = [...list].sort((a, b) => {
      let cmp = 0;
      if (sortKey === 'name') {
        cmp = a.patient.fullName.localeCompare(b.patient.fullName, 'uk');
      } else {
        cmp = (a.list?.documentName ?? '').localeCompare(b.list?.documentName ?? '', 'uk');
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return list;
  }, [rows, search, sortKey, sortDir]);

  return (
    <Box>
      {/* header */}
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

      {/* error */}
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {/* search */}
      <TextField
        size="small"
        placeholder="Пошук пацієнта за ПІБ, ID або № картки"
        value={search}
        onChange={e => setSearch(e.target.value)}
        sx={{ mb: 2, maxWidth: 400 }}
      />

      {/* table */}
      {loading ? (
        <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={{ width: 60 }}>ID</TableCell>
                <TableCell>
                  <TableSortLabel active={sortKey === 'name'} direction={sortDir}
                    onClick={() => toggleSort('name')}>
                    Пацієнт
                  </TableSortLabel>
                </TableCell>
                <TableCell>
                  <TableSortLabel active={sortKey === 'doc'} direction={sortDir}
                    onClick={() => toggleSort('doc')}>
                    Листок призначень
                  </TableSortLabel>
                </TableCell>
                <TableCell sx={{ width: 100 }}>Статус</TableCell>
                <TableCell sx={{ width: 120 }}>Дії</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredRows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center">
                    <Typography color="text.secondary" sx={{ py: 2 }}>
                      {search ? 'Пацієнтів не знайдено' : 'Немає пацієнтів у відділенні'}
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                filteredRows.map(row => (
                  <TableRow key={row.patient.id} hover>
                    <TableCell>{row.patient.id}</TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {row.patient.fullName}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {row.patient.externalId1}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {row.list ? (
                        <Typography variant="body2">{row.list.documentName}</Typography>
                      ) : (
                        <Typography variant="caption" color="text.secondary">—</Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      {row.list ? (
                        <Chip
                          label={row.list.status === 'Finished' ? 'Закрито' : 'Відкрито'}
                          color={row.list.status === 'Finished' ? 'success' : 'info'}
                          size="small"
                        />
                      ) : (
                        <Chip label="—" size="small" variant="outlined" />
                      )}
                    </TableCell>
                    <TableCell>
                      {row.list && (
                        <Button size="small" variant="outlined"
                          onClick={() => navigate(`/prescriptions/nurse/${row.list!.id}`)}>
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
    </Box>
  );
}
