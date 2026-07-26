import { IconButton, Tooltip } from '@mui/material';
import { DarkMode, LightMode } from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';
import { useThemeMode } from '../../styles/ThemeContext';

export default function ThemeToggle() {
  const theme = useTheme();
  const { toggleTheme, mode } = useThemeMode();
  return (
    <Tooltip title={mode === 'dark' ? 'Світла тема' : 'Темна тема'}>
      <IconButton
        aria-label="Переключити тему"
        onClick={toggleTheme}
        sx={{
          color: theme.palette.text.secondary,
          '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.1)' },
        }}
      >
        {mode === 'dark' ? <LightMode /> : <DarkMode />}
      </IconButton>
    </Tooltip>
  );
}
