import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LabResultsPanel from '../../components/common/LabResultsPanel';
import type { LabResult, LabResultCreateRequest } from '../../types';

const mockLabs: LabResult[] = [
  {
    id: 'lab-1', clinicalDayId: 'day-1', testCode: 'Hb', testName: 'Hemoglobin',
    result: '10.5', unit: 'g/dL', referenceMin: 12, referenceMax: 16,
    isAbnormal: true, measuredAt: '2025-06-01T12:00:00Z', createdAt: '', version: 1,
  },
  {
    id: 'lab-2', clinicalDayId: 'day-1', testCode: 'Na', testName: 'Sodium',
    result: '140', unit: 'mmol/L', referenceMin: 135, referenceMax: 145,
    isAbnormal: false, measuredAt: '2025-06-01T12:00:00Z', createdAt: '', version: 1,
  },
];

function renderPanel(props: Partial<React.ComponentProps<typeof LabResultsPanel>> = {}) {
  const utils = render(
    <LabResultsPanel
      clinicalDayId="day-1"
      labs={props.labs ?? []}
      isLocked={props.isLocked ?? false}
      onCreate={props.onCreate ?? vi.fn()}
    />
  );
  return { ...utils, selects: () => Array.from(utils.container.querySelectorAll('select')) as HTMLSelectElement[] };
}

describe('LabResultsPanel', () => {
  beforeEach(() => vi.clearAllMocks());

  it('shows empty message when no labs', () => {
    renderPanel({ labs: [] });
    expect(screen.getByText('Немає лабораторних досліджень')).toBeInTheDocument();
  });

  it('renders existing labs with abnormal flag', () => {
    renderPanel({ labs: mockLabs });
    expect(screen.getByText('Hemoglobin')).toBeInTheDocument();
    expect(screen.getByText('Аномалія')).toBeInTheDocument();
  });

  it('hides create UI when locked', () => {
    renderPanel({ labs: [], isLocked: true });
    expect(screen.queryByLabelText('Тест')).not.toBeInTheDocument();
  });

  it('creates a lab result from the form', async () => {
    const onCreate = vi.fn();
    renderPanel({ labs: [], onCreate });
    await userEvent.click(screen.getByLabelText('Тест'));
    await userEvent.click(await screen.findByRole('option', { name: /Hemoglobin|Hb/ }));
    const numberInputs = Array.from(document.querySelectorAll('input[type="number"]')) as HTMLInputElement[];
    fireEvent.change(numberInputs[0], { target: { value: '13.5' } });
    await userEvent.click(screen.getByText('Додати'));
    await waitFor(() => {
      const call = onCreate.mock.calls[0][0] as LabResultCreateRequest;
      expect(call.testCode).toBe('Hb');
      expect(call.testName).toBe('Hemoglobin');
      expect(call.result).toBe('13.5');
    });
  });
});
