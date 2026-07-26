import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PatientStatePanel from '../../components/common/PatientStatePanel';
import type { PatientStateAssessment, PatientStateCreateRequest } from '../../types';

const mockState: PatientStateAssessment[] = [
  {
    id: 'ps-1', clinicalDayId: 'day-1', recordHour: 9, consciousness: 'alert', skin: 'normal',
    edema: 'none', mucousMembranes: 'normal', peripheralCirculation: 'normal',
    bowelSounds: 'normal', generalCondition: '', additionalNotes: '', version: 1,
  },
];

function renderPanel(props: Partial<React.ComponentProps<typeof PatientStatePanel>> = {}) {
  return render(
    <PatientStatePanel
      clinicalDayId="day-1"
      assessments={props.assessments ?? []}
      isLocked={props.isLocked ?? false}
      onCreate={props.onCreate ?? vi.fn()}
    />
  );
}

describe('PatientStatePanel', () => {
  beforeEach(() => vi.clearAllMocks());

  it('shows empty message when no assessments', () => {
    renderPanel({ assessments: [] });
    expect(screen.getByText('Немає оцінок')).toBeInTheDocument();
  });

  it('renders existing assessment', () => {
    renderPanel({ assessments: mockState });
    expect(screen.getByText('9:00')).toBeInTheDocument();
    expect(screen.getAllByText(/Ясна/).length).toBeGreaterThan(0);
  });

  it('hides create UI when locked', () => {
    renderPanel({ assessments: [], isLocked: true });
    expect(screen.queryByLabelText('Свідомість')).not.toBeInTheDocument();
  });

  it('creates an assessment from the form', async () => {
    const onCreate = vi.fn();
    renderPanel({ assessments: [], onCreate });
    await userEvent.click(screen.getByLabelText('Свідомість'));
    await userEvent.click(await screen.findByRole('option', { name: 'Сонливість' }));
    await userEvent.click(screen.getByLabelText('Шкіра'));
    await userEvent.click(await screen.findByRole('option', { name: 'Ціаноз' }));
    await userEvent.click(screen.getByText('Додати'));
    await waitFor(() => {
      const call = onCreate.mock.calls[0][0] as PatientStateCreateRequest;
      expect(call.consciousness).toBe('drowsy');
      expect(call.skin).toBe('cyanotic');
    });
  });
});
