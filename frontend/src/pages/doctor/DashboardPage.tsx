import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, TextField, Button, CircularProgress, Alert } from '@mui/material';
import { Add } from '@mui/icons-material';
import { episodeApi } from '../../api/endpoints';
import EpisodeTable from '../../components/common/EpisodeTable';
import type { Episode } from '../../types';

export default function DashboardPage() {
  const navigate = useNavigate();
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    episodeApi.search({ status: 'ACTIVE' })
      .then((res) => setEpisodes(res.data))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { document.title = 'ВАІТ — Лікар'; }, []);

  const filteredEpisodes = episodes.filter((ep) =>
    ep.patientName?.toLowerCase().includes(search.toLowerCase())
  );

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3, alignItems: 'center' }}>
        <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700 }}>
          Активні пацієнти ВАІТ
        </Typography>
        <Button
          variant="contained"
          onClick={() => navigate('/doctor/create-card')}
          startIcon={<Add />}
          sx={{ bgcolor: '#FF5F33', '&:hover': { bgcolor: '#E8552E' } }}
        >
          Нова карта
        </Button>
      </Box>
      <TextField
        fullWidth
        placeholder="Пошук пацієнта за ПІБ..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        sx={{ mb: 2 }}
      />
      {filteredEpisodes.length === 0 && !loading ? (
        <Alert severity="info" sx={{ borderRadius: 2 }}>
          {search ? 'Немає пацієнтів за запитом' : 'Немає активних пацієнтів'}
        </Alert>
      ) : (
        <EpisodeTable
          episodes={filteredEpisodes}
          onSelect={(ep) => navigate('/doctor/episode/' + ep.id)}
        />
      )}
    </Box>
  );
}
