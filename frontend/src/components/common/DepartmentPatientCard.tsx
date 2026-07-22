import { Card, CardContent, Typography, Chip, Box, useTheme } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import type { DepartmentPatient } from '../../types';

const statusConfig: Record<string, { label: string; color: 'default' | 'info' | 'warning' | 'success' | 'error' }> = {
  OPEN: { label: 'Відкрито', color: 'warning' },
  NURSE_SIGNED: { label: 'Підписано медсестрою', color: 'info' },
  DOCTOR_SIGNED: { label: 'Підписано лікарем', color: 'success' },
  CLOSED: { label: 'Закрито', color: 'default' },
  REOPENED: { label: 'Відкрито повторно', color: 'warning' },
};

function daysSinceLabel(days: number): string {
  if (days === 0) return 'Сьогодні';
  if (days === 1) return '1 день';
  if (days < 5) return `${days} дні`;
  return `${days} днів`;
}

interface Props {
  patient: DepartmentPatient;
}

export default function DepartmentPatientCard({ patient }: Props) {
  const navigate = useNavigate();
  const theme = useTheme();
  const dayConfig = patient.latestDayStatus ? statusConfig[patient.latestDayStatus] : null;

  return (
    <Card
      sx={{
        cursor: 'pointer',
        transition: 'box-shadow 0.2s, transform 0.2s',
        '&:hover': {
          boxShadow: theme.shadows[4],
          transform: 'translateY(-2px)',
        },
        borderLeft: 4,
        borderColor: dayConfig?.color === 'warning' ? theme.palette.warning.main
          : dayConfig?.color === 'info' ? theme.palette.info.main
          : dayConfig?.color === 'success' ? theme.palette.success.main
          : theme.palette.grey[400],
      }}
      onClick={() => navigate('/doctor/episode/' + patient.id)}
    >
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
            {patient.patientName ?? 'Невідомий пацієнт'}
          </Typography>
          {patient.latestDayNumber != null && (
            <Chip label={`День ${patient.latestDayNumber}`} size="small" variant="outlined" sx={{ ml: 1, flexShrink: 0 }} />
          )}
        </Box>

        <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mb: 1 }}>
          {patient.latestDayStatus && dayConfig && (
            <Chip label={dayConfig.label} size="small" color={dayConfig.color} />
          )}
          {patient.ward && patient.bedNumber && (
            <Chip label={`${patient.ward} / ${patient.bedNumber}`} size="small" variant="outlined" />
          )}
          <Chip label={daysSinceLabel(patient.daysSinceAdmission)} size="small" variant="outlined" />
        </Box>

        {patient.admissionDiagnosis && (
          <Typography variant="caption" sx={{ color: theme.palette.text.secondary, display: 'block', mb: 0.5 }}>
            {patient.admissionDiagnosis}
          </Typography>
        )}

        {patient.attendingDoctorName && (
          <Typography variant="caption" sx={{ color: theme.palette.text.secondary, display: 'block' }}>
            Лікар: {patient.attendingDoctorName}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}