import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Typography, IconButton, Menu, MenuItem,
  Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, CircularProgress,
  Button, TextField, Tabs, Tab, Select, Chip, Dialog, DialogTitle, DialogContent, DialogActions,
  Alert, Snackbar,
} from '@mui/material';
import { AccountCircle, History, Refresh } from '@mui/icons-material';
import { useAuth } from '../../services/AuthContext';
import { auditApi, adminApi } from '../../api/endpoints';
import AuditLogTable from '../../components/common/AuditLogTable';
import { getErrorMessage } from '../../utils/errorMessage';
import type { User, AuditLog } from '../../types';

export default function AdminPage() {
  useEffect(() => { document.title = 'Адмін — Superhumans Lviv'; }, []);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [tabIndex, setTabIndex] = useState(0);
  const [users, setUsers] = useState<User[]>([]);
  const [stats, setStats] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);
  const [showAudit, setShowAudit] = useState(false);
  const [auditFilterEntity, setAuditFilterEntity] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleLogout = () => { logout(); navigate('/login'); };

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [usersRes, statsRes] = await Promise.all([
        adminApi.getUsers(),
        adminApi.getStats(),
      ]);
      setUsers(usersRes.data);
      setStats(statsRes.data);
    } catch { /* */ }
    setLoading(false);
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const loadAudit = useCallback(async () => {
    setAuditLoading(true);
    try {
      const params: Record<string, string> = {};
      if (auditFilterEntity) params.entity = auditFilterEntity;
      const res = await auditApi.list(params);
      setAuditLogs(res.data.content ?? res.data);
    } finally {
      setAuditLoading(false);
    }
  }, [auditFilterEntity]);

  useEffect(() => {
    if (showAudit) loadAudit();
  }, [showAudit, loadAudit]);

  const handleRoleChange = async (userId: number, role: string) => {
    try {
      const res = await adminApi.updateRole(userId, role);
      setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, role: res.data.role } : u)));
      await adminApi.getStats().then((r) => setStats(r.data));
    } catch { /* */ }
  };

  const handlePermissionToggle = async (userId: number, permission: string, hasIt: boolean) => {
    try {
      const res = await adminApi.updatePermissions(userId, hasIt ? 'remove' : 'add', permission);
      setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, permissions: res.data.permissions } : u)));
    } catch { /* */ }
  };

  const handleDelete = async (userId: number) => {
    setError(null);
    try {
      await adminApi.deleteUser(userId);
      setUsers((prev) => prev.filter((u) => u.id !== userId));
      setDialogOpen(false);
    } catch (err) {
      setError(getErrorMessage(err, 'Не вдалося видалити користувача'));
    }
  };

  const hasPerm = (u: User, perm: string) =>
    (u.permissions ?? '').split(',').some((p) => p.trim().toUpperCase() === perm.toUpperCase());

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700 }}>
          Адміністративна панель
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <IconButton aria-label="Меню користувача" onClick={(e) => setAnchorEl(e.currentTarget)}>
            <AccountCircle />
          </IconButton>
        </Box>
        <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
          <MenuItem disabled>{user?.fullName}</MenuItem>
          <MenuItem onClick={handleLogout}>Вийти</MenuItem>
        </Menu>
      </Box>

      <Tabs value={tabIndex} onChange={(_, v) => setTabIndex(v)} sx={{ mb: 3 }}>
        <Tab label="Користувачі" />
        <Tab label="Журнал аудиту" />
        <Tab label="Статистика" />
      </Tabs>

      {loading && <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />}

      {!loading && tabIndex === 0 && (
        <Paper sx={{ p: 2.5 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
            <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif' }}>Користувачі ({users.length})</Typography>
            <Button size="small" startIcon={<Refresh />} onClick={loadData}>Оновити</Button>
          </Box>
          <TableContainer sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>ПІБ</TableCell>
                  <TableCell>Логін</TableCell>
                  <TableCell>Роль</TableCell>
                  <TableCell>PRESCRIBER</TableCell>
                  <TableCell>Дії</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users.map((u) => (
                  <TableRow key={u.id} sx={{ bgcolor: u.deleted ? 'error.light' : undefined }}>
                    <TableCell>{u.id}</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>{u.fullName}</TableCell>
                    <TableCell>{u.login}</TableCell>
                    <TableCell>
                      <Select
                        size="small"
                        value={u.role}
                        onChange={(e) => handleRoleChange(u.id, e.target.value)}
                        sx={{ minWidth: 140 }}
                      >
                        <MenuItem value="DOCTOR">Лікар</MenuItem>
                        <MenuItem value="NURSE">Медсестра</MenuItem>
                        <MenuItem value="HEAD_OF_DEPARTMENT">Завідувач</MenuItem>
                        <MenuItem value="ADMINISTRATOR">Адміністратор</MenuItem>
                        <MenuItem value="AUDITOR">Аудитор</MenuItem>
                      </Select>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={hasPerm(u, 'PRESCRIBER') ? 'ТАК' : 'НІ'}
                        color={hasPerm(u, 'PRESCRIBER') ? 'success' : 'default'}
                        size="small"
                        onClick={() => handlePermissionToggle(u.id, 'PRESCRIBER', hasPerm(u, 'PRESCRIBER'))}
                        sx={{ cursor: 'pointer' }}
                      />
                    </TableCell>
                    <TableCell>
                      <Button
                        size="small"
                        color="error"
                        onClick={() => { setSelectedUser(u); setDialogOpen(true); }}
                      >
                        Видалити
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}

      {!loading && tabIndex === 1 && (
        <Paper sx={{ p: 2.5 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
            <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif' }}>Журнал аудиту</Typography>
            <Button variant={showAudit ? 'outlined' : 'contained'} startIcon={<History />}
              onClick={() => setShowAudit(!showAudit)}>
              {showAudit ? 'Сховати' : 'Переглянути'}
            </Button>
          </Box>
          {showAudit && (
            <>
              <Box sx={{ display: 'flex', gap: 1, mb: 1.5, flexWrap: 'wrap' }}>
                <TextField size="small" label="Фільтр за сутністю" value={auditFilterEntity}
                  onChange={(e) => setAuditFilterEntity(e.target.value)} sx={{ width: 200 }} />
                <Button size="small" variant="outlined" onClick={loadAudit}>Пошук</Button>
              </Box>
              <AuditLogTable logs={auditLogs} loading={auditLoading} />
            </>
          )}
        </Paper>
      )}

      {!loading && tabIndex === 2 && (
        <Paper sx={{ p: 2.5 }}>
          <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2 }}>Статистика системи</Typography>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            {Object.entries(stats).map(([key, val]) => (
              <Paper key={key} sx={{ p: 2, minWidth: 150, textAlign: 'center' }}>
                <Typography variant="h4" sx={{ fontWeight: 700 }}>{val}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {key === 'totalUsers' ? 'Всього користувачів'
                    : key === 'doctors' ? 'Лікарів'
                    : key === 'nurses' ? 'Медсестер'
                    : key === 'headsOfDepartment' ? 'Завідувачів'
                    : key === 'administrators' ? 'Адміністраторів'
                    : key === 'prescribers' ? 'PRESCRIBER' : key}
                </Typography>
              </Paper>
            ))}
          </Box>
        </Paper>
      )}

      <Snackbar open={!!error} autoHideDuration={6000} onClose={() => setError(null)}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
        <Alert severity="error" onClose={() => setError(null)} sx={{ width: '100%' }}>{error}</Alert>
      </Snackbar>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogTitle>Підтвердження видалення</DialogTitle>
        <DialogContent>
          Видалити користувача {selectedUser?.fullName} ({selectedUser?.login})?
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Скасувати</Button>
          <Button color="error" onClick={() => selectedUser && handleDelete(selectedUser.id)}>Видалити</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
