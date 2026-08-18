import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import FluidBalancePanel from '../../components/common/FluidBalancePanel';
import type { FluidBalanceItem } from '../../types/icu';

const mockItems: FluidBalanceItem[] = [
  {
    id: 'fb-1',
    clinicalDayId: 'day-1',
    hour: 10,
    intake: 500,
    output: 300,
    balance: 200,
    cumulativeBalance: 200,
    version: 1,
  },
  {
    id: 'fb-2',
    clinicalDayId: 'day-1',
    hour: 11,
    intake: 250,
    output: 400,
    balance: -150,
    cumulativeBalance: 50,
    version: 1,
  },
];

function renderPanel(props: Partial<React.ComponentProps<typeof FluidBalancePanel>> = {}) {
  return render(
    <ThemeModeProvider>
      <FluidBalancePanel
        items={props.items ?? []}
        onRecalculate={props.onRecalculate}
        loading={props.loading}
      />
    </ThemeModeProvider>
  );
}

describe('FluidBalancePanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows intake and output totals', () => {
    renderPanel({ items: mockItems });
    expect(screen.getByText(/750\s*ml/)).toBeInTheDocument();
    expect(screen.getByText(/700\s*ml/)).toBeInTheDocument();
  });

  it('shows daily balance and cumulative balance', () => {
    renderPanel({ items: mockItems });
    const balances = screen.getAllByText(/\b50\s*ml/);
    expect(balances).toHaveLength(2);
  });

  it('colors negative daily balance red', () => {
    const negativeItems: FluidBalanceItem[] = [
      {
        id: 'fb-3',
        clinicalDayId: 'day-1',
        hour: 10,
        intake: 100,
        output: 500,
        balance: -400,
        cumulativeBalance: -400,
        version: 1,
      },
    ];
    renderPanel({ items: negativeItems });
    const allBalances = screen.getAllByText(/-400\s*ml/);
    expect(allBalances).toHaveLength(2);
  });

  it('colors positive daily balance green', () => {
    renderPanel({ items: mockItems });
    const allBalances = screen.getAllByText(/\b50\s*ml/);
    expect(allBalances).toHaveLength(2);
  });

  it('shows "Перерахувати" button when onRecalculate is provided', () => {
    renderPanel({ onRecalculate: vi.fn() });
    expect(screen.getByText('Перерахувати')).toBeInTheDocument();
  });

  it('does not show recalculate button when onRecalculate is not provided', () => {
    renderPanel({ items: mockItems });
    expect(screen.queryByText('Перерахувати')).not.toBeInTheDocument();
  });

  it('calls onRecalculate when button clicked', async () => {
    const onRecalculate = vi.fn();
    renderPanel({ onRecalculate, items: mockItems });
    await userEvent.click(screen.getByText('Перерахувати'));
    expect(onRecalculate).toHaveBeenCalledTimes(1);
  });

  it('shows loading state with disabled button', () => {
    const onRecalculate = vi.fn();
    renderPanel({ onRecalculate, loading: true });
    expect(screen.getByText('Розрахунок...')).toBeInTheDocument();
    expect(screen.getByText('Розрахунок...')).toBeDisabled();
  });

  it('shows 0 values for empty items', () => {
    renderPanel({ items: [] });
    const zeros = screen.getAllByText(/0\s*(ml|мл)/);
    expect(zeros).toHaveLength(4);
  });
});
