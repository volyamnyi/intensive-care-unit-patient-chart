import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { useThemeMode } from '../../styles/ThemeContext';
import { cn } from '@/lib/utils';
import type { FluidBalanceItem } from '../../types';

const INTAKE_LABELS: Record<string, string> = {
  crystalloids: 'Кристалоїди',
  colloids: 'Колоїди',
  blood: 'Кров',
  plasma: 'Плазма',
  nutrition: 'Харчування',
  oral: 'Перорально',
  other: 'Інше',
};

const OUTPUT_LABELS: Record<string, string> = {
  diuresis: 'Діурез',
  drainage: 'Дренаж',
  gastric: 'Зонд',
  vomiting: 'Блювання',
  stool: 'Кал',
  bloodLoss: 'Крововтрата',
  other: 'Інші',
};

interface FluidBalancePanelProps {
  items: FluidBalanceItem[];
  onRecalculate?: () => void;
  loading?: boolean;
}

export default function FluidBalancePanel({ items, onRecalculate, loading }: FluidBalancePanelProps) {
  const { mode } = useThemeMode();
  const isDark = mode === 'dark';
  const totalIntake = items.reduce((s, i) => s + (i.intake || 0), 0);
  const totalOutput = items.reduce((s, i) => s + (i.output || 0), 0);
  const dailyBalance = totalIntake - totalOutput;
  const lastItem = items[items.length - 1];
  const cumulativeBalance = lastItem?.cumulativeBalance ?? 0;
  const intakeByCategory = lastItem?.intakeByCategory;
  const outputByCategory = lastItem?.outputByCategory;

  const renderCategoryList = (map: Record<string, number> | undefined, labels: Record<string, string>) => {
    if (!map) return null;
    const entries = Object.entries(map).filter(([, v]) => v > 0);
    if (entries.length === 0) return <span className="text-xs text-muted-foreground">—</span>;
    return entries.map(([key, val]) => (
      <div key={key} className="flex justify-between text-xs py-0.3">
        <span className="text-xs">{labels[key] || key}</span>
        <span className="text-xs font-semibold">{val} ml</span>
      </div>
    ));
  };

  return (
    <div
      className={cn(
        'rounded-xl border bg-card text-card-foreground shadow-sm p-2.5 mb-2'
      )}
      style={{
        borderColor: isDark ? '#2A2A2A' : '#E8E6E1',
        backgroundColor: isDark ? '#141414' : '#FFFFFF',
        boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 8px rgba(0,0,0,0.04)',
      }}
    >
      <h6 className="font-rubik mb-2 text-base font-medium">
        Водний баланс
        <Badge variant="outline" className="ml-1 text-[9px] font-bold h-[18px]">Auto</Badge>
      </h6>

      <div className="flex flex-wrap gap-2">
        {/* Intake breakdown */}
        <div className="flex-1 min-w-[180px]">
          <p className="font-bold text-xs mb-0.5" style={{ color: '#4CAF50' }}>
            Надходження — {totalIntake} ml
          </p>
          {intakeByCategory && renderCategoryList(intakeByCategory, INTAKE_LABELS)}
        </div>

        {/* Output breakdown */}
        <div className="flex-1 min-w-[180px]">
          <p className="font-bold text-xs mb-0.5" style={{ color: '#FF9100' }}>
            Виведення — {totalOutput} ml
          </p>
          {outputByCategory && renderCategoryList(outputByCategory, OUTPUT_LABELS)}
        </div>
      </div>

      <Separator className="my-1.5" />

      <div className="flex justify-between mb-0.5">
        <span className="text-sm text-muted-foreground">Денний баланс</span>
        <span className="font-bold" style={{ color: dailyBalance < 0 ? '#FF5252' : '#4CAF50' }}>
          {dailyBalance >= 0 ? '+' : ''}{dailyBalance} ml
        </span>
      </div>
      <div className="flex justify-between mb-1.5">
        <span className="text-sm text-muted-foreground">Кумулятивний баланс</span>
        <span className="font-bold" style={{ color: cumulativeBalance < 0 ? '#FF5252' : '#4CAF50' }}>
          {cumulativeBalance >= 0 ? '+' : ''}{cumulativeBalance} ml
        </span>
      </div>
      {onRecalculate && (
        <Button size="sm" variant="outline" onClick={onRecalculate} disabled={loading}>
          {loading ? 'Розрахунок...' : 'Перерахувати'}
        </Button>
      )}
    </div>
  );
}
