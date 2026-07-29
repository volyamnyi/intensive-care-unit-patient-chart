import { Alert, AlertDescription } from '@/components/ui/alert';
import { TriangleAlert } from 'lucide-react';
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
    <Alert variant="default" className="mt-1 border-amber-500/30 bg-amber-50 dark:bg-amber-950/20">
      <TriangleAlert className="size-4" />
      <AlertDescription>
        {`Увага: пацієнт має алергію на ${matches.map((a) => a.allergenName).join(', ')}`}
      </AlertDescription>
    </Alert>
  );
}
