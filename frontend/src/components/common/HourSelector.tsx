import { cn } from '@/lib/utils';
import { useThemeMode } from '../../styles/ThemeContext';

interface HourSelectorProps {
  hours: number[];
  currentHour: number;
  onSelect: (hour: number) => void;
  filledHours?: number[];
  disabledHours?: number[];
}

export default function HourSelector({ hours, currentHour, onSelect, filledHours = [], disabledHours = [] }: HourSelectorProps) {
  const { mode } = useThemeMode();
  const isDark = mode === 'dark';
  const currentIdx = hours.indexOf(currentHour);
  const disabledSet = new Set(disabledHours);

  return (
    <div className="flex gap-0.5 overflow-auto py-1">
      {hours.map((h, i) => {
        const isFilled = filledHours.includes(h);
        const isDisabled = disabledSet.has(h);
        let bg = '';
        let textColor = isDark ? '#FFFFFF' : '#1F1F1F';
        let border = isDark ? '1px solid #2A2A2A' : '1px solid #D0CEC9';

        if (isDisabled) {
          bg = isDark ? '#1A1A1A' : '#F0F0F0';
          textColor = isDark ? '#555555' : '#AAAAAA';
          border = isDark ? '1px solid #2A2A2A' : '1px solid #E0E0E0';
        } else if (i === currentIdx) {
          bg = '#FF5F33';
          textColor = '#FFFFFF';
          border = '2px solid #FF8C66';
        } else if (i < currentIdx) {
          if (isDark) {
            bg = isFilled ? '#1A3A2A' : '#3A1A1A';
            textColor = isFilled ? '#4CAF50' : '#FF5252';
          } else {
            bg = isFilled ? '#E8F5E9' : '#FFEBEE';
            textColor = isFilled ? '#2E7D32' : '#C62828';
          }
          border = isDark ? '1px solid #2A2A2A' : '1px solid #D0CEC9';
        }

        return (
          <div
            key={h}
            onClick={isDisabled ? undefined : () => onSelect(h)}
            className={cn(
              'min-w-12 text-center py-1 rounded-lg font-bold text-xs transition-all duration-200',
              isDisabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer',
              !isDisabled && i !== currentIdx && 'hover:-translate-y-0.5 hover:shadow-md'
            )}
            style={{ backgroundColor: bg, color: textColor, border }}
          >
            {h}:00
            <br />
            {isDisabled ? '\uD83D\uDD12' : i < currentIdx && (isFilled ? '\u2713' : '\u2717')}
            {!isDisabled && i === currentIdx && '\u25B6'}
          </div>
        );
      })}
    </div>
  );
}
