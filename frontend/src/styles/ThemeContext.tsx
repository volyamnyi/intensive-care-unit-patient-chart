import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { createTheme } from '@mui/material/styles';
import type { Theme } from '@mui/material/styles';

type ThemeMode = 'light' | 'dark';

interface ThemeContextType {
  mode: ThemeMode;
  toggleTheme: () => void;
  theme: Theme;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

const baseTheme = {
  typography: {
    fontFamily: '"Inter", "Rubik", "Mulish", "Helvetica", "Arial", sans-serif',
    h1: { fontFamily: '"Rubik", sans-serif', fontWeight: 800, letterSpacing: '-0.03em' },
    h2: { fontFamily: '"Rubik", sans-serif', fontWeight: 800, letterSpacing: '-0.02em' },
    h3: { fontFamily: '"Rubik", sans-serif', fontWeight: 700, letterSpacing: '-0.02em' },
    h4: { fontFamily: '"Rubik", sans-serif', fontWeight: 700 },
    h5: { fontFamily: '"Rubik", sans-serif', fontWeight: 700, letterSpacing: '-0.01em' },
    h6: { fontFamily: '"Rubik", sans-serif', fontWeight: 700 },
    subtitle1: { fontFamily: '"Rubik", sans-serif', fontWeight: 600 },
    subtitle2: { fontFamily: '"Rubik", sans-serif', fontWeight: 600 },
    button: { fontFamily: '"Rubik", sans-serif', fontWeight: 600, textTransform: 'none' },
    caption: { fontFamily: '"Mulish", sans-serif' },
  },
  shape: { borderRadius: 12 },
};

function createThemeWithMode(mode: ThemeMode): Theme {
  const isDark = mode === 'dark';
  const bgMain = isDark ? '#0D0D0D' : '#FAFAF8';
  const bgPaper = isDark ? '#1A1A1A' : '#FFFFFF';
  const textPri = isDark ? '#FFFFFF' : '#1F1F1F';
  const textSec = isDark ? '#A0A0A0' : '#5A5A5A';
  const border = isDark ? '#2A2A2A' : '#E8E6E1';
  const inputBg = isDark ? '#141414' : '#FAFAF8';
  return createTheme({
    ...baseTheme,
    palette: {
      mode,
      primary: { main: '#FF5F33' },
      secondary: { main: '#FF8C66', contrastText: '#FFFFFF' },
      background: { default: bgMain, paper: bgPaper },
      text: { primary: textPri, secondary: textSec },
      divider: border,
      success: { main: '#4CAF50' },
      error: { main: '#FF5252' },
      warning: { main: '#FFC107' },
      info: { main: '#64B5F6' },
    },
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: 50, padding: '10px 24px',
            transition: 'all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1)',
            fontWeight: 700, fontSize: 14,
            '&.MuiButton-containedPrimary': {
              boxShadow: 'none',
              background: 'linear-gradient(135deg, #FF5F33 0%, #FF8C66 100%)',
              '&:hover': {
                boxShadow: '0 8px 24px rgba(255, 95, 51, 0.4), 0 0 0 1px rgba(255, 95, 51, 0.2)',
                background: 'linear-gradient(135deg, #E8552E 0%, #FF7A4D 100%)',
                transform: 'translateY(-1px)',
              },
              '&:active': { transform: 'translateY(0)' },
            },
            '&.MuiButton-outlined': {
              borderWidth: 2, borderColor: '#FF5F33', color: '#FF5F33',
              '&:hover': {
                borderWidth: 2, borderColor: '#FF8C66', color: '#FFFFFF',
                bgcolor: 'rgba(255, 95, 51, 0.08)',
                transform: 'translateY(-1px)',
                boxShadow: '0 4px 12px rgba(255, 95, 51, 0.2)',
              },
              '&:active': { transform: 'translateY(0)' },
            },
            '&.MuiButton-text': { color: textPri, '&:hover': { color: '#FF8C66' } },
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: { borderRadius: 20, boxShadow: isDark ? '0 2px 20px rgba(0,0,0,0.3)' : '0 2px 16px rgba(0,0,0,0.06)', border: `1px solid ${border}`, transition: 'all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1)' },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: { borderRadius: 20, backgroundImage: 'none' },
        },
      },
      MuiTextField: {
        styleOverrides: {
          root: {
            '& .MuiOutlinedInput-root': {
              borderRadius: 12, transition: 'all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1)',
              backgroundColor: inputBg,
              '& fieldset': { borderColor: border },
              '&:hover fieldset': { borderColor: '#FF8C66' },
              '&.Mui-focused fieldset': { borderColor: '#FF5F33' },
              '& .MuiInputLabel-root': { color: textSec },
            },
          },
        },
      },
      MuiSelect: {
        styleOverrides: {
          root: { borderRadius: 12, backgroundColor: inputBg },
          icon: { color: textSec },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: { borderRadius: 8, fontWeight: 700, fontSize: 12 },
          outlined: { borderColor: '#4A4A4A' },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          root: { fontFamily: '"Mulish", sans-serif', borderBottomColor: border, padding: '14px 16px', color: isDark ? '#D0D0D0' : textPri },
          head: { fontFamily: '"Rubik", sans-serif', fontWeight: 600, color: textSec, fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.05em', padding: '12px 16px' },
        },
      },
      MuiTableRow: {
        styleOverrides: {
          hover: { '&:hover': { backgroundColor: isDark ? 'rgba(255, 255, 255, 0.03) !important' : 'rgba(0, 0, 0, 0.02) !important' } },
        },
      },
      MuiTableContainer: {
        styleOverrides: {
          root: { borderRadius: 16, border: `1px solid ${border}` },
        },
      },
      MuiTable: {
        styleOverrides: {
          root: { backgroundColor: isDark ? '#141414' : '#FFFFFF' },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: { backgroundImage: 'none', backgroundColor: isDark ? '#0D0D0D' : '#FFFFFF', borderBottom: `1px solid ${border}`, boxShadow: 'none' },
        },
      },
      MuiToolbar: {
        styleOverrides: {
          root: { minHeight: 64 },
        },
      },
      MuiTab: {
        styleOverrides: {
          root: { fontFamily: '"Rubik", sans-serif', fontWeight: 600, textTransform: 'none', fontSize: 14, color: textSec, '&.Mui-selected': { color: '#FF5F33' } },
        },
      },
      MuiTabs: {
        styleOverrides: {
          root: { borderBottom: `1px solid ${border}` },
          indicator: { backgroundColor: '#FF5F33', height: 3, borderRadius: 3 },
        },
      },
      MuiAlert: {
        styleOverrides: {
          root: { borderRadius: 12 },
        },
      },
      MuiMenu: {
        styleOverrides: {
          paper: { borderRadius: 12, backgroundColor: bgPaper, border: `1px solid ${border}` },
        },
      },
      MuiMenuItem: {
        styleOverrides: {
          root: { fontFamily: '"Rubik", sans-serif', fontSize: 14, '&:hover': { backgroundColor: isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.03)' } },
        },
      },
      MuiDialog: {
        styleOverrides: {
          paper: { borderRadius: 24, backgroundColor: bgPaper, border: `1px solid ${border}` },
        },
      },
    },
  }) as Theme;
}

export function ThemeModeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<ThemeMode>(() => {
    const saved = localStorage.getItem('themeMode');
    return (saved === 'light' || saved === 'dark') ? saved : 'dark';
  });

  const theme = createThemeWithMode(mode);

  useEffect(() => {
    localStorage.setItem('themeMode', mode);
  }, [mode]);

  const toggleTheme = () => {
    setMode((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  return (
    <ThemeContext.Provider value={{ mode, toggleTheme, theme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useThemeMode() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useThemeMode must be used within ThemeModeProvider');
  return ctx;
}
