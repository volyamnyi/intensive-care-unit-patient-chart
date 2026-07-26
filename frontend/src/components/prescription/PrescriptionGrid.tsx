import { useState, useCallback, useMemo } from 'react';
import {
  Box, Typography, IconButton, TextField, Button, Autocomplete,
  Paper, Tooltip, CircularProgress, Checkbox, FormControlLabel, Popover,
} from '@mui/material';
import {
  Add, Delete, ArrowBackIosNew, ArrowForwardIos,
} from '@mui/icons-material';
import type {
  PrescriptionItem, PrescriptionDayPart, MedicineCatalogItem, AllergyItem,
} from '../../types';

// ── helpers ──────────────────────────────────────────────────────────

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

function cellBg(part: PrescriptionDayPart | undefined) {
  if (!part) return '#fff';
  if (part.isCompletedFinished) return '#A5D6A7';
  if (part.isCompleted) return '#C8E6C9';
  if (part.isPlannedFinished) return '#E1BEE7';
  if (part.isPlanned) return '#BBDEFB';
  return '#fff';
}

function cellLabel(part: PrescriptionDayPart | undefined) {
  if (!part) return '';
  if (part.isCompletedFinished) return '✓✓';
  if (part.isCompleted) return '✓';
  if (part.isPlannedFinished) return '✕';
  if (part.isPlanned) return part.dose ?? '+';
  return '';
}

function dayPartKey(date: string, period: string) { return `${date}|${period}`; }

// ── types ────────────────────────────────────────────────────────────

export interface GridItem extends PrescriptionItem {
  cells: Map<string, PrescriptionDayPart>;
}

export interface GridProps {
  items: PrescriptionItem[];
  canEdit: boolean;
  isDoctor: boolean;
  isNurse: boolean;
  onPlan: (dayPartId: string, dose: string) => Promise<void>;
  onComplete: (dayPartId: string) => Promise<void>;
  onExecute?: (dayPartId: string, actualDose: string, requires2p: boolean, secondPersonId?: string) => Promise<void>;
  onAddItem: (data: { medicineName: string; medicineMethod?: string; regime?: string }) => Promise<void>;
  onRemoveItem: (itemId: string) => Promise<void>;
  onSearchMedicine: (keyword: string) => Promise<MedicineCatalogItem[]>;
  allergies: AllergyItem[];
  loading?: boolean;
}

// ── fallback catalog ─────────────────────────────────────────────────

const fallbackCatalog: MedicineCatalogItem[] = [
  { id: 1, name: 'Paracetamol', categoryRef: 1, ptgCode: '1', isHighRisk: false },
  { id: 3, name: 'Morphine', categoryRef: 14, ptgCode: '4', isHighRisk: true },
  { id: 5, name: 'Ceftriaxone', categoryRef: 2, ptgCode: '6', isHighRisk: false },
  { id: 6, name: 'Metronidazole', categoryRef: 2, ptgCode: '2,3', isHighRisk: false },
  { id: 7, name: 'Omeprazole', categoryRef: 3, ptgCode: '1', isHighRisk: false },
  { id: 8, name: 'Heparin', categoryRef: 5, ptgCode: '5', isHighRisk: false },
  { id: 9, name: 'Norepinephrine', categoryRef: 13, ptgCode: '3', isHighRisk: true },
  { id: 10, name: 'Dopamine', categoryRef: 13, ptgCode: '3', isHighRisk: true },
  { id: 11, name: 'NaCl 0.9%', categoryRef: 8, ptgCode: null, isHighRisk: false },
  { id: 12, name: 'Glucose 5%', categoryRef: 8, ptgCode: null, isHighRisk: false },
  { id: 13, name: 'Midazolam', categoryRef: 14, ptgCode: '4', isHighRisk: true },
  { id: 14, name: 'Propofol', categoryRef: 14, ptgCode: '4', isHighRisk: true },
  { id: 15, name: 'Dexamethasone', categoryRef: 1, ptgCode: '1', isHighRisk: false },
  { id: 16, name: 'Insulin', categoryRef: 6, ptgCode: null, isHighRisk: false },
  { id: 19, name: 'Ondansetron', categoryRef: 4, ptgCode: '2', isHighRisk: false },
  { id: 20, name: 'Pantoprazole', categoryRef: 3, ptgCode: '1', isHighRisk: false },
];

// ── component ────────────────────────────────────────────────────────

export default function PrescriptionGrid({
  items, canEdit, isDoctor, isNurse,
  onPlan, onComplete, onExecute,
  onAddItem, onRemoveItem,
  onSearchMedicine, allergies, loading,
}: GridProps) {

  // ── date range ──────────────────────────────────────────────────

  const allDates = useMemo(() => {
    const set = new Set<string>();
    items.forEach(it => it.dayParts?.forEach(dp => {
      if (dp.dayDate) set.add(dp.dayDate);
    }));
    return Array.from(set).sort();
  }, [items]);

  const today = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const startIdx = Math.max(0, allDates.indexOf(
    allDates.find(d => d >= today) ?? allDates[0] ?? ''
  ));
  const [viewStart, setViewStart] = useState(startIdx);
  const daysToShow = 7;
  const visibleDates = allDates.slice(viewStart, viewStart + daysToShow);

  const shiftLeft = () => setViewStart(Math.max(0, viewStart - daysToShow));
  const shiftRight = () => {
    if (viewStart + daysToShow < allDates.length) setViewStart(viewStart + daysToShow);
  };

  // ── build grid rows ─────────────────────────────────────────────

  const gridItems: GridItem[] = useMemo(() =>
    items.map(item => {
      const cells = new Map<string, PrescriptionDayPart>();
      item.dayParts?.forEach(dp => {
        if (dp.dayDate) cells.set(dayPartKey(dp.dayDate, dp.period), dp);
      });
      return { ...item, cells };
    }), [items]);

  // ── doctor: dose editing ────────────────────────────────────────

  const [editingCell, setEditingCell] = useState<string | null>(null);
  const [editingDose, setEditingDose] = useState('');

  const startEdit = (dp: PrescriptionDayPart) => {
    if (!canEdit || !isDoctor) return;
    setEditingCell(dp.id);
    setEditingDose(dp.dose ?? '');
  };

  const commitEdit = async (dp: PrescriptionDayPart) => {
    const dose = editingDose.trim();
    setEditingCell(null);
    setEditingDose('');
    if (!dose || dose === (dp.dose ?? '')) return;
    await onPlan(dp.id, dose);
  };

  // ── doctor: middle-click cancel ─────────────────────────────────

  const doctorCancel = async (dp: PrescriptionDayPart) => {
    if (!canEdit || !isDoctor || !dp.isPlanned || dp.isCompleted) return;
    await onComplete(dp.id); // marks isPlannedFinished
  };

  // ── nurse: execute popover ──────────────────────────────────────

  const [execAnchor, setExecAnchor] = useState<HTMLElement | null>(null);
  const [execDp, setExecDp] = useState<PrescriptionDayPart | null>(null);
  const [execDose, setExecDose] = useState('');
  const [exec2p, setExec2p] = useState(false);
  const [exec2pId, setExec2pId] = useState('');
  const [executing, setExecuting] = useState(false);

  const openExecute = (dp: PrescriptionDayPart, el: HTMLElement) => {
    if (!canEdit || !isNurse || !dp.isPlanned || dp.isCompleted) return;
    setExecDp(dp);
    setExecDose(dp.dose ?? '');
    setExec2p(false);
    setExec2pId('');
    setExecuting(false);
    setExecAnchor(el);
  };

  const closeExecute = () => { setExecAnchor(null); setExecDp(null); };

  const commitExecute = async () => {
    if (!execDp || !onExecute) return;
    setExecuting(true);
    try {
      await onExecute(execDp.id, execDose, exec2p, exec2p ? exec2pId : undefined);
      closeExecute();
    } finally {
      setExecuting(false);
    }
  };

  // ── delete confirmation ────────────────────────────────────────

  const [deleteAnchor, setDeleteAnchor] = useState<HTMLElement | null>(null);
  const [deleteItemId, setDeleteItemId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const openDeleteConfirm = (itemId: string, el: HTMLElement) => {
    setDeleteItemId(itemId);
    setDeleteAnchor(el);
  };

  const closeDeleteConfirm = () => { setDeleteAnchor(null); setDeleteItemId(null); };

  const confirmDelete = async () => {
    if (!deleteItemId) return;
    setDeleting(true);
    try {
      await onRemoveItem(deleteItemId);
      closeDeleteConfirm();
    } finally {
      setDeleting(false);
    }
  };

  // ── add drug (doctor only) ──────────────────────────────────────

  const [medSearch, setMedSearch] = useState('');
  const [medOptions, setMedOptions] = useState<MedicineCatalogItem[]>([]);
  const [selectedMed, setSelectedMed] = useState<MedicineCatalogItem | undefined>(undefined);
  const [newMethod, setNewMethod] = useState('');
  const [newRegime, setNewRegime] = useState('');
  const [addingDrug, setAddingDrug] = useState(false);

  const handleMedSearch = useCallback(async (q: string) => {
    setMedSearch(q);
    if (q.length < 2) { setMedOptions([]); return; }
    try {
      const res = await onSearchMedicine(q);
      setMedOptions(res.length > 0 ? res : fallbackCatalog.filter(m =>
        m.name.toLowerCase().includes(q.toLowerCase())));
    } catch {
      setMedOptions(fallbackCatalog.filter(m =>
        m.name.toLowerCase().includes(q.toLowerCase())));
    }
  }, [onSearchMedicine]);

  const handleAddDrug = async () => {
    if (!selectedMed) return;
    const allergy = allergies.find(a =>
      a.allergenName.toLowerCase() === selectedMed.name.toLowerCase()
    );
    if (allergy) {
      alert(`У пацієнта алергія на препарат "${allergy.allergenName}"!`);
      return;
    }
    setAddingDrug(true);
    try {
      await onAddItem({
        medicineName: selectedMed.name,
        medicineMethod: newMethod || undefined,
        regime: newRegime || undefined,
      });
      setSelectedMed(undefined);
      setMedSearch('');
      setNewMethod('');
      setNewRegime('');
      setMedOptions([]);
    } finally {
      setAddingDrug(false);
    }
  };

  // ── render ──────────────────────────────────────────────────────

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* ── header: date nav + legend ── */}
      <Paper sx={{ p: 1.5, display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
        <IconButton size="small" onClick={shiftLeft} disabled={viewStart === 0}>
          <ArrowBackIosNew fontSize="small" />
        </IconButton>
        <Typography variant="body2" sx={{ fontWeight: 600, minWidth: 120, textAlign: 'center' }}>
          {visibleDates.length > 0
            ? `${formatDate(visibleDates[0])} — ${formatDate(visibleDates[visibleDates.length - 1])}`
            : 'Немає даних'}
        </Typography>
        <IconButton size="small" onClick={shiftRight} disabled={viewStart + daysToShow >= allDates.length}>
          <ArrowForwardIos fontSize="small" />
        </IconButton>
        <Box sx={{ flex: 1 }} />
        <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
          {[
            ['#BBDEFB', 'Заплановано'],
            ['#C8E6C9', 'Виконано'],
            ['#E1BEE7', 'Відмінено'],
            ['#A5D6A7', 'Завершено'],
          ].map(([color, label]) => (
            <Box key={label} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <Box sx={{ width: 14, height: 14, bgcolor: color, border: '1px solid #ccc', borderRadius: 0.5 }} />
              <Typography variant="caption">{label}</Typography>
            </Box>
          ))}
        </Box>
      </Paper>

      {/* ── add drug row (doctor only) ── */}
      {canEdit && isDoctor && (
        <Paper sx={{ p: 1.5, display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
          <Autocomplete
            size="small"
            options={medOptions}
            getOptionLabel={o => o.name}
            inputValue={medSearch}
            onInputChange={(_, v) => handleMedSearch(v)}
            value={selectedMed}
            onChange={(_, v) => setSelectedMed(v ?? undefined)}
            loading={addingDrug}
            renderInput={p => <TextField {...p} label="Препарат" sx={{ minWidth: 220 }} />}
            disableClearable
          />
          <TextField size="small" label="Спосіб" value={newMethod}
            onChange={e => setNewMethod(e.target.value)} sx={{ width: 120 }} />
          <TextField size="small" label="Режим" value={newRegime}
            onChange={e => setNewRegime(e.target.value)} sx={{ width: 100 }} />
          <Button variant="contained" size="small" disabled={!selectedMed || addingDrug}
            onClick={handleAddDrug} startIcon={<Add />}>Додати</Button>
        </Paper>
      )}

      {/* ── grid ── */}
      {loading ? (
        <CircularProgress sx={{ display: 'block', mx: 'auto', mt: 4 }} />
      ) : gridItems.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">
            {isDoctor
              ? 'Немає препаратів. Додайте препарат щоб розпочати.'
              : 'Немає призначень для виконання.'}
          </Typography>
        </Paper>
      ) : (
        <Paper sx={{ overflow: 'auto' }}>
          <Box component="table" sx={{
            borderCollapse: 'collapse', minWidth: 200 + visibleDates.length * 300,
            '& th, & td': { border: '1px solid', borderColor: 'divider', p: 0 },
          }}>
            <Box component="thead">
              <Box component="tr">
                <Box component="th" sx={{
                  position: 'sticky', left: 0, bgcolor: 'background.paper',
                  zIndex: 2, minWidth: 180, p: '6px 8px !important',
                }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>Препарат / Метод</Typography>
                </Box>
                {visibleDates.map(date => (
                  <Box component="th" key={date} colSpan={4} sx={{ p: '4px 2px !important', bgcolor: 'grey.100' }}>
                    <Typography variant="caption" sx={{ fontWeight: 700 }}>
                      {formatDate(date)}
                    </Typography>
                  </Box>
                ))}
                {canEdit && isDoctor && <Box component="th" sx={{ width: 40 }} />}
              </Box>
              <Box component="tr">
                <Box component="th" sx={{ position: 'sticky', left: 0, bgcolor: 'background.paper', zIndex: 2 }} />
                {visibleDates.map(date =>
                  PERIODS.map(p => (
                    <Box component="th" key={`${date}-${p}`}
                      sx={{ width: 68, fontSize: 10, color: 'text.secondary', p: '2px !important' }}>
                      {PERIOD_LABELS[p]}
                    </Box>
                  ))
                )}
                {canEdit && isDoctor && <Box component="th" />}
              </Box>
            </Box>
            <Box component="tbody">
              {gridItems.map(item => (
                <Box component="tr" key={item.id}>
                  {/* sticky drug name */}
                  <Box component="td" sx={{
                    position: 'sticky', left: 0, bgcolor: 'background.paper',
                    zIndex: 1, p: '4px 8px !important', minWidth: 180,
                  }}>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {item.medicineName}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {item.medicineMethod || ''}{item.regime ? ` • ${item.regime}` : ''}
                    </Typography>
                  </Box>

                  {/* day cells */}
                  {visibleDates.map(date =>
                    PERIODS.map(period => {
                      const dp = item.cells.get(dayPartKey(date, period));
                      const bg = cellBg(dp);
                      const label = cellLabel(dp);
                      const isEditing = editingCell === (dp?.id);

                      // common cell click handler
                      const onClick = (e: React.MouseEvent) => {
                        if (!dp || !canEdit) return;
                        if (dp.isCompleted || dp.isCompletedFinished) return;
                        if (isDoctor && !dp.isPlanned) { startEdit(dp); return; }
                        if (isDoctor && dp.isPlanned) { startEdit(dp); return; }
                        if (isNurse && dp.isPlanned) { openExecute(dp, e.currentTarget as HTMLElement); return; }
                      };

                      const onAuxClick = (e: React.MouseEvent) => {
                        e.preventDefault();
                        if (!dp || !canEdit) return;
                        if (isDoctor && dp.isPlanned && !dp.isCompleted) doctorCancel(dp);
                      };

                      return (
                        <Box component="td" key={`${date}-${period}`} sx={{
                          width: 68, height: 32, cursor: bg === '#fff' || !dp ? 'default' : 'pointer',
                          bgcolor: bg, textAlign: 'center', verticalAlign: 'middle',
                          position: 'relative',
                        }}
                          onClick={onClick}
                          onAuxClick={onAuxClick}
                        >
                          {isEditing ? (
                            <Box component="form" onSubmit={e => { e.preventDefault(); if (dp) commitEdit(dp); }}
                              sx={{ position: 'absolute', inset: 0, zIndex: 3, display: 'flex' }}>
                              <input autoFocus value={editingDose}
                                onChange={e => setEditingDose(e.target.value)}
                                onBlur={() => dp && commitEdit(dp)}
                                style={{
                                  width: '100%', border: '2px solid #1976d2',
                                  textAlign: 'center', fontSize: 11, padding: 0, outline: 'none',
                                }} />
                            </Box>
                          ) : (
                            <Tooltip title={dp ? `${PERIOD_FULL[dp.period]}: ${dp.dose ?? '—'}` : ''} arrow>
                              <Typography variant="caption" sx={{
                                fontSize: 10, lineHeight: '32px',
                                color: dp?.isPlanned ? '#1565c0' : dp?.isCompleted ? '#2e7d32' : 'text.secondary',
                                fontWeight: dp?.isPlanned || dp?.isCompleted ? 600 : 400,
                                userSelect: 'none',
                              }}>
                                {label}
                              </Typography>
                            </Tooltip>
                          )}
                        </Box>
                      );
                    })
                  )}

                  {/* remove button (doctor only) */}
                  {canEdit && isDoctor && (
                    <Box component="td" sx={{ width: 40, textAlign: 'center' }}>
                      <IconButton size="small" onClick={(e) => openDeleteConfirm(item.id, e.currentTarget as HTMLElement)}>
                        <Delete fontSize="small" />
                      </IconButton>
                    </Box>
                  )}
                </Box>
              ))}
            </Box>
          </Box>
        </Paper>
      )}

      {/* ── nurse: execute popover ── */}
      <Popover
        open={Boolean(execAnchor)}
        anchorEl={execAnchor}
        onClose={closeExecute}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      >
        <Box sx={{ p: 2, minWidth: 240, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          <Typography variant="subtitle2">
            {execDp ? `${PERIOD_FULL[execDp.period]}: ${execDp.dose ?? '—'}` : 'Виконання дози'}
          </Typography>
          <TextField size="small" label="Фактична доза" value={execDose}
            onChange={e => setExecDose(e.target.value)} autoFocus />
          <FormControlLabel
            control={<Checkbox size="small" checked={exec2p} onChange={e => setExec2p(e.target.checked)} />}
            label={<Typography variant="body2">2-факторна авторизація</Typography>}
          />
          {exec2p && (
            <TextField size="small" label="ID другої особи" value={exec2pId}
              onChange={e => setExec2pId(e.target.value)} />
          )}
          <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
            <Button size="small" onClick={closeExecute} disabled={executing}>Скасувати</Button>
            <Button size="small" variant="contained" color="success"
              disabled={executing || !execDose.trim()}
              onClick={commitExecute}>Виконати</Button>
          </Box>
        </Box>
      </Popover>

      {/* ── delete confirmation popover ── */}
      <Popover
        open={Boolean(deleteAnchor)}
        anchorEl={deleteAnchor}
        onClose={closeDeleteConfirm}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      >
        <Box sx={{ p: 2, minWidth: 220 }}>
          <Typography variant="body2" sx={{ mb: 1.5 }}>
            Видалити препарат? Дані про дози будуть втрачені.
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
            <Button size="small" onClick={closeDeleteConfirm} disabled={deleting}>Скасувати</Button>
            <Button size="small" variant="contained" color="error"
              disabled={deleting} onClick={confirmDelete}>Видалити</Button>
          </Box>
        </Box>
      </Popover>
    </Box>
  );
}
