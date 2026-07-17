import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, TextField, Button, CircularProgress, Alert, InputAdornment, useTheme } from '@mui/material';
import { Add, Search as SearchIcon } from '@mui/icons-material';
import { episodeApi } from '../../api/endpoints';
import EpisodeTable from '../../components/common/EpisodeTable';
import type { Episode } from '../../types';
import { useTranslation } from 'react-i18next';

export default function DashboardPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const theme = useTheme();
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    episodeApi.search({ status: 'ACTIVE' })
      .then((res) => setEpisodes(res.data))
      .catch(() => setEpisodes([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { document.title = t('doctor.dashboard.title'); }, []);

  const filteredEpisodes = episodes.filter((ep) =>
    (ep.patientName ?? '').toLowerCase().includes(search.toLowerCase())
  );

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3, alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 800, color: theme.palette.text.primary }}>
            {t('doctor.dashboard.heading')}
          </Typography>
          <Typography variant="body2" sx={{ color: theme.palette.text.secondary, mt: 0.5 }}>
            {t('doctor.dashboard.subtitle')}
          </Typography>
        </Box>
        <Button
          variant="contained"
          onClick={() => navigate('/doctor/create-card')}
          startIcon={<Add />}
        >
          {t('doctor.dashboard.newCard')}
        </Button>
      </Box>
      <TextField
        fullWidth
        placeholder={t('doctor.dashboard.searchPlaceholder')}
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        sx={{ mb: 3 }}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon sx={{ color: theme.palette.text.secondary }} />
              </InputAdornment>
            ),
          },
        }}
      />
      {filteredEpisodes.length === 0 && !loading ? (
        <Alert severity="info">
          {search ? t('doctor.dashboard.noResults') : t('doctor.dashboard.empty')}
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
