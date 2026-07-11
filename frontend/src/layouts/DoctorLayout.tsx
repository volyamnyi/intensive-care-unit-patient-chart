import { Outlet, useNavigate, Link as RouterLink } from 'react-router-dom';
import { AppBar, Toolbar, Typography, Button, Box, Container, IconButton, Menu, MenuItem } from '@mui/material';
import { AccountCircle } from '@mui/icons-material';
import { useState } from 'react';
import { useAuth } from '../services/AuthContext';

export default function DoctorLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#FAFAF8' }}>
      <AppBar position="static" sx={{ bgcolor: '#1F1F1F' }}>
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1, fontFamily: '"Rubik", sans-serif', fontWeight: 700, letterSpacing: '-0.3px' }}>
            <RouterLink to="/doctor" style={{ color: 'white', textDecoration: 'none' }}>
              Карта інтенсивної терапії
            </RouterLink>
          </Typography>

          <Button
            component={RouterLink} to="/doctor"
            sx={{ color: 'rgba(255,255,255,0.85)', mr: 1, fontFamily: '"Rubik", sans-serif', fontWeight: 500, '&:hover': { color: '#fff', bgcolor: 'rgba(255,255,255,0.1)' } }}
          >
            Пацієнти
          </Button>

          <IconButton aria-label="Меню користувача" sx={{ color: 'rgba(255,255,255,0.85)', '&:hover': { color: '#fff', bgcolor: 'rgba(255,255,255,0.1)' } }} onClick={(e) => setAnchorEl(e.currentTarget)}>
            <AccountCircle />
          </IconButton>
          <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled sx={{ fontFamily: '"Rubik", sans-serif' }}>{user?.fullName}</MenuItem>
            <MenuItem disabled sx={{ fontFamily: '"Rubik", sans-serif', color: 'text.secondary' }}>Лікар</MenuItem>
            <MenuItem onClick={handleLogout}>Вийти</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ mt: 3, mb: 3 }} className="fade-in-up">
        <Outlet />
      </Container>
    </Box>
  );
}
