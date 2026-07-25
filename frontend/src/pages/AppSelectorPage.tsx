import { useNavigate } from 'react-router-dom';
import { Box, Card, CardContent, CardActionArea, Typography } from '@mui/material';
import { LocalHospital, ReceiptLong, AdminPanelSettings } from '@mui/icons-material';
import { useAuth } from '../services/AuthContext';

const cards = [
  {
    app: 'icu' as const,
    title: 'Карта інтенсивної терапії',
    subtitle: 'Відділення анестезіології та інтенсивної терапії',
    icon: <LocalHospital sx={{ fontSize: 48 }} />,
    color: '#1976d2',
    path: '/doctor',
  },
  {
    app: 'prescriptions' as const,
    title: 'Листок лікарських призначень',
    subtitle: 'Форма 003-4/о — медикаментозні призначення',
    icon: <ReceiptLong sx={{ fontSize: 48 }} />,
    color: '#2e7d32',
    path: '/prescriptions/doctor',
  },
];

export default function AppSelectorPage() {
  const { user, selectApp, hasRole } = useAuth();
  const navigate = useNavigate();

  const handleSelect = (card: (typeof cards)[0]) => {
    selectApp(card.app);
    let target = card.path;
    if (target === '/doctor' && hasRole('NURSE')) {
      target = '/nurse';
    }
    if (target === '/prescriptions/doctor' && hasRole('NURSE')) {
      target = '/prescriptions/nurse';
    }
    navigate(target, { replace: true });
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'grey.100',
        p: 3,
      }}
    >
      <Typography variant="h4" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, mb: 1 }}>
        Superhumans Lviv
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        {`Вітаємо, ${user?.fullName ?? ''}`}
      </Typography>
      <Typography variant="h6" sx={{ mb: 3 }}>
        Оберіть додаток для роботи
      </Typography>
      <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap', justifyContent: 'center', maxWidth: 700 }}>
        {cards.map((card) => (
          <Card key={card.app} sx={{ width: 300 }}>
            <CardActionArea onClick={() => handleSelect(card)} sx={{ p: 2 }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Box sx={{ color: card.color, mb: 1 }}>{card.icon}</Box>
                <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 600 }}>
                  {card.title}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {card.subtitle}
                </Typography>
              </CardContent>
            </CardActionArea>
          </Card>
        ))}
        {hasRole('ADMINISTRATOR') && (
          <Card sx={{ width: 300 }}>
            <CardActionArea onClick={() => navigate('/admin', { replace: true })} sx={{ p: 2 }}>
              <CardContent sx={{ textAlign: 'center' }}>
                <Box sx={{ color: '#7b1fa2', mb: 1 }}>
                  <AdminPanelSettings sx={{ fontSize: 48 }} />
                </Box>
                <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 600 }}>
                  Адміністративна панель
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Керування доступом, аудит, налаштування
                </Typography>
              </CardContent>
            </CardActionArea>
          </Card>
        )}
      </Box>
    </Box>
  );
}
