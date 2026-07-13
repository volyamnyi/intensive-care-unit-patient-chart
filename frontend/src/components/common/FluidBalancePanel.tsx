import { Box, Typography, Button, Paper } from '@mui/material';
import type { FluidBalanceItem } from '../../types';

interface FluidBalancePanelProps {
  items: FluidBalanceItem[];
  onRecalculate?: () => void;
  loading?: boolean;
}

export default function FluidBalancePanel({ items, onRecalculate, loading }: FluidBalancePanelProps) {
  const totalIntake = items.reduce((s, i) => s + (i.intake || 0), 0);
  const totalOutput = items.reduce((s, i) => s + (i.output || 0), 0);
  const dailyBalance = totalIntake - totalOutput;
  const lastItem = items[items.length - 1];
  const cumulativeBalance = lastItem?.cumulativeBalance ?? 0;

  return (
    <Paper sx={{
      p: 2.5, mb: 2, bgcolor: '#F5FBF8',
      border: '1px solid #D4E8DE', boxShadow: '0 2px 12px rgba(0,0,0,0.04)',
    }}>
      <Typography variant="h6" sx={{ fontFamily: '"Rubik", sans-serif', mb: 2 }}>
        Баланс рідини
      </Typography>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
        <Typography color="text.secondary">Надійшло:</Typography>
        <Typography sx={{ fontWeight: 700 }}>{totalIntake} мл</Typography>
      </Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
        <Typography color="text.secondary">Виділено:</Typography>
        <Typography sx={{ fontWeight: 700 }}>{totalOutput} мл</Typography>
      </Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
        <Typography color="text.secondary">Добовий баланс:</Typography>
        <Typography sx={{ fontWeight: 700 }} color={dailyBalance < 0 ? '#C42E1A' : '#1F6B4C'}>
          {dailyBalance} мл
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
        <Typography color="text.secondary">Кумулятивний баланс:</Typography>
        <Typography sx={{ fontWeight: 700 }} color={cumulativeBalance < 0 ? '#C42E1A' : '#1F6B4C'}>
          {cumulativeBalance} мл
        </Typography>
      </Box>
      {onRecalculate && (
        <Button size="small" variant="outlined" onClick={onRecalculate} disabled={loading}>
          {loading ? 'Розрахунок...' : 'Перерахувати'}
        </Button>
      )}
    </Paper>
  );
}
