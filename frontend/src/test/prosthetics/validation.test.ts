import { describe, it, expect } from 'vitest';
import { computeProgress, fmt, validateElementValues } from '@/prosthetics/validation';
import type { SnapshotElement } from '@/prosthetics/types';

function el(overrides: Partial<SnapshotElement> & Pick<SnapshotElement, 'id' | 'elementType' | 'label'>): SnapshotElement {
  return {
    required: false,
    unit: null,
    minValue: null,
    maxValue: null,
    minCount: null,
    maxCount: null,
    regexPattern: null,
    options: null,
    mimeTypes: null,
    maxSizeMb: null,
    ...overrides,
  };
}

describe('fmt', () => {
  it('formats zero', () => {
    expect(fmt(0)).toBe('00:00:00');
  });

  it('formats seconds below a minute', () => {
    expect(fmt(59)).toBe('00:00:59');
  });

  it('formats minutes and hours with padding', () => {
    expect(fmt(3661)).toBe('01:01:01');
    expect(fmt(3600)).toBe('01:00:00');
  });

  it('formats more than 99 hours', () => {
    expect(fmt(359999)).toBe('99:59:59');
  });

  it('formats a full day', () => {
    expect(fmt(86399)).toBe('23:59:59');
  });
});

describe('computeProgress', () => {
  it('returns 0 for empty template', () => {
    expect(computeProgress(0, 0)).toBe(0);
  });

  it('returns 0 when nothing is done', () => {
    expect(computeProgress(0, 5)).toBe(0);
  });

  it('rounds the percentage', () => {
    expect(computeProgress(2, 5)).toBe(40);
    expect(computeProgress(1, 3)).toBe(33);
  });

  it('returns 100 when everything is done', () => {
    expect(computeProgress(5, 5)).toBe(100);
  });
});

describe('validateElementValues', () => {
  it('accepts an empty step', () => {
    expect(validateElementValues([], {})).toEqual({});
  });

  it('reports required text fields', () => {
    const e = el({ id: 'a', elementType: 'TEXT_INPUT', label: 'Назва', required: true });
    expect(validateElementValues([e], {})).toEqual({ a: "Поле обов'язкове" });
    expect(validateElementValues([e], { a: '' })).toEqual({ a: "Поле обов'язкове" });
    expect(validateElementValues([e], { a: null })).toEqual({ a: "Поле обов'язкове" });
  });

  it('accepts a filled required field', () => {
    const e = el({ id: 'a', elementType: 'TEXT_INPUT', label: 'Назва', required: true });
    expect(validateElementValues([e], { a: 'Протез' })).toEqual({});
  });

  it('reports required checkbox confirmation', () => {
    const e = el({ id: 'c', elementType: 'CHECKBOX', label: 'Підтвердити', required: true });
    expect(validateElementValues([e], { c: false })).toEqual({ c: "Обов'язкове підтвердження" });
    expect(validateElementValues([e], { c: undefined })).toEqual({ c: "Обов'язкове підтвердження" });
  });

  it('accepts a checked required checkbox', () => {
    const e = el({ id: 'c', elementType: 'CHECKBOX', label: 'Підтвердити', required: true });
    expect(validateElementValues([e], { c: true })).toEqual({});
  });

  it('accepts an empty optional field', () => {
    const e = el({ id: 'a', elementType: 'TEXT_INPUT', label: 'Назва' });
    expect(validateElementValues([e], {})).toEqual({});
  });

  it('reports non-numeric values', () => {
    const e = el({ id: 'n', elementType: 'NUMERIC_INPUT', label: 'Обхват' });
    expect(validateElementValues([e], { n: 'abc' })).toEqual({ n: 'Введіть число' });
  });

  it('enforces minValue with unit', () => {
    const e = el({ id: 'n', elementType: 'NUMERIC_INPUT', label: 'Обхват', minValue: 10, unit: 'см' });
    expect(validateElementValues([e], { n: '5' })).toEqual({ n: 'Мінімум 10 см' });
  });

  it('enforces maxValue with unit', () => {
    const e = el({ id: 'n', elementType: 'NUMERIC_INPUT', label: 'Обхват', maxValue: 80, unit: 'см' });
    expect(validateElementValues([e], { n: '90' })).toEqual({ n: 'Максимум 80 см' });
  });

  it('accepts boundary values', () => {
    const e = el({ id: 'n', elementType: 'NUMERIC_INPUT', label: 'Обхват', minValue: 10, maxValue: 80 });
    expect(validateElementValues([e], { n: '10' })).toEqual({});
    expect(validateElementValues([e], { n: '80' })).toEqual({});
  });

  it('skips numeric checks for empty values', () => {
    const e = el({ id: 'n', elementType: 'NUMERIC_INPUT', label: 'Обхват', minValue: 10 });
    expect(validateElementValues([e], { n: '' })).toEqual({});
  });

  it('validates regex patterns', () => {
    const e = el({ id: 'r', elementType: 'TEXT_INPUT', label: 'Код', regexPattern: '^\\d{4}$' });
    expect(validateElementValues([e], { r: '123' })).toEqual({ r: 'Формат не відповідає вимогам' });
    expect(validateElementValues([e], { r: '1234' })).toEqual({});
  });

  it('skips invalid regex patterns', () => {
    const e = el({ id: 'r', elementType: 'TEXT_INPUT', label: 'Код', regexPattern: '(' });
    expect(validateElementValues([e], { r: '123' })).toEqual({});
  });

  it('does not regex-check non-string values', () => {
    const e = el({ id: 'r', elementType: 'TEXT_INPUT', label: 'Код', regexPattern: '^\\d{4}$' });
    expect(validateElementValues([e], { r: true })).toEqual({});
  });

  it('enforces minCount for text inputs', () => {
    const e = el({ id: 't', elementType: 'TEXT_INPUT', label: 'Опис', minCount: 5 });
    expect(validateElementValues([e], { t: 'abc' })).toEqual({ t: 'Мінімум 5 символів' });
    expect(validateElementValues([e], { t: 'abcde' })).toEqual({});
  });

  it('enforces maxCount for textareas', () => {
    const e = el({ id: 't', elementType: 'TEXTAREA', label: 'Опис', maxCount: 3 });
    expect(validateElementValues([e], { t: 'abcd' })).toEqual({ t: 'Максимум 3 символів' });
    expect(validateElementValues([e], { t: 'abc' })).toEqual({});
  });

  it('does not apply count limits to numeric inputs', () => {
    const e = el({ id: 'n', elementType: 'NUMERIC_INPUT', label: 'Обхват', minCount: 5, maxCount: 10 });
    expect(validateElementValues([e], { n: '42' })).toEqual({});
  });

  it('skips count checks for empty strings', () => {
    const e = el({ id: 't', elementType: 'TEXT_INPUT', label: 'Опис', minCount: 5 });
    expect(validateElementValues([e], { t: '' })).toEqual({});
  });

  it('collects errors for multiple elements', () => {
    const els = [
      el({ id: 'a', elementType: 'TEXT_INPUT', label: 'Назва', required: true }),
      el({ id: 'n', elementType: 'NUMERIC_INPUT', label: 'Обхват', minValue: 10 }),
    ];
    expect(validateElementValues(els, { n: '2' })).toEqual({
      a: "Поле обов'язкове",
      n: 'Мінімум 10 ',
    });
  });
});
