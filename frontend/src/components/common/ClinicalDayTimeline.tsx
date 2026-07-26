import { useEffect, useRef } from 'react';
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
    OPEN: isDark ? '#1E2A1E' : '#F0F7F0',
    NURSE_SIGNED: isDark ? '#1E2633' : '#F0F4FF',
    DOCTOR_SIGNED: isDark ? '#1E2A1E' : '#F0FAF0',
    CLOSED: isDark ? '#1A1A1A' : '#F5F5F0',
    REOPENED: isDark ? '#2A2420' : '#FFF8F0',
  };
}

function getStatusBorderColor(theme: import('@mui/material').Theme): Record<string, string> {
  const isDark = theme.palette.mode === 'dark';
  return {
    OPEN: isDark ? '#4CAF50' : '#81C784',
    NURSE_SIGNED: isDark ? '#42A5F5' : '#64B5F6',
    DOCTOR_SIGNED: isDark ? '#4CAF50' : '#81C784',
    CLOSED: isDark ? '#2A2A2A' : '#D0CEC9',
    REOPENED: isDark ? '#FF9800' : '#FFB74D',
  };
}

function getStatusLabel(status: string): string {
  switch (status) {
    case 'OPEN': return 'Відкрито';
    case 'NURSE_SIGNED': return 'Підписано м/с';
    case 'DOCTOR_SIGNED': return 'Підписано лікарем';
    case 'CLOSED': return 'Закрито';
    case 'REOPENED': return 'Перевідкрито';
    default: return status;
  }
}

export default function ClinicalDayTimeline({ days, selectedDayId, onSelectDay }: ClinicalDayTimelineProps) {
  const theme = useTheme();
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!selectedDayId || !containerRef.current) return;
    const el = containerRef.current.querySelector<HTMLElement>(`[data-day-id="${selectedDayId}"]`);
    if (el && typeof el.scrollIntoView === 'function') {
      el.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    }
  }, [selectedDayId]);

  if (days.length === 0) {
    return (
      <Box sx={{ py: 2, textAlign: 'center' }}>
        <Typography color="text.secondary" sx={{ fontSize: 13 }}>
          Немає клінічних днів
        </Typography>
      </Box>
    );
  }

  const sortedDays = [...days].sort((a, b) => a.dayNumber - b.dayNumber);

  return (
    <Box ref={containerRef} sx={{ display: 'flex', gap: 1, overflow: 'auto', py: 1 }}>
      {sortedDays.map((day) => {
        const isSelected = day.id === selectedDayId;
        return (
          <Box
            key={day.id}
            data-day-id={day.id}
            onClick={() => onSelectDay(day)}
            tabIndex={0}
            role="button"
            aria-current={isSelected ? 'true' : undefined}
            sx={{
              minWidth: 90,
              textAlign: 'center',
              py: 1.25,
              px: 1.5,
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
              Доба {day.dayNumber}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.25 }}>
              {new Date(day.startDateTime).toLocaleDateString('uk-UA', { day: 'numeric', month: 'short' })}
            </Typography>
            <Typography
              variant="caption"
              sx={{
                display: 'block', mt: 0.5, fontSize: 9, fontWeight: 600,
                color: day.status === 'OPEN' ? '#4CAF50'
                  : day.status === 'NURSE_SIGNED' ? '#42A5F5'
                  : day.status === 'DOCTOR_SIGNED' ? '#2E7D32'
                  : day.status === 'REOPENED' ? '#FF9800'
                  : 'text.secondary',
              }}
            >
              {getStatusLabel(day.status)}
            </Typography>
          </Box>
        );
      })}
    </Box>
  );
}
