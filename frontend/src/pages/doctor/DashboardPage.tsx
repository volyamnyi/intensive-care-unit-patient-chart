import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, Button, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Chip, CircularProgress, Alert
} from '@mui/material';
import { Add } from '@mui/icons-material';
import { icuCardApi } from '../../api/endpoints';
import type { IcuCard } from '../../types';

export default function DashboardPage() {
  const navigate = useNavigate();
  const [cards, setCards] = useState<IcuCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    icuCardApi.getActive()
      .then((res) => setCards(res.data))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { document.title = 'ВАІТ — Лікар'; }, []);

  const filteredCards = cards.filter((c) =>
    c.patientName.toLowerCase().includes(search.toLowerCase())
  );

  const statusChip = (status: string) => {
    const colors: Record<string, 'success' | 'warning' | 'info'> = {
      ACTIVE: 'success',
      CLOSED: 'info',
    };
    return <Chip label={status === 'ACTIVE' ? 'Активна' : 'Закрита'} color={colors[status] || 'default'} size="small" />;
  };

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3, alignItems: 'center' }}>
        <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700 }}>
          Активні пацієнти ВАІТ
        </Typography>
        <Button
          variant="contained" onClick={() => navigate('/doctor/create-card')}
          startIcon={<Add />}
          sx={{ bgcolor: '#FF5F33', '&:hover': { bgcolor: '#E8552E' } }}
        >
          Нова карта
        </Button>
      </Box>

      <TextField
        fullWidth placeholder="Пошук пацієнта за ПІБ..."
        value={search} onChange={(e) => setSearch(e.target.value)}
        sx={{ mb: 2 }}
      />

      {cards.length === 0 ? (
        <Alert severity="info" sx={{ borderRadius: 2 }}>Немає активних пацієнтів</Alert>
      ) : (
        <TableContainer component={Paper} sx={{ border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ПІП</TableCell>
                <TableCell>Доба</TableCell>
                <TableCell>Діагноз</TableCell>
                <TableCell>APACHE II</TableCell>
                <TableCell>SOFA</TableCell>
                <TableCell>Статус</TableCell>
                <TableCell>Дії</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredCards.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4, color: '#5A5A5A' }}>
                    {search ? 'Немає пацієнтів за запитом' : 'Немає активних пацієнтів'}
                  </TableCell>
                </TableRow>
              ) : (
                filteredCards.map((card) => {
                  const activeDay = card.icuDays?.find((d) => d.status === 'ACTIVE');
                  return (
                    <TableRow key={card.id} hover sx={{ '&:hover': { bgcolor: '#F5F4F0' } }}>
                      <TableCell sx={{ fontWeight: 600 }}>{card.patientName}</TableCell>
                      <TableCell>{activeDay?.dayNumber || '-'}</TableCell>
                      <TableCell>{card.diagnosis}</TableCell>
                      <TableCell>{card.apacheIi}</TableCell>
                      <TableCell>{card.sofa}</TableCell>
                      <TableCell>{statusChip(card.status)}</TableCell>
                      <TableCell>
                        <Button
                          size="small"
                          variant="outlined"
                          sx={{ borderColor: '#B6CECA', color: '#1F1F1F', '&:hover': { borderColor: '#8AAB9E', bgcolor: '#F0F5F3' } }}
                          onClick={() =>
                            navigate(`/doctor/card/${card.id}/day/${activeDay?.id || ''}`)
                          }
                        >
                          Відкрити
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
}
