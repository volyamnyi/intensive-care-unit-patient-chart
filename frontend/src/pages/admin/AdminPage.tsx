import { useState, useEffect } from 'react';
import { Outlet, useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box, AppBar, Toolbar, Typography, Container, IconButton, Menu, MenuItem,
  Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, CircularProgress,
} from '@mui/material';
import { AccountCircle } from '@mui/icons-material';
import { useAuth } from '../../services/AuthContext';
import { userApi } from '../../api/endpoints';
import type { User } from '../../types';

export default function AdminPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [doctors, setDoctors] = useState<User[]>([]);
  const [nurses, setNurses] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  const handleLogout = () => { logout(); navigate('/login'); };

  useEffect(() => {
    Promise.all([userApi.getDoctors(), userApi.getNurses()])
      .then(([d, n]) => { setDoctors(d.data); setNurses(n.data); })
      .finally(() => setLoading(false));
  }, []);

  const roleLabel = (u: User) => u.role === 'DOCTOR' ? 'Лікар'
    : u.role === 'NURSE' ? 'Медсестра'
    : u.role === 'HEAD_OF_DEPARTMENT' ? 'Завідувач відділення'
    : u.role === 'ADMINISTRATOR' ? 'Адміністратор' : u.role;

  const renderTable = (title: string, rows: User[]) => (
    <Paper sx={{ p: 2.5, mb: 3, border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
      <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>{title}</Typography>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>ПІБ</TableCell>
              <TableCell>Логін</TableCell>
              <TableCell>Роль</TableCell>
              <TableCell>Email</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((u) => (
              <TableRow key={u.id}>
                <TableCell sx={{ fontWeight: 600 }}>{u.fullName}</TableCell>
                <TableCell>{u.login}</TableCell>
                <TableCell>{roleLabel(u)}</TableCell>
                <TableCell>{u.email}</TableCell>
              </TableRow>
            ))}
            {rows.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} align="center" sx={{ py: 3, color: '#5A5A5A' }}>Немає даних</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#FAFAF8' }}>
      <AppBar position="static" sx={{ bgcolor: '#1F1F1F' }}>
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1, fontFamily: '"Rubik", sans-serif', fontWeight: 700 }}>
            <RouterLink to="/admin" style={{ color: 'white', textDecoration: 'none' }}>
              Панель адміністратора
            </RouterLink>
          </Typography>
          <IconButton aria-label="Меню користувача" sx={{ color: 'rgba(255,255,255,0.85)' }} onClick={(e) => setAnchorEl(e.currentTarget)}>
            <AccountCircle />
          </IconButton>
          <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled>{user?.fullName}</MenuItem>
            <MenuItem onClick={handleLogout}>Вийти</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ mt: 3, mb: 3 }} className="fade-in-up">
        <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700, mb: 3 }}>
          Користувачі системи
        </Typography>
        {loading ? (
          <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
        ) : (
          <>
            {renderTable('Лікарі', doctors)}
            {renderTable('Медсестри', nurses)}
          </>
        )}
        <Outlet />
      </Container>
    </Box>
  );
}
