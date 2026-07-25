import { useState, useEffect, useRef } from 'react';
import { Box, Grid, TextField, Button, Autocomplete } from '@mui/material';
import type { MedicineCatalogItem, PrescriptionItemAddRequest, AllergyItem } from '../../types';
import AllergyWarning from './AllergyWarning';

interface PrescriptionItemFormProps {
  onSubmit: (data: PrescriptionItemAddRequest) => void;
  onSearchMedicine: (keyword: string) => Promise<MedicineCatalogItem[]>;
  allergies?: AllergyItem[];
  disabled?: boolean;
}

export default function PrescriptionItemForm({ onSubmit, onSearchMedicine, allergies, disabled }: PrescriptionItemFormProps) {
  const [medicine, setMedicine] = useState<MedicineCatalogItem | null>(null);
  const [medicineMethod, setMedicineMethod] = useState('');
  const [regime, setRegime] = useState('');
  const [options, setOptions] = useState<MedicineCatalogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (inputValue.length < 2) {
      setOptions([]);
      return;
    }
    const controller = new AbortController();
    abortRef.current = controller;
    setLoading(true);
    const timeout = setTimeout(() => {
      onSearchMedicine(inputValue)
        .then((res) => {
          if (!controller.signal.aborted) {
            setOptions(res);
          }
        })
        .catch(() => setOptions([]))
        .finally(() => {
          if (!controller.signal.aborted) {
            setLoading(false);
          }
        });
    }, 300);
    return () => {
      clearTimeout(timeout);
      controller.abort();
    };
  }, [inputValue, onSearchMedicine]);

  const handleSubmit = () => {
    if (!medicine) return;
    onSubmit({
      medicineName: medicine.name,
      medicineMethod: medicineMethod || undefined,
      regime: regime || undefined,
    });
    setMedicine(null);
    setMedicineMethod('');
    setRegime('');
    setInputValue('');
    setOptions([]);
  };

  const medicineName = medicine?.name || '';

  return (
    <Box>
      <Grid container spacing={2} sx={{ alignItems: 'flex-start' }}>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Autocomplete
            options={options}
            getOptionLabel={(option) => (typeof option === 'string' ? option : option.name)}
            inputValue={inputValue}
            onInputChange={(_event, value) => setInputValue(value)}
            value={medicine}
            onChange={(_event, newValue) => setMedicine(newValue)}
            loading={loading}
            disabled={disabled}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Препарат"
                size="small"
                fullWidth
              />
            )}
            loadingText="Завантаження..."
          />
        </Grid>
        <Grid size={{ xs: 6, sm: 3 }}>
          <TextField
            fullWidth
            size="small"
            label="Спосіб введення"
            value={medicineMethod}
            onChange={(e) => setMedicineMethod(e.target.value)}
            disabled={disabled}
          />
        </Grid>
        <Grid size={{ xs: 6, sm: 3 }}>
          <TextField
            fullWidth
            size="small"
            label="Режим"
            value={regime}
            onChange={(e) => setRegime(e.target.value)}
            disabled={disabled}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 2 }}>
          <Button variant="contained" size="small" disabled={disabled || !medicine} onClick={handleSubmit}>
            Додати
          </Button>
        </Grid>
      </Grid>
      <AllergyWarning medicineName={medicineName} allergies={allergies ?? []} />
    </Box>
  );
}
