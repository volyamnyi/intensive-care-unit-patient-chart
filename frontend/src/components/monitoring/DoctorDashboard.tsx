import { cn } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import ClinicalDayTimeline from '../common/ClinicalDayTimeline';
import IntensiveCareCard from './IntensiveCareCard';
import type { DashboardProps } from './dashboardTypes';

export default function DoctorDashboard(props: DashboardProps) {
  const {
    episode, clinicalDays, selectedDay, onSelectDay,
    records, orders, balanceItems,
    isLocked, isNurse, user, onRefresh, onFeedback,
  } = props;

  const dayBadgeVariant = (status: string): 'default' | 'secondary' | 'outline' | null => {
    if (status === 'OPEN' || status === 'REOPENED') return 'default';
    if (status === 'NURSE_SIGNED') return 'secondary';
    if (status === 'DOCTOR_SIGNED') return 'default';
    return 'outline';
  };

  const badgeStyle = { fontWeight: 600, fontSize: 11 };

  return (
    <div>
      {/* Top bar: episode + day info */}
      <div
        className="rounded-xl border border-border bg-card text-card-foreground shadow-sm p-1.5 mb-1.5 flex justify-between items-center flex-wrap gap-1"
      >
        <div className="flex items-center gap-1.5">
          <h2 className="font-rubik font-extrabold text-base">
            {episode.patientName || 'Patient'}
          </h2>
          {selectedDay?.weightKg && (
            <Badge variant="outline" className="font-semibold text-xs" style={badgeStyle}>
              {`${selectedDay.weightKg} kg`}
            </Badge>
          )}
          {episode.heightCm && (
            <Badge variant="outline" className="font-semibold text-xs" style={badgeStyle}>
              {`${episode.heightCm} cm`}
            </Badge>
          )}
          {episode.ward && (
            <Badge variant="outline" className="font-semibold text-xs" style={badgeStyle}>
              {[episode.ward, episode.bedNumber].filter(Boolean).join(' / ')}
            </Badge>
          )}
          {episode.admissionDiagnosis && (
            <Badge variant="outline" className="font-semibold text-xs max-w-[200px]" style={badgeStyle}>
              {episode.admissionDiagnosis}
            </Badge>
          )}
          {selectedDay && (
            <Badge variant="outline" className="font-semibold text-xs" style={badgeStyle}>
              {`День ${selectedDay.dayNumber}`}
            </Badge>
          )}
          <Badge
            variant={dayBadgeVariant(selectedDay?.status ?? '')}
            className={cn(
              'font-semibold text-xs',
              (selectedDay?.status === 'OPEN' || selectedDay?.status === 'REOPENED') && 'bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 border-yellow-500/20',
              selectedDay?.status === 'DOCTOR_SIGNED' && 'bg-green-500/10 text-green-600 dark:text-green-400 border-green-500/20',
            )}
            style={badgeStyle}
          >
            {selectedDay?.status === 'OPEN' ? 'Відкритий'
              : selectedDay?.status === 'NURSE_SIGNED' ? 'Підписано медсестрою'
              : selectedDay?.status === 'DOCTOR_SIGNED' ? 'Підписано лікарем'
              : selectedDay?.status === 'REOPENED' ? 'Відкрито повторно'
              : 'Закрито'}
          </Badge>
        </div>
        <span className="text-xs text-muted-foreground">
          {'Епізод #' + episode.id?.slice(0, 8)}
        </span>
      </div>

      {/* Clinical day timeline */}
      <div
        className="rounded-xl border border-border bg-card text-card-foreground shadow-sm p-1.5 mb-1.5"
      >
        <ClinicalDayTimeline days={clinicalDays} selectedDayId={selectedDay?.id} onSelectDay={onSelectDay} />
      </div>

      {/* Single dynamic ICU card — all hourly inputs inline */}
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
      />
    </div>
  );
}
