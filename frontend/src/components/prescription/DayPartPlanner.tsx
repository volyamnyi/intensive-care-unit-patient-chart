import { useState } from 'react';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import type { PrescriptionDayPart } from '../../types/medication';

interface DayPartPlannerProps {
  dayParts: PrescriptionDayPart[];
  onPlan: (dayPartId: string, dose: string) => void;
  onComplete: (dayPartId: string) => void;
  canPlan?: boolean;
  canComplete?: boolean;
  planning?: boolean;
  completing?: boolean;
}

const periodLabels: Record<string, string> = {
  morning: 'Ранок',
  day: 'День',
  evening: 'Вечір',
  night: 'Ніч',
};

export default function DayPartPlanner({
  dayParts,
  onPlan,
  onComplete,
  canPlan,
  canComplete,
  planning,
  completing,
}: DayPartPlannerProps) {
  const [doses, setDoses] = useState<Record<string, string>>({});

  if (dayParts.length === 0) {
    return <p className="text-muted-foreground">Немає запланованих частин доби</p>;
  }

  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Період</TableHead>
            <TableHead>Доза</TableHead>
            <TableHead>Статус</TableHead>
            <TableHead>Дії</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {dayParts.map((part) => (
            <TableRow key={part.id}>
              <TableCell>{periodLabels[part.period] || part.period}</TableCell>
              <TableCell>{part.dose || '—'}</TableCell>
              <TableCell>
                <div className="flex gap-0.5">
                  {part.isCompleted && <Badge variant="default" className="bg-green-500/10 text-green-600 dark:text-green-400">Виконано</Badge>}
                  {part.isPlanned && !part.isCompleted && <Badge variant="secondary">Заплановано</Badge>}
                  {!part.isPlanned && <Badge variant="outline">Не заплановано</Badge>}
                </div>
              </TableCell>
              <TableCell>
                {!part.isPlanned && canPlan && (
                  <div className="flex gap-1 items-center">
                    <Input
                      placeholder="Доза"
                      value={doses[part.id] || ''}
                      onChange={(e) => setDoses((prev) => ({ ...prev, [part.id]: e.target.value }))}
                      disabled={planning}
                      className="w-24"
                    />
                    <Button
                      size="sm"
                      className="min-h-11"
                      variant="default"
                      disabled={planning || !doses[part.id]}
                      onClick={() => onPlan(part.id, doses[part.id])}
                    >
                      Запланувати
                    </Button>
                  </div>
                )}
                {part.isPlanned && !part.isCompleted && canComplete && (
                  <Button
                    size="sm"
                    className="min-h-11"
                    variant="default"
                    disabled={completing}
                    onClick={() => onComplete(part.id)}
                  >
                    Завершити
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
