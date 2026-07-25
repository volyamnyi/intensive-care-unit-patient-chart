import { Outlet, useNavigate, Link as RouterLink } from 'react-router-dom';
import { AppBar, Toolbar, Typography, Button, Box, Container, IconButton, Menu, MenuItem, useTheme } from '@mui/material';
import { AccountCircle, DarkMode, LightMode } from '@mui/icons-material';
import { useState } from 'react';
import { useAuth } from '../services/AuthContext';
import { useThemeMode } from '../styles/ThemeContext';

function roleLabel(role?: string) {
  if (role === 'HEAD_OF_DEPARTMENT') return 'Завідувач відділення';
  if (role === 'DOCTOR') return 'Лікар';
  return role ?? '';
}

export default function DoctorLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const theme = useTheme();
  const { toggleTheme, mode } = useThemeMode();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: theme.palette.background.default }}>
      <AppBar position="static">
        <Toolbar>
          <Box component={RouterLink} to="/doctor" sx={{ display: 'flex', alignItems: 'center', gap: 1.5, textDecoration: 'none', flexGrow: 1 }}>
            <Box
              component="img"
              src={mode === 'dark' ? '/superhumans-white.svg' : '/superhumans.svg'}
              alt="Superhumans"
              sx={{ height: 36, width: 'auto' }}
            />
            <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
              <Typography sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, fontSize: 18, color: theme.palette.text.primary, lineHeight: 1.2, letterSpacing: '-0.5px' }}>
                ВАІТ
              </Typography>
              <Typography sx={{ fontFamily: '"Mulish", sans-serif', fontSize: 10, color: theme.palette.text.secondary, lineHeight: 1, letterSpacing: '0.3px', textTransform: 'uppercase' }}>
                Карта інтенсивної терапії
              </Typography>
            </Box>
          </Box>

          <Button
            component={RouterLink} to="/doctor"
            sx={{ color: theme.palette.text.secondary, mr: 1, fontFamily: '"Rubik", sans-serif', fontWeight: 600, fontSize: 13, borderRadius: 50, px: 2, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.08)' } }}
          >
            Пацієнти
          </Button>
          <Button
            component={RouterLink} to="/doctor/prescriptions"
            sx={{ color: theme.palette.text.secondary, mr: 1, fontFamily: '"Rubik", sans-serif', fontWeight: 600, fontSize: 13, borderRadius: 50, px: 2, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.08)' } }}
          >
            Призначення
          </Button>
          {user?.role === 'HEAD_OF_DEPARTMENT' && (
            <Button
              component={RouterLink} to="/doctor/department"
              sx={{ color: theme.palette.text.secondary, mr: 1, fontFamily: '"Rubik", sans-serif', fontWeight: 600, fontSize: 13, borderRadius: 50, px: 2, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.08)' } }}
            >
              Відділення
            </Button>
          )}

          <IconButton
            aria-label="Переключити тему"
            onClick={toggleTheme}
            sx={{ color: theme.palette.text.secondary, mr: 0.5, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.1)' } }}
          >
            {mode === 'dark' ? <LightMode /> : <DarkMode />}
          </IconButton>
          <IconButton aria-label="Меню користувача" sx={{ color: theme.palette.text.secondary, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.08)' } }} onClick={(e) => setAnchorEl(e.currentTarget)}>
            <AccountCircle />
          </IconButton>
          <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled sx={{ fontFamily: '"Rubik", sans-serif' }}>{user?.fullName}</MenuItem>
            <MenuItem disabled sx={{ fontFamily: '"Rubik", sans-serif', color: 'text.secondary' }}>{roleLabel(user?.role)}</MenuItem>
            <MenuItem onClick={handleLogout}>Вийти</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }} className="fade-in-up">
        <Outlet />
      </Container>
    </Box>
  );
}
