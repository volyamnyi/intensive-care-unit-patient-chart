import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import ClinicalDayTimeline from '../../components/common/ClinicalDayTimeline';
import type { ClinicalDay } from '../../types';

const mockDays: ClinicalDay[] = [
  {
    id: 'day-1',
    episodeId: 'ep-1',
    dayNumber: 1,
    startDateTime: '2025-06-01T08:00:00Z',
    endDateTime: '2025-06-02T08:00:00Z',
    status: 'OPEN',
    doctorSigned: false,
    nurseSigned: false,
    closedAt: null,
    weightKg: null,
    bmi: null,
    createdBy: 1,
    createdAt: '2025-06-01T08:00:00Z',
    updatedBy: 1,
    updatedAt: '2025-06-01T08:00:00Z',
    version: 1,
  },
  {
    id: 'day-2',
    episodeId: 'ep-1',
    dayNumber: 2,
    startDateTime: '2025-06-02T08:00:00Z',
    endDateTime: '2025-06-03T08:00:00Z',
    status: 'NURSE_SIGNED',
    doctorSigned: false,
    nurseSigned: true,
    closedAt: null,
    weightKg: null,
    bmi: null,
    createdBy: 1,
    createdAt: '2025-06-02T08:00:00Z',
    updatedBy: 1,
    updatedAt: '2025-06-02T08:00:00Z',
    version: 1,
  },
  {
    id: 'day-3',
    episodeId: 'ep-1',
    dayNumber: 3,
    startDateTime: '2025-06-03T08:00:00Z',
    endDateTime: '2025-06-04T08:00:00Z',
    status: 'DOCTOR_SIGNED',
    doctorSigned: true,
    nurseSigned: true,
    closedAt: null,
    weightKg: null,
    bmi: null,
    createdBy: 1,
    createdAt: '2025-06-03T08:00:00Z',
    updatedBy: 2,
    updatedAt: '2025-06-03T08:00:00Z',
    version: 1,
  },
];

function renderTimeline(props: Partial<React.ComponentProps<typeof ClinicalDayTimeline>> = {}) {
  return render(
    <ThemeModeProvider>
      <ClinicalDayTimeline
        days={props.days ?? []}
        selectedDayId={props.selectedDayId}
        onSelectDay={props.onSelectDay ?? vi.fn()}
      />
    </ThemeModeProvider>
  );
}

describe('ClinicalDayTimeline', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows "Немає клінічних днів" empty state', () => {
    renderTimeline({ days: [] });
    expect(screen.getByText('Немає клінічних днів')).toBeInTheDocument();
  });

  it('renders day chips for each clinical day', () => {
    renderTimeline({ days: mockDays });
    expect(screen.getByText('Доба 1')).toBeInTheDocument();
    expect(screen.getByText('Доба 2')).toBeInTheDocument();
    expect(screen.getByText('Доба 3')).toBeInTheDocument();
  });

  it('highlights the selected day with a unique style', () => {
    renderTimeline({ days: mockDays, selectedDayId: 'day-2' });
    const day2 = screen.getByText('Доба 2').closest('div');
    const day1 = screen.getByText('Доба 1').closest('div');
    expect(day2).toHaveClass('border-2');
    expect(day2).toHaveClass('border-primary');
    expect(day1).not.toHaveClass('border-2');
  });

  it('calls onSelectDay when a day chip is clicked', async () => {
    const onSelectDay = vi.fn();
    renderTimeline({ days: mockDays, onSelectDay });
    await userEvent.click(screen.getByText('Доба 2'));
    expect(onSelectDay).toHaveBeenCalledWith(mockDays[1]);
  });

  it('renders dates for each day', () => {
    renderTimeline({ days: mockDays });
    expect(screen.getByText('1 черв.')).toBeInTheDocument();
    expect(screen.getByText('2 черв.')).toBeInTheDocument();
  });
});
