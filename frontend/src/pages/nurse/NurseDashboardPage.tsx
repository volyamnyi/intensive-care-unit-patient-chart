import { useState, useEffect } from 'react';
import { Box, Typography, TextField, CircularProgress, Alert } from '@mui/material';
import { episodeApi } from '../../api/endpoints';
import EpisodeTable from '../../components/common/EpisodeTable';
import type { Episode } from '../../types';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function NurseDashboardPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    episodeApi.search({ status: 'ACTIVE' })
      .then((res) => setEpisodes(res.data))
      .catch(() => setEpisodes([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { document.title = t('nurse.dashboard.title'); }, []);

  const filteredEpisodes = episodes.filter((ep) =>
    (ep.patientName ?? '').toLowerCase().includes(search.toLowerCase())
  );

  if (loading) return <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 700, mb: 3 }}>
        {t('nurse.dashboard.heading')}
      </Typography>
      <TextField
        fullWidth
        placeholder={t('nurse.dashboard.searchPlaceholder')}
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        sx={{ mb: 2 }}
      />
      {filteredEpisodes.length === 0 && !loading ? (
        <Alert severity="info" sx={{ borderRadius: 2 }}>
          {search ? t('nurse.dashboard.noResults') : t('nurse.dashboard.empty')}
        </Alert>
      ) : (
        <EpisodeTable
          episodes={filteredEpisodes}
          onSelect={(ep) => navigate('/nurse/episode/' + ep.id)}
        />
      )}
    </Box>
  );
}
