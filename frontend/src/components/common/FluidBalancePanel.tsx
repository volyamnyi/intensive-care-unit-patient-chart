import { useTranslation } from 'react-i18next';
import { Box, Typography, Button, Paper, useTheme } from '@mui/material';
import type { FluidBalanceItem } from '../../types';

interface FluidBalancePanelProps {
  items: FluidBalanceItem[];
  onRecalculate?: () => void;
  loading?: boolean;
}

export default function FluidBalancePanel({ items, onRecalculate, loading }: FluidBalancePanelProps) {
  const theme = useTheme();
  const { t } = useTranslation();
  const isDark = theme.palette.mode === 'dark';
  const totalIntake = items.reduce((s, i) => s + (i.intake || 0), 0);
  const totalOutput = items.reduce((s, i) => s + (i.output || 0), 0);
  const dailyBalance = totalIntake - totalOutput;
  const lastItem = items[items.length - 1];
  const cumulativeBalance = lastItem?.cumulativeBalance ?? 0;

  return (
    <Paper sx={{
      p: 2.5, mb: 2, bgcolor: isDark ? '#141414' : '#FFFFFF',
      border: `1px solid ${isDark ? '#2A2A2A' : '#E8E6E1'}`, boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 8px rgba(0,0,0,0.04)',
    }}>
      <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2 }}>
        {t('fluidBalance.title')}
      </Typography>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
        <Typography color="text.secondary">{t('fluidBalance.intake')}</Typography>
        <Typography sx={{ fontWeight: 700 }}>{totalIntake} {t('fluidBalance.unit')}</Typography>
      </Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
        <Typography color="text.secondary">{t('fluidBalance.output')}</Typography>
        <Typography sx={{ fontWeight: 700 }}>{totalOutput} {t('fluidBalance.unit')}</Typography>
      </Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
        <Typography color="text.secondary">{t('fluidBalance.dailyBalance')}</Typography>
        <Typography sx={{ fontWeight: 700 }} color={dailyBalance < 0 ? '#FF5252' : '#4CAF50'}>
          {dailyBalance} {t('fluidBalance.unit')}
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
        <Typography color="text.secondary">{t('fluidBalance.cumulativeBalance')}</Typography>
        <Typography sx={{ fontWeight: 700 }} color={cumulativeBalance < 0 ? '#FF5252' : '#4CAF50'}>
          {cumulativeBalance} {t('fluidBalance.unit')}
        </Typography>
      </Box>
      {onRecalculate && (
        <Button size="small" variant="outlined" onClick={onRecalculate} disabled={loading}>
          {loading ? t('fluidBalance.calculatingButton') : t('fluidBalance.recalculateButton')}
        </Button>
      )}
    </Paper>
  );
}
