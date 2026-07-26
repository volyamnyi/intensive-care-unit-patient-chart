import { Outlet, useNavigate, Link as RouterLink, useLocation } from 'react-router-dom';
import { AppBar, Toolbar, Typography, Button, Box, Container, IconButton, Menu, MenuItem, useTheme } from '@mui/material';
import { AccountCircle } from '@mui/icons-material';
import { useState, useMemo } from 'react';
import { useAuth } from '../services/AuthContext';
import { useThemeMode } from '../styles/ThemeContext';
import ThemeToggle from '../components/common/ThemeToggle';

function roleLabel(role?: string) {
  if (role === 'HEAD_OF_DEPARTMENT') return 'Завідувач відділення';
  if (role === 'DOCTOR') return 'Лікар';
  if (role === 'NURSE') return 'Медсестра';
  if (role === 'ADMINISTRATOR') return 'Адміністратор';
  if (role === 'AUDITOR') return 'Аудитор';
  return role ?? '';
}

interface AppInfo {
  title: string;
  subtitle: string;
  homePath: string;
}

function useAppInfo(): AppInfo {
  const { pathname } = useLocation();
  return useMemo(() => {
    if (pathname.startsWith('/prescriptions/nurse')) {
      return { title: 'Призначення', subtitle: 'Виконання лікарських призначень', homePath: '/prescriptions/nurse' };
    }
    if (pathname.startsWith('/prescriptions')) {
      return { title: 'Призначення', subtitle: 'Листок лікарських призначень', homePath: '/prescriptions/doctor' };
    }
    if (pathname.startsWith('/nurse')) {
      return { title: 'ВАІТ', subtitle: 'Карта інтенсивної терапії', homePath: '/nurse' };
    }
    if (pathname.startsWith('/doctor')) {
      return { title: 'ВАІТ', subtitle: 'Карта інтенсивної терапії', homePath: '/doctor' };
    }
    if (pathname.startsWith('/admin')) {
      return { title: 'Адмін', subtitle: 'Адміністративна панель', homePath: '/admin' };
    }
    return { title: 'Superhumans Lviv', subtitle: 'Вибір додатку', homePath: '/select' };
  }, [pathname]);
}

export default function GlobalLayout() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const theme = useTheme();
  const { mode } = useThemeMode();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const app = useAppInfo();

  const handleLogout = () => { logout(); navigate('/login'); };

  const isDoctorNurse = hasRole('DOCTOR') || hasRole('HEAD_OF_DEPARTMENT') || hasRole('NURSE');

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', bgcolor: theme.palette.background.default }}>
      <AppBar position="static">
        <Toolbar>
          <Box component={RouterLink} to={app.homePath} sx={{ display: 'flex', alignItems: 'center', gap: 1.5, textDecoration: 'none', flexGrow: 1 }}>
            <Box
              component="img"
              src={mode === 'dark' ? '/superhumans-white.svg' : '/superhumans.svg'}
              alt="Superhumans"
              sx={{ height: 36, width: 'auto' }}
            />
            <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
              <Typography sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, fontSize: 18, color: theme.palette.text.primary, lineHeight: 1.2, letterSpacing: '-0.5px' }}>
                {app.title}
              </Typography>
              <Typography sx={{ fontFamily: '"Mulish", sans-serif', fontSize: 10, color: theme.palette.text.secondary, lineHeight: 1, letterSpacing: '0.3px', textTransform: 'uppercase' }}>
                {app.subtitle}
              </Typography>
            </Box>
          </Box>

          {isDoctorNurse && (
            <Button
              component={RouterLink}
              to={hasRole('NURSE') ? '/nurse' : '/doctor'}
              sx={{ color: theme.palette.text.secondary, mr: 1, fontFamily: '"Rubik", sans-serif', fontWeight: 600, fontSize: 13, borderRadius: 50, px: 2, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.08)' } }}
            >
              Пацієнти
            </Button>
          )}
          {isDoctorNurse && (
            <Button
              component={RouterLink}
              to={hasRole('NURSE') ? '/prescriptions/nurse' : '/prescriptions/doctor'}
              sx={{ color: theme.palette.text.secondary, mr: 1, fontFamily: '"Rubik", sans-serif', fontWeight: 600, fontSize: 13, borderRadius: 50, px: 2, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.08)' } }}
            >
              Призначення
            </Button>
          )}
          {hasRole('HEAD_OF_DEPARTMENT') && (
            <Button
              component={RouterLink} to="/doctor/department"
              sx={{ color: theme.palette.text.secondary, mr: 1, fontFamily: '"Rubik", sans-serif', fontWeight: 600, fontSize: 13, borderRadius: 50, px: 2, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.08)' } }}
            >
              Відділення
            </Button>
          )}
          <Button
            component={RouterLink} to="/select"
            sx={{ color: theme.palette.text.secondary, mr: 1, fontFamily: '"Rubik", sans-serif', fontWeight: 600, fontSize: 13, borderRadius: 50, px: 2, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.08)' } }}
          >
            Додатки
          </Button>

          <ThemeToggle />
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

      <Box component="main" sx={{ flex: 1 }}>
        <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }} className="fade-in-up">
          <Outlet />
        </Container>
      </Box>
    </Box>
  );
}
