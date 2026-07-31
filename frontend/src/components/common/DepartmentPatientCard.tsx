import { cn } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { useThemeMode } from '../../styles/ThemeContext';
import { useNavigate } from 'react-router-dom';
import type { DepartmentPatient } from '../../types';

const statusConfig: Record<string, { label: string; borderColor: string }> = {
  OPEN: { label: 'Відкрито', borderColor: '#FF9800' },
  NURSE_SIGNED: { label: 'Підписано медсестрою', borderColor: '#2196F3' },
  DOCTOR_SIGNED: { label: 'Підписано лікарем', borderColor: '#4CAF50' },
  CLOSED: { label: 'Закрито', borderColor: '#9E9E9E' },
  REOPENED: { label: 'Відкрито повторно', borderColor: '#FF9800' },
};

function badgeVariantFor(status: string): 'default' | 'secondary' | 'destructive' | 'outline' | null {
  const map: Record<string, 'default' | 'secondary' | 'outline'> = {
    OPEN: 'default',
    NURSE_SIGNED: 'secondary',
    DOCTOR_SIGNED: 'default',
    CLOSED: 'secondary',
    REOPENED: 'default',
  };
  return map[status] || 'outline';
}

function daysSinceLabel(days: number): string {
  if (days === 0) return 'Сьогодні';
  if (days === 1) return '1 день';
  if (days < 5) return `${days} дні`;
  return `${days} днів`;
}

interface Props {
  patient: DepartmentPatient;
}

export default function DepartmentPatientCard({ patient }: Props) {
  const navigate = useNavigate();
  useThemeMode();
  const dayConfig = patient.latestDayStatus ? statusConfig[patient.latestDayStatus] : null;

  return (
    <Card
      className="cursor-pointer transition-shadow duration-200 hover:translate-y-[-2px]"
      style={{
        borderLeft: dayConfig ? `4px solid ${dayConfig.borderColor}` : undefined,
      }}
      onClick={() => navigate('/icu/doctor/episode/' + patient.id)}
    >
      <CardContent className="p-4">
        <div className="mb-1 flex items-start justify-between">
          <p className="font-rubik text-sm font-bold leading-tight">
            {patient.patientName ?? 'Невідомий пацієнт'}
          </p>
          {patient.latestDayNumber != null && (
            <Badge variant="outline" className="ml-1 shrink-0">{`День ${patient.latestDayNumber}`}</Badge>
          )}
        </div>

        <div className="mb-1 flex flex-wrap gap-0.5">
          {patient.latestDayStatus && dayConfig && (
            <Badge
              variant={badgeVariantFor(patient.latestDayStatus)}
              className={cn(
                patient.latestDayStatus === 'OPEN' || patient.latestDayStatus === 'REOPENED' ? 'bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 border-yellow-500/20' : '',
                patient.latestDayStatus === 'DOCTOR_SIGNED' ? 'bg-green-500/10 text-green-600 dark:text-green-400 border-green-500/20' : '',
              )}
            >
              {dayConfig.label}
            </Badge>
          )}
          {patient.ward && patient.bedNumber && (
            <Badge variant="outline">{`${patient.ward} / ${patient.bedNumber}`}</Badge>
          )}
          <Badge variant="outline">{daysSinceLabel(patient.daysSinceAdmission)}</Badge>
        </div>

        {patient.admissionDiagnosis && (
          <span className="mb-0.5 block text-xs text-muted-foreground font-mulish">
            {patient.admissionDiagnosis}
          </span>
        )}

        {patient.attendingDoctorName && (
          <span className="block text-xs text-muted-foreground font-mulish">
            Лікар: {patient.attendingDoctorName}
          </span>
        )}
      </CardContent>
    </Card>
  );
}
