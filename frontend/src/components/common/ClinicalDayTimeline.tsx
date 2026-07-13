import { Box, Typography } from '@mui/material';
import type { ClinicalDay } from '../../types';

interface ClinicalDayTimelineProps {
  days: ClinicalDay[];
  selectedDayId?: string;
  onSelectDay: (day: ClinicalDay) => void;
}

const statusColors: Record<string, string> = {
  OPEN: '#FFF5F3',
  NURSE_SIGNED: '#FFF5F3',
  DOCTOR_SIGNED: '#F0F7F3',
  CLOSED: '#F0F7F3',
  REOPENED: '#FFFBE6',
};

const statusBorderColors: Record<string, string> = {
  OPEN: '#FFD6CC',
  NURSE_SIGNED: '#FFD6CC',
  DOCTOR_SIGNED: '#D4E8DE',
  CLOSED: '#D4E8DE',
  REOPENED: '#FFE58F',
};

export default function ClinicalDayTimeline({ days, selectedDayId, onSelectDay }: ClinicalDayTimelineProps) {
  if (days.length === 0) {
    return <Typography color="text.secondary">Немає клінічних днів</Typography>;
  }

  return (
    <Box sx={{ display: 'flex', gap: 1, overflow: 'auto', py: 1 }}>
      {days.map((day) => {
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
              bgcolor: statusColors[day.status] || '#FAFAF8',
              border: isSelected ? '2px solid #8AAB9E' : (statusBorderColors[day.status] || '1px solid #E8E6E1'),
              fontWeight: 700,
              fontSize: 13,
              transition: 'all 0.2s ease',
              '&:hover': { transform: 'translateY(-2px)', boxShadow: '0 2px 8px rgba(0,0,0,0.08)' },
            }}
          >
            <Typography variant="body2" sx={{ fontWeight: 700 }}>
              Доба {day.dayNumber}
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
