import { Box, Typography, useTheme } from '@mui/material';
import type { ClinicalDay } from '../../types';

interface ClinicalDayTimelineProps {
  days: ClinicalDay[];
  selectedDayId?: string;
  onSelectDay: (day: ClinicalDay) => void;
}

function getStatusColor(theme: import('@mui/material').Theme): Record<string, string> {
  const isDark = theme.palette.mode === 'dark';
  return {
    OPEN: isDark ? '#1A1A1A' : '#F5F5F0',
    NURSE_SIGNED: isDark ? '#1A1A1A' : '#F5F5F0',
    DOCTOR_SIGNED: isDark ? '#1A1A1A' : '#F5F5F0',
    CLOSED: isDark ? '#1A1A1A' : '#F5F5F0',
    REOPENED: isDark ? '#2A2A2A' : '#E8E6E1',
  };
}

function getStatusBorderColor(theme: import('@mui/material').Theme): Record<string, string> {
  const isDark = theme.palette.mode === 'dark';
  return {
    OPEN: isDark ? '#2A2A2A' : '#D0CEC9',
    NURSE_SIGNED: isDark ? '#2A2A2A' : '#D0CEC9',
    DOCTOR_SIGNED: isDark ? '#2A2A2A' : '#D0CEC9',
    CLOSED: isDark ? '#2A2A2A' : '#D0CEC9',
    REOPENED: isDark ? '#2A2A2A' : '#C0BEB9',
  };
}

export default function ClinicalDayTimeline({ days, selectedDayId, onSelectDay }: ClinicalDayTimelineProps) {
  const theme = useTheme();
  if (days.length === 0) {
    return <Typography color="text.secondary">Немає клінічних днів</Typography>;
  }

  const sortedDays = [...days].sort((a, b) => a.dayNumber - b.dayNumber);

  return (
    <Box sx={{ display: 'flex', gap: 1, overflow: 'auto', py: 1 }}>
      {sortedDays.map((day) => {
        const isSelected = day.id === selectedDayId;
        return (
          <Box
            key={day.id}
            onClick={() => onSelectDay(day)}
            sx={{
              minWidth: 80,
              textAlign: 'center',
              py: 1.5,
              px: 1,
              borderRadius: 2,
              cursor: 'pointer',
              bgcolor: getStatusColor(theme)[day.status] || (theme.palette.mode === 'dark' ? '#1A1A1A' : '#F5F5F0'),
              border: isSelected ? '2px solid #FF5F33' : `1px solid ${getStatusBorderColor(theme)[day.status] || (theme.palette.mode === 'dark' ? '#2A2A2A' : '#D0CEC9')}`,
              fontWeight: 700,
              fontSize: 13,
              transition: 'all 0.2s ease',
              '&:hover': { transform: 'translateY(-2px)', boxShadow: '0 2px 8px rgba(0,0,0,0.08)' },
            }}
          >
            <Typography variant="body2" sx={{ fontWeight: 700 }}>
              День {day.dayNumber}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {new Date(day.startDateTime).toLocaleDateString('uk-UA', { day: 'numeric', month: 'short' })}
            </Typography>
          </Box>
        );
      })}
    </Box>
  );
}
