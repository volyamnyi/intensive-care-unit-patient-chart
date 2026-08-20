import { describe, it, expect } from 'vitest';
import { getErrorMessage, isConflictError } from './errorMessage';

describe('getErrorMessage', () => {
  it('extracts the backend validation message from an Axios error response', () => {
    const err = { response: { data: { message: 'Створення заборонено' } } };
    expect(getErrorMessage(err, 'fallback')).toBe('Створення заборонено');
  });

  it('falls back when the Axios response carries no message', () => {
    expect(getErrorMessage({ response: { data: {} } }, 'fallback')).toBe('fallback');
    expect(getErrorMessage({ response: {} }, 'fallback')).toBe('fallback');
  });

  it('returns the message of a plain Error', () => {
    expect(getErrorMessage(new Error('boom'), 'fallback')).toBe('boom');
  });

  it('falls back for non-object values', () => {
    expect(getErrorMessage(null, 'fallback')).toBe('fallback');
    expect(getErrorMessage(undefined, 'fallback')).toBe('fallback');
    expect(getErrorMessage('oops', 'fallback')).toBe('fallback');
    expect(getErrorMessage(42, 'fallback')).toBe('fallback');
  });

  it('falls back for plain objects without a response', () => {
    expect(getErrorMessage({ foo: 'bar' }, 'fallback')).toBe('fallback');
  });
});

describe('isConflictError', () => {
  it('returns true for a 409 response', () => {
    expect(isConflictError({ response: { status: 409 } })).toBe(true);
  });

  it('returns false for non-409 responses', () => {
    expect(isConflictError({ response: { status: 400 } })).toBe(false);
    expect(isConflictError({ response: {} })).toBe(false);
  });

  it('returns false for non-object values', () => {
    expect(isConflictError(null)).toBe(false);
    expect(isConflictError(new Error('boom'))).toBe(false);
    expect(isConflictError('conflict')).toBe(false);
  });
});