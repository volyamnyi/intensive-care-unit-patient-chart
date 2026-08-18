import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../../styles/ThemeContext';
import DayPartPlanner from '../../../components/prescription/DayPartPlanner';
import { allDayPartsCompleted } from '../../../components/prescription/prescriptionDayParts';
import type { PrescriptionDayPart } from '../../../types/medication';

const mockParts: PrescriptionDayPart[] = [
  { id: 'dp1', dayId: 'd1', period: 'morning', dose: null, isPlanned: false, isPlannedFinished: false, isCompleted: false, isCompletedFinished: false, doctorName: null, nurseName: null },
  { id: 'dp2', dayId: 'd1', period: 'evening', dose: '5mg', isPlanned: true, isPlannedFinished: false, isCompleted: false, isCompletedFinished: false, doctorName: null, nurseName: null },
  { id: 'dp3', dayId: 'd1', period: 'night', dose: '10mg', isPlanned: true, isPlannedFinished: false, isCompleted: true, isCompletedFinished: false, doctorName: null, nurseName: null },
];

function renderPlanner(props: Partial<React.ComponentProps<typeof DayPartPlanner>> = {}) {
  return render(
    <ThemeModeProvider>
      <DayPartPlanner
        dayParts={props.dayParts ?? mockParts}
        onPlan={props.onPlan ?? vi.fn()}
        onComplete={props.onComplete ?? vi.fn()}
        canPlan={props.canPlan ?? true}
        canComplete={props.canComplete ?? true}
      />
    </ThemeModeProvider>
  );
}

describe('DayPartPlanner', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders day parts', () => {
    renderPlanner();
    expect(screen.getByText('Ранок')).toBeInTheDocument();
    expect(screen.getByText('Вечір')).toBeInTheDocument();
    expect(screen.getByText('Ніч')).toBeInTheDocument();
  });

  it('shows empty state', () => {
    renderPlanner({ dayParts: [] });
    expect(screen.getByText('Немає запланованих частин доби')).toBeInTheDocument();
  });

  it('calls onPlan with dose', async () => {
    const onPlan = vi.fn();
    renderPlanner({ onPlan });
    const doseInput = screen.getAllByPlaceholderText('Доза')[0];
    await userEvent.type(doseInput, '15mg');
    await userEvent.click(screen.getAllByText('Запланувати')[0]);
    await waitFor(() => {
      expect(onPlan).toHaveBeenCalledWith('dp1', '15mg');
    });
  });

  it('calls onComplete for planned part', async () => {
    const onComplete = vi.fn();
    renderPlanner({ onComplete });
    await userEvent.click(screen.getByText('Завершити'));
    await waitFor(() => {
      expect(onComplete).toHaveBeenCalledWith('dp2');
    });
  });
});

describe('allDayPartsCompleted', () => {
  it('returns true when all completed', () => {
    const parts = mockParts.map((p) => ({ ...p, isCompleted: true }));
    expect(allDayPartsCompleted(parts)).toBe(true);
  });

  it('returns false when some are not completed', () => {
    expect(allDayPartsCompleted(mockParts)).toBe(false);
  });
});
