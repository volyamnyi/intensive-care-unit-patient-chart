import { Box } from '@mui/material';

interface HourSelectorProps {
  hours: number[];
  currentHour: number;
  onSelect: (hour: number) => void;
  filledHours?: number[];
}

export default function HourSelector({ hours, currentHour, onSelect, filledHours = [] }: HourSelectorProps) {
  const currentIdx = hours.indexOf(currentHour);

  return (
    <Box sx={{ display: 'flex', gap: 0.5, overflow: 'auto', py: 1 }}>
      {hours.map((h, i) => {
        const isFilled = filledHours.includes(h);
        let bgcolor = '';
        let textColor = '#FFFFFF';
        let border = '1px solid #2A2A2A';

        if (i === currentIdx) {
          bgcolor = '#FF5F33';
          textColor = '#FFFFFF';
          border = '2px solid #FF8C66';
        } else if (i < currentIdx) {
          bgcolor = isFilled ? '#1A3A2A' : '#3A1A1A';
          textColor = isFilled ? '#4CAF50' : '#FF5252';
          border = isFilled ? '1px solid #2A2A2A' : '1px solid #2A2A2A';
        }

        return (
          <Box
            key={h}
            onClick={() => onSelect(h)}
            sx={{
              minWidth: 48,
              textAlign: 'center',
              py: 1,
              borderRadius: 2,
              cursor: 'pointer',
              bgcolor,
              color: textColor,
              fontWeight: 700,
              fontSize: 13,
              border,
              transition: 'all 0.2s ease',
              '&:hover': { transform: 'translateY(-1px)', boxShadow: '0 2px 8px rgba(0,0,0,0.08)' },
            }}
          >
            {h}:00
            <br />
            {i < currentIdx && (isFilled ? '\u2713' : '\u2717')}
            {i === currentIdx && '\u25B6'}
          </Box>
        );
      })}
    </Box>
  );
}
