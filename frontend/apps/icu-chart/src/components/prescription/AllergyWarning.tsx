import { Alert } from '@mui/material';
import type { AllergyItem } from '../../types';

interface AllergyWarningProps {
  medicineName: string;
  allergies: AllergyItem[];
}

export default function AllergyWarning({ medicineName, allergies }: AllergyWarningProps) {
  if (!medicineName || allergies.length === 0) return null;

  const normalized = medicineName.toLowerCase();
  const matches = allergies.filter((a) => a.allergenName.toLowerCase().includes(normalized) || normalized.includes(a.allergenName.toLowerCase()));

  if (matches.length === 0) return null;

  return (
    <Alert severity="warning" sx={{ mt: 1 }}>
      {`Увага: пацієнт має алергію на ${matches.map((a) => a.allergenName).join(', ')}`}
    </Alert>
  );
}
