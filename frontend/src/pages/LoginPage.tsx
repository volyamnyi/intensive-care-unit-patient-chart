import { useState, useEffect, useRef } from 'react';
import { Box, Card, TextField, Button, Typography, Alert, InputAdornment, IconButton, Paper } from '@mui/material';
import { Visibility, VisibilityOff, LocalHospital, ReceiptLong, Add } from '@mui/icons-material';
import { useAuth } from '../services/AuthContext';
import { useThemeMode } from '../styles/ThemeContext';

const platformApps = [
  { icon: <LocalHospital />, label: 'Карта інтенсивної терапії', color: '#1976d2' },
  { icon: <ReceiptLong />, label: 'Листок лікарських призначень', color: '#2e7d32' },
];

export default function LoginPage() {
  useEffect(() => { document.title = 'ВАІТ — Вхід'; }, []);
  const { login } = useAuth();
  const { mode } = useThemeMode();
  const isDark = mode === 'dark';
  const [loginField, setLoginField] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const formRef = useRef<HTMLFormElement>(null);

  const handleButtonClick = () => {
    formRef.current?.requestSubmit();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await login({ login: loginField, password });
      window.location.href = '/';
    } catch {
      setError('Невірний логін або пароль');
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
      <Box sx={{
        position: 'absolute', top: '-30%', right: '-10%', width: '60%', height: '60%',
        background: isDark
          ? 'radial-gradient(circle, rgba(255,95,51,0.08) 0%, transparent 70%)'
          : 'radial-gradient(circle, rgba(255,95,51,0.05) 0%, transparent 70%)',
        borderRadius: '50%', pointerEvents: 'none',
      }} />
      <Box sx={{
        position: 'absolute', bottom: '-20%', left: '-10%', width: '50%', height: '50%',
        background: isDark
          ? 'radial-gradient(circle, rgba(255,140,102,0.06) 0%, transparent 70%)'
          : 'radial-gradient(circle, rgba(255,140,102,0.04) 0%, transparent 70%)',
        borderRadius: '50%', pointerEvents: 'none',
      }} />

      <Card className="fade-in-up" sx={{
        width: { xs: 'calc(100% - 32px)', sm: 800 },
        display: 'flex',
        flexDirection: { xs: 'column', sm: 'row' },
        overflow: 'hidden',
        position: 'relative',
        border: `1px solid ${isDark ? '#2A2A2A' : '#E8E6E1'}`,
        boxShadow: isDark
          ? '0 8px 48px rgba(0,0,0,0.4)'
          : '0 8px 32px rgba(0,0,0,0.08)',
        bgcolor: isDark ? '#1A1A1A' : '#FFFFFF',
      }}>
        <Box sx={{
          width: { xs: '100%', sm: 320, md: 360 },
          bgcolor: isDark ? '#151515' : '#FFF8F5',
          p: { xs: 3, sm: 4 },
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          borderRight: { xs: 'none', sm: `1px solid ${isDark ? '#2A2A2A' : '#E8E6E1'}` },
          borderBottom: { xs: `1px solid ${isDark ? '#2A2A2A' : '#E8E6E1'}`, sm: 'none' },
        }}>
          <Box
            component="img"
            src={isDark ? '/superhumans-white.svg' : '/superhumans.svg'}
            alt="Superhumans"
            sx={{ height: 40, width: 'auto', mb: 2.5 }}
          />
          <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, mb: 2.5, color: isDark ? '#FFFFFF' : '#1F1F1F', letterSpacing: '-1px' }}>
            Медична інформаційна система
          </Typography>

          <Typography variant="caption" sx={{ color: isDark ? '#707070' : '#8A8A8A', fontWeight: 600, mb: 1.5, letterSpacing: '0.5px', textTransform: 'uppercase', fontSize: 10 }}>
            Додатки платформи
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mb: 3 }}>
            {platformApps.map((app) => (
              <Paper
                key={app.label}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.5,
                  p: 1.5,
                  bgcolor: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(255,255,255,0.8)',
                  borderRadius: 2,
                  border: `1px solid ${isDark ? '#2A2A2A' : '#E8E6E1'}`,
                  boxShadow: 'none',
                }}
              >
                <Box sx={{ color: app.color, display: 'flex' }}>{app.icon}</Box>
                <Typography variant="body2" sx={{ fontSize: 12, fontWeight: 600, color: isDark ? '#C0C0C0' : '#3A3A3A' }}>
                  {app.label}
                </Typography>
              </Paper>
            ))}
            <Paper
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
                p: 1.5,
                bgcolor: 'transparent',
                borderRadius: 2,
                border: `1px dashed ${isDark ? '#3A3A3A' : '#D0D0D0'}`,
                boxShadow: 'none',
              }}
            >
              <Add sx={{ fontSize: 20, color: isDark ? '#5A5A5A' : '#A0A0A0' }} />
              <Typography variant="body2" sx={{ fontSize: 12, color: isDark ? '#5A5A5A' : '#A0A0A0', fontStyle: 'italic' }}>
                Ще більше додатків
              </Typography>
            </Paper>
          </Box>

          <Typography variant="caption" sx={{ color: '#FF8C66', fontWeight: 700, fontSize: 11, letterSpacing: '0.5px', textTransform: 'uppercase' }}>
            Вхід до системи
          </Typography>
        </Box>

        <Box sx={{
          flex: 1,
          p: { xs: 3, sm: 4 },
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
        }}>
          <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700, mb: 0.5, color: isDark ? '#FFFFFF' : '#1F1F1F' }}>
            Ласкаво просимо
          </Typography>
          <Typography variant="body2" sx={{ color: isDark ? '#A0A0A0' : '#5A5A5A', fontSize: 13, mb: 3 }}>
            Увійдіть, щоб продовжити роботу
          </Typography>

          {error && <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>{error}</Alert>}

          <Box component="form" onSubmit={handleSubmit} ref={formRef}>
            <TextField
              fullWidth label="Логін" value={loginField}
              onChange={(e) => setLoginField(e.target.value)}
              sx={{ mb: 2 }} required
            />
            <TextField
              fullWidth label="Пароль"
              type={showPassword ? 'text' : 'password'} value={password}
              onChange={(e) => setPassword(e.target.value)}
              sx={{ mb: 3.5 }} required
              slotProps={{
                input: {
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton onClick={() => setShowPassword((p) => !p)} edge="end" tabIndex={-1}>
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                },
              }}
            />
            <Button
              fullWidth variant="contained" size="large" type="submit"
              onClick={handleButtonClick}
            >
              Увійти
            </Button>
          </Box>
        </Box>
      </Card>
    </Box>
  );
}
