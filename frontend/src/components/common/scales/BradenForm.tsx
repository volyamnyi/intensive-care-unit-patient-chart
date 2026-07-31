import { useState, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

interface BradenFormProps {
  onCalculate: (rawData: Record<string, unknown>) => void;
  disabled?: boolean;
}

const sensoryOptions = [
  { value: '1', label: '1 — Цілком обмежена' },
  { value: '2', label: '2 — Дуже обмежена' },
  { value: '3', label: '3 — Злегка обмежена' },
  { value: '4', label: '4 — Не порушена' },
];

const moistureOptions = [
  { value: '1', label: '1 — Постійно волога' },
  { value: '2', label: '2 — Дуже волога' },
  { value: '3', label: '3 — Зрідка волога' },
  { value: '4', label: '4 — Суха' },
];

const activityOptions = [
  { value: '1', label: '1 — Ліжковий режим' },
  { value: '2', label: '2 — Прикутий до стільця' },
  { value: '3', label: '3 — Ходить зрідка' },
  { value: '4', label: '4 — Часто ходить' },
];

const mobilityOptions = [
  { value: '1', label: '1 — Цілком нерухомий' },
  { value: '2', label: '2 — Дуже обмежена' },
  { value: '3', label: '3 — Злегка обмежена' },
  { value: '4', label: '4 — Не обмежена' },
];

const nutritionOptions = [
  { value: '1', label: '1 — Дуже погана' },
  { value: '2', label: '2 — Ймовірно недостатня' },
  { value: '3', label: '3 — Достатня' },
  { value: '4', label: '4 — Відмінна' },
];

const frictionOptions = [
  { value: '1', label: '1 — Проблема' },
  { value: '2', label: '2 — Потенційна проблема' },
  { value: '3', label: '3 — Відсутня проблема' },
];

function riskCategory(total: number): string {
  if (total >= 19) return 'Низький';
  if (total >= 15) return 'Помірний';
  if (total >= 13) return 'Середній';
  if (total >= 10) return 'Високий';
  return 'Дуже високий';
}

export default function BradenForm({ onCalculate, disabled }: BradenFormProps) {
  const [sensoryPerception, setSensoryPerception] = useState('3');
  const [moisture, setMoisture] = useState('3');
  const [activity, setActivity] = useState('2');
  const [mobility, setMobility] = useState('3');
  const [nutrition, setNutrition] = useState('3');
  const [frictionShear, setFrictionShear] = useState('3');

  const total = useMemo(() => {
    return [sensoryPerception, moisture, activity, mobility, nutrition, frictionShear]
      .map(v => parseInt(v, 10) || 0)
      .reduce((a, b) => a + b, 0);
  }, [sensoryPerception, moisture, activity, mobility, nutrition, frictionShear]);

  const handleCalculate = () => {
    onCalculate({
      sensoryPerception: parseInt(sensoryPerception, 10),
      moisture: parseInt(moisture, 10),
      activity: parseInt(activity, 10),
      mobility: parseInt(mobility, 10),
      nutrition: parseInt(nutrition, 10),
      frictionShear: parseInt(frictionShear, 10),
    });
  };

  return (
    <div className="flex flex-col gap-2">
      <p className="text-xs font-semibold mb-1">Шкала Браден — ризик пролежнів</p>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-1.5">
        <Select value={sensoryPerception} onValueChange={setSensoryPerception} disabled={disabled}>
          <SelectTrigger className="h-7 text-xs"><SelectValue placeholder="Сенсорне сприйняття" /></SelectTrigger>
          <SelectContent>{sensoryOptions.map(o => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}</SelectContent>
        </Select>
        <Select value={moisture} onValueChange={setMoisture} disabled={disabled}>
          <SelectTrigger className="h-7 text-xs"><SelectValue placeholder="Вологість" /></SelectTrigger>
          <SelectContent>{moistureOptions.map(o => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}</SelectContent>
        </Select>
        <Select value={activity} onValueChange={setActivity} disabled={disabled}>
          <SelectTrigger className="h-7 text-xs"><SelectValue placeholder="Активність" /></SelectTrigger>
          <SelectContent>{activityOptions.map(o => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}</SelectContent>
        </Select>
        <Select value={mobility} onValueChange={setMobility} disabled={disabled}>
          <SelectTrigger className="h-7 text-xs"><SelectValue placeholder="Мобільність" /></SelectTrigger>
          <SelectContent>{mobilityOptions.map(o => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}</SelectContent>
        </Select>
        <Select value={nutrition} onValueChange={setNutrition} disabled={disabled}>
          <SelectTrigger className="h-7 text-xs"><SelectValue placeholder="Харчування" /></SelectTrigger>
          <SelectContent>{nutritionOptions.map(o => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}</SelectContent>
        </Select>
        <Select value={frictionShear} onValueChange={setFrictionShear} disabled={disabled}>
          <SelectTrigger className="h-7 text-xs"><SelectValue placeholder="Тертя/зсув" /></SelectTrigger>
          <SelectContent>{frictionOptions.map(o => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}</SelectContent>
        </Select>
      </div>
      <div className="text-xs font-semibold">
        Сума: {total} — Ризик: {riskCategory(total)}
      </div>
      <div>
        <Button size="sm" onClick={handleCalculate} disabled={disabled}>
          Зберегти Браден
        </Button>
      </div>
    </div>
  );
}
