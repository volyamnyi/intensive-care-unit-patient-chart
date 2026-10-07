import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import InteractionAlert from './InteractionAlert';
import type { PairInteractionWarning, PrescriptionInteractionsResponse } from '../../types/medication';

function wireResponse(severity: PairInteractionWarning['severity']): PrescriptionInteractionsResponse {
  return {
    warnings: [
      {
        itemId: 'item-1', nameUk: 'Парацетамол 500 мг',
        interactions: [{
          otherItemId: 'item-2', otherNameUk: 'Ібупрофен 200 мг', severity,
          interactionText: 'текст взаємодії', overlapStart: '2026-09-22', overlapEnd: '2026-09-22',
          interactionIds: ['DI-0001'],
        }],
      },
      {
        itemId: 'item-2', nameUk: 'Ібупрофен 200 мг',
        interactions: [{
          otherItemId: 'item-1', otherNameUk: 'Парацетамол 500 мг', severity,
          interactionText: 'текст взаємодії', overlapStart: '2026-09-22', overlapEnd: '2026-09-22',
          interactionIds: ['DI-0001'],
        }],
      },
    ],
    missingAtc: null,
  };
}

describe('InteractionAlert', () => {
  it('renders nothing without warnings', () => {
    const { container } = render(<InteractionAlert interactions={{ warnings: [], missingAtc: null }} />);
    expect(container).toBeEmptyDOMElement();
    render(<InteractionAlert interactions={null} />);
    expect(screen.queryByTestId('interaction-alert')).toBeNull();
  });

  it('renders a single pair with status role and one-time entrance', () => {
    render(<InteractionAlert interactions={wireResponse('high')} />);
    const alert = screen.getByTestId('interaction-alert');
    expect(alert).toHaveAttribute('role', 'status');
    expect(alert).toHaveClass('interaction-alert-enter');
    expect(screen.getByText('Парацетамол 500 мг + Ібупрофен 200 мг')).toBeInTheDocument();
    expect(screen.getByText('високо')).toBeInTheDocument();
  });

  it('uses an assertive role for critical', () => {
    render(<InteractionAlert interactions={wireResponse('critical')} />);
    expect(screen.getByTestId('interaction-alert')).toHaveAttribute('role', 'alert');
    expect(screen.getByText('критично')).toBeInTheDocument();
  });

  it('aggregates multiple pairs behind collapsed details and keeps them open across identical refetch', async () => {
    const two = wireResponse('medium');
    two.warnings.push({
      itemId: 'item-3', nameUk: 'Аспірин 100 мг',
      interactions: [{
        otherItemId: 'item-4', otherNameUk: 'Варфарин 5 мг', severity: 'medium',
        interactionText: 'другий текст', overlapStart: '2026-09-22', overlapEnd: '2026-09-23',
        interactionIds: ['DI-0002'],
      }],
    });
    const { rerender } = render(<InteractionAlert interactions={two} />);
    expect(screen.getByText(/Виявлено взаємодій: 2/)).toBeInTheDocument();
    // Native closed <details> keeps children in the DOM — assert the open state.
    const detailsOf = () => screen.getByText(/Деталі \(2\)/).closest('details');
    expect(detailsOf() as Element).not.toHaveAttribute('open');

    await userEvent.click(screen.getByText(/Деталі \(2\)/));
    expect(detailsOf() as Element).toHaveAttribute('open');
    expect(screen.queryAllByTestId('interaction-pair')).toHaveLength(2);

    // Identical refetch must not remount: the opened details stay open.
    rerender(<InteractionAlert interactions={structuredClone(two)} />);
    expect(detailsOf() as Element).toHaveAttribute('open');
    expect(screen.queryAllByTestId('interaction-pair')).toHaveLength(2);
  });
});
