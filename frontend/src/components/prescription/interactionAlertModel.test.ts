import { describe, expect, it } from 'vitest';
import type { PairInteractionWarning, PrescriptionInteractionsResponse } from '../../types/medication';
import { deriveInteractionAlertModel, priorityOf } from './interactionAlertModel';

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

function responseWith(warnings: PrescriptionInteractionsResponse['warnings']): PrescriptionInteractionsResponse {
  return { warnings, missingAtc: null };
}

describe('interactionAlertModel', () => {
  it('returns an empty invisible model for null/empty input', () => {
    for (const input of [null, undefined, responseWith([])]) {
      const m = deriveInteractionAlertModel(input);
      expect(m.visible).toBe(false);
      expect(m.pairs).toEqual([]);
      expect(m.signature).toBe('');
      expect(m.maxPriority).toBeNull();
      expect(m.maxBucket).toBeNull();
      expect(m.totalPairs).toBe(0);
    }
  });

  it('dedupes the bidirectional wire pair into one row', () => {
    const m = deriveInteractionAlertModel(responseWith([
      { itemId: 'item-1', nameUk: 'Парацетамол 500 мг', interactions: [wirePair()] },
      {
        itemId: 'item-2', nameUk: 'Ібупрофен 200 мг',
        interactions: [wirePair({ otherItemId: 'item-1', otherNameUk: 'Парацетамол 500 мг', interactionText: 'другий текст', interactionIds: ['DI-0002'] })],
      },
    ]));
    expect(m.visible).toBe(true);
    expect(m.totalPairs).toBe(1);
    const [p] = m.pairs;
    expect(p.title).toBe('Парацетамол 500 мг + Ібупрофен 200 мг');
    expect(p.description).toBe('текст взаємодії / другий текст');
    expect(p.interactionIds).toEqual(['DI-0001', 'DI-0002']);
    expect(p.wireSeverity).toBe('high');
    expect(p.bucket).toBe('HIGH');
    expect(p.priority).toBe(3);
  });

  it('maps priorities with critical folded into HIGH', () => {
    expect(priorityOf('critical')).toEqual({ bucket: 'HIGH', priority: 3 });
    expect(priorityOf('high')).toEqual({ bucket: 'HIGH', priority: 3 });
    expect(priorityOf('medium')).toEqual({ bucket: 'MEDIUM', priority: 2 });
    expect(priorityOf('low')).toEqual({ bucket: 'LOW', priority: 1 });
  });

  it('falls back to MEDIUM for unknown severities without throwing', () => {
    expect(priorityOf('extreme')).toEqual({ bucket: 'MEDIUM', priority: 2 });
    const m = deriveInteractionAlertModel(responseWith([
      { itemId: 'item-1', nameUk: 'A', interactions: [wirePair({ severity: 'extreme' })] },
    ]));
    expect(m.visible).toBe(true);
    expect(m.maxBucket).toBe('MEDIUM');
  });

  it('orders pairs critical first, then by name', () => {
    const m = deriveInteractionAlertModel(responseWith([
      { itemId: 'item-m', nameUk: 'М-препарат', interactions: [wirePair({ severity: 'medium', otherItemId: 'item-x', otherNameUk: 'X' })] },
      { itemId: 'item-c', nameUk: 'А-препарат', interactions: [wirePair({ severity: 'critical', otherItemId: 'item-y', otherNameUk: 'Y' })] },
      { itemId: 'item-h', nameUk: 'Б-препарат', interactions: [wirePair({ severity: 'high', otherItemId: 'item-z', otherNameUk: 'Z' })] },
    ]));
    expect(m.pairs.map((p) => p.wireSeverity)).toEqual(['critical', 'high', 'medium']);
    expect(m.maxPriority).toBe(3);
    expect(m.maxBucket).toBe('HIGH');
  });

  it('keeps the signature stable for identical input and changes it on overlap change', () => {
    const warnings = responseWith([
      { itemId: 'item-1', nameUk: 'A', interactions: [wirePair()] },
    ]);
    const a = deriveInteractionAlertModel(warnings);
    const b = deriveInteractionAlertModel(responseWith([
      { itemId: 'item-1', nameUk: 'A', interactions: [wirePair()] },
    ]));
    expect(b.signature).toBe(a.signature);
    const c = deriveInteractionAlertModel(responseWith([
      { itemId: 'item-1', nameUk: 'A', interactions: [wirePair({ overlapEnd: '2026-09-23' })] },
    ]));
    expect(c.signature).not.toBe(a.signature);
  });

  it('builds an accessibility label with names and level in words', () => {
    const m = deriveInteractionAlertModel(responseWith([
      { itemId: 'item-1', nameUk: 'Парацетамол 500 мг', interactions: [wirePair()] },
    ]));
    expect(m.pairs[0].accessibilityLabel).toContain('Парацетамол 500 мг');
    expect(m.pairs[0].accessibilityLabel).toContain('Ібупрофен 200 мг');
    expect(m.pairs[0].accessibilityLabel).toContain('високо');
  });
});
