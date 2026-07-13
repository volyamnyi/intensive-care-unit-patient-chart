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
        let textColor = '#1F1F1F';
        let border = '1px solid #E8E6E1';

        if (i === currentIdx) {
          bgcolor = '#B6CECA';
          textColor = '#1F1F1F';
          border = '2px solid #8AAB9E';
        } else if (i < currentIdx) {
          bgcolor = isFilled ? '#F0F7F3' : '#FFF5F3';
          textColor = isFilled ? '#1F6B4C' : '#C42E1A';
          border = isFilled ? '1px solid #D4E8DE' : '1px solid #FFD6CC';
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
