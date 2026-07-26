import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ThemeProvider, CssBaseline, Box, Typography } from '@mui/material'
import { createTheme } from '@mui/material/styles'

const theme = createTheme({
  palette: {
    primary: { main: '#1565c0' },
    secondary: { main: '#2e7d32' },
  },
})

function LoginPage() {
  return (
    <Box sx={{ p: 4, textAlign: 'center' }}>
      <Typography variant="h4">Листок лікарських призначень</Typography>
      <Typography variant="body1" sx={{ mt: 2 }}>
        Модуль призначень — в розробці
      </Typography>
    </Box>
  )
}

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  )
}
