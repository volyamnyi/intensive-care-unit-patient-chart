import { Box, Typography, Paper, Chip, useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import ClinicalDayTimeline from '../common/ClinicalDayTimeline';
import IntensiveCareCard from './IntensiveCareCard';
import type { DashboardProps } from './dashboardTypes';

export default function NurseDashboard(props: DashboardProps) {
  const {
    episode, clinicalDays, selectedDay, onSelectDay,
    records, orders, balanceItems,
    isLocked, isNurse, user,
  } = props;

  const theme = useTheme();
  const { t } = useTranslation();
  const isDark = theme.palette.mode === 'dark';
  const bd = `1px solid ${isDark ? '#2A2A2A' : '#E0DED9'}`;
  const paperSx = {
    p: 1.5, border: bd, borderRadius: 2,
    bgcolor: isDark ? '#141414' : '#FFFFFF',
    boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 4px rgba(0,0,0,0.03)',
  };

  const dayChipColor = (status: string) => {
    if (status === 'OPEN' || status === 'REOPENED') return 'warning';
    if (status === 'NURSE_SIGNED') return 'info';
    if (status === 'DOCTOR_SIGNED') return 'success';
    return 'default';
  };

  return (
    <Box>
      {/* Top bar: episode + day info */}
      <Paper sx={{ ...paperSx, mb: 1.5, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, fontSize: 16 }}>
            {episode.patientName || 'Patient'}
          </Typography>
          {selectedDay?.weightKg && (
            <Chip label={`${selectedDay.weightKg} kg`} size="small" variant="outlined" sx={{ fontWeight: 600, fontSize: 11 }} />
          )}
          {episode.ward && (
            <Chip label={[episode.ward, episode.bedNumber].filter(Boolean).join(' / ')} size="small" variant="outlined" sx={{ fontWeight: 600, fontSize: 11 }} />
          )}
          {selectedDay && (
            <Chip label={`${t('doctor.patientDay.dayPrefix')}${selectedDay.dayNumber}`} size="small" variant="outlined" sx={{ fontWeight: 600, fontSize: 12 }} />
          )}
          <Chip
            label={selectedDay?.status === 'OPEN' ? t('doctor.patientDay.statusOpen')
              : selectedDay?.status === 'NURSE_SIGNED' ? t('doctor.patientDay.statusNurseSigned')
              : selectedDay?.status === 'DOCTOR_SIGNED' ? t('doctor.patientDay.statusDoctorSigned')
              : selectedDay?.status === 'REOPENED' ? t('doctor.patientDay.statusReopened')
              : t('doctor.patientDay.statusClosed')}
            color={dayChipColor(selectedDay?.status ?? '')}
            size="small"
            sx={{ fontWeight: 600, fontSize: 11 }}
          />
        </Box>
        <Typography variant="caption" color="text.secondary">
          {t('doctor.patientDay.episodeChipPrefix', { id: episode.id?.slice(0, 8) })}
        </Typography>
      </Paper>

      {/* Clinical day timeline */}
      <Paper sx={{ ...paperSx, mb: 1.5 }}>
        <ClinicalDayTimeline days={clinicalDays} selectedDayId={selectedDay?.id} onSelectDay={onSelectDay} />
      </Paper>

      {/* Single dynamic ICU card — nurse inline edits losses + order execution */}
      <IntensiveCareCard
        episode={episode}
        selectedDay={selectedDay}
        records={records}
        orders={orders}
        balanceItems={balanceItems}
        isNurse={isNurse}
        isLocked={isLocked}
        user={user ?? null}
      />
    </Box>
  );
}
