import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Typography, IconButton, Menu, MenuItem, useTheme,
  Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, CircularProgress,
  Button, TextField,
} from '@mui/material';
import { AccountCircle, History } from '@mui/icons-material';
import { useAuth } from '../../services/AuthContext';
import { userApi, auditApi } from '../../api/endpoints';
import AuditLogTable from '../../components/common/AuditLogTable';
import type { User, AuditLog } from '../../types';

export default function AdminPage() {
  useEffect(() => { document.title = 'ВАІТ — Адміністрування'; }, []);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const theme = useTheme();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [doctors, setDoctors] = useState<User[]>([]);
  const [nurses, setNurses] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);
  const [showAudit, setShowAudit] = useState(false);
  const [auditFilterEntity, setAuditFilterEntity] = useState('');

  const handleLogout = () => { logout(); navigate('/login'); };

  useEffect(() => {
    Promise.all([userApi.getDoctors(), userApi.getNurses()])
      .then(([d, n]) => { setDoctors(d.data); setNurses(n.data); })
      .finally(() => setLoading(false));
  }, []);

  const loadAudit = async () => {
    setAuditLoading(true);
    try {
      const params: Record<string, string> = {};
      if (auditFilterEntity) params.entity = auditFilterEntity;
      const res = await auditApi.list(params);
      setAuditLogs(res.data.content ?? res.data);
    } finally {
      setAuditLoading(false);
    }
  };

  useEffect(() => {
    if (showAudit) loadAudit();
  }, [showAudit]);

  const roleLabel = (u: User) => u.role === 'DOCTOR' ? 'Лікар'
    : u.role === 'NURSE' ? 'Медсестра'
    : u.role === 'HEAD_OF_DEPARTMENT' ? 'Завідувач відділення'
    : u.role === 'ADMINISTRATOR' ? 'Адміністратор' : u.role;

  const renderTable = (title: string, rows: User[]) => (
    <Paper sx={{ p: 2.5, mb: 3 }}>
      <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>{title}</Typography>
      <TableContainer sx={{ overflowX: 'auto' }}>
        <Table size="small" sx={{ minWidth: 450 }}>
          <TableHead>
            <TableRow>
              <TableCell>ПІБ</TableCell>
              <TableCell>Логін</TableCell>
              <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>Роль</TableCell>
              <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>Email</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((u) => (
              <TableRow key={u.id}>
                <TableCell sx={{ fontWeight: 600 }}>{u.fullName}</TableCell>
                <TableCell>{u.login}</TableCell>
                <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>{roleLabel(u)}</TableCell>
                <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>{u.email}</TableCell>
              </TableRow>
            ))}
            {rows.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} align="center" sx={{ py: 3, color: theme.palette.text.secondary }}>Немає даних</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700 }}>
          Адміністрування
        </Typography>
        <IconButton aria-label="Меню користувача" onClick={(e) => setAnchorEl(e.currentTarget)}>
          <AccountCircle />
        </IconButton>
        <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
          <MenuItem disabled>{user?.fullName}</MenuItem>
          <MenuItem onClick={handleLogout}>Вийти</MenuItem>
        </Menu>
      </Box>
      {loading ? (
        <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
      ) : (
        <>
          {renderTable('Лікарі', doctors)}
          {renderTable('Медсестри', nurses)}

          {/* Audit Log Section */}
          <Paper sx={{ p: 2.5, mb: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
              <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif' }}>Журнал аудиту</Typography>
              <Button
                size="small"
                variant={showAudit ? 'outlined' : 'contained'}
                startIcon={<History />}
                onClick={() => setShowAudit(!showAudit)}
              >
                {showAudit ? 'Сховати' : 'Переглянути'}
              </Button>
            </Box>
            {showAudit && (
              <>
                <Box sx={{ display: 'flex', gap: 1, mb: 1.5, flexWrap: 'wrap' }}>
                  <TextField size="small" label="Фільтр за сутністю" value={auditFilterEntity}
                    onChange={e => setAuditFilterEntity(e.target.value)} sx={{ width: { xs: '100%', sm: 200 } }} />
                  <Button size="small" variant="outlined" onClick={loadAudit}>Пошук</Button>
                </Box>
                <AuditLogTable logs={auditLogs} loading={auditLoading} />
              </>
            )}
          </Paper>
        </>
      )}
    </Box>
  );
}
