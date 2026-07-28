import { useState, useCallback, useMemo } from 'react';
import {
  Box, Typography, IconButton, Paper, Tooltip, CircularProgress,
} from '@mui/material';
import { ArrowBackIosNew, ArrowForwardIos } from '@mui/icons-material';
import type { VitalSignEntry } from '../../types';

const PERIODS = ['morning', 'day', 'evening', 'night'] as const;
const PERIOD_LABELS: Record<string, string> = {
  morning: 'Р', day: 'Д', evening: 'В', night: 'Н',
};
const PERIOD_FULL: Record<string, string> = {
  morning: 'Ранок', day: 'День', evening: 'Вечір', night: 'Ніч',
};

function formatDate(iso: string) {
  const d = new Date(iso);
  return d.toLocaleDateString('uk-UA', { day: '2-digit', month: 'short' });
}

interface VitalParam {
  key: string;
  label: string;
  unit: string;
  numeric: boolean;
  min?: number;
  max?: number;
  step?: number;
}

const VITAL_PARAMS: VitalParam[] = [
  { key: 'temperature', label: 'Температура', unit: '°C', numeric: true, min: 34, max: 42, step: 0.1 },
  { key: 'systolicBp', label: 'АТ сист.', unit: 'мм рт.ст.', numeric: true, min: 50, max: 250, step: 1 },
  { key: 'diastolicBp', label: 'АТ діаст.', unit: 'мм рт.ст.', numeric: true, min: 30, max: 150, step: 1 },
  { key: 'spo2', label: 'SpO₂', unit: '%', numeric: true, min: 50, max: 100, step: 1 },
  { key: 'pulse', label: 'Пульс', unit: 'уд/хв', numeric: true, min: 0, max: 300, step: 1 },
  { key: 'painScore', label: 'Біль', unit: '0-10', numeric: true, min: 0, max: 10, step: 1 },
  { key: 'stool', label: 'Стул', unit: '', numeric: false },
];

interface VitalSignGridProps {
  days: { id: string; dayDate: string; entries: VitalSignEntry[] }[];
  canEdit: boolean;
  isDoctor: boolean;
  onCellUpdate: (dayId: string, period: string, paramKey: string, value: string) => Promise<void>;
  loading?: boolean;
}

function entryValue(entry: VitalSignEntry | undefined, key: string): string {
  if (!entry) return '';
  const v = (entry as Record<string, unknown>)[key];
  if (v === null || v === undefined) return '';
  return String(v);
}

function dayPartKey(dayId: string, period: string) { return `${dayId}|${period}`; }

export default function VitalSignGrid({
  days, canEdit, isDoctor, onCellUpdate, loading,
}: VitalSignGridProps) {
  const today = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const startIdx = Math.max(0, days.findIndex(d => d.dayDate >= today));
  const [viewStart, setViewStart] = useState(Math.max(0, startIdx));
  const daysToShow = 7;
  const visibleDays = days.slice(viewStart, viewStart + daysToShow);

  const shiftLeft = () => setViewStart(Math.max(0, viewStart - daysToShow));
  const shiftRight = () => {
    if (viewStart + daysToShow < days.length) setViewStart(viewStart + daysToShow);
  };

  const entriesByDayPeriod = useMemo(() => {
    const map = new Map<string, VitalSignEntry>();
    for (const day of days) {
      for (const entry of day.entries) {
        map.set(dayPartKey(day.id, entry.period), entry);
      }
    }
    return map;
  }, [days]);

  const [editingCell, setEditingCell] = useState<string | null>(null);
  const [editingValue, setEditingValue] = useState('');

  const startEdit = (dayId: string, period: string, paramKey: string, currentValue: string) => {
    if (!canEdit || !isDoctor) return;
    setEditingCell(`${dayId}|${period}|${paramKey}`);
    setEditingValue(currentValue);
  };

  const commitEdit = async (dayId: string, period: string, paramKey: string) => {
    const value = editingValue.trim();
    setEditingCell(null);
    setEditingValue('');
    if (!value) return;
    await onCellUpdate(dayId, period, paramKey, value);
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Paper sx={{ p: 1.5, display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
        <IconButton size="small" onClick={shiftLeft} disabled={viewStart === 0}>
          <ArrowBackIosNew fontSize="small" />
        </IconButton>
        <Typography variant="body2" sx={{ fontWeight: 600, minWidth: 120, textAlign: 'center' }}>
          {visibleDays.length > 0
            ? `${formatDate(visibleDays[0].dayDate)} — ${formatDate(visibleDays[visibleDays.length - 1].dayDate)}`
            : 'Немає даних'}
        </Typography>
        <IconButton size="small" onClick={shiftRight} disabled={viewStart + daysToShow >= days.length}>
          <ArrowForwardIos fontSize="small" />
        </IconButton>
      </Paper>

      {loading ? (
        <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
      ) : days.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">Немає даних життєвих показників</Typography>
        </Paper>
      ) : (
        <Paper sx={{ overflow: 'auto' }}>
          <Box component="table" sx={{
            borderCollapse: 'collapse', minWidth: 200 + visibleDays.length * 300,
            '& th, & td': { border: '1px solid', borderColor: 'divider', p: 0 },
          }}>
            <Box component="thead">
              <Box component="tr">
                <Box component="th" sx={{
                  position: 'sticky', left: 0, bgcolor: 'background.paper',
                  zIndex: 2, minWidth: 180, p: '6px 8px !important',
                }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>Показник</Typography>
                </Box>
                {visibleDays.map(day => (
                  <Box component="th" key={day.id} colSpan={4} sx={{ p: '4px 2px !important', bgcolor: 'grey.100' }}>
                    <Typography variant="caption" sx={{ fontWeight: 700 }}>
                      {formatDate(day.dayDate)}
                    </Typography>
                  </Box>
                ))}
              </Box>
              <Box component="tr">
                <Box component="th" sx={{ position: 'sticky', left: 0, bgcolor: 'background.paper', zIndex: 2 }} />
                {visibleDays.map(day =>
                  PERIODS.map(p => (
                    <Box component="th" key={`${day.id}-${p}`}
                      sx={{ width: 68, fontSize: 10, color: 'text.secondary', p: '2px !important' }}>
                      {PERIOD_LABELS[p]}
                    </Box>
                  ))
                )}
              </Box>
            </Box>
            <Box component="tbody">
              {VITAL_PARAMS.map(param => (
                <Box component="tr" key={param.key}>
                  <Box component="td" sx={{
                    position: 'sticky', left: 0, bgcolor: 'background.paper',
                    zIndex: 1, p: '4px 8px !important', minWidth: 180,
                  }}>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {param.label}
                    </Typography>
                    {param.unit && (
                      <Typography variant="caption" color="text.secondary">
                        {param.unit}
                      </Typography>
                    )}
                  </Box>

                  {visibleDays.map(day =>
                    PERIODS.map(period => {
                      const entry = entriesByDayPeriod.get(dayPartKey(day.id, period));
                      const value = entryValue(entry, param.key);
                      const cellKey = `${day.id}|${period}|${param.key}`;
                      const isEditing = editingCell === cellKey;

                      const onClick = () => {
                        if (!canEdit || !isDoctor) return;
                        startEdit(day.id, period, param.key, value);
                      };

                      return (
                        <Box component="td" key={cellKey} sx={{
                          width: 68, height: 32, cursor: canEdit && isDoctor ? 'pointer' : 'default',
                          bgcolor: value ? '#BBDEFB' : '#fff',
                          textAlign: 'center', verticalAlign: 'middle',
                          position: 'relative',
                        }} onClick={onClick}>
                          {isEditing ? (
                            <Box component="form" onSubmit={e => { e.preventDefault(); commitEdit(day.id, period, param.key); }}
                              sx={{ position: 'absolute', inset: 0, zIndex: 3, display: 'flex' }}>
                              <input autoFocus value={editingValue}
                                onChange={e => setEditingValue(e.target.value)}
                                onBlur={() => commitEdit(day.id, period, param.key)}
                                style={{
                                  width: '100%', border: '2px solid #1976d2',
                                  textAlign: 'center', fontSize: 11, padding: 0, outline: 'none',
                                }} />
                            </Box>
                          ) : (
                            <Tooltip title={entry ? `${PERIOD_FULL[entry.period]}: ${value || '—'}` : '—'} arrow>
                              <Typography variant="caption" sx={{
                                fontSize: 10, lineHeight: '32px',
                                color: value ? '#1565c0' : 'text.secondary',
                                fontWeight: value ? 600 : 400,
                                userSelect: 'none',
                              }}>
                                {value || ''}
                              </Typography>
                            </Tooltip>
                          )}
                        </Box>
                      );
                    })
                  )}
                </Box>
              ))}
            </Box>
          </Box>
        </Paper>
      )}
    </Box>
  );
}
