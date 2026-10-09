import { beforeEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import InteractionAlert from './InteractionAlert';
import type { PairInteractionWarning, PrescriptionInteractionsResponse } from '../../types/medication';

const STORE_KEY = 'interaction-alert-expanded:list-1';

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

describe('InteractionAlert', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('renders nothing without warnings', () => {
    const { container } = render(<InteractionAlert interactions={{ warnings: [], missingAtc: null }} listId="list-1" />);
    expect(container).toBeEmptyDOMElement();
    render(<InteractionAlert interactions={null} />);
    expect(screen.queryByTestId('interaction-alert')).toBeNull();
  });

  it('announces a single pair assertively on mount with the exact string', () => {
    render(<InteractionAlert interactions={wireResponse('high')} listId="list-1" />);
    const alert = screen.getByTestId('interaction-alert');
    expect(alert).toHaveAttribute('role', 'alert');
    expect(alert).toHaveAttribute(
      'aria-label',
      'Увага. Виявлено взаємодію високого рівня: Парацетамол 500 мг та Ібупрофен 200 мг. '
        + 'текст взаємодії Період перетину: 22.09 — 22.09.',
    );
    expect(alert).toHaveClass('interaction-alert-enter');
    expect(screen.getByText('Парацетамол 500 мг + Ібупрофен 200 мг')).toBeInTheDocument();
    expect(screen.getByText('високо')).toBeInTheDocument();
  });

  it('settles to a polite role after the entrance', async () => {
    render(<InteractionAlert interactions={wireResponse('medium')} listId="list-1" />);
    const alert = screen.getByTestId('interaction-alert');
    expect(alert).toHaveAttribute('role', 'alert');
    await screen.findByText('помірно');
    await new Promise((resolve) => { setTimeout(resolve, 800); });
    expect(alert).toHaveAttribute('role', 'status');
  });

  it('uses an assertive role for critical', () => {
    render(<InteractionAlert interactions={wireResponse('critical')} listId="list-1" />);
    expect(screen.getByTestId('interaction-alert')).toHaveAttribute('role', 'alert');
    expect(screen.getByText('критично')).toBeInTheDocument();
  });

  it('expands collapsed details, persists the choice, and keeps it across identical refetch', async () => {
    const two = wireResponse('medium');
    two.warnings.push({
      itemId: 'item-3', nameUk: 'Аспірин 100 мг',
      interactions: [{
        otherItemId: 'item-4', otherNameUk: 'Варфарин 5 мг', severity: 'medium',
        interactionText: 'другий текст', overlapStart: '2026-09-22', overlapEnd: '2026-09-23',
        interactionIds: ['DI-0002'],
      }],
    });
    const { rerender } = render(<InteractionAlert interactions={two} listId="list-1" />);
    expect(screen.getByText(/Виявлено взаємодій: 2/)).toBeInTheDocument();
    const toggle = screen.getByRole('button', { name: /Деталі \(2\)/ });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    // The region stays mounted (stable aria-controls) but hidden.
    const hiddenRows = screen.queryAllByTestId('interaction-pair');
    expect(hiddenRows).toHaveLength(2);
    for (const r of hiddenRows) expect(r).not.toBeVisible();

    await userEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const shownRows = screen.queryAllByTestId('interaction-pair');
    expect(shownRows).toHaveLength(2);
    for (const r of shownRows) expect(r).toBeVisible();
    expect(sessionStorage.getItem(STORE_KEY)).toBe('1');

    // Identical refetch must not reset the opened state.
    rerender(<InteractionAlert interactions={structuredClone(two)} listId="list-1" />);
    expect(screen.getByRole('button', { name: /Деталі \(2\)/ })).toHaveAttribute('aria-expanded', 'true');
    for (const r of screen.queryAllByTestId('interaction-pair')) expect(r).toBeVisible();
  });

  it('honours a stored expanded default on mount', () => {
    sessionStorage.setItem(STORE_KEY, '1');
    const two = wireResponse('medium');
    two.warnings.push({
      itemId: 'item-3', nameUk: 'Аспірин 100 мг',
      interactions: [{
        otherItemId: 'item-4', otherNameUk: 'Варфарин 5 мг', severity: 'medium',
        interactionText: 'другий текст', overlapStart: '2026-09-22', overlapEnd: '2026-09-23',
        interactionIds: ['DI-0002'],
      }],
    });
    render(<InteractionAlert interactions={two} listId="list-1" />);
    expect(screen.getByRole('button', { name: /Деталі \(2\)/ })).toHaveAttribute('aria-expanded', 'true');
    for (const r of screen.queryAllByTestId('interaction-pair')) expect(r).toBeVisible();
  });

  it('announces escalation with the exact string', () => {
    const { rerender } = render(<InteractionAlert interactions={wireResponse('medium')} listId="list-1" />);
    rerender(<InteractionAlert interactions={wireResponse('high')} listId="list-1" />);
    expect(screen.getByTestId('interaction-alert')).toHaveAttribute(
      'aria-label',
      'Увага. Рівень взаємодії підвищено до високого: Парацетамол 500 мг та Ібупрофен 200 мг. текст взаємодії',
    );
  });

  it('returns focus and announces resolution when the banner unmounts', async () => {
    const { rerender } = render(<InteractionAlert interactions={wireResponse('high')} listId="list-1" />);
    const showButton = screen.getByRole('button', { name: 'Показати в таблиці' });
    showButton.focus();
    expect(document.activeElement).toBe(showButton);

    rerender(<InteractionAlert interactions={{ warnings: [], missingAtc: null }} listId="list-1" />);
    expect(screen.queryByTestId('interaction-alert')).toBeNull();
    expect(screen.getByText('Взаємодій препаратів більше не виявлено.')).toBeInTheDocument();
    // No h1 in this render: the fallback drops focus to body.
    expect(document.activeElement).toBe(document.body);
  });
});
