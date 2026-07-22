import { useState } from 'react';
import { Grid, Paper, Typography, TextField, Button, MenuItem, Box, Chip, useTheme } from '@mui/material';
import type { ScaleResult, ClinicalScale } from '../../types';

interface ScaleResultsPanelProps {
  results: ScaleResult[];
  availableScales: ClinicalScale[];
  onCreateResult?: (scaleId: string, result: string) => void;
}

export default function ScaleResultsPanel({ results, availableScales, onCreateResult }: ScaleResultsPanelProps) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const [selectedScaleId, setSelectedScaleId] = useState('');
  const [resultValue, setResultValue] = useState('');

  const handleCreate = () => {
    if (!onCreateResult || !selectedScaleId || !resultValue.trim()) return;
    onCreateResult(selectedScaleId, resultValue.trim());
    setSelectedScaleId('');
    setResultValue('');
  };

  const isAutoScale = (name: string) => /GCS|RASS|glasgow|richmond/i.test(name);

  const getResultForScale = (scaleId: string) => results.find((r) => r.scaleId === scaleId);

  return (
    <>
      {onCreateResult && availableScales.length > 0 && (
        <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap', alignItems: 'flex-start' }}>
          <TextField
            select size="small" label={'Шкала'} value={selectedScaleId}
            onChange={(e) => setSelectedScaleId(e.target.value)}
            sx={{ minWidth: { xs: '100%', sm: 200 } }}
          >
            {availableScales.map((s) => (
              <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>
            ))}
          </TextField>
          <TextField
            size="small" label={'Результат'} value={resultValue}
            onChange={(e) => setResultValue(e.target.value)}
            sx={{ minWidth: { xs: '100%', sm: 120 } }}
          />
          <Button variant="contained" size="small" onClick={handleCreate}>
            {'Додати'}
          </Button>
        </Box>
      )}

      <Grid container spacing={2}>
        {availableScales.length === 0 && results.length === 0 ? (
          <Grid size={12}>
            <Typography color="text.secondary">{'Немає результатів'}</Typography>
          </Grid>
        ) : (
          availableScales.map((scale) => {
            const result = getResultForScale(scale.id);
            return (
              <Grid size={{ xs: 12, md: 6 }} key={scale.id}>
                <Paper sx={{ p: 2, border: `1px solid ${isDark ? '#2A2A2A' : '#E8E6E1'}`, boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 8px rgba(0,0,0,0.04)' }}>
                  <Typography variant="subtitle1" sx={{ fontFamily: '"Rubik", sans-serif', fontWeight: 600 }}>
                    {scale.name}
                    {isAutoScale(scale.name) && (
                      <Chip label="Auto" size="small" color="info" sx={{ ml: 1, fontSize: 9, fontWeight: 700, height: 18 }} />
                    )}
                  </Typography>
                  {result ? (
                    <Typography variant="body1" sx={{ mt: 0.5 }}>
                      {`Результат: ${result.result} (${new Date(result.calculatedAt).toLocaleString('uk-UA')})`}
                    </Typography>
                  ) : (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      {'Не заповнено'}
                    </Typography>
                  )}
                </Paper>
              </Grid>
            );
          })
        )}
      </Grid>
    </>
  );
}
