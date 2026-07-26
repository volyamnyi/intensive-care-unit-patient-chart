import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, TextField, CircularProgress, Alert, InputAdornment, useTheme, Paper, Grid, ToggleButtonGroup, ToggleButton } from '@mui/material';
import { Search as SearchIcon, People, Assignment, HowToReg, GppBad, TaskAlt, TableChart, Dashboard as DashboardIcon, MedicalServices, Hotel, Group } from '@mui/icons-material';
import { departmentApi } from '../../api/endpoints';
import EpisodeTable from '../../components/common/EpisodeTable';
import DepartmentPatientCard from '../../components/common/DepartmentPatientCard';
import type { DepartmentStats, DepartmentPatient } from '../../types';

const initialStats: DepartmentStats = {
  activePatients: 0,
  openDays: 0,
  nurseSignedDays: 0,
  doctorSignedDays: 0,
  closedDays: 0,
  totalBeds: 12,
  occupiedBeds: 0,
  activeDoctors: 0,
  activeNurses: 0,
};

export default function DepartmentDashboardPage() {
  const navigate = useNavigate();
  const theme = useTheme();
  const [patients, setPatients] = useState<DepartmentPatient[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [stats, setStats] = useState<DepartmentStats>(initialStats);
  const [viewMode, setViewMode] = useState<'cards' | 'table'>('cards');

  useEffect(() => {
    document.title = 'ВАІТ — Завідувач відділення';
  }, []);

  useEffect(() => {
    Promise.all([
      departmentApi.getPatients(),
      departmentApi.getStats(),
    ])
      .then(([patRes, statsRes]) => {
        setPatients(patRes.data);
        setStats(statsRes.data);
      })
      .catch(() => {
        setPatients([]);
      })
      .finally(() => setLoading(false));
  }, []);

  const filteredPatients = patients.filter((p) =>
    (p.patientName ?? '').toLowerCase().includes(search.toLowerCase())
  );

  const statCards = [
    { label: 'Активних пацієнтів', value: stats.activePatients, icon: <People />, color: '#1976d2' },
    { label: 'Відкритих днів', value: stats.openDays, icon: <Assignment />, color: '#ed6c02' },
    { label: 'Підписано медсестрою', value: stats.nurseSignedDays, icon: <HowToReg />, color: '#0288d1' },
    { label: 'Підписано лікарем', value: stats.doctorSignedDays, icon: <GppBad />, color: '#2e7d32' },
    { label: 'Зайнято ліжок', value: `${stats.occupiedBeds} / ${stats.totalBeds}`, icon: <Hotel />, color: '#5c6bc0' },
    { label: 'Активні лікарі', value: stats.activeDoctors, icon: <MedicalServices />, color: '#7b1fa2' },
    { label: 'Активні медсестри', value: stats.activeNurses, icon: <Group />, color: '#00796b' },
    { label: 'Закрито днів', value: stats.closedDays, icon: <TaskAlt />, color: '#455a64' },
  ];

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary, mb: 0.5 }}>
        Відділення анестезіології та інтенсивної терапії
      </Typography>
      <Typography variant="body2" sx={{ color: theme.palette.text.secondary, mb: 3 }}>
        Оглядова панель завідувача
      </Typography>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {statCards.map((card) => (
          <Grid size={{ xs: 12, sm: 6, md: 4, lg: 3 }} key={card.label}>
            <Paper elevation={1} sx={{ p: 2, textAlign: 'center', borderRadius: 2 }}>
              <Box sx={{ color: card.color, mb: 0.5 }}>{card.icon}</Box>
              <Typography variant="h4" sx={{ fontWeight: 800, fontFamily: '"Rubik", sans-serif', color: card.color }}>
                {card.value}
              </Typography>
              <Typography variant="caption" sx={{ color: theme.palette.text.secondary }}>
                {card.label}
              </Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2, alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
        <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700, color: theme.palette.text.primary }}>
          Активні пацієнти
        </Typography>
        <ToggleButtonGroup
          value={viewMode}
          exclusive
          onChange={(_, val) => val && setViewMode(val)}
          size="small"
        >
          <ToggleButton value="cards"><DashboardIcon fontSize="small" sx={{ mr: 0.5 }} />Картки</ToggleButton>
          <ToggleButton value="table"><TableChart fontSize="small" sx={{ mr: 0.5 }} />Таблиця</ToggleButton>
        </ToggleButtonGroup>
      </Box>

      <TextField
        fullWidth
        placeholder="Пошук пацієнта за ПІБ..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        sx={{ mb: 2 }}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon sx={{ color: theme.palette.text.secondary }} />
              </InputAdornment>
            ),
          },
        }}
      />

      {filteredPatients.length === 0 && !loading ? (
        <Alert severity="info">
          {search ? 'Немає пацієнтів за запитом' : 'Немає активних пацієнтів'}
        </Alert>
      ) : viewMode === 'cards' ? (
        <Grid container spacing={2}>
          {filteredPatients.map((p) => (
            <Grid size={{ xs: 12, sm: 6, lg: 4 }} key={p.id}>
              <DepartmentPatientCard patient={p} />
            </Grid>
          ))}
        </Grid>
      ) : (
        <EpisodeTable
          episodes={filteredPatients.map((p) => ({
            id: p.id,
            patientId: p.patientId,
            patientName: p.patientName,
            hospitalizationId: p.hospitalizationId,
            departmentId: p.departmentId,
            admissionDate: p.admissionDate,
            dischargeDate: p.dischargeDate,
            status: p.status as 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'ARCHIVED',
            heightCm: null,
            ward: p.ward,
            bedNumber: p.bedNumber,
            admissionDiagnosis: p.admissionDiagnosis,
            attendingDoctorId: p.attendingDoctorId,
            createdBy: 0,
            createdAt: p.admissionDate,
            updatedBy: 0,
            updatedAt: p.admissionDate,
            version: 0,
          }))}
          onSelect={(ep) => navigate('/doctor/episode/' + ep.id)}
        />
      )}
    </Box>
  );
}
