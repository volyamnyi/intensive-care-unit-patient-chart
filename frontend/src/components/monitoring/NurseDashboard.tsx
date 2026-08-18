import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import ClinicalDayTimeline from '../icu/ClinicalDayTimeline';
import IntensiveCareCard from './IntensiveCareCard';
import type { DashboardProps } from './dashboardTypes';

export default function NurseDashboard(props: DashboardProps) {
  const {
    episode, clinicalDays, selectedDay, onSelectDay,
    records, orders, balanceItems,
    isLocked, isNurse, user, onRefresh, onFeedback, dayLoading,
  } = props;

  const paperClass = cn('rounded-xl border border-border bg-card p-3 shadow-sm');

  const dayChipColor = (status: string) => {
    if (status === 'OPEN' || status === 'REOPENED') return 'warning';
    if (status === 'NURSE_SIGNED') return 'info';
    if (status === 'DOCTOR_SIGNED') return 'success';
    return 'default';
  };

  return (
    <div>
      <div className={cn(paperClass, 'mb-3 flex flex-wrap items-center justify-between gap-2')}>
        <div className="flex items-center gap-1.5">
          <h1 className="font-rubik text-base font-extrabold" style={{ fontWeight: 800 }}>
            {episode.patientName || 'Patient'}
          </h1>
          {selectedDay?.weightKg && (
            <Badge variant="outline" className="text-[11px] font-semibold">{`${selectedDay.weightKg} kg`}</Badge>
          )}
          {episode.ward && (
            <Badge variant="outline" className="text-[11px] font-semibold">{[episode.ward, episode.bedNumber].filter(Boolean).join(' / ')}</Badge>
          )}
          {selectedDay && (
            <Badge variant="outline" className="text-[12px] font-semibold">{`День ${selectedDay.dayNumber}`}</Badge>
          )}
          <Badge
            variant={dayChipColor(selectedDay?.status ?? '') as 'default' | 'secondary' | 'destructive' | 'outline' | 'ghost' | 'link'}
            className="text-[11px] font-semibold"
          >
            {selectedDay?.status === 'OPEN' ? 'Відкритий'
              : selectedDay?.status === 'NURSE_SIGNED' ? 'Підписано медсестрою'
              : selectedDay?.status === 'DOCTOR_SIGNED' ? 'Підписано лікарем'
              : selectedDay?.status === 'REOPENED' ? 'Відкрито повторно'
              : 'Закрито'}
          </Badge>
        </div>
        <span className="font-mulish text-xs text-muted-foreground">
          {'Епізод #' + episode.id?.slice(0, 8)}
        </span>
      </div>

      <div className={cn(paperClass, 'mb-3')}>
        <ClinicalDayTimeline days={clinicalDays} selectedDayId={selectedDay?.id} onSelectDay={onSelectDay} />
      </div>

      <IntensiveCareCard
        episode={episode}
        selectedDay={selectedDay}
        records={records}
        orders={orders}
        balanceItems={balanceItems}
        isNurse={isNurse}
        isLocked={isLocked}
        user={user ?? null}
        onRefresh={onRefresh}
        onFeedback={onFeedback}
        loading={dayLoading}
      />
    </div>
  );
}
