import { useState, useEffect } from 'react';
import { Box, Card, CardContent, TextField, Button, Typography, Alert } from '@mui/material';
import { useAuth } from '../services/AuthContext';
import { useThemeMode } from '../styles/ThemeContext';
import { useTranslation } from 'react-i18next';

export default function LoginPage() {
  const { t } = useTranslation();
  useEffect(() => { document.title = t('login.title'); }, []);
  const { login } = useAuth();
  const { mode } = useThemeMode();
  const isDark = mode === 'dark';
  const [loginField, setLoginField] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await login({ login: loginField, password });
      window.location.href = '/';
    } catch {
      setError(t('login.error'));
    }
  };

  return (
    <Box sx={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      bgcolor: isDark ? '#0D0D0D' : '#FAFAF8',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {isDark && (
        <Box sx={{
          position: 'absolute', top: '-50%', left: '-50%', width: '200%', height: '200%',
          background: 'radial-gradient(circle at 30% 50%, rgba(255, 95, 51, 0.06) 0%, transparent 50%), radial-gradient(circle at 70% 50%, rgba(255, 140, 102, 0.04) 0%, transparent 50%)',
          pointerEvents: 'none',
        }} />
      )}
      <Card className="fade-in-up" sx={{
        width: 420, p: 4, position: 'relative',
        border: `1px solid ${isDark ? '#2A2A2A' : '#E8E6E1'}`,
        boxShadow: isDark ? '0 8px 48px rgba(0,0,0,0.4), 0 0 0 1px rgba(255, 95, 51, 0.05)' : '0 4px 24px rgba(0,0,0,0.06)',
        bgcolor: isDark ? '#1A1A1A' : '#FFFFFF',
      }}>
        <CardContent sx={{ p: 0 }}>
            <Box sx={{ textAlign: 'center', mb: 4 }}>
            <Box
              component="img"
              src={isDark ? '/superhumans-white.svg' : '/superhumans.svg'}
              alt={t('login.superhumansAlt')}
              sx={{ height: 56, width: 'auto', mx: 'auto', mb: 2.5, display: 'block' }}
            />
            <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', mb: 0.5, fontWeight: 800, color: isDark ? '#FFFFFF' : '#1F1F1F', letterSpacing: '-0.5px' }}>
              {t('common.appTitle')}
            </Typography>
            <Typography variant="body2" sx={{ color: isDark ? '#A0A0A0' : '#5A5A5A', fontSize: 13, mb: 0.5 }}>
              {t('common.appSubtitle')}
            </Typography>
            <Typography variant="body2" sx={{ color: '#FF8C66', fontSize: 12, fontWeight: 600 }}>
              {t('login.heading')}
            </Typography>
          </Box>

          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

          <Box component="form" onSubmit={handleSubmit}>
            <TextField
              fullWidth label={t('login.username')} value={loginField}
              onChange={(e) => setLoginField(e.target.value)}
              sx={{ mb: 2 }} required
            />
            <TextField
              fullWidth label={t('login.password')} type="password" value={password}
              onChange={(e) => setPassword(e.target.value)}
              sx={{ mb: 3.5 }} required
            />
            <Button
              fullWidth variant="contained" size="large" type="submit"
            >
              {t('login.submit')}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
