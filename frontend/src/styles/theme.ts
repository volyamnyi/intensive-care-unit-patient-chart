import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: { main: '#1F1F1F' },
    secondary: { main: '#B6CECA', contrastText: '#1F1F1F' },
    background: { default: '#FAFAF8', paper: '#FFFFFF' },
    text: { primary: '#1F1F1F', secondary: '#5A5A5A' },
    divider: '#E8E6E1',
  },
  typography: {
    fontFamily: '"Mulish", "Helvetica", "Arial", sans-serif',
    h1: { fontFamily: '"Rubik", sans-serif', fontWeight: 700 },
    h2: { fontFamily: '"Rubik", sans-serif', fontWeight: 700 },
    h3: { fontFamily: '"Rubik", sans-serif', fontWeight: 600 },
    h4: { fontFamily: '"Rubik", sans-serif', fontWeight: 600 },
    h5: { fontFamily: '"Rubik", sans-serif', fontWeight: 600 },
    h6: { fontFamily: '"Rubik", sans-serif', fontWeight: 600 },
    subtitle1: { fontFamily: '"Rubik", sans-serif', fontWeight: 500 },
    subtitle2: { fontFamily: '"Rubik", sans-serif', fontWeight: 500 },
    button: { fontFamily: '"Rubik", sans-serif', fontWeight: 600, textTransform: 'none' },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiButton: {
      styleOverrides: {
        root: { borderRadius: 8, padding: '8px 20px', transition: 'all 0.2s ease' },
        contained: {
          boxShadow: 'none',
          '&:hover': { boxShadow: '0 4px 12px rgba(0,0,0,0.15)' },
        },
        outlined: {
          borderWidth: 2,
          '&:hover': { borderWidth: 2 },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: { borderRadius: 16, boxShadow: '0 2px 16px rgba(0,0,0,0.06)' },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { borderRadius: 12 },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: 8,
            '& fieldset': { borderColor: '#E8E6E1' },
            '&:hover fieldset': { borderColor: '#B6CECA' },
            '&.Mui-focused fieldset': { borderColor: '#1F1F1F' },
          },
        },
      },
    },
    MuiSelect: {
      styleOverrides: {
        root: { borderRadius: 8 },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { fontFamily: '"Mulish", sans-serif' },
        head: { fontFamily: '"Rubik", sans-serif', fontWeight: 600, color: '#5A5A5A' },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: { boxShadow: '0 1px 4px rgba(0,0,0,0.06)' },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: { fontFamily: '"Rubik", sans-serif', fontWeight: 600, textTransform: 'none' },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { borderRadius: 8, fontWeight: 600 },
      },
    },
    MuiTabs: {
      styleOverrides: {
        indicator: { backgroundColor: '#1F1F1F' },
      },
    },
  },
});
