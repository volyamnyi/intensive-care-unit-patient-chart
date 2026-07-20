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
import { useTranslation } from 'react-i18next';

export default function AdminPage() {
  const { t } = useTranslation();
  useEffect(() => { document.title = t('admin.title'); }, []);
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
      setAuditLogs(res.data.content ?? []);
    } finally {
      setAuditLoading(false);
    }
  };

  useEffect(() => {
    if (showAudit) loadAudit();
  }, [showAudit]);

  const roleLabel = (u: User) => u.role === 'DOCTOR' ? t('admin.roleDoctor')
    : u.role === 'NURSE' ? t('admin.roleNurse')
    : u.role === 'HEAD_OF_DEPARTMENT' ? t('admin.roleHod')
    : u.role === 'ADMINISTRATOR' ? t('admin.roleAdmin') : u.role;

  const renderTable = (title: string, rows: User[]) => (
    <Paper sx={{ p: 2.5, mb: 3 }}>
      <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1.5 }}>{title}</Typography>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{t('admin.tableHeaders.fullName')}</TableCell>
              <TableCell>{t('admin.tableHeaders.login')}</TableCell>
              <TableCell>{t('admin.tableHeaders.role')}</TableCell>
              <TableCell>{t('admin.tableHeaders.email')}</TableCell>
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
                <TableCell colSpan={4} align="center" sx={{ py: 3, color: theme.palette.text.secondary }}>{t('admin.noData')}</TableCell>
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
          {t('admin.heading')}
        </Typography>
        <IconButton aria-label="Меню користувача" onClick={(e) => setAnchorEl(e.currentTarget)}>
          <AccountCircle />
        </IconButton>
        <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
          <MenuItem disabled>{user?.fullName}</MenuItem>
          <MenuItem onClick={handleLogout}>{t('common.logout')}</MenuItem>
        </Menu>
      </Box>
      {loading ? (
        <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
      ) : (
        <>
          {renderTable(t('admin.sectionDoctors'), doctors)}
          {renderTable(t('admin.sectionNurses'), nurses)}

          {/* Audit Log Section */}
          <Paper sx={{ p: 2.5, mb: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
              <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif' }}>{t('auditLog.title')}</Typography>
              <Button
                size="small"
                variant={showAudit ? 'outlined' : 'contained'}
                startIcon={<History />}
                onClick={() => setShowAudit(!showAudit)}
              >
                {showAudit ? t('auditLog.hide') : t('auditLog.view')}
              </Button>
            </Box>
            {showAudit && (
              <>
                <Box sx={{ display: 'flex', gap: 1, mb: 1.5 }}>
                  <TextField size="small" label={t('auditLog.filterByEntity')} value={auditFilterEntity}
                    onChange={e => setAuditFilterEntity(e.target.value)} sx={{ width: 200 }} />
                  <Button size="small" variant="outlined" onClick={loadAudit}>{t('common.search')}</Button>
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
