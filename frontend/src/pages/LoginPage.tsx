import { useState, useEffect } from 'react';
import { Box, Card, CardContent, TextField, Button, Typography, Alert } from '@mui/material';
import { useAuth } from '../services/AuthContext';

export default function LoginPage() {
  useEffect(() => { document.title = 'ВАІТ — Вхід'; }, []);
  const { login } = useAuth();
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
      setError('Невірний логін або пароль');
    }
  };

  return (
    <Box sx={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      bgcolor: '#FAFAF8',
    }}>
      <Card className="fade-in-up" sx={{ width: 400, p: 3, border: '1px solid #E8E6E1', boxShadow: '0 4px 24px rgba(0,0,0,0.06)' }}>
        <CardContent sx={{ p: 0 }}>
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Box sx={{
              width: 48, height: 48, borderRadius: '50%',
              bgcolor: '#B6CECA', mx: 'auto', mb: 2,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 24,
            }}>
              🏥
            </Box>
            <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', mb: 0.5, fontWeight: 700 }}>
              Карта інтенсивної терапії
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Вхід до системи
            </Typography>
          </Box>

          {error && <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>{error}</Alert>}

          <Box component="form" onSubmit={handleSubmit}>
            <TextField
              fullWidth label="Логін" value={loginField}
              onChange={(e) => setLoginField(e.target.value)}
              sx={{ mb: 2 }} required
            />
            <TextField
              fullWidth label="Пароль" type="password" value={password}
              onChange={(e) => setPassword(e.target.value)}
              sx={{ mb: 3 }} required
            />
            <Button
              fullWidth variant="contained" size="large" type="submit"
              sx={{ bgcolor: '#FF5F33', '&:hover': { bgcolor: '#E8552E' } }}
            >
              Увійти
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
