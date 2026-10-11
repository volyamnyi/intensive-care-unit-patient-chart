import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import InteractionAlertOverlay, { INTERACTION_OVERLAY_AUTODISMISS_MS } from './InteractionAlertOverlay';
import type { PairInteractionWarning, PrescriptionInteractionsResponse } from '../../types/medication';

function wirePair(overrides: Record<string, unknown> = {}): PairInteractionWarning {
  return {
    otherItemId: 'item-2',
    otherNameUk: 'Ібупрофен 200 мг',
    severity: 'high',
    interactionText: 'текст взаємодії',
    overlapStart: '2026-09-22',
    overlapEnd: '2026-09-22',
    interactionIds: ['DI-0001'],
    ...overrides,
  } as PairInteractionWarning;
}

function wireResponse(severity: PairInteractionWarning['severity']): PrescriptionInteractionsResponse {
  return {
    warnings: [
      {
        itemId: 'item-1', nameUk: 'Парацетамол 500 мг',
        interactions: [wirePair({ severity })],
      },
      {
        itemId: 'item-2', nameUk: 'Ібупрофен 200 мг',
        interactions: [wirePair({ severity, otherItemId: 'item-1', otherNameUk: 'Парацетамол 500 мг' })],
      },
    ],
    missingAtc: null,
  };
}

describe('InteractionAlertOverlay', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders nothing without warnings', () => {
    const { container } = render(<InteractionAlertOverlay interactions={{ warnings: [], missingAtc: null }} />);
    expect(container).toBeEmptyDOMElement();
    render(<InteractionAlertOverlay interactions={null} />);
    expect(screen.queryByTestId('interaction-alert-overlay')).toBeNull();
  });

  it('slides in centered on initial detect without stealing the assertive role', () => {
    render(<InteractionAlertOverlay interactions={wireResponse('high')} />);
    const overlay = screen.getByTestId('interaction-alert-overlay');
    expect(overlay).toHaveClass('interaction-alert-overlay');
    expect(overlay.firstElementChild).toHaveClass('interaction-alert-center-enter');
    // The banner owns the single role="alert" announcement; the overlay is visual only.
    expect(overlay.firstElementChild).toHaveAttribute('role', 'group');
    expect(overlay.firstElementChild).not.toHaveAttribute('role', 'alert');
    expect(screen.getByText('Парацетамол 500 мг + Ібупрофен 200 мг')).toBeInTheDocument();
    expect(screen.getByText('високо')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Закрити сповіщення' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Показати в таблиці' })).toBeInTheDocument();
  });

  it('stays visible across an identical refetch without re-animating', () => {
    const { rerender } = render(<InteractionAlertOverlay interactions={wireResponse('high')} />);
    const first = screen.getByTestId('interaction-alert-overlay').firstElementChild;
    rerender(<InteractionAlertOverlay interactions={structuredClone(wireResponse('high'))} />);
    expect(screen.getByTestId('interaction-alert-overlay')).toBeInTheDocument();
    expect(screen.getByTestId('interaction-alert-overlay').firstElementChild).toBe(first);
  });

  it('dismisses via the close button and never reshows the same signature', () => {
    const { rerender } = render(<InteractionAlertOverlay interactions={wireResponse('high')} />);
    fireEvent.click(screen.getByRole('button', { name: 'Закрити сповіщення' }));
    expect(screen.queryByTestId('interaction-alert-overlay')).toBeNull();
    rerender(<InteractionAlertOverlay interactions={structuredClone(wireResponse('high'))} />);
    expect(screen.queryByTestId('interaction-alert-overlay')).toBeNull();
  });

  it('dismisses via Escape', () => {
    render(<InteractionAlertOverlay interactions={wireResponse('high')} />);
    expect(screen.getByTestId('interaction-alert-overlay')).toBeInTheDocument();
    act(() => { window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })); });
    expect(screen.queryByTestId('interaction-alert-overlay')).toBeNull();
  });

  it(`auto-dismisses after ${INTERACTION_OVERLAY_AUTODISMISS_MS}ms and stays dismissed`, () => {
    const { rerender } = render(<InteractionAlertOverlay interactions={wireResponse('medium')} />);
    expect(screen.getByTestId('interaction-alert-overlay')).toBeInTheDocument();
    act(() => { vi.advanceTimersByTime(INTERACTION_OVERLAY_AUTODISMISS_MS); });
    expect(screen.queryByTestId('interaction-alert-overlay')).toBeNull();
    rerender(<InteractionAlertOverlay interactions={structuredClone(wireResponse('medium'))} />);
    expect(screen.queryByTestId('interaction-alert-overlay')).toBeNull();
  });

  it('reshows on escalation to a fresh signature', () => {
    const { rerender } = render(<InteractionAlertOverlay interactions={wireResponse('medium')} />);
    fireEvent.click(screen.getByRole('button', { name: 'Закрити сповіщення' }));
    expect(screen.queryByTestId('interaction-alert-overlay')).toBeNull();
    rerender(<InteractionAlertOverlay interactions={wireResponse('high')} />);
    expect(screen.getByTestId('interaction-alert-overlay')).toBeInTheDocument();
    expect(screen.getByText('високо')).toBeInTheDocument();
  });

  it('pair button dismisses the overlay and scrolls the grid cell into view', () => {
    const scrollIntoView = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoView as unknown as typeof Element.prototype.scrollIntoView;
    try {
      render(
        <>
          <div data-item-id="item-1">Парацетамол 500 мг</div>
          <InteractionAlertOverlay interactions={wireResponse('high')} />
        </>,
      );
      fireEvent.click(screen.getByRole('button', { name: 'Показати в таблиці' }));
      expect(screen.queryByTestId('interaction-alert-overlay')).toBeNull();
      expect(scrollIntoView).toHaveBeenCalledWith({ block: 'nearest' });
      expect(document.querySelector('[data-item-id="item-1"]')).toHaveAttribute('data-active-pair', 'true');
    } finally {
      delete (Element.prototype as { scrollIntoView?: unknown }).scrollIntoView;
    }
  });
});
