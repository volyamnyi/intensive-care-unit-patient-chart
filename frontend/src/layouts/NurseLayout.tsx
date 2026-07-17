import { Outlet, useNavigate } from 'react-router-dom';
import { AppBar, Toolbar, Typography, Box, Container, IconButton, Menu, MenuItem, useTheme } from '@mui/material';
import { AccountCircle, DarkMode, LightMode } from '@mui/icons-material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../services/AuthContext';
import { useThemeMode } from '../styles/ThemeContext';

export default function NurseLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const theme = useTheme();
  const { toggleTheme, mode } = useThemeMode();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const { t } = useTranslation();

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: theme.palette.background.default }}>
      <AppBar position="static">
        <Toolbar>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexGrow: 1 }}>
            <Box
              component="img"
              src={mode === 'dark' ? '/superhumans-white.svg' : '/superhumans.svg'}
              alt="Superhumans"
              sx={{ height: 36, width: 'auto' }}
            />
            <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
              <Typography sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, fontSize: 18, color: theme.palette.text.primary, lineHeight: 1.2, letterSpacing: '-0.5px' }}>
                {t('common.appTitle')}
              </Typography>
              <Typography sx={{ fontFamily: '"Mulish", sans-serif', fontSize: 10, color: theme.palette.text.secondary, lineHeight: 1, letterSpacing: '0.3px', textTransform: 'uppercase' }}>
                {t('common.appSubtitle')}
              </Typography>
            </Box>
          </Box>

          <IconButton
            aria-label={t('common.themeToggle')}
            onClick={toggleTheme}
            sx={{ color: theme.palette.text.secondary, mr: 0.5, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.1)' } }}
          >
            {mode === 'dark' ? <LightMode /> : <DarkMode />}
          </IconButton>
          <IconButton aria-label={t('common.userMenu')} sx={{ color: theme.palette.text.secondary, '&:hover': { color: '#FF8C66', bgcolor: 'rgba(255, 95, 51, 0.08)' } }} onClick={(e) => setAnchorEl(e.currentTarget)}>
            <AccountCircle />
          </IconButton>
          <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled sx={{ fontFamily: '"Rubik", sans-serif' }}>{user?.fullName}</MenuItem>
            <MenuItem disabled sx={{ fontFamily: '"Rubik", sans-serif', color: 'text.secondary' }}>{t('nurse.layout.role')}</MenuItem>
            <MenuItem onClick={handleLogout}>{t('common.logout')}</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }} className="fade-in-up">
        <Outlet />
      </Container>
    </Box>
  );
}
