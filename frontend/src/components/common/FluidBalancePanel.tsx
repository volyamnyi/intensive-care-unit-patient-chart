import { useTranslation } from 'react-i18next';
import { Box, Typography, Button, Paper, useTheme, Divider, Chip } from '@mui/material';
import type { FluidBalanceItem } from '../../types';

const INTAKE_LABELS: Record<string, string> = {
  crystalloids: 'Кристалоїди',
  colloids: 'Колоїди',
  blood: 'Кров',
  plasma: 'Плазма',
  nutrition: 'Харчування',
  oral: 'Перорально',
  other: 'Інше',
};

const OUTPUT_LABELS: Record<string, string> = {
  diuresis: 'Діурез',
  drainage: 'Дренаж',
  vomiting: 'Блювання',
  stool: 'Кал',
  bloodLoss: 'Крововтрата',
  other: 'Інші',
};

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
  const intakeByCategory = lastItem?.intakeByCategory;
  const outputByCategory = lastItem?.outputByCategory;

  const renderCategoryList = (map: Record<string, number> | undefined, labels: Record<string, string>) => {
    if (!map) return null;
    const entries = Object.entries(map).filter(([, v]) => v > 0);
    if (entries.length === 0) return <Typography variant="caption" color="text.secondary">—</Typography>;
    return entries.map(([key, val]) => (
      <Box key={key} sx={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, py: 0.3 }}>
        <Typography variant="caption">{labels[key] || key}</Typography>
        <Typography variant="caption" sx={{ fontWeight: 600 }}>{val} ml</Typography>
      </Box>
    ));
  };

  return (
    <Paper sx={{
      p: 2.5, mb: 2, bgcolor: isDark ? '#141414' : '#FFFFFF',
      border: `1px solid ${isDark ? '#2A2A2A' : '#E8E6E1'}`, boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 8px rgba(0,0,0,0.04)',
    }}>
      <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2 }}>
        {t('fluidBalance.title')}
        <Chip label="Auto" size="small" color="info" sx={{ ml: 1, fontSize: 9, fontWeight: 700, height: 18 }} />
      </Typography>

      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
        {/* Intake breakdown */}
        <Box sx={{ flex: 1, minWidth: 180 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, fontSize: 13, mb: 0.5, color: '#4CAF50' }}>
            {t('fluidBalance.intake')} — {totalIntake} ml
          </Typography>
          {intakeByCategory && renderCategoryList(intakeByCategory, INTAKE_LABELS)}
        </Box>

        {/* Output breakdown */}
        <Box sx={{ flex: 1, minWidth: 180 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, fontSize: 13, mb: 0.5, color: '#FF9100' }}>
            {t('fluidBalance.output')} — {totalOutput} ml
          </Typography>
          {outputByCategory && renderCategoryList(outputByCategory, OUTPUT_LABELS)}
        </Box>
      </Box>

      <Divider sx={{ my: 1.5 }} />

      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
        <Typography variant="body2" color="text.secondary">{t('fluidBalance.dailyBalance')}</Typography>
        <Typography sx={{ fontWeight: 700 }} color={dailyBalance < 0 ? '#FF5252' : '#4CAF50'}>
          {dailyBalance >= 0 ? '+' : ''}{dailyBalance} {t('fluidBalance.unit')}
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
        <Typography variant="body2" color="text.secondary">{t('fluidBalance.cumulativeBalance')}</Typography>
        <Typography sx={{ fontWeight: 700 }} color={cumulativeBalance < 0 ? '#FF5252' : '#4CAF50'}>
          {cumulativeBalance >= 0 ? '+' : ''}{cumulativeBalance} {t('fluidBalance.unit')}
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
