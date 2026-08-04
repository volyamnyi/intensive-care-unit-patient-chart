import type {
  Episode, ClinicalDay, HourlyRecord, MedicalOrder, FluidBalanceItem,
} from '../../types';

export interface DashboardUser {
  id: number;
  role?: string;
}

export interface DashboardProps {
  user?: DashboardUser | null;
  episode: Episode;
  clinicalDays: ClinicalDay[];
  selectedDay: ClinicalDay | null;
  onSelectDay: (day: ClinicalDay) => void;
  records: HourlyRecord[];
  orders: MedicalOrder[];
  balanceItems: FluidBalanceItem[];
  isLocked: boolean;
  isNurse: boolean;
  onRefresh?: () => void;
  onFeedback?: (message: string, severity: 'success' | 'error') => void;
  dayLoading?: boolean;
}
