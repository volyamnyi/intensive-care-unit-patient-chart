import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import ScaleResultsPanel from './ScaleResultsPanel';
import type { ScaleResult, ClinicalScale } from '../../types/icu';

const mockScales: ClinicalScale[] = [
  { id: 'scale-1', name: 'APACHE II', description: null, isAutomatic: false, status: 'ACTIVE', createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1 },
  { id: 'scale-2', name: 'SOFA', description: null, isAutomatic: false, status: 'ACTIVE', createdBy: 1, createdAt: '', updatedBy: 0, updatedAt: '', version: 1 },
];

const mockResult: ScaleResult = {
  id: 'sr-1',
  clinicalDayId: 'day-1',
  scaleId: 'scale-1',
  scaleName: 'APACHE II',
  result: '15',
  calculatedAt: '2025-06-01T12:00:00Z',
  calculatedBy: 1,
  createdAt: '2025-06-01T12:00:00Z',
  version: 1,
};

function renderPanel(props: Partial<React.ComponentProps<typeof ScaleResultsPanel>> = {}) {
  return render(
    <ThemeModeProvider>
      <ScaleResultsPanel
        results={props.results ?? []}
        availableScales={props.availableScales ?? []}
        onCreateResult={props.onCreateResult}
      />
    </ThemeModeProvider>
  );
}

describe('ScaleResultsPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows available scales in dropdown when onCreateResult provided', async () => {
    renderPanel({ availableScales: mockScales, onCreateResult: vi.fn() });
    expect(screen.getByRole('combobox', { name: /Шкала/i })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('combobox', { name: /Шкала/i }));
    // The Base UI select renders its options asynchronously (portal + positioning).
    const option = await screen.findByRole('option', { name: 'APACHE II' });
    await userEvent.click(option);
    expect(screen.getByText('Додати')).toBeInTheDocument();
  });

  it('does not show create UI when onCreateResult is not provided', () => {
    renderPanel({ availableScales: mockScales });
    expect(screen.queryByRole('combobox', { name: /Шкала/i })).not.toBeInTheDocument();
    expect(screen.queryByText('Додати')).not.toBeInTheDocument();
  });

  it('shows existing scale results with values', () => {
    renderPanel({ results: [mockResult], availableScales: mockScales });
    expect(screen.getByText('APACHE II')).toBeInTheDocument();
    const results = screen.getAllByText(/15/);
    expect(results.length).toBeGreaterThan(0);
    expect(screen.getByText(/SOFA/)).toBeInTheDocument();
  });

  it('shows "Не заповнено" for scales without results', () => {
    renderPanel({ results: [mockResult], availableScales: mockScales });
    const notFilled = screen.getAllByText('Не заповнено');
    expect(notFilled).toHaveLength(1);
  });

  it('shows "Немає даних шкал" when no scales and no results', () => {
    renderPanel({ availableScales: [], results: [] });
    expect(screen.getByText('Немає результатів')).toBeInTheDocument();
  });

  it('can add a new scale result', async () => {
    const onCreateResult = vi.fn();
    renderPanel({ availableScales: mockScales, onCreateResult });
    const select = screen.getByRole('combobox', { name: /Шкала/i });
    await userEvent.click(select);
    const option = screen.getByRole('option', { name: 'APACHE II', hidden: true });
    await userEvent.click(option);
    const resultInput = screen.getByPlaceholderText('Результат');
    await userEvent.type(resultInput, '20');
    await userEvent.click(screen.getByText('Додати'));
    await waitFor(() => {
      expect(onCreateResult).toHaveBeenCalledWith('scale-1', '20');
    });
  });
});
