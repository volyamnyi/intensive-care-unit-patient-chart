import { useState, useRef, useEffect, useCallback } from 'react';
import { Autocomplete, TextField, Box, Typography, CircularProgress } from '@mui/material';
import { patientApi } from '../../api/endpoints';
import type { PatientDto } from '../../types';

interface PatientSearchProps {
  onSelect: (patient: PatientDto) => void;
  label?: string;
}

export default function PatientSearch({ onSelect, label }: PatientSearchProps) {
  const resolvedLabel = label ?? 'ПІБ, телефон або № медкарти';
  const [search, setSearch] = useState('');
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [selected, setSelected] = useState<PatientDto | null>(null);
  const [loading, setLoading] = useState(false);

  const debounceRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const abortRef = useRef<AbortController>(undefined);

  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
      if (abortRef.current) abortRef.current.abort();
    };
  }, []);

  const performSearch = useCallback(async (query: string) => {
    if (abortRef.current) abortRef.current.abort();
    if (query.length < 2) { setPatients([]); return; }

    const controller = new AbortController();
    abortRef.current = controller;
    setLoading(true);
    try {
      const res = await patientApi.search(query, controller.signal);
      setPatients(res.data);
    } catch (err: unknown) {
      if (err instanceof Error && err.name === 'CanceledError') return;
    } finally {
      setLoading(false);
    }
  }, []);

  const handleInputChange = (_: unknown, value: string) => {
    setSearch(value);
    if (selected) setSelected(null);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => performSearch(value), 300);
  };

  return (
    <Autocomplete
      inputValue={search}
      onInputChange={handleInputChange}
      value={selected}
      onChange={(_, v) => { setSelected(v); if (v) onSelect(v); }}
      options={patients}
      getOptionLabel={(p) => `${p.fullName} (${p.externalId1})`}
      isOptionEqualToValue={(o, v) => o.id === v.id}
      filterOptions={(x) => x}
      loading={loading}
      noOptionsText={search.length < 2 ? 'Введіть мінімум 2 символи' : 'Пацієнтів не знайдено'}
      renderOption={(props, p) => {
        const { key, ...rest } = props;
        return (
          <Box component="li" key={key} {...rest} sx={{ px: 2, py: 1.5 }}>
            <Box>
              <Typography variant="body1" sx={{ fontWeight: 600 }}>{p.fullName}</Typography>
              <Typography variant="body2" color="text.secondary">
                {p.externalId1} &middot; {p.birthDate} &middot; {p.address?.split(',')[0]?.trim()}
              </Typography>
            </Box>
          </Box>
        );
      }}
      renderInput={(params) => (
        <TextField
          {...params}
          label={resolvedLabel}
          slotProps={{
            ...params.slotProps,
            input: {
              ...params.slotProps?.input,
              endAdornment: (
                <>
                  {loading && <CircularProgress size={20} />}
                  {params.slotProps?.input?.endAdornment}
                </>
              ),
            },
          }}
        />
      )}
    />
  );
}
